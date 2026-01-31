package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

/**
 * Forge 1.19.2-specific client helper.
 * Handles API differences for 1.19.2 compared to 1.20.x.
 * Uses PoseStack instead of GuiGraphics.
 */
public class ForgeClientHelper_1_19_2 implements IClientHelper {

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
  public void renderScreenBackground(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    // 1.19.2 API - renderBackground takes just PoseStack
    screen.renderBackground(poseStack);
  }
}
