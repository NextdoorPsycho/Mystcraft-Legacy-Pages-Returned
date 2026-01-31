package art.arcane.mystcraft.client.gui.element;

import com.floopowder.api.IFlooGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Mystcraft GUI elements.
 */
public abstract class MystGuiElement {

  protected Minecraft mc;
  protected int left;
  protected int top;
  protected int width;
  protected int height;
  protected float zLevel = 0;
  protected boolean visible = true;
  protected boolean enabled = true;
  protected MystGuiElement parent;
  protected List<MystGuiElement> children = new ArrayList<>();
  protected List<Component> tooltip;

  public MystGuiElement(int left, int top, int width, int height) {
    this.mc = Minecraft.getInstance();
    this.left = left;
    this.top = top;
    this.width = width;
    this.height = height;
  }

  public void addElement(MystGuiElement element) {
    element.parent = this;
    children.add(element);
  }

  public void removeElement(MystGuiElement element) {
    element.parent = null;
    children.remove(element);
  }

  public void bringToFront(MystGuiElement element) {
    if (children.remove(element)) {
      children.add(element);
    }
  }

  public int getLeft() {
    return parent != null ? parent.getLeft() + left : left;
  }

  public void setLeft(int left) {
    this.left = left;
  }

  public int getTop() {
    return parent != null ? parent.getTop() + top : top;
  }

  public void setTop(int top) {
    this.top = top;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public float getZLevel() {
    return zLevel;
  }

  public void setZLevel(float zLevel) {
    this.zLevel = zLevel;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setTooltip(List<Component> tooltip) {
    this.tooltip = tooltip;
  }

  public boolean contains(double mouseX, double mouseY) {
    int l = getLeft();
    int t = getTop();
    return mouseX >= l && mouseX < l + width && mouseY >= t && mouseY < t + height;
  }

  // Lifecycle methods

  public void tick() {
    onTick();
    for (MystGuiElement child : children) {
      child.tick();
    }
  }

  protected void onTick() {
  }

  public void renderBackground(IFlooGraphics graphics, float partialTick, int mouseX, int mouseY) {
    if (!visible) return;
    doRenderBackground(graphics, partialTick, mouseX, mouseY);
    for (MystGuiElement child : children) {
      child.renderBackground(graphics, partialTick, mouseX, mouseY);
    }
  }

  protected void doRenderBackground(IFlooGraphics graphics, float partialTick, int mouseX, int mouseY) {
  }

  public void renderForeground(IFlooGraphics graphics, int mouseX, int mouseY) {
    if (!visible) return;
    doRenderForeground(graphics, mouseX, mouseY);
    for (MystGuiElement child : children) {
      child.renderForeground(graphics, mouseX, mouseY);
    }
  }

  protected void doRenderForeground(IFlooGraphics graphics, int mouseX, int mouseY) {
  }

  @Nullable
  public List<Component> getTooltipInfo(int mouseX, int mouseY) {
    if (!visible) return null;
    // Check children first (reverse order for z-order)
    for (int i = children.size() - 1; i >= 0; i--) {
      List<Component> childTooltip = children.get(i).getTooltipInfo(mouseX, mouseY);
      if (childTooltip != null) {
        return childTooltip;
      }
    }
    if (contains(mouseX, mouseY)) {
      return getOwnTooltip(mouseX, mouseY);
    }
    return null;
  }

  @Nullable
  protected List<Component> getOwnTooltip(int mouseX, int mouseY) {
    return tooltip;
  }

  // Input handling

  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!visible || !enabled) return false;
    // Check children first (reverse order for z-order)
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).mouseClicked(mouseX, mouseY, button)) {
        return true;
      }
    }
    if (contains(mouseX, mouseY)) {
      return onMouseClicked(mouseX, mouseY, button);
    }
    return false;
  }

  protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
    return false;
  }

  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (!visible || !enabled) return false;
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).mouseReleased(mouseX, mouseY, button)) {
        return true;
      }
    }
    return onMouseReleased(mouseX, mouseY, button);
  }

  protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
    return false;
  }

  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (!visible || !enabled) return false;
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
        return true;
      }
    }
    return onMouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  protected boolean onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    return false;
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (!visible || !enabled) return false;
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
        return true;
      }
    }
    if (contains(mouseX, mouseY)) {
      return onMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    return false;
  }

  protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    return false;
  }

  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (!visible || !enabled) return false;
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).keyPressed(keyCode, scanCode, modifiers)) {
        return true;
      }
    }
    return onKeyPressed(keyCode, scanCode, modifiers);
  }

  protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
    return false;
  }

  public boolean charTyped(char codePoint, int modifiers) {
    if (!visible || !enabled) return false;
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).charTyped(codePoint, modifiers)) {
        return true;
      }
    }
    return onCharTyped(codePoint, modifiers);
  }

  protected boolean onCharTyped(char codePoint, int modifiers) {
    return false;
  }
}
