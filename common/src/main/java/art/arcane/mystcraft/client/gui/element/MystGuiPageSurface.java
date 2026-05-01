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
 * A scrollable surface that displays pages/symbols.
 */
public class MystGuiPageSurface extends MystGuiElement {

  public static final float PAGE_WIDTH = 30;
  public static final float PAGE_HEIGHT = PAGE_WIDTH * 4 / 3;
  private final PagesProvider provider;
  private final List<Component> hoverTooltip = new ArrayList<>();
  private PositionableItem hoverItem;
  private int scrollOffset = 0;
  private int maxScroll = 0;
  private String searchText = "";
  private boolean mouseDown = false;

  public MystGuiPageSurface(PagesProvider provider, int left, int top, int width, int height) {
    super(left, top, width, height);
    this.provider = provider;
  }

  public void setSearchText(String text) {
    this.searchText = text != null ? text : "";
  }

  @Override
  protected void doRenderBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int guiLeft = getLeft();
    int guiTop = getTop();
    int scrollbarWidth = 16;
    int contentWidth = width - scrollbarWidth;

    graphics.fill(guiLeft, guiTop, guiLeft + contentWidth, guiTop + height, 0xAA000000);

    graphics.fill(guiLeft + contentWidth, guiTop, guiLeft + width, guiTop + height, 0x80404040);

    if (maxScroll > 0) {
      int scrollbarHeight = Math.max(20, (height * height) / (height + maxScroll));
      int scrollbarY = guiTop + (scrollOffset * (height - scrollbarHeight)) / maxScroll;
      graphics.fill(guiLeft + contentWidth + 2, scrollbarY, guiLeft + width - 2, scrollbarY + scrollbarHeight, 0xFFC0C0C0);
    }

    graphics.enableScissor(guiLeft, guiTop, guiLeft + contentWidth, guiTop + height);

    List<PositionableItem> pages = provider != null ? provider.getPositionedPages() : null;
    hoverItem = null;
    hoverTooltip.clear();
    maxScroll = 0;

    if (pages != null) {
      for (PositionableItem item : pages) {
        float pageX = guiLeft + item.x;
        float pageY = guiTop + item.y - scrollOffset;

        if (item.y + PAGE_HEIGHT > maxScroll + height) {
          maxScroll = (int) (item.y + PAGE_HEIGHT + 6 - height);
        }

        if (pageY + PAGE_HEIGHT < guiTop || pageY > guiTop + height) {
          continue;
        }

        ItemStack stack = item.itemstack;
        if (!searchText.isEmpty() && !stack.isEmpty()) {
          String displayName = getDisplayName(stack);
          if (displayName != null && !displayName.toLowerCase().contains(searchText.toLowerCase())) {
            continue;
          }
        }

        if (item.count > 0 && !stack.isEmpty()) {
          renderPage(graphics, stack, (int) pageX, (int) pageY, (int) PAGE_WIDTH, (int) PAGE_HEIGHT);

          if (item.count > 1) {
            String countStr = String.valueOf(item.count);
            graphics.drawString(mc.font, countStr,
                (int) (pageX + PAGE_WIDTH - mc.font.width(countStr) - 2),
                (int) (pageY + PAGE_HEIGHT - 10), 0xFFFFFF);
          }

          if (mouseX >= pageX && mouseX < pageX + PAGE_WIDTH &&
              mouseY >= pageY && mouseY < pageY + PAGE_HEIGHT &&
              mouseX < guiLeft + contentWidth) {
            hoverItem = item;
            updateHoverTooltip(stack);

            graphics.fill((int) pageX, (int) pageY,
                (int) (pageX + PAGE_WIDTH), (int) (pageY + PAGE_HEIGHT),
                0x40FFFFFF);
          }
        }
      }
    }

    graphics.disableScissor();

    if (maxScroll < 0) maxScroll = 0;
    if (scrollOffset > maxScroll) scrollOffset = maxScroll;
  }

  private void renderPage(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height) {

    graphics.renderItem(stack, x + (width - 16) / 2, y + (height - 16) / 2);
  }

  @Nullable
  private String getDisplayName(ItemStack stack) {
    ResourceLocation symbolId = Page.getSymbol(stack);
    if (symbolId != null) {
      IAgeSymbol symbol = SymbolRegistry.get(symbolId);
      if (symbol != null) {
        return symbol.getLocalizedName();
      }
      return symbolId.getPath();
    }
    return stack.getHoverName().getString();
  }

  private void updateHoverTooltip(ItemStack stack) {
    hoverTooltip.clear();

    List<Component> pageTooltip = new java.util.ArrayList<>();
    Page.getTooltip(stack, pageTooltip);
    hoverTooltip.addAll(pageTooltip);

    String displayName = getDisplayName(stack);
    if (displayName != null) {
      hoverTooltip.add(Component.literal(displayName));
    }
  }

  @Override
  @Nullable
  protected List<Component> getOwnTooltip(int mouseX, int mouseY) {
    if (hoverItem != null && !hoverTooltip.isEmpty()) {
      return hoverTooltip;
    }
    return null;
  }

  @Override
  protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
    int guiLeft = getLeft();
    int contentWidth = width - 16;

    if (mouseX < guiLeft + contentWidth) {
      if (provider == null) return false;

      ItemStack carried = mc.player.containerMenu.getCarried();
      if (!carried.isEmpty()) {
        List<PositionableItem> pages = provider.getPositionedPages();
        int index = pages != null ? pages.size() : 0;
        if (hoverItem != null) {
          index = hoverItem.slotId;
        }
        provider.place(index, button == 1);
        return true;
      }

      if (hoverItem != null && button == 2) {
        provider.copy(hoverItem);
        return true;
      }

      if (hoverItem != null && button == 0) {
        provider.pickup(hoverItem);
        return true;
      }

      mouseDown = true;
    }

    return false;
  }

  @Override
  protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
    int guiLeft = getLeft();
    int contentWidth = width - 16;

    if (mouseX >= guiLeft && mouseX < guiLeft + contentWidth &&
        hoverItem != null && button == 1 && mouseDown) {
      provider.copy(hoverItem);
    }

    mouseDown = false;
    return false;
  }

  @Override
  protected boolean onMouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta > 0) {
      scrollOffset = Math.max(0, scrollOffset - 20);
    } else if (delta < 0) {
      scrollOffset = Math.min(maxScroll, scrollOffset + 20);
    }
    return true;
  }

  /**
   * Provider interface for positioned pages.
   */
  public interface PagesProvider {
    List<PositionableItem> getPositionedPages();

    void place(int index, boolean single);

    void pickup(PositionableItem item);

    void copy(PositionableItem item);
  }

  /**
   * A positionable item on the surface.
   */
  public static class PositionableItem {
    public int slotId;
    public ItemStack itemstack = ItemStack.EMPTY;
    public float x;
    public float y;
    public int count = 1;

    public PositionableItem(int slotId, ItemStack itemstack, float x, float y, int count) {
      this.slotId = slotId;
      this.itemstack = itemstack;
      this.x = x;
      this.y = y;
      this.count = count;
    }
  }
}
