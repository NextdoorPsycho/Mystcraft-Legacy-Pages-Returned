package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

/**
 * Assertions that are safe to load and execute on a dedicated server.
 *
 * <p>This class must remain free of client, rendering, and Blaze3D references.
 * Client-only procedural texture assertions are isolated from dedicated-server registration.
 */
public final class MystcraftServerGameTestAssertions {

  private MystcraftServerGameTestAssertions() {
  }

  public static void assertCoreGameplayContentLoaded() {
    assertSymbolsLoaded();
    assertDatapacksLoaded();
    assertCreativeTabsLoaded();
    assertItemsRegistered();
    assertTableBlocksRegistered();
    assertEntitiesRegistered();
  }

  public static void assertRegisteredObjectsReachable() {
    if (ModItems.PERSONAL_LINK_BOOK.get() == Items.AIR) {
      throw new IllegalStateException("Personal link book registry object resolved to air");
    }
    if (ModBlocks.BOOK_BINDER.get() == Blocks.AIR) {
      throw new IllegalStateException("Book Binder registry object resolved to air");
    }
  }

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
      throw new IllegalStateException("ItemStackNbt lost custom data during round-trip");
    }
    if (!ItemStackNbt.hasCustomHoverName(loaded)
        || !"Component Test Book".equals(loaded.getHoverName().getString())) {
      throw new IllegalStateException("ItemStackNbt lost the custom name during round-trip");
    }
    if (!ItemStackNbt.isSameItemSameTags(original, loaded)) {
      throw new IllegalStateException("ItemStackNbt changed item components during round-trip");
    }

    CompoundTag detached = ItemStackNbt.getOrCreateTag(loaded);
    detached.putBoolean("DetachedMutation", true);
    CompoundTag beforeCommit = ItemStackNbt.getTag(loaded);
    if (beforeCommit != null && beforeCommit.getBooleanOr("DetachedMutation", false)) {
      throw new IllegalStateException("ItemStackNbt returned a live custom-data view");
    }
    ItemStackNbt.setTag(loaded, detached);
    CompoundTag afterCommit = ItemStackNbt.getTag(loaded);
    if (afterCommit == null || !afterCommit.getBooleanOr("DetachedMutation", false)) {
      throw new IllegalStateException("ItemStackNbt did not commit a detached mutation");
    }

    if (!ItemStackNbt.load(ItemStackNbt.save(ItemStack.EMPTY)).isEmpty()) {
      throw new IllegalStateException("ItemStackNbt did not preserve an empty stack");
    }
  }

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

    CompoundTag sanitized = LinkPermissions.load(permissionTag).save(new CompoundTag());
    CompoundTag sanitizedBlacklist = sanitized.getCompoundOrEmpty("EntryBlacklist");
    ListTag retainedPlayers = sanitizedBlacklist.getListOrEmpty("17");
    if (sanitizedBlacklist.contains("not-an-age")
        || retainedPlayers.size() != 1
        || !validPlayer.toString().equals(retainedPlayers.getStringOr(0, ""))
        || !sanitized.getCompoundOrEmpty("AgeOwners").isEmpty()
        || sanitized.getListOrEmpty("GlobalAdmins").size() != 1) {
      throw new IllegalStateException("LinkPermissions did not sanitize malformed entries");
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
      throw new IllegalStateException("AgeData did not recover safe persisted defaults");
    }
  }

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
    Map<String, Float> linkProperties = new HashMap<>();
    linkProperties.put("intra_linking", 0.2f);
    before.add(new InkAffinity.Entry(
        1.0f, 2, symbols, categories, poemTokens, linkProperties), 1);

    if (before.isEmpty() || before.tierBonus() != 2) {
      throw new IllegalStateException("InkBlend did not accumulate its affinity entry");
    }
    Identifier denseOres = Identifier.fromNamespaceAndPath("mystcraft", "dense_ores");
    if (before.symbolWeight(denseOres) <= 0.0f) {
      throw new IllegalStateException("Symbol weight for dense_ores was not accumulated");
    }

    CompoundTag tag = before.toNbt();
    if (tag == null || tag.isEmpty()) {
      throw new IllegalStateException("InkBlend.toNbt produced an empty tag");
    }

    InkBlend after = InkBlend.fromTag(tag);
    if (after.isEmpty() || after.tierBonus() != before.tierBonus()) {
      throw new IllegalStateException("InkBlend tier state did not round-trip");
    }
    assertNear(before.symbolWeight(denseOres), after.symbolWeight(denseOres), "symbol weight");
    assertNear(
        before.categoryWeight(SymbolCategory.FEATURE_MEDIUM),
        after.categoryWeight(SymbolCategory.FEATURE_MEDIUM),
        "category weight");
    assertNear(before.poemTokenSum(new String[]{"Deep"}),
        after.poemTokenSum(new String[]{"Deep"}), "poem-token weight");
    assertNear(before.linkPropertyProbability("intra_linking"),
        after.linkPropertyProbability("intra_linking"), "link-property weight");
  }

  public static void assertBookCoverNbtRoundTripsForEachCover() {
    List<String> covers = MystcraftConfig.bookBinderCoverItems.get();
    if (covers == null || covers.isEmpty()) {
      throw new IllegalStateException("MystcraftConfig.bookBinderCoverItems is empty");
    }

    for (String coverId : covers) {
      Identifier cover = Identifier.tryParse(coverId);
      if (cover == null) {
        throw new IllegalStateException("Invalid configured cover identifier: " + coverId);
      }
      CompoundTag tag = LinkOptions.setCoverItemId(new CompoundTag(), cover);
      Identifier parsed = LinkOptions.getCoverItemId(tag);
      if (!cover.equals(parsed)) {
        throw new IllegalStateException("Cover NBT did not round-trip for " + coverId + ": " + parsed);
      }
      Item item = BuiltInRegistries.ITEM.getValue(cover);
      if (item == Items.AIR && !"mystcraft:folder".equals(coverId)) {
        throw new IllegalStateException("Configured cover item is not registered: " + coverId);
      }
    }

    CompoundTag cleared = LinkOptions.setCoverItemId(
        new CompoundTag(), Identifier.fromNamespaceAndPath("minecraft", "leather"));
    LinkOptions.setCoverItemId(cleared, null);
    if (LinkOptions.getCoverItemId(cleared) != null) {
      throw new IllegalStateException("Setting cover NBT to null did not clear the field");
    }
  }

  public static void assertInkAffinityBiasesSymbolRoll() {
    Identifier favored = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "dense_ores");
    if (!SymbolRegistry.contains(favored)) {
      throw new IllegalStateException("Required affinity test symbol is missing: " + favored);
    }

    InkBlend blend = new InkBlend();
    Map<Identifier, Float> symbols = new HashMap<>();
    symbols.put(favored, InkBlend.WEIGHT_CAP);
    blend.add(new InkAffinity.Entry(1.0f, 0, symbols, Map.of(), Map.of(), Map.of()), 1);

    int rolls = 1000;
    int favoredHitsWithBlend = 0;
    int favoredHitsBaseline = 0;
    int totalRolledWithBlend = 0;
    int totalRolledBaseline = 0;
    RandomSource blendRandom = RandomSource.create(0xCAFE_BABEL);
    RandomSource baselineRandom = RandomSource.create(0xCAFE_BABEL);

    for (int i = 0; i < rolls; i++) {
      IAgeSymbol withBlend = SymbolRegistry.getRandomWeightedWithAffinity(blendRandom, blend);
      if (withBlend != null) {
        totalRolledWithBlend++;
        if (favored.equals(withBlend.getRegistryName())) {
          favoredHitsWithBlend++;
        }
      }
      IAgeSymbol baseline = SymbolRegistry.getRandomWeightedWithAffinity(baselineRandom, null);
      if (baseline != null) {
        totalRolledBaseline++;
        if (favored.equals(baseline.getRegistryName())) {
          favoredHitsBaseline++;
        }
      }
    }

    if (totalRolledWithBlend == 0 || totalRolledBaseline == 0) {
      throw new IllegalStateException("Symbol roller produced no symbols in " + rolls + " attempts");
    }
    double blendRate = favoredHitsWithBlend / (double) totalRolledWithBlend;
    double baselineRate = favoredHitsBaseline / (double) totalRolledBaseline;
    if (favoredHitsWithBlend <= favoredHitsBaseline
        || (baselineRate > 0.0 && blendRate / baselineRate < 1.5)) {
      throw new IllegalStateException(
          "Affinity did not sufficiently bias " + favored + ": with=" + blendRate
              + ", baseline=" + baselineRate);
    }

    InkBlend tierBlend = new InkBlend();
    tierBlend.add(new InkAffinity.Entry(1.0f, 1, Map.of(), Map.of(), Map.of(), Map.of()), 1);
    if (tierBlend.tierBonus() != 1
        || SymbolRegistry.getRandomWeightedWithAffinity(
            RandomSource.create(0xDEADBEEFL), tierBlend) == null) {
      throw new IllegalStateException("Tier-only affinity did not propagate through symbol selection");
    }
  }

  /** Verifies the pure datapack parser contract without loading a renderer. */
  public static void assertSymbolDisplayDataOverrideApplied() {
    JsonObject json = JsonParser.parseString("{"
        + "\"motif\":\"compass\","
        + "\"palette_override\":{"
        + "\"base\":\"#FF6B4F2C\","
        + "\"accent\":\"#FFE8B070\","
        + "\"ink\":\"#FF1A0F08\","
        + "\"halo\":\"#FFFFD9A0\"},"
        + "\"glyph_seeds\":{\"Stone\":-889275714,\"Fire\":\"0xCAFEBABE\"}}")
        .getAsJsonObject();
    SymbolDisplay parsed = SymbolDisplay.fromJson(
        Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "test/server_override"), json);
    if (parsed == null || !"compass".equals(parsed.motifName())) {
      throw new IllegalStateException("SymbolDisplay motif override did not parse");
    }
    if (parsed.paletteOverride() == null
        || parsed.paletteOverride().accent() == null
        || (parsed.paletteOverride().accent() & 0xFFFFFF) != 0xE8B070) {
      throw new IllegalStateException("SymbolDisplay palette override did not parse");
    }
    if (!Integer.valueOf(-889275714).equals(parsed.seedFor("Stone"))
        || !Integer.valueOf(0xCAFEBABE).equals(parsed.seedFor("Fire"))) {
      throw new IllegalStateException("SymbolDisplay glyph seeds did not parse");
    }
    SymbolDisplay empty = SymbolDisplay.fromJson(
        Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "test/server_empty"),
        JsonParser.parseString("{}").getAsJsonObject());
    if (empty != null) {
      throw new IllegalStateException("An empty SymbolDisplay block should collapse to null");
    }
  }

  private static void assertSymbolsLoaded() {
    int count = SymbolRegistry.getAll().size();
    if (count < 400) {
      throw new IllegalStateException("Expected 400+ symbols loaded, but found " + count);
    }
  }

  private static void assertCreativeTabsLoaded() {
    assertCreativeTabRegistered("mystcraft");
    assertCreativeTabRegistered("mystcraft_pages");
  }

  private static void assertItemsRegistered() {
    String[] items = {
        "linkbook", "linkbook_unlinked", "personal_link_book", "agebook", "page",
        "folder", "portfolio", "booster", "inkvial", "guidebook", "writingdesk",
        "blockinkmixer", "blockbookbinder"
    };
    for (String item : items) {
      assertItemRegistered(item);
    }
  }

  private static void assertDatapacksLoaded() {
    String[] symbols = {
        "terrain_flat", "terrain_cave", "biome_plains", "biome_forest",
        "weather_normal", "lighting_normal", "color_sky_natural"
    };
    for (String symbol : symbols) {
      assertSymbolExists(symbol);
    }
  }

  private static void assertTableBlocksRegistered() {
    assertBlockRegistered("writingdesk");
    assertBlockRegistered("blockinkmixer");
    assertBlockRegistered("blockbookbinder");
    assertBlockRegistered("blockbookreceptacle");
  }

  private static void assertEntitiesRegistered() {
    if (ModEntities.LINKBOOK == null || ModEntities.LINKBOOK.get() == null) {
      throw new IllegalStateException("Linkbook entity type is not registered");
    }
    if (ModEntities.PERSONAL_POCKET_PROXY == null
        || ModEntities.PERSONAL_POCKET_PROXY.get() == null) {
      throw new IllegalStateException("Personal pocket proxy entity type is not registered");
    }
  }

  private static void assertItemRegistered(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    if (BuiltInRegistries.ITEM.getValue(id) == Items.AIR) {
      throw new IllegalStateException("Item is not registered: " + id);
    }
  }

  private static void assertBlockRegistered(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    Block block = BuiltInRegistries.BLOCK.getValue(id);
    if (block == Blocks.AIR) {
      throw new IllegalStateException("Block is not registered: " + id);
    }
  }

  private static void assertCreativeTabRegistered(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) {
      throw new IllegalStateException("Creative tab is not registered: " + id);
    }
  }

  private static void assertSymbolExists(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    if (!SymbolRegistry.contains(id)) {
      throw new IllegalStateException("Symbol is missing: " + id);
    }
  }

  private static void assertNear(float expected, float actual, String label) {
    if (Math.abs(expected - actual) > 0.001f) {
      throw new IllegalStateException(
          "InkBlend " + label + " did not round-trip: expected=" + expected + ", actual=" + actual);
    }
  }
}
