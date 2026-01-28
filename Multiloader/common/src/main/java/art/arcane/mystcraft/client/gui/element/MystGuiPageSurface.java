package art.arcane.mystcraft.client.gui.element;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.client.render.PageRenderHelper;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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

    /**
     * Provider interface for positioned pages.
     */
    public interface PagesProvider {
        List<PositionableItem> getPositionedPages();
        void place(int index, boolean single);
        void pickup(PositionableItem item);
        void copy(PositionableItem item);
    }

    private final PagesProvider provider;
    private PositionableItem hoverItem;
    private List<Component> hoverTooltip = new ArrayList<>();
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
    protected void doRenderBackground(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = getLeft();
        int guiTop = getTop();
        int scrollbarWidth = 16;
        int contentWidth = width - scrollbarWidth;

        // Draw background
        guiGraphics.fill(guiLeft, guiTop, guiLeft + contentWidth, guiTop + height, 0xAA000000);

        // Draw scrollbar background
        guiGraphics.fill(guiLeft + contentWidth, guiTop, guiLeft + width, guiTop + height, 0x80404040);

        // Calculate scrollbar position and size
        if (maxScroll > 0) {
            int scrollbarHeight = Math.max(20, (height * height) / (height + maxScroll));
            int scrollbarY = guiTop + (scrollOffset * (height - scrollbarHeight)) / maxScroll;
            guiGraphics.fill(guiLeft + contentWidth + 2, scrollbarY, guiLeft + width - 2, scrollbarY + scrollbarHeight, 0xFFC0C0C0);
        }

        // Enable scissor for content area
        RenderSystem.enableScissor(
                (int) (guiLeft * mc.getWindow().getGuiScale()),
                (int) ((mc.getWindow().getGuiScaledHeight() - guiTop - height) * mc.getWindow().getGuiScale()),
                (int) (contentWidth * mc.getWindow().getGuiScale()),
                (int) (height * mc.getWindow().getGuiScale())
        );

        // Render pages
        List<PositionableItem> pages = provider != null ? provider.getPositionedPages() : null;
        hoverItem = null;
        hoverTooltip.clear();
        maxScroll = 0;

        if (pages != null) {
            for (PositionableItem item : pages) {
                float pageX = guiLeft + item.x;
                float pageY = guiTop + item.y - scrollOffset;

                // Track max scroll
                if (item.y + PAGE_HEIGHT > maxScroll + height) {
                    maxScroll = (int) (item.y + PAGE_HEIGHT + 6 - height);
                }

                // Skip if out of view
                if (pageY + PAGE_HEIGHT < guiTop || pageY > guiTop + height) {
                    continue;
                }

                // Filter by search text
                ItemStack stack = item.itemstack;
                if (!searchText.isEmpty() && !stack.isEmpty()) {
                    String displayName = getDisplayName(stack);
                    if (displayName != null && !displayName.toLowerCase().contains(searchText.toLowerCase())) {
                        continue;
                    }
                }

                // Render page
                if (item.count > 0 && !stack.isEmpty()) {
                    renderPage(guiGraphics, stack, (int) pageX, (int) pageY, (int) PAGE_WIDTH, (int) PAGE_HEIGHT);

                    // Show count if more than 1
                    if (item.count > 1) {
                        String countStr = String.valueOf(item.count);
                        guiGraphics.drawString(mc.font, countStr,
                                (int) (pageX + PAGE_WIDTH - mc.font.width(countStr) - 2),
                                (int) (pageY + PAGE_HEIGHT - 10), 0xFFFFFF);
                    }

                    // Check hover
                    if (mouseX >= pageX && mouseX < pageX + PAGE_WIDTH &&
                        mouseY >= pageY && mouseY < pageY + PAGE_HEIGHT &&
                        mouseX < guiLeft + contentWidth) {
                        hoverItem = item;
                        updateHoverTooltip(stack);
                        // Draw highlight
                        guiGraphics.fill((int) pageX, (int) pageY,
                                (int) (pageX + PAGE_WIDTH), (int) (pageY + PAGE_HEIGHT),
                                0x40FFFFFF);
                    }
                }
            }
        }

        RenderSystem.disableScissor();

        // Cap scroll
        if (maxScroll < 0) maxScroll = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private void renderPage(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int width, int height) {
        // Use PageRenderHelper to draw the page with D'ni symbols
        PageRenderHelper.drawPage(guiGraphics, stack, x, y, width, height, 0);
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

        // Get page tooltip
        List<Component> pageTooltip = new java.util.ArrayList<>();
        Page.getTooltip(stack, pageTooltip);
        hoverTooltip.addAll(pageTooltip);

        // Add display name
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

        // Check if in content area
        if (mouseX < guiLeft + contentWidth) {
            if (provider == null) return false;

            // Check if holding item
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

            // Copy with middle click
            if (hoverItem != null && button == 2) {
                provider.copy(hoverItem);
                return true;
            }

            // Pickup with left click
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

        // Right-click release = copy
        if (mouseX >= guiLeft && mouseX < guiLeft + contentWidth &&
            hoverItem != null && button == 1 && mouseDown) {
            provider.copy(hoverItem);
        }

        mouseDown = false;
        return false;
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            scrollOffset = Math.max(0, scrollOffset - 20);
        } else if (scrollY < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 20);
        }
        return true;
    }
}
