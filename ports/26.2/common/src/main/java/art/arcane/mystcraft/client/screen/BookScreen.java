package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.client.gui.procedural.BookTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPageTextureFactory;
import art.arcane.mystcraft.client.render.BookItemRendererBEWLR;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.network.BlockBookActivatePacket;
import art.arcane.mystcraft.network.EntityBookActivatePacket;
import art.arcane.mystcraft.network.LinkBookActivatePacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Screen for viewing book contents (Agebooks and Linkbooks). Page 0 is the
 * title page with link panel. Click to link. Navigate with left/right clicks.
 * Agebooks have gold borders.
 */
public class BookScreen extends Screen {

  private static final int BOOK_TEX_WIDTH = 327;
  private static final int BOOK_TEX_HEIGHT = 199;

  private static final int BOOK_WIDTH = 327;
  private static final int BOOK_HEIGHT = 199;

  private static final int HAND_BASED = -1;

  private final ItemStack book;
  private final InteractionHand hand;
  private final int entityId;
  private final BlockPos blockPos;
  private final boolean isAgebook;
  private final boolean isLinkbook;
  private final BookTextureFactory.BookKind bookKind;

  private final List<ItemStack> pages;
  private int currentPageIndex = 0;

  private int leftPos;
  private int topPos;
  private float xScale;
  private float yScale;

  /**
   * Constructor for hand-based book viewing.
   */
  public BookScreen(ItemStack book, InteractionHand hand) {
    this(book, hand, HAND_BASED, null);
  }

  /**
   * Constructor for entity-based book viewing.
   *
   * @param entityId the entity ID for LinkbookEntity-based
   */
  public BookScreen(ItemStack book, InteractionHand hand, int entityId) {
    this(book, hand, entityId, null);
  }

  /**
   * Constructor for vanilla Lectern-based book viewing.
   *
   * @param blockPos the position of the lectern holding the book
   */
  public BookScreen(ItemStack book, BlockPos blockPos) {
    this(book, InteractionHand.MAIN_HAND, HAND_BASED, blockPos);
  }

  private BookScreen(ItemStack book, InteractionHand hand, int entityId, BlockPos blockPos) {
    super(getBookTitle(book));
    this.book = book;
    this.hand = hand;
    this.entityId = entityId;
    this.blockPos = blockPos;
    this.isAgebook = book.getItem() instanceof AgebookItem;
    this.isLinkbook = book.getItem() instanceof LinkbookItem;
    this.bookKind = BookTextureFactory.detectKind(book);

    if (isAgebook && book.getItem() instanceof AgebookItem agebookItem) {
      this.pages = agebookItem.getPageList(book);
    } else {
      this.pages = new ArrayList<>();
    }
  }

