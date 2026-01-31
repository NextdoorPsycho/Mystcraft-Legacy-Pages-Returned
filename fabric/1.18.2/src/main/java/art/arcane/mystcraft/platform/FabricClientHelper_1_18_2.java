package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric 1.18.2-specific client helper.
 * Handles API differences for 1.18.2 compared to 1.20.x.
 */
public class FabricClientHelper_1_18_2 implements IClientHelper {

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
  public void renderScreenBackground(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    // 1.18.2 API - only PoseStack parameter
    screen.renderBackground(poseStack);
  }
}
