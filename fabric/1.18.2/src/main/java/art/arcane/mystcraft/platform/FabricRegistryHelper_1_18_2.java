package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IRegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Fabric 1.18.2 implementation of IRegistryHelper.
 * Uses Registry.* static fields instead of BuiltInRegistries and Registries classes.
 * Same API as 1.19.2.
 */
public class FabricRegistryHelper_1_18_2 implements IRegistryHelper {

  @Override
  public Iterable<Block> getBlocks() {
    return Registry.BLOCK;
  }

  @Override
  public Block getBlock(ResourceLocation location) {
    return Registry.BLOCK.get(location);
  }

  @Override
  public ResourceLocation getBlockKey(Block block) {
    return Registry.BLOCK.getKey(block);
  }

  @Override
  public ResourceKey<Registry<Biome>> getBiomeRegistryKey() {
    return Registry.BIOME_REGISTRY;
  }

  @Override
  public ResourceKey<Registry<DimensionType>> getDimensionTypeRegistryKey() {
    return Registry.DIMENSION_TYPE_REGISTRY;
  }

  @Override
  public ResourceKey<Registry<LevelStem>> getLevelStemRegistryKey() {
    return Registry.LEVEL_STEM_REGISTRY;
  }

  @Override
  public ResourceKey<Registry<NoiseGeneratorSettings>> getNoiseSettingsRegistryKey() {
    return Registry.NOISE_GENERATOR_SETTINGS_REGISTRY;
  }
}
