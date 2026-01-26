package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Screen for viewing book contents (Agebooks and Linkbooks).
 * Displays the book's link panel, pages, and allows linking.
 */
public class BookScreen extends Screen {

    // Use vanilla book texture as base
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/book.png");

    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;

    private final ItemStack book;
    private final boolean isAgebook;
    private final boolean isLinkbook;

    private int leftPos;
    private int topPos;

    private Button linkButton;

    public BookScreen(ItemStack book) {
        super(getBookTitle(book));
        this.book = book;
        this.isAgebook = book.getItem() instanceof AgebookItem;
        this.isLinkbook = book.getItem() instanceof LinkbookItem;
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

        // Add link button if the book has a valid destination
        if (hasValidDestination()) {
            this.linkButton = Button.builder(
                    Component.translatable("gui.mystcraft.book.link"),
                    button -> performLink()
            ).bounds(leftPos + BOOK_WIDTH / 2 - 50, topPos + BOOK_HEIGHT - 30, 100, 20).build();
            addRenderableWidget(linkButton);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Draw book background
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);

        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, topPos + 16, 0x000000);

        // Draw book info
        int textY = topPos + 35;
        int textX = leftPos + 36;
        int lineHeight = 12;

        if (book.getTag() != null) {
            // Dimension info
            Integer dimId = LinkOptions.getDimensionUID(book.getTag());
            if (dimId != null) {
                if (isAgebook) {
                    guiGraphics.drawString(this.font,
                            Component.translatable("gui.mystcraft.book.age", dimId),
                            textX, textY, 0x000000, false);
                } else {
                    guiGraphics.drawString(this.font,
                            Component.translatable("gui.mystcraft.book.dimension", dimId),
                            textX, textY, 0x000000, false);
                }
                textY += lineHeight;
            }

            // Destination coordinates
            BlockPos spawn = LinkOptions.getSpawn(book.getTag());
            if (spawn != null) {
                guiGraphics.drawString(this.font,
                        Component.translatable("gui.mystcraft.book.destination",
                                spawn.getX(), spawn.getY(), spawn.getZ()),
                        textX, textY, 0x000000, false);
                textY += lineHeight;
            }

            // Authors (for Agebooks)
            if (isAgebook && book.getItem() instanceof AgebookItem agebookItem) {
                Collection<String> authors = agebookItem.getAuthors(book);
                if (!authors.isEmpty()) {
                    textY += lineHeight / 2;
                    guiGraphics.drawString(this.font,
                            Component.translatable("gui.mystcraft.book.authors"),
                            textX, textY, 0x000000, false);
                    textY += lineHeight;
                    for (String author : authors) {
                        guiGraphics.drawString(this.font, "  " + author, textX, textY, 0x404040, false);
                        textY += lineHeight;
                        if (textY > topPos + BOOK_HEIGHT - 50) break; // Don't overflow
                    }
                }

                // Page count
                List<ItemStack> pages = agebookItem.getPageList(book);
                if (!pages.isEmpty()) {
                    textY += lineHeight / 2;
                    guiGraphics.drawString(this.font,
                            Component.translatable("gui.mystcraft.book.pages", pages.size()),
                            textX, textY, 0x000000, false);
                }
            }
        } else {
            // No data - show unwritten message
            guiGraphics.drawString(this.font,
                    Component.translatable("gui.mystcraft.book.unwritten"),
                    textX, textY, 0x800000, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Checks if the book has a valid link destination.
     */
    private boolean hasValidDestination() {
        if (book.getTag() == null) {
            return false;
        }
        Integer dimId = LinkOptions.getDimensionUID(book.getTag());
        BlockPos spawn = LinkOptions.getSpawn(book.getTag());
        return dimId != null && spawn != null;
    }

    /**
     * Performs the link when the button is clicked.
     */
    private void performLink() {
        // Send packet to server to perform the link
        // For now, just close the screen - actual linking happens on server
        // when the player uses the book item
        this.onClose();

        // The link will happen through the item use action
        // This screen is mainly for viewing book info
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Opens a book screen for the given item.
     */
    public static void open(ItemStack book) {
        if (book.getItem() instanceof AgebookItem || book.getItem() instanceof LinkbookItem) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new BookScreen(book));
        }
    }
}
