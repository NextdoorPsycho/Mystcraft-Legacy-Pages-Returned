package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.client.gui.element.*;
import art.arcane.mystcraft.client.gui.procedural.GuiTheme;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUI;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
 * Screen for the Writing Desk block. Multi-panel layout: left panel (228px,
 * tabs + page surface), right panel (176x166, inventory), button bar (18px),
 * total 409x185.
 */
public class WritingDeskScreen extends AbstractContainerScreen<WritingDeskMenu> {

  private static final int LEFT_SIZE = 228;
  private static final int WINDOW_SIZE_X = 176;
  private static final int WINDOW_SIZE_Y = 166;
  private static final int BUTTONS_SIZE_Y = 18;
  private static final int TAB_WIDTH = 58;
  private static final int GUI_CENTER = LEFT_SIZE + 5;
  private static final int MAIN_TOP = BUTTONS_SIZE_Y + 2;

  private static final int TOTAL_WIDTH = LEFT_SIZE + WINDOW_SIZE_X + 5;
  private static final int TOTAL_HEIGHT = WINDOW_SIZE_Y + BUTTONS_SIZE_Y + 1;

  private static final int INK_COST_PER_SYMBOL = 100;
  private final IAgeSymbol hoveredSymbol = null;

  private int surfaceLeft;
  private int surfaceTop;
  private int rightPanelLeft;
  private int rightPanelTop;

  private MystGuiPanel rootElement;
  private MystGuiSurfaceTabs surfaceTabs;
  private MystGuiPageSurface pageSurface;
  private MystGuiTextField searchField;
  private MystGuiTextField nameField;
  private MystGuiToggleButton sortAzButton;
  private MystGuiToggleButton showAllButton;
  private MystGuiFluidTank inkTank;
  private MystGuiScrollablePages bookPageList;

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

    this.leftPos = (this.width / 2) - (LEFT_SIZE / 2) - (WINDOW_SIZE_X / 2);
    this.topPos = (this.height - TOTAL_HEIGHT) / 2;

    surfaceLeft = this.leftPos;
    surfaceTop = this.topPos + MAIN_TOP;
    rightPanelLeft = this.leftPos + GUI_CENTER;
    rightPanelTop = this.topPos + MAIN_TOP;

    rootElement = new MystGuiPanel(0, 0, TOTAL_WIDTH, TOTAL_HEIGHT);

    surfaceTabs = new MystGuiSurfaceTabs(new TabHandler(), 0, MAIN_TOP, TAB_WIDTH, TOTAL_HEIGHT);
    rootElement.addElement(surfaceTabs);

    int surfaceX = 58;
    int surfaceWidth = LEFT_SIZE - 53;
    pageSurface = new MystGuiPageSurface(new PagesProvider(), surfaceX, MAIN_TOP, surfaceWidth, WINDOW_SIZE_Y);
    rootElement.addElement(pageSurface);

    sortAzButton = new MystGuiToggleButton("AZ", () -> sortAlphabetically, btn -> {
      sortAlphabetically = !sortAlphabetically;
    }, 58, 0, BUTTONS_SIZE_Y, BUTTONS_SIZE_Y);
    sortAzButton.setText("AZ");
    sortAzButton.setTooltip(List.of(Component.literal("Sort Alphabetically")));
    rootElement.addElement(sortAzButton);

    showAllButton = new MystGuiToggleButton("ALL", () -> showAll, btn -> {
      showAll = !showAll;
    }, 58 + BUTTONS_SIZE_Y, 0, BUTTONS_SIZE_Y, BUTTONS_SIZE_Y);
    showAllButton.setText("ALL");
    showAllButton.setTooltip(List.of(Component.literal("Show all Symbols")));
    rootElement.addElement(showAllButton);

