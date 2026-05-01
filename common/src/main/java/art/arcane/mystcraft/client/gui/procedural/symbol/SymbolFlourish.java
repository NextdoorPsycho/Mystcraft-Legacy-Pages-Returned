package art.arcane.mystcraft.client.gui.procedural.symbol;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Card-rank decorative frames drawn around the motif on a symbol page. The
 * frame's complexity scales with the symbol's
 * {@link art.arcane.mystcraft.api.symbol.IAgeSymbol#getCardRank() card rank} so
 * a player can read rarity at a glance:
 *
 * <pre>
 *   rank 1 → no frame (plain page edge)
 *   rank 2 → single hairline border + corner dots
 *   rank 3 → double-line border + corner pips + spine ribbon
 *   rank 4 → single-line border + corner filigree + mid-edge pips
 *   rank 5 → double-line border + corner ornaments + spine ribbon + halo
 * </pre>
 *
 * <p>The frame palette comes from the same {@link SymbolPalette.Entry}, so
 * a rank-5 biome page has a green halo, a rank-5 structure page has cobalt.
 *
 * <h3>Resolution</h3>
 * After the legibility pass, all literal pixel offsets here are doubled (was
 * tuned for {@code SymbolPageTextureFactory.PAGE_SIZE = 128} → now tuned for
 * {@code 256}). Visible ornaments (dots, filigree, halo) route through
 * {@link SymbolGlyphPrimitives}'s anti-aliased path so corner pips and halo
 * rings read as smooth-edged ink rather than pixel-stair octagons.
 */
public final class SymbolFlourish {

  private static final Treatment RANK_1 =
      new Treatment(BorderStyle.NONE, CornerOrnament.NONE, false, false, false);
  private static final Treatment RANK_2 =
      new Treatment(BorderStyle.SINGLE_HAIRLINE, CornerOrnament.DOTS, false, false, false);
  private static final Treatment RANK_3 =
      new Treatment(BorderStyle.SINGLE_HAIRLINE, CornerOrnament.PIPS, true, false, false);
  private static final Treatment RANK_4 =
      new Treatment(BorderStyle.DOUBLE_LINE, CornerOrnament.FILIGREE_3_STROKE, true, true, false);
  private static final Treatment RANK_5 =
      new Treatment(BorderStyle.DOUBLE_WITH_ORNAMENTS, CornerOrnament.FILIGREE_5_STROKE_PIP, true, true, true);
  private SymbolFlourish() {
  }

  /**
   * Returns the treatment for the given card rank. Null / unknown ranks fall
   * back to the rank-1 plain treatment so generation never fails.
   */
  @NotNull
  public static Treatment forRank(@Nullable Integer rank) {
    if (rank == null) return RANK_1;
    int r = rank.intValue();
    if (r <= 1) return RANK_1;
    if (r == 2) return RANK_2;
    if (r == 3) return RANK_3;
    if (r == 4) return RANK_4;
    return RANK_5;
  }

  /**
   * Draws the rank-appropriate frame around the rectangle
   * {@code (x0, y0, w, h)} inside {@code image}. Pixels outside the rectangle
   * (halo etc.) are clipped to image bounds.
   */
  public static void frame(@NotNull NativeImage image, int x0, int y0, int w, int h,
                           @NotNull SymbolPalette.Entry palette, @NotNull Treatment treatment) {
    int inkArgb = 0xFF000000 | (palette.inkColor() & 0xFFFFFF);
    int accentArgb = 0xFF000000 | (palette.accentColor() & 0xFFFFFF);
    int haloArgb = palette.haloColor() & 0xFFFFFF;

    switch (treatment.borderStyle()) {
      case NONE -> {
      }
      case SINGLE_HAIRLINE -> drawRectOutline(image, x0, y0, w, h, inkArgb);
      case DOUBLE_LINE -> {
        drawRectOutline(image, x0, y0, w, h, inkArgb);

        drawRectOutline(image, x0 + 4, y0 + 4, w - 8, h - 8, inkArgb);
      }
      case SINGLE_WITH_CORNERS -> {
        drawRectOutline(image, x0, y0, w, h, inkArgb);

        drawCornerNotch(image, x0, y0, w, h, accentArgb, 8);
      }
      case DOUBLE_WITH_ORNAMENTS -> {
        drawRectOutline(image, x0, y0, w, h, inkArgb);
        drawRectOutline(image, x0 + 4, y0 + 4, w - 8, h - 8, inkArgb);

        drawCornerNotch(image, x0, y0, w, h, accentArgb, 10);
      }
    }

    switch (treatment.cornerOrnament()) {
      case NONE -> {
      }

      case DOTS -> drawCornerDots(image, x0, y0, w, h, accentArgb, 2.0);
      case PIPS -> drawCornerDots(image, x0, y0, w, h, accentArgb, 4.0);
      case FILIGREE_3_STROKE ->
          drawCornerFiligree(image, x0, y0, w, h, accentArgb, 3);
      case FILIGREE_5_STROKE_PIP -> {
        drawCornerFiligree(image, x0, y0, w, h, accentArgb, 5);
        drawCornerDots(image, x0, y0, w, h, accentArgb, 4.0);
      }
    }

    if (treatment.useSpineRibbon()) {
      drawSpineRibbon(image, x0, y0, w, h, accentArgb);
    }

    if (treatment.useMidEdgePips()) {
      drawMidEdgePips(image, x0, y0, w, h, accentArgb);
    }

    if (treatment.useHalo()) {
      drawSoftHalo(image, x0, y0, w, h, haloArgb);
    }
  }

  private static void drawRectOutline(NativeImage image, int x, int y, int w, int h, int argb) {
    if (w <= 0 || h <= 0) return;
    int abgr = toAbgr(argb);
    for (int i = 0; i < w; i++) {
      safeSet(image, x + i, y, abgr);
      safeSet(image, x + i, y + h - 1, abgr);
    }
    for (int i = 0; i < h; i++) {
      safeSet(image, x, y + i, abgr);
      safeSet(image, x + w - 1, y + i, abgr);
    }
  }

  private static void drawCornerNotch(NativeImage image, int x, int y, int w, int h, int argb, int size) {
    int abgr = toAbgr(argb);
    int[][] corners = {
        {x, y, 1, 1},
        {x + w - 1, y, -1, 1},
        {x, y + h - 1, 1, -1},
        {x + w - 1, y + h - 1, -1, -1}
    };
    for (int[] corner : corners) {
      int cx = corner[0];
      int cy = corner[1];
      int dx = corner[2];
      int dy = corner[3];
      for (int i = 0; i < size; i++) {
        safeSet(image, cx + dx * i, cy, abgr);
        safeSet(image, cx, cy + dy * i, abgr);
      }
    }
  }

  private static void drawCornerDots(NativeImage image, int x, int y, int w, int h,
                                     int argb, double radius) {
    double[][] corners = {
        {x + 4, y + 4},
        {x + w - 5, y + 4},
        {x + 4, y + h - 5},
        {x + w - 5, y + h - 5}
    };
    for (double[] c : corners) {
      SymbolGlyphPrimitives.fillSmoothDisc(image, c[0], c[1], radius, argb);
    }
  }

  private static void drawCornerFiligree(NativeImage image, int x, int y, int w, int h,
                                         int argb, int strokeCount) {

    double[][] corners = {
        {x + 4, y + 4, 1, 1},
        {x + w - 5, y + 4, -1, 1},
        {x + 4, y + h - 5, 1, -1},
        {x + w - 5, y + h - 5, -1, -1}
    };
    double tendrilLen = 4 + strokeCount * 2;
    double thickness = 1.5;
    for (double[] corner : corners) {
      double cx = corner[0];
      double cy = corner[1];
      double dx = corner[2];
      double dy = corner[3];

      SymbolGlyphPrimitives.drawAaLine(image, cx, cy, cx + dx * tendrilLen, cy,
          thickness, argb);
      SymbolGlyphPrimitives.drawAaLine(image, cx, cy, cx, cy + dy * tendrilLen,
          thickness, argb);
      SymbolGlyphPrimitives.drawAaLine(image, cx, cy,
          cx + dx * tendrilLen * 0.7, cy + dy * tendrilLen * 0.7,
          thickness, argb);

      for (int s = 1; s < strokeCount - 2; s++) {
        double angleFraction = s / (double) (strokeCount - 1);
        double angle = angleFraction * (Math.PI / 2);
        double ex = cx + dx * Math.cos(angle) * tendrilLen * 0.85;
        double ey = cy + dy * Math.sin(angle) * tendrilLen * 0.85;
        SymbolGlyphPrimitives.drawAaLine(image, cx, cy, ex, ey, 1.0, argb);
      }
    }
  }

  private static void drawSpineRibbon(NativeImage image, int x, int y, int w, int h, int argb) {
    int abgr = toAbgr(argb);
    int spineX = x + 2;
    int top = y + h / 6;
    int bottom = y + h - h / 6;

    for (int sy = top; sy <= bottom; sy++) {
      safeSet(image, spineX, sy, abgr);
      safeSet(image, spineX + 1, sy, abgr);
      safeSet(image, spineX + 2, sy, abgr);
    }

    int tailX = spineX;
    int tailY = bottom;
    safeSet(image, tailX - 2, tailY + 2, abgr);
    safeSet(image, tailX + 4, tailY + 2, abgr);
  }

  private static void drawMidEdgePips(NativeImage image, int x, int y, int w, int h, int argb) {
    int midX = x + w / 2;
    int midY = y + h / 2;
    SymbolGlyphPrimitives.fillSmoothDisc(image, midX, y + 2, 2, argb);
    SymbolGlyphPrimitives.fillSmoothDisc(image, midX, y + h - 3, 2, argb);
    SymbolGlyphPrimitives.fillSmoothDisc(image, x + 2, midY, 2, argb);
    SymbolGlyphPrimitives.fillSmoothDisc(image, x + w - 3, midY, 2, argb);
  }

  private static void drawSoftHalo(NativeImage image, int x, int y, int w, int h, int rgb) {

    int[] alphas = {0x40, 0x38, 0x30, 0x28, 0x20, 0x18, 0x10, 0x08};
    for (int ring = 0; ring < alphas.length; ring++) {
      int alpha = alphas[ring];
      int argb = (alpha << 24) | (rgb & 0xFFFFFF);
      int abgr = toAbgr(argb);
      int rx = x - (ring + 1);
      int ry = y - (ring + 1);
      int rw = w + (ring + 1) * 2;
      int rh = h + (ring + 1) * 2;
      for (int i = 0; i < rw; i++) {
        safeSet(image, rx + i, ry, abgr);
        safeSet(image, rx + i, ry + rh - 1, abgr);
      }
      for (int i = 0; i < rh; i++) {
        safeSet(image, rx, ry + i, abgr);
        safeSet(image, rx + rw - 1, ry + i, abgr);
      }
    }
  }

  private static void safeSet(NativeImage image, int x, int y, int abgr) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
      return;
    image.setPixelRGBA(x, y, abgr);
  }

  private static int toAbgr(int argb) {
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    return (a << 24) | (b << 16) | (g << 8) | r;
  }

  /**
   * Visual treatment of the page border.
   */
  public enum BorderStyle {
    NONE,
    SINGLE_HAIRLINE,
    DOUBLE_LINE,
    SINGLE_WITH_CORNERS,
    DOUBLE_WITH_ORNAMENTS
  }

  /**
   * Decoration drawn at the four page corners.
   */
  public enum CornerOrnament {
    NONE,
    DOTS,
    PIPS,
    FILIGREE_3_STROKE,
    FILIGREE_5_STROKE_PIP
  }

  /**
   * Immutable per-rank flourish descriptor.
   *
   * @param borderStyle    Border treatment.
   * @param cornerOrnament Corner decoration.
   * @param useSpineRibbon Whether to draw a vertical ribbon along the binding
   *                       edge.
   * @param useMidEdgePips Whether to draw small accent dots at mid-edge.
   * @param useHalo        Whether the page gets an outward halo glow.
   */
  public record Treatment(BorderStyle borderStyle,
                          CornerOrnament cornerOrnament,
                          boolean useSpineRibbon, boolean useMidEdgePips,
                          boolean useHalo) {
  }
}
