package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.world.gen.noise.NoiseGeneratorOctaves;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

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

    static {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                float f = 10.0F / Mth.sqrt(x * x + z * z + 0.2F);
                PARABOLIC_FIELD[(x + 2) + (z + 2) * 5] = f;
            }
        }
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
     * Gets approximate height data for a biome based on its characteristics.
     * Returns [baseHeight, heightVariation].
     */
    private float[] getBiomeHeightData(Holder<Biome> biomeHolder) {
        if (biomeHolder == null) {
            return new float[]{0.125f, 0.05f}; // Default plains
        }

        Biome biome = biomeHolder.value();

        // Get biome depth (temperature) as a proxy for terrain type
        // Different biome categories have different height characteristics
        float baseDepth = biome.getModifiedClimateSettings().temperature();

        // Use biome downfall as variation proxy
        float downfall = biome.getModifiedClimateSettings().downfall();

        // Approximate height values based on biome characteristics
        // Ocean biomes (low temp, high downfall): negative depth
        // Mountain biomes (low temp, low downfall): high positive depth
        // Plains (medium): near zero depth

        float baseHeight;
        float variation;

        // Check for frozen/cold biomes (often mountainous or tundra)
        if (baseDepth < 0.2f) {
            // Cold biomes - higher terrain
            baseHeight = 0.3f + (1.0f - downfall) * 0.5f;
            variation = 0.2f + (1.0f - downfall) * 0.3f;
        } else if (baseDepth > 0.9f) {
            // Hot biomes - deserts, badlands - moderate hills
            baseHeight = 0.125f + (1.0f - downfall) * 0.2f;
            variation = 0.05f + (1.0f - downfall) * 0.15f;
        } else if (downfall > 0.8f) {
            // Wet biomes - swamps, jungles - lower terrain
            baseHeight = -0.1f + downfall * 0.1f;
            variation = 0.1f;
        } else {
            // Standard biomes - plains, forests
            baseHeight = 0.125f;
            variation = 0.05f + downfall * 0.1f;
        }

        if (amplified) {
            if (baseHeight > 0.0f) {
                baseHeight = 1.0f + baseHeight * 2.0f;
                variation = 1.0f + variation * 4.0f;
            }
        }

        return new float[]{baseHeight, variation};
    }

    @Override
    public String getType() {
        return amplified ? "amplified" : "normal";
    }
}
