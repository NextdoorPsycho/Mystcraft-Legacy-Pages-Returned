package art.arcane.mystcraft.client.gui.element;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable horizontal page list element (1.19.2 version).
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
  protected void doRenderBackground(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
    int guiLeft = getLeft();
    int guiTop = getTop();

    // Draw background
    GuiComponent.fill(poseStack, guiLeft, guiTop, guiLeft + width, guiTop + height, 0x80000000);

    List<ItemStack> pages = handler != null ? handler.getPageList() : null;
    if (pages == null) {
      pages = List.of();
    }

    hoverIndex = -1;
    hoverTooltip.clear();

    // Calculate how many pages fit
    int pagesPerRow = width / PAGE_SLOT_SIZE;
    int rowCount = (height + PAGE_SLOT_SIZE - 1) / PAGE_SLOT_SIZE;
    int visibleSlots = pagesPerRow * rowCount;

    // Draw pages
    for (int i = 0; i < visibleSlots && (i + scrollOffset) < pages.size(); i++) {
      int pageIndex = i + scrollOffset;
      ItemStack page = pages.get(pageIndex);

      int col = i % pagesPerRow;
      int row = i / pagesPerRow;
      int x = guiLeft + col * PAGE_SLOT_SIZE + 1;
      int y = guiTop + row * PAGE_SLOT_SIZE + 1;

      // Draw slot background
      GuiComponent.fill(poseStack, x, y, x + PAGE_ICON_SIZE, y + PAGE_ICON_SIZE, 0xFF2D2D2D);

      if (!page.isEmpty()) {
        // Draw page item
        mc.getItemRenderer().renderAndDecorateItem(page, x, y);

        // Check if first position needs link panel warning
        if (pageIndex == 0 && !Page.isLinkPanel(page)) {
          // Red tint for missing link panel
          GuiComponent.fill(poseStack, x - 1, y - 1, x + PAGE_ICON_SIZE + 1, y, 0xFFFF0000);
          GuiComponent.fill(poseStack, x - 1, y + PAGE_ICON_SIZE, x + PAGE_ICON_SIZE + 1, y + PAGE_ICON_SIZE + 1, 0xFFFF0000);
          GuiComponent.fill(poseStack, x - 1, y, x, y + PAGE_ICON_SIZE, 0xFFFF0000);
          GuiComponent.fill(poseStack, x + PAGE_ICON_SIZE, y, x + PAGE_ICON_SIZE + 1, y + PAGE_ICON_SIZE, 0xFFFF0000);
        }

        // Check hover
        if (mouseX >= x && mouseX < x + PAGE_ICON_SIZE &&
            mouseY >= y && mouseY < y + PAGE_ICON_SIZE) {
          hoverIndex = pageIndex;
          updateHoverTooltip(page);
          // Draw highlight
          GuiComponent.fill(poseStack, x, y, x + PAGE_ICON_SIZE, y + PAGE_ICON_SIZE, 0x40FFFFFF);
        }
      } else {
        // Check hover on empty slot
        if (mouseX >= x && mouseX < x + PAGE_ICON_SIZE &&
            mouseY >= y && mouseY < y + PAGE_ICON_SIZE) {
          hoverIndex = pageIndex;
        }
      }
    }

    // Draw scrollbar if needed
    int totalSlots = pages.size();
    int maxScroll = Math.max(0, totalSlots - visibleSlots);
    if (maxScroll > 0) {
      int scrollbarX = guiLeft + width - 4;
      int scrollbarHeight = Math.max(8, (height * visibleSlots) / totalSlots);
      int scrollbarY = guiTop + (scrollOffset * (height - scrollbarHeight)) / maxScroll;

      GuiComponent.fill(poseStack, scrollbarX, guiTop, scrollbarX + 3, guiTop + height, 0xFF404040);
      GuiComponent.fill(poseStack, scrollbarX, scrollbarY, scrollbarX + 3, scrollbarY + scrollbarHeight, 0xFFC0C0C0);
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
        hoverTooltip.add(new TextComponent(symbol.getLocalizedName()));
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
      // Insert at hover index or end
      int insertIndex = hoverIndex >= 0 ? hoverIndex : pages.size();
      handler.onItemPlace(insertIndex, button == 1);
      return true;
    } else if (hoverIndex >= 0 && hoverIndex < pages.size()) {
      // Remove from hover index
      handler.onItemRemove(hoverIndex);
      return true;
    }

    return false;
  }

  // 1.19.2 API: mouseScrolled has 3 parameters (mouseX, mouseY, delta)
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