    int searchX = 58 + (BUTTONS_SIZE_Y + 2) * 2;
    int searchWidth = LEFT_SIZE - 53 - (BUTTONS_SIZE_Y + 2) * 2;
    searchField = new MystGuiTextField("SearchBox", () -> searchText, text -> {
      searchText = text;
      pageSurface.setSearchText(text);
    }, searchX, 0, searchWidth, BUTTONS_SIZE_Y);
    rootElement.addElement(searchField);

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

    int nameX = GUI_CENTER + 28;
    int nameY = MAIN_TOP + 61;
    int nameWidth = WINDOW_SIZE_X - 48 - 9 - 20;
    nameField = new MystGuiTextField("ItemName",
        () -> getWritingItemName(),
        this::onNameChanged,
        nameX, nameY, nameWidth, 14);
    nameField.setMaxLength(21);
    rootElement.addElement(nameField);

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
  protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    int leftPanelHeight = WINDOW_SIZE_Y;
    ProceduralUI.drawPanel(guiGraphics,
        this.leftPos + TAB_WIDTH,
        this.topPos + MAIN_TOP,
        LEFT_SIZE - TAB_WIDTH,
        leftPanelHeight);

    ProceduralUI.drawPanel(guiGraphics, rightPanelLeft, rightPanelTop, WINDOW_SIZE_X, WINDOW_SIZE_Y);

    renderSlotBackgrounds(guiGraphics);

    renderBookPageListBorder(guiGraphics);

