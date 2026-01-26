package art.arcane.mystcraft.world.gen.noise;

import net.minecraft.util.RandomSource;

/**
 * Octave-based noise generator ported from legacy Mystcraft.
 * Generates multi-octave Perlin noise for terrain generation.
 */
public class NoiseGeneratorOctaves {

    private final NoiseGeneratorPerlin[] generatorArray;
    private final int octaves;

    public NoiseGeneratorOctaves(RandomSource random, int octaveCount) {
        this.octaves = octaveCount;
        this.generatorArray = new NoiseGeneratorPerlin[octaveCount];

        for (int i = 0; i < octaveCount; ++i) {
            this.generatorArray[i] = new NoiseGeneratorPerlin(random);
        }
    }

    /**
     * Generates 3D octave noise.
     * @param noiseArray Pre-allocated array to fill, or null to create new
     * @param xOffset World X offset
     * @param yOffset World Y offset
     * @param zOffset World Z offset
     * @param xSize Array size in X
     * @param ySize Array size in Y
     * @param zSize Array size in Z
     * @param xScale Noise scale in X
     * @param yScale Noise scale in Y
     * @param zScale Noise scale in Z
     * @return The filled noise array
     */
    public double[] generateNoiseOctaves(double[] noiseArray, int xOffset, int yOffset, int zOffset,
                                         int xSize, int ySize, int zSize,
                                         double xScale, double yScale, double zScale) {
        if (noiseArray == null) {
            noiseArray = new double[xSize * ySize * zSize];
        } else {
            for (int i = 0; i < noiseArray.length; ++i) {
                noiseArray[i] = 0.0D;
            }
        }

        double scale = 1.0D;

        for (int octave = 0; octave < this.octaves; ++octave) {
            double scaledX = xOffset * scale * xScale;
            double scaledY = yOffset * scale * yScale;
            double scaledZ = zOffset * scale * zScale;

            // Get integer parts for wrapping
            long xLong = (long) Math.floor(scaledX);
            long zLong = (long) Math.floor(scaledZ);
            scaledX -= xLong;
            scaledZ -= zLong;
            xLong %= 16777216L;
            zLong %= 16777216L;
            scaledX += xLong;
            scaledZ += zLong;

            this.generatorArray[octave].populateNoiseArray(noiseArray,
                    scaledX, scaledY, scaledZ,
                    xSize, ySize, zSize,
                    xScale * scale, yScale * scale, zScale * scale, scale);

            scale /= 2.0D;
        }

        return noiseArray;
    }

    /**
     * Generates 2D octave noise (for surface height variation).
     * @param noiseArray Pre-allocated array or null
     * @param xOffset World X offset
     * @param zOffset World Z offset
     * @param xSize Array size in X
     * @param zSize Array size in Z
     * @param xScale Noise scale in X
     * @param zScale Noise scale in Z
     * @return The filled noise array
     */
    public double[] generateNoiseOctaves(double[] noiseArray, int xOffset, int zOffset,
                                         int xSize, int zSize,
                                         double xScale, double zScale) {
        return generateNoiseOctaves(noiseArray, xOffset, 10, zOffset, xSize, 1, zSize, xScale, 1.0D, zScale);
    }
}
