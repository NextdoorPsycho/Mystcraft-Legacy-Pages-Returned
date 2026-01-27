package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.world.gen.noise.NoiseGeneratorOctaves;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.HashMap;
import java.util.Map;

/**
 * Normal terrain generator that creates Minecraft-like terrain with biome height variation.
 * Uses multiple octave noise generators blended with biome heights.
 */
public class TerrainGeneratorNormal extends TerrainGeneratorBase {

    private final boolean amplified;

    // Biome source for height queries
    private BiomeSource biomeSource;

    // Noise generators
    protected NoiseGeneratorOctaves noiseGen1;
    protected NoiseGeneratorOctaves noiseGen2;
    protected NoiseGeneratorOctaves noiseGen3;
    protected NoiseGeneratorOctaves noiseGen5;

    // Noise data arrays
    protected double[] noiseData1;
    protected double[] noiseData2;
    protected double[] noiseData3;
    protected double[] noiseData5;

    // Biome height parameters
    protected double[] biomeMinHeights;
    protected double[] biomeMaxHeights;

    // Pre-calculated parabolic falloff field for biome blending
    protected static final float[] PARABOLIC_FIELD = new float[25];

    // Biome height lookup table: maps biome resource location to [baseHeight, heightVariation]
    // Maps biome to [baseHeight, heightVariation] for terrain shaping
    private static final Map<ResourceLocation, float[]> BIOME_HEIGHT_MAP = new HashMap<>();

