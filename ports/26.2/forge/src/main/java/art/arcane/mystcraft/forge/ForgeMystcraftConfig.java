package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.config.MystcraftConfig;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge-backed common configuration, preserving the legacy config path and defaults. */
final class ForgeMystcraftConfig {

  private static final List<String> DEFAULT_COVERS = List.of(
      "minecraft:leather", "mystcraft:folder");
  private static final List<String> DEFAULT_DISABLED_SYMBOLS = List.of(
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
      "mystcraft:block_minecraft_ancient_debris");
  private static final List<String> DEFAULT_POCKET_PALETTE = List.of(
      "minecraft:oak_planks",
      "minecraft:spruce_planks",
      "minecraft:birch_planks",
      "minecraft:jungle_planks",
      "minecraft:acacia_planks",
      "minecraft:dark_oak_planks",
      "minecraft:mangrove_planks",
      "minecraft:cherry_planks");

  private static final ForgeConfigSpec SPEC;

  private static final ForgeConfigSpec.BooleanValue giveGuidebookOnFirstSpawn;
  private static final ForgeConfigSpec.IntValue maxSymbolsPerBook;
  private static final ForgeConfigSpec.BooleanValue deleteAgesOnStartup;
  private static final ForgeConfigSpec.BooleanValue enablePersonalLinkBooks;
  private static final ForgeConfigSpec.BooleanValue linkPanelInBoosterPacks;
  private static final ForgeConfigSpec.BooleanValue enableBoosterLoot;
  private static final ForgeConfigSpec.BooleanValue enablePageLoot;
  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> bookBinderCoverItems;
  private static final ForgeConfigSpec.BooleanValue allowGravityBlocksInAges;
  private static final ForgeConfigSpec.BooleanValue microDimensionsEnabled;
  private static final ForgeConfigSpec.IntValue microDimensionRadiusChunks;
  private static final ForgeConfigSpec.IntValue microDimensionExtraChunks;
  private static final ForgeConfigSpec.BooleanValue safeStories;
  private static final ForgeConfigSpec.BooleanValue droppedBooksBecomeLivingEntities;
  private static final ForgeConfigSpec.BooleanValue dropBooksOnRead;
  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledSymbols;
  private static final ForgeConfigSpec.BooleanValue proceduralUiEnabled;
  private static final ForgeConfigSpec.BooleanValue proceduralBookCoversEnabled;
  private static final ForgeConfigSpec.BooleanValue proceduralSymbolPagesEnabled;
  private static final ForgeConfigSpec.BooleanValue proceduralSymbolGlyphsEnabled;

  private static final ForgeConfigSpec.IntValue pocketInnerHalfSizeXZ;
  private static final ForgeConfigSpec.IntValue pocketInnerHalfSizeY;
  private static final ForgeConfigSpec.IntValue pocketInnerThickness;
  private static final ForgeConfigSpec.IntValue pocketOuterThickness;
  private static final ForgeConfigSpec.IntValue pocketCenterY;
  private static final ForgeConfigSpec.ConfigValue<List<? extends String>> pocketInnerBlockPalette;
  private static final ForgeConfigSpec.ConfigValue<String> pocketOuterBlock;

