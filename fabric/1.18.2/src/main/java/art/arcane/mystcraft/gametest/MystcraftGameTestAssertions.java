package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
}
