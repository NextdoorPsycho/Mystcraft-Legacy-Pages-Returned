package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.network.EntityBookActivatePacket;
import art.arcane.mystcraft.network.LinkBookActivatePacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Screen for viewing book contents (Agebooks and Linkbooks).
 * Displays the book in the legacy Mystcraft style with proper textures.
 *
 * Legacy behavior:
 * - Page 0 is the title page with link panel
 * - Click link panel to trigger linking
 * - Click left/right sides to navigate pages
 * - Gold borders for Agebooks
 */
public class BookScreen extends Screen {

    // Textures from legacy assets
    private static final ResourceLocation BOOK_COVER =
            new ResourceLocation("mystcraft", "gui/bookui_cover.png");
    private static final ResourceLocation BOOK_PAGE_LEFT =
            new ResourceLocation("mystcraft", "gui/bookui_pagel.png");
    private static final ResourceLocation BOOK_PAGE_RIGHT =
            new ResourceLocation("mystcraft", "gui/bookui_pager.png");
    private static final ResourceLocation BOOK_PAGE_RIGHT_SOLID =
            new ResourceLocation("mystcraft", "gui/bookui_rpage_full.png");

    // Book dimensions from legacy (327x199)
    private static final int BOOK_TEX_WIDTH = 327;
    private static final int BOOK_TEX_HEIGHT = 199;

    // Actual rendered size (may be scaled)
    private static final int BOOK_WIDTH = 327;
    private static final int BOOK_HEIGHT = 199;

    /** Entity ID of -1 means the book is in player's hand, otherwise it's a LinkbookEntity */
    private static final int HAND_BASED = -1;

    private final ItemStack book;
    private final InteractionHand hand;
    private final int entityId;
    private final boolean isAgebook;
    private final boolean isLinkbook;

