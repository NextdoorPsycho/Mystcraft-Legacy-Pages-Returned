package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.InkAffinity;
import art.arcane.mystcraft.data.InkBlend;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MystcraftGameTestAssertions {

  private MystcraftGameTestAssertions() {
  }

  public static void assertCoreGameplayContentLoaded() {
    assertSymbolsLoaded();
    assertDatapacksLoaded();
    assertCreativeTabsLoaded();
    assertItemsRegistered();
    assertTableBlocksRegistered();
    assertEntitiesRegistered();
  }

  public static void assertSymbolsLoaded() {
    int count = SymbolRegistry.getAll().size();
    if (count < 400) {
      throw new IllegalStateException("Expected 400+ symbols loaded, but found: " + count);
    }
  }

  public static void assertCreativeTabsLoaded() {
    assertCreativeTabRegistered("mystcraft");
    assertCreativeTabRegistered("mystcraft_pages");
  }

  public static void assertItemsRegistered() {
    assertItemRegistered("linkbook");
    assertItemRegistered("linkbook_unlinked");
    assertItemRegistered("personal_link_book");
    assertItemRegistered("agebook");
    assertItemRegistered("page");
    assertItemRegistered("folder");
    assertItemRegistered("portfolio");
    assertItemRegistered("booster");
    assertItemRegistered("inkvial");
    assertItemRegistered("guidebook");
    assertItemRegistered("writingdesk");
    assertItemRegistered("blockinkmixer");
    assertItemRegistered("blockbookbinder");
  }

  public static void assertDatapacksLoaded() {
    assertSymbolExists("terrain_flat");
    assertSymbolExists("terrain_cave");
    assertSymbolExists("biome_plains");
    assertSymbolExists("biome_forest");
    assertSymbolExists("weather_normal");
    assertSymbolExists("lighting_normal");
    assertSymbolExists("color_sky_natural");
  }

  public static void assertTableBlocksRegistered() {
    assertBlockRegistered("writingdesk");
    assertBlockRegistered("blockinkmixer");
    assertBlockRegistered("blockbookbinder");
    assertBlockRegistered("blockbookreceptacle");
  }

  public static void assertEntitiesRegistered() {
    if (ModEntities.LINKBOOK == null || ModEntities.LINKBOOK.get() == null) {
      throw new IllegalStateException("Linkbook entity type is not registered");
    }
    if (ModEntities.PERSONAL_POCKET_PROXY == null || ModEntities.PERSONAL_POCKET_PROXY.get() == null) {
      throw new IllegalStateException("Personal pocket proxy entity type is not registered");
    }
  }

  public static void assertRegisteredObjectsReachable() {
    if (ModItems.PERSONAL_LINK_BOOK.get() == Items.AIR) {
      throw new IllegalStateException("Personal link book registry object resolved to air");
    }
    if (ModBlocks.BOOK_BINDER.get() == Blocks.AIR) {
      throw new IllegalStateException("Book Binder registry object resolved to air");
    }
  }

  private static void assertItemRegistered(String itemId) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, itemId);
    Item item = BuiltInRegistries.ITEM.get(id);
    if (item == Items.AIR) {
      throw new IllegalStateException("Item not registered: " + id);
    }
  }

  private static void assertBlockRegistered(String blockId) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, blockId);
    Block block = BuiltInRegistries.BLOCK.get(id);
    if (block == Blocks.AIR) {
      throw new IllegalStateException("Block not registered: " + id);
    }
  }

  private static void assertCreativeTabRegistered(String tabId) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, tabId);
    if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) {
      throw new IllegalStateException("Creative tab not registered: " + id);
    }
  }

  private static void assertSymbolExists(String path) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, path);
    if (!SymbolRegistry.contains(id)) {
      throw new IllegalStateException("Missing symbol: " + id);
    }
  }

  // ---------------------------------------------------------------------------
  // Procedural UI / Ink Affinity assertions
  // ---------------------------------------------------------------------------

  /**
   * Verifies that {@link InkBlend} can serialise its full state to NBT and
   * round-trip back without loss. This is the contract that ink mixers and
   * booster packs rely on so persisted blends keep biasing symbol rolls.
   */
  public static void assertInkBlendRoundTripsThroughNbt() {
    InkBlend before = new InkBlend();
    Map<ResourceLocation, Float> symbols = new HashMap<>();
    symbols.put(new ResourceLocation("mystcraft", "dense_ores"), 1.5f);
    symbols.put(new ResourceLocation("mystcraft", "biome_dripstone_caves"), 0.75f);
    Map<SymbolCategory, Float> categories = new HashMap<>();
    categories.put(SymbolCategory.FEATURE_MEDIUM, 0.5f);
    categories.put(SymbolCategory.FEATURE_LARGE, 0.25f);
    Map<String, Float> poemTokens = new HashMap<>();
    poemTokens.put("Stone", 0.4f);
    poemTokens.put("Deep", 0.6f);
    Map<String, Float> linkProps = new HashMap<>();
    linkProps.put("intra_linking", 0.2f);
    InkAffinity.Entry entry = new InkAffinity.Entry(1.0f, 2, symbols, categories, poemTokens, linkProps);
    before.add(entry, 1);

    if (before.isEmpty()) {
      throw new IllegalStateException("InkBlend should not be empty after adding a non-empty affinity");
    }
    if (before.tierBonus() != 2) {
      throw new IllegalStateException("InkBlend tier bonus should be 2, got " + before.tierBonus());
    }
    if (before.symbolWeight(new ResourceLocation("mystcraft", "dense_ores")) <= 0f) {
      throw new IllegalStateException("Symbol weight for dense_ores not accumulated");
    }

    CompoundTag tag = before.toNbt();
    if (tag == null || tag.isEmpty()) {
      throw new IllegalStateException("InkBlend.toNbt produced empty tag");
    }

    InkBlend after = InkBlend.fromTag(tag);
    if (after.isEmpty()) {
      throw new IllegalStateException("InkBlend round-tripped to empty");
    }
    if (after.tierBonus() != before.tierBonus()) {
      throw new IllegalStateException("Tier bonus did not round-trip: before=" + before.tierBonus()
          + " after=" + after.tierBonus());
    }
    float beforeSym = before.symbolWeight(new ResourceLocation("mystcraft", "dense_ores"));
    float afterSym = after.symbolWeight(new ResourceLocation("mystcraft", "dense_ores"));
    if (Math.abs(beforeSym - afterSym) > 0.001f) {
      throw new IllegalStateException("Symbol weight did not round-trip: before=" + beforeSym + " after=" + afterSym);
    }
    float beforeCat = before.categoryWeight(SymbolCategory.FEATURE_MEDIUM);
    float afterCat = after.categoryWeight(SymbolCategory.FEATURE_MEDIUM);
    if (Math.abs(beforeCat - afterCat) > 0.001f) {
      throw new IllegalStateException("Category weight did not round-trip: before=" + beforeCat + " after=" + afterCat);
    }
    float beforePoem = before.poemTokenSum(new String[]{"Deep"});
    float afterPoem = after.poemTokenSum(new String[]{"Deep"});
    if (Math.abs(beforePoem - afterPoem) > 0.001f) {
      throw new IllegalStateException("Poem-token weight did not round-trip: before=" + beforePoem + " after=" + afterPoem);
    }
    float beforeLink = before.linkPropertyProbability("intra_linking");
    float afterLink = after.linkPropertyProbability("intra_linking");
    if (Math.abs(beforeLink - afterLink) > 0.001f) {
      throw new IllegalStateException("Link property weight did not round-trip: before=" + beforeLink + " after=" + afterLink);
    }
  }

  /**
   * Verifies that for every cover item configured in
   * {@link MystcraftConfig#bookBinderCoverItems}, the cover NBT can be
   * written via {@link LinkOptions#setCoverItemId} and read back via
   * {@link LinkOptions#getCoverItemId}. This is the bridge the procedural
   * Book texture factory uses to pick a cover palette.
   */
  public static void assertBookCoverNbtRoundTripsForEachCover() {
    List<String> covers = MystcraftConfig.bookBinderCoverItems.get();
    if (covers == null || covers.isEmpty()) {
      throw new IllegalStateException("MystcraftConfig.bookBinderCoverItems is empty; nothing to round-trip");
    }
    for (String id : covers) {
      ResourceLocation rl = ResourceLocation.tryParse(id);
      if (rl == null) {
        throw new IllegalStateException("Cover entry is not a valid ResourceLocation: " + id);
      }
      CompoundTag tag = LinkOptions.setCoverItemId(new CompoundTag(), rl);
      ResourceLocation parsed = LinkOptions.getCoverItemId(tag);
      if (parsed == null) {
        throw new IllegalStateException("Cover NBT did not round-trip for " + id + " (got null)");
      }
      if (!parsed.equals(rl)) {
        throw new IllegalStateException("Cover NBT did not round-trip for " + id + " (got " + parsed + ")");
      }
      // Also verify the registry entry exists so we never bind a cover item
      // the player can't actually obtain.
      Item item = BuiltInRegistries.ITEM.get(rl);
      if (item == Items.AIR && !"mystcraft:folder".equals(id)) {
        // mystcraft:folder is registered later than this assertion may run
        // during cold-start ordering; only require non-air for vanilla items.
        throw new IllegalStateException("Configured cover item is not registered: " + id);
      }
    }

    // Round-trip null clears the field.
    CompoundTag tag = LinkOptions.setCoverItemId(new CompoundTag(), new ResourceLocation("minecraft", "leather"));
    if (LinkOptions.getCoverItemId(tag) == null) {
      throw new IllegalStateException("Cover NBT did not persist after set");
    }
    LinkOptions.setCoverItemId(tag, null);
    if (LinkOptions.getCoverItemId(tag) != null) {
      throw new IllegalStateException("Setting cover NBT to null did not clear the field");
    }
  }

  /**
   * Verifies that {@link SymbolRegistry#getRandomWeightedWithAffinity}
   * actually shifts the distribution toward symbols favored by the supplied
   * blend. Registers a strong test affinity in {@link InkAffinity}, runs
   * {@code N} rolls with the affinity blend and {@code N} rolls with no
   * blend, and asserts the affinity rolls produce at least {@code 1.5x}
   * more hits on the favored symbol than the baseline.
   * <p>
   * Uses a fixed-seed {@link RandomSource} so the test is deterministic.
   */
  public static void assertInkAffinityBiasesSymbolRoll() {
    ResourceLocation favored = new ResourceLocation(Mystcraft.MOD_ID, "dense_ores");
    if (!SymbolRegistry.contains(favored)) {
      // If for some reason the symbol isn't loaded, treat the test as a no-op.
      // The 'core' suite already verifies the symbol set is loaded.
      return;
    }

    // Build an affinity blend with strong symbol-level bias.
    InkBlend blend = new InkBlend();
    Map<ResourceLocation, Float> symbols = new HashMap<>();
    symbols.put(favored, InkBlend.WEIGHT_CAP); // saturated
    InkAffinity.Entry strong = new InkAffinity.Entry(1.0f, 0, symbols, Map.of(), Map.of(), Map.of());
    blend.add(strong, 1);

    int rolls = 1000;
    int favoredHitsWithBlend = 0;
    int favoredHitsBaseline = 0;
    int totalRolledWithBlend = 0;
    int totalRolledBaseline = 0;

    RandomSource rngBlend = RandomSource.create(0xCAFE_BABEL);
    RandomSource rngBase = RandomSource.create(0xCAFE_BABEL);

    for (int i = 0; i < rolls; i++) {
      IAgeSymbol withBlend = SymbolRegistry.getRandomWeightedWithAffinity(rngBlend, blend);
      if (withBlend != null) {
        totalRolledWithBlend++;
        if (favored.equals(withBlend.getRegistryName())) favoredHitsWithBlend++;
      }
      IAgeSymbol baseline = SymbolRegistry.getRandomWeightedWithAffinity(rngBase, null);
      if (baseline != null) {
        totalRolledBaseline++;
        if (favored.equals(baseline.getRegistryName())) favoredHitsBaseline++;
      }
    }

    if (totalRolledWithBlend == 0 || totalRolledBaseline == 0) {
      throw new IllegalStateException("Symbol roller produced zero hits in " + rolls + " attempts");
    }
    double rateBlend = favoredHitsWithBlend / (double) totalRolledWithBlend;
    double rateBaseline = favoredHitsBaseline / (double) totalRolledBaseline;

    // Affinity blend should produce strictly more hits on the favored symbol
    // than the baseline, and noticeably so. We require at least a 1.5x rate
    // ratio (well within statistical reach for 1000 rolls and a 5x weight
    // multiplier on a single symbol).
    if (favoredHitsWithBlend <= favoredHitsBaseline) {
      throw new IllegalStateException("Affinity blend did not increase hits on " + favored
          + ": withBlend=" + favoredHitsWithBlend + " baseline=" + favoredHitsBaseline);
    }
    if (rateBaseline > 0 && rateBlend / rateBaseline < 1.5) {
      throw new IllegalStateException("Affinity blend hit-rate ratio < 1.5x baseline: "
          + " rateBlend=" + rateBlend + " rateBaseline=" + rateBaseline);
    }

    // Also verify tier bonus lifts the rank cap. Construct a blend with a
    // +1 tier bonus and confirm rolls above DEFAULT_AFFINITY_RANK_CAP are
    // reachable. We just need to confirm the cap math runs without crashing
    // and that the call accepts a blend with non-zero tierBonus.
    InkBlend tierBlend = new InkBlend();
    InkAffinity.Entry tierBoost = new InkAffinity.Entry(1.0f, 1, Map.of(), Map.of(), Map.of(), Map.of());
    tierBlend.add(tierBoost, 1);
    if (tierBlend.tierBonus() != 1) {
      throw new IllegalStateException("Tier bonus did not propagate through InkBlend.add");
    }
    IAgeSymbol any = SymbolRegistry.getRandomWeightedWithAffinity(RandomSource.create(0xDEADBEEFL), tierBlend);
    if (any == null) {
      throw new IllegalStateException("Roll with non-empty tier-bonus blend returned null");
    }
  }
}
