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
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.item.ItemStack;

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
                    Codec.INT.fieldOf("age_uid").forGetter(gen -> gen.ageUID),
                    Codec.STRING.optionalFieldOf("secondary_terrain_type", "none").forGetter(gen -> gen.secondaryTerrainType),
                    Codec.STRING.optionalFieldOf("terrain_mix_mode", "none").forGetter(gen -> gen.terrainMixMode)
            ).apply(instance, AgeChunkGenerator::new)
    );

    // Configuration stored for serialization
    private final String terrainType;
    private final String secondaryTerrainType;
    private final String terrainMixMode;
    private final int groundLevel;
    private final int seaLevelValue;
    private final boolean hasSea;
    private final long seed;
    private final int ageUID;

    // Director with registered interfaces (symbols register here)
    private AgeDirectorImpl director;

    // Vanilla generator delegates - used for normal/amplified terrain
    // volatile for safe double-check locking (no full synchronized needed)
    private volatile NoiseBasedChunkGenerator vanillaDelegate;
    private volatile RandomState vanillaRandomState;
    private volatile NoiseBasedChunkGenerator vanillaDelegate2;
    private volatile RandomState vanillaRandomState2;
    private volatile boolean delegateInitialized = false;

    // Debug counters for first N chunks
    private final AtomicInteger fillFromNoiseCount = new AtomicInteger(0);
    private final AtomicInteger buildSurfaceCount = new AtomicInteger(0);
    private final AtomicInteger biomeDecorationCount = new AtomicInteger(0);
    private static final int DEBUG_CHUNK_LIMIT = 50;

    // Default block states
    private final BlockState bedrockBlock = Blocks.BEDROCK.defaultBlockState();

    /**
     * Creates a chunk generator from codec deserialization.
     */
    public AgeChunkGenerator(BiomeSource biomeSource, String terrainType, int groundLevel,
                             int seaLevel, boolean hasSea, long seed, int ageUID,
                             String secondaryTerrainType, String terrainMixMode) {
        super(biomeSource);
        this.terrainType = terrainType;
        this.secondaryTerrainType = secondaryTerrainType != null ? secondaryTerrainType : "none";
        this.terrainMixMode = terrainMixMode != null ? terrainMixMode : "none";
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
        this.secondaryTerrainType = director.getSecondaryTerrainType();
        this.terrainMixMode = director.getTerrainMixMode();
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
     * Gets the terrain type string (e.g. "normal", "nether", "end", "void", "flat", "cave", "skylands").
     */
    public String getTerrainType() {
        return terrainType;
    }

    private boolean usesVanillaDelegate() {
        return isVanillaDelegateType(terrainType);
    }

    private static boolean isNetherOrCaveTerrain(String type) {
        return "nether".equals(type) || "cave".equals(type);
    }

    private static boolean isVanillaDelegateType(String type) {
        return "normal".equals(type) || "amplified".equals(type)
                || "nether".equals(type) || "end".equals(type)
                || "cave".equals(type) || "skylands".equals(type)
                || type == null;
    }

    private boolean hasMixMode() {
        return !"none".equals(terrainMixMode) && !"none".equals(secondaryTerrainType);
    }

    /**
     * Determines which terrain type to use for a given chunk based on the mix mode.
     * Returns true if the primary terrain should be used, false for secondary.
     */
    private boolean usePrimaryTerrain(int chunkX, int chunkZ) {
        if (!hasMixMode()) {
            return true;
        }
        switch (terrainMixMode) {
            case "checkerboard" -> {
                return ((chunkX / 64) + (chunkZ / 64)) % 2 == 0;
            }
            case "noise" -> {
                // Simple noise sampling using seed-based hash
                double nx = chunkX * 0.02;
                double nz = chunkZ * 0.02;
                double noiseVal = Math.sin(nx * 12.9898 + nz * 78.233 + seed * 0.0001) * 43758.5453;
                noiseVal = noiseVal - Math.floor(noiseVal); // Fractional part, 0-1
                return noiseVal < 0.5;
            }
            case "stripes" -> {
                return (Math.floorDiv(chunkX, 16) % 2) == 0;
            }
            default -> {
                return true;
            }
        }
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

            MinecraftServer server = Mystcraft.getCurrentServer();
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
                } else if ("nether".equals(terrainType) || "cave".equals(terrainType)) {
                    noiseSettings = createStretchedNetherSettings(noiseSettingsRegistry);
                } else if ("end".equals(terrainType)) {
                    noiseSettings = createStretchedSettings(
                            noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.END).value());
                } else {
                    // "normal", "skylands", and fallback all use overworld noise settings
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
                        ("nether".equals(terrainType) || "cave".equals(terrainType)) ? "NETHER" :
                        "end".equals(terrainType) ? "END" : "OVERWORLD");

                // Initialize secondary delegate for mixed terrain modes (independent try-catch
                // so primary delegate failure doesn't block generation entirely)
                if (hasMixMode() && isVanillaDelegateType(secondaryTerrainType)) {
                    try {
                        Holder<NoiseGeneratorSettings> secondarySettings;
                        if ("amplified".equals(secondaryTerrainType)) {
                            secondarySettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED);
                        } else if ("nether".equals(secondaryTerrainType) || "cave".equals(secondaryTerrainType)) {
                            secondarySettings = createStretchedNetherSettings(noiseSettingsRegistry);
                        } else if ("end".equals(secondaryTerrainType)) {
                            secondarySettings = createStretchedSettings(
                                    noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.END).value());
                        } else {
                            secondarySettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
                        }
                        vanillaDelegate2 = new NoiseBasedChunkGenerator(biomeSource, secondarySettings);
                        vanillaRandomState2 = RandomState.create(
                                secondarySettings.value(),
                                server.registryAccess().lookupOrThrow(Registries.NOISE),
                                seed
                        );
                        Mystcraft.LOGGER.info("[ChunkGen] Age {} initialized SECONDARY vanilla terrain delegate (type: {})",
                                ageUID, secondaryTerrainType);
                    } catch (Exception e) {
                        Mystcraft.LOGGER.error("[ChunkGen] Age {} failed to initialize secondary delegate (type: {}), falling back to primary only",
                                ageUID, secondaryTerrainType, e);
                    }
                }
            } catch (Exception e) {
                Mystcraft.LOGGER.error("Failed to initialize vanilla delegate for age {}", ageUID, e);
            }

            // Always mark as initialized so we don't block all chunk gen threads
            // retrying forever if something went wrong
            delegateInitialized = true;
        }
    }

    /**
     * Gets the appropriate vanilla delegate for the given chunk position,
     * considering the terrain mix mode.
     */
    private NoiseBasedChunkGenerator getDelegateForChunk(int chunkX, int chunkZ) {
        if (hasMixMode() && !usePrimaryTerrain(chunkX, chunkZ) && vanillaDelegate2 != null) {
            return vanillaDelegate2;
        }
        return vanillaDelegate;
    }

    /**
     * Gets the appropriate RandomState for the given chunk position,
     * considering the terrain mix mode.
     */
    private RandomState getRandomStateForChunk(int chunkX, int chunkZ) {
        if (hasMixMode() && !usePrimaryTerrain(chunkX, chunkZ) && vanillaRandomState2 != null) {
            return vanillaRandomState2;
        }
        return vanillaRandomState;
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

        Mystcraft.LOGGER.debug("Reconstructing director for age {} from {} pages ({} symbols)",
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

        // Use vanillaRandomState (built from real noise settings) when available,
        // otherwise fall back to the parameter's randomState (which may be dummy for
        // non-NoiseBasedChunkGenerator generators).
        RandomState effectiveState = vanillaRandomState != null ? vanillaRandomState : randomState;

        // Run vanilla carvers via delegate. This ensures caves and ravines generate.
        if (vanillaDelegate != null) {
            vanillaDelegate.applyCarvers(level, seed, effectiveState, biomeManager,
                    structureManager, chunk, step);
        }
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
        NoiseBasedChunkGenerator surfaceDelegate = getDelegateForChunk(chunkX, chunkZ);
        RandomState surfaceRandomState = getRandomStateForChunk(chunkX, chunkZ);
        if (surfaceDelegate != null) {
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} buildSurface #{}: delegating to vanilla [thread: {}]",
                        ageUID, count, Thread.currentThread().getName());
            }
            surfaceDelegate.buildSurface(level, structureManager, surfaceRandomState, chunk);
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} buildSurface #{}: vanilla COMPLETE [thread: {}]",
                        ageUID, count, Thread.currentThread().getName());
            }
        }

        // Strip bedrock from all Mystcraft Age terrain. Ages should not have
        // vanilla bedrock barriers. Must happen after vanilla surface rules.
        stripBedrockBlocks(chunk);

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
               state.is(Blocks.PODZOL) || state.is(Blocks.MUD) ||
               state.is(Blocks.END_STONE) ||
               state.is(Blocks.NETHERRACK) || state.is(Blocks.SOUL_SAND) ||
               state.is(Blocks.SOUL_SOIL) || state.is(Blocks.CRIMSON_NYLIUM) ||
               state.is(Blocks.WARPED_NYLIUM);
    }

    private boolean isSubsurfaceBlock(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.SAND) ||
               state.is(Blocks.GRAVEL) || state.is(Blocks.SANDSTONE) ||
               state.is(Blocks.MUD) ||
               state.is(Blocks.END_STONE) || state.is(Blocks.NETHERRACK);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Spawn vanilla mobs via delegate for all terrain types
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

        // Run vanilla biome features (trees, flowers, ores, dungeons, etc.) via super.
        // The base ChunkGenerator.applyBiomeDecoration() uses our BiomeSource's feature
        // lists directly, so this works for ALL terrain types -- not just delegate types.
        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: running vanilla features [thread: {}]",
                    ageUID, count, Thread.currentThread().getName());
        }
        try {
            super.applyBiomeDecoration(level, chunk, structureManager);
        } catch (Exception e) {
            Mystcraft.LOGGER.error("[ChunkGen] Age {} applyBiomeDecoration #{}: vanilla features FAILED [{}, {}]: {}",
                    ageUID, count, chunkX, chunkZ, e.getMessage(), e);
        }
        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} applyBiomeDecoration #{}: vanilla features COMPLETE [thread: {}]",
                    ageUID, count, Thread.currentThread().getName());
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
            String popId = populator.getIdentifier();
            if (count <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} chunk [{}, {}] >> START populator '{}' [thread: {}]",
                        ageUID, chunkX, chunkZ, popId, Thread.currentThread().getName());
            }
            long startTime = System.nanoTime();
            try {
                populator.populate(level, random, chunkPos);
                if (count <= DEBUG_CHUNK_LIMIT) {
                    long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                    Mystcraft.LOGGER.info("[ChunkGen] Age {} chunk [{}, {}] << DONE  populator '{}' ({}ms)",
                            ageUID, chunkX, chunkZ, popId, elapsed);
                }
            } catch (Exception e) {
                long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                Mystcraft.LOGGER.error("[ChunkGen] Age {} chunk [{}, {}] !! ERROR populator '{}' after {}ms: {}",
                        ageUID, chunkX, chunkZ, popId, elapsed, e.getMessage(), e);
            }
        }

        if (count <= DEBUG_CHUNK_LIMIT) {
            Mystcraft.LOGGER.info("[ChunkGen] Age {} chunk [{}, {}] == ALL POPULATORS DONE ({} total) [thread: {}]",
                    ageUID, chunkX, chunkZ, populators.size(), Thread.currentThread().getName());
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
        // Use per-chunk delegate selection for mixed terrain modes
        NoiseBasedChunkGenerator chunkDelegate = getDelegateForChunk(chunkX, chunkZ);
        RandomState chunkRandomState = getRandomStateForChunk(chunkX, chunkZ);
        if (chunkDelegate != null) {
            if (count <= DEBUG_CHUNK_LIMIT) {
                boolean isPrimary = usePrimaryTerrain(chunkX, chunkZ);
                Mystcraft.LOGGER.info("[ChunkGen] Age {} fillFromNoise #{}: DELEGATING to vanilla (type: {}, primary: {}) [thread: {}]",
                        ageUID, count, isPrimary ? terrainType : secondaryTerrainType, isPrimary, threadName);
            }
            return chunkDelegate.fillFromNoise(executor, blender, chunkRandomState, structureManager, chunk)
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

        int altCount = fillFromNoiseCount.get(); // reuse fillFromNoise counter for gating
        for (ITerrainAlteration alteration : alterations) {
            String altName = alteration.getClass().getSimpleName();
            if (altCount <= DEBUG_CHUNK_LIMIT) {
                Mystcraft.LOGGER.info("[ChunkGen] Age {} chunk [{}, {}] >> START terrain alteration '{}' [thread: {}]",
                        ageUID, chunkX, chunkZ, altName, Thread.currentThread().getName());
            }
            long startTime = System.nanoTime();
            try {
                alteration.alterTerrain(null, chunkX, chunkZ, chunk, random);
                if (altCount <= DEBUG_CHUNK_LIMIT) {
                    long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                    Mystcraft.LOGGER.info("[ChunkGen] Age {} chunk [{}, {}] << DONE  terrain alteration '{}' ({}ms)",
                            ageUID, chunkX, chunkZ, altName, elapsed);
                }
            } catch (Exception e) {
                long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                Mystcraft.LOGGER.error("[ChunkGen] Age {} chunk [{}, {}] !! ERROR terrain alteration '{}' after {}ms: {}",
                        ageUID, chunkX, chunkZ, altName, elapsed, e.getMessage(), e);
            }
        }
    }

    /**
     * Replaces vanilla stone and water with symbol-specified blocks.
     * Skips replacement directly under surface blocks (grass, dirt, sand, etc.)
     * to prevent non-solid terrain blocks from collapsing surfaces.
     */
    private void applyBlockReplacements(ChunkAccess chunk) {
        if (director == null) {
            return;
        }

        BlockState terrainBlock = director.getTerrainBlock();
        BlockState seaBlock = director.getSeaBlock();

        boolean replaceStone = terrainBlock != null && !terrainBlock.is(Blocks.STONE);
        boolean replaceWater = seaBlock != null && !seaBlock.is(Blocks.WATER);

        if (!replaceStone && !replaceWater) {
            return;
        }

        // If the terrain block is not a full solid cube, we need to protect
        // the subsurface layers so that surface blocks (grass etc.) don't collapse.
        boolean terrainNeedsSurfaceProtection = replaceStone && !terrainBlock.canOcclude();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (replaceStone && isTerrainReplaceable(state)) {
                        if (terrainNeedsSurfaceProtection && isUnderSurface(chunk, x, y, z, maxY, checkPos)) {
                            continue;
                        }
                        chunk.setBlockState(pos, terrainBlock, false);
                    } else if (replaceWater && (state.is(Blocks.WATER) || state.is(Blocks.LAVA))) {
                        chunk.setBlockState(pos, seaBlock, false);
                    }
                }
            }
        }
    }

    /**
     * Checks if a block position is within the subsurface zone (top 5 blocks
     * below a surface block like grass, dirt, sand, etc.).
     */
    private boolean isUnderSurface(ChunkAccess chunk, int x, int y, int z, int maxY, BlockPos.MutableBlockPos checkPos) {
        for (int dy = 1; dy <= 5 && y + dy < maxY; dy++) {
            checkPos.set(x, y + dy, z);
            BlockState above = chunk.getBlockState(checkPos);
            if (above.isAir() || above.is(Blocks.WATER)) {
                return false;
            }
            if (isSurfaceBlock(above) || isSubsurfaceBlock(above)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a block is a dimension-native terrain block that should be replaced
     * when a terrain block symbol is specified. Covers overworld stone, end stone,
     * and nether terrain blocks.
     */
    private boolean isTerrainReplaceable(BlockState state) {
        return state.is(Blocks.STONE) ||
               state.is(Blocks.END_STONE) ||
               state.is(Blocks.NETHERRACK) ||
               state.is(Blocks.SOUL_SAND) ||
               state.is(Blocks.SOUL_SOIL) ||
               state.is(Blocks.BASALT);
    }

    /**
     * Creates noise settings based on vanilla NETHER but stretched to fill the full
     * dimension height (-64 to 320). Mystcraft nether Ages have no bedrock barriers
     * and terrain extends from min to max build height.
     */
    private Holder<NoiseGeneratorSettings> createStretchedNetherSettings(
            net.minecraft.core.Registry<NoiseGeneratorSettings> registry) {
        return createStretchedSettings(registry.getHolderOrThrow(NoiseGeneratorSettings.NETHER).value());
    }

    /**
     * Creates a copy of the given noise settings with the height stretched to
     * the full dimension range (-64 to 320) and noise size parameters varied
     * based on the Age seed. This gives each Age structurally different terrain
     * shapes while keeping the overall style (e.g. End islands vary in density
     * and size, Nether caves vary in proportion).
     */
    private Holder<NoiseGeneratorSettings> createStretchedSettings(NoiseGeneratorSettings vanilla) {
        int baseH = vanilla.noiseSettings().noiseSizeHorizontal();
        int baseV = vanilla.noiseSettings().noiseSizeVertical();

        // Vary noise size by +/- 25% based on the Age seed
        java.util.Random noiseRand = new java.util.Random(seed ^ 0x4E6F69736553697AL);
        int variedH = Math.max(1, baseH + noiseRand.nextInt(baseH / 2 + 1) - baseH / 4);
        int variedV = Math.max(1, baseV + noiseRand.nextInt(baseV / 2 + 1) - baseV / 4);

        Mystcraft.LOGGER.debug("[ChunkGen] Age {} noise variation: horizontal {} -> {}, vertical {} -> {}",
                ageUID, baseH, variedH, baseV, variedV);

        NoiseSettings stretched = NoiseSettings.create(-64, 384, variedH, variedV);
        NoiseGeneratorSettings mystcraftSettings = new NoiseGeneratorSettings(
                stretched,
                vanilla.defaultBlock(),
                vanilla.defaultFluid(),
                vanilla.noiseRouter(),
                vanilla.surfaceRule(),
                vanilla.spawnTarget(),
                vanilla.seaLevel(),
                vanilla.disableMobGeneration(),
                vanilla.aquifersEnabled(),
                vanilla.oreVeinsEnabled(),
                vanilla.useLegacyRandomSource()
        );
        return Holder.direct(mystcraftSettings);
    }

    /**
     * Strips all bedrock blocks from the chunk and replaces them with the terrain block.
     * Mystcraft Ages should not have bedrock barriers.
     */
    private void stripBedrockBlocks(ChunkAccess chunk) {
        BlockState replacement = Blocks.NETHERRACK.defaultBlockState();
        if (director != null && director.getTerrainBlock() != null) {
            replacement = director.getTerrainBlock();
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
                        chunk.setBlockState(pos, replacement, false);
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
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        NoiseBasedChunkGenerator heightDelegate = getDelegateForChunk(chunkX, chunkZ);
        RandomState heightRandomState = getRandomStateForChunk(chunkX, chunkZ);
        if (heightDelegate != null) {
            return heightDelegate.getBaseHeight(x, z, type, level, heightRandomState);
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
        if (hasMixMode()) {
            info.add("Mix Mode: " + terrainMixMode + ", Secondary: " + secondaryTerrainType);
        }
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