    if (rootElement != null) {
      rootElement.setLeft(this.leftPos);
      rootElement.setTop(this.topPos);
      rootElement.renderBackground(guiGraphics, partialTick, mouseX, mouseY);
    }
  }

  private void renderSlotBackgrounds(GuiGraphics guiGraphics) {

    ProceduralUI.drawSlot(guiGraphics, this.leftPos + 8 + GUI_CENTER - 1, this.topPos + 60 + MAIN_TOP - 1);
    ProceduralUI.drawSlot(guiGraphics, this.leftPos + 8 + GUI_CENTER - 1, this.topPos + 8 + MAIN_TOP - 1);
    ProceduralUI.drawSlot(guiGraphics, this.leftPos + 152 + GUI_CENTER - 1, this.topPos + 8 + MAIN_TOP - 1);
    ProceduralUI.drawSlot(guiGraphics, this.leftPos + 152 + GUI_CENTER - 1, this.topPos + 60 + MAIN_TOP - 1);

    ProceduralUI.drawSlotGrid(guiGraphics,
        this.leftPos + 8 + GUI_CENTER - 1,
        this.topPos + 84 + MAIN_TOP - 1,
        9, 3, 0);

    ProceduralUI.drawSlotGrid(guiGraphics,
        this.leftPos + 8 + GUI_CENTER - 1,
        this.topPos + 142 + MAIN_TOP - 1,
        9, 1, 0);
  }

  private void renderBookPageListBorder(GuiGraphics guiGraphics) {

    int listX = this.leftPos + GUI_CENTER + 27;
    int listY = this.topPos + MAIN_TOP + 6;
    int listWidth = WINDOW_SIZE_X - 47 - 9 - 19;
    int listHeight = 50;

    ProceduralUI.drawInsetBorder(guiGraphics, listX - 1, listY - 1, listWidth + 2, listHeight + 2);
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    this.renderTooltip(guiGraphics, mouseX, mouseY);

    if (rootElement != null) {
      rootElement.renderForeground(guiGraphics, mouseX, mouseY);
    }

    if (rootElement != null) {
      List<Component> tooltip = rootElement.getTooltipInfo(mouseX, mouseY);
      if (tooltip != null && !tooltip.isEmpty()) {
        guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
      }
    }
  }

  private IAgeSymbol getHoveredSymbolFromProvider(int mouseX, int mouseY) {

    int surfaceX = this.leftPos + 58;
    int surfaceY = this.topPos + MAIN_TOP;
    int surfaceWidth = LEFT_SIZE - 53;
    int surfaceHeight = WINDOW_SIZE_Y;

    if (mouseX < surfaceX || mouseX >= surfaceX + surfaceWidth ||
        mouseY < surfaceY || mouseY >= surfaceY + surfaceHeight) {
      return null;
    }

    PagesProvider provider = new PagesProvider();
    List<MystGuiPageSurface.PositionableItem> pages = provider.getPositionedPages();
    if (pages == null) {
      return null;
    }

    for (MystGuiPageSurface.PositionableItem item : pages) {

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

  private void renderSymbolCostIndicator(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    List<Component> costTooltip = new ArrayList<>();

    costTooltip.add(Component.literal(hoveredSymbol.getLocalizedName())
        .withStyle(ChatFormatting.AQUA));

    SymbolCategory category = hoveredSymbol.getCategory();
    if (category != null) {
      costTooltip.add(Component.literal("Category: " + category.name())
          .withStyle(ChatFormatting.GRAY));
    }

    costTooltip.add(Component.empty());

    int inkCost = calculateInkCost(hoveredSymbol);
    int currentInk = menu.getInkAmount();
    ChatFormatting inkColor = currentInk >= inkCost ? ChatFormatting.GREEN : ChatFormatting.RED;
    costTooltip.add(Component.literal("Ink Cost: ")
        .withStyle(ChatFormatting.GRAY)
        .append(Component.literal(inkCost + " mB")
            .withStyle(inkColor)));

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

    Integer rank = hoveredSymbol.getCardRank();
    if (rank != null && rank > 0) {
      String rankStars = "\u2605".repeat(rank);
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

    if (currentInk < inkCost) {
      costTooltip.add(Component.empty());
      costTooltip.add(Component.literal("Not enough ink!")
          .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    guiGraphics.renderTooltip(this.font, costTooltip, java.util.Optional.empty(), mouseX + 16, mouseY);
  }

  private int calculateInkCost(IAgeSymbol symbol) {
    int baseCost = INK_COST_PER_SYMBOL;

    Integer rank = symbol.getCardRank();
    if (rank != null && rank > 0) {
      baseCost *= rank;
    }

    float instability = symbol.getInstabilityCost();
    if (instability > 10) {
      baseCost = (int) (baseCost * 1.5);
    }

    return baseCost;
  }

  @Override
  protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {

    int textColor = GuiTheme.color("text_primary") & 0x00FFFFFF;

    guiGraphics.drawString(this.font, this.title, GUI_CENTER + 8, MAIN_TOP - 12, textColor, false);
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

  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    return handleMouseScroll(mouseX, mouseY, delta);
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    return handleMouseScroll(mouseX, mouseY, scrollY);
  }

  private boolean handleMouseScroll(double mouseX, double mouseY, double delta) {
    if (rootElement != null && rootElement.mouseScrolled(mouseX, mouseY, delta)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (rootElement != null && rootElement.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }

    if (keyCode == 256) {
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

      ItemStack carried = menu.getCarried();
      if (!carried.isEmpty()) {

        return;
      }

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

      if (sortAlphabetically) {
        pages = new ArrayList<>(pages);
        pages.sort((a, b) -> {
          String nameA = getPageName(a);
          String nameB = getPageName(b);
          return nameA.compareToIgnoreCase(nameB);
        });
      }

      float x = 6;
      float y = 6;
      int slotId = 0;
      int surfaceWidth = LEFT_SIZE - 53 - 16;
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

      ItemStack writing = menu.slots.get(WritingDeskMenu.SLOT_WRITING).getItem();
      if (writing.isEmpty()) {
        return List.of();
      }

      if (writing.getItem() instanceof art.arcane.mystcraft.item.AgebookItem agebook) {
        return agebook.getPageList(writing);
      }

      if (writing.getItem() instanceof art.arcane.mystcraft.item.LinkbookItem linkbook) {
        return linkbook.getPageList(Minecraft.getInstance().player, writing);
      }
      return List.of();
    }

    @Override
    public void onItemPlace(int index, boolean single) {

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
