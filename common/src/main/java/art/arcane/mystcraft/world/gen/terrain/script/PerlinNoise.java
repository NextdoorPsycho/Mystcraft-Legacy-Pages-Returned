package art.arcane.mystcraft.world.gen.terrain.script;

import net.minecraft.util.RandomSource;

/**
 * Lightweight Perlin noise sampler for scripted terrain.
 */
public class PerlinNoise {

  private final int[] permutations = new int[512];
  private final double xCoord;
  private final double yCoord;
  private final double zCoord;

  public PerlinNoise(RandomSource random) {
    this.xCoord = random.nextDouble() * 256.0D;
    this.yCoord = random.nextDouble() * 256.0D;
    this.zCoord = random.nextDouble() * 256.0D;
    for (int i = 0; i < 256; ++i) {
      permutations[i] = i;
    }
    for (int i = 0; i < 256; ++i) {
      int j = random.nextInt(256 - i) + i;
      int temp = permutations[i];
      permutations[i] = permutations[j];
      permutations[j] = temp;
      permutations[i + 256] = permutations[i];
    }
  }

  private static double fade(double t) {
    return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
  }

  private static double lerp(double t, double a, double b) {
    return a + t * (b - a);
  }

  private static double grad(int hash, double x, double y, double z) {
    int h = hash & 15;
    double u = h < 8 ? x : y;
    double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
  }

  private static int fastFloor(double value) {
    int i = (int) value;
    return value < i ? i - 1 : i;
  }

  public double noise(double x, double y, double z) {
    double xPos = x + xCoord;
    double yPos = y + yCoord;
    double zPos = z + zCoord;

    int xInt = fastFloor(xPos);
    int yInt = fastFloor(yPos);
    int zInt = fastFloor(zPos);

    xPos -= xInt;
    yPos -= yInt;
    zPos -= zInt;

    int xWrapped = xInt & 255;
    int yWrapped = yInt & 255;
    int zWrapped = zInt & 255;

    double u = fade(xPos);
    double v = fade(yPos);
    double w = fade(zPos);

    int a = permutations[xWrapped] + yWrapped;
    int aa = permutations[a] + zWrapped;
    int ab = permutations[a + 1] + zWrapped;
    int b = permutations[xWrapped + 1] + yWrapped;
    int ba = permutations[b] + zWrapped;
    int bb = permutations[b + 1] + zWrapped;

    double x1 = lerp(u, grad(permutations[aa], xPos, yPos, zPos),
        grad(permutations[ba], xPos - 1.0D, yPos, zPos));
    double x2 = lerp(u, grad(permutations[ab], xPos, yPos - 1.0D, zPos),
        grad(permutations[bb], xPos - 1.0D, yPos - 1.0D, zPos));
    double y1 = lerp(v, x1, x2);

    double x3 = lerp(u, grad(permutations[aa + 1], xPos, yPos, zPos - 1.0D),
        grad(permutations[ba + 1], xPos - 1.0D, yPos, zPos - 1.0D));
    double x4 = lerp(u, grad(permutations[ab + 1], xPos, yPos - 1.0D, zPos - 1.0D),
        grad(permutations[bb + 1], xPos - 1.0D, yPos - 1.0D, zPos - 1.0D));
    double y2 = lerp(v, x3, x4);

    return lerp(w, y1, y2);
  }
}
