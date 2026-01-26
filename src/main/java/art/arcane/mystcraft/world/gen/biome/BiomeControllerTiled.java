package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Biome controller that arranges biomes in a repeating tile pattern.
 * Each tile is a fixed size and contains one biome.
 */
public class BiomeControllerTiled extends BiomeControllerBase {

    private final int tileSize;

    /**
     * Creates a tiled biome controller with default 256 block tiles.
     */
    public BiomeControllerTiled(List<Holder<Biome>> biomes, long seed) {
        this(biomes, seed, 256);
    }

    /**
     * Creates a tiled biome controller with custom tile size.
     * @param biomes List of biomes to tile
     * @param seed World seed
     * @param tileSize Size of each tile in blocks
     */
    public BiomeControllerTiled(List<Holder<Biome>> biomes, long seed, int tileSize) {
        super(biomes, seed);
        this.tileSize = Math.max(16, tileSize); // Minimum 16 blocks
    }

    @Override
    public Holder<Biome> getBiomeAtCoords(int x, int z) {
        if (biomes.isEmpty()) {
            return null;
        }

        // Calculate which tile this coordinate is in
        int tileX = Math.floorDiv(x, tileSize);
        int tileZ = Math.floorDiv(z, tileSize);

        // Use a simple pattern that cycles through biomes
        // Add tileZ * 7 to offset the pattern for visual variety
        int index = Math.abs((tileX + tileZ * 7)) % biomes.size();

        return biomes.get(index);
    }

    @Override
    public String getType() {
        return "tiled";
    }
}
