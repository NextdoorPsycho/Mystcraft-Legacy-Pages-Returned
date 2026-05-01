package art.arcane.mystcraft.client.gui.procedural.symbol;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;

/**
 * Pure drawing primitives used by {@code SymbolGlyphFactory} (phase 1) to
 * compose D'ni-style glyph tiles directly into a {@link NativeImage}.
 * <p>
 * Each primitive is a static, side-effect-free function (apart from writes to
 * the supplied image) and takes a deterministic {@code seed} for sub-pixel
 * jitter, so the same call always produces the same pixels.
 * <p>
 * All primitives accept colors as ARGB ints; alpha is honoured during blends.
 * The internal pixel layout is ABGR (NativeImage's little-endian convention)
 * and the conversion is handled here so callers always think in ARGB.
 *
 * <h3>Two render paths</h3>
 * <ol>
 *   <li><b>Aliased fast path</b> ({@link #drawLine}, {@link #fillDisc})
 *       — exact-pixel writes via {@link #safeSet}. Used by callers that
 *       want crisp 1-pixel strokes (parchment ruling lines, rectangle
 *       outlines, single-pixel dots in dot clusters).</li>
 *   <li><b>Anti-aliased thick path</b> ({@link #drawAaLine},
 *       {@link #drawAaArc}, {@link #fillSmoothDisc}) — distance-based
 *       coverage with alpha blending. Used by every visible glyph
 *       primitive ({@link #drawArc}, {@link #drawSpiral},
 *       {@link #drawNotchedRing}, {@link #drawCrossedBars},
 *       {@link #drawHatching}, {@link #drawRunicStroke},
 *       {@link #drawCrown}). Result: smooth edges + multi-pixel stroke
 *       weight without the pixelated diagonals of pure Bresenham.</li>
 * </ol>
 *
 * <p>The AA primitives compute distance-from-curve and convert it to
 * an alpha coverage value, blending into the destination via a stored
 * Porter-Duff "src over dst". This is roughly 4-8× slower than the
 * aliased path but only runs during the cache-miss render — once a
 * glyph is in the LRU, the cost is amortised to zero.
 *
 * <h3>Stroke thickness</h3>
 * The AA primitives all take a {@code thickness} parameter (in pixels).
 * Anything &lt; 1 collapses to a single sub-pixel line; values 1-3
 * produce the crisp inked-stroke aesthetic; values &gt; 3 read as a
 * brush-like wedge. Glyph primitives default to {@code 2.0} after the
 * resolution bump (was effectively {@code 1.0} at the legacy size).
 */
public final class SymbolGlyphPrimitives {

  private static final double DEFAULT_THICKNESS = 2.0;

  private SymbolGlyphPrimitives() {
  }

  /**
   * Draws an arc segment from {@code startDeg} (clockwise sweep, 0° = east)
   * spanning {@code sweepDeg}. Anti-aliased; the stroke is
   * {@link #DEFAULT_THICKNESS} pixels wide.
   */
  public static void drawArc(@NotNull NativeImage image, int cx, int cy, int radius,
                             float startDeg, float sweepDeg, int color, int seed) {
    drawAaArc(image, cx, cy, radius, startDeg, sweepDeg, DEFAULT_THICKNESS, color);
  }

  /**
   * Fermat spiral: r = c·sqrt(theta). Draws ~1.5 turns, growing from the centre
   * outward to {@code radius}. {@code seed} controls the rotation offset.
   * Anti-aliased + {@link #DEFAULT_THICKNESS} pixels thick.
   */
  public static void drawSpiral(@NotNull NativeImage image, int cx, int cy, int radius,
                                int color, int seed) {
    if (radius <= 0) return;
    double phaseOffset = (seed & 0xFFFF) / 65535.0 * (Math.PI * 2);
    int steps = Math.max(120, radius * 16);
    double maxTheta = Math.PI * 3.0;
    double c = radius / Math.sqrt(maxTheta);
    double half = DEFAULT_THICKNESS / 2.0;
    for (int i = 0; i < steps; i++) {
      double theta = (i / (double) steps) * maxTheta;
      double r = c * Math.sqrt(theta);
      double x = cx + Math.cos(theta + phaseOffset) * r;
      double y = cy - Math.sin(theta + phaseOffset) * r;
      fillSmoothDisc(image, x, y, half, color);
    }
  }

