package art.arcane.mystcraft.world.gen.noise;

import java.util.Random;

/**
 * Improved Perlin noise generator for terrain generation.
 * <p>
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public class NoiseGeneratorPerlin {

  private final int[] permutations;
  public double xCoord;
  public double yCoord;
  public double zCoord;

  public NoiseGeneratorPerlin(Random random) {
    this.permutations = new int[512];
    this.xCoord = random.nextDouble() * 256.0D;
    this.yCoord = random.nextDouble() * 256.0D;
    this.zCoord = random.nextDouble() * 256.0D;

    for (int i = 0; i < 256; ++i) {
      this.permutations[i] = i;
    }

    for (int i = 0; i < 256; ++i) {
      int j = random.nextInt(256 - i) + i;
      int temp = this.permutations[i];
      this.permutations[i] = this.permutations[j];
      this.permutations[j] = temp;
      this.permutations[i + 256] = this.permutations[i];
    }
  }

  private static double lerp(double t, double a, double b) {
    return a + t * (b - a);
  }

  private static double grad2D(int hash, double x, double z) {
    int h = hash & 15;
    double u = h < 8 ? x : z;
    double v = h < 4 ? z : (h == 12 || h == 14 ? x : 0.0D);
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
  }

  private static double grad(int hash, double x, double y, double z) {
    int h = hash & 15;
    double u = h < 8 ? x : y;
    double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
  }

  public void populateNoiseArray(double[] array, double x, double y, double z,
                                 int xSize, int ySize, int zSize,
                                 double xScale, double yScale, double zScale, double amplitude) {
    if (ySize == 1) {
      // 2D noise optimization
      int index = 0;

      double invAmplitude = 1.0D / amplitude;

      for (int zz = 0; zz < zSize; ++zz) {
        double zPos = z + zz * zScale + this.zCoord;
        int zInt = (int) zPos;
        if (zPos < zInt) {
          --zInt;
        }
        int zWrapped = zInt & 255;
        zPos -= zInt;
        double zFade = zPos * zPos * zPos * (zPos * (zPos * 6.0D - 15.0D) + 10.0D);

        for (int xx = 0; xx < xSize; ++xx) {
          double xPos = x + xx * xScale + this.xCoord;
          int xInt = (int) xPos;
          if (xPos < xInt) {
            --xInt;
          }
          int xWrapped = xInt & 255;
          xPos -= xInt;
          double xFade = xPos * xPos * xPos * (xPos * (xPos * 6.0D - 15.0D) + 10.0D);

          int l1 = this.permutations[xWrapped];
          int i2 = this.permutations[l1] + zWrapped;
          int j2 = this.permutations[xWrapped + 1];
          int k2 = this.permutations[j2] + zWrapped;

          double d1 = lerp(xFade, grad2D(this.permutations[i2], xPos, zPos),
              grad(this.permutations[k2], xPos - 1.0D, 0.0D, zPos));
          double d2 = lerp(xFade, grad(this.permutations[i2 + 1], xPos, 0.0D, zPos - 1.0D),
              grad(this.permutations[k2 + 1], xPos - 1.0D, 0.0D, zPos - 1.0D));
          double result = lerp(zFade, d1, d2);

          array[index++] += result * invAmplitude;
        }
      }
    } else {
      // 3D noise
      int index = 0;
      double invAmplitude = 1.0D / amplitude;

      int prevXInt = -1;
      double d1 = 0.0D;
      double d2 = 0.0D;
      double d3 = 0.0D;
      double d4 = 0.0D;

      for (int zz = 0; zz < zSize; ++zz) {
        double zPos = z + zz * zScale + this.zCoord;
        int zInt = (int) zPos;
        if (zPos < zInt) {
          --zInt;
        }
        int zWrapped = zInt & 255;
        zPos -= zInt;
        double zFade = zPos * zPos * zPos * (zPos * (zPos * 6.0D - 15.0D) + 10.0D);

        for (int xx = 0; xx < xSize; ++xx) {
          double xPos = x + xx * xScale + this.xCoord;
          int xInt = (int) xPos;
          if (xPos < xInt) {
            --xInt;
          }
          int xWrapped = xInt & 255;
          xPos -= xInt;
          double xFade = xPos * xPos * xPos * (xPos * (xPos * 6.0D - 15.0D) + 10.0D);

          for (int yy = 0; yy < ySize; ++yy) {
            double yPos = y + yy * yScale + this.yCoord;
            int yInt = (int) yPos;
            if (yPos < yInt) {
              --yInt;
            }
            int yWrapped = yInt & 255;
            yPos -= yInt;
            double yFade = yPos * yPos * yPos * (yPos * (yPos * 6.0D - 15.0D) + 10.0D);

            if (yy == 0 || yWrapped != prevXInt) {
              prevXInt = yWrapped;
              int l2 = this.permutations[xWrapped] + yWrapped;
              int i3 = this.permutations[l2] + zWrapped;
              int j3 = this.permutations[l2 + 1] + zWrapped;
              int k3 = this.permutations[xWrapped + 1] + yWrapped;
              int l3 = this.permutations[k3] + zWrapped;
              int i4 = this.permutations[k3 + 1] + zWrapped;

              d1 = lerp(xFade, grad(this.permutations[i3], xPos, yPos, zPos),
                  grad(this.permutations[l3], xPos - 1.0D, yPos, zPos));
              d2 = lerp(xFade, grad(this.permutations[j3], xPos, yPos - 1.0D, zPos),
                  grad(this.permutations[i4], xPos - 1.0D, yPos - 1.0D, zPos));
              d3 = lerp(xFade, grad(this.permutations[i3 + 1], xPos, yPos, zPos - 1.0D),
                  grad(this.permutations[l3 + 1], xPos - 1.0D, yPos, zPos - 1.0D));
              d4 = lerp(xFade, grad(this.permutations[j3 + 1], xPos, yPos - 1.0D, zPos - 1.0D),
                  grad(this.permutations[i4 + 1], xPos - 1.0D, yPos - 1.0D, zPos - 1.0D));
            }

            double d5 = lerp(yFade, d1, d2);
            double d6 = lerp(yFade, d3, d4);
            double result = lerp(zFade, d5, d6);

            array[index++] += result * invAmplitude;
          }
        }
      }
    }
  }
}
