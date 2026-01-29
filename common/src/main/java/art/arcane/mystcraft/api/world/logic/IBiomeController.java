package art.arcane.mystcraft.api.world.logic;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Interface for biome controllers that determine biome distribution in an Age.
 * Implementations define how biomes are distributed across the world based on coordinates.
 */
public interface IBiomeController {

  /**
   * Gets the biome at the specified world coordinates.
   *
   * @param x The X coordinate (block position)
   * @param z The Z coordinate (block position)
   * @return The biome at that location
   */
  Holder<Biome> getBiomeAtCoords(int x, int z);

  /**
   * Gets biomes for a generation region.
   * Used during terrain generation to get biome data for height weighting.
   *
   * @param biomes Array to fill, or null to create new
   * @param x      Starting X coordinate
   * @param z      Starting Z coordinate
   * @param width  Width of region
   * @param height Height of region (in blocks, not Y)
   * @return Array of biomes for the region
   */
  Holder<Biome>[] getBiomesForGeneration(Holder<Biome>[] biomes, int x, int z, int width, int height);

  /**
   * Gets the list of biomes players can spawn in.
   *
   * @return List of valid spawn biomes
   */
  List<Holder<Biome>> getValidSpawnBiomes();

  /**
   * Cleans up any cached biome data.
   * Called periodically to free memory.
   */
  void cleanupCache();

  /**
   * Gets the biome controller type identifier.
   *
   * @return The type name (e.g., "single", "native", "large")
   */
  String getType();

  /**
   * Gets all biomes used by this controller.
   *
   * @return List of biomes
   */
  List<Holder<Biome>> getBiomes();
}
