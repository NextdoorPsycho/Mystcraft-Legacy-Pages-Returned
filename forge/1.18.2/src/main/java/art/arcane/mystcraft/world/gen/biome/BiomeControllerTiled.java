package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Biome controller that tiles biomes in a repeating pattern with Voronoi-jittered
 * boundaries. Each tile center is offset by a deterministic jitter based on the seed,
 * creating organic edges instead of sharp rectangles while maintaining a regular layout.
 * <p>
 * 1.18.2 version - identical to common version.
 */
public class BiomeControllerTiled extends BiomeControllerBase {

  private final int tileSize;

  public BiomeControllerTiled(List<Holder<Biome>> biomes, long seed) {
    this(biomes, seed, 256);
  }

  /**
   * @param biomes   List of biomes to tile
   * @param seed     World seed
   * @param tileSize Approximate size of each tile in blocks
   */
  public BiomeControllerTiled(List<Holder<Biome>> biomes, long seed, int tileSize) {
    super(biomes, seed);
    this.tileSize = Math.max(16, tileSize);
  }

  @Override
  public Holder<Biome> getBiomeAtCoords(int x, int z) {
    if (biomes.isEmpty()) {
      return null;
    }

    int tileX = Math.floorDiv(x, tileSize);
    int tileZ = Math.floorDiv(z, tileSize);

    double closestDist = Double.MAX_VALUE;
    int closestTileX = tileX;
    int closestTileZ = tileZ;

    // Check 3x3 neighborhood for closest jittered tile center
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        int tx = tileX + dx;
        int tz = tileZ + dz;

        long tileHash = hashTile(tx, tz);
        double jitterX = (tileHash & 0xFFFFL) / (double) 0xFFFF;
        double jitterZ = ((tileHash >>> 16) & 0xFFFFL) / (double) 0xFFFF;

        double centerX = (tx + 0.15 + jitterX * 0.7) * tileSize;
        double centerZ = (tz + 0.15 + jitterZ * 0.7) * tileSize;

        double distSq = (x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ);

        if (distSq < closestDist) {
          closestDist = distSq;
          closestTileX = tx;
          closestTileZ = tz;
        }
      }
    }

    // Deterministic biome index from tile position (repeating pattern)
    int index = Math.abs((closestTileX + closestTileZ * 7)) % biomes.size();
    return biomes.get(index);
  }

  private long hashTile(int tx, int tz) {
    long h = seed;
    h ^= (long) tx * 73856093L;
    h ^= (long) tz * 83492791L;
    h = h * 6364136223846793005L + 1442695040888963407L;
    h ^= h >>> 16;
    return h;
  }

  @Override
  public String getType() {
    return "tiled";
  }
}