  private static final ForgeConfigSpec.BooleanValue instabilityEnabled;
  private static final ForgeConfigSpec.BooleanValue deathEffectsEnabled;
  private static final ForgeConfigSpec.BooleanValue allowUnstableAges;
  private static final ForgeConfigSpec.DoubleValue instabilityMultiplier;
  private static final ForgeConfigSpec.DoubleValue maxAllowedInstability;
  private static final ForgeConfigSpec.DoubleValue thresholdDecay;
  private static final ForgeConfigSpec.DoubleValue thresholdTransmute;
  private static final ForgeConfigSpec.DoubleValue thresholdLightning;
  private static final ForgeConfigSpec.DoubleValue thresholdMeteor;
  private static final ForgeConfigSpec.DoubleValue thresholdPoison;
  private static final ForgeConfigSpec.DoubleValue thresholdWither;
  private static final ForgeConfigSpec.DoubleValue chanceDecay;
  private static final ForgeConfigSpec.DoubleValue chanceTransmute;
  private static final ForgeConfigSpec.DoubleValue chanceLightning;
  private static final ForgeConfigSpec.DoubleValue chanceMeteor;
  private static final ForgeConfigSpec.DoubleValue chancePlayerEffect;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    builder.push("general");
    giveGuidebookOnFirstSpawn = builder.define("giveGuidebookOnFirstSpawn", true);
    maxSymbolsPerBook = builder.defineInRange("maxSymbolsPerBook", 50, -1, 1000);
    deleteAgesOnStartup = builder.define("deleteAgesOnStartup", false);
    enablePersonalLinkBooks = builder.define("enablePersonalLinkBooks", true);
    linkPanelInBoosterPacks = builder.define("linkPanelInBoosterPacks", true);
    enableBoosterLoot = builder.define("enableBoosterLoot", true);
    enablePageLoot = builder.define("enablePageLoot", true);
    bookBinderCoverItems = builder.defineListAllowEmpty(
        "bookBinderCoverItems", DEFAULT_COVERS, ForgeMystcraftConfig::validIdentifier);
    allowGravityBlocksInAges = builder.define("allowGravityBlocksInAges", false);
    microDimensionsEnabled = builder.define("microDimensionsEnabled", false);
    microDimensionRadiusChunks = builder.defineInRange(
        "microDimensionRadiusChunks", 0, 0, 2048);
    microDimensionExtraChunks = builder.defineInRange(
        "microDimensionExtraChunks", 1, 0, 16);
    safeStories = builder.define("safeStories", true);
    droppedBooksBecomeLivingEntities = builder.define(
        "droppedBooksBecomeLivingEntities", true);
    dropBooksOnRead = builder.define("dropBooksOnRead", true);
    disabledSymbols = builder.defineListAllowEmpty(
        "disabledSymbols", DEFAULT_DISABLED_SYMBOLS, ForgeMystcraftConfig::validIdentifier);
    proceduralUiEnabled = builder.define("proceduralUiEnabled", true);
    proceduralBookCoversEnabled = builder.define("proceduralBookCoversEnabled", true);
    proceduralSymbolPagesEnabled = builder.define("proceduralSymbolPagesEnabled", true);
    proceduralSymbolGlyphsEnabled = builder.define("proceduralSymbolGlyphsEnabled", true);
    builder.pop();

    builder.push("personal_pocket");
    pocketInnerHalfSizeXZ = builder.defineInRange("innerHalfSizeXZ", 24, 2, 4096);
    pocketInnerHalfSizeY = builder.defineInRange("innerHalfSizeY", 24, 2, 4096);
    pocketInnerThickness = builder.defineInRange("innerThickness", 3, 1, 32);
    pocketOuterThickness = builder.defineInRange("outerThickness", 5, 1, 32);
    pocketCenterY = builder.defineInRange("centerY", 0, -2032, 2032);
    pocketInnerBlockPalette = builder.defineListAllowEmpty(
        "innerBlockPalette", DEFAULT_POCKET_PALETTE, ForgeMystcraftConfig::validIdentifier);
    pocketOuterBlock = builder.define("outerBlock", "minecraft:bedrock",
        ForgeMystcraftConfig::validIdentifier);
    builder.pop();

    builder.push("instability");
    instabilityEnabled = builder.define("enabled", true);
    deathEffectsEnabled = builder.define("deathEffectsEnabled", true);
    allowUnstableAges = builder.define("allowUnstableAges", true);
    instabilityMultiplier = builder.defineInRange("effectMultiplier", 1.0D, 0.0D, 10.0D);
    maxAllowedInstability = builder.defineInRange(
        "maxAllowedInstability", 150.0D, 0.0D, 1000.0D);
    builder.pop();

    builder.push("instability_thresholds");
    thresholdDecay = builder.defineInRange("decay", 40.0D, 0.0D, 500.0D);
    thresholdTransmute = builder.defineInRange("transmute", 50.0D, 0.0D, 500.0D);
    thresholdLightning = builder.defineInRange("lightning", 70.0D, 0.0D, 500.0D);
    thresholdMeteor = builder.defineInRange("meteor", 70.0D, 0.0D, 500.0D);
    thresholdPoison = builder.defineInRange("poison", 80.0D, 0.0D, 500.0D);
    thresholdWither = builder.defineInRange("wither", 100.0D, 0.0D, 500.0D);
    builder.pop();

