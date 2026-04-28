package art.arcane.mystcraft.client.gui.element;

import art.arcane.mystcraft.client.gui.procedural.GuiTheme;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab interface for page collections (folders/portfolios) on the side of the Writing Desk.
 */
public class MystGuiSurfaceTabs extends MystGuiElement {

  private static final int TAB_COUNT = 4;
  private static final int TAB_WIDTH = 58;
  private static final int TAB_HEIGHT = 37;
  private static final int ARROW_HEIGHT = 9;
  private final TabHandler handler;

  public MystGuiSurfaceTabs(TabHandler handler, int left, int top, int width, int height) {
    super(left, top, width, height);
    this.handler = handler;
  }

  private void cycleTabUp() {
    int topSlot = handler.getTopSlot();
    if (topSlot > 0) {
      handler.setTopTabSlot(topSlot - 1);
    }
  }

  private void cycleTabDown() {
    int topSlot = handler.getTopSlot();
    int maxTabs = handler.getMaxTabs();
    if (topSlot < maxTabs - TAB_COUNT) {
      handler.setTopTabSlot(topSlot + 1);
    }
  }

  @Override
  protected void doRenderBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int guiLeft = getLeft();
    int guiTop = getTop();
    int topSlot = handler.getTopSlot();
    int activeSlot = handler.getActiveTab();
    int maxTabs = handler.getMaxTabs();

    int tabY = guiTop;

    // Themed accents (fall back to legacy colors for unknown keys).
    int dark = GuiTheme.color("panel_border_dark");
    int activeBg = GuiTheme.color("accent_link");
    int activeInner = ProceduralUI.lighten(activeBg, 1.18f);
    int idleBg = ProceduralUI.darken(GuiTheme.color("panel_bg"), 0.55f);
    int idleInner = ProceduralUI.lighten(idleBg, 1.18f);
    int textOn = GuiTheme.color("panel_border_light");
    int textOff = 0xFFA0A0A0;
    int arrowOn = textOn;
    int arrowOff = 0xFF666666;

    // Up arrow
    int upArrowColor = topSlot > 0 ? arrowOn : arrowOff;
    if (activeSlot < topSlot) {
      upArrowColor = activeBg | 0xFF000000;
    }
    graphics.fill(guiLeft, tabY, guiLeft + TAB_WIDTH, tabY + ARROW_HEIGHT, dark);
    ProceduralUI.drawChevron(graphics, guiLeft + TAB_WIDTH / 2 - 3, tabY + 2, 5, ProceduralUI.ChevronDir.UP, upArrowColor);
    tabY += ARROW_HEIGHT;

    // Render tabs
    for (int i = 0; i < TAB_COUNT; i++) {
      int slot = topSlot + i;
      boolean isActive = (slot == activeSlot);

      int bg = isActive ? activeBg : idleBg;
      int inner = isActive ? activeInner : idleInner;
      graphics.fill(guiLeft, tabY, guiLeft + TAB_WIDTH, tabY + TAB_HEIGHT, bg);
      graphics.fill(guiLeft + 1, tabY + 1, guiLeft + TAB_WIDTH - 1, tabY + TAB_HEIGHT - 1, inner);

      // Slot number
      graphics.drawString(mc.font, String.valueOf(slot), guiLeft + 4, tabY + 3, textOn);

      // Item name if present
      ItemStack stack = handler.getItemInSlot(slot);
      if (!stack.isEmpty()) {
        String name = stack.getHoverName().getString();
        int maxWidth = TAB_WIDTH - 8;
        if (mc.font.width(name) > maxWidth) {
          // Truncate with ellipsis
          while (mc.font.width(name + "...") > maxWidth && name.length() > 0) {
            name = name.substring(0, name.length() - 1);
          }
          name = name + "...";
        }
        graphics.drawString(mc.font, name, guiLeft + 4, tabY + TAB_HEIGHT - 12, textOff);

        // Render item icon
        graphics.renderItem(stack, guiLeft + TAB_WIDTH - 20, tabY + 8);
      }

      tabY += TAB_HEIGHT;
    }

    // Down arrow
    int downArrowColor = topSlot < maxTabs - TAB_COUNT ? arrowOn : arrowOff;
    if (activeSlot >= topSlot + TAB_COUNT) {
      downArrowColor = activeBg | 0xFF000000;
    }
    graphics.fill(guiLeft, tabY, guiLeft + TAB_WIDTH, tabY + ARROW_HEIGHT, dark);
    ProceduralUI.drawChevron(graphics, guiLeft + TAB_WIDTH / 2 - 3, tabY + 2, 5, ProceduralUI.ChevronDir.DOWN, downArrowColor);
  }

  @Override
  protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
    int guiLeft = getLeft();
    int guiTop = getTop();
    int topSlot = handler.getTopSlot();

    int tabY = guiTop;

    // Check up arrow
    if (mouseX >= guiLeft && mouseX < guiLeft + TAB_WIDTH &&
        mouseY >= tabY && mouseY < tabY + ARROW_HEIGHT) {
      cycleTabUp();
      return true;
    }
    tabY += ARROW_HEIGHT;

    // Check tabs
    for (int i = 0; i < TAB_COUNT; i++) {
      int slot = topSlot + i;
      if (mouseX >= guiLeft && mouseX < guiLeft + TAB_WIDTH &&
          mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
        handler.onTabClick(button, slot);
        return true;
      }
      tabY += TAB_HEIGHT;
    }

    // Check down arrow
    if (mouseX >= guiLeft && mouseX < guiLeft + TAB_WIDTH &&
        mouseY >= tabY && mouseY < tabY + ARROW_HEIGHT) {
      cycleTabDown();
      return true;
    }

    return false;
  }

  @Override
  protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
    // W/Up = cycle up, S/Down = cycle down
    if (keyCode == 265 || keyCode == 87) { // Up or W
      cycleTabUp();
      return true;
    } else if (keyCode == 264 || keyCode == 83) { // Down or S
      cycleTabDown();
      return true;
    }
    return false;
  }

  @Override
  @Nullable
  protected List<Component> getOwnTooltip(int mouseX, int mouseY) {
    int guiLeft = getLeft();
    int guiTop = getTop();
    int topSlot = handler.getTopSlot();

    int tabY = guiTop + ARROW_HEIGHT;

    for (int i = 0; i < TAB_COUNT; i++) {
      int slot = topSlot + i;
      if (mouseX >= guiLeft && mouseX < guiLeft + TAB_WIDTH &&
          mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
        ItemStack stack = handler.getItemInSlot(slot);
        if (!stack.isEmpty()) {
          List<Component> tooltip = new ArrayList<>();
          tooltip.add(stack.getHoverName());
          return tooltip;
        }
      }
      tabY += TAB_HEIGHT;
    }

    return null;
  }

  /**
   * Handler interface for tab interactions.
   */
  public interface TabHandler {
    ItemStack getItemInSlot(int slot);

    void setTopTabSlot(int topSlot);

    void onTabClick(int button, int slot);

    int getTopSlot();

    int getActiveTab();

    int getMaxTabs();
  }
}
