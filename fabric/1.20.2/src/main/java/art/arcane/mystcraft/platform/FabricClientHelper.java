package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class FabricClientHelper implements IClientHelper {
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
    // 1.20.2 API - 4 parameters
    screen.renderBackground(graphics, mouseX, mouseY, partialTick);
  }
}
