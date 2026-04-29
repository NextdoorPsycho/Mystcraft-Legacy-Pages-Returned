package art.arcane.mystcraft.client.gui.procedural.symbol;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;

/**
 * Pure drawing primitives used by {@code SymbolGlyphFactory} (phase 1) to
 * compose D'ni-style glyph tiles directly into a {@link NativeImage}.
 * <p>
 * Each primitive is a static, side-effect-free function (apart from
 * writes to the supplied image) and takes a deterministic {@code seed}
 * for sub-pixel jitter, so the same call always produces the same pixels.
 * <p>
 * All primitives accept colors as ARGB ints; alpha is honoured during
 * blends. The internal pixel layout is ABGR (NativeImage's
 * little-endian convention) and the conversion is handled here so callers
 * always think in ARGB.
 *
 * <h3>Sample ASCII outputs (16×16 tiles, dot per coloured pixel)</h3>
 * <pre>
 *   drawArc(cx=8, cy=8, r=6, start=0, sweep=180):
 *     . . . # # # # # # # # # # . . .
 *
 *   drawSpiral(cx=8, cy=8, r=6):
 *     . . . . # . . . . . # . . . . .
 *     . . . # . # # . # # . # . . . .
 *
 *   drawDotCluster(cx=8, cy=8, r=6, count=5):
 *     . . . . . . # . . . . . . . . .
 *     . . . . . . . . # . . . . . . .
 *     . . # . . . . . . . . . . . . .
 *
 *   drawNotchedRing(cx=8, cy=8, r=6, notches=3):
 *     a circle with three short outward notches
 * </pre>
 *
 * Pixel positions are integer-rounded; antialiasing is intentionally
 * coarse so the result reads clearly at the 32-px in-game scale.
 */
public final class SymbolGlyphPrimitives {

  private SymbolGlyphPrimitives() {
  }

  /**
   * Draws an arc segment from {@code startDeg} (clockwise sweep, 0° = east)
   * spanning {@code sweepDeg}. Implemented as 1° subdivisions; the result
   * is a 1-pixel-thick polyline.
   */
  public static void drawArc(@NotNull NativeImage image, int cx, int cy, int radius,
                             float startDeg, float sweepDeg, int color, @SuppressWarnings("unused") int seed) {
    if (radius <= 0) return;
    int abgr = toAbgr(color);
    int steps = Math.max(1, Math.round(Math.abs(sweepDeg)));
    for (int i = 0; i <= steps; i++) {
      double t = startDeg + sweepDeg * (i / (double) steps);
      double rad = Math.toRadians(t);
      int x = cx + (int) Math.round(Math.cos(rad) * radius);
      int y = cy - (int) Math.round(Math.sin(rad) * radius);
      safeSet(image, x, y, abgr);
    }
  }

  /**
   * Fermat spiral: r = c·sqrt(theta). Draws ~1.5 turns, growing from the
   * centre outward to {@code radius}. {@code seed} controls the rotation
   * offset so two spirals with different seeds wind from different
   * starting angles.
   */
  public static void drawSpiral(@NotNull NativeImage image, int cx, int cy, int radius,
                                int color, int seed) {
    if (radius <= 0) return;
    int abgr = toAbgr(color);
    double phaseOffset = (seed & 0xFFFF) / 65535.0 * (Math.PI * 2);
    int steps = Math.max(60, radius * 12);
    double maxTheta = Math.PI * 3.0; // 1.5 turns
    double c = radius / Math.sqrt(maxTheta);
    for (int i = 0; i < steps; i++) {
      double theta = (i / (double) steps) * maxTheta;
      double r = c * Math.sqrt(theta);
      int x = cx + (int) Math.round(Math.cos(theta + phaseOffset) * r);
      int y = cy - (int) Math.round(Math.sin(theta + phaseOffset) * r);
      safeSet(image, x, y, abgr);
    }
  }

