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
  public static Supplier<Boolean> linkPanelInBoosterPacks = () -> true;
  public static Supplier<Boolean> enableBoosterLoot = () -> true;
  public static Supplier<Boolean> enablePageLoot = () -> true;

  // Procedural UI / Book covers
  /**
   * Master switch for the procedural UI system. When false, screens still
   * render through the procedural code path (no PNG fallback ships any more)
   * but skip optional flourishes (drop shadows, decorative borders) so the
   * UI stays cheap on potato hardware. Defaults to {@code true}.
   */
  public static Supplier<Boolean> proceduralUiEnabled = () -> true;
  /**
   * When false, {@link art.arcane.mystcraft.client.gui.procedural.BookTextureFactory}
   * renders a uniform leather cover regardless of NBT (cheap escape hatch
   * for resource-pack authors who want every book to look identical, or
   * for low-VRAM clients). Defaults to {@code true}.
   */
  public static Supplier<Boolean> proceduralBookCoversEnabled = () -> true;
  /**
   * Master switch for procedural <em>symbol page</em> rendering (item icon,
   * in-book illustration, and writing-desk thumbnail). When {@code false},
   * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPageTextureFactory}
   * draws only the parchment background — no glyphs, no motif, no
   * flourish, no halo. Pages stay readable as items but lose
   * content-aware art. Defaults to {@code true}.
   */
  public static Supplier<Boolean> proceduralSymbolPagesEnabled = () -> true;
  /**
   * Glyph-render escape hatch. When {@code false}, the procedural glyph
   * pipeline (arcs / spirals / rune strokes from
   * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory})
   * is replaced with a letter-based fallback that renders the first two
   * characters of the symbol's registry path in the centre of each tile
   * using {@code Minecraft.getInstance().font}. Useful for accessibility
   * (high-contrast text), low-end clients (cheaper than primitive
   * composition), and procedural-UI debugging. Defaults to {@code true}.
   */
  public static Supplier<Boolean> proceduralSymbolGlyphsEnabled = () -> true;

  public static Supplier<List<String>> bookBinderCoverItems = () -> List.of(
      "minecraft:leather",
      "mystcraft:folder"
  );
  public static Supplier<Boolean> allowGravityBlocksInAges = () -> false;
  public static Supplier<Boolean> microDimensionsEnabled = () -> false;
  public static Supplier<Integer> microDimensionRadiusChunks = () -> 0;
  public static Supplier<Integer> microDimensionExtraChunks = () -> 1;
  public static Supplier<Boolean> safeStories = () -> true;
  public static Supplier<Boolean> droppedBooksBecomeLivingEntities = () -> true;
  public static Supplier<Boolean> dropBooksOnRead = () -> true;
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
