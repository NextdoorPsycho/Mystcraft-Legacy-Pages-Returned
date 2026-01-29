package art.arcane.mystcraft.config;

import java.util.List;
import java.util.function.Supplier;

/**
 * Common config accessor for Mystcraft settings.
 * Platform modules populate these suppliers during initialization.
 * Each supplier wraps the platform-specific config value (ForgeConfigSpec, JSON, etc.).
 */
public final class MystcraftConfig {

  // General
  public static Supplier<Boolean> giveGuidebookOnFirstSpawn = () -> true;
  public static Supplier<Integer> maxSymbolsPerBook = () -> 50;
  public static Supplier<Boolean> deleteAgesOnStartup = () -> false;
  public static Supplier<Boolean> enablePersonalLinkBooks = () -> true;
  public static Supplier<Boolean> allowGravityBlocksInAges = () -> false;
  public static Supplier<Boolean> microDimensionsEnabled = () -> false;
  public static Supplier<Integer> microDimensionRadiusChunks = () -> 0;
  public static Supplier<Integer> microDimensionExtraChunks = () -> 1;
  public static Supplier<Boolean> safeStories = () -> true;
  // Ore block terrain symbols are disabled by default as they are overpowered
  public static Supplier<List<String>> disabledSymbols = () -> List.of(
      // Ore storage blocks
      "mystcraft:block_minecraft_coal_block",
      "mystcraft:block_minecraft_copper_block",
      "mystcraft:block_minecraft_diamond_block",
      "mystcraft:block_minecraft_emerald_block",
      "mystcraft:block_minecraft_gold_block",
      "mystcraft:block_minecraft_iron_block",
      "mystcraft:block_minecraft_lapis_block",
      "mystcraft:block_minecraft_netherite_block",
      "mystcraft:block_minecraft_raw_copper_block",
      "mystcraft:block_minecraft_raw_gold_block",
      "mystcraft:block_minecraft_raw_iron_block",
      "mystcraft:block_minecraft_redstone_block",
      // Ancient debris
      "mystcraft:block_minecraft_ancient_debris"
  );

  // Personal Pocket Dimension
  public static Supplier<Integer> pocketInnerHalfSizeXZ = () -> 24;
  public static Supplier<Integer> pocketInnerHalfSizeY = () -> 24;
  public static Supplier<Integer> pocketInnerThickness = () -> 3;
  public static Supplier<Integer> pocketOuterThickness = () -> 5;
  public static Supplier<Integer> pocketCenterY = () -> 0;
  public static Supplier<List<String>> pocketInnerBlockPalette = () -> List.of(
      "minecraft:oak_planks",
      "minecraft:spruce_planks",
      "minecraft:birch_planks",
      "minecraft:jungle_planks",
      "minecraft:acacia_planks",
      "minecraft:dark_oak_planks",
      "minecraft:mangrove_planks",
      "minecraft:cherry_planks"
  );
  public static Supplier<String> pocketOuterBlock = () -> "minecraft:bedrock";

  // Instability
  public static Supplier<Boolean> instabilityEnabled = () -> true;
  public static Supplier<Boolean> deathEffectsEnabled = () -> true;
  public static Supplier<Boolean> allowUnstableAges = () -> true;
  public static Supplier<Double> instabilityMultiplier = () -> 1.0;
  public static Supplier<Double> maxAllowedInstability = () -> 150.0;

  // Instability thresholds
  public static Supplier<Double> thresholdDecay = () -> 40.0;
  public static Supplier<Double> thresholdTransmute = () -> 50.0;
  public static Supplier<Double> thresholdLightning = () -> 70.0;
  public static Supplier<Double> thresholdMeteor = () -> 70.0;
  public static Supplier<Double> thresholdPoison = () -> 80.0;
  public static Supplier<Double> thresholdWither = () -> 100.0;

  // Effect chances
  public static Supplier<Double> chanceDecay = () -> 0.001;
  public static Supplier<Double> chanceTransmute = () -> 0.002;
  public static Supplier<Double> chanceLightning = () -> 0.0005;
  public static Supplier<Double> chanceMeteor = () -> 0.0002;
  public static Supplier<Double> chancePlayerEffect = () -> 0.0001;

  private MystcraftConfig() {
  }
}