  private static Component getBookTitle(ItemStack book) {
    if (ItemStackNbt.hasCustomHoverName(book)) {
      return book.getHoverName();
    }
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag != null) {
      String displayName = LinkOptions.getDisplayName(tag);
      if (!"???".equals(displayName)) {
        return Component.literal(displayName);
      }
    }
    return book.getItemName();
  }

  /**
   * Opens a book screen for the given item held in player's hand.
   */
  public static void open(ItemStack book) {
    if (book.getItem() instanceof AgebookItem || book.getItem() instanceof LinkbookItem) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;

      InteractionHand hand = InteractionHand.MAIN_HAND;
      if (ItemStackNbt.isSameItemSameTags(mc.player.getOffhandItem(), book)) {
        hand = InteractionHand.OFF_HAND;
      }

      mc.setScreenAndShow(new BookScreen(book, hand));
    }
  }

  /**
   * Opens a book screen for a book in a LinkbookEntity.
   */
  public static void openForEntity(ItemStack book, int entityId) {
    if (book.getItem() instanceof AgebookItem || book.getItem() instanceof LinkbookItem) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;

      mc.setScreenAndShow(new BookScreen(book, InteractionHand.MAIN_HAND, entityId));
    }
  }

  /**
   * Opens a book screen for a book on a vanilla Lectern.
   */
  public static void openForBlock(ItemStack book, BlockPos blockPos) {
    if (book.getItem() instanceof AgebookItem || book.getItem() instanceof LinkbookItem) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;

      mc.setScreenAndShow(new BookScreen(book, blockPos));
    }
  }

  @Override
  protected void init() {
    super.init();
    this.leftPos = (this.width - BOOK_WIDTH) / 2;
    this.topPos = (this.height - BOOK_HEIGHT) / 2;
    this.xScale = 1.0f;
    this.yScale = 1.0f;

    if (entityId == HAND_BASED) {
      BookItemRendererBEWLR.setHeldBookOpen(true);
    }
  }

  @Override
  public void removed() {
    super.removed();

    BookItemRendererBEWLR.setHeldBookOpen(false);
  }

  @Override
  public void extractBackground(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                float partialTick) {
    super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
  }

  @Override
  public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                 float partialTick) {
    super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

    guiGraphics.pose().pushMatrix();
    guiGraphics.pose().translate(leftPos, topPos);
    guiGraphics.pose().scale(xScale, yScale);

    Identifier coverTex = BookTextureFactory.getCoverTexture(book, bookKind);
    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, coverTex, 0, 7, 152, 0, 34, 192, 256, 256);
    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, coverTex, 34, 7, 49, 0, 103, 192, 256, 256);
    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, coverTex, 137, 7, 45, 0, 4, 192, 256, 256);
    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, coverTex, 141, 7, 0, 0, 186, 192, 256, 256);

    if (bookKind != BookTextureFactory.BookKind.GENERIC) {
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, coverTex, 0, 7, 186, 0, 34, 192, 256, 256);
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, coverTex, 293, 7, 186, 0, 34, 192, 256, 256);
    }

    if (currentPageIndex > 0 && !pages.isEmpty()) {
      drawPageInterior(guiGraphics, 7, 0, 156, 195);
    }

    ItemStack currentPage = getCurrentPage();
    boolean hasLinkPanel = !currentPage.isEmpty() && Page.isLinkPanel(currentPage);

    if (currentPageIndex == 0) {

      drawPageInterior(guiGraphics, 163, 0, 156, 195);

      drawLinkPanel(guiGraphics, 173, 20, 132, 83);

      drawTitlePage(guiGraphics);
    } else if (hasLinkPanel) {

      drawPageInterior(guiGraphics, 163, 0, 156, 195);
      drawLinkPanel(guiGraphics, 173, 20, 132, 83);
    } else if (!currentPage.isEmpty()) {

      drawPageInterior(guiGraphics, 163, 0, 156, 195);
      drawSymbolOnPage(guiGraphics, currentPage, 163, 0, 156, 195);
    } else {

      drawPageInterior(guiGraphics, 163, 0, 156, 195);
    }

    drawPageNumbers(guiGraphics);

    guiGraphics.pose().popMatrix();
  }

  private void drawPageInterior(GuiGraphicsExtractor g, int x, int y, int w, int h) {
    int parchment = 0xFFF1E8CC;
    int shade = 0x40A88E5C;
    int rule = 0x40A88E5C;
    int frame = 0xFF8C6E3A;

    g.fill(x, y, x + w, y + h, parchment);

    g.fill(x, y, x + w, y + 2, shade);
    g.fill(x, y, x + 2, y + h, shade);

    g.fill(x, y + h - 1, x + w, y + h, 0x30FFFFFF);
    g.fill(x + w - 1, y, x + w, y + h, 0x30FFFFFF);

    g.fill(x, y, x + w, y + 1, frame);
    g.fill(x, y + h - 1, x + w, y + h, frame);
    g.fill(x, y, x + 1, y + h, frame);
    g.fill(x + w - 1, y, x + w, y + h, frame);

    for (int ly = y + 12; ly < y + h - 8; ly += 12) {
      g.fill(x + 8, ly, x + w - 8, ly + 1, rule);
    }
  }

  private void drawSymbolOnPage(GuiGraphicsExtractor g, ItemStack page, int x, int y, int w, int h) {
    int margin = 8;
    int square = Math.min(w, h) - margin * 2;
    int sx = x + (w - square) / 2;
    int sy = y + margin;

    Identifier pageTex = SymbolPageTextureFactory.getPageTexture(page);
    g.blit(RenderPipelines.GUI_TEXTURED, pageTex, sx, sy, 0, 0,
        square, square, square, square);
  }

  private void drawLinkPanel(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
    boolean visited = isTargetVisited();
    int topColor = visited ? 0xFF000044 : 0xFF000000;
    int bottomColor = visited ? 0xFF006666 : 0xFF000000;
    if (isPersonalBook() && !hasValidDestination()) {
      topColor = 0xFFF8F8F8;
      bottomColor = 0xFFEFEFEF;
    }

    guiGraphics.fillGradient(x, y, x + width, y + height, topColor, bottomColor);

    if (!hasValidDestination()) {
      Component unwrittenText = Component.translatable("gui.mystcraft.book.unwritten");
      int textWidth = this.font.width(unwrittenText);
      int textX = x + (width - textWidth) / 2;
      int textY = y + (height - 8) / 2;
      int textColor = isPersonalBook() ? 0x444444 : 0x888888;
      guiGraphics.text(this.font, unwrittenText, textX, textY, textColor, false);
    }
  }

  private void drawTitlePage(GuiGraphicsExtractor guiGraphics) {

    String bookTitle = getBookDisplayName();
    guiGraphics.text(this.font, bookTitle, 40, 40, 0x000000, false);

    Collection<String> authors = getAuthors();
    int y = 55;
    if (!authors.isEmpty()) {
      guiGraphics.text(this.font, Component.translatable("gui.mystcraft.book.by"), 40, y, 0x404040, false);
      y += 12;
      for (String author : authors) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(50, y);
        guiGraphics.pose().scale(0.8f, 0.8f);
        guiGraphics.text(this.font, author, 0, 0, 0x000000, false);
        guiGraphics.pose().popMatrix();
        y += 10;
      }
    }

    CompoundTag linkTag = ItemStackNbt.getTag(book);
    if (linkTag != null) {
      y += 10;
      Integer dimId = LinkOptions.getDimensionUID(linkTag);
      BlockPos spawn = LinkOptions.getSpawn(linkTag);

      if (dimId != null) {
        String dimText = isAgebook ? "Age " + dimId : "Dimension " + dimId;
        guiGraphics.text(this.font, dimText, 40, y, 0x404040, false);
        y += 12;
      }

      if (spawn != null) {
        String posText = "Pos: " + spawn.getX() + ", " + spawn.getY() + ", " + spawn.getZ();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(40, y);
        guiGraphics.pose().scale(0.7f, 0.7f);
        guiGraphics.text(this.font, posText, 0, 0, 0x606060, false);
        guiGraphics.pose().popMatrix();
      }
    }
  }

  private void drawPageNumbers(GuiGraphicsExtractor guiGraphics) {
    int pageCount = getPageCount();
    String pageText;
    if (pageCount > 0) {
      pageText = currentPageIndex + "/" + pageCount;
    } else {
      pageText = "- " + currentPageIndex + " -";
    }
    int textWidth = this.font.width(pageText);
    guiGraphics.text(this.font, pageText, 165 - textWidth / 2, 185, 0x000000, false);
  }

  @Override
  public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
    double mouseX = event.x();
    double mouseY = event.y();
    if (event.button() == 0) {

      double localX = (mouseX - leftPos) / xScale;
      double localY = (mouseY - topPos) / yScale;

      boolean hasLinkPanelOnCurrentPage = (currentPageIndex == 0) ||
          (!getCurrentPage().isEmpty() && Page.isLinkPanel(getCurrentPage()));

      if (hasLinkPanelOnCurrentPage &&
          localX >= 173 && localX <= 305 &&
          localY >= 20 && localY <= 103) {
        if (canLink()) {
          performLink();
          return true;
        }
      }

      if (localX >= 0 && localX <= 156 && localY >= 0 && localY <= 195) {
        pageLeft();
        return true;
      }

      if (localX >= 158 && localX <= 312 && localY >= 0 && localY <= 195) {
        pageRight();
        return true;
      }
    }

    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public boolean keyPressed(@NotNull KeyEvent event) {
    int keyCode = event.key();

    if (keyCode == 263 || keyCode == 65) {
      pageLeft();
      return true;
    }

    if (keyCode == 262 || keyCode == 68) {
      pageRight();
      return true;
    }
    return super.keyPressed(event);
  }

  private void pageLeft() {
    if (currentPageIndex > 0) {
      currentPageIndex--;
    }
  }

  private void pageRight() {
    if (currentPageIndex < getPageCount()) {
      currentPageIndex++;
    }
  }

  private ItemStack getCurrentPage() {
    if (pages.isEmpty()) {
      return ItemStack.EMPTY;
    }
    if (currentPageIndex < 0 || currentPageIndex >= pages.size()) {
      return ItemStack.EMPTY;
    }
    return pages.get(currentPageIndex);
  }

  private int getPageCount() {
    return pages.size();
  }

  private String getBookDisplayName() {
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag != null) {
      String name = LinkOptions.getDisplayName(tag);
      if (!"???".equals(name) && !name.isEmpty()) {
        return name;
      }
    }
    if (isPersonalBook()) {
      return "Personal Link Book";
    }
    return isAgebook ? "Descriptive Book" : "Linking Book";
  }

  private Collection<String> getAuthors() {
    if (isAgebook && book.getItem() instanceof AgebookItem agebookItem) {
      return agebookItem.getAuthors(book);
    }
    if (isLinkbook && book.getItem() instanceof LinkbookItem linkbookItem) {
      return linkbookItem.getAuthors(book);
    }
    return List.of();
  }

  private boolean isTargetVisited() {

    return ItemStackNbt.getTag(book) != null && LinkOptions.getDimensionUID(ItemStackNbt.getTag(book)) != null;
  }

  private boolean hasValidDestination() {
    if (ItemStackNbt.getTag(book) == null) {
      return false;
    }
    CompoundTag tag2 = ItemStackNbt.getTag(book);
    Integer dimId = LinkOptions.getDimensionUID(tag2);
    BlockPos spawn = LinkOptions.getSpawn(tag2);

    if (dimId != null && spawn != null) {
      return true;
    }

    if (isAgebook) {
      return AgebookItem.isNewAgebook(book);
    }

    if (isPersonalBook()) {
      return false;
    }

    return false;
  }

  private boolean canLink() {
    return hasValidDestination() || isPersonalBook();
  }

  private boolean isPersonalBook() {
    return book.getItem() instanceof PersonalLinkBookItem;
  }

  private void performLink() {
    if (blockPos != null) {

      MystcraftNetwork.sendToServer(new BlockBookActivatePacket(blockPos));
    } else if (entityId == HAND_BASED) {

      MystcraftNetwork.sendToServer(new LinkBookActivatePacket(hand));
    } else {

      MystcraftNetwork.sendToServer(new EntityBookActivatePacket(entityId));
    }
    this.onClose();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
