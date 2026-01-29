package art.arcane.mystcraft.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class MystcraftGameTests {

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
}
