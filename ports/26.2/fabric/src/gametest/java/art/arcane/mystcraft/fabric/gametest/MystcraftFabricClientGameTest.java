package art.arcane.mystcraft.fabric.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.gametest.MystcraftGameTestAssertions;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

/** Strict render-thread coverage for Mystcraft's procedural client pipeline. */
@SuppressWarnings("UnstableApiUsage")
public final class MystcraftFabricClientGameTest implements FabricClientGameTest {

  @Override
  public void runTest(ClientGameTestContext context) {
    if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
      throw new IllegalStateException("Mystcraft client GameTests require a client environment");
    }

    try (TestSingleplayerContext singleplayer = context.worldBuilder()
        .setUseConsistentSettings(true)
        .create()) {
      context.waitFor(client -> !client.isPaused() && client.level != null, 600);

      context.runOnClient(client -> {
        runAssertion("symbol glyph determinism",
            MystcraftGameTestAssertions::assertSymbolGlyphIsDeterministic);
        runAssertion("symbol motif dispatch",
            MystcraftGameTestAssertions::assertMotifDispatchPerCategory);
        runAssertion("symbol rank progression",
            MystcraftGameTestAssertions::assertSymbolRankProgression);
        runAssertion("symbol display override rendering",
            MystcraftGameTestAssertions::assertSymbolDisplayOverrideApplied);
        runAssertion("procedural cache reload",
            MystcraftGameTestAssertions::assertProceduralUiReloadFlushesSymbolCaches);
        runAssertion("procedural symbol warm",
            MystcraftGameTestAssertions::assertProceduralSymbolWarmCompletes);
        runAssertion("guidebook and unlinked book rendering",
            MystcraftGameTestAssertions::assertGuidebookAndUnlinkedBookKindsRender);
      });
    }
  }

  private static void runAssertion(String name, Runnable assertion) {
    Mystcraft.LOGGER.info("[ClientGameTest] START {}", name);
    assertion.run();
    Mystcraft.LOGGER.info("[ClientGameTest] PASS {}", name);
  }
}
