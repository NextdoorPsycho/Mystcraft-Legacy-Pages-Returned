package art.arcane.mystcraft.client.gui.element;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Container panel for other GUI elements (1.19.2 version).
 */
public class MystGuiPanel extends MystGuiElement {

  private AbstractContainerScreen<?> screen;

  public MystGuiPanel(int left, int top, int width, int height) {
    super(left, top, width, height);
  }

  /**
   * Gets the screen this panel is attached to.
   */
  @Nullable
  public AbstractContainerScreen<?> getScreen() {
    return screen;
  }

  /**
   * Sets the screen this panel is attached to.
   */
  public void setScreen(AbstractContainerScreen<?> screen) {
    this.screen = screen;
  }

  /**
   * Adds a child element to this panel.
   * Alias for addElement for convenience.
   */
  public void addChild(MystGuiElement element) {
    addElement(element);
  }

  /**
   * Removes a child element from this panel.
   * Alias for removeElement for convenience.
   */
  public void removeChild(MystGuiElement element) {
    removeElement(element);
  }

  /**
   * Gets tooltip for the element at the mouse position.
   * Alias for getTooltipInfo for convenience.
   */
  @Nullable
  public List<Component> getTooltip(int mouseX, int mouseY) {
    return getTooltipInfo(mouseX, mouseY);
  }
}
