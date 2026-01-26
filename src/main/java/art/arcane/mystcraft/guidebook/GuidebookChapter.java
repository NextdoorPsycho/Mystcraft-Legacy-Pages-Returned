package art.arcane.mystcraft.guidebook;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chapter in the Mystcraft Guidebook.
 * Each chapter has a title and contains multiple pages.
 */
public class GuidebookChapter {

    private final String title;
    private final List<GuidebookPage> pages;

    public GuidebookChapter(String title) {
        this.title = title;
        this.pages = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public List<GuidebookPage> getPages() {
        return pages;
    }

    public GuidebookChapter addPage(GuidebookPage page) {
        this.pages.add(page);
        return this;
    }

    public GuidebookChapter addPage(String title, String content) {
        return addPage(GuidebookPage.of(title, content));
    }

    public GuidebookChapter addStabilityPage(String title, String content) {
        return addPage(GuidebookPage.withStability(title, content));
    }

    public GuidebookChapter addTextPage(String content) {
        return addPage(GuidebookPage.textOnly(content));
    }

    public static GuidebookChapter create(String title) {
        return new GuidebookChapter(title);
    }
}
