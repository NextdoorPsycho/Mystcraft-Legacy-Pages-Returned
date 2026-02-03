package art.arcane.mystcraft.world.gen.biome;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * BiomeSource implementation that wraps an IBiomeController from the AgeDirector.
 * This allows the Mystcraft biome controller system to integrate with Minecraft's
 * chunk generation pipeline.
 * <p>
 * 1.18.2 version using Registry.BIOME_REGISTRY instead of Registries.BIOME.
 */
public class AgeBiomeSource extends BiomeSource {

  public static final Codec<AgeBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.LONG.fieldOf("seed").forGetter(source -> source.seed),
          Codec.STRING.fieldOf("controller_type").forGetter(source -> source.controllerType),
          ResourceLocation.CODEC.listOf().fieldOf("biome_ids").forGetter(source -> source.biomeIds)
      ).apply(instance, AgeBiomeSource::new)
  );

  private final long seed;
  private final String controllerType;
  private final List<ResourceLocation> biomeIds;
  private IBiomeController biomeController;
  private List<Holder<Biome>> cachedBiomes;

  /**
   * Creates an AgeBiomeSource from an existing biome controller.
   * This is the primary constructor used when creating a new Age.
   */
  public AgeBiomeSource(IBiomeController controller, long seed) {
    // 1.18.2 requires a biome stream in the constructor
    super(collectBiomesFromController(controller));
    this.biomeController = controller;
    this.seed = seed;
    this.controllerType = controller != null ? controller.getType() : "native";

    // Extract biome IDs for serialization
    this.biomeIds = new ArrayList<>();
    if (controller != null) {
      for (Holder<Biome> biome : controller.getBiomes()) {
        biome.unwrapKey().ifPresent(key -> biomeIds.add(key.location()));
      }
    }

    // Cache the biomes list
    this.cachedBiomes = controller != null ? new ArrayList<>(controller.getBiomes()) : new ArrayList<>();

    // If no biomes registered, add plains as fallback
    if (cachedBiomes.isEmpty()) {
      MinecraftServer server = Mystcraft.getCurrentServer();
      if (server != null) {
        server.registryAccess().registry(Registry.BIOME_REGISTRY).ifPresent(registry -> {
          Holder<Biome> plains = registry.getHolderOrThrow(Biomes.PLAINS);
          cachedBiomes.add(plains);
          biomeIds.add(Biomes.PLAINS.location());
        });
      }
    }
  }

  /**
   * Helper method to collect biomes from a controller for the super constructor.
   */
  private static Stream<Holder<Biome>> collectBiomesFromController(IBiomeController controller) {
    if (controller != null && !controller.getBiomes().isEmpty()) {
      return controller.getBiomes().stream();
    }
    // Return a fallback stream with plains
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server != null) {
      return server.registryAccess().registry(Registry.BIOME_REGISTRY)
          .map(registry -> Stream.of(registry.getHolderOrThrow(Biomes.PLAINS)))
          .orElse(Stream.empty());
    }
    return Stream.empty();
  }

  /**
   * Reconstruction constructor for codec deserialization.
   * Recreates the biome controller from saved data.
   */
  public AgeBiomeSource(long seed, String controllerType, List<ResourceLocation> biomeIds) {
    // 1.18.2 requires a biome stream in the constructor
    super(collectBiomesFromIds(biomeIds));
    this.seed = seed;
    this.controllerType = controllerType;
    this.biomeIds = new ArrayList<>(biomeIds);
    this.cachedBiomes = new ArrayList<>();

    // Reconstruct biome holders from IDs
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server != null) {
      server.registryAccess().registry(Registry.BIOME_REGISTRY).ifPresent(registry -> {
        for (ResourceLocation biomeId : biomeIds) {
          ResourceKey<Biome> key = ResourceKey.create(Registry.BIOME_REGISTRY, biomeId);
          registry.getHolder(key).ifPresent(cachedBiomes::add);
        }
      });
    }

    // If biomes couldn't be loaded, add plains as fallback
    if (cachedBiomes.isEmpty() && server != null) {
      server.registryAccess().registry(Registry.BIOME_REGISTRY).ifPresent(registry -> {
        Holder<Biome> plains = registry.getHolderOrThrow(Biomes.PLAINS);
        cachedBiomes.add(plains);
      });
    }

    // Reconstruct the biome controller
    reconstructController();
  }

  /**
   * Helper method to collect biomes from IDs for the super constructor.
   */
  private static Stream<Holder<Biome>> collectBiomesFromIds(List<ResourceLocation> biomeIds) {
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server != null && biomeIds != null && !biomeIds.isEmpty()) {
      return server.registryAccess().registry(Registry.BIOME_REGISTRY)
          .map(registry -> {
            List<Holder<Biome>> biomes = new ArrayList<>();
            for (ResourceLocation biomeId : biomeIds) {
              ResourceKey<Biome> key = ResourceKey.create(Registry.BIOME_REGISTRY, biomeId);
              registry.getHolder(key).ifPresent(biomes::add);
            }
            if (biomes.isEmpty()) {
              biomes.add(registry.getHolderOrThrow(Biomes.PLAINS));
            }
            return biomes.stream();
          })
          .orElse(Stream.empty());
    }
    // Fallback to plains
    if (server != null) {
      return server.registryAccess().registry(Registry.BIOME_REGISTRY)
          .map(registry -> Stream.of(registry.getHolderOrThrow(Biomes.PLAINS)))
          .orElse(Stream.empty());
    }
    return Stream.empty();
  }

  /**
   * Reconstructs the biome controller from saved data.
   */
  private void reconstructController() {
    if (cachedBiomes.isEmpty()) {
      Mystcraft.LOGGER.warn("Cannot reconstruct biome controller: no biomes available");
      return;
    }

    switch (controllerType) {
      case "single":
        this.biomeController = new BiomeControllerSingle(cachedBiomes, seed);
        break;
      case "native":
        this.biomeController = new BiomeControllerNative(seed);
        break;
      case "tiny":
        this.biomeController = new BiomeControllerNoise(cachedBiomes, seed, BiomeControllerNoise.Scale.TINY);
        break;
      case "small":
        this.biomeController = new BiomeControllerNoise(cachedBiomes, seed, BiomeControllerNoise.Scale.SMALL);
        break;
      case "medium":
        this.biomeController = new BiomeControllerNoise(cachedBiomes, seed, BiomeControllerNoise.Scale.MEDIUM);
        break;
      case "large":
        this.biomeController = new BiomeControllerNoise(cachedBiomes, seed, BiomeControllerNoise.Scale.LARGE);
        break;
      case "huge":
        this.biomeController = new BiomeControllerNoise(cachedBiomes, seed, BiomeControllerNoise.Scale.HUGE);
        break;
      case "tiled":
        this.biomeController = new BiomeControllerTiled(cachedBiomes, seed);
        break;
      case "grid":
        this.biomeController = new BiomeControllerGrid(cachedBiomes, seed);
        break;
      case "shuffle":
        this.biomeController = new BiomeControllerShuffle(cachedBiomes, seed);
        break;
      default:
        // Default to medium noise
        this.biomeController = new BiomeControllerNoise(cachedBiomes, seed, BiomeControllerNoise.Scale.MEDIUM);
        break;
    }

    Mystcraft.LOGGER.debug("Reconstructed biome controller type '{}' with {} biomes",
        controllerType, cachedBiomes.size());
  }

  @Override
  protected Codec<? extends BiomeSource> codec() {
    return CODEC;
  }

  /**
   * 1.18.2 required method: creates a new biome source with the given seed.
   */
  @Override
  public BiomeSource withSeed(long newSeed) {
    return new AgeBiomeSource(newSeed, controllerType, biomeIds);
  }

  @Override
  public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
    // Convert quart coordinates to block coordinates
    int blockX = quartX << 2;
    int blockZ = quartZ << 2;

    if (biomeController != null) {
      Holder<Biome> biome = biomeController.getBiomeAtCoords(blockX, blockZ);
      if (biome != null) {
        return biome;
      }
    }

    // Fallback to first biome in list or plains
    if (!cachedBiomes.isEmpty()) {
      return cachedBiomes.get(0);
    }

    // Last resort fallback - get plains from registry
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server != null) {
      return server.registryAccess()
          .registry(Registry.BIOME_REGISTRY)
          .map(registry -> registry.getHolderOrThrow(Biomes.PLAINS))
          .orElse(null);
    }

    return null;
  }

  /**
   * Gets the underlying biome controller.
   */
  public IBiomeController getBiomeController() {
    return biomeController;
  }

  /**
   * Sets the biome controller after reconstruction.
   */
  public void setBiomeController(IBiomeController controller) {
    this.biomeController = controller;
    if (controller != null) {
      this.cachedBiomes = new ArrayList<>(controller.getBiomes());
    }
  }

  /**
   * Gets the list of biomes in this source.
   */
  public List<Holder<Biome>> getBiomes() {
    return cachedBiomes;
  }

  /**
   * Gets the controller type string.
   */
  public String getControllerType() {
    return controllerType;
  }
}
