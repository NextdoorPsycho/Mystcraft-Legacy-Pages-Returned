package art.arcane.mystcraft.world.gen.biome;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for biome controllers that handle biome distribution in an Age.
 * <p>
 * 1.18.2 version using Registry.BIOME_REGISTRY instead of Registries.BIOME.
 */
public abstract class BiomeControllerBase implements IBiomeController {

  private static final Logger LOGGER = LoggerFactory.getLogger(BiomeControllerBase.class);

  protected final List<Holder<Biome>> biomes;
  protected final long seed;

  public BiomeControllerBase(List<Holder<Biome>> biomes, long seed) {
    this.biomes = new ArrayList<>(biomes);
    this.seed = seed;
  }

  /**
   * Resolves a diverse set of fallback biomes from the registry.
   * Used when a controller is created with an empty biome list.
   */
  protected static List<Holder<Biome>> getDefaultBiomeSet() {
    List<Holder<Biome>> defaults = new ArrayList<>();
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server == null) {
      LOGGER.warn("[BiomeController] Cannot resolve fallback biomes: server not available");
      return defaults;
    }
    server.registryAccess().registry(Registry.BIOME_REGISTRY).ifPresent(registry -> {
      defaults.add(registry.getHolderOrThrow(Biomes.PLAINS));
      defaults.add(registry.getHolderOrThrow(Biomes.FOREST));
      defaults.add(registry.getHolderOrThrow(Biomes.DESERT));
      defaults.add(registry.getHolderOrThrow(Biomes.TAIGA));
      defaults.add(registry.getHolderOrThrow(Biomes.JUNGLE));
      defaults.add(registry.getHolderOrThrow(Biomes.SWAMP));
    });
    if (!defaults.isEmpty()) {
      LOGGER.info("[BiomeController] Using {} fallback biomes for empty biome list", defaults.size());
    }
    return defaults;
  }

  /**
   * Resolves a single fallback biome (Plains) from the registry.
   */
  protected static Holder<Biome> getDefaultSingleBiome() {
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server == null) {
      return null;
    }
    Holder<Biome>[] result = new Holder[1];
    server.registryAccess().registry(Registry.BIOME_REGISTRY).ifPresent(registry ->
        result[0] = registry.getHolderOrThrow(Biomes.PLAINS)
    );
    return result[0];
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
