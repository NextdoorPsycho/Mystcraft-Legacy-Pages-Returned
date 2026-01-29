package art.arcane.mystcraft.world.gen.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Biome controller that delegates to the overworld's native biome generation.
 * Uses the server's overworld biome source for authentic Minecraft biome distribution.
 */
public class BiomeControllerNative extends BiomeControllerBase {

  private BiomeSource delegateBiomeSource;
  private Registry<Biome> biomeRegistry;

  /**
   * Creates a native biome controller.
   * The biome source will be initialized when first queried with a server level.
   */
  public BiomeControllerNative(long seed) {
    super(Collections.emptyList(), seed);
  }

  /**
   * Initializes the delegate biome source from a server level.
   * Should be called once the world is available.
   */
  public void initializeFromLevel(ServerLevel level) {
    if (delegateBiomeSource == null) {
      // Get the overworld's biome source
      ServerLevel overworld = level.getServer().overworld();
      this.delegateBiomeSource = overworld.getChunkSource().getGenerator().getBiomeSource();
      this.biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
    }
  }

  @Override
  public Holder<Biome> getBiomeAtCoords(int x, int z) {
    if (delegateBiomeSource != null) {
      // Use the delegate's biome generation
      // Note: BiomeSource.getNoiseBiome uses quarter-resolution coordinates
      return delegateBiomeSource.getNoiseBiome(x >> 2, 64 >> 2, z >> 2, null);
    }
    return null;
  }

  @Override
  public List<Holder<Biome>> getBiomes() {
    if (delegateBiomeSource != null) {
      return new ArrayList<>(delegateBiomeSource.possibleBiomes());
    }
    return Collections.emptyList();
  }

  @Override
  public List<Holder<Biome>> getValidSpawnBiomes() {
    // Return all possible biomes from the delegate
    return getBiomes();
  }

  @Override
  public String getType() {
    return "native";
  }
}