    static {
        // Initialize parabolic field
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                float f = 10.0F / Mth.sqrt(x * x + z * z + 0.2F);
                PARABOLIC_FIELD[(x + 2) + (z + 2) * 5] = f;
            }
        }

        // Initialize biome height map
        // Format: [baseHeight, heightVariation]
        // Positive baseHeight = above sea level, negative = below

        // Ocean biomes
        BIOME_HEIGHT_MAP.put(Biomes.OCEAN.location(), new float[]{-1.0f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.DEEP_OCEAN.location(), new float[]{-1.8f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.FROZEN_OCEAN.location(), new float[]{-1.0f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.DEEP_FROZEN_OCEAN.location(), new float[]{-1.8f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.COLD_OCEAN.location(), new float[]{-1.0f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.DEEP_COLD_OCEAN.location(), new float[]{-1.8f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.LUKEWARM_OCEAN.location(), new float[]{-1.0f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.DEEP_LUKEWARM_OCEAN.location(), new float[]{-1.8f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.WARM_OCEAN.location(), new float[]{-1.0f, 0.1f});

        // Plains and flat biomes
        BIOME_HEIGHT_MAP.put(Biomes.PLAINS.location(), new float[]{0.125f, 0.05f});
        BIOME_HEIGHT_MAP.put(Biomes.SUNFLOWER_PLAINS.location(), new float[]{0.125f, 0.05f});
        BIOME_HEIGHT_MAP.put(Biomes.SNOWY_PLAINS.location(), new float[]{0.125f, 0.05f});
        BIOME_HEIGHT_MAP.put(Biomes.ICE_SPIKES.location(), new float[]{0.425f, 0.45f});
        BIOME_HEIGHT_MAP.put(Biomes.MEADOW.location(), new float[]{0.1f, 0.2f});

        // Forest biomes
        BIOME_HEIGHT_MAP.put(Biomes.FOREST.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.FLOWER_FOREST.location(), new float[]{0.1f, 0.4f});
        BIOME_HEIGHT_MAP.put(Biomes.BIRCH_FOREST.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.OLD_GROWTH_BIRCH_FOREST.location(), new float[]{0.2f, 0.4f});
        BIOME_HEIGHT_MAP.put(Biomes.DARK_FOREST.location(), new float[]{0.1f, 0.2f});

        // Taiga biomes
        BIOME_HEIGHT_MAP.put(Biomes.TAIGA.location(), new float[]{0.2f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.SNOWY_TAIGA.location(), new float[]{0.2f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.OLD_GROWTH_PINE_TAIGA.location(), new float[]{0.2f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA.location(), new float[]{0.2f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.GROVE.location(), new float[]{0.3f, 0.4f});

        // Mountain biomes
        BIOME_HEIGHT_MAP.put(Biomes.WINDSWEPT_HILLS.location(), new float[]{1.0f, 0.5f});
        BIOME_HEIGHT_MAP.put(Biomes.WINDSWEPT_GRAVELLY_HILLS.location(), new float[]{1.0f, 0.5f});
        BIOME_HEIGHT_MAP.put(Biomes.WINDSWEPT_FOREST.location(), new float[]{1.0f, 0.5f});
        BIOME_HEIGHT_MAP.put(Biomes.STONY_PEAKS.location(), new float[]{1.0f, 0.3f});
        BIOME_HEIGHT_MAP.put(Biomes.JAGGED_PEAKS.location(), new float[]{1.5f, 0.8f});
        BIOME_HEIGHT_MAP.put(Biomes.FROZEN_PEAKS.location(), new float[]{1.5f, 0.8f});
        BIOME_HEIGHT_MAP.put(Biomes.SNOWY_SLOPES.location(), new float[]{0.45f, 0.3f});

        // Jungle biomes
        BIOME_HEIGHT_MAP.put(Biomes.JUNGLE.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.SPARSE_JUNGLE.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.BAMBOO_JUNGLE.location(), new float[]{0.1f, 0.2f});

        // Swamp biomes
        BIOME_HEIGHT_MAP.put(Biomes.SWAMP.location(), new float[]{-0.2f, 0.1f});
        BIOME_HEIGHT_MAP.put(Biomes.MANGROVE_SWAMP.location(), new float[]{-0.2f, 0.1f});

        // Desert and badlands
        BIOME_HEIGHT_MAP.put(Biomes.DESERT.location(), new float[]{0.125f, 0.05f});
        BIOME_HEIGHT_MAP.put(Biomes.BADLANDS.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.ERODED_BADLANDS.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.WOODED_BADLANDS.location(), new float[]{0.1f, 0.2f});

        // Savanna biomes
        BIOME_HEIGHT_MAP.put(Biomes.SAVANNA.location(), new float[]{0.125f, 0.05f});
        BIOME_HEIGHT_MAP.put(Biomes.SAVANNA_PLATEAU.location(), new float[]{1.5f, 0.025f});
        BIOME_HEIGHT_MAP.put(Biomes.WINDSWEPT_SAVANNA.location(), new float[]{0.3625f, 1.225f});

        // Beach and shore biomes
        BIOME_HEIGHT_MAP.put(Biomes.BEACH.location(), new float[]{0.0f, 0.025f});
        BIOME_HEIGHT_MAP.put(Biomes.SNOWY_BEACH.location(), new float[]{0.0f, 0.025f});
        BIOME_HEIGHT_MAP.put(Biomes.STONY_SHORE.location(), new float[]{0.1f, 0.8f});

        // River biomes
        BIOME_HEIGHT_MAP.put(Biomes.RIVER.location(), new float[]{-0.5f, 0.0f});
        BIOME_HEIGHT_MAP.put(Biomes.FROZEN_RIVER.location(), new float[]{-0.5f, 0.0f});

        // Mushroom biome
        BIOME_HEIGHT_MAP.put(Biomes.MUSHROOM_FIELDS.location(), new float[]{0.2f, 0.3f});

        // Cave biomes (treat as underground, use low height)
        BIOME_HEIGHT_MAP.put(Biomes.LUSH_CAVES.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.DRIPSTONE_CAVES.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.DEEP_DARK.location(), new float[]{0.1f, 0.2f});

        // Cherry grove
        BIOME_HEIGHT_MAP.put(Biomes.CHERRY_GROVE.location(), new float[]{0.1f, 0.2f});

        // Nether biomes (for nether-style terrain)
        BIOME_HEIGHT_MAP.put(Biomes.NETHER_WASTES.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.SOUL_SAND_VALLEY.location(), new float[]{0.0f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.CRIMSON_FOREST.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.WARPED_FOREST.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.BASALT_DELTAS.location(), new float[]{0.1f, 0.2f});

        // End biomes
        BIOME_HEIGHT_MAP.put(Biomes.THE_END.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.END_HIGHLANDS.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.END_MIDLANDS.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.SMALL_END_ISLANDS.location(), new float[]{0.1f, 0.2f});
        BIOME_HEIGHT_MAP.put(Biomes.END_BARRENS.location(), new float[]{0.1f, 0.2f});

        // The Void
        BIOME_HEIGHT_MAP.put(Biomes.THE_VOID.location(), new float[]{0.1f, 0.2f});
    }

    public TerrainGeneratorNormal(AgeDirector controller, long seed, boolean amplified) {
        super(controller, seed);
        this.amplified = amplified;

        RandomSource random = RandomSource.create(seed);
        this.noiseGen1 = new NoiseGeneratorOctaves(random, 16);
        this.noiseGen2 = new NoiseGeneratorOctaves(random, 16);
        this.noiseGen3 = new NoiseGeneratorOctaves(random, 8);
        // Skip noiseGen4 as it's unused
        random = RandomSource.create(seed); // Reset for noiseGen5
        for (int i = 0; i < 10; i++) random.nextLong(); // Skip some values
        this.noiseGen5 = new NoiseGeneratorOctaves(random, 16);
    }

    @Override
    protected double[] initializeNoiseField(double[] field, int x, int y, int z,
                                            int xSize, int ySize, int zSize) {
        if (field == null) {
            field = new double[xSize * ySize * zSize];
        }

        // Generate noise arrays
        noiseData5 = noiseGen5.generateNoiseOctaves(noiseData5, x, z, xSize, zSize, 200.0D, 200.0D);
        noiseData3 = noiseGen3.generateNoiseOctaves(noiseData3, x, y, z, xSize, ySize, zSize,
                684.412D / 80.0D, 684.412D / 160.0D, 684.412D / 80.0D);
        noiseData1 = noiseGen1.generateNoiseOctaves(noiseData1, x, y, z, xSize, ySize, zSize,
                684.412D, 684.412D, 684.412D);
        noiseData2 = noiseGen2.generateNoiseOctaves(noiseData2, x, y, z, xSize, ySize, zSize,
                684.412D, 684.412D, 684.412D);

        // Get biome heights (simplified - use default values)
        if (biomeMinHeights == null || biomeMinHeights.length < xSize * zSize) {
            biomeMinHeights = new double[xSize * zSize];
            biomeMaxHeights = new double[xSize * zSize];
        }

        calculateBiomeHeights(x, z, xSize, zSize);

        int noiseIndex = 0;
        int surfaceIndex = 0;

        for (int gridX = 0; gridX < xSize; gridX++) {
            for (int gridZ = 0; gridZ < zSize; gridZ++) {
                // Get averaged biome height values
                double minHeight = biomeMinHeights[surfaceIndex];
                double maxHeight = biomeMaxHeights[surfaceIndex];
                surfaceIndex++;

                // Apply surface variation from noise5
                minHeight += noiseData5[gridX + gridZ * xSize] * 0.2D;
                minHeight = (minHeight * ySize) / 16.0D;
                // Ground level at Y=64 = gridY 16 (since minY=-64, gridY=16 -> Y=-64+16*8=64)
                double baseHeight = 16.0D + minHeight * 4.0D;

                for (int gridY = 0; gridY < ySize; gridY++) {
                    // Calculate density gradient based on distance from terrain surface
                    double avgDensity = ((gridY - baseHeight) * 12.0D * 128.0D) / 128.0D / maxHeight;
                    if (avgDensity < 0.0D) {
                        avgDensity *= 4.0D; // Steeper transition underground
                    }

                    // Blend the two noise sources using noise3 as blend factor
                    double noise1 = noiseData1[noiseIndex] / 512.0D;
                    double noise2 = noiseData2[noiseIndex] / 512.0D;
                    double blend = (noiseData3[noiseIndex] / 10.0D + 1.0D) / 2.0D;

                    double density;
                    if (blend < 0.0D) {
                        density = noise1;
                    } else if (blend > 1.0D) {
                        density = noise2;
                    } else {
                        density = noise1 + (noise2 - noise1) * blend;
                    }

                    density -= avgDensity;

                    // Fade to negative at world top (last 4 grid cells)
                    if (gridY > ySize - 4) {
                        double fade = (gridY - (ySize - 4)) / 3.0F;
                        density = density * (1.0D - fade) + -10.0D * fade;
                    }

                    field[noiseIndex] = density;
                    noiseIndex++;
                }
            }
        }

        return field;
    }

    /**
     * Calculate biome-weighted height values using parabolic falloff.
     */
    protected void calculateBiomeHeights(int gridX, int gridZ, int xSize, int zSize) {
        // Default biome heights (plains) used when no biome source available
        float defaultBaseHeight = 0.125F;
        float defaultVariation = 0.05F;

        if (amplified && biomeSource == null) {
            if (defaultBaseHeight > 0.0F) {
                defaultBaseHeight = 1.0F + defaultBaseHeight * 2.0F;
                defaultVariation = 1.0F + defaultVariation * 4.0F;
            }
        }

        int surfaceIndex = 0;
        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {
                float totalWeight = 0.0F;
                float avgMinHeight = 0.0F;
                float avgMaxHeight = 0.0F;

                // Apply parabolic blending with surrounding area
                for (int ox = -2; ox <= 2; ox++) {
                    for (int oz = -2; oz <= 2; oz++) {
                        float height;
                        float variation;

                        if (biomeSource != null) {
                            // Query actual biome from biome source
                            // Convert grid coordinates to world coordinates (grid * XZ_STEP * 4)
                            int worldX = (gridX + x + ox) * XZ_STEP * 4;
                            int worldZ = (gridZ + z + oz) * XZ_STEP * 4;

                            // Get biome at this position (use Y=64 for surface biome)
                            Holder<Biome> biome = biomeSource.getNoiseBiome(
                                    QuartPos.fromBlock(worldX),
                                    QuartPos.fromBlock(64),
                                    QuartPos.fromBlock(worldZ),
                                    Climate.empty()
                            );

                            float[] heightData = getBiomeHeightData(biome);
                            height = heightData[0];
                            variation = heightData[1];
                        } else {
                            // Fallback to defaults
                            height = defaultBaseHeight;
                            variation = defaultVariation;
                        }

                        float weight = PARABOLIC_FIELD[(ox + 2) + (oz + 2) * 5] / (height + 2.0F);

                        totalWeight += weight;
                        avgMinHeight += height * weight;
                        avgMaxHeight += variation * weight;
                    }
                }

                avgMinHeight /= totalWeight;
                avgMaxHeight /= totalWeight;

                biomeMinHeights[surfaceIndex] = avgMinHeight;
                biomeMaxHeights[surfaceIndex] = avgMaxHeight + 0.05D; // Minimum variation
                surfaceIndex++;
            }
        }
    }

    /**
     * Sets the biome source for height queries.
     */
    public void setBiomeSource(BiomeSource biomeSource) {
        this.biomeSource = biomeSource;
    }

    /**
     * Gets height data for a biome from the lookup table.
     * Returns [baseHeight, heightVariation].
     *
     * Uses the biome height lookup table.
     * Falls back to plains-like values for unknown biomes.
     */
    private float[] getBiomeHeightData(Holder<Biome> biomeHolder) {
        // Default plains values
        float baseHeight = 0.125f;
        float variation = 0.05f;

        if (biomeHolder != null) {
            // Try to get the biome's resource location for lookup
            ResourceLocation biomeId = biomeHolder.unwrapKey()
                    .map(ResourceKey::location)
                    .orElse(null);

            if (biomeId != null) {
                float[] heights = BIOME_HEIGHT_MAP.get(biomeId);
                if (heights != null) {
                    baseHeight = heights[0];
                    variation = heights[1];
                }
            }
        }

        // Apply amplified terrain modifications
        if (amplified && baseHeight > 0.0f) {
            baseHeight = 1.0f + baseHeight * 2.0f;
            variation = 1.0f + variation * 4.0f;
        }

        return new float[]{baseHeight, variation};
    }

    /**
     * Registers custom biome height data for modded biomes.
     * Call during mod initialization to add support for biomes not in the default lookup table.
     *
     * @param biomeId The biome's resource location
     * @param baseHeight The base terrain height (-2.0 to 2.0, 0 = sea level)
     * @param heightVariation The terrain variation/roughness (0.0 to 2.0)
     */
    public static void registerBiomeHeight(ResourceLocation biomeId, float baseHeight, float heightVariation) {
        BIOME_HEIGHT_MAP.put(biomeId, new float[]{baseHeight, heightVariation});
    }

    @Override
    public String getType() {
        return amplified ? "amplified" : "normal";
    }
}
