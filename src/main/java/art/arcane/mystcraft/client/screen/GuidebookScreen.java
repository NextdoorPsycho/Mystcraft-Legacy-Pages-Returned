package art.arcane.mystcraft.client.screen;

import art.arcane.mystcraft.client.GuidebookTexture;
import art.arcane.mystcraft.guidebook.GuidebookContent;
import art.arcane.mystcraft.guidebook.GuidebookPage;
import art.arcane.mystcraft.guidebook.GuidebookChapter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Screen for displaying the Mystcraft Guidebook.
 * Clean book interface with chapter navigation and page turning.
 */
public class GuidebookScreen extends Screen {

    // Book dimensions
    private static final int BOOK_WIDTH = 276;
    private static final int BOOK_HEIGHT = 180;

    // Text area for left page (with better margins)
    private static final int LEFT_PAGE_X = 24;
    private static final int LEFT_PAGE_WIDTH = 104;

    // Text area for right page (with better margins)
    private static final int RIGHT_PAGE_X = 148;
    private static final int RIGHT_PAGE_WIDTH = 104;

    // Vertical text positioning (more breathing room)
    private static final int TEXT_TOP = 18;
    private static final int TEXT_BOTTOM = 152;
    private static final int LINE_HEIGHT = 11;

    // Navigation arrow positions
    private static final int ARROW_Y = 160;
    private static final int PREV_ARROW_X = 26;
    private static final int NEXT_ARROW_X = 250;

    // Colors (higher contrast for readability)
    private static final int TEXT_COLOR = 0x1A1A1A;
    private static final int TITLE_COLOR = 0x000000;
    private static final int LINK_COLOR = 0x0040A0;
    private static final int LINK_HOVER_COLOR = 0x0060D0;
    private static final int PAGE_NUM_COLOR = 0x505050;
    private static final int STABILITY_POSITIVE = 0x107010;
    private static final int STABILITY_NEGATIVE = 0xA01010;

    private final GuidebookContent content;

    private int leftPos;
    private int topPos;

    private int currentChapterIndex = 0;
    private int currentPageIndex = 0;
    private boolean showingTableOfContents = true;
    private int tocScrollOffset = 0;

    public GuidebookScreen() {
        super(Component.translatable("item.mystcraft.guidebook"));
        this.content = GuidebookContent.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - BOOK_WIDTH) / 2;
        this.topPos = (this.height - BOOK_HEIGHT) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw book background using dynamic texture
        ResourceLocation texture = GuidebookTexture.getTexture();
        graphics.blit(texture, leftPos, topPos, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, 512, 256);

        if (showingTableOfContents) {
            renderTableOfContents(graphics, mouseX, mouseY);
        } else {
            renderChapterContent(graphics, mouseX, mouseY);
        }

