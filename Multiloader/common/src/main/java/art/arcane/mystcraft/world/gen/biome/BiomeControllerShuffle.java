package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Biome controller that shuffles biomes per column using a position hash.
 * Produces highly varied, randomized biome distribution.
 */
public class BiomeControllerShuffle extends BiomeControllerBase {

    public BiomeControllerShuffle(List<Holder<Biome>> biomes, long seed) {
        super(biomes, seed);
        if (this.biomes.isEmpty()) {
            this.biomes.addAll(getDefaultBiomeSet());
        }
    }

    @Override
    public Holder<Biome> getBiomeAtCoords(int x, int z) {
        if (biomes.isEmpty()) {
            Holder<Biome> fallback = getDefaultSingleBiome();
            return fallback != null ? fallback : null;
        }
        long h = positionHash(seed, x, z);
        int idx = (int) Math.floorMod(h, biomes.size());
        return biomes.get(idx);
    }

    private static long positionHash(long seed, int x, int z) {
        long h = seed;
        h ^= (long) x * 73428767L;
        h ^= (long) z * 91236781L;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= h >>> 16;
        return h;
    }

    @Override
    public String getType() {
        return "shuffle";
    }
}
