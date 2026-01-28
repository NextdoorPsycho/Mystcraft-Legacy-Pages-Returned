package art.arcane.mystcraft.fabric.gametest;

import art.arcane.mystcraft.gametest.MystcraftGameTestAssertions;
import art.arcane.mystcraft.gametest.MystcraftGameTestRunner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class MystcraftFabricGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registries_load(GameTestHelper helper) {
        MystcraftGameTestAssertions.assertRegistries(helper.getLevel());
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void linkbook_drops_by_default(GameTestHelper helper) {
        MystcraftGameTestAssertions.assertLinkbookDropsByDefault();
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 600)
    public void random_books_create_dimensions(GameTestHelper helper) {
        MystcraftGameTestRunner.runRandomBookDimensionTest(helper);
    }
}