    // Pages for Agebooks
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
        this(book, hand, HAND_BASED);
    }

    /**
     * Constructor for entity-based or hand-based book viewing.
     * @param entityId -1 for hand-based, or the entity ID for LinkbookEntity-based
     */
    public BookScreen(ItemStack book, InteractionHand hand, int entityId) {
        super(getBookTitle(book));
        this.book = book;
        this.hand = hand;
        this.entityId = entityId;
        this.isAgebook = book.getItem() instanceof AgebookItem;
        this.isLinkbook = book.getItem() instanceof LinkbookItem;

        // Get pages for Agebooks
        if (isAgebook && book.getItem() instanceof AgebookItem agebookItem) {
            this.pages = agebookItem.getPageList(book);
        } else {
            this.pages = new ArrayList<>();
        }
    }

    private static Component getBookTitle(ItemStack book) {
        if (book.hasCustomHoverName()) {
            return book.getHoverName();
        }
        if (book.getTag() != null) {
            String displayName = LinkOptions.getDisplayName(book.getTag());
            if (!"???".equals(displayName)) {
                return Component.literal(displayName);
            }
        }
        return book.getItem().getDescription();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - BOOK_WIDTH) / 2;
        this.topPos = (this.height - BOOK_HEIGHT) / 2;
        this.xScale = 1.0f;
        this.yScale = 1.0f;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Don't render dark background overlay - book has its own backing
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos, topPos, 0);
        guiGraphics.pose().scale(xScale, yScale, 1);

        // Draw book cover/backing
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(BOOK_COVER, 0, 7, 152, 0, 34, 192, 256, 256);  // Left border
        guiGraphics.blit(BOOK_COVER, 34, 7, 49, 0, 103, 192, 256, 256); // Left panel
        guiGraphics.blit(BOOK_COVER, 137, 7, 45, 0, 4, 192, 256, 256);  // Left panel edge
        guiGraphics.blit(BOOK_COVER, 141, 7, 0, 0, 186, 192, 256, 256); // Spine + right

        // Gold borders for Agebooks
        if (isAgebook) {
            guiGraphics.blit(BOOK_COVER, 0, 7, 186, 0, 34, 192, 256, 256);   // Left gold border
            guiGraphics.blit(BOOK_COVER, 293, 7, 186, 0, 34, 192, 256, 256); // Right gold border
        }

        // Draw left page if not on page 0
        if (currentPageIndex > 0 && !pages.isEmpty()) {
            guiGraphics.blit(BOOK_PAGE_LEFT, 7, 0, 0, 0, 156, 195, 256, 256);
        }

        // Draw right page with link panel
        ItemStack currentPage = getCurrentPage();
        boolean hasLinkPanel = !currentPage.isEmpty() && Page.isLinkPanel(currentPage);

        // On page 0 (title page), draw the link panel
        if (currentPageIndex == 0) {
            // Link panel area (173, 20) size (132, 83)
            drawLinkPanel(guiGraphics, 173, 20, 132, 83);

            // Draw right page with panel cutout
            guiGraphics.blit(BOOK_PAGE_RIGHT, 163, 0, 0, 0, 156, 195, 256, 256);

            // Draw title and authors on left page
            drawTitlePage(guiGraphics);
        } else if (hasLinkPanel) {
            // Link panel on non-title page
            drawLinkPanel(guiGraphics, 173, 20, 132, 83);
            guiGraphics.blit(BOOK_PAGE_RIGHT, 163, 0, 0, 0, 156, 195, 256, 256);
        } else if (!currentPage.isEmpty()) {
            // Solid right page for symbol pages
            guiGraphics.blit(BOOK_PAGE_RIGHT_SOLID, 163, 0, 0, 0, 156, 195, 256, 256);
            // Could render symbol here if we implement symbol rendering
        } else {
            // Empty page - just show solid right page
            guiGraphics.blit(BOOK_PAGE_RIGHT_SOLID, 163, 0, 0, 0, 156, 195, 256, 256);
        }

        // Draw page numbers
        drawPageNumbers(guiGraphics);

        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Draws the link panel with gradient background.
     */
    private void drawLinkPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        boolean visited = isTargetVisited();
        int topColor = visited ? 0xFF000044 : 0xFF000000;
        int bottomColor = visited ? 0xFF006666 : 0xFF000000;

        // Draw gradient rectangle
        guiGraphics.fillGradient(x, y, x + width, y + height, topColor, bottomColor);

        // Show "unwritten" text only if no valid destination
        if (!hasValidDestination()) {
            Component unwrittenText = Component.translatable("gui.mystcraft.book.unwritten");
            int textWidth = this.font.width(unwrittenText);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - 8) / 2;
            guiGraphics.drawString(this.font, unwrittenText, textX, textY, 0x888888, false);
        }
    }

    /**
     * Draws the title page (page 0) content.
     */
    private void drawTitlePage(GuiGraphics guiGraphics) {
        // Book title
        String bookTitle = getBookDisplayName();
        guiGraphics.drawString(this.font, bookTitle, 40, 40, 0x000000, false);

        // Authors
        Collection<String> authors = getAuthors();
        int y = 55;
        if (!authors.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.mystcraft.book.by"), 40, y, 0x404040, false);
            y += 12;
            for (String author : authors) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(50, y, 0);
                guiGraphics.pose().scale(0.8f, 0.8f, 1);
                guiGraphics.drawString(this.font, author, 0, 0, 0x000000, false);
                guiGraphics.pose().popPose();
                y += 10;
            }
        }

        // Destination info
        if (book.getTag() != null) {
            y += 10;
            Integer dimId = LinkOptions.getDimensionUID(book.getTag());
            BlockPos spawn = LinkOptions.getSpawn(book.getTag());

            if (dimId != null) {
                String dimText = isAgebook ? "Age " + dimId : "Dimension " + dimId;
                guiGraphics.drawString(this.font, dimText, 40, y, 0x404040, false);
                y += 12;
            }

            if (spawn != null) {
                String posText = "Pos: " + spawn.getX() + ", " + spawn.getY() + ", " + spawn.getZ();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(40, y, 0);
                guiGraphics.pose().scale(0.7f, 0.7f, 1);
                guiGraphics.drawString(this.font, posText, 0, 0, 0x606060, false);
                guiGraphics.pose().popPose();
            }
        }
    }

    /**
     * Draws page numbers at the bottom.
     */
    private void drawPageNumbers(GuiGraphics guiGraphics) {
        int pageCount = getPageCount();
        String pageText;
        if (pageCount > 0) {
            pageText = currentPageIndex + "/" + pageCount;
        } else {
            pageText = "- " + currentPageIndex + " -";
        }
        int textWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText, 165 - textWidth / 2, 185, 0x000000, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Convert to local coordinates
            double localX = (mouseX - leftPos) / xScale;
            double localY = (mouseY - topPos) / yScale;

            // Check link panel click (page 0, right page panel area)
            if (currentPageIndex == 0 &&
                localX >= 173 && localX <= 305 &&
                localY >= 20 && localY <= 103) {
                if (canLink()) {
                    performLink();
                    return true;
                }
            }

            // Left page click - go left
            if (localX >= 0 && localX <= 156 && localY >= 0 && localY <= 195) {
                pageLeft();
                return true;
            }

            // Right page click - go right
            if (localX >= 158 && localX <= 312 && localY >= 0 && localY <= 195) {
                pageRight();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Left arrow or A key
        if (keyCode == 263 || keyCode == 65) {
            pageLeft();
            return true;
        }
        // Right arrow or D key
        if (keyCode == 262 || keyCode == 68) {
            pageRight();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        if (book.getTag() != null) {
            String name = LinkOptions.getDisplayName(book.getTag());
            if (!"???".equals(name) && !name.isEmpty()) {
                return name;
            }
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
        // For now, always return false (unvisited appearance)
        // Could track visited ages in the future
        return book.getTag() != null && LinkOptions.getDimensionUID(book.getTag()) != null;
    }

    /**
     * Checks if the book has a valid link destination or can create one.
     */
    private boolean hasValidDestination() {
        if (book.getTag() == null) {
            return false;
        }
        Integer dimId = LinkOptions.getDimensionUID(book.getTag());
        BlockPos spawn = LinkOptions.getSpawn(book.getTag());

        // Existing books with destination
        if (dimId != null && spawn != null) {
            return true;
        }

        // New Agebooks can be activated if they have a link panel
        if (isAgebook) {
            return AgebookItem.isNewAgebook(book);
        }

        return false;
    }

    /**
     * Checks if linking is allowed (for permission overlay).
     */
    private boolean canLink() {
        return hasValidDestination();
    }

    /**
     * Performs the link when the link panel is clicked.
     */
    private void performLink() {
        if (entityId == HAND_BASED) {
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

    /**
     * Opens a book screen for the given item held in player's hand.
     */
    public static void open(ItemStack book) {
        if (book.getItem() instanceof AgebookItem || book.getItem() instanceof LinkbookItem) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (ItemStack.isSameItemSameTags(mc.player.getOffhandItem(), book)) {
                hand = InteractionHand.OFF_HAND;
            }

            mc.setScreen(new BookScreen(book, hand));
        }
    }

    /**
     * Opens a book screen for a book in a LinkbookEntity.
     */
    public static void openForEntity(ItemStack book, int entityId) {
        if (book.getItem() instanceof AgebookItem || book.getItem() instanceof LinkbookItem) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            mc.setScreen(new BookScreen(book, InteractionHand.MAIN_HAND, entityId));
        }
    }
}
