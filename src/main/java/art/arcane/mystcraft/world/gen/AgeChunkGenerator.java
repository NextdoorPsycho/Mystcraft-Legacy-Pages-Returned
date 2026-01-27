package art.arcane.mystcraft.world.gen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import art.arcane.mystcraft.api.world.logic.IChunkProviderFinalization;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerNoise;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorFlat;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorVoid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chunk generator for Mystcraft Ages that WRAPS vanilla's NoiseBasedChunkGenerator.
 *
 * For normal/amplified terrain: Delegates to vanilla's terrain generation, then applies
 * Mystcraft alterations and populators on top. This allows symbols to MODULATE vanilla
 * terrain rather than replace it entirely.
 *
 * For special terrain (void, flat): Uses custom lightweight generators.
 *
 * Most symbols modify vanilla generation rather than replacing it.
 */
public class AgeChunkGenerator extends ChunkGenerator {

    public static final Codec<AgeChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource),
                    Codec.STRING.fieldOf("terrain_type").forGetter(gen -> gen.terrainType),
                    Codec.INT.fieldOf("ground_level").forGetter(gen -> gen.groundLevel),
                    Codec.INT.fieldOf("sea_level").forGetter(gen -> gen.seaLevelValue),
                    Codec.BOOL.fieldOf("has_sea").forGetter(gen -> gen.hasSea),
                    Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed),
                    Codec.INT.fieldOf("age_uid").forGetter(gen -> gen.ageUID)
            ).apply(instance, AgeChunkGenerator::new)
    );

    // Configuration stored for serialization
    private final String terrainType;
    private final int groundLevel;
    private final int seaLevelValue;
    private final boolean hasSea;
    private final long seed;
    private final int ageUID;

    // Director with registered interfaces (symbols register here)
    private AgeDirectorImpl director;

    // Vanilla generator delegate - used for normal/amplified terrain
    // volatile for safe double-check locking (no full synchronized needed)
    private volatile NoiseBasedChunkGenerator vanillaDelegate;
    private volatile RandomState vanillaRandomState;
    private volatile boolean delegateInitialized = false;

    // Debug counters for first N chunks
    private final AtomicInteger fillFromNoiseCount = new AtomicInteger(0);
    private final AtomicInteger buildSurfaceCount = new AtomicInteger(0);
    private final AtomicInteger biomeDecorationCount = new AtomicInteger(0);
    private static final int DEBUG_CHUNK_LIMIT = 10;

    // Default block states
    private final BlockState bedrockBlock = Blocks.BEDROCK.defaultBlockState();

    /**
     * Creates a chunk generator from codec deserialization.
     */
    public AgeChunkGenerator(BiomeSource biomeSource, String terrainType, int groundLevel,
                             int seaLevel, boolean hasSea, long seed, int ageUID) {
        super(biomeSource);
        this.terrainType = terrainType;
        this.groundLevel = groundLevel;
        this.seaLevelValue = seaLevel;
        this.hasSea = hasSea;
        this.seed = seed;
        this.ageUID = ageUID;
    }

    /**
     * Creates a chunk generator from an AgeDirector configuration.
     */
    public AgeChunkGenerator(AgeDirectorImpl director, BiomeSource biomeSource, long seed, int ageUID) {
        super(biomeSource);
        this.director = director;
        this.terrainType = director.getTerrainType();
        this.groundLevel = director.getAverageGroundLevel();
        this.seaLevelValue = director.getSeaLevel();
        this.hasSea = director.hasSea();
        this.seed = seed;
        this.ageUID = ageUID;
    }

    /**
     * Creates a chunk generator from an AgeDirector.
     */
    public static AgeChunkGenerator fromDirector(AgeDirectorImpl director, BiomeSource biomeSource, int ageUID) {
        return new AgeChunkGenerator(director, biomeSource, director.getSeed(), ageUID);
    }

    /**
     * Sets the director for this generator.
     */
    public void setDirector(AgeDirectorImpl director) {
        this.director = director;
    }

    /**
     * Checks if this terrain type should use vanilla delegation.
     * Normal, amplified, nether, and end all use vanilla noise-based generation.
     */
    /**
     * Gets the terrain type string (e.g. "normal", "nether", "end", "void", "flat", "cave", "skylands").
     */
    public String getTerrainType() {
        return terrainType;
    }

    private boolean usesVanillaDelegate() {
        return "normal".equals(terrainType) || "amplified".equals(terrainType)
                || "nether".equals(terrainType) || "end".equals(terrainType)
                || terrainType == null;
    }

    /**
     * Initializes the vanilla delegate generator lazily.
     * This gets the overworld's NoiseGeneratorSettings and creates a NoiseBasedChunkGenerator.
     */
    private void ensureVanillaDelegate() {
        // Fast path: volatile read, no locking needed once initialized
        if (delegateInitialized) {
            return;
        }

        // Slow path: double-check locking to avoid full synchronization
        // on every chunk gen call (which can cause monitor deadlocks with
        // the server thread's managedBlock task pumping)
        synchronized (this) {
            if (delegateInitialized) {
                return;
            }

            String threadName = Thread.currentThread().getName();
            Mystcraft.LOGGER.info("[ChunkGen] Age {} ensureVanillaDelegate called on thread: {}", ageUID, threadName);

            if (!usesVanillaDelegate()) {
                delegateInitialized = true;
                Mystcraft.LOGGER.info("[ChunkGen] Age {} does not use vanilla delegate (type: {})", ageUID, terrainType);
                return;
            }

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                // Don't set delegateInitialized - retry next call when server may be available
                Mystcraft.LOGGER.warn("[ChunkGen] Age {} cannot initialize vanilla delegate: server not available (will retry) [thread: {}]",
                        ageUID, threadName);
                return;
            }
            // Note: delegateInitialized set to true at the END of this block,
            // after vanillaDelegate and vanillaRandomState are fully assigned.
            // This ensures other threads see the delegate before the flag.

            try {
                // Get the appropriate NoiseGeneratorSettings for the terrain type
                Holder<NoiseGeneratorSettings> noiseSettings;
                var noiseSettingsRegistry = server.registryAccess()
                        .registryOrThrow(Registries.NOISE_SETTINGS);
                if ("amplified".equals(terrainType)) {
                    noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED);
                } else if ("nether".equals(terrainType)) {
                    noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.NETHER);
                } else if ("end".equals(terrainType)) {
                    noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.END);
                } else {
                    noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
                }

                // Create vanilla generator with our biome source
                vanillaDelegate = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);

                // Create a proper RandomState with real noise settings instead of dummy
                // ServerChunkCache creates RandomState with NoiseGeneratorSettings.dummy() for
                // non-NoiseBasedChunkGenerator generators, which produces flat terrain.
                // We need a RandomState built from the actual overworld/amplified noise settings.
                vanillaRandomState = RandomState.create(
                        noiseSettings.value(),
                        server.registryAccess().lookupOrThrow(Registries.NOISE),
                        seed
                );

                Mystcraft.LOGGER.info("[ChunkGen] Age {} initialized vanilla terrain delegate (type: {}, noiseSettings: {})",
                        ageUID, terrainType,
                        "amplified".equals(terrainType) ? "AMPLIFIED" :
                        "nether".equals(terrainType) ? "NETHER" :
                        "end".equals(terrainType) ? "END" : "OVERWORLD");
            } catch (Exception e) {
                Mystcraft.LOGGER.error("Failed to initialize vanilla delegate for age {}", ageUID, e);
            }

            // Set AFTER delegate and randomState are assigned so other threads
            // see the fully-initialized fields before the flag.
            delegateInitialized = true;
        }
    }

    /**
     * Reconstructs the director from saved AgeData.
     */
    public void reconstructDirectorFromAgeData(ServerLevel level) {
        Mystcraft.LOGGER.info("[ChunkGen] Age {} reconstructDirectorFromAgeData called on thread: {} | director={}",
                ageUID, Thread.currentThread().getName(), director != null ? "exists" : "null");

        if (director != null) {
            return;
        }

        if (ageUID <= 0) {
            Mystcraft.LOGGER.warn("Cannot reconstruct director: no valid ageUID");
            return;
        }

        AgeData ageData = AgeData.getIfPresent(level);
        if (ageData == null) {
            Mystcraft.LOGGER.warn("Cannot reconstruct director for age {}: no AgeData found", ageUID);
            return;
        }

        List<ItemStack> pages = ageData.getPages();
        List<IAgeSymbol> symbols = new ArrayList<>();

        for (ItemStack page : pages) {
            net.minecraft.resources.ResourceLocation symbolId = Page.getSymbol(page);
            if (symbolId != null) {
                IAgeSymbol symbol = SymbolRegistry.get(symbolId);
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
        }

        Mystcraft.LOGGER.info("Reconstructing director for age {} from {} pages ({} symbols)",
                ageUID, pages.size(), symbols.size());

        AgeBuilder builder = new AgeBuilder(symbols, seed);
        this.director = builder.build();
        this.director.setInstability(ageData.getInstability());

        // Update the biome source if we have an AgeBiomeSource
        if (biomeSource instanceof AgeBiomeSource ageBiomeSource) {
            IBiomeController biomeController = director.getBiomeControllerImpl();
            if (biomeController != null) {
                ageBiomeSource.setBiomeController(biomeController);
            } else if (!director.getBiomes().isEmpty()) {
                IBiomeController defaultController = new BiomeControllerNoise(
                        director.getBiomes(), seed, BiomeControllerNoise.Scale.MEDIUM);
                director.registerInterface(defaultController);
                ageBiomeSource.setBiomeController(defaultController);
            }
        }

        Mystcraft.LOGGER.info("Director reconstructed successfully for age {}", ageUID);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        ensureVanillaDelegate();

        // Delegate carving to vanilla for normal terrain
        if (vanillaDelegate != null) {
            vanillaDelegate.applyCarvers(level, seed, vanillaRandomState, biomeManager,
                    structureManager, chunk, step);
        }

        // Apply any Mystcraft terrain alterations that work during carving
        // (most alterations apply during fillFromNoise instead)
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        int count = buildSurfaceCount.incrementAndGet();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} buildSurface #{}: chunk [{}, {}] on thread: {} | delegate={}",
                    ageUID, count, chunkX, chunkZ, Thread.currentThread().getName(),
                    vanillaDelegate != null ? "ready" : (delegateInitialized ? "null" : "not-init"));
        }

        ensureVanillaDelegate();

        // Delegate surface building to vanilla - this applies proper biome surfaces
        // (grass, dirt, sand, etc.) on top of stone
        if (vanillaDelegate != null) {
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} buildSurface #{}: delegating to vanilla [thread: {}]",
                        ageUID, count, Thread.currentThread().getName());
            }
            vanillaDelegate.buildSurface(level, structureManager, vanillaRandomState, chunk);
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} buildSurface #{}: vanilla COMPLETE [thread: {}]",
                        ageUID, count, Thread.currentThread().getName());
            }
        }

        // AFTER vanilla applies biome surfaces, replace remaining stone/water with
        // symbol-specified blocks. This order is critical: vanilla's surface builder
        // needs to find stone to know where to place grass/dirt. If we replaced stone
        // before surface building, surfaces would never be applied.
        if (director != null) {
            applyBlockReplacements(chunk);
            applySurfaceModifications(chunk);
        }

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} buildSurface #{}: DONE [{}, {}]", ageUID, count, chunkX, chunkZ);
        }
    }

    /**
     * Applies Mystcraft surface modifications after vanilla surface building.
     * This is where symbols like "Sand Surface" or "Snow Surface" take effect.
     */
    private void applySurfaceModifications(ChunkAccess chunk) {
        BlockState surfaceOverride = director.getSurfaceBlock();
        BlockState subsurfaceOverride = director.getSubsurfaceBlock();

        if (surfaceOverride == null && subsurfaceOverride == null) {
            return; // No modifications needed
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Find surface
                for (int y = maxY - 1; y >= minY; y--) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.isAir() || state.getBlock() == Blocks.WATER) {
                        continue;
                    }

                    // Found surface - check if it's a replaceable surface block
                    if (isSurfaceBlock(state)) {
                        if (surfaceOverride != null) {
                            chunk.setBlockState(pos, surfaceOverride, false);
                        }

                        // Replace subsurface (next 3 blocks down)
                        if (subsurfaceOverride != null) {
                            for (int d = 1; d <= 3 && y - d >= minY; d++) {
                                pos.set(x, y - d, z);
                                BlockState sub = chunk.getBlockState(pos);
                                if (isSubsurfaceBlock(sub)) {
                                    chunk.setBlockState(pos, subsurfaceOverride, false);
                                }
                            }
                        }
                    }
                    break; // Move to next column
                }
            }
        }
    }

    private boolean isSurfaceBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) ||
               state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) ||
               state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MYCELIUM) ||
               state.is(Blocks.PODZOL) || state.is(Blocks.MUD);
    }

    private boolean isSubsurfaceBlock(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.SAND) ||
               state.is(Blocks.GRAVEL) || state.is(Blocks.SANDSTONE) ||
               state.is(Blocks.MUD);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        ensureVanillaDelegate();
        if (vanillaDelegate != null) {
            vanillaDelegate.spawnOriginalMobs(level);
        }
    }

    /**
     * Applies biome decoration including vanilla features AND Mystcraft populators.
     * This is the correct place for trees, ores, dungeons, etc.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        int count = biomeDecorationCount.incrementAndGet();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: chunk [{}, {}] on thread: {}",
                    ageUID, count, chunkX, chunkZ, Thread.currentThread().getName());
        }

        ensureVanillaDelegate();

        // First, let vanilla do its biome decoration (trees, flowers, ores, etc.)
        if (vanillaDelegate != null) {
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: delegating vanilla decoration [thread: {}]",
                        ageUID, count, Thread.currentThread().getName());
            }
            vanillaDelegate.applyBiomeDecoration(level, chunk, structureManager);
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: vanilla decoration COMPLETE [thread: {}]",
                        ageUID, count, Thread.currentThread().getName());
            }
        }

        // Then apply Mystcraft populators ON TOP of vanilla
        if (director == null) {
            return;
        }

        List<IPopulate> populators = director.getPopulateFunctions();
        if (populators.isEmpty()) {
            return;
        }

        BlockPos chunkPos = new BlockPos(chunkX * 16, 0, chunkZ * 16);

        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
        RandomSource random = RandomSource.create(chunkSeed);

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: applying {} Mystcraft populators [{}, {}] [thread: {}]",
                    ageUID, count, populators.size(), chunkX, chunkZ, Thread.currentThread().getName());
        }

        for (IPopulate populator : populators) {
            try {
                if (count <= DEBUG_CHUNK_LIMIT) {
                    Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: running populator '{}' [{}, {}]",
                            ageUID, count, populator.getIdentifier(), chunkX, chunkZ);
                }
                populator.populate(level, random, chunkPos);
            } catch (Exception e) {
                Mystcraft.LOGGER.error("[Population] Error in populator {} on chunk [{}, {}]: {}",
                        populator.getIdentifier(), chunkX, chunkZ, e.getMessage(), e);
            }
        }

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: ALL DONE [{}, {}]",
                    ageUID, count, chunkX, chunkZ);
        }
    }

    @Override
    public int getGenDepth() {
        ensureVanillaDelegate();
        if (vanillaDelegate != null) {
            return vanillaDelegate.getGenDepth();
        }
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState, StructureManager structureManager,
                                                        ChunkAccess chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int count = fillFromNoiseCount.incrementAndGet();
        String threadName = Thread.currentThread().getName();

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: chunk [{}, {}] on thread: {} | delegate={}, director={}",
                    ageUID, count, chunkX, chunkZ, threadName,
                    vanillaDelegate != null ? "ready" : (delegateInitialized ? "null(failed)" : "not-init"),
                    director != null ? "yes" : "no");
        }

        ensureVanillaDelegate();

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: post-init delegate={} [thread: {}]",
                    ageUID, count,
                    vanillaDelegate != null ? "ready" : "null",
                    threadName);
        }

        // For normal/amplified terrain, delegate to vanilla with proper RandomState
        if (vanillaDelegate != null) {
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: DELEGATING to vanilla (type: {}) [thread: {}]",
                        ageUID, count, terrainType, threadName);
            }
            return vanillaDelegate.fillFromNoise(executor, blender, vanillaRandomState, structureManager, chunk)
                    .thenApply(filledChunk -> {
                        if (count <= DEBUG_CHUNK_LIMIT) {
                            Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: vanilla COMPLETE for [{}, {}] [thread: {}]",
                                    ageUID, count, chunkX, chunkZ, Thread.currentThread().getName());
                        }
                        // Apply Mystcraft terrain alterations AFTER vanilla fills the chunk
                        // Note: block replacements (terrain/sea block symbols) happen in buildSurface
                        // AFTER vanilla applies biome surfaces, so surface builder can find stone
                        applyTerrainAlterations(filledChunk, randomState);
                        if (count <= DEBUG_CHUNK_LIMIT) {
                            Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: alterations COMPLETE for [{}, {}]",
                                    ageUID, count, chunkX, chunkZ);
                        }
                        return filledChunk;
                    });
        }

        // For special terrain types (void, flat), use custom generation
        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: CUSTOM generation (type: {}) for [{}, {}] [thread: {}]",
                    ageUID, count, terrainType, chunkX, chunkZ, threadName);
        }
        return CompletableFuture.supplyAsync(() -> {
            generateSpecialTerrain(chunk, randomState);
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: custom COMPLETE for [{}, {}] [thread: {}]",
                        ageUID, count, chunkX, chunkZ, Thread.currentThread().getName());
            }
            return chunk;
        }, executor);
    }

    /**
     * Applies Mystcraft terrain alterations after vanilla terrain generation.
     * This includes caves, ravines, floating islands, tendrils, etc.
     */
    private void applyTerrainAlterations(ChunkAccess chunk, RandomState randomState) {
        if (director == null) {
            return;
        }

        List<ITerrainAlteration> alterations = director.getTerrainAlterations();
        if (alterations.isEmpty()) {
            return;
        }

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
        RandomSource random = RandomSource.create(chunkSeed);

        for (ITerrainAlteration alteration : alterations) {
            alteration.alterTerrain(null, chunkX, chunkZ, chunk, random);
        }
    }

    /**
     * Replaces vanilla's default stone and water blocks with symbol-specified blocks.
     * This is how terrain block symbols (granite, netherrack, etc.) and sea block symbols
     * (lava, packed ice, etc.) take effect on normal/amplified terrain.
     */
    private void applyBlockReplacements(ChunkAccess chunk) {
        if (director == null) {
            return;
        }

        BlockState terrainBlock = director.getTerrainBlock();
        BlockState seaBlock = director.getSeaBlock();

        // Skip if using default blocks (stone and water)
        boolean replaceStone = terrainBlock != null && !terrainBlock.is(Blocks.STONE);
        boolean replaceWater = seaBlock != null && !seaBlock.is(Blocks.WATER);

        if (!replaceStone && !replaceWater) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (replaceStone && state.is(Blocks.STONE)) {
                        chunk.setBlockState(pos, terrainBlock, false);
                    } else if (replaceWater && state.is(Blocks.WATER)) {
                        chunk.setBlockState(pos, seaBlock, false);
                    }
                }
            }
        }
    }

    /**
     * Generates special terrain types (void, flat) that don't use vanilla generation.
     */
    private void generateSpecialTerrain(ChunkAccess chunk, RandomState randomState) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
        RandomSource random = RandomSource.create(chunkSeed);

        switch (terrainType) {
            case "void" -> generateVoidTerrain(chunk, random);
            case "flat" -> generateFlatTerrain(chunk, random);
            default -> {
                // Fallback to flat if unknown special type
                Mystcraft.LOGGER.warn("Unknown terrain type '{}', using flat", terrainType);
                generateFlatTerrain(chunk, random);
            }
        }

        // Apply terrain alterations even for special terrain
        applyTerrainAlterations(chunk, randomState);
    }

    private void generateVoidTerrain(ChunkAccess chunk, RandomSource random) {
        // Void terrain - completely empty, no bedrock.
        // Only generate a stone spawn platform at (0,0) in the spawn chunk
        // so the player has somewhere to stand.
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        if (chunkX == 0 && chunkZ == 0) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            BlockState platformBlock = Blocks.STONE.defaultBlockState();
            int platformY = 64;

            // 5x5 platform centered at (8, 64, 8) within the chunk
            for (int x = 6; x <= 10; x++) {
                for (int z = 6; z <= 10; z++) {
                    pos.set(x, platformY, z);
                    chunk.setBlockState(pos, platformBlock, false);
                }
            }
        }
        // All other chunks are pure void (air)
    }

    private void generateFlatTerrain(ChunkAccess chunk, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();

        BlockState terrainBlock = director != null && director.getTerrainBlock() != null
                ? director.getTerrainBlock()
                : Blocks.STONE.defaultBlockState();

        // Flat terrain: bedrock at bottom, then terrain block up to ground level
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < groundLevel; y++) {
                    pos.set(x, y, z);
                    if (y == minY) {
                        chunk.setBlockState(pos, bedrockBlock, false);
                    } else {
                        chunk.setBlockState(pos, terrainBlock, false);
                    }
                }
            }
        }
    }

    @Override
    public int getSeaLevel() {
        ensureVanillaDelegate();
        if (vanillaDelegate != null) {
            return vanillaDelegate.getSeaLevel();
        }
        return seaLevelValue;
    }

    @Override
    public int getMinY() {
        ensureVanillaDelegate();
        if (vanillaDelegate != null) {
            return vanillaDelegate.getMinY();
        }
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        if (!delegateInitialized) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} getBaseHeight called BEFORE delegate init at ({},{}) on thread: {}",
                    ageUID, x, z, Thread.currentThread().getName());
        }
        ensureVanillaDelegate();
        if (vanillaDelegate != null) {
            return vanillaDelegate.getBaseHeight(x, z, type, level, vanillaRandomState);
        }

        // Fallback for special terrain
        if ("void".equals(terrainType)) {
            return level.getMinBuildHeight();
        }
        return groundLevel;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        ensureVanillaDelegate();
        if (vanillaDelegate != null) {
            return vanillaDelegate.getBaseColumn(x, z, level, vanillaRandomState);
        }

        // Fallback for special terrain
        BlockState[] states = new BlockState[level.getHeight()];
        int minY = level.getMinBuildHeight();

        for (int i = 0; i < states.length; i++) {
            int y = minY + i;
            if (y == minY) {
                states[i] = bedrockBlock;
            } else if ("void".equals(terrainType)) {
                states[i] = Blocks.AIR.defaultBlockState();
            } else if (y < groundLevel) {
                states[i] = Blocks.STONE.defaultBlockState();
            } else {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Mystcraft Age - Terrain: " + terrainType + (vanillaDelegate != null ? " (vanilla delegate)" : ""));
        info.add("Ground Level: " + groundLevel + ", Sea Level: " + seaLevelValue);
        if (director != null) {
            info.add("Alterations: " + director.getTerrainAlterations().size());
            info.add("Populators: " + director.getPopulateFunctions().size());
        }
    }

    public AgeDirectorImpl getDirector() {
        return director;
    }

    public int getAgeUID() {
        return ageUID;
    }

    public boolean needsDirectorReconstruction() {
        return director == null && ageUID > 0;
    }
}
