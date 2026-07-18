package art.arcane.mystcraft.client;

import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Resolves client-visible colors for Age-bound blocks and items from synced Age metadata.
 */
public final class AgeColorUtils {

  private AgeColorUtils() {
  }

  public static int getCurrentAgeUID() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return -1;

    if (!AgeDimensionFactory.isMystcraftAge(mc.level.dimension())) {
      return -1;
    }

    String path = mc.level.dimension().identifier().getPath();
    if (path.startsWith("mystcraft_age_")) {
      try {
        return Integer.parseInt(path.substring("mystcraft_age_".length()));
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }

  public static int selectColorFromPalette(List<Integer> colors, BlockPos pos, int ageUID) {
    double scale = 0.021;
    double nx = pos.getX() * scale;
    double nz = pos.getZ() * scale;
    double noise = perlinNoise2D(nx, nz, ageUID);
    double normalized = (noise + 1.0) * 0.5;
    int index = (int) (normalized * colors.size());
    index = Math.max(0, Math.min(index, colors.size() - 1));
    return colors.get(index);
  }

  private static double perlinNoise2D(double x, double y, int seed) {
    x += seed * 31.7;
    y += seed * 17.3;

    int xi = floorInt(x);
    int yi = floorInt(y);
    double xf = x - xi;
    double yf = y - yi;

    double u = fade(xf);
    double v = fade(yf);

    int aa = hash2D(xi, yi, seed);
    int ab = hash2D(xi, yi + 1, seed);
    int ba = hash2D(xi + 1, yi, seed);
    int bb = hash2D(xi + 1, yi + 1, seed);

    double x1 = lerp(grad2D(aa, xf, yf), grad2D(ba, xf - 1, yf), u);
    double x2 = lerp(grad2D(ab, xf, yf - 1), grad2D(bb, xf - 1, yf - 1), u);

    return lerp(x1, x2, v);
  }

  private static int floorInt(double x) {
    int xi = (int) x;
    return x < xi ? xi - 1 : xi;
  }

  private static double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private static double lerp(double a, double b, double t) {
    return a + t * (b - a);
  }

  private static int hash2D(int x, int y, int seed) {
    int h = seed;
    h ^= x * 374761393;
    h ^= y * 668265263;
    h = (h ^ (h >> 13)) * 1274126177;
    return h;
  }

  private static double grad2D(int hash, double x, double y) {
    int h = hash & 3;
    double u = h < 2 ? x : y;
    double v = h < 2 ? y : x;
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
  }
}
