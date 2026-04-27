package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Forge 1.20.1-specific client helper.
 * Handles Forge client API calls for Minecraft 1.20.1.
 */
public class ForgeClientHelper_1_20_1 implements IClientHelper {

  @Override
  public void registerMenuScreens() {
    // Registered in MystcraftForge via FMLClientSetupEvent
  }

  @Override
  public void registerBlockEntityRenderers() {
    // Registered via EntityRenderersEvent.RegisterRenderers
  }

  @Override
  public void registerEntityRenderers() {
    // Registered via EntityRenderersEvent.RegisterRenderers
  }

  @Override
  public void registerModelLayers() {
    // Registered via EntityRenderersEvent.RegisterLayerDefinitions
  }

  @Override
  public void registerColorHandlers() {
    // Registered via RegisterColorHandlersEvent
  }

  @Override
  public void registerRenderTypes() {
    // Registered in FMLClientSetupEvent
  }

  @Override
  public void renderScreenBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    // 1.20.1 API - 1 parameter (ignores mouseX, mouseY, partialTick)
    screen.renderBackground(graphics);
  }
}
