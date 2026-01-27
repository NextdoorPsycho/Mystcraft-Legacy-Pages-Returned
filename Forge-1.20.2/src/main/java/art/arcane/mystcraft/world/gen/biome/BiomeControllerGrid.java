package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Biome controller that arranges biomes in a Voronoi-jittered grid.
 * Each grid cell has a jittered center point, and the closest center determines
 * the biome. This creates irregular, organic-looking cell boundaries instead of
 * the sharp rectangular edges of a plain grid.
 */
public class BiomeControllerGrid extends BiomeControllerBase {

    private final int gridSize;

    public BiomeControllerGrid(List<Holder<Biome>> biomes, long seed) {
        this(biomes, seed, 128);
    }

    /**
     * @param biomes List of biomes
     * @param seed World seed
     * @param gridSize Approximate size of each grid cell in blocks
     */
    public BiomeControllerGrid(List<Holder<Biome>> biomes, long seed, int gridSize) {
        super(biomes, seed);
        this.gridSize = Math.max(16, gridSize);
    }

    @Override
    public Holder<Biome> getBiomeAtCoords(int x, int z) {
        if (biomes.isEmpty()) {
            return null;
        }

        int cellX = Math.floorDiv(x, gridSize);
        int cellZ = Math.floorDiv(z, gridSize);

        double closestDist = Double.MAX_VALUE;
        int closestIndex = 0;

        // Check 3x3 neighborhood for the closest jittered center
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = cellX + dx;
                int cz = cellZ + dz;

                long cellHash = hashCell(cx, cz);
                double jitterX = (cellHash & 0xFFFFL) / (double) 0xFFFF;
                double jitterZ = ((cellHash >>> 16) & 0xFFFFL) / (double) 0xFFFF;

                double centerX = (cx + 0.15 + jitterX * 0.7) * gridSize;
                double centerZ = (cz + 0.15 + jitterZ * 0.7) * gridSize;

                double distSq = (x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ);

                if (distSq < closestDist) {
                    closestDist = distSq;
                    closestIndex = Math.abs((int) ((cellHash >>> 32) % biomes.size()));
                }
            }
        }

        return biomes.get(closestIndex);
    }

    protected long hashCell(int gridX, int gridZ) {
        long hash = seed;
        hash ^= (long) gridX * 0x5DEECE66DL;
        hash ^= (long) gridZ * 0xBB38E497L;
        hash ^= (hash >>> 33);
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= (hash >>> 33);
        return hash;
    }

    @Override
    public String getType() {
        return "grid";
    }
}
