package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
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
}
