package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
@GameTestHolder(Mystcraft.MOD_ID)
public class MystcraftNeoForgeGameTests {

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void registries_load(GameTestHelper helper) {
        MystcraftGameTestAssertions.assertRegistries(helper.getLevel());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void linkbook_drops_by_default(GameTestHelper helper) {
        MystcraftGameTestAssertions.assertLinkbookDropsByDefault();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", timeoutTicks = 600)
    public void random_books_create_dimensions(GameTestHelper helper) {
        MystcraftGameTestRunner.runRandomBookDimensionTest(helper);
    }
}
