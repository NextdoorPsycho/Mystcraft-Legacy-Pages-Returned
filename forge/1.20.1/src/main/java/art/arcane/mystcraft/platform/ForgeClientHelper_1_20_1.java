package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Forge 1.20.1-specific client helper. Handles Forge client API calls for
 * Minecraft 1.20.1.
 */
public class ForgeClientHelper_1_20_1 implements IClientHelper {

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

    screen.renderBackground(graphics);
  }
}
