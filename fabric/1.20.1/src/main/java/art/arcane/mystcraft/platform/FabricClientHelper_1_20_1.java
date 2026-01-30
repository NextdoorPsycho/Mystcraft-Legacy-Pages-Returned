package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric 1.20.1-specific client helper.
 * Handles API differences for 1.20.1 compared to 1.20.2.
 */
public class FabricClientHelper_1_20_1 implements IClientHelper {

  @Override
  public void registerMenuScreens() {
  }

  @Override
  public void registerBlockEntityRenderers() {
  }

  @Override
  public void registerEntityRenderers() {
  }

  @Override
  public void registerModelLayers() {
  }

  @Override
  public void registerColorHandlers() {
  }

  @Override
  public void registerRenderTypes() {
  }

  @Override
  public void renderScreenBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    // 1.20.1 API - 1 parameter (ignores mouseX, mouseY, partialTick)
    screen.renderBackground(graphics);
  }
}
