package art.arcane.mystcraft.platform.services;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

/**
 * Abstracts client-side registration operations.
 * Handles menu screen bindings, entity renderers, block entity renderers, model layers, and item colors.
 * Also provides version-specific screen rendering utilities.
 *
 * 1.19.2 version: Uses PoseStack instead of GuiGraphics.
 */
public interface IClientHelper {

  /**
   * Registers menu screen factories (binds MenuType to Screen).
   */
  void registerMenuScreens();

  /**
   * Registers block entity renderers.
   */
  void registerBlockEntityRenderers();

  /**
   * Registers entity renderers.
   */
  void registerEntityRenderers();

  /**
   * Registers model layer definitions.
   */
  void registerModelLayers();

  /**
   * Registers item and block color handlers.
   */
  void registerColorHandlers();

  /**
   * Registers custom render types for blocks.
   */
  void registerRenderTypes();

  /**
   * Renders the screen background using the version-appropriate API.
   * <p>
   * In 1.19.2: calls screen.renderBackground(poseStack)
   *
   * @param screen      The screen to render background for
   * @param poseStack   The PoseStack for rendering
   * @param mouseX      Mouse X position
   * @param mouseY      Mouse Y position
   * @param partialTick Partial tick time
   */
  void renderScreenBackground(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTick);

  /**
   * Handles mouse scroll events using the version-appropriate API.
   *
   * @param scrollX Horizontal scroll amount
   * @param scrollY Vertical scroll amount
   * @return The effective scroll delta for vertical scrolling
   */
  default double getEffectiveScrollDelta(double scrollX, double scrollY) {
    return scrollY;
  }
}
