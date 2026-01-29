package art.arcane.mystcraft.platform.services;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Abstracts client-side registration operations.
 * Handles menu screen bindings, entity renderers, block entity renderers, model layers, and item colors.
 * Also provides version-specific screen rendering utilities.
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
   * In 1.20.1: calls screen.renderBackground(graphics)
   * In 1.20.2: calls screen.renderBackground(graphics, mouseX, mouseY, partialTick)
   *
   * @param screen The screen to render background for
   * @param graphics The GuiGraphics context
   * @param mouseX Mouse X position
   * @param mouseY Mouse Y position
   * @param partialTick Partial tick time
   */
  void renderScreenBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

  /**
   * Handles mouse scroll events using the version-appropriate API.
   * <p>
   * In 1.20.1: passes (mouseX, mouseY, scrollDelta) - scrollDelta combines X and Y
   * In 1.20.2: passes (mouseX, mouseY, scrollX, scrollY) - separate scroll axes
   *
   * @param scrollX Horizontal scroll amount (0 in 1.20.1)
   * @param scrollY Vertical scroll amount (scrollDelta in 1.20.1)
   * @return The effective scroll delta for vertical scrolling
   */
  default double getEffectiveScrollDelta(double scrollX, double scrollY) {
    // Default to 1.20.2 behavior where scrollY is the vertical delta
    return scrollY;
  }
}
