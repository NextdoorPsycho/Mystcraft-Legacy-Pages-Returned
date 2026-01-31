package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.client.gui.element.*;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for the Writing Desk block (1.19.2 version). Multi-panel layout:
 * left panel (228px, tabs + page surface), right panel (176x166, inventory),
 * button bar (18px), total 409x185.
 */
public class WritingDeskScreen extends AbstractContainerScreen<WritingDeskMenu> {

  private static final ResourceLocation TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "gui/writingdesk.png");

  // Layout constants
  private static final int LEFT_SIZE = 228;
  private static final int WINDOW_SIZE_X = 176;
  private static final int WINDOW_SIZE_Y = 166;
  private static final int BUTTONS_SIZE_Y = 18;
  private static final int TAB_WIDTH = 58;
  private static final int GUI_CENTER = LEFT_SIZE + 5; // 233
  private static final int MAIN_TOP = BUTTONS_SIZE_Y + 2; // 20

  // Component sizes
  private static final int TOTAL_WIDTH = LEFT_SIZE + WINDOW_SIZE_X + 5; // 409
  private static final int TOTAL_HEIGHT = WINDOW_SIZE_Y + BUTTONS_SIZE_Y + 1; // 185
  // Symbol cost display
  private static final int INK_COST_PER_SYMBOL = 100; // Base ink cost
  private final IAgeSymbol hoveredSymbol = null;
  // Panel positions
  private int surfaceLeft;
  private int surfaceTop;
  private int rightPanelLeft;
  private int rightPanelTop;
  // GUI elements
  private MystGuiPanel rootElement;
  private MystGuiSurfaceTabs surfaceTabs;
  private MystGuiPageSurface pageSurface;
  private MystGuiTextField searchField;
  private MystGuiTextField nameField;
  private MystGuiToggleButton sortAzButton;
  private MystGuiToggleButton showAllButton;
  private MystGuiFluidTank inkTank;
  private MystGuiScrollablePages bookPageList;
  // State
  private int activeTabSlot = 0;
  private int topTabSlot = 0;
  private boolean sortAlphabetically = false;
  private boolean showAll = false;
  private String searchText = "";

  public WritingDeskScreen(WritingDeskMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = TOTAL_WIDTH;
    this.imageHeight = TOTAL_HEIGHT;
  }

  @Override
  protected void init() {
    // Center the combined GUI
    this.leftPos = (this.width / 2) - (LEFT_SIZE / 2) - (WINDOW_SIZE_X / 2);
    this.topPos = (this.height - TOTAL_HEIGHT) / 2;

    // Calculate panel positions
    surfaceLeft = this.leftPos;
    surfaceTop = this.topPos + MAIN_TOP;
    rightPanelLeft = this.leftPos + GUI_CENTER;
    rightPanelTop = this.topPos + MAIN_TOP;

    // Create root element
    rootElement = new MystGuiPanel(0, 0, TOTAL_WIDTH, TOTAL_HEIGHT);

    // Surface tabs (left side)
    surfaceTabs = new MystGuiSurfaceTabs(new TabHandler(), 0, MAIN_TOP, TAB_WIDTH, TOTAL_HEIGHT);
    rootElement.addElement(surfaceTabs);

    // Page surface (center)
    int surfaceX = 58;
    int surfaceWidth = LEFT_SIZE - 53; // 175
    pageSurface = new MystGuiPageSurface(new PagesProvider(), surfaceX, MAIN_TOP, surfaceWidth, WINDOW_SIZE_Y);
    rootElement.addElement(pageSurface);

    // Sort buttons
    sortAzButton = new MystGuiToggleButton("AZ", () -> sortAlphabetically, btn -> {
      sortAlphabetically = !sortAlphabetically;
    }, 58, 0, BUTTONS_SIZE_Y, BUTTONS_SIZE_Y);
    sortAzButton.setText("AZ");
    sortAzButton.setTooltip(List.of(Component.literal("Sort Alphabetically")));
    rootElement.addElement(sortAzButton);

    // Show-all toggle at (76, 0, 18, 18)
    showAllButton = new MystGuiToggleButton("ALL", () -> showAll, btn -> {
      showAll = !showAll;
    }, 58 + BUTTONS_SIZE_Y, 0, BUTTONS_SIZE_Y, BUTTONS_SIZE_Y);
    showAllButton.setText("ALL");
    showAllButton.setTooltip(List.of(Component.literal("Show all Symbols")));
    rootElement.addElement(showAllButton);

    // Search field at (98, 0, 135, 18)
    int searchX = 58 + (BUTTONS_SIZE_Y + 2) * 2;
    int searchWidth = LEFT_SIZE - 53 - (BUTTONS_SIZE_Y + 2) * 2;
    searchField = new MystGuiTextField("SearchBox", () -> searchText, text -> {
      searchText = text;
      pageSurface.setSearchText(text);
    }, searchX, 0, searchWidth, BUTTONS_SIZE_Y);
    rootElement.addElement(searchField);

    // Ink tank display at (365, 27, 16, 70)
    int tankX = GUI_CENTER + WINDOW_SIZE_X - 44;
    int tankY = MAIN_TOP + 7;
    inkTank = new MystGuiFluidTank(
        () -> menu.getInkAmount(),
        () -> menu.getInkCapacity(),
        0xFF1A1A2E,
        "Black Ink",
        tankX, tankY, 16, 70
    );
    rootElement.addElement(inkTank);

    // Name field at (261, 81, 99, 14)
    int nameX = GUI_CENTER + 28;
    int nameY = MAIN_TOP + 61;
    int nameWidth = WINDOW_SIZE_X - 48 - 9 - 20;
    nameField = new MystGuiTextField("ItemName",
        () -> getWritingItemName(),
        this::onNameChanged,
        nameX, nameY, nameWidth, 14);
    nameField.setMaxLength(21);
    rootElement.addElement(nameField);

    // Book page list at (260, 26, 101, 50)
    int listX = GUI_CENTER + 27;
    int listY = MAIN_TOP + 6;
    int listWidth = WINDOW_SIZE_X - 47 - 9 - 19;
    int listHeight = 50;
    bookPageList = new MystGuiScrollablePages(new BookPageListHandler(), listX, listY, listWidth, listHeight);
    rootElement.addElement(bookPageList);
  }

  private String getWritingItemName() {
    ItemStack writing = menu.getBlockEntity().getMainInventory().getItem(WritingDeskBlockEntity.SLOT_WRITING);
    if (writing.isEmpty()) return "";
    return writing.getHoverName().getString();
  }

  private void onNameChanged(String text) {
    MystcraftNetwork.sendToServer(new ContainerActionPacket(
        ContainerActionPacket.Action.WRITING_DESK_SET_TITLE,
        menu.containerId,
        false,
        text
    ));
  }

  @Override
  public void containerTick() {
    super.containerTick();
    if (rootElement != null) {
      rootElement.tick();
    }
  }

  @Override
  protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);

    // Draw right panel background (main inventory texture) at (guiLeft + guiCenter, guiTop + mainTop)
    blit(poseStack, rightPanelLeft, rightPanelTop, 0, 0, WINDOW_SIZE_X, WINDOW_SIZE_Y);

    // Draw slot backgrounds for better visibility
    renderSlotBackgrounds(poseStack);

    // Draw border around book page list area
    renderBookPageListBorder(poseStack);

    // Render GUI elements
    if (rootElement != null) {
      rootElement.setLeft(this.leftPos);
      rootElement.setTop(this.topPos);
      rootElement.renderBackground(poseStack, partialTick, mouseX, mouseY);
    }
  }

  /**
   * Draws 3D slot backgrounds for the main inventory slots.
   */
  private void renderSlotBackgrounds(PoseStack poseStack) {
    // Slot background colors (Minecraft style)
    int borderDark = 0xFF373737;
    int borderLight = 0xFFFFFFFF;
    int slotBg = 0xFF8B8B8B;

    // Writing slot at (8+xShift, 60+yShift) relative to rightPanelLeft/rightPanelTop
    drawSlotBackground(poseStack, this.leftPos + 8 + GUI_CENTER, this.topPos + 60 + MAIN_TOP, borderDark, borderLight, slotBg);

    // Paper slot at (8+xShift, 8+yShift)
    drawSlotBackground(poseStack, this.leftPos + 8 + GUI_CENTER, this.topPos + 8 + MAIN_TOP, borderDark, borderLight, slotBg);

    // Container in slot at (152+xShift, 8+yShift)
    drawSlotBackground(poseStack, this.leftPos + 152 + GUI_CENTER, this.topPos + 8 + MAIN_TOP, borderDark, borderLight, slotBg);

    // Container out slot at (152+xShift, 60+yShift)
    drawSlotBackground(poseStack, this.leftPos + 152 + GUI_CENTER, this.topPos + 60 + MAIN_TOP, borderDark, borderLight, slotBg);
  }

  /**
   * Draws a single 3D slot background at the given position.
   */
  private void drawSlotBackground(PoseStack poseStack, int x, int y, int borderDark, int borderLight, int slotBg) {
    int size = 18; // Standard slot size
    // Top edge (dark)
    fill(poseStack, x - 1, y - 1, x + size - 1, y, borderDark);
    // Left edge (dark)
    fill(poseStack, x - 1, y - 1, x, y + size - 1, borderDark);
    // Bottom edge (light)
    fill(poseStack, x - 1, y + size - 2, x + size - 1, y + size - 1, borderLight);
    // Right edge (light)
    fill(poseStack, x + size - 2, y - 1, x + size - 1, y + size - 1, borderLight);
    // Inner background
    fill(poseStack, x, y, x + size - 2, y + size - 2, slotBg);
  }

  /**
   * Draws a border around the book page list area for better visibility.
   */
  private void renderBookPageListBorder(PoseStack poseStack) {
    // Book page list is at (guiCenter + 27, mainTop + 6) with size (101, 50)
    int listX = this.leftPos + GUI_CENTER + 27;
    int listY = this.topPos + MAIN_TOP + 6;
    int listWidth = WINDOW_SIZE_X - 47 - 9 - 19; // 101
    int listHeight = 50;

    int borderDark = 0xFF373737;
    int borderLight = 0xFFAAAAAA;

    // Draw inset border
    // Top edge (dark)
    fill(poseStack, listX - 1, listY - 1, listX + listWidth + 1, listY, borderDark);
    // Left edge (dark)
    fill(poseStack, listX - 1, listY - 1, listX, listY + listHeight + 1, borderDark);
    // Bottom edge (light)
    fill(poseStack, listX - 1, listY + listHeight, listX + listWidth + 1, listY + listHeight + 1, borderLight);
    // Right edge (light)
    fill(poseStack, listX + listWidth, listY - 1, listX + listWidth + 1, listY + listHeight + 1, borderLight);
  }

  @Override
  public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    // 1.19.2 API: renderBackground takes PoseStack
    this.renderBackground(poseStack);
    super.render(poseStack, mouseX, mouseY, partialTick);

    // Render foreground elements
    if (rootElement != null) {
      rootElement.renderForeground(poseStack, mouseX, mouseY);
    }

    this.renderTooltip(poseStack, mouseX, mouseY);

    // Render element tooltips
    if (rootElement != null) {
      List<Component> tooltip = rootElement.getTooltipInfo(mouseX, mouseY);
      if (tooltip != null && !tooltip.isEmpty()) {
        renderTooltip(poseStack, tooltip, java.util.Optional.empty(), mouseX, mouseY);
      }
    }

    // Symbol cost indicator is shown via page surface tooltips
    // The MystGuiPageSurface already handles hover detection
  }

  /**
   * Gets the symbol being hovered over from the PagesProvider.
   * Note: This uses the internal provider from our local PagesProvider implementation.
   */
  private IAgeSymbol getHoveredSymbolFromProvider(int mouseX, int mouseY) {
    // Check if mouse is over page surface area
    int surfaceX = this.leftPos + 58;
    int surfaceY = this.topPos + MAIN_TOP;
    int surfaceWidth = LEFT_SIZE - 53;
    int surfaceHeight = WINDOW_SIZE_Y;

    if (mouseX < surfaceX || mouseX >= surfaceX + surfaceWidth ||
        mouseY < surfaceY || mouseY >= surfaceY + surfaceHeight) {
      return null;
    }

    // Use our own provider to get pages
    PagesProvider provider = new PagesProvider();
    List<MystGuiPageSurface.PositionableItem> pages = provider.getPositionedPages();
    if (pages == null) {
      return null;
    }

    for (MystGuiPageSurface.PositionableItem item : pages) {
      // Calculate item bounds
      float itemX = surfaceX + item.x;
      float itemY = surfaceY + item.y;
      float itemWidth = MystGuiPageSurface.PAGE_WIDTH;
      float itemHeight = MystGuiPageSurface.PAGE_HEIGHT;

      if (mouseX >= itemX && mouseX < itemX + itemWidth &&
          mouseY >= itemY && mouseY < itemY + itemHeight) {
        if (!item.itemstack.isEmpty()) {
          ResourceLocation symbolId = Page.getSymbol(item.itemstack);
          if (symbolId != null) {
            return SymbolRegistry.get(symbolId);
          }
        }
      }
    }

    return null;
  }

  /**
   * Renders the symbol cost indicator showing ink cost and instability.
   */
  private void renderSymbolCostIndicator(PoseStack poseStack, int mouseX, int mouseY) {
    List<Component> costTooltip = new ArrayList<>();

    // Symbol name
    costTooltip.add(Component.literal(hoveredSymbol.getLocalizedName())
        .withStyle(ChatFormatting.AQUA));

    // Category
    SymbolCategory category = hoveredSymbol.getCategory();
    if (category != null) {
      costTooltip.add(Component.literal("Category: " + category.name())
          .withStyle(ChatFormatting.GRAY));
    }

    costTooltip.add(Component.empty());

    // Ink cost
    int inkCost = calculateInkCost(hoveredSymbol);
    int currentInk = menu.getInkAmount();
    ChatFormatting inkColor = currentInk >= inkCost ? ChatFormatting.GREEN : ChatFormatting.RED;
    costTooltip.add(Component.literal("Ink Cost: ")
        .withStyle(ChatFormatting.GRAY)
        .append(Component.literal(inkCost + " mB")
            .withStyle(inkColor)));

    // Instability cost
    float instability = hoveredSymbol.getInstabilityCost();
    ChatFormatting instabilityColor;
    if (instability < 0) {
      instabilityColor = ChatFormatting.GREEN;
    } else if (instability > 10) {
      instabilityColor = ChatFormatting.RED;
    } else if (instability > 0) {
      instabilityColor = ChatFormatting.YELLOW;
    } else {
      instabilityColor = ChatFormatting.GRAY;
    }
    costTooltip.add(Component.literal("Instability: ")
        .withStyle(ChatFormatting.GRAY)
        .append(Component.literal(String.format("%+.1f", instability))
            .withStyle(instabilityColor)));

    // Card rank
    Integer rank = hoveredSymbol.getCardRank();
    if (rank != null && rank > 0) {
      String rankStars = "\u2605".repeat(rank); // Star character
      ChatFormatting rankColor = switch (rank) {
        case 1 -> ChatFormatting.WHITE;
        case 2 -> ChatFormatting.GREEN;
        case 3 -> ChatFormatting.BLUE;
        case 4 -> ChatFormatting.GOLD;
        default -> ChatFormatting.LIGHT_PURPLE;
      };
      costTooltip.add(Component.literal("Rank: ")
          .withStyle(ChatFormatting.GRAY)
          .append(Component.literal(rankStars)
              .withStyle(rankColor)));
    }

    // Warning if not enough ink
    if (currentInk < inkCost) {
      costTooltip.add(Component.empty());
      costTooltip.add(Component.literal("Not enough ink!")
          .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    // Render offset from cursor to not obscure the page
    renderTooltip(poseStack, costTooltip, java.util.Optional.empty(), mouseX + 16, mouseY);
  }

  /**
   * Calculates the ink cost for writing a symbol.
   */
  private int calculateInkCost(IAgeSymbol symbol) {
    int baseCost = INK_COST_PER_SYMBOL;

    // Higher rank symbols cost more
    Integer rank = symbol.getCardRank();
    if (rank != null && rank > 0) {
      baseCost *= rank;
    }

    // High instability symbols cost more
    float instability = symbol.getInstabilityCost();
    if (instability > 10) {
      baseCost = (int) (baseCost * 1.5);
    }

    return baseCost;
  }

  @Override
  protected void renderLabels(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
    // Note: renderLabels is called with an offset to the GUI origin (leftPos, topPos),
    // so positions here are relative to (0, 0) of the GUI, not screen coordinates.

    // Title is baked into the texture; no labels rendered
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (rootElement != null && rootElement.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (rootElement != null && rootElement.mouseReleased(mouseX, mouseY, button)) {
      return true;
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (rootElement != null && rootElement.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  // 1.19.2 API: mouseScrolled has 3 parameters (mouseX, mouseY, delta)
  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (rootElement != null && rootElement.mouseScrolled(mouseX, mouseY, delta)) {
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (rootElement != null && rootElement.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }
    // Allow closing with inventory key even when text field focused
    if (keyCode == 256) { // Escape
      this.onClose();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char codePoint, int modifiers) {
    if (rootElement != null && rootElement.charTyped(codePoint, modifiers)) {
      return true;
    }
    return super.charTyped(codePoint, modifiers);
  }

  // Handler implementations

  private class TabHandler implements MystGuiSurfaceTabs.TabHandler {
    @Override
    public ItemStack getItemInSlot(int slot) {
      if (slot < 0 || slot >= WritingDeskBlockEntity.TAB_SLOT_COUNT) {
        return ItemStack.EMPTY;
      }
      return menu.getBlockEntity().getTabInventory().getItem(slot);
    }

    @Override
    public void setTopTabSlot(int topSlot) {
      topTabSlot = Math.max(0, Math.min(topSlot, getMaxTabs() - 4));
    }

    @Override
    public void onTabClick(int button, int slot) {
      // If holding item, try to place in tab slot
      ItemStack carried = menu.getCarried();
      if (!carried.isEmpty()) {
        // TODO: Send packet to add to tab
        return;
      }

      // Otherwise, set as active tab
      if (activeTabSlot != slot) {
        activeTabSlot = slot;
        MystcraftNetwork.sendToServer(new ContainerActionPacket(
            ContainerActionPacket.Action.WRITING_DESK_SET_ACTIVE_TAB,
            menu.containerId,
            slot
        ));
      }
    }

    @Override
    public int getTopSlot() {
      return topTabSlot;
    }

    @Override
    public int getActiveTab() {
      return activeTabSlot;
    }

    @Override
    public int getMaxTabs() {
      return WritingDeskBlockEntity.TAB_SLOT_COUNT;
    }
  }

  private class PagesProvider implements MystGuiPageSurface.PagesProvider {
    @Override
    public List<MystGuiPageSurface.PositionableItem> getPositionedPages() {
      List<MystGuiPageSurface.PositionableItem> result = new ArrayList<>();

      // Get pages from active tab
      ItemStack tabItem = menu.getBlockEntity().getTabInventory().getItem(activeTabSlot);
      if (tabItem.isEmpty()) {
        return result;
      }

      List<ItemStack> pages = null;
      if (tabItem.getItem() instanceof FolderItem) {
        pages = FolderItem.getPages(tabItem);
      } else if (tabItem.getItem() instanceof PortfolioItem) {
        pages = PortfolioItem.getPages(tabItem);
      }

      if (pages == null || pages.isEmpty()) {
        return result;
      }

      // Sort if needed
      if (sortAlphabetically) {
        pages = new ArrayList<>(pages);
        pages.sort((a, b) -> {
          String nameA = getPageName(a);
          String nameB = getPageName(b);
          return nameA.compareToIgnoreCase(nameB);
        });
      }

      // Position pages in a grid
      // Surface width is LEFT_SIZE - 53 = 175
      float x = 6;
      float y = 6;
      int slotId = 0;
      int surfaceWidth = LEFT_SIZE - 53 - 16; // Account for scrollbar
      for (ItemStack page : pages) {
        result.add(new MystGuiPageSurface.PositionableItem(slotId++, page, x, y, 1));
        x += MystGuiPageSurface.PAGE_WIDTH + 4;
        if (x + MystGuiPageSurface.PAGE_WIDTH > surfaceWidth) {
          x = 6;
          y += MystGuiPageSurface.PAGE_HEIGHT + 4;
        }
      }

      return result;
    }

    private String getPageName(ItemStack page) {
      ResourceLocation symbol = Page.getSymbol(page);
      if (symbol != null) {
        IAgeSymbol s = SymbolRegistry.get(symbol);
        if (s != null) {
          return s.getLocalizedName();
        }
        return symbol.getPath();
      }
      return page.getHoverName().getString();
    }

    @Override
    public void place(int index, boolean single) {
      MystcraftNetwork.sendToServer(new ContainerActionPacket(
          ContainerActionPacket.Action.WRITING_DESK_ADD_TO_SURFACE,
          menu.containerId,
          single,
          "",
          index
      ));
    }

    @Override
    public void pickup(MystGuiPageSurface.PositionableItem item) {
      MystcraftNetwork.sendToServer(new ContainerActionPacket(
          ContainerActionPacket.Action.WRITING_DESK_REMOVE_FROM_SURFACE,
          menu.containerId,
          item.slotId
      ));
    }

    @Override
    public void copy(MystGuiPageSurface.PositionableItem item) {
      ResourceLocation symbol = Page.getSymbol(item.itemstack);
      if (symbol != null) {
        MystcraftNetwork.sendToServer(new ContainerActionPacket(
            ContainerActionPacket.Action.WRITING_DESK_WRITE_SYMBOL,
            menu.containerId,
            false,
            symbol.toString()
        ));
      }
    }
  }

  private class BookPageListHandler implements MystGuiScrollablePages.PageListHandler {
    @Override
    public List<ItemStack> getPageList() {
      // Get pages from writing slot using synchronized menu slot (includes NBT data)
      ItemStack writing = menu.slots.get(WritingDeskMenu.SLOT_WRITING).getItem();
      if (writing.isEmpty()) {
        return List.of();
      }
      // Get pages from agebook
      if (writing.getItem() instanceof art.arcane.mystcraft.item.AgebookItem agebook) {
        return agebook.getPageList(writing);
      }
      // Get pages from linkbook (requires player)
      if (writing.getItem() instanceof art.arcane.mystcraft.item.LinkbookItem linkbook) {
        return linkbook.getPageList(Minecraft.getInstance().player, writing);
      }
      return List.of();
    }

    @Override
    public void onItemPlace(int index, boolean single) {
      // Insert page into book
      MystcraftNetwork.sendToServer(new ContainerActionPacket(
          ContainerActionPacket.Action.WRITING_DESK_ADD_TO_BOOK,
          menu.containerId,
          single,
          "",
          index
      ));
    }

    @Override
    public void onItemRemove(int index) {
      // Remove page from book
      MystcraftNetwork.sendToServer(new ContainerActionPacket(
          ContainerActionPacket.Action.WRITING_DESK_REMOVE_FROM_BOOK,
          menu.containerId,
          index
      ));
    }

    @Override
    public ItemStack getHeldItem() {
      return menu.getCarried();
    }
  }
}
