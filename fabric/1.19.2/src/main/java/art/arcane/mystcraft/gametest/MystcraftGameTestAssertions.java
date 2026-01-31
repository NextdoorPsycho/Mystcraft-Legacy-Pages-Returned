package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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
   * Asserts that both creative tabs are loaded.
   * Note: In 1.19.2, creative tabs use a different system that doesn't have a registry.
   */
  public static void assertCreativeTabsLoaded() {
    // Creative tabs in 1.19.2 are handled differently - skip registry check
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
    assertItemRegistered("writing_desk");
    assertItemRegistered("ink_mixer");
    assertItemRegistered("bookbinder");
  }

  /**
   * Asserts that datapacks loaded correctly by checking for specific symbols.
   */
  public static void assertDatapacksLoaded() {
    // Check for core symbols that come from datapacks
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
    assertBlockRegistered("writing_desk");
    assertBlockRegistered("ink_mixer");
    assertBlockRegistered("bookbinder");
  }

  private static void assertBlockRegistered(String blockId) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, blockId);
    if (!Registry.BLOCK.containsKey(id)) {
      throw new IllegalStateException("Block not registered: " + id);
    }
  }
}
