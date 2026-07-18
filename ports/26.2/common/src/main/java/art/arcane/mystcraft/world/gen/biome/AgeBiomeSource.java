package art.arcane.mystcraft.world.gen.biome;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * BiomeSource implementation that wraps an IBiomeController from the
 * AgeDirector. This allows the Mystcraft biome controller system to integrate
 * with Minecraft's chunk generation pipeline.
 */
public class AgeBiomeSource extends BiomeSource {

  public static final MapCodec<AgeBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(
          Codec.LONG.fieldOf("seed").forGetter(source -> source.seed),
          Codec.STRING.fieldOf("controller_type").forGetter(source -> source.controllerType),
          Identifier.CODEC.listOf().fieldOf("biome_ids").forGetter(source -> source.biomeIds)
      ).apply(instance, AgeBiomeSource::new)
  );

  private final long seed;
  private final String controllerType;
  private final List<Identifier> biomeIds;
  private IBiomeController biomeController;
  private List<Holder<Biome>> cachedBiomes;

  /**
   * Creates an AgeBiomeSource from an existing biome controller. This is the
   * primary constructor used when creating a new Age.
   */
  public AgeBiomeSource(IBiomeController controller, long seed) {
    this.biomeController = controller;
    this.seed = seed;
    this.controllerType = controller != null ? controller.getType() : "native";

    this.biomeIds = new ArrayList<>();
    if (controller != null) {
      for (Holder<Biome> biome : controller.getBiomes()) {
        biome.unwrapKey().ifPresent(key -> biomeIds.add(key.identifier()));
      }
    }

    this.cachedBiomes = controller != null ? new ArrayList<>(controller.getBiomes()) : new ArrayList<>();

    if (cachedBiomes.isEmpty()) {
      MinecraftServer server = Mystcraft.getCurrentServer();
      if (server != null) {
        server.registryAccess().lookup(Registries.BIOME).ifPresent(registry -> {
          Holder<Biome> plains = registry.getOrThrow(Biomes.PLAINS);
          cachedBiomes.add(plains);
          biomeIds.add(Biomes.PLAINS.identifier());
        });
      }
    }
  }

  /**
   * Reconstruction constructor for codec deserialization. Recreates the biome
   * controller from saved data.
   */
  public AgeBiomeSource(long seed, String controllerType, List<Identifier> biomeIds) {
    this.seed = seed;
    this.controllerType = controllerType;
    this.biomeIds = new ArrayList<>(biomeIds);
    this.cachedBiomes = new ArrayList<>();

    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server != null) {
      server.registryAccess().lookup(Registries.BIOME).ifPresent(registry -> {
        for (Identifier biomeId : biomeIds) {
          ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, biomeId);
          registry.get(key).ifPresent(cachedBiomes::add);
        }
      });
    }

    if (cachedBiomes.isEmpty() && server != null) {
      server.registryAccess().lookup(Registries.BIOME).ifPresent(registry -> {
        Holder<Biome> plains = registry.getOrThrow(Biomes.PLAINS);
        cachedBiomes.add(plains);
      });
    }

    reconstructController();
  }

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

        this.biomeController = new BiomeControllerNoise(cachedBiomes, seed, BiomeControllerNoise.Scale.MEDIUM);
        break;
    }

    Mystcraft.LOGGER.debug("Reconstructed biome controller type '{}' with {} biomes",
        controllerType, cachedBiomes.size());
  }

  @Override
  protected MapCodec<? extends BiomeSource> codec() {
    return CODEC;
  }

  @Override
  protected Stream<Holder<Biome>> collectPossibleBiomes() {
    return cachedBiomes.stream();
  }

  @Override
  public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {

    int blockX = quartX << 2;
    int blockZ = quartZ << 2;

    if (biomeController != null) {
      Holder<Biome> biome = biomeController.getBiomeAtCoords(blockX, blockZ);
      if (biome != null) {
        return biome;
      }
    }

    if (!cachedBiomes.isEmpty()) {
      return cachedBiomes.get(0);
    }

    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server != null) {
      return server.registryAccess()
          .lookup(Registries.BIOME)
          .map(registry -> registry.getOrThrow(Biomes.PLAINS))
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
