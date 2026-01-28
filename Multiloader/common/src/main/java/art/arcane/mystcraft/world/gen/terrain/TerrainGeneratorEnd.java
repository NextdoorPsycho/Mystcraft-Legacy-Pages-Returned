package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.world.gen.noise.NoiseGeneratorOctaves;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/**
 * End-style terrain generator that creates floating islands that are denser near the center.
 * Uses distance-based density falloff from the world origin.
 */
public class TerrainGeneratorEnd extends TerrainGeneratorBase {

    // Noise generators
    protected NoiseGeneratorOctaves noiseGen1;
    protected NoiseGeneratorOctaves noiseGen2;
    protected NoiseGeneratorOctaves noiseGen3;

    // Noise data arrays
    protected double[] noiseData1;
    protected double[] noiseData2;
    protected double[] noiseData3;

    public TerrainGeneratorEnd(AgeDirector controller, long seed) {
        super(controller, seed);
        this.terrainBlock = Blocks.END_STONE.defaultBlockState();
        this.seaBlock = Blocks.AIR.defaultBlockState(); // End has no sea

        RandomSource random = RandomSource.create(seed);
        this.noiseGen1 = new NoiseGeneratorOctaves(random, 16);
        this.noiseGen2 = new NoiseGeneratorOctaves(random, 16);
        this.noiseGen3 = new NoiseGeneratorOctaves(random, 8);
    }

    @Override
    protected double[] initializeNoiseField(double[] field, int x, int y, int z,
                                            int xSize, int ySize, int zSize) {
        if (field == null) {
            field = new double[xSize * ySize * zSize];
        }

        // Generate noise
        noiseData3 = noiseGen3.generateNoiseOctaves(noiseData3, x, y, z, xSize, ySize, zSize,
                684.412D / 80.0D, 684.412D / 160.0D, 684.412D / 80.0D);
        noiseData1 = noiseGen1.generateNoiseOctaves(noiseData1, x, y, z, xSize, ySize, zSize,
                684.412D, 684.412D, 684.412D);
        noiseData2 = noiseGen2.generateNoiseOctaves(noiseData2, x, y, z, xSize, ySize, zSize,
                684.412D, 684.412D, 684.412D);

        int noiseIndex = 0;

        for (int gridX = 0; gridX < xSize; gridX++) {
            // Calculate world X coordinate for this grid point
            int worldX = (x + gridX) * XZ_STEP;

            for (int gridZ = 0; gridZ < zSize; gridZ++) {
                // Calculate world Z coordinate
                int worldZ = (z + gridZ) * XZ_STEP;

                // Distance-based density factor (islands denser near center)
                float distFactor = calculateDistanceFactor(worldX, worldZ);

                for (int gridY = 0; gridY < ySize; gridY++) {
                    // Blend noise sources
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

                    // Apply height gradient (islands centered around y=64)
                    int centerY = ySize / 2;
                    double heightFactor = ((gridY - centerY) * 8.0D);
                    if (gridY > centerY) {
                        heightFactor *= 2.0D; // Sharper falloff above
                    }
                    density -= heightFactor;

                    // Apply distance factor
                    density += distFactor;

                    // Fade at world top
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
     * Calculates the distance-based density factor.
     * Islands are more likely near the center, less likely far away.
     */
    protected float calculateDistanceFactor(int worldX, int worldZ) {
        float distance = Mth.sqrt(worldX * worldX + worldZ * worldZ);
        float factor = 100.0F - distance * 4.0F;

        // Clamp to reasonable range
        if (factor > 80.0F) {
            factor = 80.0F;
        }
        if (factor < -100.0F) {
            factor = -100.0F;
        }

        return factor;
    }

    @Override
    public String getType() {
        return "end";
    }
}