    builder.push("instability_chances");
    chanceDecay = builder.defineInRange("decay", 0.001D, 0.0D, 1.0D);
    chanceTransmute = builder.defineInRange("transmute", 0.002D, 0.0D, 1.0D);
    chanceLightning = builder.defineInRange("lightning", 0.0005D, 0.0D, 1.0D);
    chanceMeteor = builder.defineInRange("meteor", 0.0002D, 0.0D, 1.0D);
    chancePlayerEffect = builder.defineInRange("playerEffect", 0.0001D, 0.0D, 1.0D);
    builder.pop();

    SPEC = builder.build();
  }

  private ForgeMystcraftConfig() {
  }

  static void register(FMLJavaModLoadingContext context) {
    context.registerConfig(
        ModConfig.Type.COMMON, SPEC, "mystcraft-common.toml");
    bindCommonSuppliers();
  }

  private static void bindCommonSuppliers() {
    MystcraftConfig.giveGuidebookOnFirstSpawn = giveGuidebookOnFirstSpawn;
    MystcraftConfig.maxSymbolsPerBook = maxSymbolsPerBook;
    MystcraftConfig.deleteAgesOnStartup = deleteAgesOnStartup;
    MystcraftConfig.enablePersonalLinkBooks = enablePersonalLinkBooks;
    MystcraftConfig.linkPanelInBoosterPacks = linkPanelInBoosterPacks;
    MystcraftConfig.enableBoosterLoot = enableBoosterLoot;
    MystcraftConfig.enablePageLoot = enablePageLoot;
    MystcraftConfig.bookBinderCoverItems = () -> List.copyOf(bookBinderCoverItems.get());
    MystcraftConfig.allowGravityBlocksInAges = allowGravityBlocksInAges;
    MystcraftConfig.microDimensionsEnabled = microDimensionsEnabled;
    MystcraftConfig.microDimensionRadiusChunks = microDimensionRadiusChunks;
    MystcraftConfig.microDimensionExtraChunks = microDimensionExtraChunks;
    MystcraftConfig.safeStories = safeStories;
    MystcraftConfig.droppedBooksBecomeLivingEntities = droppedBooksBecomeLivingEntities;
    MystcraftConfig.dropBooksOnRead = dropBooksOnRead;
    MystcraftConfig.disabledSymbols = () -> List.copyOf(disabledSymbols.get());
    MystcraftConfig.proceduralUiEnabled = proceduralUiEnabled;
    MystcraftConfig.proceduralBookCoversEnabled = proceduralBookCoversEnabled;
    MystcraftConfig.proceduralSymbolPagesEnabled = proceduralSymbolPagesEnabled;
    MystcraftConfig.proceduralSymbolGlyphsEnabled = proceduralSymbolGlyphsEnabled;

    MystcraftConfig.pocketInnerHalfSizeXZ = pocketInnerHalfSizeXZ;
    MystcraftConfig.pocketInnerHalfSizeY = pocketInnerHalfSizeY;
    MystcraftConfig.pocketInnerThickness = pocketInnerThickness;
    MystcraftConfig.pocketOuterThickness = pocketOuterThickness;
    MystcraftConfig.pocketCenterY = pocketCenterY;
    MystcraftConfig.pocketInnerBlockPalette = () -> List.copyOf(pocketInnerBlockPalette.get());
    MystcraftConfig.pocketOuterBlock = pocketOuterBlock;

    MystcraftConfig.instabilityEnabled = instabilityEnabled;
    MystcraftConfig.deathEffectsEnabled = deathEffectsEnabled;
    MystcraftConfig.allowUnstableAges = allowUnstableAges;
    MystcraftConfig.instabilityMultiplier = instabilityMultiplier;
    MystcraftConfig.maxAllowedInstability = maxAllowedInstability;
    MystcraftConfig.thresholdDecay = thresholdDecay;
    MystcraftConfig.thresholdTransmute = thresholdTransmute;
    MystcraftConfig.thresholdLightning = thresholdLightning;
    MystcraftConfig.thresholdMeteor = thresholdMeteor;
    MystcraftConfig.thresholdPoison = thresholdPoison;
    MystcraftConfig.thresholdWither = thresholdWither;
    MystcraftConfig.chanceDecay = chanceDecay;
    MystcraftConfig.chanceTransmute = chanceTransmute;
    MystcraftConfig.chanceLightning = chanceLightning;
    MystcraftConfig.chanceMeteor = chanceMeteor;
    MystcraftConfig.chancePlayerEffect = chancePlayerEffect;
  }

  private static boolean validIdentifier(Object value) {
    return value instanceof String string && Identifier.tryParse(string) != null;
  }
}