        // Don't call super.render - we're not using widgets
    }

    private void renderTableOfContents(GuiGraphics graphics, int mouseX, int mouseY) {
        // Left page - Title and description
        int leftX = leftPos + LEFT_PAGE_X;
        int y = topPos + TEXT_TOP + 4;

        // Book title
        String title = "The Art of Writing";
        graphics.drawString(this.font, title, leftX + (LEFT_PAGE_WIDTH - font.width(title)) / 2, y, TITLE_COLOR, false);
        y += LINE_HEIGHT + 6;

        // Decorative line
        int lineStart = leftX + 15;
        int lineEnd = leftX + LEFT_PAGE_WIDTH - 15;
        graphics.fill(lineStart, y, lineEnd, y + 1, 0xFF000000 | TITLE_COLOR);
        y += LINE_HEIGHT;

        // Subtitle
        String subtitle = "A Guide to Ages";
        graphics.drawString(this.font, subtitle, leftX + (LEFT_PAGE_WIDTH - font.width(subtitle)) / 2, y, TEXT_COLOR, false);
        y += LINE_HEIGHT + 8;

        // Description text
        String[] desc = {
                "Within these pages",
                "lies the knowledge",
                "of the D'ni, masters",
                "of linking worlds.",
                "",
                "Study well, and you",
                "may learn to write",
                "passages to realms",
                "of your own design."
        };

        for (String line : desc) {
            if (!line.isEmpty()) {
                graphics.drawString(this.font, line, leftX + (LEFT_PAGE_WIDTH - font.width(line)) / 2, y, TEXT_COLOR, false);
            }
            y += LINE_HEIGHT;
        }

        // Right page - Chapter list (single column, scrollable)
        int rightX = leftPos + RIGHT_PAGE_X;
        y = topPos + TEXT_TOP + 4;

        graphics.drawString(this.font, "Contents", rightX + (RIGHT_PAGE_WIDTH - font.width("Contents")) / 2, y, TITLE_COLOR, false);
        y += LINE_HEIGHT + 8;

        // Chapter list
        List<GuidebookChapter> chapters = content.getChapters();
        int itemHeight = LINE_HEIGHT;
        int maxVisible = 11;

        for (int i = 0; i < Math.min(chapters.size(), maxVisible); i++) {
            int displayIndex = i + tocScrollOffset;
            if (displayIndex >= chapters.size()) break;

            GuidebookChapter chapter = chapters.get(displayIndex);
            String entry = (displayIndex + 1) + ". " + truncate(chapter.getTitle(), 14);

            int itemX = rightX + 2;
            int itemY = y + (i * itemHeight);

            boolean hovered = isInBounds(mouseX, mouseY, itemX, itemY, RIGHT_PAGE_WIDTH - 4, itemHeight);
            int color = hovered ? LINK_HOVER_COLOR : LINK_COLOR;

            graphics.drawString(this.font, entry, itemX, itemY, color, false);
        }

        // Scroll hint if needed
        if (chapters.size() > maxVisible) {
            String scrollHint = "(scroll for more)";
            graphics.drawString(this.font, scrollHint, rightX + (RIGHT_PAGE_WIDTH - font.width(scrollHint)) / 2, topPos + TEXT_BOTTOM - 8, PAGE_NUM_COLOR, false);
        }
    }

    private void renderChapterContent(GuiGraphics graphics, int mouseX, int mouseY) {
        List<GuidebookChapter> chapters = content.getChapters();
        if (currentChapterIndex >= chapters.size()) return;

        GuidebookChapter chapter = chapters.get(currentChapterIndex);
        List<GuidebookPage> pages = chapter.getPages();

        // Render left page
        if (currentPageIndex < pages.size()) {
            renderPage(graphics, pages.get(currentPageIndex), leftPos + LEFT_PAGE_X, topPos + TEXT_TOP, LEFT_PAGE_WIDTH);
        }

        // Render right page
        if (currentPageIndex + 1 < pages.size()) {
            renderPage(graphics, pages.get(currentPageIndex + 1), leftPos + RIGHT_PAGE_X, topPos + TEXT_TOP, RIGHT_PAGE_WIDTH);
        }

        // Chapter title centered at top
        String chapterTitle = chapter.getTitle();
        int titleX = leftPos + BOOK_WIDTH / 2 - font.width(chapterTitle) / 2;
        graphics.drawString(this.font, chapterTitle, titleX, topPos + 4, TITLE_COLOR, false);

        // Page numbers at bottom
        int leftPageNum = currentPageIndex + 1;
        int rightPageNum = currentPageIndex + 2;
        int totalPages = pages.size();

        String leftNum = String.valueOf(leftPageNum);
        String rightNum = rightPageNum <= totalPages ? String.valueOf(rightPageNum) : "";

        graphics.drawString(this.font, leftNum, leftPos + LEFT_PAGE_X + LEFT_PAGE_WIDTH / 2 - font.width(leftNum) / 2, topPos + TEXT_BOTTOM, PAGE_NUM_COLOR, false);
        if (!rightNum.isEmpty()) {
            graphics.drawString(this.font, rightNum, leftPos + RIGHT_PAGE_X + RIGHT_PAGE_WIDTH / 2 - font.width(rightNum) / 2, topPos + TEXT_BOTTOM, PAGE_NUM_COLOR, false);
        }

        // Navigation arrows
        boolean hasPrev = currentPageIndex > 0 || currentChapterIndex > 0;
        boolean hasNext = currentPageIndex + 2 < pages.size() || currentChapterIndex < chapters.size() - 1;

        if (hasPrev) {
            boolean hovered = isInBounds(mouseX, mouseY, leftPos + PREV_ARROW_X, topPos + ARROW_Y, 18, 10);
            int color = hovered ? LINK_HOVER_COLOR : LINK_COLOR;
            graphics.drawString(this.font, "<< Prev", leftPos + PREV_ARROW_X, topPos + ARROW_Y, color, false);
        }

        if (hasNext) {
            String nextText = "Next >>";
            boolean hovered = isInBounds(mouseX, mouseY, leftPos + NEXT_ARROW_X - font.width(nextText), topPos + ARROW_Y, font.width(nextText), 10);
            int color = hovered ? LINK_HOVER_COLOR : LINK_COLOR;
            graphics.drawString(this.font, nextText, leftPos + NEXT_ARROW_X - font.width(nextText), topPos + ARROW_Y, color, false);
        }

        // Contents link
        String contentsText = "[Contents]";
        int contentsX = leftPos + BOOK_WIDTH / 2 - font.width(contentsText) / 2;
        boolean contentsHovered = isInBounds(mouseX, mouseY, contentsX, topPos + ARROW_Y, font.width(contentsText), 10);
        graphics.drawString(this.font, contentsText, contentsX, topPos + ARROW_Y, contentsHovered ? LINK_HOVER_COLOR : LINK_COLOR, false);
    }

    private void renderPage(GuiGraphics graphics, GuidebookPage page, int x, int y, int width) {
        int availableHeight = TEXT_BOTTOM - TEXT_TOP - 16;
        int maxLines = availableHeight / LINE_HEIGHT;
        int contentY = y;

        // Page title
        String title = page.getTitle();
        if (title != null && !title.isEmpty()) {
            // Draw title (bold effect via shadow)
            graphics.drawString(this.font, title, x, contentY, TITLE_COLOR, false);
            contentY += LINE_HEIGHT + 2;
            // Subtle underline
            graphics.fill(x, contentY - 1, x + Math.min(font.width(title), width), contentY, 0xFF000000 | TITLE_COLOR);
            contentY += 6; // Extra space after title
            maxLines -= 2;
        }

        // Page content - wrap text
        String pageContent = page.getContent();
        List<FormattedCharSequence> lines = this.font.split(Component.literal(pageContent), width);

        int lineCount = 0;
        String[] rawLines = pageContent.split("\n");
        int rawLineIndex = 0;

        for (FormattedCharSequence line : lines) {
            if (lineCount >= maxLines) break;

            int color = TEXT_COLOR;
            if (page.hasStabilityIndicators() && rawLineIndex < rawLines.length) {
                String rawLine = rawLines[rawLineIndex];
                if (rawLine.trim().startsWith("+")) color = STABILITY_POSITIVE;
                else if (rawLine.trim().startsWith("-")) color = STABILITY_NEGATIVE;
            }

            graphics.drawString(this.font, line, x, contentY + (lineCount * LINE_HEIGHT), color, false);
            lineCount++;

            // Track raw line breaks for stability coloring
            if (lineCount > 0 && rawLineIndex < rawLines.length - 1) {
                rawLineIndex++;
            }
        }
    }

    private String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars - 2) + "..";
    }

    private boolean isInBounds(double x, double y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (showingTableOfContents) {
            // Check chapter clicks (single column layout)
            int rightX = leftPos + RIGHT_PAGE_X;
            int y = topPos + TEXT_TOP + 4 + LINE_HEIGHT + 8;
            int itemHeight = LINE_HEIGHT;
            int maxVisible = 11;

            List<GuidebookChapter> chapters = content.getChapters();
            for (int i = 0; i < Math.min(chapters.size(), maxVisible); i++) {
                int displayIndex = i + tocScrollOffset;
                if (displayIndex >= chapters.size()) break;

                int itemX = rightX + 2;
                int itemY = y + (i * itemHeight);

                if (isInBounds(mouseX, mouseY, itemX, itemY, RIGHT_PAGE_WIDTH - 4, itemHeight)) {
                    openChapter(displayIndex);
                    return true;
                }
            }
        } else {
            List<GuidebookChapter> chapters = content.getChapters();
            GuidebookChapter chapter = chapters.get(currentChapterIndex);
            List<GuidebookPage> pages = chapter.getPages();

            // Check prev arrow
            boolean hasPrev = currentPageIndex > 0 || currentChapterIndex > 0;
            if (hasPrev && isInBounds(mouseX, mouseY, leftPos + PREV_ARROW_X, topPos + ARROW_Y, 40, 10)) {
                previousPage();
                return true;
            }

            // Check next arrow
            boolean hasNext = currentPageIndex + 2 < pages.size() || currentChapterIndex < chapters.size() - 1;
            String nextText = "Next >>";
            if (hasNext && isInBounds(mouseX, mouseY, leftPos + NEXT_ARROW_X - font.width(nextText), topPos + ARROW_Y, font.width(nextText), 10)) {
                nextPage();
                return true;
            }

            // Check contents link
            String contentsText = "[Contents]";
            int contentsX = leftPos + BOOK_WIDTH / 2 - font.width(contentsText) / 2;
            if (isInBounds(mouseX, mouseY, contentsX, topPos + ARROW_Y, font.width(contentsText), 10)) {
                showTableOfContents();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (showingTableOfContents) {
            int maxScroll = Math.max(0, content.getChapters().size() - 11);
            if (deltaY > 0) {
                tocScrollOffset = Math.max(0, tocScrollOffset - 1);
            } else {
                tocScrollOffset = Math.min(maxScroll, tocScrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void openChapter(int index) {
        this.currentChapterIndex = index;
        this.currentPageIndex = 0;
        this.showingTableOfContents = false;
    }

    private void showTableOfContents() {
        this.showingTableOfContents = true;
    }

    private void previousPage() {
        if (showingTableOfContents) return;

        if (currentPageIndex > 0) {
            currentPageIndex -= 2;
            if (currentPageIndex < 0) currentPageIndex = 0;
        } else if (currentChapterIndex > 0) {
            currentChapterIndex--;
            List<GuidebookPage> pages = content.getChapters().get(currentChapterIndex).getPages();
            currentPageIndex = ((pages.size() - 1) / 2) * 2;
        }
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
            currentChapterIndex++;
            currentPageIndex = 0;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 262) { // Right arrow
            nextPage();
            return true;
        } else if (keyCode == 263) { // Left arrow
            previousPage();
            return true;
        } else if (keyCode == 256) { // Escape
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
