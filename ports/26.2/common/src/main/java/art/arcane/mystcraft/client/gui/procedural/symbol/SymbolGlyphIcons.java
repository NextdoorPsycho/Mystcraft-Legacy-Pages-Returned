package art.arcane.mystcraft.client.gui.procedural.symbol;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Hieroglyph-style icon library for the curated D'ni vocabulary.
 * <p>
 * Each entry maps a curated word (e.g. {@code "fire"}, {@code "mountain"}) to a
 * hand-designed shape that conveys the word's meaning even though the viewer
 * can't "read" it. Same principle as Egyptian hieroglyphs: a flame icon reads
 * as fire, a triangular peak reads as mountain, an eye reads as image.
 * <p>
 * Words not in this dictionary fall back to {@link SymbolGlyphFactory}'s
 * structured composition system, so this library is purely additive — unknown
 * words still render, they just look more abstract.
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>All icons are tuned for a 128&times;128 tile centred at (64, 64).</li>
 *   <li>Strokes use {@link SymbolGlyphPrimitives#drawAaLine} with thickness
 *       2-3 px so they read crisply at the page's final on-screen size.</li>
 *   <li>Filled regions use {@link SymbolGlyphPrimitives#fillSmoothDisc}.</li>
 *   <li>Each icon paints in <em>two colour roles</em>:
 *       <code>accent</code> for primary strokes, <code>ink</code> for
 *       darker accents/sparks. Pinned thematic colours (from
 *       {@link art.arcane.mystcraft.client.render.DrawableWordManager})
 *       arrive as <code>accent</code>, so a "fire" icon reads as
 *       red-orange flames automatically.</li>
 *   <li>Drawers are deterministic — same inputs always produce the same
 *       pixels, preserving the determinism contract enforced by the
 *       {@code procedural_ui_symbol_glyph_is_deterministic} test.</li>
 * </ul>
 */
public final class SymbolGlyphIcons {

  private static final Map<String, IconDrawer> ICONS = new HashMap<>();

  static {
    registerAll();
  }

  private SymbolGlyphIcons() {
  }

  /**
   * Attempts to draw an iconographic glyph for the given word. Returns
   * {@code true} when an icon was drawn, {@code false} when the word isn't in
   * the dictionary (the caller should fall back to procedural composition).
   */
  public static boolean tryDraw(@NotNull NativeImage image, @Nullable String word,
                                int accent, int ink, int seed) {
    if (word == null) return false;
    IconDrawer drawer = ICONS.get(word.toLowerCase(Locale.ROOT));
    if (drawer == null) return false;
    int cx = image.getWidth() / 2;
    int cy = image.getHeight() / 2;
    drawer.draw(image, cx, cy, accent, ink, seed);
    return true;
  }

  /**
   * Number of icons registered — used by tests to detect accidental drops.
   */
  public static int iconCount() {
    return ICONS.size();
  }

  private static void registerAll() {

    ICONS.put("fire", SymbolGlyphIcons::drawFire);
    ICONS.put("water", SymbolGlyphIcons::drawWater);
    ICONS.put("earth", SymbolGlyphIcons::drawEarth);
    ICONS.put("air", SymbolGlyphIcons::drawAir);
    ICONS.put("light", SymbolGlyphIcons::drawLight);
    ICONS.put("dark", SymbolGlyphIcons::drawDark);
    ICONS.put("sky", SymbolGlyphIcons::drawSky);
    ICONS.put("life", SymbolGlyphIcons::drawLife);
    ICONS.put("death", SymbolGlyphIcons::drawDeath);
    ICONS.put("growth", SymbolGlyphIcons::drawGrowth);
    ICONS.put("decay", SymbolGlyphIcons::drawDecay);

    ICONS.put("forest", SymbolGlyphIcons::drawForest);
    ICONS.put("desert", SymbolGlyphIcons::drawDesert);
    ICONS.put("ocean", SymbolGlyphIcons::drawOcean);
    ICONS.put("mountain", SymbolGlyphIcons::drawMountain);
    ICONS.put("plains", SymbolGlyphIcons::drawPlains);
    ICONS.put("swamp", SymbolGlyphIcons::drawSwamp);
    ICONS.put("jungle", SymbolGlyphIcons::drawJungle);
    ICONS.put("tundra", SymbolGlyphIcons::drawTundra);
    ICONS.put("cave", SymbolGlyphIcons::drawCave);

    ICONS.put("storm", SymbolGlyphIcons::drawStorm);
    ICONS.put("rain", SymbolGlyphIcons::drawRain);
    ICONS.put("snow", SymbolGlyphIcons::drawSnow);
    ICONS.put("wind", SymbolGlyphIcons::drawWind);
    ICONS.put("cloud", SymbolGlyphIcons::drawCloud);
    ICONS.put("lightning", SymbolGlyphIcons::drawLightning);
    ICONS.put("thunder", SymbolGlyphIcons::drawThunder);

    ICONS.put("village", SymbolGlyphIcons::drawVillage);
    ICONS.put("ruin", SymbolGlyphIcons::drawRuin);
    ICONS.put("portal", SymbolGlyphIcons::drawPortal);
    ICONS.put("tower", SymbolGlyphIcons::drawTower);
    ICONS.put("dungeon", SymbolGlyphIcons::drawDungeon);
    ICONS.put("library", SymbolGlyphIcons::drawLibrary);

    ICONS.put("image", SymbolGlyphIcons::drawImage);
    ICONS.put("stimulate", SymbolGlyphIcons::drawStimulate);
    ICONS.put("reflect", SymbolGlyphIcons::drawReflect);
    ICONS.put("radiate", SymbolGlyphIcons::drawRadiate);

    ICONS.put("large", SymbolGlyphIcons::drawLarge);
    ICONS.put("small", SymbolGlyphIcons::drawSmall);
    ICONS.put("dense", SymbolGlyphIcons::drawDense);
    ICONS.put("sparse", SymbolGlyphIcons::drawSparse);
    ICONS.put("bright", SymbolGlyphIcons::drawBright);
    ICONS.put("dim", SymbolGlyphIcons::drawDim);
    ICONS.put("fast", SymbolGlyphIcons::drawFast);
    ICONS.put("slow", SymbolGlyphIcons::drawSlow);
    ICONS.put("normal", SymbolGlyphIcons::drawNormal);
    ICONS.put("extreme", SymbolGlyphIcons::drawExtreme);

    ICONS.put("system", SymbolGlyphIcons::drawSystem);
    ICONS.put("motion", SymbolGlyphIcons::drawMotion);
    ICONS.put("cycle", SymbolGlyphIcons::drawCycle);
    ICONS.put("form", SymbolGlyphIcons::drawForm);
    ICONS.put("force", SymbolGlyphIcons::drawForce);
    ICONS.put("change", SymbolGlyphIcons::drawChange);
    ICONS.put("flow", SymbolGlyphIcons::drawFlow);
    ICONS.put("order", SymbolGlyphIcons::drawOrder);
    ICONS.put("chaos", SymbolGlyphIcons::drawChaos);
    ICONS.put("time", SymbolGlyphIcons::drawTime);
    ICONS.put("space", SymbolGlyphIcons::drawSpace);
    ICONS.put("energy", SymbolGlyphIcons::drawEnergy);
    ICONS.put("matter", SymbolGlyphIcons::drawMatter);
    ICONS.put("void", SymbolGlyphIcons::drawVoid);

    ICONS.put("random", SymbolGlyphIcons::drawRandom);
    ICONS.put("gradient", SymbolGlyphIcons::drawGradient);
    ICONS.put("standard", SymbolGlyphIcons::drawStandard);
    ICONS.put("special", SymbolGlyphIcons::drawSpecial);
    ICONS.put("instability", SymbolGlyphIcons::drawInstability);
    ICONS.put("stability", SymbolGlyphIcons::drawStability);

    ICONS.put("biome", SymbolGlyphIcons::drawBiome);
    ICONS.put("weather", SymbolGlyphIcons::drawWeather);
    ICONS.put("terrain", SymbolGlyphIcons::drawTerrain);
    ICONS.put("color", SymbolGlyphIcons::drawColor);

    ICONS.put("red", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("blue", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("green", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("yellow", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("orange", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("purple", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("white", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("black", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("gray", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("brown", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("pink", SymbolGlyphIcons::drawColorSwatch);
    ICONS.put("cyan", SymbolGlyphIcons::drawColorSwatch);

    ICONS.put("easter", SymbolGlyphIcons::drawEaster);
  }

  private static void drawFire(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx + 4, cy + 8, 38, 90, 70, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx - 4, cy + 8, 38, 20, 70, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx + 22, cy + 16, 22, 90, 70, 2.0, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx - 22, cy + 16, 22, 20, 70, 2.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy - 38, 2.5, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 14, cy - 32, 1.8, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 14, cy - 32, 1.8, iArgb);
  }

  private static void drawWater(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawSineRow(img, cx, cy - 22, 50, 6, 2.5, aArgb);
    drawSineRow(img, cx, cy, 50, 6, 2.5, aArgb);
    drawSineRow(img, cx, cy + 22, 50, 6, 2.5, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 38, 4, iArgb);
  }

  private static void drawEarth(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy - 22, cx + 36, cy - 22, 5.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy, cx + 36, cy, 5.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy + 22, cx + 36, cy + 22, 5.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 18, cy, 1.8, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 12, cy, 1.8, aArgb);
  }

  private static void drawAir(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy - 26, 40, 200, 140, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 40, 200, 140, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 26, 40, 200, 140, 2.5, aArgb);
  }

  private static void drawLight(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 14, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 14, 0, 360, 1.5, iArgb);

    for (int i = 0; i < 8; i++) {
      double t = i * Math.PI / 4;
      double ix = cx + Math.cos(t) * 22;
      double iy = cy + Math.sin(t) * 22;
      double ox = cx + Math.cos(t) * 44;
      double oy = cy + Math.sin(t) * 44;
      SymbolGlyphPrimitives.drawAaLine(img, ix, iy, ox, oy, 2.5, aArgb);
    }
  }

  private static void drawDark(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 32, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 18, iArgb);
  }

  private static void drawSky(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 20, 44, 0, 180, 2.5, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 44, cy + 20, cx + 44, cy + 20, 2.0, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy - 10, 8, aArgb);
  }

  private static void drawLife(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy + 32, cx, cy - 24, 3.0, iArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx - 12, cy - 4, 14, 290, 110, 2.5, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx + 12, cy - 4, 14, 250, 70, 2.5, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy - 28, 3, aArgb);
  }

  private static void drawDeath(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 26, cy - 14, cx + 26, cy + 26, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 26, cy - 14, cx - 26, cy + 26, 3.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 14, cy - 28, 4, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 14, cy - 28, 4, iArgb);
  }

  private static void drawGrowth(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy + 32, cx, cy - 28, 3.0, iArgb);

    for (int i = 0; i < 3; i++) {
      int y = cy + 16 - i * 16;
      int r = 9 + i * 2;
      SymbolGlyphPrimitives.drawAaArc(img, cx - r, y, r, 290, 110, 2.0, aArgb);
      SymbolGlyphPrimitives.drawAaArc(img, cx + r, y, r, 250, 70, 2.0, aArgb);
    }
  }

  private static void drawDecay(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 30, cy - 22, cx - 18, cy - 6, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 8, cy - 18, cx + 4, cy + 2, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 14, cy - 22, cx + 26, cy - 6, 2.5, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 16, cy + 16, 2.5, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 4, cy + 24, 2.0, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 22, cy + 18, 2.5, iArgb);
  }

  private static void drawForest(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawTreeTriangle(img, cx - 24, cy + 12, 18, aArgb, iArgb);
    drawTreeTriangle(img, cx + 4, cy + 18, 26, aArgb, iArgb);
    drawTreeTriangle(img, cx + 26, cy + 12, 16, aArgb, iArgb);
  }

  private static void drawDesert(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 16, cy - 26, 7, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx - 18, cy + 18, 22, 180, 180, 2.5, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx + 18, cy + 14, 28, 180, 180, 2.5, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 26, 36, 180, 180, 2.5, iArgb);
  }

  private static void drawOcean(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawSineRow(img, cx, cy - 26, 50, 5, 2.0, aArgb);
    drawSineRow(img, cx, cy - 12, 50, 5, 2.0, aArgb);
    drawSineRow(img, cx, cy + 2, 50, 5, 2.0, aArgb);
    drawSineRow(img, cx, cy + 16, 50, 5, 2.0, aArgb);
    drawSineRow(img, cx, cy + 30, 50, 5, 2.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 10, cy - 32, cx + 10, cy - 32, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 32, cx, cy - 42, 2.0, iArgb);
  }

  private static void drawMountain(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    drawPeak(img, cx, cy + 24, 36, aArgb, iArgb);
    drawPeak(img, cx - 26, cy + 24, 22, aArgb, iArgb);
    drawPeak(img, cx + 26, cy + 24, 22, aArgb, iArgb);
  }

  private static void drawPlains(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 44, cy + 12, cx + 44, cy + 12, 2.5, iArgb);

    for (int i = -2; i <= 2; i++) {
      int gx = cx + i * 16;
      SymbolGlyphPrimitives.drawAaLine(img, gx, cy + 12, gx, cy + 4, 2.0, aArgb);
      SymbolGlyphPrimitives.drawAaLine(img, gx - 3, cy + 12, gx - 3, cy + 7, 2.0, aArgb);
      SymbolGlyphPrimitives.drawAaLine(img, gx + 3, cy + 12, gx + 3, cy + 7, 2.0, aArgb);
    }

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 24, cy - 22, 6, aArgb);
  }

  private static void drawSwamp(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 14, 36, 0, 180, 2.5, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 14, 36, 180, 180, 2.5, iArgb);

    for (int i = -2; i <= 2; i++) {
      int rx = cx + i * 12;
      SymbolGlyphPrimitives.drawAaLine(img, rx, cy + 12, rx, cy - 22 + (i & 1) * 6, 2.0, aArgb);
    }

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 8, cy + 22, 2.5, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 6, cy + 18, 1.8, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 14, cy + 24, 2.0, aArgb);
  }

  private static void drawJungle(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    drawTreeTriangle(img, cx - 18, cy + 28, 32, aArgb, iArgb);
    drawTreeTriangle(img, cx + 18, cy + 28, 36, aArgb, iArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy - 4, 24, 30, 120, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 8, 18, 60, 60, 2.0, iArgb);
  }

  private static void drawTundra(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);

    for (int i = 0; i < 6; i++) {
      double t = i * Math.PI / 3;
      double dx = Math.cos(t);
      double dy = Math.sin(t);
      double ox = cx + dx * 36;
      double oy = cy + dy * 36;
      SymbolGlyphPrimitives.drawAaLine(img, cx, cy, ox, oy, 2.5, aArgb);

      double mx = cx + dx * 18;
      double my = cy + dy * 18;
      double px = -dy;
      double py = dx;
      SymbolGlyphPrimitives.drawAaLine(img, mx - px * 5, my - py * 5, mx + px * 5, my + py * 5, 1.8, aArgb);
      double m2x = cx + dx * 28;
      double m2y = cy + dy * 28;
      SymbolGlyphPrimitives.drawAaLine(img, m2x - px * 4, m2y - py * 4, m2x + px * 4, m2y + py * 4, 1.8, aArgb);
    }
  }

  private static void drawCave(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 16, 36, 180, 180, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy + 16, cx + 36, cy + 16, 3.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 4, 14, iArgb);

    for (int i = -1; i <= 1; i++) {
      int sx = cx + i * 12;
      SymbolGlyphPrimitives.drawAaLine(img, sx - 3, cy - 16, sx, cy - 8, 1.8, aArgb);
      SymbolGlyphPrimitives.drawAaLine(img, sx + 3, cy - 16, sx, cy - 8, 1.8, aArgb);
    }
  }

  private static void drawStorm(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawCloudShape(img, cx, cy - 14, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx + 4, cy - 4, cx - 8, cy + 12, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 8, cy + 12, cx + 6, cy + 12, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 6, cy + 12, cx - 4, cy + 32, 3.0, aArgb);
  }

  private static void drawRain(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawCloudShape(img, cx, cy - 14, iArgb);

    for (int i = -2; i <= 2; i++) {
      int rx = cx + i * 10;
      SymbolGlyphPrimitives.drawAaLine(img, rx, cy + 4, rx - 4, cy + 18, 2.0, aArgb);
    }
  }

  private static void drawSnow(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawCloudShape(img, cx, cy - 14, iArgb);

    for (int i = -1; i <= 1; i++) {
      int fx = cx + i * 16;
      int fy = cy + 14;

      SymbolGlyphPrimitives.drawAaLine(img, fx - 4, fy, fx + 4, fy, 1.5, aArgb);
      SymbolGlyphPrimitives.drawAaLine(img, fx, fy - 4, fx, fy + 4, 1.5, aArgb);
      SymbolGlyphPrimitives.drawAaLine(img, fx - 3, fy - 3, fx + 3, fy + 3, 1.5, aArgb);
      SymbolGlyphPrimitives.drawAaLine(img, fx + 3, fy - 3, fx - 3, fy + 3, 1.5, aArgb);
    }
  }

  private static void drawWind(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx - 6, cy - 18, 22, 200, 220, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx + 6, cy + 6, 26, 0, 220, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx - 12, cy + 24, 18, 180, 220, 2.0, aArgb);
  }

  private static void drawCloud(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawCloudShape(img, cx, cy, iArgb);
  }

  private static void drawLightning(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 8, cy - 36, cx - 12, cy - 4, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 12, cy - 4, cx + 8, cy - 4, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 8, cy - 4, cx - 8, cy + 36, 4.0, aArgb);
  }

  private static void drawThunder(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawCloudShape(img, cx, cy - 14, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx + 2, cy - 2, cx - 6, cy + 10, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 6, cy + 10, cx + 4, cy + 10, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 4, cy + 10, cx - 2, cy + 28, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx - 26, cy + 8, 18, 60, 60, 1.8, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx + 26, cy + 8, 18, 240, 60, 1.8, aArgb);
  }

  private static void drawVillage(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawHouse(img, cx - 28, cy + 16, 18, aArgb, iArgb);
    drawHouse(img, cx, cy + 18, 22, aArgb, iArgb);
    drawHouse(img, cx + 28, cy + 16, 18, aArgb, iArgb);
  }

  private static void drawRuin(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 40, cy + 28, cx + 40, cy + 28, 3.0, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 26, cy + 28, cx - 26, cy - 8, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 22, cy - 8, cx - 30, cy - 14, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy + 28, cx, cy + 4, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 4, cy + 4, cx + 4, cy, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 26, cy + 28, cx + 26, cy - 18, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 22, cy - 18, cx + 30, cy - 14, 3.0, aArgb);
  }

  private static void drawPortal(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 28, 0, 360, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 22, 0, 360, 1.5, iArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 14, 90, 270, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 8, 270, 270, 2.0, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 3, aArgb);
  }

  private static void drawTower(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 12, cy + 32, cx - 12, cy - 18, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 12, cy + 32, cx + 12, cy - 18, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 18, cy + 32, cx + 18, cy + 32, 3.0, aArgb);

    for (int i = -2; i <= 2; i++) {
      int bx = cx + i * 6;
      SymbolGlyphPrimitives.drawAaLine(img, bx, cy - 18, bx, cy - 24, 2.0, aArgb);
    }
    SymbolGlyphPrimitives.drawAaLine(img, cx - 12, cy - 18, cx + 12, cy - 18, 2.5, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 24, cx, cy - 40, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 38, cx + 10, cy - 34, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 30, cx + 10, cy - 34, 2.0, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 4, cy + 32, cx - 4, cy + 22, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 4, cy + 32, cx + 4, cy + 22, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 22, 4, 180, 180, 2.0, iArgb);
  }

  private static void drawDungeon(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 26, cy + 28, cx - 26, cy - 8, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 26, cy + 28, cx + 26, cy - 8, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy - 8, 26, 180, 180, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 30, cy + 28, cx + 30, cy + 28, 3.0, aArgb);

    for (int i = -1; i <= 1; i++) {
      SymbolGlyphPrimitives.drawAaLine(img, cx + i * 12, cy - 4, cx + i * 12, cy + 28, 2.0, iArgb);
    }

    SymbolGlyphPrimitives.drawAaLine(img, cx - 18, cy + 14, cx + 18, cy + 14, 2.0, iArgb);
  }

  private static void drawLibrary(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 28, cy - 4, cx - 4, cy - 12, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 28, cy - 4, cx + 4, cy - 12, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 28, cy - 4, cx - 28, cy + 18, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 28, cy - 4, cx + 28, cy + 18, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 28, cy + 18, cx - 4, cy + 14, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 28, cy + 18, cx + 4, cy + 14, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 12, cx, cy + 14, 3.0, aArgb);

    for (int i = 0; i < 3; i++) {
      int ty = cy + 2 + i * 4;
      SymbolGlyphPrimitives.drawAaLine(img, cx - 22, ty, cx - 8, ty, 1.5, iArgb);
      SymbolGlyphPrimitives.drawAaLine(img, cx + 8, ty, cx + 22, ty, 1.5, iArgb);
    }

    SymbolGlyphPrimitives.drawAaLine(img, cx - 12, cy + 18, cx - 18, cy + 30, 2.5, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 12, cy + 18, cx + 18, cy + 30, 2.5, iArgb);
  }

  private static void drawImage(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 22, 38, 230, 80, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy - 22, 38, 50, 80, 3.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 11, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 5, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 4, cy - 4, 2, aArgb);
  }

  private static void drawStimulate(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 6, aArgb);

    drawArrow(img, cx, cy, cx, cy - 36, 2.5, aArgb);
    drawArrow(img, cx, cy, cx, cy + 36, 2.5, aArgb);
    drawArrow(img, cx, cy, cx - 36, cy, 2.5, aArgb);
    drawArrow(img, cx, cy, cx + 36, cy, 2.5, aArgb);
  }

  private static void drawReflect(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 36, cx, cy + 36, 4.0, iArgb);
    for (int i = -3; i <= 3; i++) {
      SymbolGlyphPrimitives.drawAaLine(img, cx + 4, cy + i * 10, cx + 8, cy + i * 10 - 4, 1.5, iArgb);
    }

    drawArrow(img, cx - 32, cy - 18, cx - 4, cy - 4, 2.5, aArgb);
    drawArrow(img, cx - 4, cy - 4, cx - 32, cy + 10, 2.5, aArgb);
  }

  private static void drawRadiate(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 10, aArgb);

    for (int i = 0; i < 12; i++) {
      double t = i * Math.PI / 6;
      double ix = cx + Math.cos(t) * 14;
      double iy = cy + Math.sin(t) * 14;
      double ox = cx + Math.cos(t) * 32;
      double oy = cy + Math.sin(t) * 32;
      SymbolGlyphPrimitives.drawAaLine(img, ix, iy, ox, oy, 2.5, aArgb);

      SymbolGlyphPrimitives.fillSmoothDisc(img, cx + Math.cos(t) * 38, cy + Math.sin(t) * 38, 1.8, aArgb);
    }
  }

  private static void drawLarge(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    drawSquareOutline(img, cx, cy, 38, 4.0, aArgb);
  }

  private static void drawSmall(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    drawSquareOutline(img, cx, cy, 38, 2.0, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 6, aArgb);
  }

  private static void drawDense(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    for (int yi = -3; yi <= 3; yi++) {
      for (int xi = -3; xi <= 3; xi++) {
        SymbolGlyphPrimitives.fillSmoothDisc(img, cx + xi * 9, cy + yi * 9, 2.8, aArgb);
      }
    }
  }

  private static void drawSparse(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 28, cy - 18, 4, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 22, cy + 4, 4, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 8, cy + 26, 4, aArgb);
  }

  private static void drawBright(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 18, aArgb);

    for (int i = 0; i < 16; i++) {
      double t = i * Math.PI / 8;
      SymbolGlyphPrimitives.drawAaLine(img,
          cx + Math.cos(t) * 22, cy + Math.sin(t) * 22,
          cx + Math.cos(t) * 46, cy + Math.sin(t) * 46,
          2.5, aArgb);
    }
  }

  private static void drawDim(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 14, 0, 360, 2.0, aArgb);

    for (int i = 0; i < 4; i++) {
      double t = i * Math.PI / 2 + Math.PI / 4;
      SymbolGlyphPrimitives.drawAaLine(img,
          cx + Math.cos(t) * 18, cy + Math.sin(t) * 18,
          cx + Math.cos(t) * 28, cy + Math.sin(t) * 28,
          1.5, aArgb);
    }
  }

  private static void drawFast(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 38, cy - 16, cx + 18, cy - 16, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 30, cy, cx + 24, cy, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 22, cy + 16, cx + 18, cy + 16, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx + 30, cy, cx + 18, cy - 8, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 30, cy, cx + 18, cy + 8, 3.0, aArgb);
  }

  private static void drawSlow(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 8, 30, 180, 180, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy + 8, cx, cy - 22, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy + 8, cx - 26, cy + 8, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy + 8, cx + 26, cy + 8, 2.0, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 30, cy + 8, 4, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 24, cy + 14, 3, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 8, cy + 14, 3, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 8, cy + 14, 3, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 22, cy + 14, 3, aArgb);
  }

  private static void drawNormal(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy, cx + 36, cy, 4.0, aArgb);

    for (int i = -2; i <= 2; i++) {
      SymbolGlyphPrimitives.drawAaLine(img, cx + i * 16, cy - 4, cx + i * 16, cy + 4, 2.0, iArgb);
    }
  }

  private static void drawExtreme(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 28, cx, cy + 16, 5.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 30, 5, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 10, cy - 36, cx - 4, cy - 26, 2.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 10, cy - 36, cx + 4, cy - 26, 2.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 42, cx, cy - 32, 2.0, aArgb);
  }

  private static void drawSystem(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    int n1x = cx, n1y = cy - 26;
    int n2x = cx - 28, n2y = cy + 18;
    int n3x = cx + 28, n3y = cy + 18;

    SymbolGlyphPrimitives.drawAaLine(img, n1x, n1y, n2x, n2y, 2.5, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, n2x, n2y, n3x, n3y, 2.5, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, n3x, n3y, n1x, n1y, 2.5, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, n1x, n1y, 7, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, n2x, n2y, 7, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, n3x, n3y, 7, aArgb);
  }

  private static void drawMotion(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    drawArrow(img, cx - 32, cy, cx + 32, cy, 4.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 40, cy - 12, cx - 18, cy - 12, 2.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 40, cy + 12, cx - 18, cy + 12, 2.0, aArgb);
  }

  private static void drawCycle(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 30, 30, 300, 3.5, aArgb);

    double t = Math.toRadians(330);
    double tipX = cx + Math.cos(t) * 30;
    double tipY = cy + Math.sin(t) * 30;
    double tx = -Math.sin(t);
    double ty = Math.cos(t);
    SymbolGlyphPrimitives.drawAaLine(img, tipX, tipY, tipX - tx * 8 - ty * 4, tipY - ty * 8 + tx * 4, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, tipX, tipY, tipX - tx * 8 + ty * 4, tipY - ty * 8 - tx * 4, 3.0, aArgb);
  }

  private static void drawForm(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 38, cx - 38, cy + 26, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 38, cx + 38, cy + 26, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 38, cy + 26, cx + 38, cy + 26, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 4, 22, 0, 360, 2.5, iArgb);

    drawSquareOutline(img, cx, cy + 4, 12, 2.0, aArgb);
  }

  private static void drawForce(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 10, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy - 36, cx - 14, cy - 14, 5.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 36, cy - 36, cx + 14, cy - 14, 5.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy + 36, cx - 14, cy + 14, 5.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 36, cy + 36, cx + 14, cy + 14, 5.0, aArgb);
  }

  private static void drawChange(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 32, 0, 360, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy - 16, 16, 0, 360, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 16, 16, 0, 360, 2.5, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy - 16, 4, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 16, 4, aArgb);
  }

  private static void drawFlow(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    drawSineRow(img, cx, cy - 22, 40, 6, 3.0, aArgb);
    drawSineRow(img, cx, cy, 40, 6, 3.0, aArgb);
    drawSineRow(img, cx, cy + 22, 40, 6, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx + 40, cy - 22, cx + 32, cy - 28, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 40, cy - 22, cx + 32, cy - 16, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 40, cy, cx + 32, cy - 6, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 40, cy, cx + 32, cy + 6, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 40, cy + 22, cx + 32, cy + 16, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 40, cy + 22, cx + 32, cy + 28, 2.5, aArgb);
  }

  private static void drawOrder(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    for (int yi = -1; yi <= 1; yi++) {
      for (int xi = -1; xi <= 1; xi++) {
        SymbolGlyphPrimitives.fillSmoothDisc(img, cx + xi * 24, cy + yi * 24, 4, aArgb);
      }
    }
  }

  private static void drawChaos(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx - 4, cy + 4, 16, 30, 280, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx + 6, cy - 6, 12, 100, 250, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx + 2, cy + 12, 22, 200, 200, 2.5, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 30, cy - 22, 3, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 28, cy - 18, 4, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 22, cy + 28, 3, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 26, cy + 24, 4, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy - 36, 2, iArgb);
  }

  private static void drawTime(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 32, 0, 360, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 28, cx, cy - 22, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx, cy + 22, cx, cy + 28, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 28, cy, cx - 22, cy, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 22, cy, cx + 28, cy, 2.5, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy, cx + 12, cy - 8, 3.0, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy, cx, cy - 22, 2.5, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 3, iArgb);
  }

  private static void drawSpace(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 36, 0, 360, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 24, 0, 360, 2.0, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 12, 0, 360, 2.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 28, cy - 14, 2, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 30, cy + 18, 2, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 14, cy + 30, 2, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 4, aArgb);
  }

  private static void drawEnergy(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 8, aArgb);

    for (int i = 0; i < 8; i++) {
      double t = i * Math.PI / 4;
      double midX = cx + Math.cos(t) * 18;
      double midY = cy + Math.sin(t) * 18;
      double tipX = cx + Math.cos(t) * 36;
      double tipY = cy + Math.sin(t) * 36;

      double perpX = -Math.sin(t) * 5;
      double perpY = Math.cos(t) * 5;
      SymbolGlyphPrimitives.drawAaLine(img, cx + Math.cos(t) * 9, cy + Math.sin(t) * 9, midX + perpX, midY + perpY, 2.5, aArgb);
      SymbolGlyphPrimitives.drawAaLine(img, midX + perpX, midY + perpY, tipX, tipY, 2.5, aArgb);
    }
  }

  private static void drawMatter(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 12, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 28, 0, 360, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 36, 30, 320, 2.0, iArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 22, 60, 240, 2.0, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 28, cy, 3, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 18, cy + 26, 3, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 16, cy - 22, 3, aArgb);
  }

  private static void drawVoid(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 36, 0, 360, 4.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 6, cy - 6, cx + 6, cy + 6, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 6, cy + 6, cx + 6, cy - 6, 2.5, aArgb);
  }

  private static void drawRandom(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy - 12, 14, 180, 270, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 10, cy - 4, cx, cy + 10, 4.0, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 22, 5, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 28, cy - 22, 3, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 26, cy - 18, 3, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 30, cy + 24, 3, iArgb);
  }

  private static void drawGradient(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    for (int i = 0; i < 5; i++) {
      int barWidth = 8 + i * 6;
      int yOff = -16 + i * 8;
      SymbolGlyphPrimitives.drawAaLine(img, cx - barWidth, cy + yOff, cx + barWidth, cy + yOff, 4.0, aArgb);
    }
  }

  private static void drawStandard(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    drawSquareOutline(img, cx, cy, 30, 3.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 14, cy + 2, cx - 4, cy + 12, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 4, cy + 12, cx + 16, cy - 10, 4.0, aArgb);
  }

  private static void drawSpecial(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    int n = 5;
    double outR = 28, inR = 12;
    double[] xs = new double[2 * n];
    double[] ys = new double[2 * n];
    for (int i = 0; i < 2 * n; i++) {
      double r = (i % 2 == 0) ? outR : inR;
      double t = -Math.PI / 2 + i * Math.PI / n;
      xs[i] = cx + Math.cos(t) * r;
      ys[i] = cy + Math.sin(t) * r;
    }
    for (int i = 0; i < 2 * n; i++) {
      int j = (i + 1) % (2 * n);
      SymbolGlyphPrimitives.drawAaLine(img, xs[i], ys[i], xs[j], ys[j], 3.0, aArgb);
    }

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 36, cy, 2, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 36, cy, 2, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy - 36, 2, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 36, 2, iArgb);
  }

  private static void drawInstability(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 32, cy + 20, cx - 16, cy - 18, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 16, cy - 18, cx - 4, cy + 12, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 4, cy + 12, cx + 8, cy - 22, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 8, cy - 22, cx + 22, cy + 14, 4.0, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 22, cy + 14, cx + 32, cy - 8, 4.0, aArgb);
  }

  private static void drawStability(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy - 16, cx + 36, cy - 16, 4.0, aArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 24, cy - 16, cx - 28, cy + 18, 3.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 24, cy - 16, cx - 20, cy + 18, 3.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 24, cy - 16, cx + 20, cy + 18, 3.0, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 24, cy - 16, cx + 28, cy + 18, 3.0, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 36, cy + 22, cx + 36, cy + 22, 3.0, aArgb);
  }

  private static void drawBiome(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 30, 38, 180, 180, 3.0, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 22, cy - 18, 8, aArgb);

    drawTreeTriangle(img, cx - 18, cy + 4, 22, aArgb, iArgb);
  }

  private static void drawWeather(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 22, cy - 14, 10, aArgb);
    for (int i = 0; i < 8; i++) {
      double t = i * Math.PI / 4;
      SymbolGlyphPrimitives.drawAaLine(img,
          cx - 22 + Math.cos(t) * 14, cy - 14 + Math.sin(t) * 14,
          cx - 22 + Math.cos(t) * 22, cy - 14 + Math.sin(t) * 22,
          2.0, aArgb);
    }

    drawCloudShape(img, cx + 14, cy + 4, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx + 14, cy + 22, cx + 10, cy + 32, 2.5, aArgb);
  }

  private static void drawTerrain(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 8, 40, 180, 180, 3.0, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 14, 28, 180, 180, 2.5, aArgb);
    SymbolGlyphPrimitives.drawAaArc(img, cx, cy + 18, 16, 180, 180, 2.0, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 4, 4, aArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 30, cy + 28, cx + 30, cy + 28, 2.5, aArgb);
  }

  private static void drawColor(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 30, 0, 360, 3.0, iArgb);

    for (int yi = -28; yi <= 28; yi++) {
      double r = Math.sqrt(900 - yi * yi);
      if (Double.isNaN(r)) continue;
      SymbolGlyphPrimitives.drawAaLine(img, cx - r, cy + yi, cx, cy + yi, 1.0, aArgb);
    }

    SymbolGlyphPrimitives.drawAaLine(img, cx, cy - 30, cx, cy + 30, 2.5, iArgb);
  }

  private static void drawColorSwatch(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy, 30, aArgb);

    SymbolGlyphPrimitives.drawAaArc(img, cx, cy, 30, 0, 360, 2.5, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 8, cy - 8, 6, 0xFFFFFFFF & ((accent & 0xFFFFFF) | 0x40404040));
  }

  private static void drawEaster(@NotNull NativeImage img, int cx, int cy, int accent, int ink, int seed) {
    int aArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int iArgb = 0xFF000000 | (ink & 0xFFFFFF);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 12, cy - 22, 8, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 12, cy - 22, 8, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 12, cy - 8, 6, aArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 12, cy - 8, 6, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 8, 18, aArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 6, cy + 4, 2, iArgb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 6, cy + 4, 2, iArgb);

    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy + 12, 2, iArgb);

    SymbolGlyphPrimitives.drawAaLine(img, cx - 6, cy + 14, cx - 18, cy + 12, 1.5, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - 6, cy + 16, cx - 18, cy + 18, 1.5, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 6, cy + 14, cx + 18, cy + 12, 1.5, iArgb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + 6, cy + 16, cx + 18, cy + 18, 1.5, iArgb);
  }

  private static void drawSquareOutline(@NotNull NativeImage img, int cx, int cy, int half,
                                        double thickness, int argb) {
    SymbolGlyphPrimitives.drawAaLine(img, cx - half, cy - half, cx + half, cy - half, thickness, argb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - half, cy + half, cx + half, cy + half, thickness, argb);
    SymbolGlyphPrimitives.drawAaLine(img, cx - half, cy - half, cx - half, cy + half, thickness, argb);
    SymbolGlyphPrimitives.drawAaLine(img, cx + half, cy - half, cx + half, cy + half, thickness, argb);
  }

  private static void drawPeak(@NotNull NativeImage img, int baseX, int baseY,
                               int height, int rockArgb, int snowArgb) {
    int top = baseY - height;
    int half = height / 2;

    SymbolGlyphPrimitives.drawAaLine(img, baseX - half, baseY, baseX, top, 3.0, rockArgb);
    SymbolGlyphPrimitives.drawAaLine(img, baseX + half, baseY, baseX, top, 3.0, rockArgb);

    SymbolGlyphPrimitives.drawAaLine(img, baseX - half, baseY, baseX + half, baseY, 2.5, rockArgb);

    int capY = top + height / 3;
    int capLeft = baseX - height / 6;
    int capRight = baseX + height / 6;
    SymbolGlyphPrimitives.drawAaLine(img, capLeft, capY, baseX, top, 2.0, snowArgb);
    SymbolGlyphPrimitives.drawAaLine(img, capRight, capY, baseX, top, 2.0, snowArgb);
    SymbolGlyphPrimitives.drawAaLine(img, capLeft, capY, capRight, capY, 2.0, snowArgb);
  }

  private static void drawCloudShape(@NotNull NativeImage img, int cx, int cy, int argb) {
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 14, cy + 4, 12, argb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx, cy - 4, 14, argb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 14, cy + 4, 12, argb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx - 6, cy + 6, 10, argb);
    SymbolGlyphPrimitives.fillSmoothDisc(img, cx + 6, cy + 6, 10, argb);
  }

  private static void drawHouse(@NotNull NativeImage img, int baseX, int baseY,
                                int size, int wallArgb, int roofArgb) {
    int half = size / 2;
    int top = baseY - size;
    int eaves = top + size / 2;

    SymbolGlyphPrimitives.drawAaLine(img, baseX - half, baseY, baseX - half, eaves, 2.5, wallArgb);
    SymbolGlyphPrimitives.drawAaLine(img, baseX + half, baseY, baseX + half, eaves, 2.5, wallArgb);
    SymbolGlyphPrimitives.drawAaLine(img, baseX - half, baseY, baseX + half, baseY, 2.5, wallArgb);

    SymbolGlyphPrimitives.drawAaLine(img, baseX - half - 2, eaves, baseX, top, 2.5, roofArgb);
    SymbolGlyphPrimitives.drawAaLine(img, baseX + half + 2, eaves, baseX, top, 2.5, roofArgb);
    SymbolGlyphPrimitives.drawAaLine(img, baseX - half - 2, eaves, baseX + half + 2, eaves, 2.0, roofArgb);

    SymbolGlyphPrimitives.drawAaLine(img, baseX, baseY, baseX, baseY - size / 3, 2.0, roofArgb);
  }

  private static void drawArrow(@NotNull NativeImage img, double x0, double y0,
                                double x1, double y1, double thickness, int argb) {
    SymbolGlyphPrimitives.drawAaLine(img, x0, y0, x1, y1, thickness, argb);

    double dx = x1 - x0;
    double dy = y1 - y0;
    double len = Math.hypot(dx, dy);
    if (len < 4) return;
    dx /= len;
    dy /= len;

    double headLen = 6;
    double headSpread = 0.5;
    double cosA = Math.cos(headSpread);
    double sinA = Math.sin(headSpread);
    double hx1 = x1 - (dx * cosA + dy * sinA) * headLen;
    double hy1 = y1 - (-dx * sinA + dy * cosA) * headLen;
    double hx2 = x1 - (dx * cosA - dy * sinA) * headLen;
    double hy2 = y1 - (dx * sinA + dy * cosA) * headLen;
    SymbolGlyphPrimitives.drawAaLine(img, x1, y1, hx1, hy1, thickness, argb);
    SymbolGlyphPrimitives.drawAaLine(img, x1, y1, hx2, hy2, thickness, argb);
  }

  private static void drawTreeTriangle(@NotNull NativeImage img, int baseX, int baseY,
                                       int size, int crownArgb, int trunkArgb) {
    int top = baseY - size;
    int left = baseX - size / 2;
    int right = baseX + size / 2;
    SymbolGlyphPrimitives.drawAaLine(img, left, baseY, right, baseY, 2.0, crownArgb);
    SymbolGlyphPrimitives.drawAaLine(img, left, baseY, baseX, top, 2.5, crownArgb);
    SymbolGlyphPrimitives.drawAaLine(img, right, baseY, baseX, top, 2.5, crownArgb);

    SymbolGlyphPrimitives.drawAaLine(img, baseX, baseY, baseX, baseY + 4, 2.0, trunkArgb);
  }

  private static void drawSineRow(@NotNull NativeImage img, int cx, int cy,
                                  int halfWidth, int amplitude, double thickness, int argb) {
    double prevX = cx - halfWidth;
    double prevY = cy;
    int steps = halfWidth;
    for (int i = 1; i <= steps * 2; i++) {
      double t = i / (double) (steps * 2);
      double x = (cx - halfWidth) + t * halfWidth * 2;
      double y = cy + Math.sin(t * Math.PI * 4) * amplitude;
      SymbolGlyphPrimitives.drawAaLine(img, prevX, prevY, x, y, thickness, argb);
      prevX = x;
      prevY = y;
    }
  }

  @FunctionalInterface
  private interface IconDrawer {
    void draw(@NotNull NativeImage image, int cx, int cy, int accent, int ink, int seed);
  }
}

