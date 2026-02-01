package art.arcane.mystcraft.config;

import art.arcane.mystcraft.Mystcraft;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fabric-side configuration for Mystcraft (1.20.1).
 * Uses TOML file storage to match Forge/NeoForge format.
 * Fields expose .get() methods to match the ForgeConfigSpec API used by common code.
 */
public class FabricMystcraftConfig {

  // --- General ---
  public static final BooleanValue giveGuidebookOnFirstSpawn = new BooleanValue(true);
  public static final IntValue maxSymbolsPerBook = new IntValue(50);
  public static final BooleanValue deleteAgesOnStartup = new BooleanValue(false);
  public static final BooleanValue enablePersonalLinkBooks = new BooleanValue(true);
  public static final BooleanValue linkPanelInBoosterPacks = new BooleanValue(true);
  public static final BooleanValue enableBoosterLoot = new BooleanValue(true);
  public static final BooleanValue enablePageLoot = new BooleanValue(true);
  public static final StringListValue bookBinderCoverItems = new StringListValue(new ArrayList<>(List.of(
      "minecraft:leather",
      "mystcraft:folder"
  )));
  public static final BooleanValue allowGravityBlocksInAges = new BooleanValue(false);
  public static final BooleanValue microDimensionsEnabled = new BooleanValue(false);
  public static final IntValue microDimensionRadiusChunks = new IntValue(0);
  public static final IntValue microDimensionExtraChunks = new IntValue(1);
  public static final BooleanValue safeStories = new BooleanValue(true);
  public static final BooleanValue droppedBooksBecomeLivingEntities = new BooleanValue(true);
  public static final BooleanValue dropBooksOnRead = new BooleanValue(true);
  // --- Personal Pocket Dimension ---
  public static final IntValue pocketInnerHalfSizeXZ = new IntValue(24);
  public static final IntValue pocketInnerHalfSizeY = new IntValue(24);
  public static final IntValue pocketInnerThickness = new IntValue(3);
  public static final IntValue pocketOuterThickness = new IntValue(5);
  public static final IntValue pocketCenterY = new IntValue(0);
  public static final StringListValue pocketInnerBlockPalette = new StringListValue(new ArrayList<>(List.of(
      "minecraft:oak_planks",
      "minecraft:spruce_planks",
      "minecraft:birch_planks",
      "minecraft:jungle_planks",
      "minecraft:acacia_planks",
      "minecraft:dark_oak_planks",
      "minecraft:mangrove_planks",
      "minecraft:cherry_planks"
  )));
  public static final StringValue pocketOuterBlock = new StringValue("minecraft:bedrock");
  // --- Instability ---
  public static final BooleanValue instabilityEnabled = new BooleanValue(true);
  public static final BooleanValue deathEffectsEnabled = new BooleanValue(true);
  public static final BooleanValue allowUnstableAges = new BooleanValue(true);
  public static final DoubleValue instabilityMultiplier = new DoubleValue(1.0);
  public static final DoubleValue maxAllowedInstability = new DoubleValue(150.0);
  // --- Instability Thresholds ---
  public static final DoubleValue thresholdDecay = new DoubleValue(40.0);
  public static final DoubleValue thresholdTransmute = new DoubleValue(50.0);
  public static final DoubleValue thresholdLightning = new DoubleValue(70.0);
  public static final DoubleValue thresholdMeteor = new DoubleValue(70.0);
  public static final DoubleValue thresholdPoison = new DoubleValue(80.0);
  public static final DoubleValue thresholdWither = new DoubleValue(100.0);
  // --- Effect Chances ---
  public static final DoubleValue chanceDecay = new DoubleValue(0.001);
  public static final DoubleValue chanceTransmute = new DoubleValue(0.002);
  public static final DoubleValue chanceLightning = new DoubleValue(0.0005);
  public static final DoubleValue chanceMeteor = new DoubleValue(0.0002);
  public static final DoubleValue chancePlayerEffect = new DoubleValue(0.0001);
  private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mystcraft-common.toml");
  // --- Symbols ---
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
      "mystcraft:block_minecraft_ancient_debris"
  );
  public static final StringListValue disabledSymbols = new StringListValue(new ArrayList<>(DEFAULT_DISABLED_SYMBOLS));

  private FabricMystcraftConfig() {
  }

  /**
   * Loads configuration from disk. Creates default file if it does not exist.
   */
  public static void load() {
    CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_PATH)
        .sync()
        .autosave()
        .writingMode(WritingMode.REPLACE)
        .build();

    config.load();

    // Set defaults and read values

    // --- General ---
    setCommentAndDefault(config, "general.giveGuidebookOnFirstSpawn", giveGuidebookOnFirstSpawn.defaultValue,
        "Whether to give new players a copy of the Mystcraft Guidebook when they first join the world.");
    giveGuidebookOnFirstSpawn.set(config.getOrElse("general.giveGuidebookOnFirstSpawn", giveGuidebookOnFirstSpawn.defaultValue));

    setCommentAndDefault(config, "general.maxSymbolsPerBook", maxSymbolsPerBook.defaultValue,
        "Maximum number of symbol pages allowed in a single Agebook. Set to -1 for unlimited.");
    maxSymbolsPerBook.set(config.getOrElse("general.maxSymbolsPerBook", maxSymbolsPerBook.defaultValue));

    setCommentAndDefault(config, "general.deleteAgesOnStartup", deleteAgesOnStartup.defaultValue,
        "If true, all Mystcraft Ages will be deleted every time the server starts. Use for development/testing.");
    deleteAgesOnStartup.set(config.getOrElse("general.deleteAgesOnStartup", deleteAgesOnStartup.defaultValue));

    setCommentAndDefault(config, "general.enablePersonalLinkBooks", enablePersonalLinkBooks.defaultValue,
        "If true, personal link books and personal pocket dimensions are enabled.");
    enablePersonalLinkBooks.set(config.getOrElse("general.enablePersonalLinkBooks", enablePersonalLinkBooks.defaultValue));

    setCommentAndDefault(config, "general.linkPanelInBoosterPacks", linkPanelInBoosterPacks.defaultValue,
        "If true, booster packs have a chance to contain link panels. If false, only symbol pages.");
    linkPanelInBoosterPacks.set(config.getOrElse("general.linkPanelInBoosterPacks", linkPanelInBoosterPacks.defaultValue));

    setCommentAndDefault(config, "general.enableBoosterLoot", enableBoosterLoot.defaultValue,
        "If true, booster packs can be found in dungeon chests and as mob drops.");
    enableBoosterLoot.set(config.getOrElse("general.enableBoosterLoot", enableBoosterLoot.defaultValue));

    setCommentAndDefault(config, "general.enablePageLoot", enablePageLoot.defaultValue,
        "If true, symbol pages can be found in dungeon chests.");
    enablePageLoot.set(config.getOrElse("general.enablePageLoot", enablePageLoot.defaultValue));

    setCommentAndDefault(config, "general.bookBinderCoverItems", new ArrayList<>(bookBinderCoverItems.defaultValue),
        "List of item IDs that can be used as book covers in the Book Binder.");
    bookBinderCoverItems.set(config.getOrElse("general.bookBinderCoverItems", new ArrayList<>(bookBinderCoverItems.defaultValue)));

    setCommentAndDefault(config, "general.allowGravityBlocksInAges", allowGravityBlocksInAges.defaultValue,
        "If true, gravity blocks (sand, gravel, anvils, concrete powder) can fall in Mystcraft Ages.");
    allowGravityBlocksInAges.set(config.getOrElse("general.allowGravityBlocksInAges", allowGravityBlocksInAges.defaultValue));

    setCommentAndDefault(config, "general.microDimensionsEnabled", microDimensionsEnabled.defaultValue,
        "If true, newly created Ages are limited to a small chunk radius around the spawn chunk.");
    microDimensionsEnabled.set(config.getOrElse("general.microDimensionsEnabled", microDimensionsEnabled.defaultValue));

    setCommentAndDefault(config, "general.microDimensionRadiusChunks", microDimensionRadiusChunks.defaultValue,
        "Radius in chunks from the spawn chunk center for micro dimensions. 0 = single chunk.");
    microDimensionRadiusChunks.set(config.getOrElse("general.microDimensionRadiusChunks", microDimensionRadiusChunks.defaultValue));

    setCommentAndDefault(config, "general.microDimensionExtraChunks", microDimensionExtraChunks.defaultValue,
        "Additional chunk rings generated beyond the border for visual continuity.");
    microDimensionExtraChunks.set(config.getOrElse("general.microDimensionExtraChunks", microDimensionExtraChunks.defaultValue));

    setCommentAndDefault(config, "general.safeStories", safeStories.defaultValue,
        "If true, players who die or fall into the void in Mystcraft Ages are returned to where they linked from.");
    safeStories.set(config.getOrElse("general.safeStories", safeStories.defaultValue));

    setCommentAndDefault(config, "general.droppedBooksBecomeLivingEntities", droppedBooksBecomeLivingEntities.defaultValue,
        "If true, dropped linkbooks/agebooks become living book entities instead of normal item drops.");
    droppedBooksBecomeLivingEntities.set(config.getOrElse("general.droppedBooksBecomeLivingEntities", droppedBooksBecomeLivingEntities.defaultValue));

    setCommentAndDefault(config, "general.dropBooksOnRead", dropBooksOnRead.defaultValue,
        "If true, reading a linkbook drops it into the world. If false, it stays in your inventory.");
    dropBooksOnRead.set(config.getOrElse("general.dropBooksOnRead", dropBooksOnRead.defaultValue));

    setCommentAndDefault(config, "general.disabledSymbols", new ArrayList<>(DEFAULT_DISABLED_SYMBOLS),
        "List of symbol IDs to disable. Disabled symbols are hidden from books and not registered at runtime.");
    disabledSymbols.set(config.getOrElse("general.disabledSymbols", new ArrayList<>(DEFAULT_DISABLED_SYMBOLS)));

    // --- Personal Pocket ---
    setCommentAndDefault(config, "personal_pocket.innerHalfSizeXZ", pocketInnerHalfSizeXZ.defaultValue,
        "Half the inner void space horizontally (X/Z axes). Range: 2-4096. Default: 24 (48 block diameter).");
    pocketInnerHalfSizeXZ.set(config.getOrElse("personal_pocket.innerHalfSizeXZ", pocketInnerHalfSizeXZ.defaultValue));

    setCommentAndDefault(config, "personal_pocket.innerHalfSizeY", pocketInnerHalfSizeY.defaultValue,
        "Half the inner void space vertically (Y axis). Range: 2-4096. Minecraft 1.20.1 max is ~4048 due to dimension height.");
    pocketInnerHalfSizeY.set(config.getOrElse("personal_pocket.innerHalfSizeY", pocketInnerHalfSizeY.defaultValue));

    setCommentAndDefault(config, "personal_pocket.innerThickness", pocketInnerThickness.defaultValue,
        "Thickness of the inner shell surrounding the void. Default: 3 blocks.");
    pocketInnerThickness.set(config.getOrElse("personal_pocket.innerThickness", pocketInnerThickness.defaultValue));

    setCommentAndDefault(config, "personal_pocket.outerThickness", pocketOuterThickness.defaultValue,
        "Thickness of the outer shell surrounding the inner layer. Default: 5 blocks.");
    pocketOuterThickness.set(config.getOrElse("personal_pocket.outerThickness", pocketOuterThickness.defaultValue));

    setCommentAndDefault(config, "personal_pocket.centerY", pocketCenterY.defaultValue,
        "Y coordinate of the pocket center. Range: -2032 to 2032. Default: 0 (centered in dimension).");
    pocketCenterY.set(config.getOrElse("personal_pocket.centerY", pocketCenterY.defaultValue));

    setCommentAndDefault(config, "personal_pocket.innerBlockPalette", new ArrayList<>(pocketInnerBlockPalette.defaultValue),
        "List of block IDs for the inner shell layer. Multiple blocks create a varied pattern.");
    pocketInnerBlockPalette.set(config.getOrElse("personal_pocket.innerBlockPalette", new ArrayList<>(pocketInnerBlockPalette.defaultValue)));

    setCommentAndDefault(config, "personal_pocket.outerBlock", pocketOuterBlock.defaultValue,
        "Block ID for the outer shell. Default: minecraft:bedrock");
    pocketOuterBlock.set(config.getOrElse("personal_pocket.outerBlock", pocketOuterBlock.defaultValue));

    // --- Instability ---
    setCommentAndDefault(config, "instability.enabled", instabilityEnabled.defaultValue,
        "Master switch for the instability system. If false, no instability effects occur.");
    instabilityEnabled.set(config.getOrElse("instability.enabled", instabilityEnabled.defaultValue));

    setCommentAndDefault(config, "instability.deathEffectsEnabled", deathEffectsEnabled.defaultValue,
        "Whether thematic death effects occur when players die in Ages.");
    deathEffectsEnabled.set(config.getOrElse("instability.deathEffectsEnabled", deathEffectsEnabled.defaultValue));

    setCommentAndDefault(config, "instability.allowUnstableAges", allowUnstableAges.defaultValue,
        "Whether to allow creation of Ages that exceed the maximum instability threshold.");
    allowUnstableAges.set(config.getOrElse("instability.allowUnstableAges", allowUnstableAges.defaultValue));

    setCommentAndDefault(config, "instability.effectMultiplier", instabilityMultiplier.defaultValue,
        "Global multiplier for all instability effect chances. 1.0 = normal frequency.");
    instabilityMultiplier.set(config.getOrElse("instability.effectMultiplier", instabilityMultiplier.defaultValue));

    setCommentAndDefault(config, "instability.maxAllowedInstability", maxAllowedInstability.defaultValue,
        "Maximum instability allowed when 'allowUnstableAges' is false.");
    maxAllowedInstability.set(config.getOrElse("instability.maxAllowedInstability", maxAllowedInstability.defaultValue));

    // --- Instability Thresholds ---
    setCommentAndDefault(config, "instability_thresholds.decay", thresholdDecay.defaultValue,
        "Instability threshold for decay blocks to start spreading.");
    thresholdDecay.set(config.getOrElse("instability_thresholds.decay", thresholdDecay.defaultValue));

    setCommentAndDefault(config, "instability_thresholds.transmute", thresholdTransmute.defaultValue,
        "Instability threshold for random block transmutation to begin.");
    thresholdTransmute.set(config.getOrElse("instability_thresholds.transmute", thresholdTransmute.defaultValue));

    setCommentAndDefault(config, "instability_thresholds.lightning", thresholdLightning.defaultValue,
        "Instability threshold for random lightning strikes.");
    thresholdLightning.set(config.getOrElse("instability_thresholds.lightning", thresholdLightning.defaultValue));

    setCommentAndDefault(config, "instability_thresholds.meteor", thresholdMeteor.defaultValue,
        "Instability threshold for meteor falls.");
    thresholdMeteor.set(config.getOrElse("instability_thresholds.meteor", thresholdMeteor.defaultValue));

    setCommentAndDefault(config, "instability_thresholds.poison", thresholdPoison.defaultValue,
        "Instability threshold for poison/hunger effects on players.");
    thresholdPoison.set(config.getOrElse("instability_thresholds.poison", thresholdPoison.defaultValue));

    setCommentAndDefault(config, "instability_thresholds.wither", thresholdWither.defaultValue,
        "Instability threshold for wither effects on players (most severe).");
    thresholdWither.set(config.getOrElse("instability_thresholds.wither", thresholdWither.defaultValue));

    // --- Effect Chances ---
    setCommentAndDefault(config, "instability_chances.decay", chanceDecay.defaultValue,
        "Base chance per tick for decay to spread (0.001 = 0.1%).");
    chanceDecay.set(config.getOrElse("instability_chances.decay", chanceDecay.defaultValue));

    setCommentAndDefault(config, "instability_chances.transmute", chanceTransmute.defaultValue,
        "Base chance per tick for block transmutation (0.002 = 0.2%).");
    chanceTransmute.set(config.getOrElse("instability_chances.transmute", chanceTransmute.defaultValue));

    setCommentAndDefault(config, "instability_chances.lightning", chanceLightning.defaultValue,
        "Base chance per tick for lightning strikes (0.0005 = 0.05%).");
    chanceLightning.set(config.getOrElse("instability_chances.lightning", chanceLightning.defaultValue));

    setCommentAndDefault(config, "instability_chances.meteor", chanceMeteor.defaultValue,
        "Base chance per tick for meteor spawns (0.0002 = 0.02%).");
    chanceMeteor.set(config.getOrElse("instability_chances.meteor", chanceMeteor.defaultValue));

    setCommentAndDefault(config, "instability_chances.playerEffect", chancePlayerEffect.defaultValue,
        "Base chance per tick for player debuffs (0.0001 = 0.01%).");
    chancePlayerEffect.set(config.getOrElse("instability_chances.playerEffect", chancePlayerEffect.defaultValue));

    config.save();
    config.close();

    Mystcraft.LOGGER.info("[FabricMystcraftConfig] Loaded config from {}", CONFIG_PATH);
  }

  private static <T> void setCommentAndDefault(CommentedFileConfig config, String path, T defaultValue, String comment) {
    config.setComment(path, comment);
    if (!config.contains(path)) {
      config.set(path, defaultValue);
    }
  }

  // --- Value wrapper types matching ForgeConfigSpec API ---

  public static final class BooleanValue {
    private final boolean defaultValue;
    private boolean value;

    public BooleanValue(boolean defaultValue) {
      this.defaultValue = defaultValue;
      this.value = defaultValue;
    }

    public boolean get() {
      return value;
    }

    public void set(boolean value) {
      this.value = value;
    }
  }

  public static final class IntValue {
    private final int defaultValue;
    private int value;

    public IntValue(int defaultValue) {
      this.defaultValue = defaultValue;
      this.value = defaultValue;
    }

    public int get() {
      return value;
    }

    public void set(int value) {
      this.value = value;
    }
  }

  public static final class DoubleValue {
    private final double defaultValue;
    private double value;

    public DoubleValue(double defaultValue) {
      this.defaultValue = defaultValue;
      this.value = defaultValue;
    }

    public Double get() {
      return value;
    }

    public void set(double value) {
      this.value = value;
    }
  }

  public static final class StringListValue {
    private final List<String> defaultValue;
    private List<String> value;

    public StringListValue(List<String> defaultValue) {
      this.defaultValue = new ArrayList<>(defaultValue);
      this.value = new ArrayList<>(defaultValue);
    }

    public List<String> get() {
      return value;
    }

    public void set(List<String> value) {
      this.value = new ArrayList<>(value);
    }
  }

  public static final class StringValue {
    private final String defaultValue;
    private String value;

    public StringValue(String defaultValue) {
      this.defaultValue = defaultValue;
      this.value = defaultValue;
    }

    public String get() {
      return value;
    }

    public void set(String value) {
      this.value = value;
    }
  }
}
