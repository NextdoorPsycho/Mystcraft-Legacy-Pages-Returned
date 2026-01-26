package art.arcane.mystcraft.world.gen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.world.logic.IChunkProviderFinalization;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import art.arcane.mystcraft.api.world.logic.ITerrainGenerator;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorFlat;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorNormal;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom chunk generator for Mystcraft Ages.
 * Uses registered terrain generators, biome controllers, and terrain alterations
 * from the AgeDirector to generate world terrain following the legacy Mystcraft
 * world generation pipeline.
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

    // Director with registered interfaces
    private AgeDirectorImpl director;

    // Default block states (used when no director is set)
    private final BlockState stoneBlock = Blocks.STONE.defaultBlockState();
    private final BlockState waterBlock = Blocks.WATER.defaultBlockState();
    private final BlockState bedrockBlock = Blocks.BEDROCK.defaultBlockState();
    private final BlockState airBlock = Blocks.AIR.defaultBlockState();

    /**
     * Creates a chunk generator from codec deserialization.
     */
    public AgeChunkGenerator(BiomeSource biomeSource, String terrainType, int groundLevel, int seaLevel, boolean hasSea, long seed, int ageUID) {
        super(biomeSource);
        this.terrainType = terrainType;
        this.groundLevel = groundLevel;
        this.seaLevelValue = seaLevel;
        this.hasSea = hasSea;
        this.seed = seed;
        this.ageUID = ageUID;
        // Director will be reconstructed on first terrain generation if needed
    }

    /**
     * Creates a chunk generator from an AgeDirector configuration.
     * This is the preferred constructor that uses registered interfaces.
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
     * Creates a chunk generator from an AgeDirector configuration (legacy method).
     */
    public static AgeChunkGenerator fromDirector(AgeDirectorImpl director, BiomeSource biomeSource, int ageUID) {
        return new AgeChunkGenerator(director, biomeSource, director.getSeed(), ageUID);
    }

    /**
     * Sets the director for this generator.
     * Used when the generator is deserialized and needs to be reconnected to its director.
     */
    public void setDirector(AgeDirectorImpl director) {
        this.director = director;
    }

    /**
     * Reconstructs the director from saved AgeData.
     * Called when the generator is deserialized and needs its director rebuilt.
     */
    public void reconstructDirectorFromAgeData(ServerLevel level) {
        if (director != null) {
            return; // Already have a director
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

        // Get pages from AgeData and extract symbols
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

        // Rebuild director using AgeBuilder
        AgeBuilder builder = new AgeBuilder(symbols, seed);
        this.director = builder.build();

        // Copy back any settings that might have been modified
        this.director.setInstability(ageData.getInstability());

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
        // Cave and ravine carving is now handled by ITerrainAlteration implementations
        // that are applied during fillFromNoise
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        // Surface building is handled by the terrain generators themselves
        // or by biome-specific surface rules
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Default mob spawning
    }

    /**
     * Applies biome decoration including Mystcraft populators.
     * This is the correct place to add ores, trees, dungeons, and other features
     * in 1.20.2+ - during the FEATURES chunk status, NOT after chunk loading.
     *
     * WorldGenLevel provides a limited view of the world that is safe for
     * generation - setBlock() calls here don't trigger lighting updates or
     * cascade chunk loads like they would on a fully-loaded chunk.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        // Let vanilla handle biome decoration first (trees, flowers, etc. from biomes)
        // We skip this for now to avoid conflicts - Mystcraft controls all decoration
        // super.applyBiomeDecoration(level, chunk, structureManager);

        // Now apply Mystcraft populators
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

        // Create random source for this chunk (matching legacy behavior)
        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
        RandomSource random = RandomSource.create(chunkSeed);

        Mystcraft.LOGGER.debug("[Population] Applying {} populators to chunk [{}, {}]",
                populators.size(), chunkX, chunkZ);

        // Call all registered population functions
        for (IPopulate populator : populators) {
            String id = populator.getIdentifier();
            try {
                populator.populate(level, random, chunkPos);
            } catch (Exception e) {
                Mystcraft.LOGGER.error("[Population] Error in populator {} on chunk [{}, {}]: {}",
                        id, chunkX, chunkZ, e.getMessage(), e);
            }
        }
    }

    @Override
    public int getGenDepth() {
        return 384; // Default world height
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
            RandomSource random = RandomSource.create(chunkSeed);

            // 1. Base terrain generation using registered ITerrainGenerator
            generateTerrain(chunk, chunkX, chunkZ, random);

            // 2. Apply terrain alterations (caves, ravines, floating islands, etc.)
            applyTerrainAlterations(chunk, chunkX, chunkZ, random);

            // 3. Apply chunk finalizers
            applyChunkFinalizers(chunk, chunkX, chunkZ);

            return chunk;
        }, executor);
    }

    /**
     * Generates base terrain using the registered terrain generator.
     */
    private void generateTerrain(ChunkAccess chunk, int chunkX, int chunkZ, RandomSource random) {
        if (director != null) {
            ITerrainGenerator generator = director.getTerrainGenerator();
            if (generator != null) {
                // Pass BiomeSource to terrain generator if it supports it
                if (generator instanceof TerrainGeneratorNormal normalGen) {
                    normalGen.setBiomeSource(biomeSource);
                }
                // Use the registered terrain generator
                generator.generateTerrain(chunkX, chunkZ, chunk, random);
                return;
            }
        }

        // Fallback: create a default terrain generator based on terrain type
        generateFallbackTerrain(chunk, chunkX, chunkZ, random);
    }

    /**
     * Fallback terrain generation when no generator is registered.
     */
    private void generateFallbackTerrain(ChunkAccess chunk, int chunkX, int chunkZ, RandomSource random) {
        // Create a temporary director for the fallback generator
        AgeDirectorImpl tempDirector = new AgeDirectorImpl(seed);
        tempDirector.setAverageGroundLevel(groundLevel);
        tempDirector.setSeaLevel(seaLevelValue);
        tempDirector.setHasSea(hasSea);

        ITerrainGenerator fallbackGen;
        switch (terrainType) {
            case "flat":
                fallbackGen = new TerrainGeneratorFlat(tempDirector, seed);
                break;
            case "void":
                // Void terrain - just bedrock at bottom
                generateVoidFallback(chunk);
                return;
            default:
                // Normal terrain
                TerrainGeneratorNormal normalGen = new TerrainGeneratorNormal(tempDirector, seed, "amplified".equals(terrainType));
                normalGen.setBiomeSource(biomeSource);
                fallbackGen = normalGen;
                break;
        }

        fallbackGen.generateTerrain(chunkX, chunkZ, chunk, random);
    }

    /**
     * Simple void terrain fallback.
     */
    private void generateVoidFallback(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                pos.set(x, minY, z);
                chunk.setBlockState(pos, bedrockBlock, false);
            }
        }
    }

    /**
     * Applies all registered terrain alterations (caves, ravines, etc.) in priority order.
     */
    private void applyTerrainAlterations(ChunkAccess chunk, int chunkX, int chunkZ, RandomSource random) {
        if (director == null) {
            return;
        }

        List<ITerrainAlteration> alterations = director.getTerrainAlterations();
        if (alterations.isEmpty()) {
            return;
        }

        // Get server level if available (may be null during initial generation)
        ServerLevel serverLevel = null;

        // Apply each alteration in order (already sorted by priority in AgeDirectorImpl)
        for (ITerrainAlteration alteration : alterations) {
            alteration.alterTerrain(serverLevel, chunkX, chunkZ, chunk, random);
        }
    }

    /**
     * Applies all registered chunk finalizers.
     */
    private void applyChunkFinalizers(ChunkAccess chunk, int chunkX, int chunkZ) {
        if (director == null) {
            return;
        }

        List<IChunkProviderFinalization> finalizers = director.getChunkFinalizers();
        for (IChunkProviderFinalization finalizer : finalizers) {
            finalizer.finalizeChunk(chunk, chunkX, chunkZ);
        }
    }

    @Override
    public int getSeaLevel() {
        return seaLevelValue;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        // Use the terrain type to estimate base height
        if ("void".equals(terrainType)) {
            return level.getMinBuildHeight();
        }
        if ("flat".equals(terrainType)) {
            return groundLevel;
        }
        // For noise-based terrain, return ground level as approximation
        return groundLevel;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        int height = getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        BlockState[] states = new BlockState[level.getHeight()];

        int minY = level.getMinBuildHeight();
        for (int i = 0; i < states.length; i++) {
            int y = minY + i;
            if (y == minY) {
                states[i] = bedrockBlock;
            } else if (y <= height) {
                states[i] = stoneBlock;
            } else if (hasSea && y <= seaLevelValue) {
                states[i] = waterBlock;
            } else {
                states[i] = airBlock;
            }
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Mystcraft Age - Terrain: " + terrainType);
        info.add("Ground Level: " + groundLevel + ", Sea Level: " + seaLevelValue);
        if (director != null) {
            ITerrainGenerator gen = director.getTerrainGenerator();
            if (gen != null) {
                info.add("Generator: " + gen.getType());
            }
            info.add("Alterations: " + director.getTerrainAlterations().size());
        }
    }

    /**
     * Gets the AgeDirector for this generator.
     */
    public AgeDirectorImpl getDirector() {
        return director;
    }

    /**
     * Gets the age UID for this generator.
     */
    public int getAgeUID() {
        return ageUID;
    }

    /**
     * Checks if this generator needs its director reconstructed.
     */
    public boolean needsDirectorReconstruction() {
        return director == null && ageUID > 0;
    }
}
