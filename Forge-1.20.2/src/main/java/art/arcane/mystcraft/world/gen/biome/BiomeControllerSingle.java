package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.Collections;
import java.util.List;

/**
 * Biome controller that returns a single biome for the entire Age.
 */
public class BiomeControllerSingle extends BiomeControllerBase {

    private final Holder<Biome> singleBiome;

    public BiomeControllerSingle(Holder<Biome> biome, long seed) {
        super(Collections.singletonList(biome), seed);
        this.singleBiome = biome;
    }

    public BiomeControllerSingle(List<Holder<Biome>> biomes, long seed) {
        super(biomes.isEmpty() ? Collections.emptyList() : Collections.singletonList(biomes.get(0)), seed);
        this.singleBiome = biomes.isEmpty() ? null : biomes.get(0);
    }

    @Override
    public Holder<Biome> getBiomeAtCoords(int x, int z) {
        return singleBiome;
    }

    @Override
    public String getType() {
        return "single";
    }
}
