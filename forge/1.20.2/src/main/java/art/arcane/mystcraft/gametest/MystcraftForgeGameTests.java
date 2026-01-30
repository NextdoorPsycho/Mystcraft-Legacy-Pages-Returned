package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestDontPrefix
@GameTestHolder(value = Mystcraft.MOD_ID, namespace = "minecraft")
public class MystcraftForgeGameTests {

  @GameTest(template = "empty")
  public void registries_load(GameTestHelper helper) {
    MystcraftGameTestAssertions.assertRegistries(helper.getLevel());
    helper.succeed();
  }

  @GameTest(template = "empty")
  public void linkbook_drops_by_default(GameTestHelper helper) {
    MystcraftGameTestAssertions.assertLinkbookDropsByDefault();
    helper.succeed();
  }

  @GameTest(template = "empty")
  public void books_drop_as_linkbook_entity(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookDropEntityTest(helper);
  }

  @GameTest(template = "empty", timeoutTicks = 800)
  public void linkbook_entity_decays_and_drops(GameTestHelper helper) {
    MystcraftGameTestRunner.runLinkbookEntityDecayTest(helper);
  }

  @GameTest(template = "empty", timeoutTicks = 600)
  public void random_books_create_dimensions(GameTestHelper helper) {
    MystcraftGameTestRunner.runRandomBookDimensionTest(helper);
  }

  @GameTest(template = "empty", timeoutTicks = 600)
  public void preset_cave_book_creates_dimension(GameTestHelper helper) {
    MystcraftGameTestRunner.runPresetBookDimensionTest(helper);
  }
}