  /**
   * Places {@code count} small filled discs (radius 1.5) inside a ring of
   * {@code radius} centred at {@code (cx, cy)}. Disc positions are spread by an
   * irrational angle so neighbouring counts don't visually collapse. Each dot
   * is anti-aliased.
   */
  public static void drawDotCluster(@NotNull NativeImage image, int cx, int cy, int radius,
                                    int count, int color, int seed) {
    if (radius <= 0 || count <= 0) return;
    double goldenAngle = Math.PI * (3 - Math.sqrt(5));
    double phase = ((seed >>> 8) & 0xFFFF) / 65535.0 * (Math.PI * 2);

    double dotRadius = Math.max(1.2, radius / 12.0);
    for (int i = 0; i < count; i++) {
      double theta = phase + i * goldenAngle;
      double r = radius * Math.sqrt((i + 0.5) / count);
      double x = cx + Math.cos(theta) * r;
      double y = cy - Math.sin(theta) * r;
      fillSmoothDisc(image, x, y, dotRadius, color);
    }
  }

  /**
   * Circle with {@code notches} short outward strokes evenly spaced. Each notch
   * points outward from the ring centre. Anti-aliased ring + thick notches;
   * useful as a "ringed" base for biome / weather / lighting glyphs.
   */
  public static void drawNotchedRing(@NotNull NativeImage image, int cx, int cy, int radius,
                                     int notches, int color, int seed) {
    if (radius <= 0) return;
    drawAaArc(image, cx, cy, radius, 0, 360, DEFAULT_THICKNESS, color);
    if (notches <= 0) return;
    double phase = ((seed >>> 16) & 0xFFFF) / 65535.0 * (Math.PI * 2);
    double notchOuterR = radius + Math.max(2.0, radius * 0.18);
    for (int i = 0; i < notches; i++) {
      double theta = phase + i * (Math.PI * 2 / notches);
      double cosT = Math.cos(theta);
      double sinT = Math.sin(theta);
      double x0 = cx + cosT * radius;
      double y0 = cy - sinT * radius;
      double x1 = cx + cosT * notchOuterR;
      double y1 = cy - sinT * notchOuterR;
      drawAaLine(image, x0, y0, x1, y1, DEFAULT_THICKNESS, color);
    }
  }

  /**
   * Two crossed strokes through {@code (cx, cy)} forming an X (rotated by a
   * seed-derived angle). Anti-aliased + thick. Optional cap dots are drawn at
   * the four endpoints when {@code capDots} is true.
   */
  public static void drawCrossedBars(@NotNull NativeImage image, int cx, int cy, int radius,
                                     boolean capDots, int color, int seed) {
    if (radius <= 0) return;
    double rotation = (seed & 0xFFFF) / 65535.0 * (Math.PI * 0.5);
    drawAaSegment(image, cx, cy, rotation, radius, color);
    drawAaSegment(image, cx, cy, rotation + Math.PI * 0.5, radius, color);
    if (capDots) {
      double capRadius = Math.max(1.5, DEFAULT_THICKNESS);
      addAaCapDot(image, cx, cy, rotation, radius, capRadius, color);
      addAaCapDot(image, cx, cy, rotation + Math.PI, radius, capRadius, color);
      addAaCapDot(image, cx, cy, rotation + Math.PI * 0.5, radius, capRadius, color);
      addAaCapDot(image, cx, cy, rotation + Math.PI * 1.5, radius, capRadius, color);
    }
  }

