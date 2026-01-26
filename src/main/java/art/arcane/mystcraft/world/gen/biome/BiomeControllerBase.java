package art.arcane.mystcraft.world.gen.biome;

import art.arcane.mystcraft.api.world.logic.IBiomeController;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for biome controllers that handle biome distribution in an Age.
 */
public abstract class BiomeControllerBase implements IBiomeController {

    protected final List<Holder<Biome>> biomes;
    protected final long seed;

    public BiomeControllerBase(List<Holder<Biome>> biomes, long seed) {
        this.biomes = new ArrayList<>(biomes);
        this.seed = seed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Holder<Biome>[] getBiomesForGeneration(Holder<Biome>[] biomeArray, int x, int z, int width, int height) {
        if (biomeArray == null || biomeArray.length < width * height) {
            biomeArray = (Holder<Biome>[]) new Holder<?>[width * height];
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                biomeArray[i + j * width] = getBiomeAtCoords(x + i, z + j);
            }
        }

        return biomeArray;
    }

    @Override
    public List<Holder<Biome>> getValidSpawnBiomes() {
        // By default, all biomes in this controller are valid for spawning
        return Collections.unmodifiableList(biomes);
    }

    @Override
    public void cleanupCache() {
        // Default implementation does nothing
        // Subclasses with caching can override this
    }

    @Override
    public List<Holder<Biome>> getBiomes() {
        return Collections.unmodifiableList(biomes);
    }
}
