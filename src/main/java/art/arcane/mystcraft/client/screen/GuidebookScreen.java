package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.guidebook.GuidebookContent;
import art.arcane.mystcraft.guidebook.GuidebookPage;
import art.arcane.mystcraft.guidebook.GuidebookChapter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Screen for displaying the Mystcraft Guidebook.
 * Features chapter navigation, page turning, and formatted text content.
 */
public class GuidebookScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/gui/guidebook.png");
    private static final ResourceLocation VANILLA_BOOK =
            new ResourceLocation("textures/gui/book.png");

    private static final int BOOK_WIDTH = 296;
    private static final int BOOK_HEIGHT = 180;
    private static final int TEXT_WIDTH = 114;
    private static final int TEXT_START_X_LEFT = 20;
    private static final int TEXT_START_X_RIGHT = 158;
    private static final int TEXT_START_Y = 18;
    private static final int LINE_HEIGHT = 9;
    private static final int LINES_PER_PAGE = 14;

    private final GuidebookContent content;

    private int leftPos;
    private int topPos;

    private int currentChapterIndex = 0;
    private int currentPageIndex = 0;
    private boolean showingTableOfContents = true;

    private Button prevPageButton;
    private Button nextPageButton;
    private Button tocButton;

    public GuidebookScreen() {
        super(Component.translatable("item.mystcraft.guidebook"));
        this.content = GuidebookContent.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - BOOK_WIDTH) / 2;
        this.topPos = (this.height - BOOK_HEIGHT) / 2;

        int buttonY = topPos + BOOK_HEIGHT - 16;

        this.prevPageButton = Button.builder(
                Component.literal("<"),
                button -> previousPage()
        ).bounds(leftPos + 20, buttonY, 20, 12).build();

        this.nextPageButton = Button.builder(
                Component.literal(">"),
                button -> nextPage()
        ).bounds(leftPos + BOOK_WIDTH - 40, buttonY, 20, 12).build();

        this.tocButton = Button.builder(
                Component.translatable("gui.mystcraft.guidebook.contents"),
                button -> showTableOfContents()
        ).bounds(leftPos + BOOK_WIDTH / 2 - 30, buttonY, 60, 12).build();

        addRenderableWidget(prevPageButton);
        addRenderableWidget(nextPageButton);
        addRenderableWidget(tocButton);

        updateButtonVisibility();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw book background (using vanilla book texture scaled)
        guiGraphics.blit(VANILLA_BOOK, leftPos, topPos, 0, 0, 146, 180, 256, 256);
        guiGraphics.blit(VANILLA_BOOK, leftPos + 146, topPos, 110, 0, 146, 180, 256, 256);

        if (showingTableOfContents) {
            renderTableOfContents(guiGraphics, mouseX, mouseY);
        } else {
            renderChapterContent(guiGraphics);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTableOfContents(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        String title = Component.translatable("gui.mystcraft.guidebook.title").getString();
        guiGraphics.drawCenteredString(this.font, title, leftPos + BOOK_WIDTH / 2, topPos + TEXT_START_Y, 0x000000);

        // Subtitle
        String subtitle = Component.translatable("gui.mystcraft.guidebook.subtitle").getString();
        guiGraphics.drawCenteredString(this.font, subtitle, leftPos + BOOK_WIDTH / 2, topPos + TEXT_START_Y + 12, 0x404040);

        // Chapter list
        List<GuidebookChapter> chapters = content.getChapters();
        int startY = topPos + TEXT_START_Y + 35;

        for (int i = 0; i < chapters.size(); i++) {
            GuidebookChapter chapter = chapters.get(i);
            String chapterTitle = (i + 1) + ". " + chapter.getTitle();

            int textX = leftPos + 36;
            int textY = startY + (i * 14);

            boolean hovered = mouseX >= textX && mouseX < textX + this.font.width(chapterTitle)
                    && mouseY >= textY && mouseY < textY + 12;

            int color = hovered ? 0x0000AA : 0x000000;
            guiGraphics.drawString(this.font, chapterTitle, textX, textY, color, false);

            if (hovered && this.minecraft != null) {
                // Draw underline on hover
                guiGraphics.fill(textX, textY + 10, textX + this.font.width(chapterTitle), textY + 11, 0xFF0000AA);
            }
        }
    }

    private void renderChapterContent(GuiGraphics guiGraphics) {
        List<GuidebookChapter> chapters = content.getChapters();
        if (currentChapterIndex >= chapters.size()) return;

        GuidebookChapter chapter = chapters.get(currentChapterIndex);
        List<GuidebookPage> pages = chapter.getPages();

        // Render left page
        if (currentPageIndex < pages.size()) {
            renderPage(guiGraphics, pages.get(currentPageIndex), leftPos + TEXT_START_X_LEFT, topPos + TEXT_START_Y);
        }

        // Render right page
        if (currentPageIndex + 1 < pages.size()) {
            renderPage(guiGraphics, pages.get(currentPageIndex + 1), leftPos + TEXT_START_X_RIGHT, topPos + TEXT_START_Y);
        }

        // Page numbers
        String pageNum = String.format("%d / %d", currentPageIndex + 1, pages.size());
        guiGraphics.drawCenteredString(this.font, pageNum, leftPos + BOOK_WIDTH / 2, topPos + BOOK_HEIGHT - 28, 0x404040);

        // Chapter title
        guiGraphics.drawCenteredString(this.font, chapter.getTitle(), leftPos + BOOK_WIDTH / 2, topPos + 5, 0x000000);
    }

    private void renderPage(GuiGraphics guiGraphics, GuidebookPage page, int x, int y) {
        // Page title
        if (page.getTitle() != null && !page.getTitle().isEmpty()) {
            guiGraphics.drawString(this.font, page.getTitle(), x, y, 0x000000, false);
            y += LINE_HEIGHT + 4;
        }

        // Page content - wrap text
        String content = page.getContent();
        List<FormattedCharSequence> lines = this.font.split(Component.literal(content), TEXT_WIDTH);

        int lineCount = 0;
        for (FormattedCharSequence line : lines) {
            if (lineCount >= LINES_PER_PAGE) break;

            int color = getLineColor(page, lineCount);
            guiGraphics.drawString(this.font, line, x, y + (lineCount * LINE_HEIGHT), color, false);
            lineCount++;
        }
    }

    private int getLineColor(GuidebookPage page, int lineIndex) {
        // Check for stability indicators in the content
        // Green = adds stability, Red = adds instability
        if (page.hasStabilityIndicators()) {
            String[] lines = page.getContent().split("\n");
            if (lineIndex < lines.length) {
                String line = lines[lineIndex];
                if (line.startsWith("+")) return 0x006600; // Green - adds stability
                if (line.startsWith("-")) return 0x880000; // Red - adds instability
            }
        }
        return 0x000000;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showingTableOfContents && button == 0) {
            List<GuidebookChapter> chapters = content.getChapters();
            int startY = topPos + TEXT_START_Y + 35;

            for (int i = 0; i < chapters.size(); i++) {
                GuidebookChapter chapter = chapters.get(i);
                String chapterTitle = (i + 1) + ". " + chapter.getTitle();

                int textX = leftPos + 36;
                int textY = startY + (i * 14);

                if (mouseX >= textX && mouseX < textX + this.font.width(chapterTitle)
                        && mouseY >= textY && mouseY < textY + 12) {
                    openChapter(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openChapter(int index) {
        this.currentChapterIndex = index;
        this.currentPageIndex = 0;
        this.showingTableOfContents = false;
        updateButtonVisibility();
    }

    private void showTableOfContents() {
        this.showingTableOfContents = true;
        updateButtonVisibility();
    }

    private void previousPage() {
        if (showingTableOfContents) return;

        if (currentPageIndex > 0) {
            currentPageIndex -= 2;
            if (currentPageIndex < 0) currentPageIndex = 0;
        } else if (currentChapterIndex > 0) {
            // Go to previous chapter's last page
            currentChapterIndex--;
            List<GuidebookPage> pages = content.getChapters().get(currentChapterIndex).getPages();
            currentPageIndex = Math.max(0, pages.size() - 2);
            if (currentPageIndex % 2 != 0) currentPageIndex--;
        }
        updateButtonVisibility();
    }

    private void nextPage() {
        if (showingTableOfContents) {
            if (!content.getChapters().isEmpty()) {
                openChapter(0);
            }
            return;
        }

        List<GuidebookChapter> chapters = content.getChapters();
        if (currentChapterIndex >= chapters.size()) return;

        List<GuidebookPage> pages = chapters.get(currentChapterIndex).getPages();
        if (currentPageIndex + 2 < pages.size()) {
            currentPageIndex += 2;
        } else if (currentChapterIndex < chapters.size() - 1) {
            // Go to next chapter
            currentChapterIndex++;
            currentPageIndex = 0;
        }
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean hasPrev = !showingTableOfContents && (currentPageIndex > 0 || currentChapterIndex > 0);
        boolean hasNext;

        if (showingTableOfContents) {
            hasNext = !content.getChapters().isEmpty();
        } else {
            List<GuidebookChapter> chapters = content.getChapters();
            if (currentChapterIndex < chapters.size()) {
                List<GuidebookPage> pages = chapters.get(currentChapterIndex).getPages();
                hasNext = currentPageIndex + 2 < pages.size() || currentChapterIndex < chapters.size() - 1;
            } else {
                hasNext = false;
            }
        }

        prevPageButton.visible = hasPrev;
        nextPageButton.visible = hasNext;
        tocButton.visible = !showingTableOfContents;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 262) { // Right arrow
            nextPage();
            return true;
        } else if (keyCode == 263) { // Left arrow
            previousPage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
