package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IRegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Forge 1.20.1 implementation of IRegistryHelper. Uses BuiltInRegistries for
 * static registries and Registries for dynamic registry keys.
 */
public class ForgeRegistryHelper_1_20_1 implements IRegistryHelper {

  @Override
  public Iterable<Block> getBlocks() {
    return BuiltInRegistries.BLOCK;
  }

  @Override
  public Block getBlock(ResourceLocation location) {
    return BuiltInRegistries.BLOCK.get(location);
  }

  @Override
  public ResourceLocation getBlockKey(Block block) {
    return BuiltInRegistries.BLOCK.getKey(block);
  }

  @Override
  public ResourceKey<Registry<Biome>> getBiomeRegistryKey() {
    return Registries.BIOME;
  }

  @Override
  public ResourceKey<Registry<DimensionType>> getDimensionTypeRegistryKey() {
    return Registries.DIMENSION_TYPE;
  }

  @Override
  public ResourceKey<Registry<LevelStem>> getLevelStemRegistryKey() {
    return Registries.LEVEL_STEM;
  }

  @Override
  public ResourceKey<Registry<NoiseGeneratorSettings>> getNoiseSettingsRegistryKey() {
    return Registries.NOISE_SETTINGS;
  }
}