  /**
   * Short parallel hatching strokes inside a wedge centred on {@code (cx, cy)}.
   * {@code count} controls how many strokes; {@code radius} is the wedge
   * length. Anti-aliased + thick. Hatching direction is seed-rotated.
   */
  public static void drawHatching(@NotNull NativeImage image, int cx, int cy, int radius,
                                  int count, int color, int seed) {
    if (radius <= 0 || count <= 0) return;
    double rotation = (seed & 0x3FF) / 1024.0 * (Math.PI * 2);
    double cos = Math.cos(rotation);
    double sin = Math.sin(rotation);
    double spacing = Math.max(2.0, radius / (double) count);
    for (int i = 0; i < count; i++) {
      double offset = (i - (count - 1) / 2.0) * spacing;
      double startX = cx + cos * offset - sin * (radius * 0.4);
      double startY = cy + sin * offset + cos * (radius * 0.4);
      double endX = cx + cos * offset + sin * (radius * 0.4);
      double endY = cy + sin * offset - cos * (radius * 0.4);
      drawAaLine(image, startX, startY, endX, endY, DEFAULT_THICKNESS, color);
    }
  }

  /**
   * Angled bar with two outward notches (a D'ni-style runic stroke). Notches
   * sit at 1/3 and 2/3 along the stroke and project perpendicular to it.
   * Anti-aliased + thick (slightly heavier weight than other primitives so
   * runic strokes read as the dominant glyph element).
   */
  public static void drawRunicStroke(@NotNull NativeImage image, int cx, int cy, int radius,
                                     int color, int seed) {
    if (radius <= 0) return;
    double rotation = ((seed >>> 4) & 0xFFFF) / 65535.0 * (Math.PI * 2);
    double cos = Math.cos(rotation);
    double sin = Math.sin(rotation);

    double mainThickness = DEFAULT_THICKNESS + 0.5;
    double x0 = cx - cos * radius;
    double y0 = cy - sin * radius;
    double x1 = cx + cos * radius;
    double y1 = cy + sin * radius;
    drawAaLine(image, x0, y0, x1, y1, mainThickness, color);

    double notchLen = Math.max(1.5, radius * 0.35);
    for (double t : new double[]{-radius * 0.33, radius * 0.33}) {
      double bx = cx + cos * t;
      double by = cy + sin * t;
      double nx = bx + (-sin) * notchLen;
      double ny = by + cos * notchLen;
      drawAaLine(image, bx, by, nx, ny, DEFAULT_THICKNESS, color);
    }
  }

  /**
   * Chevron with three short vertical strokes ("crown" motif). Used by curated
   * structural words like {@code "stability"} and {@code "order"}. Anti-aliased
   * + thick.
   */
  public static void drawCrown(@NotNull NativeImage image, int cx, int cy, int radius,
                               int color, int seed) {
    if (radius <= 0) return;
    int span = radius;

    drawAaLine(image, cx - span, cy, cx, cy - radius / 2.0, DEFAULT_THICKNESS, color);
    drawAaLine(image, cx, cy - radius / 2.0, cx + span, cy, DEFAULT_THICKNESS, color);

    int strokeHeight = Math.max(2, radius / 2);
    double phase = (seed & 0xFF) / 255.0 * 2 - 1;
    double leftX = cx - span * (0.55 + phase * 0.05);
    double rightX = cx + span * (0.55 + phase * 0.05);
    drawAaLine(image, leftX, cy - radius / 4.0, leftX, cy - radius / 4.0 - strokeHeight,
        DEFAULT_THICKNESS, color);
    drawAaLine(image, cx, cy - radius / 2.0, cx, cy - radius / 2.0 - strokeHeight,
        DEFAULT_THICKNESS, color);
    drawAaLine(image, rightX, cy - radius / 4.0, rightX, cy - radius / 4.0 - strokeHeight,
        DEFAULT_THICKNESS, color);
  }

