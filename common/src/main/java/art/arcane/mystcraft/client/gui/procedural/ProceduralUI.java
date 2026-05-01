package art.arcane.mystcraft.client.gui.procedural;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Stateless drawing primitives for procedural UI rendering.
 * <p>
 * All methods consume a {@link GuiGraphics} and draw immediately at screen
 * coordinates. Color values come from {@link GuiTheme} so resource packs can
 * restyle without code changes.
 */
public final class ProceduralUI {

  private ProceduralUI() {
  }

  /**
   * Draws a 1-pixel raised 3D border (light top/left, dark bottom/right). Used
   * for buttons and elements that should appear to sit above the surface.
   */
  public static void drawRaisedBorder(@NotNull GuiGraphics g, int x, int y, int w, int h) {
    int dark = GuiTheme.color("panel_border_dark");
    int light = GuiTheme.color("panel_border_light");

    g.fill(x, y, x + w, y + 1, light);
    g.fill(x, y, x + 1, y + h, light);

    g.fill(x, y + h - 1, x + w, y + h, dark);
    g.fill(x + w - 1, y, x + w, y + h, dark);
  }

  /**
   * Draws a 1-pixel inset 3D border (dark top/left, light bottom/right). Used
   * for slots and recessed areas.
   */
  public static void drawInsetBorder(@NotNull GuiGraphics g, int x, int y, int w, int h) {
    int dark = GuiTheme.color("panel_border_dark");
    int light = GuiTheme.color("panel_border_light");

    g.fill(x, y, x + w, y + 1, dark);
    g.fill(x, y, x + 1, y + h, dark);

    g.fill(x, y + h - 1, x + w, y + h, light);
    g.fill(x + w - 1, y, x + w, y + h, light);
  }

  /**
   * Draws a standard container panel with raised 3D border and background fill.
   * Matches the look used by {@code PortfolioScreen} / {@code FolderScreen}.
   */
  public static void drawPanel(@NotNull GuiGraphics g, int x, int y, int w, int h) {
    g.fill(x, y, x + w, y + h, GuiTheme.color("panel_bg"));
    drawRaisedBorder(g, x, y, w, h);
  }

  /**
   * Draws a panel with a header bar at the top.
   *
   * @param titleHeight pixel height of the header bar
   */
  public static void drawTitleBar(@NotNull GuiGraphics g, Font font, @NotNull Component title,
                                  int x, int y, int w, int titleHeight) {
    int titleBg = GuiTheme.color("title_bg");
    int titleText = GuiTheme.color("title_text");
    g.fill(x, y, x + w, y + titleHeight, titleBg);
    drawInsetBorder(g, x, y, w, titleHeight);

    int textWidth = font.width(title);
    int textX = x + (w - textWidth) / 2;
    int textY = y + (titleHeight - 8) / 2;
    g.drawString(font, title, textX, textY, titleText, false);
  }

  /**
   * Draws a horizontal 1-pixel divider line.
   */
  public static void drawHorizontalDivider(@NotNull GuiGraphics g, int x, int y, int w) {
    g.fill(x, y, x + w, y + 1, GuiTheme.color("panel_border_dark"));
    g.fill(x, y + 1, x + w, y + 2, GuiTheme.color("panel_border_light"));
  }

  /**
   * Draws a single 18x18 inventory slot with the standard inset 3D look.
   * Matches the appearance produced by vanilla Minecraft slot textures.
   */
  public static void drawSlot(@NotNull GuiGraphics g, int x, int y) {
    drawSlot(g, x, y, 18, 18);
  }

  /**
   * Draws an inventory slot of arbitrary size.
   */
  public static void drawSlot(@NotNull GuiGraphics g, int x, int y, int w, int h) {
    int bg = GuiTheme.color("slot_bg");
    int dark = GuiTheme.color("panel_border_dark");
    int light = GuiTheme.color("panel_border_light");

    g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);

    g.fill(x, y, x + w, y + 1, dark);
    g.fill(x, y, x + 1, y + h, dark);

