package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

/**
 * Forge 1.18.2-specific client helper.
 * Uses PoseStack instead of GuiGraphics (which doesn't exist in 1.18.2).
 */
public class ForgeClientHelper_1_18_2 implements IClientHelper {

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
    // Registered via ColorHandlerEvent
  }

  @Override
  public void registerRenderTypes() {
    // Registered in FMLClientSetupEvent
  }

  /**
   * Renders screen background using 1.18.2 API.
   * Note: In 1.18.2, renderBackground takes only a PoseStack parameter.
   */
  @Override
  public void renderScreenBackground(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    screen.renderBackground(poseStack);
  }
}
