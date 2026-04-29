package art.arcane.mystcraft.client.gui.procedural.symbol;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Card-rank decorative frames drawn around the motif on a symbol page.
 * The frame's complexity scales with the symbol's
 * {@link art.arcane.mystcraft.api.symbol.IAgeSymbol#getCardRank() card rank}
 * so a player can read rarity at a glance:
 *
 * <pre>
 *   rank 1 → no frame (plain page edge)
 *   rank 2 → single hairline border + corner dots
 *   rank 3 → double-line border + corner pips + spine ribbon
 *   rank 4 → single-line border + corner filigree + mid-edge pips
 *   rank 5 → double-line border + corner ornaments + spine ribbon + halo
 * </pre>
 *
 * The frame palette comes from the same {@link SymbolPalette.Entry}, so
 * a rank-5 biome page has a green halo, a rank-5 structure page has cobalt.
 */
public final class SymbolFlourish {

  /** Visual treatment of the page border. */
  public enum BorderStyle {
    NONE,
    SINGLE_HAIRLINE,
    DOUBLE_LINE,
    SINGLE_WITH_CORNERS,
    DOUBLE_WITH_ORNAMENTS
  }

  /** Decoration drawn at the four page corners. */
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
   * @param useSpineRibbon Whether to draw a vertical ribbon along the binding edge.
   * @param useMidEdgePips Whether to draw small accent dots at mid-edge.
   * @param useHalo        Whether the page gets an outward halo glow.
   */
  public record Treatment(BorderStyle borderStyle, CornerOrnament cornerOrnament,
                          boolean useSpineRibbon, boolean useMidEdgePips, boolean useHalo) {
  }

  // Rank treatments are intentionally monotone in visual weight: each
  // higher rank inherits everything the previous rank had and only ADDS
  // ornamentation. This makes the on-page "rarity read" predictable and
  // lets `procedural_symbol_rank_progression` assert strictly increasing
  // non-transparent pixel counts.
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
   * Returns the treatment for the given card rank. Null / unknown ranks
   * fall back to the rank-1 plain treatment so generation never fails.
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
   * Draws the rank-appropriate frame around the rectangle {@code (x0, y0, w, h)}
   * inside {@code image}. Pixels outside the rectangle (halo etc.) are
   * clipped to image bounds.
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
        drawRectOutline(image, x0 + 2, y0 + 2, w - 4, h - 4, inkArgb);
      }
      case SINGLE_WITH_CORNERS -> {
        drawRectOutline(image, x0, y0, w, h, inkArgb);
        drawCornerNotch(image, x0, y0, w, h, accentArgb, 4);
      }
      case DOUBLE_WITH_ORNAMENTS -> {
        drawRectOutline(image, x0, y0, w, h, inkArgb);
        drawRectOutline(image, x0 + 2, y0 + 2, w - 4, h - 4, inkArgb);
        drawCornerNotch(image, x0, y0, w, h, accentArgb, 5);
      }
    }

    switch (treatment.cornerOrnament()) {
      case NONE -> {
      }
      case DOTS -> drawCornerDots(image, x0, y0, w, h, accentArgb, 1);
      case PIPS -> drawCornerDots(image, x0, y0, w, h, accentArgb, 2);
      case FILIGREE_3_STROKE -> drawCornerFiligree(image, x0, y0, w, h, accentArgb, 3);
      case FILIGREE_5_STROKE_PIP -> {
        drawCornerFiligree(image, x0, y0, w, h, accentArgb, 5);
        drawCornerDots(image, x0, y0, w, h, accentArgb, 2);
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

  // -------------- low-level helpers ----------------------------------------

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
        {x, y, 1, 1},                       // top-left
        {x + w - 1, y, -1, 1},              // top-right
        {x, y + h - 1, 1, -1},              // bottom-left
        {x + w - 1, y + h - 1, -1, -1}      // bottom-right
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

  private static void drawCornerDots(NativeImage image, int x, int y, int w, int h, int argb, int radius) {
    int[][] corners = {
        {x + 2, y + 2},
        {x + w - 3, y + 2},
        {x + 2, y + h - 3},
        {x + w - 3, y + h - 3}
    };
    for (int[] c : corners) {
      fillDisc(image, c[0], c[1], radius, argb);
    }
  }

  private static void drawCornerFiligree(NativeImage image, int x, int y, int w, int h, int argb, int strokeCount) {
    int abgr = toAbgr(argb);
    int[][] corners = {
        {x + 2, y + 2, 1, 1},
        {x + w - 3, y + 2, -1, 1},
        {x + 2, y + h - 3, 1, -1},
        {x + w - 3, y + h - 3, -1, -1}
    };
    for (int[] corner : corners) {
      int cx = corner[0];
      int cy = corner[1];
      int dx = corner[2];
      int dy = corner[3];
      for (int s = 0; s < strokeCount; s++) {
        // Three short tendrils emanating from the corner along x, y and the diagonal.
        for (int t = 0; t <= s; t++) {
          safeSet(image, cx + dx * (1 + t), cy + dy * 0, abgr);
          safeSet(image, cx + dx * 0, cy + dy * (1 + t), abgr);
          safeSet(image, cx + dx * (1 + t), cy + dy * (1 + t), abgr);
        }
      }
    }
  }

  private static void drawSpineRibbon(NativeImage image, int x, int y, int w, int h, int argb) {
    int abgr = toAbgr(argb);
    int spineX = x + 1;
    int top = y + h / 6;
    int bottom = y + h - h / 6;
    for (int sy = top; sy <= bottom; sy++) {
      safeSet(image, spineX, sy, abgr);
      safeSet(image, spineX + 1, sy, abgr);
    }
    // Forked tail at bottom.
    int tailX = spineX;
    int tailY = bottom;
    safeSet(image, tailX - 1, tailY + 1, abgr);
    safeSet(image, tailX + 2, tailY + 1, abgr);
  }

  private static void drawMidEdgePips(NativeImage image, int x, int y, int w, int h, int argb) {
    int midX = x + w / 2;
    int midY = y + h / 2;
    fillDisc(image, midX, y + 1, 1, argb);
    fillDisc(image, midX, y + h - 2, 1, argb);
    fillDisc(image, x + 1, midY, 1, argb);
    fillDisc(image, x + w - 2, midY, 1, argb);
  }

  /**
   * Draws a soft 4-pixel halo around the rectangle by writing increasingly
   * transparent halo color values. Skipped if the image isn't large enough
   * to hold the outward bleed.
   */
  private static void drawSoftHalo(NativeImage image, int x, int y, int w, int h, int rgb) {
    int[] alphas = {0x40, 0x30, 0x20, 0x10};
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

  private static void fillDisc(NativeImage image, int cx, int cy, int radius, int argb) {
    int abgr = toAbgr(argb);
    int rSq = radius * radius;
    for (int dy = -radius; dy <= radius; dy++) {
      for (int dx = -radius; dx <= radius; dx++) {
        if (dx * dx + dy * dy <= rSq) {
          safeSet(image, cx + dx, cy + dy, abgr);
        }
      }
    }
  }

  private static void safeSet(NativeImage image, int x, int y, int abgr) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
    image.setPixelRGBA(x, y, abgr);
  }

  /**
   * Converts an ARGB int (Java-friendly) into the ABGR layout NativeImage
   * stores internally on little-endian systems.
   */
  private static int toAbgr(int argb) {
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    return (a << 24) | (b << 16) | (g << 8) | r;
  }
}
