package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

// Forge client registration is handled via @SubscribeEvent on FMLClientSetupEvent and related events.
public class ForgeClientHelper implements IClientHelper {

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
    // 1.20.2 API - 4 parameters
    screen.renderBackground(graphics, mouseX, mouseY, partialTick);
  }
}
