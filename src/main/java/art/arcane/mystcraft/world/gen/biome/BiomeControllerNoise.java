package art.arcane.mystcraft.world.gen.biome;

import art.arcane.mystcraft.world.gen.noise.NoiseGeneratorOctaves;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Biome controller that uses noise to create organic biome regions.
 * The scale parameter controls the size of biome regions.
 */
public class BiomeControllerNoise extends BiomeControllerBase {

    /**
     * Preset scales for different biome region sizes.
     */
    public enum Scale {
        TINY(0.02),      // Very small biome regions
        SMALL(0.01),     // Small biome regions
        MEDIUM(0.005),   // Medium biome regions
        LARGE(0.002),    // Large biome regions
        HUGE(0.001);     // Huge biome regions

        public final double value;

        Scale(double value) {
            this.value = value;
        }
    }

    private final double scale;
    private final NoiseGeneratorOctaves noiseGen;
    private final Map<Long, Holder<Biome>> cache;
    private static final int CACHE_MAX_SIZE = 4096;

    /**
     * Creates a noise-based biome controller with the specified scale.
     */
    public BiomeControllerNoise(List<Holder<Biome>> biomes, long seed, Scale scale) {
        this(biomes, seed, scale.value);
    }

    /**
     * Creates a noise-based biome controller with a custom scale.
     * @param biomes List of biomes
     * @param seed World seed
     * @param scale Noise scale (smaller = larger regions)
     */
    public BiomeControllerNoise(List<Holder<Biome>> biomes, long seed, double scale) {
        super(biomes, seed);
        this.scale = scale;
        this.noiseGen = new NoiseGeneratorOctaves(RandomSource.create(seed), 4);
        this.cache = new HashMap<>();
    }

    @Override
    public Holder<Biome> getBiomeAtCoords(int x, int z) {
        if (biomes.isEmpty()) {
            return null;
        }

        // Check cache first
        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
        Holder<Biome> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        // Generate noise value at this location
        double[] noise = noiseGen.generateNoiseOctaves(null, x, z, 1, 1, scale, scale);
        double noiseValue = noise[0];

        // Map noise (-1 to 1 range) to biome index
        // Add 1 to shift range to 0-2, then divide by 2 to get 0-1
        double normalized = (noiseValue + 1.0) * 0.5;
        normalized = Math.max(0.0, Math.min(1.0, normalized)); // Clamp to 0-1

        int index = (int) (normalized * biomes.size());
        if (index >= biomes.size()) {
            index = biomes.size() - 1;
        }

        Holder<Biome> biome = biomes.get(index);

        // Add to cache (with size limit)
        if (cache.size() < CACHE_MAX_SIZE) {
            cache.put(key, biome);
        }

        return biome;
    }

    @Override
    public void cleanupCache() {
        cache.clear();
    }

    @Override
    public String getType() {
        // Return type based on scale
        if (scale <= Scale.TINY.value) {
            return "tiny";
        } else if (scale <= Scale.SMALL.value) {
            return "small";
        } else if (scale <= Scale.MEDIUM.value) {
            return "medium";
        } else if (scale <= Scale.LARGE.value) {
            return "large";
        } else {
            return "huge";
        }
    }
}
