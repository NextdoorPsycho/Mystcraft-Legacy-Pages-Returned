package art.arcane.mystcraft.platform.services;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

/**
 * Forge 1.18.2 version of IClientHelper interface.
 * Uses PoseStack instead of GuiGraphics (which doesn't exist in 1.18.2).
 */
public interface IClientHelper {

  void registerMenuScreens();

  void registerBlockEntityRenderers();

  void registerEntityRenderers();

  void registerModelLayers();

  void registerColorHandlers();

  void registerRenderTypes();

  void renderScreenBackground(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTick);
}
