package art.arcane.mystcraft.world.gen;

import art.arcane.mystcraft.world.AgeDirectorImpl;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom chunk generator for Mystcraft Ages.
 * Applies terrain configurations from the AgeDirector.
 */
public class AgeChunkGenerator extends ChunkGenerator {

    public static final Codec<AgeChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource),
                    Codec.STRING.fieldOf("terrain_type").forGetter(gen -> gen.terrainType),
                    Codec.INT.fieldOf("ground_level").forGetter(gen -> gen.groundLevel),
                    Codec.INT.fieldOf("sea_level").forGetter(gen -> gen.seaLevel),
                    Codec.BOOL.fieldOf("has_sea").forGetter(gen -> gen.hasSea)
            ).apply(instance, AgeChunkGenerator::new)
    );

    private final String terrainType;
    private final int groundLevel;
    private final int seaLevel;
    private final boolean hasSea;

    // Cached block states
    private final BlockState stoneBlock = Blocks.STONE.defaultBlockState();
    private final BlockState waterBlock = Blocks.WATER.defaultBlockState();
    private final BlockState bedrockBlock = Blocks.BEDROCK.defaultBlockState();
    private final BlockState airBlock = Blocks.AIR.defaultBlockState();
    private final BlockState endStoneBlock = Blocks.END_STONE.defaultBlockState();
    private final BlockState netherrackBlock = Blocks.NETHERRACK.defaultBlockState();

    public AgeChunkGenerator(BiomeSource biomeSource, String terrainType, int groundLevel, int seaLevel, boolean hasSea) {
        super(biomeSource);
        this.terrainType = terrainType;
        this.groundLevel = groundLevel;
        this.seaLevel = seaLevel;
        this.hasSea = hasSea;
    }

    /**
     * Creates a chunk generator from an AgeDirector configuration.
     */
    public static AgeChunkGenerator fromDirector(AgeDirectorImpl director, BiomeSource biomeSource) {
        return new AgeChunkGenerator(
                biomeSource,
                director.getTerrainType(),
                director.getAverageGroundLevel(),
                director.getSeaLevel(),
                director.hasSea()
        );
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Only apply carvers if not void terrain
        if (!"void".equals(terrainType)) {
            // Default carver application would go here
            // For simplicity, we skip custom carvers
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        // Surface building handled in fillFromNoise for custom terrain types
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Default mob spawning
    }

    @Override
    public int getGenDepth() {
        return 384; // Default world height
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateTerrain(chunk);
            return chunk;
        }, executor);
    }

    /**
     * Generates terrain based on the configured terrain type.
     */
    private void generateTerrain(ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        switch (terrainType) {
            case "void":
                // Completely empty - just bedrock at the bottom
                generateVoidTerrain(chunk, minY);
                break;
            case "flat":
                // Flat terrain at ground level
                generateFlatTerrain(chunk, minY, maxY);
                break;
            case "nether":
                // Nether-style terrain
                generateNetherTerrain(chunk, minY, maxY);
                break;
            case "end":
                // End-style islands
                generateEndTerrain(chunk, minY, maxY);
                break;
            case "amplified":
                // Extra tall mountains - use normal for now
                generateNormalTerrain(chunk, minY, maxY, 1.5f);
                break;
            default:
                // Normal terrain
                generateNormalTerrain(chunk, minY, maxY, 1.0f);
                break;
        }
    }

    private void generateVoidTerrain(ChunkAccess chunk, int minY) {
        // Just a single layer of bedrock at the bottom
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                pos.set(x, minY, z);
                chunk.setBlockState(pos, bedrockBlock, false);
            }
        }
    }

    private void generateFlatTerrain(ChunkAccess chunk, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Bedrock at bottom
                pos.set(x, minY, z);
                chunk.setBlockState(pos, bedrockBlock, false);

                // Fill with stone up to ground level - 4
                for (int y = minY + 1; y < groundLevel - 4; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, stoneBlock, false);
                }

                // Dirt layer
                for (int y = groundLevel - 4; y < groundLevel; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                }

                // Grass on top
                pos.set(x, groundLevel, z);
                chunk.setBlockState(pos, Blocks.GRASS_BLOCK.defaultBlockState(), false);
            }
        }
    }

    private void generateNetherTerrain(ChunkAccess chunk, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long chunkSeed = chunk.getPos().toLong();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Bedrock ceiling and floor
                pos.set(x, minY, z);
                chunk.setBlockState(pos, bedrockBlock, false);

                // Fill with netherrack
                for (int y = minY + 1; y < 32; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, netherrackBlock, false);
                }

                // Some open space in middle
                // Ceiling netherrack
                for (int y = 100; y < 128; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, netherrackBlock, false);
                }

                // Bedrock ceiling
                pos.set(x, 128, z);
                chunk.setBlockState(pos, bedrockBlock, false);
            }
        }
    }

    private void generateEndTerrain(ChunkAccess chunk, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Generate sparse end stone islands
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        // Simple island generation based on chunk position
        if ((Math.abs(chunkX) % 4 == 0) && (Math.abs(chunkZ) % 4 == 0)) {
            int islandY = groundLevel;
            int radius = 8;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int dx = x - 8;
                    int dz = z - 8;
                    int dist = dx * dx + dz * dz;

                    if (dist < radius * radius) {
                        int thickness = (int) (3 + (radius - Math.sqrt(dist)) / 2);
                        for (int y = islandY - thickness; y <= islandY; y++) {
                            pos.set(x, y, z);
                            chunk.setBlockState(pos, endStoneBlock, false);
                        }
                    }
                }
            }
        }
    }

    private void generateNormalTerrain(ChunkAccess chunk, int minY, int maxY, float heightScale) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Calculate height using simple noise
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int height = calculateTerrainHeight(worldX, worldZ, heightScale);

                // Bedrock at bottom
                pos.set(x, minY, z);
                chunk.setBlockState(pos, bedrockBlock, false);

                // Fill with stone
                for (int y = minY + 1; y <= height - 4; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, stoneBlock, false);
                }

                // Dirt layer
                for (int y = height - 3; y < height; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                }

                // Grass on top (or sand if below sea level)
                pos.set(x, height, z);
                if (height < seaLevel && hasSea) {
                    chunk.setBlockState(pos, Blocks.SAND.defaultBlockState(), false);
                } else {
                    chunk.setBlockState(pos, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                }

                // Fill with water if below sea level
                if (hasSea && height < seaLevel) {
                    for (int y = height + 1; y <= seaLevel; y++) {
                        pos.set(x, y, z);
                        chunk.setBlockState(pos, waterBlock, false);
                    }
                }
            }
        }
    }

    /**
     * Simple terrain height calculation using pseudo-noise.
     */
    private int calculateTerrainHeight(int x, int z, float scale) {
        // Use multiple octaves for more natural terrain
        double noise = 0;
        noise += Math.sin(x * 0.01) * Math.cos(z * 0.01) * 20;
        noise += Math.sin(x * 0.03) * Math.cos(z * 0.03) * 10;
        noise += Math.sin(x * 0.1) * Math.cos(z * 0.1) * 5;

        int height = groundLevel + (int) (noise * scale);
        return Math.max(-64, Math.min(height, 256));
    }

    @Override
    public int getSeaLevel() {
        return seaLevel;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return calculateTerrainHeight(x, z, "amplified".equals(terrainType) ? 1.5f : 1.0f);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        int height = calculateTerrainHeight(x, z, "amplified".equals(terrainType) ? 1.5f : 1.0f);
        BlockState[] states = new BlockState[level.getHeight()];

        int minY = level.getMinBuildHeight();
        for (int i = 0; i < states.length; i++) {
            int y = minY + i;
            if (y == minY) {
                states[i] = bedrockBlock;
            } else if (y <= height) {
                states[i] = stoneBlock;
            } else if (hasSea && y <= seaLevel) {
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
        info.add("Ground Level: " + groundLevel + ", Sea Level: " + seaLevel);
    }
}
