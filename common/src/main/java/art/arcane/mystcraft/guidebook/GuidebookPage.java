package art.arcane.mystcraft.guidebook;

/**
 * Represents a single page in the Mystcraft Guidebook.
 * Pages contain a title and text content that can be formatted
 * with stability indicators (+/-) for color coding.
 */
public class GuidebookPage {

  private final String title;
  private final String content;
  private final boolean stabilityIndicators;

  public GuidebookPage(String title, String content) {
    this(title, content, false);
  }

  public GuidebookPage(String title, String content, boolean stabilityIndicators) {
    this.title = title;
    this.content = content;
    this.stabilityIndicators = stabilityIndicators;
  }

  public static GuidebookPage of(String title, String content) {
    return new GuidebookPage(title, content);
  }

  public static GuidebookPage withStability(String title, String content) {
    return new GuidebookPage(title, content, true);
  }

  public static GuidebookPage textOnly(String content) {
    return new GuidebookPage(null, content);
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public boolean hasStabilityIndicators() {
    return stabilityIndicators;
  }
}
