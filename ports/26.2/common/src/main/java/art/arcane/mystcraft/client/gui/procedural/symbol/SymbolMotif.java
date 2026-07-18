package art.arcane.mystcraft.client.gui.procedural.symbol;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.client.gui.procedural.ProceduralTextureCache;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Layout templates for placing per-poem-word glyph tiles onto a symbol page.
 * The motif decides where each glyph sits and what motif-specific decoration is
 * drawn around them.
 * <p>
 * Each {@link SymbolCategory} maps to one default motif via
 * {@link #defaultFor(SymbolCategory)}, mirroring the table in plan §5.3.2.
 * Resource packs can override per-category defaults via
 * {@code assets/mystcraft/gui/symbol_palette.json}.
 * <p>
 * Each enum value is a stateless layout strategy; calling
 * {@link #layout(NativeImage, int, int, int, int, List, SymbolPalette.Entry,
 * int)} is safe from any thread that owns the supplied {@link NativeImage}. The
 * {@code seed} parameter is mixed with motif-specific salt to deterministically
 * place decoration (e.g. STAR_FIELD's dot field) so the same symbol always
 * produces the same pixels.
 */
public enum SymbolMotif {

  /**
   * Original Mystcraft layout: four glyph tiles arranged at the points of a
   * 45°-rotated square (top, right, bottom, left). Tiles smaller than four
   * entries leave the unfilled positions transparent. Used by
   * {@link SymbolCategory#MODIFIER} to retain the legacy look.
   */
  DIAMOND("diamond") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;

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
   * {@link SymbolCategory#TERRAIN}: a horizon line at mid-height with four
   * glyphs stacked vertically through it. Reads as "ground meets sky" — apt for
   * terrain rules.
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

      int yLine = y0 + h / 2;
      int accent = 0xFF000000 | palette.accentColor();
      SymbolGlyphPrimitives.drawAaLine(image, x0, yLine, x0 + w - 1, yLine, 4.0, accent);
    }
  },

  /**
   * {@link SymbolCategory#BIOME_CONTROLLER}: four glyphs at N/E/S/W with a
   * central inkdot — the "compass" reads as a master directive over the
   * surrounding biomes.
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
      blitCentered(image, getOrNull(glyphs, 0), cx, cy - radius, tileSize);
      blitCentered(image, getOrNull(glyphs, 1), cx + radius, cy, tileSize);
      blitCentered(image, getOrNull(glyphs, 2), cx, cy + radius, tileSize);
      blitCentered(image, getOrNull(glyphs, 3), cx - radius, cy, tileSize);

      int abgrInkSoft = SymbolGlyphPrimitives.toAbgr(0x40000000 | palette.inkColor());
      SymbolGlyphPrimitives.drawLine(image, cx - radius, cy, cx + radius, cy, abgrInkSoft);
      SymbolGlyphPrimitives.drawLine(image, cx, cy - radius, cx, cy + radius, abgrInkSoft);
      SymbolGlyphPrimitives.fillSmoothDisc(image, cx, cy, 5, 0xFF000000 | palette.inkColor());
    }
  },

  /**
   * {@link SymbolCategory#BIOME}: a centred glyph framed by a leaf-arc wreath
   * of secondary glyphs. The wreath is drawn as two arcs on the accent colour
   * with small notches to read as foliage.
   */
  WREATH("wreath") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int cy = y0 + h / 2;

      int outer = Math.min(w, h) / 2 - 12;
      int hero = Math.max(12, Math.min(w, h) / 3);
      int leaf = Math.max(8, hero * 2 / 3);

      blitCentered(image, getOrNull(glyphs, 0), cx, cy, hero);

      int accent = 0xFF000000 | palette.accentColor();
      SymbolGlyphPrimitives.drawNotchedRing(image, cx, cy, outer, 16, accent, seed ^ 0xBADA55);

      double topArc = Math.toRadians(225);
      double sweep = Math.toRadians(90);
      for (int i = 1; i < Math.min(4, glyphs.size()); i++) {
        double t = topArc + sweep * (i / 3.0);
        int gx = cx + (int) Math.round(Math.cos(t) * outer);
        int gy = cy - (int) Math.round(Math.sin(t) * outer);
        blitCentered(image, getOrNull(glyphs, i), gx, gy, leaf);
      }
    }
  },

  /**
   * {@link SymbolCategory#WEATHER}: a unified cumulus-style cloud silhouette
   * built from 7 overlapping AA discs (4 base puffs + 3 top peaks), with
   * vertical rain strokes hanging below it. Glyphs sit in a row across the
   * lower third so the cloud reads cleanly as a single shape rather than four
   * separate elements.
   */
  STORM("storm") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;
      int accent = 0xFF000000 | palette.accentColor();

      int accentSoft = 0xC0000000 | mixColors(palette.accentColor(), 0x000000, 0.15f);
      int ink = 0xFF000000 | palette.inkColor();

      int baseY = y0 + h / 3;
      int peakRise = h / 12;
      double basePuff = w / 7.0;
      double peakPuff = w / 9.0;

      double xLeftEdge = cx - w * 0.30;
      double xLeftMid = cx - w * 0.10;
      double xRightMid = cx + w * 0.10;
      double xRightEdge = cx + w * 0.30;
      SymbolGlyphPrimitives.fillSmoothDisc(image, xLeftEdge, baseY, basePuff, accentSoft);
      SymbolGlyphPrimitives.fillSmoothDisc(image, xLeftMid, baseY, basePuff, accentSoft);
      SymbolGlyphPrimitives.fillSmoothDisc(image, xRightMid, baseY, basePuff, accentSoft);
      SymbolGlyphPrimitives.fillSmoothDisc(image, xRightEdge, baseY, basePuff, accentSoft);

      SymbolGlyphPrimitives.fillSmoothDisc(image, cx - w * 0.18, baseY - peakRise * 2, peakPuff, accent);
      SymbolGlyphPrimitives.fillSmoothDisc(image, cx, baseY - peakRise * 3, peakPuff * 1.05, accent);
      SymbolGlyphPrimitives.fillSmoothDisc(image, cx + w * 0.18, baseY - peakRise * 2, peakPuff, accent);

      Random rng = new Random(seed ^ 0x510C151L);
      int rainTop = baseY + (int) Math.round(basePuff) + 4;
      double rainSpan = w * 0.7;
      double rainStartX = cx - rainSpan / 2;
      for (int i = 0; i < 7; i++) {
        double rx = rainStartX + (rainSpan * i) / 6.0;
        double len = 18 + rng.nextInt(10);
        SymbolGlyphPrimitives.drawAaLine(image, rx, rainTop, rx, rainTop + len, 1.5, ink);
      }

      int tileSize = Math.max(10, Math.min(w, h) / 6);
      int gy = y0 + (h * 5) / 6 - tileSize / 2;
      for (int i = 0; i < Math.min(4, glyphs.size()); i++) {
        int gx = x0 + (w * (2 * i + 1)) / 8;
        blitCentered(image, getOrNull(glyphs, i), gx, gy, tileSize);
      }
    }
  },

  /**
   * {@link SymbolCategory#LIGHTING}: concentric rings + central glyph + faint
   * rays radiating outward. Reads as a lantern's light cone.
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

      int abgrHalo = SymbolGlyphPrimitives.toAbgr(halo);

      int rayInner = hero / 2 + 8;
      int rayOuter = Math.min(w, h) / 2 - 12;
      for (int i = 0; i < 8; i++) {
        double t = i * (Math.PI / 4) + Math.toRadians(seed & 0x1F);
        double ix = cx + Math.cos(t) * rayInner;
        double iy = cy + Math.sin(t) * rayInner;
        double ox = cx + Math.cos(t) * rayOuter;
        double oy = cy + Math.sin(t) * rayOuter;

        SymbolGlyphPrimitives.drawAaLine(image, ix, iy, ox, oy, 1.5, halo);
      }

      SymbolGlyphPrimitives.drawArc(image, cx, cy, rayInner, 0, 360, accent, seed);
      SymbolGlyphPrimitives.drawArc(image, cx, cy, rayOuter, 0, 360, accent, seed ^ 0x10);

      blitCentered(image, getOrNull(glyphs, 0), cx, cy, hero);
      int midR = (rayInner + rayOuter) / 2;
      blitCentered(image, getOrNull(glyphs, 1), cx, cy - midR, small);
      blitCentered(image, getOrNull(glyphs, 2), cx + midR, cy, small);
      blitCentered(image, getOrNull(glyphs, 3), cx - midR, cy, small);
    }
  },

  /**
   * {@link SymbolCategory#COLOR}: four colour discs at corners of a centred
   * square, one glyph per disc + a hero glyph at the centre. The discs are
   * tinted variants of the palette accent so the page itself becomes a swatch
   * card.
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

      blitCentered(image, getOrNull(glyphs, 0), cx, cy, hero);

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
        SymbolGlyphPrimitives.fillSmoothDisc(image, dx, dy, discR, discColors[i]);
        if (i + 1 < glyphs.size()) {
          blitCentered(image, getOrNull(glyphs, i + 1), dx, dy, discR * 2);
        }
      }
    }
  },

  /**
   * {@link SymbolCategory#VISUAL_EFFECT}: concentric quarter-arcs from one
   * corner with glyphs riding the arc crests. Reads as a wave of effect
   * spreading across the page.
   */
  RIPPLE("ripple") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;

      int ox = x0 + 4;
      int oy = y0 + h - 4;
      int tileSize = Math.max(8, Math.min(w, h) / 5);
      int accent = 0xFF000000 | palette.accentColor();
      int halo = 0x60000000 | palette.haloColor();
      int maxR = Math.min(w, h);

      for (int i = 1; i <= 4; i++) {
        int r = (maxR * i) / 4;
        int color = (i % 2 == 0) ? accent : halo;
        SymbolGlyphPrimitives.drawArc(image, ox, oy, r, 0f, 90f, color, seed ^ (i * 31));
      }

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
   * "constellation node" glyphs connected by faint lines. Each symbol has its
   * own star pattern derived from the seed.
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

      Random rng = new Random(seed ^ 0x57A25F1E1DL);
      for (int i = 0; i < 64; i++) {
        int sx = x0 + rng.nextInt(Math.max(1, w));
        int sy = y0 + rng.nextInt(Math.max(1, h));
        SymbolGlyphPrimitives.fillDisc(image, sx, sy, rng.nextInt(2), halo);
      }

      int qw = w / 2;
      int qh = h / 2;
      int[][] nodes = {
          {x0 + qw / 2, y0 + qh / 2},
          {x0 + qw + qw / 2, y0 + qh / 2},
          {x0 + qw / 2, y0 + qh + qh / 2},
          {x0 + qw + qw / 2, y0 + qh + qh / 2}
      };

      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0x80000000 | palette.accentColor());
      SymbolGlyphPrimitives.drawLine(image, nodes[0][0], nodes[0][1], nodes[1][0], nodes[1][1], abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, nodes[1][0], nodes[1][1], nodes[3][0], nodes[3][1], abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, nodes[3][0], nodes[3][1], nodes[2][0], nodes[2][1], abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, nodes[2][0], nodes[2][1], nodes[0][0], nodes[0][1], abgrAccent);
      for (int i = 0; i < 4; i++) {

        SymbolGlyphPrimitives.fillSmoothDisc(image, nodes[i][0], nodes[i][1], 4, accent);
        blitCentered(image, getOrNull(glyphs, i), nodes[i][0], nodes[i][1], tileSize);
      }
    }
  },

  /**
   * {@link SymbolCategory#FEATURE_LARGE}, {@link SymbolCategory#FEATURE_MEDIUM}
   * and {@link SymbolCategory#FEATURE_SMALL}: a 3×3 grid with glyphs at the
   * four corners. Suggests features as scattered placements across the world
   * map.
   */
  LATTICE("lattice") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int tileSize = Math.max(8, Math.min(w, h) / 4);
      int abgrAccent = SymbolGlyphPrimitives.toAbgr(0x80000000 | palette.accentColor());

      int x1 = x0 + w / 3;
      int x2 = x0 + (2 * w) / 3;
      int y1 = y0 + h / 3;
      int y2 = y0 + (2 * h) / 3;
      SymbolGlyphPrimitives.drawLine(image, x1, y0, x1, y0 + h - 1, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, x2, y0, x2, y0 + h - 1, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, x0, y1, x0 + w - 1, y1, abgrAccent);
      SymbolGlyphPrimitives.drawLine(image, x0, y2, x0 + w - 1, y2, abgrAccent);

      blitCentered(image, getOrNull(glyphs, 0), x0 + w / 6, y0 + h / 6, tileSize);
      blitCentered(image, getOrNull(glyphs, 1), x0 + (5 * w) / 6, y0 + h / 6, tileSize);
      blitCentered(image, getOrNull(glyphs, 2), x0 + w / 6, y0 + (5 * h) / 6, tileSize);
      blitCentered(image, getOrNull(glyphs, 3), x0 + (5 * w) / 6, y0 + (5 * h) / 6, tileSize);
    }
  },

  /**
   * {@link SymbolCategory#STRUCTURE}: a Roman-style architectural arch — two
   * thick pillars with capitals + bases, joined by a semicircular vault on top.
   * A hero glyph sits inside the arch opening with up to three secondaries
   * flanking outside. Reads as "built environment" / architecture, replacing
   * the legacy flat lintel that read as a Π (capital pi) shape.
   */
  ARCH("arch") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int accent = 0xFF000000 | palette.accentColor();
      int ink = 0xFF000000 | palette.inkColor();
      int cx = x0 + w / 2;

      double pillarThickness = Math.max(8.0, w / 14.0);
      int pillarSpan = w / 4;
      int leftCx = cx - pillarSpan;
      int rightCx = cx + pillarSpan;
      int capitalY = y0 + h / 3;
      int baseY = y0 + h - h / 8;
      double archRadius = pillarSpan;
      int archCenterY = capitalY;

      SymbolGlyphPrimitives.drawAaLine(image, leftCx, capitalY, leftCx, baseY,
          pillarThickness, accent);
      SymbolGlyphPrimitives.drawAaLine(image, rightCx, capitalY, rightCx, baseY,
          pillarThickness, accent);

      double capW = pillarThickness * 1.5;
      double capH = 4.0;
      SymbolGlyphPrimitives.drawAaLine(image,
          leftCx - capW, capitalY, leftCx + capW, capitalY, capH, ink);
      SymbolGlyphPrimitives.drawAaLine(image,
          rightCx - capW, capitalY, rightCx + capW, capitalY, capH, ink);

      double baseW = pillarThickness * 1.8;
      double baseH = 5.0;
      SymbolGlyphPrimitives.drawAaLine(image,
          leftCx - baseW, baseY, leftCx + baseW, baseY, baseH, ink);
      SymbolGlyphPrimitives.drawAaLine(image,
          rightCx - baseW, baseY, rightCx + baseW, baseY, baseH, ink);

      SymbolGlyphPrimitives.drawAaArc(image,
          cx, archCenterY, archRadius, 180, 180, 4.0, accent);

      double innerR = archRadius - 8;
      if (innerR > 4) {
        int innerInk = 0x80000000 | palette.inkColor();
        SymbolGlyphPrimitives.drawAaArc(image,
            cx, archCenterY, innerR, 180, 180, 2.0, innerInk);
      }

      int heroSize = Math.max(14, Math.min((int) (pillarSpan * 1.5), h / 3));
      int archInnerCy = archCenterY + (baseY - archCenterY) / 2;
      blitCentered(image, getOrNull(glyphs, 0), cx, archInnerCy, heroSize);

      int keyTile = Math.max(10, w / 9);
      int keystoneY = archCenterY - (int) Math.round(archRadius) - keyTile / 2 - 4;
      if (keystoneY > y0 + 4) {
        blitCentered(image, getOrNull(glyphs, 1), cx, keystoneY, keyTile);
      }

      int sideTile = Math.max(10, w / 11);
      int sideMargin = sideTile / 2 + 6;
      blitCentered(image, getOrNull(glyphs, 2), x0 + sideMargin, archInnerCy, sideTile);
      blitCentered(image, getOrNull(glyphs, 3), x0 + w - sideMargin, archInnerCy, sideTile);
    }
  },

  /**
   * {@link SymbolCategory#ANGLE}: a bold cardinal arrow with three faint
   * counter-arrows. Glyphs sit at each cardinal — the bold one is the "primary
   * direction" the symbol nudges the world toward.
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

      int accent = 0xFF000000 | palette.accentColor();
      SymbolGlyphPrimitives.drawAaLine(image, cx, cy, cx, cy - radius, 2.5, accent);
      SymbolGlyphPrimitives.drawAaLine(image, cx, cy - radius, cx - 8, cy - radius + 8, 2.0, accent);
      SymbolGlyphPrimitives.drawAaLine(image, cx, cy - radius, cx + 8, cy - radius + 8, 2.0, accent);

      SymbolGlyphPrimitives.drawLine(image, cx, cy, cx + radius, cy, abgrInkSoft);
      SymbolGlyphPrimitives.drawLine(image, cx, cy, cx, cy + radius, abgrInkSoft);
      SymbolGlyphPrimitives.drawLine(image, cx, cy, cx - radius, cy, abgrInkSoft);

      blitCentered(image, getOrNull(glyphs, 0), cx, cy - radius - 8, tileBig);
      blitCentered(image, getOrNull(glyphs, 1), cx + radius + 8, cy, tileSmall);
      blitCentered(image, getOrNull(glyphs, 2), cx, cy + radius + 8, tileSmall);
      blitCentered(image, getOrNull(glyphs, 3), cx - radius - 8, cy, tileSmall);
    }
  },

  /**
   * {@link SymbolCategory#PHASE}: four moon-phase discs across the page (full →
   * gibbous → half → crescent), each containing a glyph.
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

      for (int i = 0; i < 4; i++) {
        int dcx = x0 + slot / 2 + slot * i;
        SymbolGlyphPrimitives.fillSmoothDisc(image, dcx, cy, discR, dim);

        double litOffset = discR * (i / 3.0);
        SymbolGlyphPrimitives.fillSmoothDisc(image, dcx + litOffset - discR / 2.0, cy,
            discR - 1.0, bright);
        blitCentered(image, getOrNull(glyphs, i), dcx, cy, tile);
      }
    }
  },

  /**
   * {@link SymbolCategory#LENGTH}: a horizontal ruler with major + minor tick
   * marks. Glyphs sit above each quartile so the page reads as "magnitude
   * divided into named segments".
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

      int accent = 0xFF000000 | palette.accentColor();
      int ink = 0xFF000000 | palette.inkColor();
      SymbolGlyphPrimitives.drawAaLine(image, x0 + 8, yLine, x0 + w - 8, yLine, 2.0, accent);

      for (int i = 0; i <= 8; i++) {
        double tx = x0 + 8 + ((w - 16) * i) / 8.0;
        double len = (i % 2 == 0) ? 16 : 8;
        SymbolGlyphPrimitives.drawAaLine(image, tx, yLine, tx, yLine - len, 1.5, ink);
      }

      for (int i = 0; i < Math.min(4, glyphs.size()); i++) {
        int gx = x0 + 8 + ((w - 16) * (i * 2 + 1)) / 8;
        blitCentered(image, getOrNull(glyphs, i), gx, yLine - 28 - tileSize / 2, tileSize);
      }
    }
  },

  /**
   * {@link SymbolCategory#SEA}: three stacked sinusoids with glyphs riding the
   * wave peaks. Reads as the rolling ocean surface.
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

      int waveAccent = 0xFF000000 | palette.accentColor();
      int waveSoft = 0x80000000 | palette.accentColor();
      int[] yLevels = {y0 + h / 4, y0 + h / 2, y0 + (3 * h) / 4};
      for (int level = 0; level < 3; level++) {
        int ly = yLevels[level];
        double phase = ((seed >>> (level * 8)) & 0xFF) / 255.0 * Math.PI * 2;
        int color = (level == 1) ? waveAccent : waveSoft;
        double prevX = x0;
        double prevY = ly + Math.sin(phase) * amplitude;
        for (int xi = 2; xi < w; xi += 2) {
          double t = (xi / (double) w) * Math.PI * 4 + phase;
          double yi = ly + Math.sin(t) * amplitude;
          SymbolGlyphPrimitives.drawAaLine(image, prevX, prevY, x0 + xi, yi, 1.5, color);
          prevX = x0 + xi;
          prevY = yi;
        }
      }

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
   * {@link SymbolCategory#SPECIAL}: a five-point star with a translucent filled
   * body, bold outline, and a hero glyph at its centre. Three small secondary
   * glyphs sit in a row below the star so the star silhouette reads cleanly
   * without point-cluttering. Reads as ceremonial / unique.
   */
  STAR("star") {
    @Override
    public void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                       @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                       int seed) {
      if (glyphs.isEmpty()) return;
      int cx = x0 + w / 2;

      int starCy = y0 + (h * 2) / 5;
      int outerR = Math.min(w, h * 2 / 3) / 2 - 8;

      int innerR = (int) Math.round(outerR * 0.382);
      int hero = Math.max(14, Math.min(w, h) / 4);
      int starAccent = 0xFF000000 | palette.accentColor();

      int starFill = 0x80000000 | mixColors(palette.accentColor(), 0xFFFFFF, 0.55f);

      int points = 10;
      double[] pxs = new double[points];
      double[] pys = new double[points];
      for (int i = 0; i < points; i++) {
        double t = -Math.PI / 2 + i * Math.PI / 5;
        double r = (i % 2 == 0) ? outerR : innerR;
        pxs[i] = cx + Math.cos(t) * r;
        pys[i] = starCy + Math.sin(t) * r;
      }

      SymbolGlyphPrimitives.fillPolygon(image, pxs, pys, starFill);

      for (int i = 0; i < points; i++) {
        int j = (i + 1) % points;
        SymbolGlyphPrimitives.drawAaLine(image, pxs[i], pys[i], pxs[j], pys[j], 3.0, starAccent);
      }

      blitCentered(image, getOrNull(glyphs, 0), cx, starCy, hero);

      int tile = Math.max(10, Math.min(w, h) / 7);
      int gy = y0 + h - tile / 2 - 8;
      int spacing = tile + 6;
      int countSec = Math.min(3, glyphs.size() - 1);
      for (int i = 0; i < countSec; i++) {
        int gx = (int) Math.round(cx + (i - (countSec - 1) / 2.0f) * spacing);
        blitCentered(image, getOrNull(glyphs, i + 1), gx, gy, tile);
      }
    }
  };

  /**
   * Native texture size used for cached motif renders. Doubled 128→256 in the
   * legibility pass so the four 128-px glyph tiles (from
   * {@link SymbolGlyphFactory#GLYPH_SIZE}) fit at full size in the diamond
   * layout without scaling. The bigger texture also gives motif decorations
   * (notched rings, cloud discs, lantern concentric rings) 4× the pixel
   * headroom.
   */
  public static final int TEX_SIZE = 256;
  private final String name;

  /** Defers client texture classes until a render/cache operation actually runs. */
  private static final class CacheHolder {
    private static final ProceduralTextureCache INSTANCE =
        new ProceduralTextureCache("symbol_motif", 96);
  }

  SymbolMotif(@NotNull String name) {
    this.name = name;
  }

  /**
   * Resolves a motif from its lower-case name. Returns {@code null} for unknown
   * identifiers so callers can warn-and-fallback.
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
   * Returns the canonical default motif for {@code category} per plan §5.3.2.
   * Unrecognised categories (and {@code null}) fall back to {@link #DIAMOND} so
   * resource-pack authors can introduce new categories without breaking
   * rendering.
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
   * Drops every cached motif render — call on resource-pack reload.
   */
  public static void invalidateCache() {
    CacheHolder.INSTANCE.clear();
  }

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

  @Nullable
  protected static NativeImage getOrNull(@NotNull List<NativeImage> glyphs, int index) {
    return index < glyphs.size() ? glyphs.get(index) : null;
  }

  /**
   * Centred convenience over {@link #blitSafe} — places {@code source} with its
   * centre at {@code (cx, cy)} and a {@code tileSize × tileSize} footprint.
   * Out-of-bounds pixels are silently clipped.
   */
  protected static void blitCentered(@NotNull NativeImage dest, @Nullable NativeImage source,
                                     int cx, int cy, int tileSize) {
    blitSafe(dest, source, cx - tileSize / 2, cy - tileSize / 2, tileSize);
  }

  /**
   * Bilinear-scaled blit of {@code source} into {@code dest} at
   * {@code (dx, dy)} with a {@code targetSize × targetSize} footprint.
   * Out-of-bounds pixels are clipped silently. Source alpha is honoured
   * (over-blend); the destination keeps its existing pixels under the source's
   * transparent regions.
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

        int sourcePixel = net.minecraft.util.ARGB.toABGR(source.getPixel(srcX, srcY));
        int sa = (sourcePixel >>> 24) & 0xFF;
        if (sa == 0) continue;
        if (sa == 0xFF) {
          dest.setPixelABGR(destX, destY, sourcePixel);
          continue;
        }

        int destPixel = net.minecraft.util.ARGB.toABGR(dest.getPixel(destX, destY));
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
        dest.setPixelABGR(destX, destY, abgr);
      }
    }
  }

  /**
   * Filled-rectangle helper used by motif decoration drawers. Pixels outside
   * the destination are clipped silently. Accepts ARGB.
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
        dest.setPixelABGR(px, py, abgr);
      }
    }
  }

  /**
   * Linear blend between two RGB triples, both supplied as 0xRRGGBB. {@code t}
   * = 0 returns {@code a}, {@code t} = 1 returns {@code b}. Used by motifs that
   * want palette-derived "lighter" or "darker" variants without baking extra
   * colours into {@link SymbolPalette}.
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

  /**
   * Lower-case identifier used in datapack JSON (e.g. {@code "diamond"}).
   */
  @NotNull
  public String getName() {
    return name;
  }

  /**
   * Composes the supplied glyph tiles into the destination image at the given
   * rectangle. Implementations write directly to {@code image}.
   *
   * @param image   destination — must be at least {@code w × h} starting at
   *                {@code (x0, y0)}
   * @param x0      destination top-left X
   * @param y0      destination top-left Y
   * @param w       destination width
   * @param h       destination height
   * @param glyphs  per-poem-word glyph tiles (typically 4 entries, one per
   *                word)
   * @param palette palette entry for the symbol's category, used for motif
   *                decoration
   * @param seed    deterministic seed mixed into motif-specific decoration
   *                (e.g. STAR_FIELD's dot positions). Same
   *                {@code (motif, symbol)} should always pass the same seed.
   */
  public abstract void layout(@NotNull NativeImage image, int x0, int y0, int w, int h,
                              @NotNull List<NativeImage> glyphs, @NotNull SymbolPalette.Entry palette,
                              int seed);

  /**
   * Renders this motif for the given symbol + palette and returns a
   * {@link Identifier} whose backing {@code DynamicTexture} contains a
   * {@link #TEX_SIZE TEX_SIZE × TEX_SIZE} composition (transparent background +
   * glyphs laid out by {@link #layout}).
   * <p>
   * Subsequent calls with the same {@code (motif, symbol, palette)} hit the
   * cache. Resource-pack reload should call {@link #invalidateCache()} to drop
   * stale entries.
   */
  @NotNull
  public Identifier getTextureFor(@Nullable IAgeSymbol symbol,
                                        @NotNull SymbolPalette.Entry palette) {
    String key = name + "/"
        + (symbol != null && symbol.getRegistryName() != null
        ? symbol.getRegistryName().toString()
        : "null")
        + "/" + Integer.toHexString(palette.hashCode());
    return CacheHolder.INSTANCE.getOrCreate(key, TEX_SIZE, TEX_SIZE,
        image -> renderTo(image, symbol, palette));
  }

  /**
   * Convenience: renders the motif for {@code symbol} and blits the resulting
   * texture into the GUI at {@code (x, y)} with size {@code scale × scale}.
   * <p>
   * Caller is responsible for any pose-stack push/pop or alpha state around
   * this call. Texture filtering is the GUI default (linear when upscaled),
   * which matches the procedural-UI look elsewhere in the mod.
   */
  public void draw(@NotNull GuiGraphicsExtractor graphics,
                   @Nullable IAgeSymbol symbol,
                   @NotNull SymbolPalette.Entry palette,
                   float x, float y, float scale) {
    if (scale <= 0f) return;
    Identifier texture = getTextureFor(symbol, palette);
    int size = (int) Math.ceil(scale);
    graphics.blit(texture, (int) x, (int) y, 0, 0, size, size, TEX_SIZE, TEX_SIZE);
  }

  private void renderTo(@NotNull NativeImage image, @Nullable IAgeSymbol symbol,
                        @NotNull SymbolPalette.Entry palette) {
    image.fillRect(0, 0, image.getWidth(), image.getHeight(), 0);
    List<NativeImage> glyphs = collectGlyphs(symbol, palette);
    if (glyphs.isEmpty()) return;
    int seed = SymbolSeed.derive(symbol != null ? symbol.getRegistryName() : null,
        getName(), ordinal());
    layout(image, 0, 0, image.getWidth(), image.getHeight(), glyphs, palette, seed);
  }
}
