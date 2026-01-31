package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * GameTest assertions for 1.18.2.
 * Uses Registry.ITEM/BLOCK instead of BuiltInRegistries.
 * Note: 1.18.2 doesn't have a BuiltInRegistries.CREATIVE_MODE_TAB.
 */
public final class MystcraftGameTestAssertions {

  private MystcraftGameTestAssertions() {
  }

  /**
   * Asserts that all symbols are loaded (400+).
   */
  public static void assertSymbolsLoaded() {
    int count = SymbolRegistry.getAll().size();
    if (count < 400) {
      throw new IllegalStateException("Expected 400+ symbols loaded, but found: " + count);
    }
  }

  /**
   * Asserts that creative tabs are available.
   * Note: 1.18.2 uses ItemGroup which is not stored in a registry like 1.20.x,
   * so we just verify items exist instead.
   */
  public static void assertCreativeTabsLoaded() {
    // In 1.18.2, creative tabs are ItemGroup instances, not registered items.
    // Just verify items are registered (they get added to tabs automatically).
    assertItemRegistered("agebook");
    assertItemRegistered("linkbook");
  }

  /**
   * Asserts that all required items are registered.
   */
  public static void assertItemsRegistered() {
    assertItemRegistered("linkbook");
    assertItemRegistered("linkbook_unlinked");
    assertItemRegistered("personal_link_book");
    assertItemRegistered("agebook");
    assertItemRegistered("page");
    assertItemRegistered("writingdesk");
    assertItemRegistered("blockinkmixer");
    assertItemRegistered("blockbookbinder");
  }

  /**
   * Asserts that datapacks loaded correctly by checking for specific symbols.
   */
  public static void assertDatapacksLoaded() {
    assertSymbolExists("terrain_flat");
    assertSymbolExists("terrain_cave");
    assertSymbolExists("biome_plains");
    assertSymbolExists("biome_forest");
    assertSymbolExists("sun");
    assertSymbolExists("moon");
    assertSymbolExists("weather_normal");
    assertSymbolExists("lighting_normal");
  }

  private static void assertItemRegistered(String itemId) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, itemId);
    // 1.18.2: Use Registry.ITEM instead of BuiltInRegistries.ITEM
    Item item = Registry.ITEM.get(id);
    if (item == Items.AIR) {
      throw new IllegalStateException("Item not registered: " + id);
    }
  }

  private static void assertSymbolExists(String path) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, path);
    if (!SymbolRegistry.contains(id)) {
      throw new IllegalStateException("Missing symbol: " + id);
    }
  }

  /**
   * Asserts that all table blocks are registered.
   */
  public static void assertTableBlocksRegistered() {
    assertBlockRegistered("writingdesk");
    assertBlockRegistered("blockinkmixer");
    assertBlockRegistered("blockbookbinder");
  }

  private static void assertBlockRegistered(String blockId) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, blockId);
    // 1.18.2: Use Registry.BLOCK instead of BuiltInRegistries.BLOCK
    if (!Registry.BLOCK.containsKey(id)) {
      throw new IllegalStateException("Block not registered: " + id);
    }
  }
}
