package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome controller that uses Voronoi cell regions for clean, organic biome distribution.
 * Each cell center is derived from a grid with jittered positions, and the closest center
 * determines the biome. This produces smooth, non-dithered region boundaries with
 * natural-looking shapes at every scale.
 */
public class BiomeControllerNoise extends BiomeControllerBase {

  private static final int CACHE_MAX_SIZE = 4096;
  private final int cellSize;
  private final Map<Long, Holder<Biome>> cache;

  public BiomeControllerNoise(List<Holder<Biome>> biomes, long seed, Scale scale) {
    this(biomes, seed, scale.cellSize);
  }

  /**
   * @param biomes   List of biomes
   * @param seed     World seed
   * @param cellSize Grid cell size in blocks (larger = larger biome regions)
   */
  public BiomeControllerNoise(List<Holder<Biome>> biomes, long seed, int cellSize) {
    super(biomes, seed);
    this.cellSize = Math.max(32, cellSize);
    this.cache = new ConcurrentHashMap<>();
  }

  /**
   * Backwards-compatible constructor accepting the old double scale parameter.
   * Converts the old noise scale to an approximate cell size.
   */
  public BiomeControllerNoise(List<Holder<Biome>> biomes, long seed, double scale) {
    this(biomes, seed, scaleToCell(scale));
  }

  private static int scaleToCell(double scale) {
    if (scale >= 0.02) return Scale.TINY.cellSize;
    if (scale >= 0.01) return Scale.SMALL.cellSize;
    if (scale >= 0.005) return Scale.MEDIUM.cellSize;
    if (scale >= 0.002) return Scale.LARGE.cellSize;
    return Scale.HUGE.cellSize;
  }

  @Override
  public Holder<Biome> getBiomeAtCoords(int x, int z) {
    if (biomes.isEmpty()) {
      return null;
    }

    long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
    Holder<Biome> cached = cache.get(key);
    if (cached != null) {
      return cached;
    }

    Holder<Biome> biome = findNearestVoronoiCell(x, z);

    if (cache.size() < CACHE_MAX_SIZE) {
      cache.put(key, biome);
    }

    return biome;
  }

  /**
   * Finds the closest Voronoi cell center and returns its biome.
   * Checks the 3x3 grid of cells surrounding the query point.
   */
  private Holder<Biome> findNearestVoronoiCell(int x, int z) {
    int cellX = Math.floorDiv(x, cellSize);
    int cellZ = Math.floorDiv(z, cellSize);

    double closestDist = Double.MAX_VALUE;
    int closestIndex = 0;

    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        int cx = cellX + dx;
        int cz = cellZ + dz;

        // Jittered center position for this cell
        long cellHash = hashCell(cx, cz);
        double jitterX = (cellHash & 0xFFFFL) / (double) 0xFFFF;
        double jitterZ = ((cellHash >>> 16) & 0xFFFFL) / (double) 0xFFFF;

        double centerX = (cx + 0.1 + jitterX * 0.8) * cellSize;
        double centerZ = (cz + 0.1 + jitterZ * 0.8) * cellSize;

        double distSq = (x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ);

        if (distSq < closestDist) {
          closestDist = distSq;
          // Biome index from cell hash
          closestIndex = (int) ((cellHash >>> 32) & 0x7FFFFFFFL) % biomes.size();
        }
      }
    }

    return biomes.get(closestIndex);
  }

  private long hashCell(int cx, int cz) {
    long h = seed;
    h ^= (long) cx * 73856093L;
    h ^= (long) cz * 83492791L;
    h = h * 6364136223846793005L + 1442695040888963407L;
    h ^= h >>> 16;
    h = h * 2246822519L + 1;
    h ^= h >>> 13;
    return h;
  }

  @Override
  public void cleanupCache() {
    cache.clear();
  }

  @Override
  public String getType() {
    if (cellSize <= Scale.TINY.cellSize) return "tiny";
    if (cellSize <= Scale.SMALL.cellSize) return "small";
    if (cellSize <= Scale.MEDIUM.cellSize) return "medium";
    if (cellSize <= Scale.LARGE.cellSize) return "large";
    return "huge";
  }

  /**
   * Preset scales for different biome region sizes.
   */
  public enum Scale {
    TINY(64),
    SMALL(128),
    MEDIUM(256),
    LARGE(512),
    HUGE(1024);

    public final int cellSize;

    Scale(int cellSize) {
      this.cellSize = cellSize;
    }
  }
}