  /**
   * Places {@code count} small filled discs (radius 1) inside a ring of
   * {@code radius} centred at {@code (cx, cy)}. Disc positions are spread
   * by an irrational angle so neighbouring counts don't visually collapse.
   */
  public static void drawDotCluster(@NotNull NativeImage image, int cx, int cy, int radius,
                                    int count, int color, int seed) {
    if (radius <= 0 || count <= 0) return;
    double goldenAngle = Math.PI * (3 - Math.sqrt(5)); // ~2.39996 rad
    double phase = ((seed >>> 8) & 0xFFFF) / 65535.0 * (Math.PI * 2);
    for (int i = 0; i < count; i++) {
      double theta = phase + i * goldenAngle;
      double r = radius * Math.sqrt((i + 0.5) / count);
      int x = cx + (int) Math.round(Math.cos(theta) * r);
      int y = cy - (int) Math.round(Math.sin(theta) * r);
      fillDisc(image, x, y, 1, color);
    }
  }

  /**
   * Circle with {@code notches} short outward strokes evenly spaced. Each
   * notch points outward from the ring centre. Useful as a "ringed" base
   * for biome / weather / lighting glyphs.
   */
  public static void drawNotchedRing(@NotNull NativeImage image, int cx, int cy, int radius,
                                     int notches, int color, int seed) {
    if (radius <= 0) return;
    drawArc(image, cx, cy, radius, 0, 360, color, seed);
    if (notches <= 0) return;
    int abgr = toAbgr(color);
    double phase = ((seed >>> 16) & 0xFFFF) / 65535.0 * (Math.PI * 2);
    for (int i = 0; i < notches; i++) {
      double theta = phase + i * (Math.PI * 2 / notches);
      for (int k = radius; k <= radius + 2; k++) {
        int x = cx + (int) Math.round(Math.cos(theta) * k);
        int y = cy - (int) Math.round(Math.sin(theta) * k);
        safeSet(image, x, y, abgr);
      }
    }
  }

  /**
   * Two crossed strokes through {@code (cx, cy)} forming an X (rotated by
   * a seed-derived angle). Optional cap dots are drawn at the four
   * endpoints when {@code capDots} is true.
   */
  public static void drawCrossedBars(@NotNull NativeImage image, int cx, int cy, int radius,
                                     boolean capDots, int color, int seed) {
    if (radius <= 0) return;
    double rotation = (seed & 0xFFFF) / 65535.0 * (Math.PI * 0.5);
    drawSegment(image, cx, cy, rotation, radius, color);
    drawSegment(image, cx, cy, rotation + Math.PI * 0.5, radius, color);
    if (capDots) {
      addCapDot(image, cx, cy, rotation, radius, color);
      addCapDot(image, cx, cy, rotation + Math.PI, radius, color);
      addCapDot(image, cx, cy, rotation + Math.PI * 0.5, radius, color);
      addCapDot(image, cx, cy, rotation + Math.PI * 1.5, radius, color);
    }
  }

  /**
   * Short parallel hatching strokes inside a wedge centred on
   * {@code (cx, cy)}. {@code count} controls how many strokes; {@code radius}
   * is the wedge length. Hatching direction is seed-rotated.
   */
  public static void drawHatching(@NotNull NativeImage image, int cx, int cy, int radius,
                                  int count, int color, int seed) {
    if (radius <= 0 || count <= 0) return;
    int abgr = toAbgr(color);
    double rotation = (seed & 0x3FF) / 1024.0 * (Math.PI * 2);
    double cos = Math.cos(rotation);
    double sin = Math.sin(rotation);
    int spacing = Math.max(1, radius / count);
    for (int i = 0; i < count; i++) {
      double offset = (i - (count - 1) / 2.0) * spacing;
      double startX = cx + cos * offset - sin * (radius * 0.4);
      double startY = cy + sin * offset + cos * (radius * 0.4);
      double endX = cx + cos * offset + sin * (radius * 0.4);
      double endY = cy + sin * offset - cos * (radius * 0.4);
      drawLine(image, (int) Math.round(startX), (int) Math.round(startY),
          (int) Math.round(endX), (int) Math.round(endY), abgr);
    }
  }

