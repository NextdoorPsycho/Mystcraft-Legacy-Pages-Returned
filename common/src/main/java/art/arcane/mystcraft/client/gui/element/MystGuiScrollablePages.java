package art.arcane.mystcraft.client.gui.element;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable horizontal page list element.
 */
public class MystGuiScrollablePages extends MystGuiElement {

  private static final int PAGE_SLOT_SIZE = 18;
  private static final int PAGE_ICON_SIZE = 16;
  private final PageListHandler handler;
  private final List<Component> hoverTooltip = new ArrayList<>();
  private int scrollOffset = 0;
  private int hoverIndex = -1;

  public MystGuiScrollablePages(PageListHandler handler, int left, int top, int width, int height) {
    super(left, top, width, height);
    this.handler = handler;
  }

  @Override
  protected void doRenderBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int guiLeft = getLeft();
    int guiTop = getTop();

    graphics.fill(guiLeft, guiTop, guiLeft + width, guiTop + height, 0x80000000);

    List<ItemStack> pages = handler != null ? handler.getPageList() : null;
    if (pages == null) {
      pages = List.of();
    }

    hoverIndex = -1;
    hoverTooltip.clear();

    int pagesPerRow = width / PAGE_SLOT_SIZE;
    int rowCount = (height + PAGE_SLOT_SIZE - 1) / PAGE_SLOT_SIZE;
    int visibleSlots = pagesPerRow * rowCount;

    for (int i = 0; i < visibleSlots && (i + scrollOffset) < pages.size(); i++) {
      int pageIndex = i + scrollOffset;
      ItemStack page = pages.get(pageIndex);

      int col = i % pagesPerRow;
      int row = i / pagesPerRow;
      int x = guiLeft + col * PAGE_SLOT_SIZE + 1;
      int y = guiTop + row * PAGE_SLOT_SIZE + 1;

      graphics.fill(x, y, x + PAGE_ICON_SIZE, y + PAGE_ICON_SIZE, 0xFF2D2D2D);

      if (!page.isEmpty()) {

        graphics.renderItem(page, x, y);

        if (pageIndex == 0 && !Page.isLinkPanel(page)) {

          graphics.fill(x - 1, y - 1, x + PAGE_ICON_SIZE + 1, y, 0xFFFF0000);
          graphics.fill(x - 1, y + PAGE_ICON_SIZE, x + PAGE_ICON_SIZE + 1, y + PAGE_ICON_SIZE + 1, 0xFFFF0000);
          graphics.fill(x - 1, y, x, y + PAGE_ICON_SIZE, 0xFFFF0000);
          graphics.fill(x + PAGE_ICON_SIZE, y, x + PAGE_ICON_SIZE + 1, y + PAGE_ICON_SIZE, 0xFFFF0000);
        }

        if (mouseX >= x && mouseX < x + PAGE_ICON_SIZE &&
            mouseY >= y && mouseY < y + PAGE_ICON_SIZE) {
          hoverIndex = pageIndex;
          updateHoverTooltip(page);

          graphics.fill(x, y, x + PAGE_ICON_SIZE, y + PAGE_ICON_SIZE, 0x40FFFFFF);
        }
      } else {

        if (mouseX >= x && mouseX < x + PAGE_ICON_SIZE &&
            mouseY >= y && mouseY < y + PAGE_ICON_SIZE) {
          hoverIndex = pageIndex;
        }
      }
    }

    int totalSlots = pages.size();
    int maxScroll = Math.max(0, totalSlots - visibleSlots);
    if (maxScroll > 0) {
      int scrollbarX = guiLeft + width - 4;
      int scrollbarHeight = Math.max(8, (height * visibleSlots) / totalSlots);
      int scrollbarY = guiTop + (scrollOffset * (height - scrollbarHeight)) / maxScroll;

      graphics.fill(scrollbarX, guiTop, scrollbarX + 3, guiTop + height, 0xFF404040);
      graphics.fill(scrollbarX, scrollbarY, scrollbarX + 3, scrollbarY + scrollbarHeight, 0xFFC0C0C0);
    }
  }

  private void updateHoverTooltip(ItemStack page) {
    hoverTooltip.clear();

    List<Component> pageTooltip = new java.util.ArrayList<>();
    Page.getTooltip(page, pageTooltip);
    hoverTooltip.addAll(pageTooltip);

    ResourceLocation symbolId = Page.getSymbol(page);
    if (symbolId != null) {
      IAgeSymbol symbol = SymbolRegistry.get(symbolId);
      if (symbol != null) {
        hoverTooltip.add(Component.literal(symbol.getLocalizedName()));
      }
    }
  }

  @Override
  @Nullable
  protected List<Component> getOwnTooltip(int mouseX, int mouseY) {
    if (hoverIndex >= 0 && !hoverTooltip.isEmpty()) {
      return hoverTooltip;
    }
    return null;
  }

  @Override
  protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
    if (handler == null) return false;

    List<ItemStack> pages = handler.getPageList();
    if (pages == null) pages = List.of();

    ItemStack held = handler.getHeldItem();

    if (!held.isEmpty()) {

      int insertIndex = hoverIndex >= 0 ? hoverIndex : pages.size();
      handler.onItemPlace(insertIndex, button == 1);
      return true;
    } else if (hoverIndex >= 0 && hoverIndex < pages.size()) {

      handler.onItemRemove(hoverIndex);
      return true;
    }

    return false;
  }

  @Override
  protected boolean onMouseScrolled(double mouseX, double mouseY, double delta) {
    List<ItemStack> pages = handler != null ? handler.getPageList() : null;
    if (pages == null) return false;

    int pagesPerRow = width / PAGE_SLOT_SIZE;
    int rowCount = (height + PAGE_SLOT_SIZE - 1) / PAGE_SLOT_SIZE;
    int visibleSlots = pagesPerRow * rowCount;
    int maxScroll = Math.max(0, pages.size() - visibleSlots);

    if (delta > 0) {
      scrollOffset = Math.max(0, scrollOffset - pagesPerRow);
    } else if (delta < 0) {
      scrollOffset = Math.min(maxScroll, scrollOffset + pagesPerRow);
    }
    return true;
  }

  /**
   * Handler interface for page interactions.
   */
  public interface PageListHandler {
    List<ItemStack> getPageList();

    void onItemPlace(int index, boolean single);

    void onItemRemove(int index);

    ItemStack getHeldItem();
  }
}
