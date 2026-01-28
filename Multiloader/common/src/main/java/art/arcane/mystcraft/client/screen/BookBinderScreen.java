package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.menu.BookBinderMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Screen for the Book Binder block.
 * Includes text field for book name and scrollable page list.
 */
public class BookBinderScreen extends AbstractContainerScreen<BookBinderMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "gui/pagebinder.png");

    // Page list area
    private static final int PAGE_LIST_X = 7;
    private static final int PAGE_LIST_Y = 45;
    private static final int PAGE_LIST_WIDTH = 162; // 176 - 14
    private static final int PAGE_LIST_HEIGHT = 40;
    private static final int PAGE_SIZE = 16; // Size of each page icon
    private static final int PAGE_SLOT_SIZE = PAGE_SIZE + 2; // Including spacing
    private static final int PAGES_PER_ROW = PAGE_LIST_WIDTH / PAGE_SLOT_SIZE; // 162/18 = 9

    // Text field area
    private static final int TEXT_FIELD_X = 7;
    private static final int TEXT_FIELD_Y = 9;
    private static final int TEXT_FIELD_WIDTH = 116; // 176 - 60
    private static final int TEXT_FIELD_HEIGHT = 14;

    // Missing panel icon position
    private static final int MISSING_PANEL_X = 27;
    private static final int MISSING_PANEL_Y = 26;

    private EditBox titleField;
    private int scrollOffset = 0;
    private float warningAlpha = 1.0f;

    public BookBinderScreen(BookBinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 181;
    }

    @Override
    protected void init() {
        super.init();
        // Position inventory label for ySize=181 (at ySize - 94 = 87)
        this.inventoryLabelY = this.imageHeight - 94;

        // Create title text field
        titleField = new EditBox(this.font, this.leftPos + TEXT_FIELD_X, this.topPos + TEXT_FIELD_Y,
                TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT, Component.literal("Book Title"));
        titleField.setMaxLength(21);
        titleField.setBordered(true);
        titleField.setVisible(true);
        titleField.setTextColor(0xA0A0A0);

        // Set initial value from block entity
        BookBinderBlockEntity be = menu.getBlockEntity();
        titleField.setValue(be.getPendingTitle());

        // Set responder for text changes
        titleField.setResponder(this::onTitleChanged);

        addRenderableWidget(titleField);
    }

    private void onTitleChanged(String text) {
        // Send title change to server
        MystcraftNetwork.sendToServer(new ContainerActionPacket(
                ContainerActionPacket.Action.BOOK_BINDER_SET_TITLE,
                menu.containerId,
                false,
                text
        ));
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // Update warning alpha (pulsing effect)
        long time = System.currentTimeMillis();
        warningAlpha = (time % 4000) / 2000.0f;
        if (warningAlpha > 1) {
            warningAlpha = 2 - warningAlpha;
        }
        warningAlpha += 0.3f;
        warningAlpha = Math.min(1.0f, warningAlpha);

        // Update text field border color based on whether title is empty
        if (titleField.getValue().isEmpty()) {
            titleField.setTextColor(0xFF0000); // Red when empty
        } else {
            titleField.setTextColor(0xA0A0A0); // Normal gray
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw main GUI texture
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Draw page list
        renderPageList(guiGraphics, mouseX, mouseY);

        // Draw missing link panel warning if needed
        renderMissingPanelWarning(guiGraphics, mouseX, mouseY);
    }

    private void renderPageList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ItemStack> pages = menu.getBlockEntity().getPageList();

        int listLeft = this.leftPos + PAGE_LIST_X;
        int listTop = this.topPos + PAGE_LIST_Y;

        // Slot rendering colors (standard Minecraft slot style)
        int slotBgDark = 0xFF373737;   // Dark inner background
        int slotBorderDark = 0xFF373737;  // Top/left border (shadow)
        int slotBorderLight = 0xFFFFFFFF; // Bottom/right border (highlight)
        int slotBg = 0xFF8B8B8B;       // Main slot background

        // Calculate max visible slots (2 rows)
        int maxVisibleSlots = PAGES_PER_ROW * 2;

        // First pass: Draw slot backgrounds for ALL visible positions
        for (int i = 0; i < maxVisibleSlots; i++) {
            int col = i % PAGES_PER_ROW;
            int row = i / PAGES_PER_ROW;

            int x = listLeft + col * (PAGE_SIZE + 2); // +2 for spacing
            int y = listTop + row * (PAGE_SIZE + 2);

            // Draw 3D slot border (Minecraft style)
            // Top edge (dark)
            guiGraphics.fill(x, y, x + PAGE_SIZE + 2, y + 1, slotBorderDark);
            // Left edge (dark)
            guiGraphics.fill(x, y, x + 1, y + PAGE_SIZE + 2, slotBorderDark);
            // Bottom edge (light)
            guiGraphics.fill(x, y + PAGE_SIZE + 1, x + PAGE_SIZE + 2, y + PAGE_SIZE + 2, slotBorderLight);
            // Right edge (light)
            guiGraphics.fill(x + PAGE_SIZE + 1, y, x + PAGE_SIZE + 2, y + PAGE_SIZE + 2, slotBorderLight);
            // Inner background
            guiGraphics.fill(x + 1, y + 1, x + PAGE_SIZE + 1, y + PAGE_SIZE + 1, slotBg);
        }

        // Second pass: Draw page items on top of slots
        for (int i = scrollOffset; i < pages.size() && i < scrollOffset + maxVisibleSlots; i++) {
            int displayIndex = i - scrollOffset;
            int col = displayIndex % PAGES_PER_ROW;
            int row = displayIndex / PAGES_PER_ROW;

            int x = listLeft + col * (PAGE_SIZE + 2) + 1; // +1 to center in slot
            int y = listTop + row * (PAGE_SIZE + 2) + 1;

            ItemStack page = pages.get(i);
            if (!page.isEmpty()) {
                // Draw page item
                guiGraphics.renderItem(page, x, y);

                // Highlight first position if it's not a link panel
                if (i == 0 && !Page.isLinkPanel(page)) {
                    // Draw red border around first slot
                    int bx = x - 1;
                    int by = y - 1;
                    guiGraphics.fill(bx - 1, by - 1, bx + PAGE_SIZE + 2, by, 0xFFFF0000);
                    guiGraphics.fill(bx - 1, by + PAGE_SIZE + 1, bx + PAGE_SIZE + 2, by + PAGE_SIZE + 2, 0xFFFF0000);
                    guiGraphics.fill(bx - 1, by, bx, by + PAGE_SIZE + 1, 0xFFFF0000);
                    guiGraphics.fill(bx + PAGE_SIZE + 1, by, bx + PAGE_SIZE + 2, by + PAGE_SIZE + 1, 0xFFFF0000);
                }
            }
        }
    }

    private void renderMissingPanelWarning(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ItemStack> pages = menu.getBlockEntity().getPageList();

        // Show warning if no pages or first page is not a link panel
        boolean showWarning = pages.isEmpty() || !Page.isLinkPanel(pages.get(0));
        if (!showWarning) {
            return;
        }

        int warningX = this.leftPos + MISSING_PANEL_X;
        int warningY = this.topPos + MISSING_PANEL_Y;

        // Draw warning icon from texture at (176, 0)
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 0.5f, 0.5f, warningAlpha);
        guiGraphics.blit(TEXTURE, warningX, warningY, 176, 0, 18, 18);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render warning tooltip if hovering over warning area
        renderWarningTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderWarningTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ItemStack> pages = menu.getBlockEntity().getPageList();
        boolean showWarning = pages.isEmpty() || !Page.isLinkPanel(pages.get(0));
        if (!showWarning) {
            return;
        }

        int warningX = this.leftPos + MISSING_PANEL_X;
        int warningY = this.topPos + MISSING_PANEL_Y;

        if (mouseX >= warningX && mouseX < warningX + 18 &&
            mouseY >= warningY && mouseY < warningY + 18) {
            guiGraphics.renderTooltip(this.font,
                    List.of(
                            Component.literal("Missing Link Panel"),
                            Component.literal("Add a link panel as the first page")
                    ),
                    java.util.Optional.empty(),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only render inventory label (title is baked into texture)
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // Show page count
        int pageCount = menu.getPageCount();
        String pageText = "Pages: " + pageCount;
        guiGraphics.drawString(this.font, pageText, this.imageWidth - 8 - this.font.width(pageText), 9, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on page list area
        int listLeft = this.leftPos + PAGE_LIST_X;
        int listTop = this.topPos + PAGE_LIST_Y;
        int slotSize = PAGE_SIZE + 2; // Same spacing as rendering
        int listRight = listLeft + PAGES_PER_ROW * slotSize;
        int listBottom = listTop + 2 * slotSize;

        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom) {
            int relX = (int) mouseX - listLeft;
            int relY = (int) mouseY - listTop;

            int col = relX / slotSize;
            int row = relY / slotSize;
            int pageIndex = row * PAGES_PER_ROW + col + scrollOffset;

            List<ItemStack> pages = menu.getBlockEntity().getPageList();
            ItemStack carried = menu.getCarried();

            if (!carried.isEmpty()) {
                // Insert page at this index
                boolean singleItem = (button == 1); // Right-click = single
                MystcraftNetwork.sendToServer(new ContainerActionPacket(
                        ContainerActionPacket.Action.BOOK_BINDER_INSERT_PAGE,
                        menu.containerId,
                        singleItem,
                        "",
                        pageIndex
                ));
                return true;
            } else if (pageIndex < pages.size()) {
                // Remove page from this index
                MystcraftNetwork.sendToServer(new ContainerActionPacket(
                        ContainerActionPacket.Action.BOOK_BINDER_REMOVE_PAGE,
                        menu.containerId,
                        pageIndex
                ));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scroll the page list
        int listLeft = this.leftPos + PAGE_LIST_X;
        int listTop = this.topPos + PAGE_LIST_Y;
        int slotSize = PAGE_SIZE + 2;
        int listRight = listLeft + PAGES_PER_ROW * slotSize;
        int listBottom = listTop + 2 * slotSize;

        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom) {
            List<ItemStack> pages = menu.getBlockEntity().getPageList();
            int maxScroll = Math.max(0, pages.size() - PAGES_PER_ROW * 2);

            if (scrollY > 0) {
                scrollOffset = Math.max(0, scrollOffset - PAGES_PER_ROW);
            } else if (scrollY < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + PAGES_PER_ROW);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Let the title field handle key input first
        if (titleField.isFocused()) {
            if (keyCode == 256) { // Escape
                titleField.setFocused(false);
                return true;
            }
            return titleField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (titleField.isFocused()) {
            return titleField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