  /**
   * Anti-aliased disc with smooth edges. Pixels inside {@code radius - 0.5} are
   * full coverage; pixels in the outer half-pixel rim get fractional coverage
   * proportional to how much of their area lies inside the disc. Result: no
   * jagged "stair-step" silhouette at any radius from 0.5 upwards.
   *
   * <p>Use this for any circle that's part of the glyph silhouette.
   * The aliased {@link #fillDisc} is reserved for crisp single-pixel dots where
   * AA bleed is undesirable.
   */
  public static void fillSmoothDisc(@NotNull NativeImage image, double cx, double cy,
                                    double radius, int argb) {
    if (radius <= 0) return;
    double innerRSq = (radius - 0.5) * (radius - 0.5);
    double outerR = radius + 0.5;
    double outerRSq = outerR * outerR;
    int pad = (int) Math.ceil(outerR);
    int xc = (int) Math.round(cx);
    int yc = (int) Math.round(cy);
    int minX = xc - pad;
    int maxX = xc + pad;
    int minY = yc - pad;
    int maxY = yc + pad;
    int w = image.getWidth();
    int h = image.getHeight();
    for (int py = minY; py <= maxY; py++) {
      if (py < 0 || py >= h) continue;
      double dy = py - cy;
      for (int px = minX; px <= maxX; px++) {
        if (px < 0 || px >= w) continue;
        double dx = px - cx;
        double distSq = dx * dx + dy * dy;
        if (distSq >= outerRSq) continue;
        double alpha;
        if (distSq <= innerRSq) {
          alpha = 1.0;
        } else {
          alpha = outerR - Math.sqrt(distSq);
          if (alpha <= 0) continue;
          if (alpha > 1) alpha = 1;
        }
        blendPixel(image, px, py, argb, alpha);
      }
    }
  }

  /**
   * Anti-aliased thick line from {@code (x0,y0)} to {@code (x1,y1)}. Pixels
   * within {@code thickness/2} of the segment get full coverage; pixels within
   * an extra half-pixel rim get fractional coverage.
   *
   * <p>Performance: O(bbox-area). For a typical glyph stroke (50 px
   * long, 2 px thick) that's roughly 51×3 = 150 pixel evaluations.
   */
  public static void drawAaLine(@NotNull NativeImage image, double x0, double y0,
                                double x1, double y1, double thickness, int argb) {
    if (thickness <= 0) return;
    double half = thickness / 2.0;
    double pad = 1.0;

    double dx = x1 - x0;
    double dy = y1 - y0;
    double lenSq = dx * dx + dy * dy;

    if (lenSq < 1e-9) {
      fillSmoothDisc(image, x0, y0, half, argb);
      return;
    }

    double envHalf = half + pad;
    int minX = (int) Math.floor(Math.min(x0, x1) - envHalf);
    int maxX = (int) Math.ceil(Math.max(x0, x1) + envHalf);
    int minY = (int) Math.floor(Math.min(y0, y1) - envHalf);
    int maxY = (int) Math.ceil(Math.max(y0, y1) + envHalf);
    int w = image.getWidth();
    int h = image.getHeight();

    double envHalfSq = envHalf * envHalf;
    double halfSq = half * half;

    for (int py = minY; py <= maxY; py++) {
      if (py < 0 || py >= h) continue;
      double pyD = py;
      for (int px = minX; px <= maxX; px++) {
        if (px < 0 || px >= w) continue;
        double pxD = px;

        double t = ((pxD - x0) * dx + (pyD - y0) * dy) / lenSq;
        if (t < 0) t = 0;
        else if (t > 1) t = 1;
        double cxClosest = x0 + t * dx;
        double cyClosest = y0 + t * dy;
        double rx = pxD - cxClosest;
        double ry = pyD - cyClosest;
        double distSq = rx * rx + ry * ry;
        if (distSq >= envHalfSq) continue;

        double alpha;
        if (distSq <= halfSq) {
          alpha = 1.0;
        } else {
          alpha = envHalf - Math.sqrt(distSq);
          if (alpha <= 0) continue;
          if (alpha > 1) alpha = 1;
        }
        blendPixel(image, px, py, argb, alpha);
      }
    }
  }