  /**
   * Angled bar with two outward notches (a D'ni-style runic stroke).
   * Notches sit at 1/3 and 2/3 along the stroke and project perpendicular
   * to it.
   */
  public static void drawRunicStroke(@NotNull NativeImage image, int cx, int cy, int radius,
                                     int color, int seed) {
    if (radius <= 0) return;
    int abgr = toAbgr(color);
    double rotation = ((seed >>> 4) & 0xFFFF) / 65535.0 * (Math.PI * 2);
    double cos = Math.cos(rotation);
    double sin = Math.sin(rotation);

    int x0 = cx - (int) Math.round(cos * radius);
    int y0 = cy - (int) Math.round(sin * radius);
    int x1 = cx + (int) Math.round(cos * radius);
    int y1 = cy + (int) Math.round(sin * radius);
    drawLine(image, x0, y0, x1, y1, abgr);

    // Two notches, perpendicular to the stroke.
    double notchLen = Math.max(1, radius * 0.35);
    for (double t : new double[] {-radius * 0.33, radius * 0.33}) {
      int bx = cx + (int) Math.round(cos * t);
      int by = cy + (int) Math.round(sin * t);
      int nx = bx + (int) Math.round(-sin * notchLen);
      int ny = by + (int) Math.round(cos * notchLen);
      drawLine(image, bx, by, nx, ny, abgr);
    }
  }

  /**
   * Chevron with three short vertical strokes ("crown" motif). Used by
   * curated structural words like {@code "stability"} and {@code "order"}.
   */
  public static void drawCrown(@NotNull NativeImage image, int cx, int cy, int radius,
                               int color, int seed) {
    if (radius <= 0) return;
    int abgr = toAbgr(color);
    int span = radius;

    // Chevron baseline.
    drawLine(image, cx - span, cy, cx, cy - radius / 2, abgr);
    drawLine(image, cx, cy - radius / 2, cx + span, cy, abgr);

    // Three vertical strokes (left, centre, right) rising from the chevron.
    int strokeHeight = Math.max(2, radius / 2);
    double phase = (seed & 0xFF) / 255.0 * 2 - 1;
    int leftX = cx - (int) Math.round(span * (0.55 + phase * 0.05));
    int rightX = cx + (int) Math.round(span * (0.55 + phase * 0.05));
    drawLine(image, leftX, cy - radius / 4, leftX, cy - radius / 4 - strokeHeight, abgr);
    drawLine(image, cx, cy - radius / 2, cx, cy - radius / 2 - strokeHeight, abgr);
    drawLine(image, rightX, cy - radius / 4, rightX, cy - radius / 4 - strokeHeight, abgr);
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------

  /** Filled disc — utility used by {@link #drawDotCluster} and motif drawers. */
  public static void fillDisc(@NotNull NativeImage image, int cx, int cy, int radius, int color) {
    int abgr = toAbgr(color);
    int rSq = radius * radius;
    for (int dy = -radius; dy <= radius; dy++) {
      for (int dx = -radius; dx <= radius; dx++) {
        if (dx * dx + dy * dy <= rSq) {
          safeSet(image, cx + dx, cy + dy, abgr);
        }
      }
    }
  }

  /** Bresenham line. */
  public static void drawLine(@NotNull NativeImage image, int x0, int y0, int x1, int y1, int abgr) {
    int dx = Math.abs(x1 - x0);
    int dy = -Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx + dy;
    int x = x0;
    int y = y0;
    while (true) {
      safeSet(image, x, y, abgr);
      if (x == x1 && y == y1) break;
      int e2 = err * 2;
      if (e2 >= dy) {
        if (x == x1) break;
        err += dy;
        x += sx;
      }
      if (e2 <= dx) {
        if (y == y1) break;
        err += dx;
        y += sy;
      }
    }
  }

  private static void drawSegment(NativeImage image, int cx, int cy, double angleRad, int radius, int color) {
    int abgr = toAbgr(color);
    int x0 = cx - (int) Math.round(Math.cos(angleRad) * radius);
    int y0 = cy - (int) Math.round(Math.sin(angleRad) * radius);
    int x1 = cx + (int) Math.round(Math.cos(angleRad) * radius);
    int y1 = cy + (int) Math.round(Math.sin(angleRad) * radius);
    drawLine(image, x0, y0, x1, y1, abgr);
  }

  private static void addCapDot(NativeImage image, int cx, int cy, double angleRad, int radius, int color) {
    int x = cx + (int) Math.round(Math.cos(angleRad) * radius);
    int y = cy + (int) Math.round(Math.sin(angleRad) * radius);
    fillDisc(image, x, y, 1, color);
  }

  private static void safeSet(NativeImage image, int x, int y, int abgr) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
    image.setPixelRGBA(x, y, abgr);
  }

  /** Public ARGB→ABGR converter — exposed so motif drawers can pre-convert once. */
  public static int toAbgr(int argb) {
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    return (a << 24) | (b << 16) | (g << 8) | r;
  }
}
