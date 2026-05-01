package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.world.gen.noise.NoiseGeneratorOctaves;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/**
 * Nether-style terrain generator that creates a ceiling/floor with open caves
 * in between. Uses cosine-based vertical modulation to create the
 * characteristic nether shape.
 */
public class TerrainGeneratorNether extends TerrainGeneratorBase {

  protected NoiseGeneratorOctaves noiseGen1;
  protected NoiseGeneratorOctaves noiseGen2;
  protected NoiseGeneratorOctaves noiseGen3;

  protected double[] noiseData1;
  protected double[] noiseData2;
  protected double[] noiseData3;

  protected double[] verticalEnvelope;

  public TerrainGeneratorNether(AgeDirector controller, long seed) {
    super(controller, seed);
    this.terrainBlock = Blocks.NETHERRACK.defaultBlockState();
    this.seaBlock = Blocks.LAVA.defaultBlockState();

    RandomSource random = RandomSource.create(seed);
    this.noiseGen1 = new NoiseGeneratorOctaves(random, 16);
    this.noiseGen2 = new NoiseGeneratorOctaves(random, 16);
    this.noiseGen3 = new NoiseGeneratorOctaves(random, 8);

    initializeVerticalEnvelope();
  }

  protected void initializeVerticalEnvelope() {
    int height = NOISE_HEIGHT;
    verticalEnvelope = new double[height];

    for (int y = 0; y < height; y++) {

      verticalEnvelope[y] = Math.cos((y * Math.PI * 6.0D) / height) * 2.0D;

      double distFromCenter = y;
      if (y > height / 2) {
        distFromCenter = height - 1 - y;
      }

      if (distFromCenter < 4.0D) {
        distFromCenter = 4.0D - distFromCenter;
        verticalEnvelope[y] -= distFromCenter * distFromCenter * distFromCenter * 10.0D;
      }
    }
  }

  @Override
  protected double[] initializeNoiseField(double[] field, int x, int y, int z,
                                          int xSize, int ySize, int zSize) {
    if (field == null) {
      field = new double[xSize * ySize * zSize];
    }

    double cfactor1 = 684.412D;
    double cfactor2 = 2053.236D;

    noiseData3 = noiseGen3.generateNoiseOctaves(noiseData3, x, y, z, xSize, ySize, zSize,
        cfactor1 / 80.0D, cfactor2 / 60.0D, cfactor1 / 80.0D);
    noiseData1 = noiseGen1.generateNoiseOctaves(noiseData1, x, y, z, xSize, ySize, zSize,
        cfactor1, cfactor2, cfactor1);
    noiseData2 = noiseGen2.generateNoiseOctaves(noiseData2, x, y, z, xSize, ySize, zSize,
        cfactor1, cfactor2, cfactor1);

    int noiseIndex = 0;

    for (int gridX = 0; gridX < xSize; gridX++) {
      for (int gridZ = 0; gridZ < zSize; gridZ++) {
        for (int gridY = 0; gridY < ySize; gridY++) {

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

          density -= verticalEnvelope[gridY];

          field[noiseIndex] = density;
          noiseIndex++;
        }
      }
    }

    return field;
  }

  @Override
  public String getType() {
    return "nether";
  }
}
