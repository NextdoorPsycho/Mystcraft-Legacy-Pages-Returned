package art.arcane.mystcraft.client.gui.procedural.symbol;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.client.gui.procedural.ProceduralTextureCache;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.datapack.symbol.SymbolDisplay;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level factory for the procedural <em>symbol page</em> item texture.
 * <p>
 * Each {@link ItemStack} of {@code mystcraft:page} resolves to a square
 * {@link ResourceLocation} that contains:
 * <ol>
 *   <li>A cream parchment background with subtle ruling.</li>
 *   <li>The symbol's {@link SymbolMotif} composition (glyphs from
 *       {@link SymbolGlyphFactory}).</li>
 *   <li>(Phase 3) {@link SymbolFlourish} card-rank frame.</li>
 *   <li>(Phase 4) Ink tint blend + affinity splash from the recorded
 *       {@link Page#getInkTint(ItemStack)} and
 *       {@link Page#getAffinitySnapshot(ItemStack)}.</li>
 * </ol>
 *
 * Output is cached by a key derived from the page's content
 * ({@code symbolId}, kind) so the same logical page item always returns
 * the same {@link DynamicTexture}.
 *
 * <h2>Architecture</h2>
 * Used from two render paths:
 * <ul>
 *   <li><b>{@code PageItemRendererBEWLR}</b> — held / inventory / on
 *       writing-desk page item icons.</li>
 *   <li>(Phase 4) Writing-desk thumbnail surface — same texture,
 *       different blit position.</li>
 * </ul>
 */
public final class SymbolPageTextureFactory {

  /** Output texture side length. Matches the legacy BEWLR output size. */
  public static final int TEX_SIZE = 128;

  /**
   * Inset (px) reserved for the parchment border before the motif starts.
   * Tightened from 12 → 6 in the "bigger symbols" pass so the four
   * glyphs read clearly when the page texture is rendered at full
   * item-frame size (see {@code page.json} {@code fixed} transform).
   * Six pixels still leaves room for the rank-5 flourish + instability
   * halo without clipping the bevel edge.
   */
  private static final int MOTIF_MARGIN = 6;

  /** 96 cached page textures × 128² × 4 bytes ≈ 6 MB max. */
  private static final ProceduralTextureCache CACHE = new ProceduralTextureCache("symbol_page", 96);

  /** Parchment fill colour (light cream). */
  private static final int PARCHMENT = 0xFFF1E8CC;

  /** Soft ruling line colour. */
  private static final int RULE_COLOR = 0x60A88E5C;

  /** Bevel edge colour. */
  private static final int EDGE_COLOR = 0xFFB89B68;

  /** Spine-side shadow colour. */
  private static final int SPINE_SHADE = 0xFFC4A77A;

  /** Link-panel ink fallback colour (when no tint is recorded). */
  private static final int LINK_INK_DEFAULT = 0xFF101010;

  private SymbolPageTextureFactory() {
  }

  /** Resets the cache and releases GPU memory. Call on resource-pack reload. */
  public static void reset() {
    CACHE.clear();
  }

  /**
   * Returns the procedural page texture for {@code stack}. Cached by
   * page kind + symbol id + recorded ink tint. Empty stacks render as
   * a neutral parchment.
   */
  @NotNull
  public static ResourceLocation getPageTexture(@NotNull ItemStack stack) {
    PageInfo info = analyse(stack);
    String key = computeKey(info);
    return CACHE.getOrCreate(key, TEX_SIZE, TEX_SIZE, image -> render(image, info));
  }

  // ---------------------------------------------------------------------
  // Internals
  // ---------------------------------------------------------------------

  /** Cache-friendly snapshot of a page item's renderable state. */
  private record PageInfo(@NotNull String kind, @Nullable ResourceLocation symbolId,
                          int inkTint, boolean hasAffinity) {
    boolean isLinkPanel() {
      return "linkpanel".equals(kind);
    }

    boolean isSymbol() {
      return "symbol".equals(kind);
    }
  }

  @NotNull
  private static PageInfo analyse(@NotNull ItemStack stack) {
    if (stack.isEmpty()) {
      return new PageInfo("empty", null, -1, false);
    }
    if (Page.isLinkPanel(stack)) {
      CompoundTag aff = Page.getAffinitySnapshot(stack);
      return new PageInfo("linkpanel", null, Page.getInkTint(stack), aff != null && !aff.isEmpty());
    }
    ResourceLocation sym = Page.getSymbol(stack);
    if (sym != null) {
      return new PageInfo("symbol", sym, -1, false);
    }
    return new PageInfo("blank", null, -1, false);
  }

  @NotNull
  private static String computeKey(@NotNull PageInfo info) {
    StringBuilder sb = new StringBuilder(64);
    sb.append(info.kind());
    if (info.symbolId() != null) {
      sb.append('/').append(info.symbolId().toString().replace(':', '_').replace('/', '_'));
    }
    if (info.inkTint() != -1) {
      sb.append("/t").append(Integer.toHexString(info.inkTint() & 0xFFFFFF));
    }
    if (info.hasAffinity()) {
      sb.append("/a");
    }
    return sb.toString();
  }

  private static void render(@NotNull NativeImage image, @NotNull PageInfo info) {
    // 1. Transparent base (covers any leftover state).
    image.fillRect(0, 0, TEX_SIZE, TEX_SIZE, 0);

    // 2. Parchment background.
    drawParchment(image, info);

    // 3. Symbol motif or link-panel block.
    if (info.isLinkPanel()) {
      drawLinkPanel(image, info);
      return;
    }
    if (info.isSymbol() && info.symbolId() != null) {
      drawSymbolMotif(image, info.symbolId(), info.inkTint(), info.hasAffinity());
    }
    // "blank" / "empty" pages just show the parchment.
  }

  // ---------------------------------------------------------------------
  // Parchment & decoration
  // ---------------------------------------------------------------------

  private static void drawParchment(@NotNull NativeImage image, @NotNull PageInfo info) {
    int parchment = "empty".equals(info.kind()) ? 0xFFEBE3D0 : PARCHMENT;

    fillRect(image, 0, 0, TEX_SIZE, TEX_SIZE, parchment);

    // Subtle horizontal ruled lines (every 6px); diagonals omitted to
    // preserve visual headroom for the motif at the centre.
    int ruleSpacing = 6;
    for (int ly = 6; ly < TEX_SIZE - 6; ly += ruleSpacing) {
      for (int lx = 6; lx < TEX_SIZE - 6; lx++) {
        if (((lx + ly) & 1) == 0) {
          safeSet(image, lx, ly, RULE_COLOR);
        }
      }
    }

    // Bevel edge — draw a 1-pixel rect outline.
    drawRectOutline(image, 0, 0, TEX_SIZE, TEX_SIZE, EDGE_COLOR);

    // Spine shadow (left edge — assume binding is on the left).
    for (int sy = 0; sy < TEX_SIZE; sy++) {
      safeSet(image, 0, sy, SPINE_SHADE);
      safeSet(image, 1, sy, SPINE_SHADE);
    }

    // Faint extra fade for empty pages.
    if ("empty".equals(info.kind())) {
      for (int sy = 0; sy < TEX_SIZE; sy++) {
        for (int sx = 0; sx < TEX_SIZE; sx++) {
          int existing = readPixel(image, sx, sy);
          int faded = (existing & 0x00FFFFFF) | 0x80000000;
          setPixel(image, sx, sy, faded);
        }
      }
    }
  }

  // ---------------------------------------------------------------------
  // Symbol motif
  // ---------------------------------------------------------------------

  private static void drawSymbolMotif(@NotNull NativeImage image, @NotNull ResourceLocation symbolId,
                                      int inkTint, boolean hasAffinity) {
    IAgeSymbol symbol = SymbolRegistry.get(symbolId);
    if (symbol == null) return;
    drawSymbolMotifFor(image, symbol, inkTint, hasAffinity);
  }

  /**
   * Convenience overload used by the test helper and by callers that
   * have a fully-resolved symbol already. Renders without ink tint /
   * affinity overlays.
   */
  public static void drawSymbolMotifFor(@NotNull NativeImage image, @NotNull IAgeSymbol symbol) {
    drawSymbolMotifFor(image, symbol, -1, false);
  }

  /**
   * Renders the motif + flourish + instability halo + ink-tint blend +
   * affinity splash for an arbitrary {@link IAgeSymbol} (registered or
   * not). Public so tests can exercise the rank-progression and
   * motif-dispatch paths without pulling the symbol through the
   * registry.
   *
   * <p>Compose order is fixed at:
   * <ol>
   *   <li>Resolve {@link SymbolDisplay} override (motif / palette / seeds).</li>
   *   <li>Blend ink tint into accent (weight 0.35 when {@code inkTint > 0}).</li>
   *   <li>{@link SymbolMotif#layout motif} (glyphs + decoration)</li>
   *   <li>{@link SymbolFlourish#frame flourish} (rank-driven border + halo)</li>
   *   <li>{@link #drawInstabilityHalo instability halo} (red glow when cost &gt; 0)</li>
   *   <li>{@link #drawAffinitySplash affinity splash} (when affinity snapshot present)</li>
   * </ol>
   *
   * @param inkTint     ARGB ink tint from {@link Page#getInkTint(net.minecraft.world.item.ItemStack)}.
   *                    {@code -1} or {@code 0} disables the blend.
   * @param hasAffinity {@code true} when the page carries a non-empty
   *                    {@link Page#getAffinitySnapshot}; draws a small
   *                    splash near the spine.
   */
  public static void drawSymbolMotifFor(@NotNull NativeImage image, @NotNull IAgeSymbol symbol,
                                        int inkTint, boolean hasAffinity) {
    // Master switch (config 5.1). When disabled, leave only the parchment
    // background that the caller already painted — the page reads as a
    // generic "themed page" without committing to a glyph render.
    if (!MystcraftConfig.proceduralSymbolPagesEnabled.get()) {
      return;
    }

    // 1. Resolve datapack display override (may be null).
    SymbolDisplay display = symbol.getDisplay();

    // 2. Compose the effective palette: category default → display.paletteOverride → ink-tint blend.
    SymbolPalette.Entry palette = SymbolPalette.get(symbol.getCategory());
    if (display != null && display.paletteOverride() != null) {
      palette = applyPaletteOverride(palette, display.paletteOverride());
    }
    if (inkTint != -1 && (inkTint & 0xFFFFFF) != 0) {
      palette = blendInkTint(palette, inkTint);
    }

    // 3. Resolve motif: display.motif → category default.
    SymbolMotif motif = null;
    if (display != null && display.motifName() != null) {
      motif = SymbolMotif.fromName(display.motifName());
    }
    if (motif == null) {
      motif = SymbolMotif.defaultFor(symbol.getCategory());
    }

    int x0 = MOTIF_MARGIN;
    int y0 = MOTIF_MARGIN;
    int w = TEX_SIZE - MOTIF_MARGIN * 2;
    int h = TEX_SIZE - MOTIF_MARGIN * 2;

    List<NativeImage> glyphs = collectGlyphs(symbol, palette);
    if (glyphs.isEmpty()) return;
    int seed = SymbolSeed.derive(symbol.getRegistryName(), motif.getName(), motif.ordinal());
    motif.layout(image, x0, y0, w, h, glyphs, palette, seed);

    SymbolFlourish.Treatment treatment = SymbolFlourish.forRank(symbol.getCardRank());
    SymbolFlourish.frame(image, x0, y0, w, h, palette, treatment);

    if (symbol.getInstabilityCost() > 0.0f) {
      drawInstabilityHalo(image, x0, y0, w, h, symbol.getInstabilityCost());
    }

    if (hasAffinity) {
      drawAffinitySplash(image, palette, inkTint);
    }
  }

  /**
   * Builds a derived {@link SymbolPalette.Entry} where each non-null
   * channel of {@code override} replaces the matching field in
   * {@code base}. Preserves {@link SymbolPalette.Entry#defaultMotif()}
   * since the motif is overridden separately via
   * {@link SymbolDisplay#motifName()}.
   */
  @NotNull
  private static SymbolPalette.Entry applyPaletteOverride(@NotNull SymbolPalette.Entry base,
                                                          @NotNull SymbolDisplay.PaletteOverride override) {
    int b = override.base() != null ? override.base() : base.baseColor();
    int a = override.accent() != null ? override.accent() : base.accentColor();
    int i = override.ink() != null ? override.ink() : base.inkColor();
    int h = override.halo() != null ? override.halo() : base.haloColor();
    return new SymbolPalette.Entry(b, a, i, h, base.defaultMotif());
  }

  /**
   * Returns a derived palette where the accent color is blended 35%
   * toward the supplied ink tint. The blend ratio is fixed (0.35 ink /
   * 0.65 palette accent) so the palette identity stays recognisable
   * even when a saturated ink is used.
   *
   * <p>Halo is also nudged 20% toward the tint so a heavily-themed page
   * keeps a coherent silhouette at high ranks.
   */
  @NotNull
  private static SymbolPalette.Entry blendInkTint(@NotNull SymbolPalette.Entry base, int inkArgb) {
    int blendedAccent = blendRgb(base.accentColor(), inkArgb, 0.35f);
    int blendedHalo = blendRgb(base.haloColor(), inkArgb, 0.20f);
    return new SymbolPalette.Entry(
        base.baseColor(),
        blendedAccent,
        base.inkColor(),
        blendedHalo,
        base.defaultMotif());
  }

  /**
   * Blends two RGB integers (alpha is discarded) by linear interpolation.
   * {@code weight} is the fraction of {@code overlay} applied; {@code 0}
   * returns {@code base}, {@code 1} returns {@code overlay}.
   */
  private static int blendRgb(int base, int overlay, float weight) {
    if (weight <= 0f) return base;
    if (weight >= 1f) return overlay & 0xFFFFFF;
    int br = (base >>> 16) & 0xFF;
    int bg = (base >>> 8) & 0xFF;
    int bb = base & 0xFF;
    int or = (overlay >>> 16) & 0xFF;
    int og = (overlay >>> 8) & 0xFF;
    int ob = overlay & 0xFF;
    int rr = Math.round(br * (1 - weight) + or * weight);
    int rg = Math.round(bg * (1 - weight) + og * weight);
    int rb = Math.round(bb * (1 - weight) + ob * weight);
    return (rr << 16) | (rg << 8) | rb;
  }

  /**
   * Draws a small affinity-splash blob near the binding spine. Re-uses
   * the same teardrop shape as {@code PageTextureFactory.render} for
   * link panels so the player reads them as visually related. Color
   * matches the palette accent (after ink-tint blend has been applied)
   * so the splash always reads as "of the page".
   */
  private static void drawAffinitySplash(@NotNull NativeImage image,
                                         @NotNull SymbolPalette.Entry palette, int inkTint) {
    int splashColor = (inkTint != -1 && (inkTint & 0xFFFFFF) != 0)
        ? (0xFF000000 | (inkTint & 0xFFFFFF))
        : (0xFF000000 | (palette.accentColor() & 0xFFFFFF));

    // Position: just outside the motif rect, near the top-left spine, so
    // the splash reads like a "page-corner sigil" rather than an overlay.
    int blobX = 6;
    int blobY = TEX_SIZE / 2;

    for (int dy = -3; dy <= 3; dy++) {
      for (int dx = -3; dx <= 4; dx++) {
        if (dx * dx + dy * dy <= 10) {
          safeSet(image, blobX + dx, blobY + dy, splashColor);
        }
      }
    }
  }

  @NotNull
  private static List<NativeImage> collectGlyphs(@NotNull IAgeSymbol symbol,
                                                 @NotNull SymbolPalette.Entry palette) {
    List<NativeImage> glyphs = new ArrayList<>(4);
    String[] poem = symbol.getPoem();
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
  // Link-panel block
  // ---------------------------------------------------------------------

  private static void drawLinkPanel(@NotNull NativeImage image, @NotNull PageInfo info) {
    int ink = (info.inkTint() == -1 || info.inkTint() == 0)
        ? LINK_INK_DEFAULT
        : (0xFF000000 | (info.inkTint() & 0xFFFFFF));

    int panelX = (int) (TEX_SIZE * 0.18);
    int panelY = (int) (TEX_SIZE * 0.18);
    int panelW = TEX_SIZE - panelX * 2;
    int panelH = (int) (TEX_SIZE * 0.42);

    fillRect(image, panelX, panelY, panelW, panelH, ink);

    // Affinity splash — small spread blob in the middle.
    if (info.hasAffinity()) {
      int blobX = panelX + panelW / 2;
      int blobY = panelY + panelH / 2;
      for (int dy = -4; dy <= 4; dy++) {
        for (int dx = -6; dx <= 6; dx++) {
          if (dx * dx + dy * dy <= 24) {
            // Slightly lighter shade so the splash reads against the panel.
            int splash = 0xFFFFFFFF;
            int alphaBlend = blendOver(splash, ink, 0x80);
            safeSet(image, blobX + dx, blobY + dy, alphaBlend);
          }
        }
      }
    }
  }

  // ---------------------------------------------------------------------
  // Instability halo
  // ---------------------------------------------------------------------

  /** Warning halo color (warm red-orange). Distinct from any palette halo. */
  private static final int INSTABILITY_RGB = 0xE0532A;

  /**
   * Draws a soft red-orange halo around the rectangle {@code (x0, y0, w, h)}
   * whose intensity scales with {@code cost}. The halo is independent of
   * the {@link SymbolFlourish} rank-5 halo: a high-instability rank-1
   * symbol still gets the warning glow, and a stable rank-5 symbol does
   * not stack two halos. Intensity caps at {@code cost >= 5}.
   *
   * <p>Five concentric rings, each one pixel further out than the last,
   * with monotonically decreasing alpha. The first ring overlaps the
   * outermost flourish border; the test compares pixel counts at the
   * page level so the overlap doesn't muddy rank progression — only
   * symbols with {@code cost == 0} are used in
   * {@code procedural_symbol_rank_progression}.
   */
  private static void drawInstabilityHalo(@NotNull NativeImage image,
                                          int x0, int y0, int w, int h, float cost) {
    float clamped = Math.min(1.0f, cost / 5.0f);
    int peakAlpha = (int) (0x80 * clamped);
    if (peakAlpha <= 0) return;

    int[] alphaProfile = {peakAlpha, peakAlpha * 3 / 4, peakAlpha / 2, peakAlpha / 4, peakAlpha / 8};
    for (int ring = 0; ring < alphaProfile.length; ring++) {
      int alpha = alphaProfile[ring];
      if (alpha <= 0) continue;
      int argb = (alpha << 24) | (INSTABILITY_RGB & 0xFFFFFF);
      int rx = x0 - (ring + 1);
      int ry = y0 - (ring + 1);
      int rw = w + (ring + 1) * 2;
      int rh = h + (ring + 1) * 2;
      drawSoftRectOutline(image, rx, ry, rw, rh, argb);
    }
  }

  private static void drawSoftRectOutline(NativeImage image, int x, int y, int w, int h, int argb) {
    int srcA = (argb >>> 24) & 0xFF;
    if (srcA == 0) return;
    int srcR = (argb >>> 16) & 0xFF;
    int srcG = (argb >>> 8) & 0xFF;
    int srcB = argb & 0xFF;
    for (int i = 0; i < w; i++) {
      blendPixel(image, x + i, y, srcR, srcG, srcB, srcA);
      blendPixel(image, x + i, y + h - 1, srcR, srcG, srcB, srcA);
    }
    for (int i = 0; i < h; i++) {
      blendPixel(image, x, y + i, srcR, srcG, srcB, srcA);
      blendPixel(image, x + w - 1, y + i, srcR, srcG, srcB, srcA);
    }
  }

  private static void blendPixel(NativeImage image, int x, int y,
                                 int srcR, int srcG, int srcB, int srcA) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
    int dst = readPixel(image, x, y);
    int dstA = (dst >>> 24) & 0xFF;
    int dstR = (dst >>> 16) & 0xFF;
    int dstG = (dst >>> 8) & 0xFF;
    int dstB = dst & 0xFF;
    int outA = srcA + (dstA * (0xFF - srcA)) / 0xFF;
    if (outA <= 0) return;
    int outR = (srcR * srcA + dstR * dstA * (0xFF - srcA) / 0xFF) / outA;
    int outG = (srcG * srcA + dstG * dstA * (0xFF - srcA) / 0xFF) / outA;
    int outB = (srcB * srcA + dstB * dstA * (0xFF - srcA) / 0xFF) / outA;
    setPixel(image, x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
  }

  // ---------------------------------------------------------------------
  // Test helpers
  // ---------------------------------------------------------------------

  /**
   * Renders the full symbol-page composition (parchment + motif +
   * flourish + instability halo) for an arbitrary {@link IAgeSymbol}
   * into a freshly-allocated {@link NativeImage}, bypassing the cache
   * entirely. The caller takes ownership of the returned image and
   * must {@link NativeImage#close() close} it.
   *
   * <p>Used by the GameTest assertions in
   * {@code MystcraftGameTestAssertions} so they can supply rank-overriding
   * wrapper symbols without polluting the production cache.
   */
  @NotNull
  public static NativeImage composeSymbolPageImage(@NotNull IAgeSymbol symbol) {
    NativeImage image = new NativeImage(NativeImage.Format.RGBA, TEX_SIZE, TEX_SIZE, false);
    image.fillRect(0, 0, TEX_SIZE, TEX_SIZE, 0);
    PageInfo info = new PageInfo("symbol", symbol.getRegistryName(), -1, false);
    drawParchment(image, info);
    drawSymbolMotifFor(image, symbol);
    return image;
  }

  // ---------------------------------------------------------------------
  // Pixel helpers (mirror PageTextureFactory's conventions)
  // ---------------------------------------------------------------------

  private static void fillRect(NativeImage image, int x, int y, int w, int h, int argb) {
    for (int py = y; py < y + h; py++) {
      for (int px = x; px < x + w; px++) {
        safeSet(image, px, py, argb);
      }
    }
  }

  private static void drawRectOutline(NativeImage image, int x, int y, int w, int h, int argb) {
    int color = 0xFF000000 | (argb & 0xFFFFFF);
    for (int i = 0; i < w; i++) {
      safeSet(image, x + i, y, color);
      safeSet(image, x + i, y + h - 1, color);
    }
    for (int i = 0; i < h; i++) {
      safeSet(image, x, y + i, color);
      safeSet(image, x + w - 1, y + i, color);
    }
  }

  /**
   * Stores an ARGB color into the image, converting to NativeImage's
   * ABGR layout. {@link NativeImage#setPixelRGBA} calls this internally
   * but this wrapper is centralised so it's obvious why we shuffle bytes.
   */
  private static void safeSet(NativeImage image, int x, int y, int argb) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
    setPixel(image, x, y, argb);
  }

  private static void setPixel(NativeImage image, int x, int y, int argb) {
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
    image.setPixelRGBA(x, y, abgr);
  }

  private static int readPixel(NativeImage image, int x, int y) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return 0;
    int abgr = image.getPixelRGBA(x, y);
    int a = (abgr >>> 24) & 0xFF;
    int b = (abgr >>> 16) & 0xFF;
    int g = (abgr >>> 8) & 0xFF;
    int r = abgr & 0xFF;
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  /**
   * Manual "src over dst" blend — used for the affinity splash effect
   * because we need to mix a colour with controlled alpha into an
   * already-opaque parchment / ink pixel.
   */
  private static int blendOver(int srcArgb, int dstArgb, int alpha) {
    int sr = (srcArgb >>> 16) & 0xFF;
    int sg = (srcArgb >>> 8) & 0xFF;
    int sb = srcArgb & 0xFF;
    int dr = (dstArgb >>> 16) & 0xFF;
    int dg = (dstArgb >>> 8) & 0xFF;
    int db = dstArgb & 0xFF;
    int inv = 0xFF - alpha;
    int rr = (sr * alpha + dr * inv) / 0xFF;
    int rg = (sg * alpha + dg * inv) / 0xFF;
    int rb = (sb * alpha + db * inv) / 0xFF;
    return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
  }
}
