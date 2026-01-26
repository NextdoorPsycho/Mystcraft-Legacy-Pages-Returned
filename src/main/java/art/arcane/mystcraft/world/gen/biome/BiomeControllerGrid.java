package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Biome controller that arranges biomes in a grid pattern with randomized cells.
 * Each grid cell gets a random biome based on its position and the seed.
 */
public class BiomeControllerGrid extends BiomeControllerBase {

    private final int gridSize;

    /**
     * Creates a grid biome controller with default 128 block cells.
     */
    public BiomeControllerGrid(List<Holder<Biome>> biomes, long seed) {
        this(biomes, seed, 128);
    }

    /**
     * Creates a grid biome controller with custom cell size.
     * @param biomes List of biomes
     * @param seed World seed
     * @param gridSize Size of each grid cell in blocks
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

        // Calculate which grid cell this coordinate is in
        int gridX = Math.floorDiv(x, gridSize);
        int gridZ = Math.floorDiv(z, gridSize);

        // Create a hash from the cell position and seed
        long cellHash = hashCell(gridX, gridZ);
        int index = Math.abs((int) (cellHash % biomes.size()));

        return biomes.get(index);
    }

    /**
     * Creates a hash value for a grid cell.
     */
    protected long hashCell(int gridX, int gridZ) {
        // Use a simple but effective hash combining position and seed
        long hash = seed;
        hash ^= gridX * 0x5DEECE66DL;
        hash ^= gridZ * 0xBB38E497L;
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
