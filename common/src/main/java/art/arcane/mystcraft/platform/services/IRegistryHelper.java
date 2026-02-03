package art.arcane.mystcraft.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Abstracts registry access across Minecraft versions.
 * <p>
 * 1.20.x: Uses {@code BuiltInRegistries.BLOCK}, {@code Registries.BIOME}, etc.
 * 1.19.x: Uses {@code Registry.BLOCK}, {@code Registry.BIOME_REGISTRY}, etc.
 */
public interface IRegistryHelper {

  /**
   * Gets the block registry for iteration.
   *
   * @return Iterable over all registered blocks
   */
  Iterable<Block> getBlocks();

  /**
   * Gets a block by its resource location.
   *
   * @param location The resource location of the block
   * @return The block, or air if not found
   */
  Block getBlock(ResourceLocation location);

  /**
   * Gets the resource location of a block.
   *
   * @param block The block
   * @return The resource location
   */
  ResourceLocation getBlockKey(Block block);

  /**
   * Gets the biome registry key for use with dynamic registries.
   * <p>
   * 1.20.x: {@code Registries.BIOME}
   * 1.19.x: {@code Registry.BIOME_REGISTRY}
   *
   * @return The biome registry key
   */
  ResourceKey<Registry<Biome>> getBiomeRegistryKey();

  /**
   * Gets the dimension type registry key for use with dynamic registries.
   * <p>
   * 1.20.x: {@code Registries.DIMENSION_TYPE}
   * 1.19.x: {@code Registry.DIMENSION_TYPE_REGISTRY}
   *
   * @return The dimension type registry key
   */
  ResourceKey<Registry<DimensionType>> getDimensionTypeRegistryKey();

  /**
   * Gets the level stem registry key for use with dynamic registries.
   * <p>
   * 1.20.x: {@code Registries.LEVEL_STEM}
   * 1.19.x: {@code Registry.LEVEL_STEM_REGISTRY}
   *
   * @return The level stem registry key
   */
  ResourceKey<Registry<LevelStem>> getLevelStemRegistryKey();

  /**
   * Gets the noise settings registry key for use with dynamic registries.
   * <p>
   * 1.20.x: {@code Registries.NOISE_SETTINGS}
   * 1.19.x: {@code Registry.NOISE_GENERATOR_SETTINGS_REGISTRY}
   *
   * @return The noise settings registry key
   */
  ResourceKey<Registry<NoiseGeneratorSettings>> getNoiseSettingsRegistryKey();
}
