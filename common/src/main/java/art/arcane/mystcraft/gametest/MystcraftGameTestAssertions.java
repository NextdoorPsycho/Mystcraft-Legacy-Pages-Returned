package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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
    assertBlockRegistered("blockbookstand");
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
}
