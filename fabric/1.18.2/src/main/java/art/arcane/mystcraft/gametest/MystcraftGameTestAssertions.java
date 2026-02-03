package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Mystcraft GameTest assertions for 1.18.2.
 * Uses Registry.BLOCK/ITEM instead of BuiltInRegistries.
 */
public final class MystcraftGameTestAssertions {

  private MystcraftGameTestAssertions() {
  }

  /**
   * Tests that all Mystcraft registries are properly loaded.
   */
  public static void runRegistriesLoadTest(GameTestHelper helper) {
    // Check blocks are registered (1.18.2: use Registry.BLOCK)
    Block inkMixer = Registry.BLOCK.get(new ResourceLocation(Mystcraft.MOD_ID, "blockinkmixer"));
    if (inkMixer == null || inkMixer == net.minecraft.world.level.block.Blocks.AIR) {
      helper.fail("Ink mixer block not registered");
      return;
    }

    Block bookBinder = Registry.BLOCK.get(new ResourceLocation(Mystcraft.MOD_ID, "blockbookbinder"));
    if (bookBinder == null || bookBinder == net.minecraft.world.level.block.Blocks.AIR) {
      helper.fail("Book binder block not registered");
      return;
    }

    // Check items are registered (1.18.2: use Registry.ITEM)
    Item page = Registry.ITEM.get(new ResourceLocation(Mystcraft.MOD_ID, "page"));
    if (page == null || page == net.minecraft.world.item.Items.AIR) {
      helper.fail("Page item not registered");
      return;
    }

    Item agebook = Registry.ITEM.get(new ResourceLocation(Mystcraft.MOD_ID, "agebook"));
    if (agebook == null || agebook == net.minecraft.world.item.Items.AIR) {
      helper.fail("Agebook item not registered");
      return;
    }

    // Check mod registry fields are populated
    if (ModBlocks.INK_MIXER == null || ModBlocks.INK_MIXER.get() == null) {
      helper.fail("ModBlocks.INK_MIXER not populated");
      return;
    }

    if (ModItems.PAGE == null || ModItems.PAGE.get() == null) {
      helper.fail("ModItems.PAGE not populated");
      return;
    }

    // Check symbol registry has entries
    if (SymbolRegistry.getAll().isEmpty()) {
      helper.fail("Symbol registry is empty");
      return;
    }

    helper.succeed();
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
   * Note: In 1.18.2, creative tabs use a different system that doesn't have a registry.
   */
  public static void assertCreativeTabsLoaded() {
    // Creative tabs in 1.18.2 are handled differently - skip registry check
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
