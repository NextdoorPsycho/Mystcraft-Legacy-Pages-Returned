package art.arcane.mystcraft.fabric.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Fabric 1.20.1-specific Mystcraft GameTests.
 * Uses simplified tests that don't require 1.20.2+ APIs.
 */
public class MystcraftFabricGameTests implements FabricGameTest {

  // ===== REGISTRATION TESTS =====

  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
  public void registries_load(GameTestHelper helper) {
    // Check items registered
    assertItemRegistered("linkbook");
    assertItemRegistered("agebook");
    assertItemRegistered("page");
    assertItemRegistered("guidebook");
    assertItemRegistered("inkvial");
    helper.succeed();
  }

  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
  public void symbols_loaded_400_plus(GameTestHelper helper) {
    int count = SymbolRegistry.getAll().size();
    if (count < 400) {
      helper.fail("Expected 400+ symbols loaded, but found: " + count);
      return;
    }
    helper.succeed();
  }

  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
  public void creative_tabs_loaded(GameTestHelper helper) {
    ResourceLocation mainTab = new ResourceLocation(Mystcraft.MOD_ID, "mystcraft");
    ResourceLocation pagesTab = new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_pages");

    if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(mainTab)) {
      helper.fail("Main creative tab not registered");
      return;
    }
    if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(pagesTab)) {
      helper.fail("Pages creative tab not registered");
      return;
    }
    helper.succeed();
  }

  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
  public void datapacks_loaded(GameTestHelper helper) {
    // Check for core symbols that come from datapacks
    assertSymbolExists(helper, "terrain_flat");
    assertSymbolExists(helper, "terrain_cave");
    assertSymbolExists(helper, "biome_plains");
    assertSymbolExists(helper, "sun");
    assertSymbolExists(helper, "moon");
    assertSymbolExists(helper, "weather_normal");
    assertSymbolExists(helper, "lighting_normal");
    helper.succeed();
  }

  // ===== BLOCK ENTITY TESTS =====

  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
  public void table_blocks_have_block_entities(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();

    BlockPos inkMixerPos = BlockPos.ZERO;
    BlockPos bookBinderPos = new BlockPos(2, 0, 0);

    helper.setBlock(inkMixerPos, ModBlocks.INK_MIXER.get().defaultBlockState());
    helper.setBlock(bookBinderPos, ModBlocks.BOOK_BINDER.get().defaultBlockState());

    helper.runAtTickTime(5, () -> {
      BlockPos absInkMixer = helper.absolutePos(inkMixerPos);
      BlockPos absBookBinder = helper.absolutePos(bookBinderPos);

      if (level.getBlockEntity(absInkMixer) == null) {
        helper.fail("Ink mixer block entity not created");
        return;
      }

      if (level.getBlockEntity(absBookBinder) == null) {
        helper.fail("Book binder block entity not created");
        return;
      }

      helper.succeed();
    });
  }

  // ===== HELPERS =====

  private static void assertItemRegistered(String itemId) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, itemId);
    Item item = BuiltInRegistries.ITEM.get(id);
    if (item == Items.AIR) {
      throw new IllegalStateException("Item not registered: " + id);
    }
  }

  private static void assertSymbolExists(GameTestHelper helper, String path) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, path);
    if (!SymbolRegistry.contains(id)) {
      helper.fail("Missing symbol: " + id);
    }
  }
}
