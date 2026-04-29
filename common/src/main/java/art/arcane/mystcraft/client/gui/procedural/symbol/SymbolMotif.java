package art.arcane.mystcraft.client.gui.procedural.symbol;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.client.gui.procedural.ProceduralTextureCache;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Layout templates for placing per-poem-word glyph tiles onto a symbol
 * page. The motif decides where each glyph sits and what motif-specific
 * decoration is drawn around them.
 * <p>
 * Each {@link SymbolCategory} maps to one default motif via
 * {@link #defaultFor(SymbolCategory)}, mirroring the table in plan
 * §5.3.2. Resource packs can override per-category defaults via
 * {@code assets/mystcraft/gui/symbol_palette.json}.
 * <p>
 * Each enum value is a stateless layout strategy; calling
 * {@link #layout(NativeImage, int, int, int, int, List, SymbolPalette.Entry, int)}
 * is safe from any thread that owns the supplied {@link NativeImage}.
 * The {@code seed} parameter is mixed with motif-specific salt to
 * deterministically place decoration (e.g. STAR_FIELD's dot field) so
 * the same symbol always produces the same pixels.
 */
public enum SymbolMotif {

  /**
   * Original Mystcraft layout: four glyph tiles arranged at the points
   * of a 45°-rotated square (top, right, bottom, left). Tiles smaller
   * than four entries leave the unfilled positions transparent. Used
   * by {@link SymbolCategory#MODIFIER} to retain the legacy look.
   */
  DIAMOND("diamond") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      // sqrt(2)+1 factor mirrors the legacy diamond layout maths used by
      // PageRenderHelper.drawSymbol prior to this refactor.
      float diag = w / 2.414f;
      float offset = diag * 1.414f / 2f;
      int tileSize = Math.max(1, Math.round(diag * 0.8f));

      int cx = x0 + w / 2;
      int cy = y0 + h / 2;

      blitCentered(image, getOrNull(glyphs, 0), cx, Math.round(cy - offset), tileSize);
      blitCentered(image, getOrNull(glyphs, 1), Math.round(cx + offset), cy, tileSize);
      blitCentered(image, getOrNull(glyphs, 2), cx, Math.round(cy + offset), tileSize);
      blitCentered(image, getOrNull(glyphs, 3), Math.round(cx - offset), cy, tileSize);
    }
  },

  /**
   * {@link SymbolCategory#TERRAIN}: a horizon line at mid-height with
   * four glyphs stacked vertically through it. Reads as "ground meets
   * sky" — apt for terrain rules.
   */
  HORIZON("horizon") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int tileSize = Math.max(8, Math.min(w, h) / 4);
      int slot = h / 5;
      blitCentered(image, getOrNull(glyphs, 0), cx, y0 + slot, tileSize);
      blitCentered(image, getOrNull(glyphs, 1), cx, y0 + slot * 2, tileSize);
      blitCentered(image, getOrNull(glyphs, 2), cx, y0 + slot * 3, tileSize);
      blitCentered(image, getOrNull(glyphs, 3), cx, y0 + slot * 4, tileSize);
      // Horizon line: 3-pixel-tall accent stroke at mid-height.
      int yLine = y0 + h / 2;
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0xFF000000 | palette.accentColor());
      for (int dy = -1; dy <= 1; dy++) {
        SymbolGlyphPrimitives.drawLine(image, x0, yLine + dy, x0 + w - 1, yLine + dy, abgrAccent);
      }
    }
  },

  /**
   * {@link SymbolCategory#BIOME_CONTROLLER}: four glyphs at N/E/S/W
   * with a central inkdot — the "compass" reads as a master directive
   * over the surrounding biomes.
   */
  COMPASS("compass") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;
      int radius = Math.min(w, h) / 3;
      int tileSize = Math.max(8, Math.min(w, h) / 4);
      blitCentered(image, getOrNull(glyphs, 0), cx, cy - radius, tileSize); // N
      blitCentered(image, getOrNull(glyphs, 1), cx + radius, cy, tileSize); // E
      blitCentered(image, getOrNull(glyphs, 2), cx, cy + radius, tileSize); // S
      blitCentered(image, getOrNull(glyphs, 3), cx - radius, cy, tileSize); // W
      // Faint connecting cross + central dot.
      int abgrInkSoft = SymbolGlyphPrimitives.toAbgr(0x40000000 | palette.inkColor());
      SymbolGlyphPrimitives.drawLine(image, cx - radius, cy, cx + radius, cy, abgrInkSoft);
      SymbolGlyphPrimitives.drawLine(image, cx, cy - radius, cx, cy + radius, abgrInkSoft);
      SymbolGlyphPrimitives.fillDisc(image, cx, cy, 3, 0xFF000000 | palette.inkColor());
    }
  },

  /**
   * {@link SymbolCategory#BIOME}: a centred glyph framed by a leaf-arc
   * wreath of secondary glyphs. The wreath is drawn as two arcs on the
   * accent colour with small notches to read as foliage.
   */
  WREATH("wreath") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;
      int outer = Math.min(w, h) / 2 - 6;
      int hero = Math.max(12, Math.min(w, h) / 3);
      int leaf = Math.max(8, hero * 2 / 3);
      // Hero glyph centred.
      blitCentered(image, getOrNull(glyphs, 0), cx, cy, hero);
      // Wreath: leaf arc (notched ring) on accent colour.
      int accent = 0xFF000000 | palette.accentColor();
      SymbolGlyphPrimitives.drawNotchedRing(image, cx, cy, outer, 12, accent, seed ^ 0xBADA55);
      // Three secondary glyphs around the top half of the wreath.
      double topArc = Math.toRadians(225); // start (lower-left)
      double sweep = Math.toRadians(90);   // sweep to top
      for (int i = 1; i < Math.min(4, glyphs.size()); i++) {
        double t = topArc + sweep * (i / 3.0);
        int gx = cx + (int) Math.round(Math.cos(t) * outer);
        int gy = cy - (int) Math.round(Math.sin(t) * outer);
        blitCentered(image, getOrNull(glyphs, i), gx, gy, leaf);
      }
    }
  },

  /**
   * {@link SymbolCategory#WEATHER}: cloud silhouette in the top half,
   * vertical rain strokes in the bottom half. Glyphs sit in the four
   * quadrants so each poem word reads as a meteorological aspect.
   */
  STORM("storm") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;
      int tileSize = Math.max(8, Math.min(w, h) / 5);
      // Cloud silhouette: three overlapping discs at the top quarter.
      int cloudY = y0 + h / 4;
      int accent = 0xFF000000 | palette.accentColor();
      int ink = 0xFF000000 | palette.inkColor();
      SymbolGlyphPrimitives.fillDisc(image, cx - w / 6, cloudY, w / 8, accent);
      SymbolGlyphPrimitives.fillDisc(image, cx,           cloudY - 4, w / 7, accent);
      SymbolGlyphPrimitives.fillDisc(image, cx + w / 6, cloudY, w / 8, accent);
      // Rain: 6 short vertical strokes in the bottom half.
      int abgrInk = SymbolGlyphPrimitives.toAbgr(ink);
      Random rng = new Random(seed ^ 0x510C151L);
      for (int i = 0; i < 6; i++) {
        int rx = x0 + 12 + rng.nextInt(Math.max(1, w - 24));
        int ry = cy + 8 + rng.nextInt(Math.max(1, h / 3));
        int len = 6 + rng.nextInt(6);
        SymbolGlyphPrimitives.drawLine(image, rx, ry, rx, ry + len, abgrInk);
      }
      // Glyphs in the four quadrants.
      blitCentered(image, getOrNull(glyphs, 0), cx - w / 4, cy - h / 4, tileSize);
      blitCentered(image, getOrNull(glyphs, 1), cx + w / 4, cy - h / 4, tileSize);
      blitCentered(image, getOrNull(glyphs, 2), cx - w / 4, cy + h / 4, tileSize);
      blitCentered(image, getOrNull(glyphs, 3), cx + w / 4, cy + h / 4, tileSize);
    }
  },

  /**
   * {@link SymbolCategory#LIGHTING}: concentric rings + central glyph +
   * faint rays radiating outward. Reads as a lantern's light cone.
   */
  LANTERN("lantern") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;
      int hero = Math.max(12, Math.min(w, h) / 3);
      int small = Math.max(8, hero * 2 / 3);
      int accent = 0xFF000000 | palette.accentColor();
      int halo = 0x80000000 | palette.haloColor();
      // 8 rays.
      int abgrHalo = SymbolGlyphPrimitives.toAbgr(halo);
      int rayInner = hero / 2 + 4;
      int rayOuter = Math.min(w, h) / 2 - 6;
      for (int i = 0; i < 8; i++) {
        double t = i * (Math.PI / 4) + Math.toRadians(seed & 0x1F);
        int ix = cx + (int) Math.round(Math.cos(t) * rayInner);
        int iy = cy + (int) Math.round(Math.sin(t) * rayInner);
        int ox = cx + (int) Math.round(Math.cos(t) * rayOuter);
        int oy = cy + (int) Math.round(Math.sin(t) * rayOuter);
        SymbolGlyphPrimitives.drawLine(image, ix, iy, ox, oy, abgrHalo);
      }
      // Two concentric rings.
      SymbolGlyphPrimitives.drawArc(image, cx, cy, rayInner, 0, 360, accent, seed);
      SymbolGlyphPrimitives.drawArc(image, cx, cy, rayOuter, 0, 360, accent, seed ^ 0x10);
      // Central glyph + 3 secondaries at NESW between rings.
      blitCentered(image, getOrNull(glyphs, 0), cx, cy, hero);
      int midR = (rayInner + rayOuter) / 2;
      blitCentered(image, getOrNull(glyphs, 1), cx, cy - midR, small);
      blitCentered(image, getOrNull(glyphs, 2), cx + midR, cy, small);
      blitCentered(image, getOrNull(glyphs, 3), cx - midR, cy, small);
    }
  },

  /**
   * {@link SymbolCategory#COLOR}: four colour discs at corners of a
   * centred square, one glyph per disc + a hero glyph at the centre.
   * The discs are tinted variants of the palette accent so the page
   * itself becomes a swatch card.
   */
  SWATCH_RING("swatch_ring") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;
      int hero = Math.max(12, Math.min(w, h) / 3);
      int discR = Math.max(6, Math.min(w, h) / 8);
      int square = Math.min(w, h) / 4;
      // Hero glyph centred.
      blitCentered(image, getOrNull(glyphs, 0), cx, cy, hero);
      // Four corner discs in tinted accents (lighter / darker / accent / halo).
      int[] discColors = {
          0xFF000000 | mixColors(palette.accentColor(), 0xFFFFFF, 0.35f),
          0xFF000000 | mixColors(palette.accentColor(), 0x000000, 0.35f),
          0xFF000000 | palette.accentColor(),
          0xFF000000 | palette.haloColor()
      };
      int[][] cornerOffsets = {{-square, -square}, {square, -square}, {-square, square}, {square, square}};
      for (int i = 0; i < 4; i++) {
        int dx = cx + cornerOffsets[i][0];
        int dy = cy + cornerOffsets[i][1];
        SymbolGlyphPrimitives.fillDisc(image, dx, dy, discR, discColors[i]);
        if (i + 1 < glyphs.size()) {
          blitCentered(image, getOrNull(glyphs, i + 1), dx, dy, discR * 2);
        }
      }
    }
  },

  /**
   * {@link SymbolCategory#VISUAL_EFFECT}: concentric quarter-arcs from
   * one corner with glyphs riding the arc crests. Reads as a wave of
   * effect spreading across the page.
   */
  RIPPLE("ripple") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      // Origin: bottom-left corner of the region.
      int ox = x0 + 4;
      int oy = y0 + h - 4;
      int tileSize = Math.max(8, Math.min(w, h) / 5);
      int accent = 0xFF000000 | palette.accentColor();
      int halo = 0x60000000 | palette.haloColor();
      int maxR = Math.min(w, h);
      // 4 expanding arcs.
      for (int i = 1; i <= 4; i++) {
        int r = (maxR * i) / 4;
        int color = (i % 2 == 0) ? accent : halo;
        SymbolGlyphPrimitives.drawArc(image, ox, oy, r, 0f, 90f, color, seed ^ (i * 31));
      }
      // 4 glyphs along the 1.5th arc crest at 22.5°, 45°, 67.5° angles.
      int crestR = (maxR * 5) / 8;
      double[] angles = {Math.toRadians(15), Math.toRadians(40), Math.toRadians(65), Math.toRadians(80)};
      for (int i = 0; i < Math.min(4, glyphs.size()); i++) {
        int gx = ox + (int) Math.round(Math.cos(angles[i]) * crestR);
        int gy = oy - (int) Math.round(Math.sin(angles[i]) * crestR);
        blitCentered(image, getOrNull(glyphs, i), gx, gy, tileSize);
      }
    }
  },

  /**
   * {@link SymbolCategory#ENVIRONMENT}: a seeded star field with four
   * "constellation node" glyphs connected by faint lines. Each symbol
   * has its own star pattern derived from the seed.
   */
  STAR_FIELD("star_field") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int tileSize = Math.max(8, Math.min(w, h) / 5);
      int halo = 0x60000000 | palette.haloColor();
      int accent = 0xFF000000 | palette.accentColor();
      // Background star field (~32 dots).
      Random rng = new Random(seed ^ 0x57A25F1E1DL);
      for (int i = 0; i < 32; i++) {
        int sx = x0 + rng.nextInt(Math.max(1, w));
        int sy = y0 + rng.nextInt(Math.max(1, h));
        SymbolGlyphPrimitives.fillDisc(image, sx, sy, rng.nextInt(2), halo);
      }
      // 4 constellation nodes at quadrant centres.
      int qw = w / 2;
      int qh = h / 2;
      int[][] nodes = {
          {x0 + qw / 2,           y0 + qh / 2},
          {x0 + qw + qw / 2,      y0 + qh / 2},
          {x0 + qw / 2,           y0 + qh + qh / 2},
          {x0 + qw + qw / 2,      y0 + qh + qh / 2}
      };
      // Constellation lines connect adjacent nodes (top-left → top-right → bottom-right → bottom-left).
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0x80000000 | palette.accentColor());
      SymbolGlyphPrimitives.drawLine(image, nodes[0][0], nodes[0][1], nodes[1][0], nodes[1][1], abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, nodes[1][0], nodes[1][1], nodes[3][0], nodes[3][1], abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, nodes[3][0], nodes[3][1], nodes[2][0], nodes[2][1], abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, nodes[2][0], nodes[2][1], nodes[0][0], nodes[0][1], abgrAccent);
      for (int i = 0; i < 4; i++) {
        // Bright node disc beneath the glyph.
        SymbolGlyphPrimitives.fillDisc(image, nodes[i][0], nodes[i][1], 2, accent);
        blitCentered(image, getOrNull(glyphs, i), nodes[i][0], nodes[i][1], tileSize);
      }
    }
  },

  /**
   * {@link SymbolCategory#FEATURE_LARGE}, {@link SymbolCategory#FEATURE_MEDIUM}
   * and {@link SymbolCategory#FEATURE_SMALL}: a 3×3 grid with glyphs at
   * the four corners. Suggests features as scattered placements across
   * the world map.
   */
  LATTICE("lattice") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int tileSize = Math.max(8, Math.min(w, h) / 4);
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0x80000000 | palette.accentColor());
      // 3×3 grid lines.
      int x1 = x0 + w / 3;
      int x2 = x0 + (2 * w) / 3;
      int y1 = y0 + h / 3;
      int y2 = y0 + (2 * h) / 3;
      SymbolGlyphPrimitives.drawLine(image, x1, y0, x1, y0 + h - 1, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, x2, y0, x2, y0 + h - 1, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, x0, y1, x0 + w - 1, y1, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, x0, y2, x0 + w - 1, y2, abgrAccent);
      // 4 corner glyphs (centred in the corner cells).
      blitCentered(image, getOrNull(glyphs, 0), x0 + w / 6,        y0 + h / 6, tileSize);
      blitCentered(image, getOrNull(glyphs, 1), x0 + (5 * w) / 6,  y0 + h / 6, tileSize);
      blitCentered(image, getOrNull(glyphs, 2), x0 + w / 6,        y0 + (5 * h) / 6, tileSize);
      blitCentered(image, getOrNull(glyphs, 3), x0 + (5 * w) / 6,  y0 + (5 * h) / 6, tileSize);
    }
  },

  /**
   * {@link SymbolCategory#STRUCTURE}: two pillars with a lintel on top,
   * glyphs stacked on each pillar and a keystone glyph in the lintel.
   * Reads as architecture / built environment.
   */
  ARCH("arch") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int accent = 0xFF000000 | palette.accentColor();
      int ink = 0xFF000000 | palette.inkColor();
      int pillarW = Math.max(6, w / 12);
      int pillarH = (h * 2) / 3;
      int lintelH = Math.max(8, h / 8);
      int leftX = x0 + w / 6;
      int rightX = x0 + w - w / 6 - pillarW;
      int topY = y0 + h / 6;
      // Pillars.
      fillRect(image, leftX, topY, pillarW, pillarH, accent);
      fillRect(image, rightX, topY, pillarW, pillarH, accent);
      // Lintel.
      fillRect(image, leftX, topY - lintelH, rightX + pillarW - leftX, lintelH, ink);
      // Keystone glyph in the lintel.
      int keyTile = Math.max(8, lintelH * 2);
      blitCentered(image, getOrNull(glyphs, 0), x0 + w / 2, topY - lintelH / 2, keyTile);
      // Two glyphs stacked on each pillar.
      int colTile = Math.max(8, pillarW + 4);
      int leftCx = leftX + pillarW / 2;
      int rightCx = rightX + pillarW / 2;
      blitCentered(image, getOrNull(glyphs, 1), leftCx,  topY + pillarH / 4, colTile);
      blitCentered(image, getOrNull(glyphs, 2), rightCx, topY + pillarH / 4, colTile);
      blitCentered(image, getOrNull(glyphs, 3), x0 + w / 2, topY + pillarH - colTile / 2, colTile);
    }
  },

  /**
   * {@link SymbolCategory#ANGLE}: a bold cardinal arrow with three
   * faint counter-arrows. Glyphs sit at each cardinal — the bold one
   * is the "primary direction" the symbol nudges the world toward.
   */
  COMPASS_ROSE("compass_rose") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;
      int radius = Math.min(w, h) / 3;
      int tileBig = Math.max(10, Math.min(w, h) / 4);
      int tileSmall = Math.max(8, tileBig * 2 / 3);
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0xFF000000 | palette.accentColor());
      int abgrInkSoft = SymbolGlyphPrimitives.toAbgr(0x40000000 | palette.inkColor());
      // 4 arrow shafts: bold N, faint E/S/W.
      // N arrow (bold, with arrowhead).
      SymbolGlyphPrimitives.drawLine(image, cx, cy, cx, cy - radius, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, cx, cy - radius, cx - 4, cy - radius + 4, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, cx, cy - radius, cx + 4, cy - radius + 4, abgrAccent);
      // E/S/W faint shafts.
      SymbolGlyphPrimitives.drawLine(image, cx, cy, cx + radius, cy, abgrInkSoft);
      SymbolGlyphPrimitives.drawLine(image, cx, cy, cx, cy + radius, abgrInkSoft);
      SymbolGlyphPrimitives.drawLine(image, cx, cy, cx - radius, cy, abgrInkSoft);
      // Glyphs: bold at N, smaller at E/S/W.
      blitCentered(image, getOrNull(glyphs, 0), cx, cy - radius - 4, tileBig);
      blitCentered(image, getOrNull(glyphs, 1), cx + radius + 4, cy, tileSmall);
      blitCentered(image, getOrNull(glyphs, 2), cx, cy + radius + 4, tileSmall);
      blitCentered(image, getOrNull(glyphs, 3), cx - radius - 4, cy, tileSmall);
    }
  },

  /**
   * {@link SymbolCategory#PHASE}: four moon-phase discs across the
   * page (full → gibbous → half → crescent), each containing a glyph.
   */
  MOON_CYCLE("moon_cycle") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cy = y0 + h / 2;
      int discR = Math.max(8, Math.min(w / 9, h / 3));
      int slot = w / 4;
      int tile = Math.max(8, discR * 2 - 4);
      int dim = 0xFF000000 | palette.inkColor();
      int bright = 0xFF000000 | mixColors(palette.haloColor(), 0xFFFFFF, 0.4f);
      // 4 phases: full bright, gibbous (3/4), half (1/2), crescent (1/4).
      for (int i = 0; i < 4; i++) {
        int dcx = x0 + slot / 2 + slot * i;
        SymbolGlyphPrimitives.fillDisc(image, dcx, cy, discR, dim);
        // Lit portion: a smaller disc offset rightward by a phase-dependent amount.
        int litOffset = (int) Math.round(discR * (i / 3.0));
        SymbolGlyphPrimitives.fillDisc(image, dcx + litOffset - discR / 2, cy, discR - 1, bright);
        blitCentered(image, getOrNull(glyphs, i), dcx, cy, tile);
      }
    }
  },

  /**
   * {@link SymbolCategory#LENGTH}: a horizontal ruler with major + minor
   * tick marks. Glyphs sit above each quartile so the page reads as
   * "magnitude divided into named segments".
   */
  RULER("ruler") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int yLine = y0 + (h * 2) / 3;
      int tileSize = Math.max(8, Math.min(w, h) / 5);
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0xFF000000 | palette.accentColor());
      int abgrInk = SymbolGlyphPrimitives.toAbgr(0xFF000000 | palette.inkColor());
      // Ruler line.
      SymbolGlyphPrimitives.drawLine(image, x0 + 4, yLine, x0 + w - 4, yLine, abgrAccent);
      // 9 ticks: tall at quartiles, short between.
      for (int i = 0; i <= 8; i++) {
        int tx = x0 + 4 + ((w - 8) * i) / 8;
        int len = (i % 2 == 0) ? 8 : 4;
        SymbolGlyphPrimitives.drawLine(image, tx, yLine, tx, yLine - len, abgrInk);
      }
      // 4 glyphs at the four quartile positions, above the ruler.
      for (int i = 0; i < Math.min(4, glyphs.size()); i++) {
        int gx = x0 + 4 + ((w - 8) * (i * 2 + 1)) / 8;
        blitCentered(image, getOrNull(glyphs, i), gx, yLine - 14 - tileSize / 2, tileSize);
      }
    }
  },

  /**
   * {@link SymbolCategory#SEA}: three stacked sinusoids with glyphs
   * riding the wave peaks. Reads as the rolling ocean surface.
   */
  WAVE("wave") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int amplitude = h / 8;
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0xFF000000 | palette.accentColor());
      int abgrSoft = SymbolGlyphPrimitives.toAbgr(0x80000000 | palette.accentColor());
      int tileSize = Math.max(8, Math.min(w, h) / 5);
      // 3 stacked sinusoids.
      int[] yLevels = {y0 + h / 4, y0 + h / 2, y0 + (3 * h) / 4};
      for (int level = 0; level < 3; level++) {
        int ly = yLevels[level];
        double phase = ((seed >>> (level * 8)) & 0xFF) / 255.0 * Math.PI * 2;
        int abgr = (level == 1) ? abgrAccent : abgrSoft;
        int prevX = x0;
        int prevY = ly + (int) Math.round(Math.sin(phase) * amplitude);
        for (int xi = 1; xi < w; xi++) {
          double t = (xi / (double) w) * Math.PI * 4 + phase;
          int yi = ly + (int) Math.round(Math.sin(t) * amplitude);
          SymbolGlyphPrimitives.drawLine(image, prevX, prevY, x0 + xi, yi, abgr);
          prevX = x0 + xi;
          prevY = yi;
        }
      }
      // 4 glyphs riding wave peaks across the middle wave.
      double phase = ((seed >>> 8) & 0xFF) / 255.0 * Math.PI * 2;
      int midY = yLevels[1];
      for (int i = 0; i < Math.min(4, glyphs.size()); i++) {
        int xi = ((w - 12) * (2 * i + 1)) / 8 + 6;
        double t = (xi / (double) w) * Math.PI * 4 + phase;
        int yi = midY + (int) Math.round(Math.sin(t) * amplitude);
        blitCentered(image, getOrNull(glyphs, i), x0 + xi, yi, tileSize);
      }
    }
  },

  /**
   * {@link SymbolCategory#SPECIAL}: a five-point star outline with the
   * primary glyph at the centre and small sigils at four of the five
   * points. Reads as ceremonial / unique.
   */
  STAR("star") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;
      int outerR = Math.min(w, h) / 2 - 4;
      int innerR = outerR * 4 / 10;
      int hero = Math.max(12, Math.min(w, h) / 3);
      int tip = Math.max(8, hero * 2 / 3);
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0xFF000000 | palette.accentColor());
      // Five-point star outline: alternate outer and inner radii at 36°.
      int points = 10;
      int[] pxs = new int[points];
      int[] pys = new int[points];
      for (int i = 0; i < points; i++) {
        double t = -Math.PI / 2 + i * Math.PI / 5;
        int r = (i % 2 == 0) ? outerR : innerR;
        pxs[i] = cx + (int) Math.round(Math.cos(t) * r);
        pys[i] = cy + (int) Math.round(Math.sin(t) * r);
      }
      for (int i = 0; i < points; i++) {
        int j = (i + 1) % points;
        SymbolGlyphPrimitives.drawLine(image, pxs[i], pys[i], pxs[j], pys[j], abgrAccent);
      }
      // Hero glyph at centre.
      blitCentered(image, getOrNull(glyphs, 0), cx, cy, hero);
      // Place 4 secondaries at 4 of the 5 outer points (skip the topmost so the centre glyph reads).
      int[] pointIndices = {2, 4, 6, 8};
      for (int i = 1; i < Math.min(4, glyphs.size()); i++) {
        int pi = pointIndices[i - 1];
        blitCentered(image, getOrNull(glyphs, i), pxs[pi], pys[pi], tip);
      }
    }
  };

  private final String name;

  /**
   * Native texture size used for cached motif renders. Picked to match
   * the 4 × {@link SymbolGlyphFactory#GLYPH_SIZE} expected by the diamond
   * layout; smaller than this and corner glyphs would clip.
   */
  public static final int TEX_SIZE = 128;

  /**
   * LRU cache for {@code (motif, symbol, palette)} → DynamicTexture.
   * 256 entries is roughly one motif × every symbol in the registry
   * (~570 symbols stay under the 256 cap thanks to the LRU).
   */
  private static final ProceduralTextureCache MOTIF_CACHE =
      new ProceduralTextureCache("symbol_motif", 256);

  SymbolMotif(@NotNull String name) {
    this.name = name;
  }

  /**
   * Lower-case identifier used in datapack JSON (e.g. {@code "diamond"}).
   */
  @NotNull
  public String getName() {
    return name;
  }

  /**
   * Resolves a motif from its lower-case name. Returns {@code null} for
   * unknown identifiers so callers can warn-and-fallback.
   */
  @Nullable
  public static SymbolMotif fromName(@Nullable String raw) {
    if (raw == null) return null;
    String needle = raw.trim().toLowerCase(Locale.ROOT);
    for (SymbolMotif m : values()) {
      if (m.name.equals(needle)) return m;
    }
    return null;
  }

  /**
   * Returns the canonical default motif for {@code category} per plan
   * §5.3.2. Unrecognised categories (and {@code null}) fall back to
   * {@link #DIAMOND} so resource-pack authors can introduce new
   * categories without breaking rendering.
   */
  @NotNull
  public static SymbolMotif defaultFor(@Nullable SymbolCategory category) {
    if (category == null) return DIAMOND;
    return switch (category) {
      case TERRAIN -> HORIZON;
      case BIOME_CONTROLLER -> COMPASS;
      case BIOME -> WREATH;
      case WEATHER -> STORM;
      case LIGHTING -> LANTERN;
      case COLOR -> SWATCH_RING;
      case VISUAL_EFFECT -> RIPPLE;
      case ENVIRONMENT -> STAR_FIELD;
      case FEATURE_LARGE, FEATURE_MEDIUM, FEATURE_SMALL -> LATTICE;
      case STRUCTURE -> ARCH;
      case ANGLE -> COMPASS_ROSE;
      case PHASE -> MOON_CYCLE;
      case LENGTH -> RULER;
      case SEA -> WAVE;
      case SPECIAL -> STAR;
      case MODIFIER -> DIAMOND;
    };
  }

  /**
   * Composes the supplied glyph tiles into the destination image at the
   * given rectangle. Implementations write directly to {@code image}.
   *
   * @param image   destination — must be at least {@code w × h} starting at {@code (x0, y0)}
   * @param x0      destination top-left X
   * @param y0      destination top-left Y
   * @param w       destination width
   * @param h       destination height
   * @param glyphs  per-poem-word glyph tiles (typically 4 entries, one per word)
   * @param palette palette entry for the symbol's category, used for motif decoration
   * @param seed    deterministic seed mixed into motif-specific decoration
   *                (e.g. STAR_FIELD's dot positions). Same {@code (motif,
   *                symbol)} should always pass the same seed.
   */
  public abstract void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                              @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                              int seed);

  // ---------------------------------------------------------------------
  // High-level render API used by client code.
  // ---------------------------------------------------------------------

  /**
   * Renders this motif for the given symbol + palette and returns a
   * {@link ResourceLocation} whose backing {@code DynamicTexture} contains
   * a {@link #TEX_SIZE TEX_SIZE × TEX_SIZE} composition (transparent
   * background + glyphs laid out by {@link #layout}).
   * <p>
   * Subsequent calls with the same {@code (motif, symbol, palette)} hit
   * the cache. Resource-pack reload should call
   * {@link #invalidateCache()} to drop stale entries.
   */
  @NotNull
  public ResourceLocation getTextureFor(@Nullable IAgeSymbol symbol,
                                        @NotNull SymbolPalette.Entry palette) {
    String key = name + "/"
        + (symbol != null && symbol.getRegistryName() != null
        ? symbol.getRegistryName().toString()
        : "null")
        + "/" + Integer.toHexString(palette.hashCode());
    return MOTIF_CACHE.getOrCreate(key, TEX_SIZE, TEX_SIZE,
        image -> renderTo(image, symbol, palette));
  }

  /**
   * Convenience: renders the motif for {@code symbol} and blits the
   * resulting texture into the GUI at {@code (x, y)} with size
   * {@code scale × scale}.
   * <p>
   * Caller is responsible for any pose-stack push/pop or alpha state
   * around this call. Texture filtering is the GUI default (linear when
   * upscaled), which matches the procedural-UI look elsewhere in the
   * mod.
   */
  public void draw(@NotNull GuiGraphics graphics,
                   @Nullable IAgeSymbol symbol,
                   @NotNull SymbolPalette.Entry palette,
                   float x, float y, float scale) {
    if (scale <= 0f) return;
    ResourceLocation texture = getTextureFor(symbol, palette);
    int size = (int) Math.ceil(scale);
    graphics.blit(texture, (int) x, (int) y, 0, 0, size, size, TEX_SIZE, TEX_SIZE);
  }

  /** Drops every cached motif render — call on resource-pack reload. */
  public static void invalidateCache() {
    MOTIF_CACHE.clear();
  }

  // ---------------------------------------------------------------------
  // Internals
  // ---------------------------------------------------------------------

  /**
   * Composes glyph tiles for {@code symbol}'s poem words and dispatches
   * to {@link #layout} for placement. The destination image is initially
   * fully transparent. The motif's seed is derived from the symbol id +
   * motif ordinal so the same symbol always renders identically.
   */
  private void renderTo(@NotNull NativeImage image, @Nullable IAgeSymbol symbol,
                        @NotNull SymbolPalette.Entry palette) {
    image.fillRect(0, 0, image.getWidth(), image.getHeight(), 0);
    List<NativeImage> glyphs = collectGlyphs(symbol, palette);
    if (glyphs.isEmpty()) return;
    int seed = SymbolSeed.derive(symbol != null ? symbol.getRegistryName() : null,
        getName(), ordinal());
    layout(image, 0, 0, image.getWidth(), image.getHeight(), glyphs, palette, seed);
  }

  /**
   * Loads the up-to-four glyph tiles for {@code symbol}'s poem. Falls
   * back to a single fallback tile if the poem is empty so the cache
   * never returns a fully transparent texture for a real symbol.
   */
  @NotNull
  private static List<NativeImage> collectGlyphs(@Nullable IAgeSymbol symbol,
                                                 @NotNull SymbolPalette.Entry palette) {
    List<NativeImage> glyphs = new ArrayList<>(4);
    String[] poem = symbol != null ? symbol.getPoem() : null;
    if (poem == null || poem.length == 0) {
      glyphs.add(SymbolGlyphFactory.glyph(symbol, null, palette));
      return glyphs;
    }
    for (int i = 0; i < Math.min(4, poem.length); i++) {
      glyphs.add(SymbolGlyphFactory.glyph(symbol, poem[i], palette));
    }
    return glyphs;
  }

  // ---------------------------------------------------------------------
  // Shared helpers used by motif implementations.
  // ---------------------------------------------------------------------

  @Nullable
  protected static NativeImage getOrNull(@NotNull List<NativeImage> glyphs, int index) {
    return index < glyphs.size() ? glyphs.get(index) : null;
  }

  /**
   * Centred convenience over {@link #blitSafe} — places {@code source}
   * with its centre at {@code (cx, cy)} and a {@code tileSize × tileSize}
   * footprint. Out-of-bounds pixels are silently clipped.
   */
  protected static void blitCentered(@NotNull NativeImage dest, @Nullable NativeImage source,
                                     int cx, int cy, int tileSize) {
    blitSafe(dest, source, cx - tileSize / 2, cy - tileSize / 2, tileSize);
  }

  /**
   * Bilinear-scaled blit of {@code source} into {@code dest} at
   * {@code (dx, dy)} with a {@code targetSize × targetSize} footprint.
   * Out-of-bounds pixels are clipped silently. Source alpha is honoured
   * (over-blend); the destination keeps its existing pixels under the
   * source's transparent regions.
   */
  protected static void blitSafe(@NotNull NativeImage dest, @Nullable NativeImage source,
                                 int dx, int dy, int targetSize) {
    if (source == null || targetSize <= 0) return;
    int srcW = source.getWidth();
    int srcH = source.getHeight();
    if (srcW <= 0 || srcH <= 0) return;

    int destW = dest.getWidth();
    int destH = dest.getHeight();

    for (int py = 0; py < targetSize; py++) {
      int destY = dy + py;
      if (destY < 0 || destY >= destH) continue;
      int srcY = (py * srcH) / targetSize;
      if (srcY >= srcH) srcY = srcH - 1;

      for (int px = 0; px < targetSize; px++) {
        int destX = dx + px;
        if (destX < 0 || destX >= destW) continue;
        int srcX = (px * srcW) / targetSize;
        if (srcX >= srcW) srcX = srcW - 1;

        int sourcePixel = source.getPixelRGBA(srcX, srcY);
        int sa = (sourcePixel >>> 24) & 0xFF;
        if (sa == 0) continue;
        if (sa == 0xFF) {
          dest.setPixelRGBA(destX, destY, sourcePixel);
          continue;
        }
        // Blend over existing destination pixel.
        int destPixel = dest.getPixelRGBA(destX, destY);
        int da = (destPixel >>> 24) & 0xFF;

        int sr = sourcePixel & 0xFF;
        int sg = (sourcePixel >>> 8) & 0xFF;
        int sb = (sourcePixel >>> 16) & 0xFF;

        int dr = destPixel & 0xFF;
        int dg = (destPixel >>> 8) & 0xFF;
        int db = (destPixel >>> 16) & 0xFF;

        int inv = 0xFF - sa;
        int outR = (sr * sa + dr * inv) / 0xFF;
        int outG = (sg * sa + dg * inv) / 0xFF;
        int outB = (sb * sa + db * inv) / 0xFF;
        int outA = Math.min(0xFF, sa + (da * inv) / 0xFF);
        int abgr = (outA << 24) | (outB << 16) | (outG << 8) | outR;
        dest.setPixelRGBA(destX, destY, abgr);
      }
    }
  }

  /**
   * Filled-rectangle helper used by motif decoration drawers. Pixels
   * outside the destination are clipped silently. Accepts ARGB.
   */
  protected static void fillRect(@NotNull NativeImage dest, int x, int y, int w, int h, int argb) {
    int destW = dest.getWidth();
    int destH = dest.getHeight();
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
    for (int py = y; py < y + h; py++) {
      if (py < 0 || py >= destH) continue;
      for (int px = x; px < x + w; px++) {
        if (px < 0 || px >= destW) continue;
        dest.setPixelRGBA(px, py, abgr);
      }
    }
  }

  /**
   * Linear blend between two RGB triples, both supplied as 0xRRGGBB.
   * {@code t} = 0 returns {@code a}, {@code t} = 1 returns {@code b}.
   * Used by motifs that want palette-derived "lighter" or "darker"
   * variants without baking extra colours into {@link SymbolPalette}.
   */
  protected static int mixColors(int a, int b, float t) {
    t = Math.max(0f, Math.min(1f, t));
    int ar = (a >>> 16) & 0xFF;
    int ag = (a >>> 8) & 0xFF;
    int ab = a & 0xFF;
    int br = (b >>> 16) & 0xFF;
    int bg = (b >>> 8) & 0xFF;
    int bb = b & 0xFF;
    int rr = Math.round(ar * (1 - t) + br * t);
    int rg = Math.round(ag * (1 - t) + bg * t);
    int rb = Math.round(ab * (1 - t) + bb * t);
    return (rr << 16) | (rg << 8) | rb;
  }
}
