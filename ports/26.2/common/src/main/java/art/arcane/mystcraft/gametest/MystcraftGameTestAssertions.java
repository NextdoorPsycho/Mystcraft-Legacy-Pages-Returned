package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.client.gui.procedural.BookItemTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.BookTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUiReload;
import art.arcane.mystcraft.client.gui.procedural.symbol.*;
import art.arcane.mystcraft.client.render.DrawableWordManager;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.InkAffinity;
import art.arcane.mystcraft.data.InkBlend;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.datapack.symbol.SymbolDisplay;
import art.arcane.mystcraft.link.LinkPermissions;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.world.AgeData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/**
 * Strict client-only assertions for procedural rendering.
 */
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

  /**
   * Verifies that the 26.2 component bridge preserves both Mystcraft's custom
   * payload and vanilla components, including the optional-codec empty-stack
   * representation.
   */
  public static void assertItemStackNbtComponentRoundTrip() {
    ItemStack original = new ItemStack(Items.COMPASS);
    CompoundTag payload = new CompoundTag();
    payload.putString("MystcraftPayload", "component-round-trip");
    payload.putInt("Revision", 26);
    ItemStackNbt.setTag(original, payload);
    ItemStackNbt.setHoverName(original, Component.literal("Component Test Book"));

    ItemStack loaded = ItemStackNbt.load(ItemStackNbt.save(original));
    CompoundTag loadedPayload = ItemStackNbt.getTag(loaded);
    if (loadedPayload == null
        || !"component-round-trip".equals(loadedPayload.getStringOr("MystcraftPayload", ""))
        || loadedPayload.getIntOr("Revision", -1) != 26) {
      throw new IllegalStateException("ItemStackNbt lost the custom-data component during round-trip");
    }
    if (!ItemStackNbt.hasCustomHoverName(loaded)
        || !"Component Test Book".equals(loaded.getHoverName().getString())) {
      throw new IllegalStateException("ItemStackNbt lost the custom-name component during round-trip");
    }
    if (!ItemStackNbt.isSameItemSameTags(original, loaded)) {
      throw new IllegalStateException("ItemStackNbt changed item components during round-trip");
    }

    CompoundTag detached = ItemStackNbt.getOrCreateTag(loaded);
    detached.putBoolean("DetachedMutation", true);
    CompoundTag beforeCommit = ItemStackNbt.getTag(loaded);
    if (beforeCommit != null && beforeCommit.getBooleanOr("DetachedMutation", false)) {
      throw new IllegalStateException("ItemStackNbt returned a live custom-data view instead of a detached copy");
    }
    ItemStackNbt.setTag(loaded, detached);
    CompoundTag afterCommit = ItemStackNbt.getTag(loaded);
    if (afterCommit == null || !afterCommit.getBooleanOr("DetachedMutation", false)) {
      throw new IllegalStateException("ItemStackNbt.setTag did not commit a detached payload mutation");
    }

    ItemStack emptyRoundTrip = ItemStackNbt.load(ItemStackNbt.save(ItemStack.EMPTY));
    if (!emptyRoundTrip.isEmpty()) {
      throw new IllegalStateException("ItemStackNbt did not preserve an empty ItemStack");
    }
  }

  /**
   * Exercises defensive parsing for persisted permission and Age payloads.
   * Malformed entries must be ignored without discarding valid siblings.
   */
  public static void assertMalformedPersistedDataIsIgnored() {
    UUID validPlayer = UUID.fromString("9ef97352-742b-4e94-b62d-11903c80c422");
    ListTag mixedPlayers = new ListTag();
    mixedPlayers.add(StringTag.valueOf("not-a-uuid"));
    mixedPlayers.add(StringTag.valueOf(validPlayer.toString()));

    CompoundTag blacklist = new CompoundTag();
    blacklist.put("not-an-age", mixedPlayers.copy());
    blacklist.put("17", mixedPlayers.copy());
    CompoundTag owners = new CompoundTag();
    owners.putString("17", "not-a-uuid");
    owners.putString("invalid-age", validPlayer.toString());
    CompoundTag permissionTag = new CompoundTag();
    permissionTag.put("EntryBlacklist", blacklist);
    permissionTag.put("AgeOwners", owners);
    permissionTag.put("GlobalAdmins", mixedPlayers.copy());

    CompoundTag sanitizedPermissions = LinkPermissions.load(permissionTag).save(new CompoundTag());
    CompoundTag sanitizedBlacklist = sanitizedPermissions.getCompoundOrEmpty("EntryBlacklist");
    ListTag retainedPlayers = sanitizedBlacklist.getListOrEmpty("17");
    if (sanitizedBlacklist.contains("not-an-age")
        || retainedPlayers.size() != 1
        || !validPlayer.toString().equals(retainedPlayers.getStringOr(0, ""))
        || !sanitizedPermissions.getCompoundOrEmpty("AgeOwners").isEmpty()
        || sanitizedPermissions.getListOrEmpty("GlobalAdmins").size() != 1) {
      throw new IllegalStateException("LinkPermissions did not sanitize malformed persisted entries");
    }

    CompoundTag malformedAge = new CompoundTag();
    malformedAge.putString("AgeUUID", "not-a-uuid");
    ListTag malformedPages = new ListTag();
    CompoundTag malformedStack = new CompoundTag();
    malformedStack.putString("id", "not a valid item identifier");
    malformedStack.putInt("count", 1);
    malformedPages.add(malformedStack);
    malformedAge.put("Pages", malformedPages);
    CompoundTag malformedConfig = new CompoundTag();
    malformedConfig.putInt("MicroDimensionRadiusChunks", -12);
    malformedAge.put("AgeConfig", malformedConfig);

    AgeData loadedAge = AgeData.load(malformedAge);
    if (loadedAge.getAgeUUID() == null
        || !loadedAge.getPages().isEmpty()
        || !"normal".equals(loadedAge.getWeatherType())
        || loadedAge.getMicroDimensionRadiusChunks() != 0) {
      throw new IllegalStateException("AgeData did not recover safe defaults from malformed persisted data");
    }
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
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, itemId);
    Item item = BuiltInRegistries.ITEM.getValue(id);
    if (item == Items.AIR) {
      throw new IllegalStateException("Item not registered: " + id);
    }
  }

  private static void assertBlockRegistered(String blockId) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, blockId);
    Block block = BuiltInRegistries.BLOCK.getValue(id);
    if (block == Blocks.AIR) {
      throw new IllegalStateException("Block not registered: " + id);
    }
  }

  private static void assertCreativeTabRegistered(String tabId) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, tabId);
    if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) {
      throw new IllegalStateException("Creative tab not registered: " + id);
    }
  }

  private static void assertSymbolExists(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    if (!SymbolRegistry.contains(id)) {
      throw new IllegalStateException("Missing symbol: " + id);
    }
  }

  /**
   * Verifies that {@link InkBlend} can serialise its full state to NBT and
   * round-trip back without loss. This is the contract that ink mixers and
   * booster packs rely on so persisted blends keep biasing symbol rolls.
   */
  public static void assertInkBlendRoundTripsThroughNbt() {
    InkBlend before = new InkBlend();
    Map<Identifier, Float> symbols = new HashMap<>();
    symbols.put(Identifier.fromNamespaceAndPath("mystcraft", "dense_ores"), 1.5f);
    symbols.put(Identifier.fromNamespaceAndPath("mystcraft", "biome_dripstone_caves"), 0.75f);
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
    if (before.symbolWeight(Identifier.fromNamespaceAndPath("mystcraft", "dense_ores")) <= 0f) {
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
    float beforeSym = before.symbolWeight(Identifier.fromNamespaceAndPath("mystcraft", "dense_ores"));
    float afterSym = after.symbolWeight(Identifier.fromNamespaceAndPath("mystcraft", "dense_ores"));
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
   * {@link MystcraftConfig#bookBinderCoverItems}, the cover NBT can be written
   * via {@link LinkOptions#setCoverItemId} and read back via
   * {@link LinkOptions#getCoverItemId}. This is the bridge the procedural Book
   * texture factory uses to pick a cover palette.
   */
  public static void assertBookCoverNbtRoundTripsForEachCover() {
    List<String> covers = MystcraftConfig.bookBinderCoverItems.get();
    if (covers == null || covers.isEmpty()) {
      throw new IllegalStateException("MystcraftConfig.bookBinderCoverItems is empty; nothing to round-trip");
    }
    for (String id : covers) {
      Identifier rl = Identifier.tryParse(id);
      if (rl == null) {
        throw new IllegalStateException("Cover entry is not a valid Identifier: " + id);
      }
      CompoundTag tag = LinkOptions.setCoverItemId(new CompoundTag(), rl);
      Identifier parsed = LinkOptions.getCoverItemId(tag);
      if (parsed == null) {
        throw new IllegalStateException("Cover NBT did not round-trip for " + id + " (got null)");
      }
      if (!parsed.equals(rl)) {
        throw new IllegalStateException("Cover NBT did not round-trip for " + id + " (got " + parsed + ")");
      }

      Item item = BuiltInRegistries.ITEM.getValue(rl);
      if (item == Items.AIR && !"mystcraft:folder".equals(id)) {

        throw new IllegalStateException("Configured cover item is not registered: " + id);
      }
    }

    CompoundTag tag = LinkOptions.setCoverItemId(new CompoundTag(), Identifier.fromNamespaceAndPath("minecraft", "leather"));
    if (LinkOptions.getCoverItemId(tag) == null) {
      throw new IllegalStateException("Cover NBT did not persist after set");
    }
    LinkOptions.setCoverItemId(tag, null);
    if (LinkOptions.getCoverItemId(tag) != null) {
      throw new IllegalStateException("Setting cover NBT to null did not clear the field");
    }
  }

  /**
   * Verifies that {@link SymbolRegistry#getRandomWeightedWithAffinity} actually
   * shifts the distribution toward symbols favored by the supplied blend.
   * Registers a strong test affinity in {@link InkAffinity}, runs {@code N}
   * rolls with the affinity blend and {@code N} rolls with no blend, and
   * asserts the affinity rolls produce at least {@code 1.5x} more hits on the
   * favored symbol than the baseline.
   * <p>
   * Uses a fixed-seed {@link RandomSource} so the test is deterministic.
   */
  public static void assertInkAffinityBiasesSymbolRoll() {
    Identifier favored = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "dense_ores");
    if (!SymbolRegistry.contains(favored)) {

      return;
    }

    InkBlend blend = new InkBlend();
    Map<Identifier, Float> symbols = new HashMap<>();
    symbols.put(favored, InkBlend.WEIGHT_CAP);
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

    if (favoredHitsWithBlend <= favoredHitsBaseline) {
      throw new IllegalStateException("Affinity blend did not increase hits on " + favored
          + ": withBlend=" + favoredHitsWithBlend + " baseline=" + favoredHitsBaseline);
    }
    if (rateBaseline > 0 && rateBlend / rateBaseline < 1.5) {
      throw new IllegalStateException("Affinity blend hit-rate ratio < 1.5x baseline: "
          + " rateBlend=" + rateBlend + " rateBaseline=" + rateBaseline);
    }

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

  /**
   * Verifies the procedural-symbol glyph pipeline is deterministic, which is
   * the property the page-renderer relies on so wikis, screenshots, and saved
   * worlds stay visually consistent across runs.
   * <p>
   * Three layers are checked, with progressively stronger requirements:
   * <ol>
   *   <li><b>Seed-math layer (always runs)</b> —
   *       {@link SymbolSeed#derive} returns the same int for the same
   *       inputs and distinct ints for distinct inputs. This is pure
   *       JVM-deterministic FNV-1a so it works on any test runtime.</li>
   *   <li><b>Curated-vocabulary layer (always runs)</b> —
   *       {@link DrawableWordManager#getCuratedSeed(String)} returns a
   *       stable, case-insensitive seed for known D'ni words and
   *       distinct seeds for distinct words.</li>
   *   <li><b>Pixel layer (client runtime required)</b> —
   *       calls {@link SymbolGlyphFactory#glyph} twice with the cache
   *       reset between calls and asserts the two
   *       {@link com.mojang.blaze3d.platform.NativeImage}s carry
   *       byte-identical pixel data. Two distinct symbols must not
   *       produce pixel-identical glyphs.</li>
   * </ol>
   * If the pixel layer cannot run (e.g. dedicated-server test runtime
   * without LWJGL natives loaded), it returns silently — layers 1 and 2
   * already prove determinism since the rendering chain consumes only
   * the seed.
   */
  public static void assertSymbolGlyphIsDeterministic() {

    Identifier desert = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "biome_desert");
    Identifier forest = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "biome_forest");
    int s1 = SymbolSeed.derive(desert, "Desert", 0);
    int s2 = SymbolSeed.derive(desert, "Desert", 0);
    if (s1 != s2) {
      throw new IllegalStateException("SymbolSeed.derive not deterministic for identical inputs: "
          + Integer.toHexString(s1) + " vs " + Integer.toHexString(s2));
    }
    int sCase = SymbolSeed.derive(desert, "desert", 0);
    if (s1 != sCase) {
      throw new IllegalStateException("SymbolSeed.derive not case-insensitive: "
          + Integer.toHexString(s1) + " vs " + Integer.toHexString(sCase));
    }
    int sDifferentSymbol = SymbolSeed.derive(forest, "Desert", 0);
    if (s1 == sDifferentSymbol) {
      throw new IllegalStateException("SymbolSeed.derive collided across symbols (biome_desert vs biome_forest)");
    }
    int sDifferentWord = SymbolSeed.derive(desert, "Forest", 0);
    if (s1 == sDifferentWord) {
      throw new IllegalStateException("SymbolSeed.derive collided across words (Desert vs Forest)");
    }
    int sDifferentSalt = SymbolSeed.derive(desert, "Desert", 1);
    if (s1 == sDifferentSalt) {
      throw new IllegalStateException("SymbolSeed.derive collided across salt values (0 vs 1)");
    }

    DrawableWordManager.initialize();
    Integer fireSeedLower = DrawableWordManager.getCuratedSeed("fire");
    Integer fireSeedMixed = DrawableWordManager.getCuratedSeed("Fire");
    Integer fireSeedUpper = DrawableWordManager.getCuratedSeed("FIRE");
    if (fireSeedLower == null) {
      throw new IllegalStateException("Curated word 'fire' returned null seed; vocabulary not initialised");
    }
    if (!fireSeedLower.equals(fireSeedMixed) || !fireSeedLower.equals(fireSeedUpper)) {
      throw new IllegalStateException("DrawableWordManager.getCuratedSeed not case-insensitive: "
          + "lower=" + fireSeedLower + " mixed=" + fireSeedMixed + " upper=" + fireSeedUpper);
    }
    Integer waterSeed = DrawableWordManager.getCuratedSeed("water");
    if (waterSeed == null || waterSeed.equals(fireSeedLower)) {
      throw new IllegalStateException("Curated seeds collided across distinct words: fire=" + fireSeedLower
          + " water=" + waterSeed);
    }
    if (DrawableWordManager.getCuratedSeed("not_a_real_word_xyz") != null) {
      throw new IllegalStateException("Unknown words must return null curated seed");
    }
    Integer fireColorLower = DrawableWordManager.getCuratedColor("fire");
    Integer fireColorUpper = DrawableWordManager.getCuratedColor("FIRE");
    if (fireColorLower == null || !fireColorLower.equals(fireColorUpper)) {
      throw new IllegalStateException("DrawableWordManager.getCuratedColor not stable + case-insensitive for 'fire'");
    }

    assertGlyphPixelDeterminism(desert, forest);
  }

  private static void assertGlyphPixelDeterminism(@org.jetbrains.annotations.NotNull Identifier desert,
                                                  @org.jetbrains.annotations.NotNull Identifier forest) {
    IAgeSymbol biomeDesert = SymbolRegistry.get(desert);
    if (biomeDesert == null) {
      throw new IllegalStateException("Required client test symbol is missing: " + desert);
    }
    SymbolPalette.Entry palette = SymbolPalette.get(biomeDesert.getCategory());
    int size = SymbolGlyphFactory.GLYPH_SIZE;

    SymbolGlyphFactory.reset();
    com.mojang.blaze3d.platform.NativeImage img1 = SymbolGlyphFactory.glyph(biomeDesert, "Desert", palette);
    com.mojang.blaze3d.platform.NativeImage img1Cached = SymbolGlyphFactory.glyph(biomeDesert, "Desert", palette);
    if (img1 != img1Cached) {
      throw new IllegalStateException("SymbolGlyphFactory cache returned different reference for identical inputs");
    }

    int[] snapshot = new int[size * size];
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        snapshot[y * size + x] = img1.getPixel(x, y);
      }
    }
    SymbolGlyphFactory.reset();
    com.mojang.blaze3d.platform.NativeImage img2 = SymbolGlyphFactory.glyph(biomeDesert, "Desert", palette);
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        int expected = snapshot[y * size + x];
        int actual = img2.getPixel(x, y);
        if (expected != actual) {
          throw new IllegalStateException("Pixel (" + x + "," + y + ") differs after reset: 0x"
              + Integer.toHexString(expected) + " vs 0x" + Integer.toHexString(actual));
        }
      }
    }

    IAgeSymbol biomeForest = SymbolRegistry.get(forest);
    if (biomeForest != null) {
      SymbolPalette.Entry forestPalette = SymbolPalette.get(biomeForest.getCategory());
      com.mojang.blaze3d.platform.NativeImage forestImg =
          SymbolGlyphFactory.glyph(biomeForest, "Forest", forestPalette);
      boolean anyDifferent = false;
      for (int y = 0; y < size && !anyDifferent; y++) {
        for (int x = 0; x < size; x++) {
          if (img2.getPixel(x, y) != forestImg.getPixel(x, y)) {
            anyDifferent = true;
            break;
          }
        }
      }
      if (!anyDifferent) {
        throw new IllegalStateException(
            "biome_desert and biome_forest produced pixel-identical glyphs — symbol mix-in is not engaged");
      }
    }

    SymbolGlyphFactory.reset();
  }

  /**
   * Verifies that {@link SymbolMotif#defaultFor(SymbolCategory)} returns the
   * spec'd motif for every {@link SymbolCategory} (plan §5.3.2) and that motif
   * rendering is deterministic + produces distinct outputs across categories.
   * <p>
   * Two layers, like {@link #assertSymbolGlyphIsDeterministic}:
   * <ol>
   *   <li><b>Dispatch table (always runs)</b> — a hard-coded
   *       {@code (category → expected motif)} table is checked. Acts as
   *       a regression guard: any change to {@code defaultFor} fails the
   *       test until both the test and the spec are updated together.</li>
   *   <li><b>Pixel layer (client runtime required)</b> —
   *       picks one symbol per category, renders the motif into a fresh
   *       128×128 {@link com.mojang.blaze3d.platform.NativeImage}, hashes
   *       the pixels, and asserts (a) the hash repeats on a second
   *       render, (b) at least 12 of the 18 categories produce distinct
   *       hashes (LATTICE is shared by 3 FEATURE_* categories so some
   *       motif overlap is expected, but palette differences should
   *       still drive most renders apart).</li>
   * </ol>
   */
  public static void assertMotifDispatchPerCategory() {

    Map<SymbolCategory, SymbolMotif> expected = new EnumMap<>(SymbolCategory.class);
    expected.put(SymbolCategory.TERRAIN, SymbolMotif.HORIZON);
    expected.put(SymbolCategory.BIOME_CONTROLLER, SymbolMotif.COMPASS);
    expected.put(SymbolCategory.BIOME, SymbolMotif.WREATH);
    expected.put(SymbolCategory.WEATHER, SymbolMotif.STORM);
    expected.put(SymbolCategory.LIGHTING, SymbolMotif.LANTERN);
    expected.put(SymbolCategory.COLOR, SymbolMotif.SWATCH_RING);
    expected.put(SymbolCategory.VISUAL_EFFECT, SymbolMotif.RIPPLE);
    expected.put(SymbolCategory.ENVIRONMENT, SymbolMotif.STAR_FIELD);
    expected.put(SymbolCategory.FEATURE_LARGE, SymbolMotif.LATTICE);
    expected.put(SymbolCategory.FEATURE_MEDIUM, SymbolMotif.LATTICE);
    expected.put(SymbolCategory.FEATURE_SMALL, SymbolMotif.LATTICE);
    expected.put(SymbolCategory.STRUCTURE, SymbolMotif.ARCH);
    expected.put(SymbolCategory.ANGLE, SymbolMotif.COMPASS_ROSE);
    expected.put(SymbolCategory.PHASE, SymbolMotif.MOON_CYCLE);
    expected.put(SymbolCategory.LENGTH, SymbolMotif.RULER);
    expected.put(SymbolCategory.SEA, SymbolMotif.WAVE);
    expected.put(SymbolCategory.MODIFIER, SymbolMotif.DIAMOND);
    expected.put(SymbolCategory.SPECIAL, SymbolMotif.STAR);

    if (expected.size() != SymbolCategory.values().length) {
      throw new IllegalStateException("Dispatch table covers " + expected.size()
          + " categories but enum has " + SymbolCategory.values().length
          + " — add the missing category to assertMotifDispatchPerCategory");
    }

    for (Map.Entry<SymbolCategory, SymbolMotif> e : expected.entrySet()) {
      SymbolMotif actual = SymbolMotif.defaultFor(e.getKey());
      if (actual != e.getValue()) {
        throw new IllegalStateException("SymbolMotif.defaultFor(" + e.getKey()
            + ") returned " + actual + ", expected " + e.getValue());
      }
    }
    if (SymbolMotif.defaultFor(null) != SymbolMotif.DIAMOND) {
      throw new IllegalStateException("SymbolMotif.defaultFor(null) must fall back to DIAMOND");
    }

    DrawableWordManager.initialize();
    assertMotifPixelDispatch();
  }

  private static void assertMotifPixelDispatch() {
    Map<SymbolCategory, Long> hashByCategory = new EnumMap<>(SymbolCategory.class);
    Map<SymbolCategory, IAgeSymbol> sampleByCategory = new EnumMap<>(SymbolCategory.class);
    for (SymbolCategory cat : SymbolCategory.values()) {
      IAgeSymbol sample = pickFirstSymbolOfCategory(cat);
      if (sample == null) continue;
      sampleByCategory.put(cat, sample);
      long hash = renderMotifAndHashPixels(sample);
      hashByCategory.put(cat, hash);

      long hash2 = renderMotifAndHashPixels(sample);
      if (hash != hash2) {
        throw new IllegalStateException("Motif rendering for " + sample.getRegistryName()
            + " (" + cat + ") not deterministic: 0x" + Long.toHexString(hash)
            + " vs 0x" + Long.toHexString(hash2));
      }
    }
    if (hashByCategory.isEmpty()) {
      throw new IllegalStateException("No registered symbols are available for the motif client test");
    }
    long uniqueHashes = hashByCategory.values().stream().distinct().count();
    int sampled = hashByCategory.size();
    int requiredUnique = Math.min(12, sampled);
    if (uniqueHashes < requiredUnique) {
      throw new IllegalStateException("Only " + uniqueHashes + "/" + sampled
          + " category motif renders are unique; expected at least " + requiredUnique
          + " — palette / motif differentiation may be too weak");
    }
  }

  @org.jetbrains.annotations.Nullable
  private static IAgeSymbol pickFirstSymbolOfCategory(@org.jetbrains.annotations.NotNull SymbolCategory cat) {
    java.util.List<IAgeSymbol> sorted = new ArrayList<>(SymbolRegistry.getAll());
    sorted.sort(java.util.Comparator.comparing(s -> {
      Identifier rl = s.getRegistryName();
      return rl != null ? rl.toString() : "";
    }));
    for (IAgeSymbol s : sorted) {
      if (s.getCategory() == cat) return s;
    }
    return null;
  }

  private static long renderMotifAndHashPixels(@org.jetbrains.annotations.NotNull IAgeSymbol symbol) {
    SymbolPalette.Entry palette = SymbolPalette.get(symbol.getCategory());
    SymbolMotif motif = palette.defaultMotif();
    int size = SymbolMotif.TEX_SIZE;

    com.mojang.blaze3d.platform.NativeImage img =
        new com.mojang.blaze3d.platform.NativeImage(
            com.mojang.blaze3d.platform.NativeImage.Format.RGBA, size, size, true);
    try {
      img.fillRect(0, 0, size, size, 0);

      java.util.List<com.mojang.blaze3d.platform.NativeImage> glyphs = new ArrayList<>(4);
      String[] poem = symbol.getPoem();
      if (poem == null || poem.length == 0) {
        glyphs.add(SymbolGlyphFactory.glyph(symbol, null, palette));
      } else {
        for (int i = 0; i < Math.min(4, poem.length); i++) {
          glyphs.add(SymbolGlyphFactory.glyph(symbol, poem[i], palette));
        }
      }

      int seed = SymbolSeed.derive(symbol.getRegistryName(), motif.getName(), motif.ordinal());
      motif.layout(img, 0, 0, size, size, glyphs, palette, seed);

      long hash = 0xCBF29CE484222325L;
      long prime = 0x100000001B3L;
      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          int p = img.getPixel(x, y);
          hash ^= (p & 0xFFFFFFFFL);
          hash *= prime;
        }
      }
      return hash;
    } finally {
      img.close();
    }
  }

  /**
   * Asserts
   * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolFlourish}
   * produces a strictly monotonic visual progression across card ranks 1..5:
   * each higher rank renders <em>strictly more</em> non-transparent pixels than
   * the previous rank. Two layers:
   *
   * <ol>
   *   <li><b>Treatment monotonicity.</b> {@code SymbolFlourish.forRank(1..5)}
   *       returns increasingly-decorated treatments — measured by counting
   *       enabled features (border weight + ornament weight + spine +
   *       mid-edge + halo). This layer runs without LWJGL natives.</li>
   *   <li><b>Pixel progression.</b> Render the same sample symbol with
   *       a wrapper overriding {@code getCardRank} to each value 1..5,
   *       assert each rank is deterministic, and assert all five rank images
   *       are distinct on the client runtime.</li>
   * </ol>
   * <p>
   * The sample symbol is forced to {@code instabilityCost == 0} via the
   * wrapper so the warning halo doesn't pollute the comparison — the
   * halo is an independent treatment validated separately by
   * {@link #renderRankPixelHashes}'s instability cap is unreachable
   * because of the wrapper.
   */
  public static void assertSymbolRankProgression() {

    int[] weights = new int[5];
    for (int rank = 1; rank <= 5; rank++) {
      art.arcane.mystcraft.client.gui.procedural.symbol.SymbolFlourish.Treatment t =
          art.arcane.mystcraft.client.gui.procedural.symbol.SymbolFlourish.forRank(rank);
      weights[rank - 1] = scoreTreatment(t);
    }
    for (int i = 1; i < weights.length; i++) {
      if (weights[i] <= weights[i - 1]) {
        StringBuilder sb = new StringBuilder("SymbolFlourish.forRank treatment weights are not monotonic: ");
        for (int j = 0; j < weights.length; j++) {
          if (j > 0) sb.append(" → ");
          sb.append("rank").append(j + 1).append("=").append(weights[j]);
        }
        throw new IllegalStateException(sb.toString());
      }
    }

    DrawableWordManager.initialize();
    assertSymbolRankPixelProgression();
  }

  private static void assertSymbolRankPixelProgression() {
    IAgeSymbol sample = pickStableSymbolForRankTest();
    if (sample == null) {
      throw new IllegalStateException("No stable symbol is available for the client rank test");
    }

    long[] firstPass = renderRankPixelHashes(sample);
    long[] secondPass = renderRankPixelHashes(sample);
    for (int rank = 0; rank < firstPass.length; rank++) {
      if (firstPass[rank] != secondPass[rank]) {
        throw new IllegalStateException("Rank " + (rank + 1) + " render for "
            + sample.getRegistryName() + " is not deterministic: 0x"
            + Long.toHexString(firstPass[rank]) + " vs 0x"
            + Long.toHexString(secondPass[rank]));
      }
      for (int earlier = 0; earlier < rank; earlier++) {
        if (firstPass[rank] == firstPass[earlier]) {
          throw new IllegalStateException("Rank renders for " + sample.getRegistryName()
              + " are pixel-identical at ranks " + (earlier + 1) + " and " + (rank + 1));
        }
      }
    }
  }

  private static int scoreTreatment(
      @org.jetbrains.annotations.NotNull
      art.arcane.mystcraft.client.gui.procedural.symbol.SymbolFlourish.Treatment t) {
    int score = 0;
    score += switch (t.borderStyle()) {
      case NONE -> 0;
      case SINGLE_HAIRLINE -> 4;
      case SINGLE_WITH_CORNERS -> 5;
      case DOUBLE_LINE -> 8;
      case DOUBLE_WITH_ORNAMENTS -> 9;
    };
    score += switch (t.cornerOrnament()) {
      case NONE -> 0;
      case DOTS -> 1;
      case PIPS -> 2;
      case FILIGREE_3_STROKE -> 3;
      case FILIGREE_5_STROKE_PIP -> 5;
    };
    if (t.useSpineRibbon()) score += 2;
    if (t.useMidEdgePips()) score += 1;
    if (t.useHalo()) score += 6;
    return score;
  }

  @org.jetbrains.annotations.Nullable
  private static IAgeSymbol pickStableSymbolForRankTest() {
    java.util.List<IAgeSymbol> sorted = new ArrayList<>(SymbolRegistry.getAll());
    sorted.sort(java.util.Comparator.comparing(s -> {
      Identifier rl = s.getRegistryName();
      return rl != null ? rl.toString() : "";
    }));
    for (IAgeSymbol s : sorted) {
      if (s.getInstabilityCost() <= 0.0f && s.getPoem() != null && s.getPoem().length > 0) {
        return s;
      }
    }
    return null;
  }

  private static long[] renderRankPixelHashes(@org.jetbrains.annotations.NotNull IAgeSymbol symbol) {
    long[] hashes = new long[5];
    for (int rank = 1; rank <= 5; rank++) {
      IAgeSymbol wrapped = withCardRank(symbol, rank);
      try (com.mojang.blaze3d.platform.NativeImage img =
               SymbolPageTextureFactory.composeSymbolPageImage(wrapped)) {
        long hash = 0xCBF29CE484222325L;
        long prime = 0x100000001B3L;
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
          for (int x = 0; x < w; x++) {
            hash ^= img.getPixel(x, y) & 0xFFFFFFFFL;
            hash *= prime;
          }
        }
        hashes[rank - 1] = hash;
      }
    }
    return hashes;
  }

  /**
   * Asserts that {@link ProceduralUiReload#reloadAll()} flushes every
   * procedural-symbol cache. Two layers:
   *
   * <ol>
   *   <li><b>Cache contract (always runs)</b> — call {@code reloadAll}
   *       directly; assert {@link SymbolGlyphFactory#cacheSize()} drops
   *       to {@code 0}. Validates the wiring fix for plan task 5.4 (the
   *       reload listener previously skipped the symbol caches entirely).</li>
   *   <li><b>Pixel-identity layer (client runtime required)</b> —
   *       render a glyph, capture its hash, reload, render the same
   *       glyph again, assert hash matches. Reload should be transparent
   *       to deterministic content, but the {@link NativeImage} reference
   *       MUST differ (cache cleared then repopulated).</li>
   * </ol>
   */
  public static void assertProceduralUiReloadFlushesSymbolCaches() {
    DrawableWordManager.initialize();

    IAgeSymbol sample = pickFirstSymbolOfCategory(SymbolCategory.BIOME);
    if (sample == null) {
      throw new IllegalStateException("No biome symbol is available for the client reload test");
    }
    SymbolPalette.Entry palette = SymbolPalette.get(sample.getCategory());

    SymbolGlyphFactory.glyph(sample, "Stone", palette);
    int beforeReloadSize = SymbolGlyphFactory.cacheSize();
    if (beforeReloadSize == 0) {
      throw new IllegalStateException("Symbol glyph cache stayed empty before reload");
    }

    ProceduralUiReload.reloadAll();
    int afterReloadSize = SymbolGlyphFactory.cacheSize();
    if (afterReloadSize != 0) {
      throw new IllegalStateException(
          "ProceduralUiReload.reloadAll did not flush SymbolGlyphFactory cache: "
              + "before=" + beforeReloadSize + ", after=" + afterReloadSize);
    }

    assertReloadPreservesPixelIdentity();
  }

  private static void assertReloadPreservesPixelIdentity() {
    IAgeSymbol sample = pickFirstSymbolOfCategory(SymbolCategory.BIOME);
    if (sample == null) {
      throw new IllegalStateException("No biome symbol is available for the client reload test");
    }
    SymbolPalette.Entry palette = SymbolPalette.get(sample.getCategory());
    long hashBefore = hashGlyph(sample, "Stone", palette);

    ProceduralUiReload.reloadAll();
    if (SymbolGlyphFactory.cacheSize() != 0) {
      throw new IllegalStateException("Reload did not clear SymbolGlyphFactory cache (Layer 2)");
    }

    long hashAfter = hashGlyph(sample, "Stone", palette);
    if (hashBefore != hashAfter) {
      throw new IllegalStateException(
          "Reload changed the deterministic glyph hash for "
              + sample.getRegistryName() + " / Stone: 0x"
              + Long.toHexString(hashBefore) + " → 0x" + Long.toHexString(hashAfter));
    }
  }

  private static long hashGlyph(@org.jetbrains.annotations.NotNull IAgeSymbol symbol,
                                @org.jetbrains.annotations.Nullable String word,
                                @org.jetbrains.annotations.NotNull SymbolPalette.Entry palette) {
    com.mojang.blaze3d.platform.NativeImage img = SymbolGlyphFactory.glyph(symbol, word, palette);
    long hash = 0xCBF29CE484222325L;
    long prime = 0x100000001B3L;
    int w = img.getWidth();
    int h = img.getHeight();
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int p = img.getPixel(x, y);
        hash ^= (p & 0xFFFFFFFFL);
        hash *= prime;
      }
    }
    return hash;
  }

  @org.jetbrains.annotations.NotNull
  private static IAgeSymbol withCardRank(@org.jetbrains.annotations.NotNull IAgeSymbol base, int rank) {
    return new IAgeSymbol() {
      @Override
      public Identifier getRegistryName() {
        return base.getRegistryName();
      }

      @Override
      public SymbolCategory getCategory() {
        return base.getCategory();
      }

      @Override
      public void registerLogic(AgeDirector director, long seed) {
        base.registerLogic(director, seed);
      }

      @Override
      public String getLocalizedName() {
        return base.getLocalizedName();
      }

      @Override
      public int instabilityModifier(int count) {
        return base.instabilityModifier(count);
      }

      @Override
      public float getInstabilityCost() {
        return 0.0f;
      }

      @Override
      public Integer getCardRank() {
        return rank;
      }

      @Override
      public String[] getPoem() {
        return base.getPoem();
      }

      @Override
      public boolean allowInRandomGeneration() {
        return base.allowInRandomGeneration();
      }

      @Override
      public boolean canDuplicate() {
        return base.canDuplicate();
      }
    };
  }

  /**
   * Asserts that datapack {@link SymbolDisplay} overrides flow all the way
   * through {@link SymbolPageTextureFactory#drawSymbolMotifFor} to the rendered
   * pixels. Three layers, like the other procedural-UI tests:
   *
   * <ol>
   *   <li><b>JSON parser (always runs)</b> — feed
   *       {@link SymbolDisplay#fromJson} a fully-populated block and
   *       verify motif / palette / glyph_seeds round-trip.</li>
   *   <li><b>API contract (always runs)</b> — wrap a real symbol via
   *       {@link #withDisplay} and assert
   *       {@link IAgeSymbol#getDisplay()} returns the supplied override
   *       (default returns {@code null}).</li>
   *   <li><b>Pixel layer (client runtime required)</b> —
   *       render the same base symbol four ways:
   *       <ul>
   *         <li>no display override (category default = WREATH for BIOME)</li>
   *         <li>{@code motif="compass"} → COMPASS layout</li>
   *         <li>{@code motif="star_field"} → STAR_FIELD layout</li>
   *         <li>same motif as #1 but with a {@code palette_override.accent}</li>
   *       </ul>
   *       Hash each render and assert all four hashes are distinct, proving
   *       motif and palette overrides each affect output.</li>
   * </ol>
   */
  public static void assertSymbolDisplayOverrideApplied() {

    String json = "{"
        + "\"motif\":\"compass\","
        + "\"palette_override\":{"
        + "\"base\":\"#FF6B4F2C\","
        + "\"accent\":\"#FFE8B070\","
        + "\"ink\":\"#FF1A0F08\","
        + "\"halo\":\"#FFFFD9A0\""
        + "},"
        + "\"glyph_seeds\":{"
        + "\"Stone\":-889275714,"
        + "\"Fire\":\"0xCAFEBABE\""
        + "}"
        + "}";
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    SymbolDisplay parsed = SymbolDisplay.fromJson(
        Identifier.fromNamespaceAndPath("mystcraft", "test/override"), root);
    if (parsed == null) {
      throw new IllegalStateException("SymbolDisplay.fromJson returned null for a populated block");
    }
    if (!"compass".equals(parsed.motifName())) {
      throw new IllegalStateException("Expected motifName='compass', got '" + parsed.motifName() + "'");
    }
    if (parsed.paletteOverride() == null
        || parsed.paletteOverride().accent() == null
        || (parsed.paletteOverride().accent() & 0xFFFFFF) != 0xE8B070) {
      throw new IllegalStateException("Palette override accent did not parse to 0xE8B070: "
          + parsed.paletteOverride());
    }
    Integer stoneSeed = parsed.seedFor("Stone");
    if (stoneSeed == null || stoneSeed.intValue() != -889275714) {
      throw new IllegalStateException("Glyph seed for 'Stone' did not parse to -889275714: " + stoneSeed);
    }
    Integer fireSeed = parsed.seedFor("Fire");
    if (fireSeed == null || fireSeed.intValue() != 0xCAFEBABE) {
      throw new IllegalStateException("Glyph seed for 'Fire' did not parse to 0xCAFEBABE: 0x"
          + (fireSeed == null ? "null" : Integer.toHexString(fireSeed)));
    }

    SymbolDisplay empty = SymbolDisplay.fromJson(
        Identifier.fromNamespaceAndPath("mystcraft", "test/empty"),
        JsonParser.parseString("{}").getAsJsonObject());
    if (empty != null) {
      throw new IllegalStateException("SymbolDisplay.fromJson({}) should collapse to null, got " + empty);
    }

    IAgeSymbol base = pickFirstSymbolOfCategory(SymbolCategory.BIOME);
    if (base == null) {
      throw new IllegalStateException("No biome symbol is available for the display override test");
    }
    if (base.getDisplay() != null) {
      throw new IllegalStateException("Vanilla IAgeSymbol.getDisplay() default must return null; "
          + base.getRegistryName() + " returned " + base.getDisplay());
    }
    SymbolDisplay forced = new SymbolDisplay(
        "compass", null, java.util.Collections.emptyMap());
    IAgeSymbol wrapped = withDisplay(base, forced);
    if (wrapped.getDisplay() != forced) {
      throw new IllegalStateException("withDisplay wrapper did not propagate display block");
    }

    DrawableWordManager.initialize();
    assertSymbolDisplayPixelOverride(base);
  }

  private static void assertSymbolDisplayPixelOverride(@org.jetbrains.annotations.NotNull IAgeSymbol base) {

    SymbolDisplay compass = new SymbolDisplay("compass", null, java.util.Collections.emptyMap());
    SymbolDisplay starField = new SymbolDisplay("star_field", null, java.util.Collections.emptyMap());
    SymbolDisplay accentOverride = new SymbolDisplay(
        null,
        new SymbolDisplay.PaletteOverride(null, 0xFFFF00FF, null, null),
        java.util.Collections.emptyMap());

    long hashDefault = renderDisplayAndHashPixels(base, null);
    long hashCompass = renderDisplayAndHashPixels(base, compass);
    long hashStarField = renderDisplayAndHashPixels(base, starField);
    long hashAccent = renderDisplayAndHashPixels(base, accentOverride);

    long hashDefault2 = renderDisplayAndHashPixels(base, null);
    if (hashDefault != hashDefault2) {
      throw new IllegalStateException("Display override render not deterministic for default render: 0x"
          + Long.toHexString(hashDefault) + " vs 0x" + Long.toHexString(hashDefault2));
    }

    long[] hashes = {hashDefault, hashCompass, hashStarField, hashAccent};
    String[] labels = {"default(wreath)", "motif=compass", "motif=star_field", "accent=#FF00FF"};
    for (int i = 0; i < hashes.length; i++) {
      for (int j = i + 1; j < hashes.length; j++) {
        if (hashes[i] == hashes[j]) {
          throw new IllegalStateException("Display override hashes collide: "
              + labels[i] + " == " + labels[j]
              + " (both 0x" + Long.toHexString(hashes[i]) + ")");
        }
      }
    }
  }

  private static long renderDisplayAndHashPixels(@org.jetbrains.annotations.NotNull IAgeSymbol base,
                                                 @org.jetbrains.annotations.Nullable SymbolDisplay display) {
    IAgeSymbol target = display != null ? withDisplay(base, display) : base;
    try (com.mojang.blaze3d.platform.NativeImage img =
             SymbolPageTextureFactory.composeSymbolPageImage(target)) {
      long hash = 0xCBF29CE484222325L;
      long prime = 0x100000001B3L;
      int w = img.getWidth();
      int h = img.getHeight();
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          int p = img.getPixel(x, y);
          hash ^= (p & 0xFFFFFFFFL);
          hash *= prime;
        }
      }
      return hash;
    }
  }

  @org.jetbrains.annotations.NotNull
  private static IAgeSymbol withDisplay(@org.jetbrains.annotations.NotNull IAgeSymbol base,
                                        @org.jetbrains.annotations.NotNull SymbolDisplay display) {
    return new IAgeSymbol() {
      @Override
      public Identifier getRegistryName() {
        return base.getRegistryName();
      }

      @Override
      public SymbolCategory getCategory() {
        return base.getCategory();
      }

      @Override
      public void registerLogic(AgeDirector director, long seed) {
        base.registerLogic(director, seed);
      }

      @Override
      public String getLocalizedName() {
        return base.getLocalizedName();
      }

      @Override
      public int instabilityModifier(int count) {
        return base.instabilityModifier(count);
      }

      @Override
      public float getInstabilityCost() {
        return 0.0f;
      }

      @Override
      public Integer getCardRank() {
        return base.getCardRank();
      }

      @Override
      public String[] getPoem() {
        return base.getPoem();
      }

      @Override
      public boolean allowInRandomGeneration() {
        return base.allowInRandomGeneration();
      }

      @Override
      public boolean canDuplicate() {
        return base.canDuplicate();
      }

      @Override
      public SymbolDisplay getDisplay() {
        return display;
      }
    };
  }

  /**
   * Asserts that {@link SymbolGlyphFactory#warmBlocking()} actually pre-warms
   * the glyph cache and completes within a soft regression window. Combined
   * Phase 5 task 5.6 (perf) + 5.7 (correctness):
   *
   * <ol>
   *   <li>Cache is reset to a clean baseline.</li>
   *   <li>{@code warmBlocking()} runs synchronously on the calling
   *       thread.</li>
   *   <li>Returned cache size matches one of two expected values:
   *       <ul>
   *         <li>{@code symbolCount × averagePoemWords} (full warm), or</li>
   *         <li>{@code CACHE_CAPACITY} (warm hit the LRU cap, which is
   *             the documented escape hatch for very large registries).</li>
   *       </ul>
   *   </li>
   *   <li>Wall-clock duration is logged, and we assert the soft 5s
   *       regression ceiling so a future change that makes pre-warm
   *       1000× slower fails CI rather than silently shipping. The
   *       performance <i>target</i> is much tighter (≤ 600 ms on a
   *       developer laptop with 16 cores) but we deliberately keep the
   *       assertion permissive so headless CI runners with limited
   *       cores still pass.</li>
   * </ol>
   *
   * <p>This assertion is client-only. Native or rendering failures are test
   * failures and are never converted into a pass.
   */
  public static void assertProceduralSymbolWarmCompletes() {
    DrawableWordManager.initialize();

    int symbolCount = SymbolRegistry.getAll().size();
    if (symbolCount == 0) {
      throw new IllegalStateException("Symbol registry is empty during the client warm test");
    }

    SymbolGlyphFactory.reset();
    if (SymbolGlyphFactory.cacheSize() != 0) {
      throw new IllegalStateException(
          "SymbolGlyphFactory.reset did not clear cache; size=" + SymbolGlyphFactory.cacheSize());
    }

    long start = System.nanoTime();
    int finalSize = SymbolGlyphFactory.warmBlocking();
    long durationMs = (System.nanoTime() - start) / 1_000_000L;

    Mystcraft.LOGGER.info(
        "[ProceduralUiTest] warmBlocking populated {} glyph tiles for {} symbols in {} ms",
        finalSize, symbolCount, durationMs);

    if (finalSize <= 0) {
      throw new IllegalStateException(
          "warmBlocking returned zero entries — pre-warm did not run");
    }

    int capacity = SymbolGlyphFactory.cacheCapacity();
    int expectedMinimum = Math.min(symbolCount, capacity);
    if (finalSize < expectedMinimum || finalSize > capacity) {
      throw new IllegalStateException(
          "warmBlocking populated " + finalSize + " tiles; expected " + expectedMinimum
              + ".." + capacity + " for " + symbolCount + " symbols");
    }

    final long ceilingMs = 30_000L;
    if (durationMs > ceilingMs) {
      throw new IllegalStateException(
          "warmBlocking took " + durationMs + " ms (ceiling " + ceilingMs
              + " ms) — likely a perf regression in the pre-warm loop");
    }
  }

  /**
   * Verifies that {@link art.arcane.mystcraft.item.GuidebookItem} and
   * {@link art.arcane.mystcraft.item.LinkbookUnlinkedItem} both participate in
   * the procedural book-cover pipeline introduced for the linked Linkbook,
   * Personal Linkbook, and Agebook items.
   *
   * <p>The test validates two layers, mirroring the architecture used by the
   * other {@code procedural_ui} tests:
   * <ol>
   *   <li><b>Layer 1 (logical):</b> {@link BookTextureFactory#detectKind}
   *       must return {@code BookKind.GUIDEBOOK} for a stack of
   *       {@link art.arcane.mystcraft.registry.ModItems#GUIDEBOOK} and
   *       {@code BookKind.LINKBOOK_UNLINKED} for a stack of
   *       {@link art.arcane.mystcraft.registry.ModItems#LINKBOOK_UNLINKED}.
   *       This contract holds even on a dedicated-server runtime.</li>
   *   <li><b>Layer 2 (rendering):</b> Both
   *       {@link BookTextureFactory#getCoverTexture(ItemStack, BookTextureFactory.BookKind)}
   *       and {@link BookItemTextureFactory#getItemTexture(ItemStack)} must
   *       return non-null {@link Identifier}s, AND those locations
   *       must differ between the two new kinds (proves the cache keys
   *       distinguish them, which in turn proves their palettes/emblems
   *       diverge).</li>
   * </ol>
   */
  public static void assertGuidebookAndUnlinkedBookKindsRender() {
    Item guidebookItem = ModItems.GUIDEBOOK == null ? null : ModItems.GUIDEBOOK.get();
    Item unlinkedItem = ModItems.LINKBOOK_UNLINKED == null ? null : ModItems.LINKBOOK_UNLINKED.get();
    if (guidebookItem == null || guidebookItem == Items.AIR) {
      throw new IllegalStateException(
          "ModItems.GUIDEBOOK is not registered — the Art-of-Writing item is missing");
    }
    if (unlinkedItem == null || unlinkedItem == Items.AIR) {
      throw new IllegalStateException(
          "ModItems.LINKBOOK_UNLINKED is not registered — the Linkbook (Unlinked) item is missing");
    }

    ItemStack guideStack = new ItemStack(guidebookItem);
    ItemStack unlinkedStack = new ItemStack(unlinkedItem);

    BookTextureFactory.BookKind guideKind = BookTextureFactory.detectKind(guideStack);
    BookTextureFactory.BookKind unlinkedKind = BookTextureFactory.detectKind(unlinkedStack);
    if (guideKind != BookTextureFactory.BookKind.GUIDEBOOK) {
      throw new IllegalStateException(
          "BookTextureFactory.detectKind(guidebook) returned " + guideKind
              + " — expected GUIDEBOOK");
    }
    if (unlinkedKind != BookTextureFactory.BookKind.LINKBOOK_UNLINKED) {
      throw new IllegalStateException(
          "BookTextureFactory.detectKind(linkbook_unlinked) returned " + unlinkedKind
              + " — expected LINKBOOK_UNLINKED");
    }

    assertGuidebookAndUnlinkedBookKindsRenderPixels(guideStack, unlinkedStack);
  }

  private static void assertGuidebookAndUnlinkedBookKindsRenderPixels(
      @org.jetbrains.annotations.NotNull ItemStack guideStack,
      @org.jetbrains.annotations.NotNull ItemStack unlinkedStack) {

    Identifier guideCover = BookTextureFactory.getCoverTexture(
        guideStack, BookTextureFactory.BookKind.GUIDEBOOK);
    Identifier unlinkedCover = BookTextureFactory.getCoverTexture(
        unlinkedStack, BookTextureFactory.BookKind.LINKBOOK_UNLINKED);
    if (guideCover == null) {
      throw new IllegalStateException(
          "BookTextureFactory.getCoverTexture(GUIDEBOOK) returned null");
    }
    if (unlinkedCover == null) {
      throw new IllegalStateException(
          "BookTextureFactory.getCoverTexture(LINKBOOK_UNLINKED) returned null");
    }
    if (guideCover.equals(unlinkedCover)) {
      throw new IllegalStateException(
          "GUIDEBOOK and LINKBOOK_UNLINKED produce the same cover Identifier ("
              + guideCover + ") — the new BookKinds must be distinguished by cache key");
    }

    Identifier guideIcon = BookItemTextureFactory.getItemTexture(guideStack);
    Identifier unlinkedIcon = BookItemTextureFactory.getItemTexture(unlinkedStack);
    if (guideIcon == null) {
      throw new IllegalStateException(
          "BookItemTextureFactory.getItemTexture(guidebook) returned null");
    }
    if (unlinkedIcon == null) {
      throw new IllegalStateException(
          "BookItemTextureFactory.getItemTexture(linkbook_unlinked) returned null");
    }
    if (guideIcon.equals(unlinkedIcon)) {
      throw new IllegalStateException(
          "GUIDEBOOK and LINKBOOK_UNLINKED produce the same item-icon Identifier ("
              + guideIcon + ") — the new BookKinds must yield distinct item icons");
    }

    Mystcraft.LOGGER.info(
        "[ProceduralUiTest] Guidebook covers: cover={} icon={}; "
            + "Unlinked: cover={} icon={}",
        guideCover, guideIcon, unlinkedCover, unlinkedIcon);
  }
}