    g.fill(x + w - 1, y, x + w, y + h, light);
    g.fill(x, y + h - 1, x + w, y + h, light);
  }

  /**
   * Draws a grid of slots aligned to a top-left anchor.
   *
   * @param spacing pixel spacing between slots (typically 0; slot size is 18)
   */
  public static void drawSlotGrid(@NotNull GuiGraphics g, int x, int y, int cols, int rows, int spacing) {
    int slot = 18;
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        drawSlot(g, x + col * (slot + spacing), y + row * (slot + spacing));
      }
    }
  }

  /**
   * Fills a region with a paper-like background: cream base, faint horizontal
   * lines mimicking ruled parchment.
   */
  public static void drawPaperBackground(@NotNull GuiGraphics g, int x, int y, int w, int h) {
    g.fill(x, y, x + w, y + h, GuiTheme.color("page_bg"));
    int line = GuiTheme.color("page_line");

    for (int ly = y + 6; ly < y + h - 4; ly += 8) {
      g.fill(x + 4, ly, x + w - 4, ly + 1, line);
    }
  }

  /**
   * Fills a region with leather grain: solid base color plus a noisy stipple
   * pattern of darker/lighter pixels. Deterministic for the same x/y offsets.
   *
   * @param baseColor ARGB base color of the leather
   */
  public static void drawLeatherGrain(@NotNull GuiGraphics g, int x, int y, int w, int h, int baseColor) {
    g.fill(x, y, x + w, y + h, baseColor);
    int dark = darken(baseColor, 0.78f);
    int light = lighten(baseColor, 1.15f);

    for (int dy = 0; dy < h; dy++) {
      for (int dx = 0; dx < w; dx++) {
        int hash = (dx * 1664525 + dy * 1013904223) ^ (baseColor >>> 8);
        int bucket = hash & 0xFF;
        if (bucket < 18) {
          g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, dark);
        } else if (bucket > 240) {
          g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, light);
        }
      }
    }
  }

  /**
   * Fills a region with a wood-grain look: vertical streaks of varying tone.
   */
  public static void drawWoodGrain(@NotNull GuiGraphics g, int x, int y, int w, int h, int baseColor) {
    g.fill(x, y, x + w, y + h, baseColor);
    int streak1 = darken(baseColor, 0.85f);
    int streak2 = darken(baseColor, 0.72f);
    int highlight = lighten(baseColor, 1.10f);
    for (int dx = 0; dx < w; dx++) {
      int hash = (dx * 2654435761L > 0 ? (int) (dx * 2654435761L) : -(int) (dx * 2654435761L)) ^ baseColor;
      int bucket = hash & 0x1F;
      if (bucket == 0) {
        g.fill(x + dx, y, x + dx + 1, y + h, streak2);
      } else if (bucket == 1 || bucket == 2) {
        g.fill(x + dx, y, x + dx + 1, y + h, streak1);
      } else if (bucket == 31) {
        g.fill(x + dx, y, x + dx + 1, y + h, highlight);
      }
    }
  }

  /**
   * Draws a small filled triangle warning glyph (yellow with a dark outline).
   * The glyph fits inside the given width/height.
   */
  public static void drawWarningGlyph(@NotNull GuiGraphics g, int x, int y, int w, int h) {
    int border = 0xFF202020;
    int fill = 0xFFFFCC22;
    int center = 0xFF3A2A04;

    for (int row = 0; row < h; row++) {
      float t = row / (float) (h - 1);
      int halfWidth = (int) (t * (w / 2));
      int leftX = x + (w / 2) - halfWidth;
      int rightX = x + (w / 2) + halfWidth + 1;
      int rowY = y + row;
      g.fill(leftX, rowY, rightX, rowY + 1, fill);

      g.fill(leftX, rowY, leftX + 1, rowY + 1, border);
      g.fill(rightX - 1, rowY, rightX, rowY + 1, border);
    }

    g.fill(x, y + h - 1, x + w, y + h, border);

    int midX = x + w / 2;
    int markTop = y + h / 4;
    int markBottom = y + (h * 5) / 8;
    g.fill(midX, markTop, midX + 1, markBottom, center);
    g.fill(midX, markBottom + 1, midX + 1, markBottom + 2, center);
  }

  /**
   * Draws a chevron arrow (left/right/up/down) inside the given box.
   */
  public static void drawChevron(@NotNull GuiGraphics g, int x, int y, int size, @NotNull ChevronDir dir, int color) {
    for (int i = 0; i < size; i++) {
      int t = i;
      int u = size - 1 - i;
      switch (dir) {
        case UP -> g.fill(x + t, y + u, x + size + 1 - t, y + u + 1, color);
        case DOWN -> g.fill(x + t, y + t, x + size + 1 - t, y + t + 1, color);
        case LEFT -> g.fill(x + u, y + t, x + u + 1, y + size + 1 - t, color);
        case RIGHT -> g.fill(x + t, y + t, x + t + 1, y + size + 1 - t, color);
      }
    }
  }

  /**
   * Draws a filled circle (or disc) by row-scanning the bounding box.
   * Pixel-perfect at small radii; uses {@link GuiGraphics#fill} for each row.
   */
  public static void drawDisc(@NotNull GuiGraphics g, int centerX, int centerY, int radius, int color) {
    int r2 = radius * radius;
    for (int dy = -radius; dy <= radius; dy++) {
      int dx = (int) Math.floor(Math.sqrt(Math.max(0, r2 - dy * dy)));
      g.fill(centerX - dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
    }
  }

  /**
   * Draws a 1px ring at the given radius — used to outline a
   * {@link #drawDisc}.
   */
  public static void drawRing(@NotNull GuiGraphics g, int centerX, int centerY, int radius, int color) {
    int r2Outer = radius * radius;
    int r2Inner = (radius - 1) * (radius - 1);
    for (int dy = -radius; dy <= radius; dy++) {
      int dxOuter = (int) Math.floor(Math.sqrt(Math.max(0, r2Outer - dy * dy)));
      int dxInner = (int) Math.floor(Math.sqrt(Math.max(0, r2Inner - dy * dy)));

      g.fill(centerX - dxOuter, centerY + dy, centerX - dxInner, centerY + dy + 1, color);

      g.fill(centerX + dxInner, centerY + dy, centerX + dxOuter + 1, centerY + dy + 1, color);
    }
  }

  /**
   * Draws a circular ink basin: dark base disc, inset rim ring, optional rim
   * highlight. Used by the InkMixer screen.
   */
  public static void drawBasin(@NotNull GuiGraphics g, int centerX, int centerY, int radius) {
    int rim = GuiTheme.color("ink_basin_rim");
    int base = GuiTheme.color("ink_base");
    drawDisc(g, centerX, centerY, radius, rim);
    drawDisc(g, centerX, centerY, radius - 2, base);

    drawRing(g, centerX, centerY, radius + 1, darken(rim, 0.6f));
  }

  /**
   * Multiplies the RGB components of an ARGB color by {@code factor}.
   */
  public static int darken(int argb, float factor) {
    int a = (argb >>> 24) & 0xFF;
    int r = clamp((int) (((argb >>> 16) & 0xFF) * factor));
    int g = clamp((int) (((argb >>> 8) & 0xFF) * factor));
    int b = clamp((int) ((argb & 0xFF) * factor));
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  /**
   * Same as {@link #darken(int, float)} but for factors > 1.0.
   */
  public static int lighten(int argb, float factor) {
    return darken(argb, factor);
  }

  /**
   * Linearly interpolates between two ARGB colors.
   */
  public static int lerp(int a, int b, float t) {
    int aA = (a >>> 24) & 0xFF;
    int aR = (a >>> 16) & 0xFF;
    int aG = (a >>> 8) & 0xFF;
    int aB = a & 0xFF;
    int bA = (b >>> 24) & 0xFF;
    int bR = (b >>> 16) & 0xFF;
    int bG = (b >>> 8) & 0xFF;
    int bB = b & 0xFF;
    int rA = clamp((int) (aA + (bA - aA) * t));
    int rR = clamp((int) (aR + (bR - aR) * t));
    int rG = clamp((int) (aG + (bG - aG) * t));
    int rB = clamp((int) (aB + (bB - aB) * t));
    return (rA << 24) | (rR << 16) | (rG << 8) | rB;
  }

  private static int clamp(int v) {
    if (v < 0) return 0;
    if (v > 255) return 255;
    return v;
  }

  /**
   * Direction enum for chevron icons.
   */
  public enum ChevronDir {
    UP, DOWN, LEFT, RIGHT
  }
}
