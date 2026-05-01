package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.client.gui.procedural.GuiTheme;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Screen for the Book Binder block. Includes text field for book name and
 * scrollable page list.
 */
public class BookBinderScreen extends AbstractContainerScreen<BookBinderMenu> {

  private static final int PAGE_LIST_X = 7;
  private static final int PAGE_LIST_Y = 45;
  private static final int PAGE_LIST_WIDTH = 162;
  private static final int PAGE_LIST_HEIGHT = 40;
  private static final int PAGE_SIZE = 16;
  private static final int PAGE_SLOT_SIZE = PAGE_SIZE + 2;
  private static final int PAGES_PER_ROW = PAGE_LIST_WIDTH / PAGE_SLOT_SIZE;

  private static final int TEXT_FIELD_X = 7;
  private static final int TEXT_FIELD_Y = 9;
  private static final int TEXT_FIELD_WIDTH = 116;
  private static final int TEXT_FIELD_HEIGHT = 14;

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

    this.inventoryLabelY = this.imageHeight - 94;

    titleField = new EditBox(this.font, this.leftPos + TEXT_FIELD_X, this.topPos + TEXT_FIELD_Y,
        TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT, Component.literal("Book Title"));
    titleField.setMaxLength(21);
    titleField.setBordered(true);
    titleField.setVisible(true);
    titleField.setTextColor(0xA0A0A0);

    BookBinderBlockEntity be = menu.getBlockEntity();
    titleField.setValue(be.getPendingTitle());

    titleField.setResponder(this::onTitleChanged);

    addRenderableWidget(titleField);
  }

  private void onTitleChanged(String text) {

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

    long time = System.currentTimeMillis();
    warningAlpha = (time % 4000) / 2000.0f;
    if (warningAlpha > 1) {
      warningAlpha = 2 - warningAlpha;
    }
    warningAlpha += 0.3f;
    warningAlpha = Math.min(1.0f, warningAlpha);

    if (titleField.getValue().isEmpty()) {
      titleField.setTextColor(0xFF0000);
    } else {
      titleField.setTextColor(0xA0A0A0);
    }
  }

  @Override
  protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    ProceduralUI.drawPanel(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

    ProceduralUI.drawInsetBorder(guiGraphics,
        this.leftPos + TEXT_FIELD_X - 2,
        this.topPos + TEXT_FIELD_Y - 2,
        TEXT_FIELD_WIDTH + 4,
        TEXT_FIELD_HEIGHT + 4);

    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + this.imageHeight - 82, 9, 3, 0);
    ProceduralUI.drawSlotGrid(guiGraphics, this.leftPos + 8, this.topPos + this.imageHeight - 24, 9, 1, 0);

    renderPageList(guiGraphics, mouseX, mouseY);

    renderMissingPanelWarning(guiGraphics, mouseX, mouseY);
  }

  private void renderPageList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    List<ItemStack> pages = menu.getBlockEntity().getPageList();

    int listLeft = this.leftPos + PAGE_LIST_X;
    int listTop = this.topPos + PAGE_LIST_Y;

    int maxVisibleSlots = PAGES_PER_ROW * 2;

    for (int i = 0; i < maxVisibleSlots; i++) {
      int col = i % PAGES_PER_ROW;
      int row = i / PAGES_PER_ROW;
      int x = listLeft + col * PAGE_SLOT_SIZE;
      int y = listTop + row * PAGE_SLOT_SIZE;
      ProceduralUI.drawSlot(guiGraphics, x, y, PAGE_SLOT_SIZE, PAGE_SLOT_SIZE);
    }

    for (int i = scrollOffset; i < pages.size() && i < scrollOffset + maxVisibleSlots; i++) {
      int displayIndex = i - scrollOffset;
      int col = displayIndex % PAGES_PER_ROW;
      int row = displayIndex / PAGES_PER_ROW;

      int x = listLeft + col * (PAGE_SIZE + 2) + 1;
      int y = listTop + row * (PAGE_SIZE + 2) + 1;

      ItemStack page = pages.get(i);
      if (!page.isEmpty()) {

        guiGraphics.renderItem(page, x, y);

        if (i == 0 && !Page.isLinkPanel(page)) {
          int warn = GuiTheme.color("text_warning");
          int bx = x - 1;
          int by = y - 1;
          guiGraphics.fill(bx - 1, by - 1, bx + PAGE_SIZE + 2, by, warn);
          guiGraphics.fill(bx - 1, by + PAGE_SIZE + 1, bx + PAGE_SIZE + 2, by + PAGE_SIZE + 2, warn);
          guiGraphics.fill(bx - 1, by, bx, by + PAGE_SIZE + 1, warn);
          guiGraphics.fill(bx + PAGE_SIZE + 1, by, bx + PAGE_SIZE + 2, by + PAGE_SIZE + 1, warn);
        }
      }
    }
  }

  private void renderMissingPanelWarning(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    List<ItemStack> pages = menu.getBlockEntity().getPageList();

    boolean showWarning = pages.isEmpty() || !Page.isLinkPanel(pages.get(0));
    if (!showWarning) {
      return;
    }

    int warningX = this.leftPos + MISSING_PANEL_X;
    int warningY = this.topPos + MISSING_PANEL_Y;

    RenderSystem.enableBlend();
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, warningAlpha);
    ProceduralUI.drawWarningGlyph(guiGraphics, warningX, warningY, 18, 18);
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    RenderSystem.disableBlend();
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);

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
    int textColor = GuiTheme.color("text_primary") & 0x00FFFFFF;
    guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, textColor, false);

    int pageCount = menu.getPageCount();
    String pageText = "Pages: " + pageCount;
    guiGraphics.drawString(this.font, pageText, this.imageWidth - 8 - this.font.width(pageText), 12, textColor, false);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {

    int listLeft = this.leftPos + PAGE_LIST_X;
    int listTop = this.topPos + PAGE_LIST_Y;
    int slotSize = PAGE_SIZE + 2;
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

        boolean singleItem = (button == 1);
        MystcraftNetwork.sendToServer(new ContainerActionPacket(
            ContainerActionPacket.Action.BOOK_BINDER_INSERT_PAGE,
            menu.containerId,
            singleItem,
            "",
            pageIndex
        ));
        return true;
      } else if (pageIndex < pages.size()) {

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

  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    return handleMouseScroll(mouseX, mouseY, delta);
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    return handleMouseScroll(mouseX, mouseY, scrollY);
  }

  private boolean handleMouseScroll(double mouseX, double mouseY, double delta) {

    int listLeft = this.leftPos + PAGE_LIST_X;
    int listTop = this.topPos + PAGE_LIST_Y;
    int slotSize = PAGE_SIZE + 2;
    int listRight = listLeft + PAGES_PER_ROW * slotSize;
    int listBottom = listTop + 2 * slotSize;

    if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom) {
      List<ItemStack> pages = menu.getBlockEntity().getPageList();
      int maxScroll = Math.max(0, pages.size() - PAGES_PER_ROW * 2);

      if (delta > 0) {
        scrollOffset = Math.max(0, scrollOffset - PAGES_PER_ROW);
      } else if (delta < 0) {
        scrollOffset = Math.min(maxScroll, scrollOffset + PAGES_PER_ROW);
      }
      return true;
    }

    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

    if (titleField.isFocused()) {
      if (keyCode == 256) {
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
