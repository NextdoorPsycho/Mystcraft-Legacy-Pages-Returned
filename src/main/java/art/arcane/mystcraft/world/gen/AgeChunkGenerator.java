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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Chunk generator for Mystcraft Ages that WRAPS vanilla's NoiseBasedChunkGenerator.
 *
 * For normal/amplified terrain: Delegates to vanilla's terrain generation, then applies
 * Mystcraft alterations and populators on top. This allows symbols to MODULATE vanilla
 * terrain rather than replace it entirely.
 *
 * For special terrain (void, flat): Uses custom lightweight generators.
 *
 * This matches how legacy Mystcraft worked - most symbols modified vanilla generation
 * rather than replacing it.
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
    private NoiseBasedChunkGenerator vanillaDelegate;
    private boolean delegateInitialized = false;

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
     */
    private boolean usesVanillaDelegate() {
        return "normal".equals(terrainType) || "amplified".equals(terrainType) || terrainType == null;
    }

    /**
     * Initializes the vanilla delegate generator lazily.
     * This gets the overworld's NoiseGeneratorSettings and creates a NoiseBasedChunkGenerator.
     */
    private void ensureVanillaDelegate() {
        if (delegateInitialized) {
            return;
        }
        delegateInitialized = true;

        if (!usesVanillaDelegate()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            Mystcraft.LOGGER.warn("Cannot initialize vanilla delegate: server not available");
            return;
        }

        try {
            // Get the overworld's NoiseGeneratorSettings
            Holder<NoiseGeneratorSettings> noiseSettings;
            if ("amplified".equals(terrainType)) {
                noiseSettings = server.registryAccess()
                        .registryOrThrow(Registries.NOISE_SETTINGS)
                        .getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED);
            } else {
                noiseSettings = server.registryAccess()
                        .registryOrThrow(Registries.NOISE_SETTINGS)
                        .getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
            }

            // Create vanilla generator with our biome source
            vanillaDelegate = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);

            Mystcraft.LOGGER.info("Age {} initialized vanilla terrain delegate (type: {})",
                    ageUID, terrainType);
        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to initialize vanilla delegate for age {}", ageUID, e);
        }
    }

    /**
     * Reconstructs the director from saved AgeData.
     */
    public void reconstructDirectorFromAgeData(ServerLevel level) {
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
            vanillaDelegate.applyCarvers(level, seed, randomState, biomeManager,
                    structureManager, chunk, step);
        }

        // Apply any Mystcraft terrain alterations that work during carving
        // (most alterations apply during fillFromNoise instead)
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        ensureVanillaDelegate();

        // Delegate surface building to vanilla - this applies proper biome surfaces
        if (vanillaDelegate != null) {
            vanillaDelegate.buildSurface(level, structureManager, randomState, chunk);
        }

        // Mystcraft surface type symbols can modify the surface AFTER vanilla builds it
        // This allows symbols like "Sand Surface" to replace grass with sand
        if (director != null) {
            applySurfaceModifications(chunk);
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
        ensureVanillaDelegate();

        // First, let vanilla do its biome decoration (trees, flowers, ores, etc.)
        if (vanillaDelegate != null) {
            vanillaDelegate.applyBiomeDecoration(level, chunk, structureManager);
        }

        // Then apply Mystcraft populators ON TOP of vanilla
        // This is how "Big Trees" makes trees bigger, "Dense Ores" adds more ores, etc.
        if (director == null) {
            return;
        }

        List<IPopulate> populators = director.getPopulateFunctions();
        if (populators.isEmpty()) {
            return;
        }

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        BlockPos chunkPos = new BlockPos(chunkX * 16, 0, chunkZ * 16);

        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
        RandomSource random = RandomSource.create(chunkSeed);

        Mystcraft.LOGGER.trace("[Population] Applying {} Mystcraft populators to chunk [{}, {}]",
                populators.size(), chunkX, chunkZ);

        for (IPopulate populator : populators) {
            try {
                populator.populate(level, random, chunkPos);
            } catch (Exception e) {
                Mystcraft.LOGGER.error("[Population] Error in populator {} on chunk [{}, {}]: {}",
                        populator.getIdentifier(), chunkX, chunkZ, e.getMessage(), e);
            }
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
        ensureVanillaDelegate();

        // For normal/amplified terrain, delegate to vanilla
        if (vanillaDelegate != null) {
            return vanillaDelegate.fillFromNoise(executor, blender, randomState, structureManager, chunk)
                    .thenApply(filledChunk -> {
                        // Apply Mystcraft terrain alterations AFTER vanilla fills the chunk
                        applyTerrainAlterations(filledChunk, randomState);
                        return filledChunk;
                    });
        }

        // For special terrain types (void, flat), use custom generation
        return CompletableFuture.supplyAsync(() -> {
            generateSpecialTerrain(chunk, randomState);
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
        // Void terrain - just bedrock at bottom, everything else is air
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                pos.set(x, minY, z);
                chunk.setBlockState(pos, bedrockBlock, false);
            }
        }
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
        ensureVanillaDelegate();
        if (vanillaDelegate != null) {
            return vanillaDelegate.getBaseHeight(x, z, type, level, randomState);
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
            return vanillaDelegate.getBaseColumn(x, z, level, randomState);
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