  /**
   * Anti-aliased thick arc segment. Pixels whose distance from the circle
   * radius is within {@code thickness/2} get full coverage and an extra
   * half-pixel rim gets fractional coverage. Sweep is clockwise from
   * {@code startDeg}; 0° = east, positive = north.
   *
   * <p>Performance: O(bbox-area). A 30-px-radius arc has ~62² = 3844
   * pixel evaluations. The angular check (atan2 + range test) means pixels
   * outside the sweep skip cheaply.
   */
  public static void drawAaArc(@NotNull NativeImage image, double cx, double cy, double radius,
                               double startDeg, double sweepDeg, double thickness, int argb) {
    if (radius <= 0 || thickness <= 0) return;
    double half = thickness / 2.0;
    double pad = 1.0;
    double envHalf = half + pad;
    double innerR = Math.max(0, radius - envHalf);
    double outerR = radius + envHalf;
    double innerRSq = innerR * innerR;
    double outerRSq = outerR * outerR;
    double bandHalfSq = half * half;
    double bandEnvSq = envHalf * envHalf;

    int pad2 = (int) Math.ceil(outerR);
    int xc = (int) Math.round(cx);
    int yc = (int) Math.round(cy);
    int minX = xc - pad2;
    int maxX = xc + pad2;
    int minY = yc - pad2;
    int maxY = yc + pad2;
    int w = image.getWidth();
    int h = image.getHeight();

    double startRad = Math.toRadians(startDeg);
    double endRad = startRad + Math.toRadians(sweepDeg);
    boolean fullCircle = Math.abs(sweepDeg) >= 360.0 - 0.001;
    double minSweep = Math.min(startRad, endRad);
    double maxSweep = Math.max(startRad, endRad);

    for (int py = minY; py <= maxY; py++) {
      if (py < 0 || py >= h) continue;
      double dy = py - cy;
      for (int px = minX; px <= maxX; px++) {
        if (px < 0 || px >= w) continue;
        double dx = px - cx;
        double distSq = dx * dx + dy * dy;
        if (distSq < innerRSq || distSq > outerRSq) continue;
        if (!fullCircle) {

          double pixelAngle = Math.atan2(-dy, dx);

          while (pixelAngle < minSweep - Math.PI) pixelAngle += Math.PI * 2;
          while (pixelAngle > maxSweep + Math.PI) pixelAngle -= Math.PI * 2;
          if (pixelAngle < minSweep || pixelAngle > maxSweep) continue;
        }
        double bandDist = Math.abs(Math.sqrt(distSq) - radius);
        if (bandDist > envHalf) continue;
        double alpha;
        if (bandDist * bandDist <= bandHalfSq) {
          alpha = 1.0;
        } else {
          alpha = envHalf - bandDist;
          if (alpha <= 0) continue;
          if (alpha > 1) alpha = 1;
        }
        blendPixel(image, px, py, argb, alpha);
      }
    }
  }

  /**
   * Fills the interior of a closed polygon defined by paired
   * {@code (xs[i], ys[i])} vertices using the even-odd fill rule. Pixels
   * strictly inside the polygon get full coverage; the single-pixel rim around
   * each edge gets fractional alpha so the filled silhouette reads cleanly on
   * top of any background.
   *
   * <p>Used by {@code SymbolMotif.STAR} to fill the star body so the
   * shape is solid rather than just a thin outline. Performance: O(scanlines ×
   * vertices) — for a 200-px-tall star with 10 vertices that's ~2000 vertex
   * checks, negligible inside the cache-miss render path.
   */
  public static void fillPolygon(@NotNull NativeImage image, @NotNull double[] xs,
                                 @NotNull double[] ys, int argb) {
    if (xs.length < 3 || xs.length != ys.length) return;
    int destW = image.getWidth();
    int destH = image.getHeight();

    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (double y : ys) {
      if (y < minY) minY = y;
      if (y > maxY) maxY = y;
    }
    int yStart = Math.max(0, (int) Math.floor(minY));
    int yEnd = Math.min(destH - 1, (int) Math.ceil(maxY));
    if (yStart > yEnd) return;

    java.util.ArrayList<Double> hits = new java.util.ArrayList<>(xs.length);
    for (int y = yStart; y <= yEnd; y++) {

      double scanY = y + 0.5;
      hits.clear();
      for (int i = 0; i < xs.length; i++) {
        int j = (i + 1) % xs.length;
        double y1 = ys[i];
        double y2 = ys[j];
        if (y1 == y2) continue;
        double yLo = Math.min(y1, y2);
        double yHi = Math.max(y1, y2);

        if (scanY < yLo || scanY >= yHi) continue;
        double t = (scanY - y1) / (y2 - y1);
        double xi = xs[i] + t * (xs[j] - xs[i]);
        hits.add(xi);
      }
      java.util.Collections.sort(hits);

      for (int i = 0; i + 1 < hits.size(); i += 2) {
        double x0 = hits.get(i);
        double x1 = hits.get(i + 1);

        int xLo = (int) Math.ceil(x0);
        int xHi = (int) Math.floor(x1);
        if (xLo > 0 && xLo - 1 < destW) {
          double leftCov = xLo - x0;
          if (leftCov > 0) blendPixel(image, xLo - 1, y, argb, leftCov);
        }
        for (int x = Math.max(0, xLo); x <= Math.min(destW - 1, xHi); x++) {
          blendPixel(image, x, y, argb, 1.0);
        }
        if (xHi + 1 < destW && xHi + 1 >= 0) {
          double rightCov = x1 - xHi;
          if (rightCov > 0) blendPixel(image, xHi + 1, y, argb, rightCov);
        }
      }
    }
  }

  /**
   * Filled disc — aliased, hard-edged variant. Use this when you need an exact
   * pixel-count footprint (e.g. tests that count non-transparent pixels, or
   * 1-pixel "ink dots"). For any visible glyph circle, prefer
   * {@link #fillSmoothDisc}.
   */
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

  /**
   * Bresenham line — aliased. Used by callers that need a crisp single-pixel
   * stroke (motif horizon line, rule lines, rect outlines). For any thick or
   * visible stroke, prefer {@link #drawAaLine}.
   */
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

  private static void drawAaSegment(NativeImage image, int cx, int cy, double angleRad,
                                    int radius, int color) {
    double cos = Math.cos(angleRad);
    double sin = Math.sin(angleRad);
    double x0 = cx - cos * radius;
    double y0 = cy - sin * radius;
    double x1 = cx + cos * radius;
    double y1 = cy + sin * radius;
    drawAaLine(image, x0, y0, x1, y1, DEFAULT_THICKNESS, color);
  }

  private static void addAaCapDot(NativeImage image, int cx, int cy, double angleRad,
                                  int radius, double dotRadius, int color) {
    double x = cx + Math.cos(angleRad) * radius;
    double y = cy + Math.sin(angleRad) * radius;
    fillSmoothDisc(image, x, y, dotRadius, color);
  }

  private static void blendPixel(@NotNull NativeImage image, int x, int y, int argb, double alpha) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
      return;
    if (alpha <= 0) return;
    if (alpha > 1) alpha = 1;

    int srcA = (argb >>> 24) & 0xFF;
    int srcR = (argb >>> 16) & 0xFF;
    int srcG = (argb >>> 8) & 0xFF;
    int srcB = argb & 0xFF;

    int finalA = (int) Math.round(srcA * alpha);
    if (finalA <= 0) return;
    if (finalA > 0xFF) finalA = 0xFF;

    int dstAbgr = image.getPixelRGBA(x, y);
    int dstA = (dstAbgr >>> 24) & 0xFF;
    int dstB = (dstAbgr >>> 16) & 0xFF;
    int dstG = (dstAbgr >>> 8) & 0xFF;
    int dstR = dstAbgr & 0xFF;

    int outA = finalA + (dstA * (0xFF - finalA)) / 0xFF;
    if (outA <= 0) return;

    int outR = (srcR * finalA + (dstR * dstA * (0xFF - finalA)) / 0xFF) / outA;
    int outG = (srcG * finalA + (dstG * dstA * (0xFF - finalA)) / 0xFF) / outA;
    int outB = (srcB * finalA + (dstB * dstA * (0xFF - finalA)) / 0xFF) / outA;

    int outAbgr = (outA << 24) | (outB << 16) | (outG << 8) | outR;
    image.setPixelRGBA(x, y, outAbgr);
  }

  private static void safeSet(NativeImage image, int x, int y, int abgr) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
      return;
    image.setPixelRGBA(x, y, abgr);
  }

  /**
   * Public ARGB→ABGR converter — exposed so motif drawers can pre-convert
   * once.
   */
  public static int toAbgr(int argb) {
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    return (a << 24) | (b << 16) | (g << 8) | r;
  }
}
