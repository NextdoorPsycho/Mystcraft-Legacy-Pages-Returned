package art.arcane.mystcraft.client.gui.procedural.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.client.render.DrawableWordManager;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.datapack.symbol.SymbolDisplay;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds 128×128 D'ni-style glyph tiles for a given symbol + poem-word
 * combination. The result is deterministic — same symbol id + same word always
 * yield identical pixels — so wikis, screenshots, and saved worlds stay
 * visually consistent across runs and machines.
 * <p>
 * Two layers of seed mixing are used:
 * <ol>
 *   <li><b>Word seed pin</b> — words registered via
 *       {@link DrawableWordManager} (the curated D'ni vocabulary) get a
 *       fixed integer seed so e.g. "Fire" always renders with the same
 *       primitive selection and rotation. Pinned colors are also honoured.</li>
 *   <li><b>Symbol mix-in</b> — the pinned seed is combined with the
 *       {@link IAgeSymbol#getRegistryName() symbol id} via
 *       {@link SymbolSeed#derive} so two distinct symbols whose poems
 *       both contain "Fire" don't end up pixel-identical.</li>
 * </ol>
 *
 * <p><b>Resolution bump:</b> tile size doubled 64→128 in the
 * "legibility pass" so primitives (discs, arcs, runic strokes) read
 * crisply when the page texture stretches to fill an item frame or
 * the held-as-map pose. Memory cap stays the same (~12 MB) by
 * trimming the LRU from 256→192 entries.
 */
public final class SymbolGlyphFactory {

  /**
   * Tile size in pixels. Doubled 64→128 for the legibility pass: primitives now
   * have 4× the pixel headroom, eliminating the "pixelated discs" complaint
   * without changing primitive semantics.
   */
  public static final int GLYPH_SIZE = 128;

  private static final int CACHE_CAPACITY = 192;

  private static final ConcurrentHashMap<String, NativeImage> CACHE = new ConcurrentHashMap<>();
  private static final Composition[] COMPOSITIONS = Composition.values();
  private static final Primitive[] SOLO_PRIMITIVES = {
      Primitive.RUNIC_STROKE,
      Primitive.CROWN,
      Primitive.NOTCHED_RING,
      Primitive.CROSSED_BARS,
      Primitive.SPIRAL
  };
  private static final Primitive[] SMALL_PRIMITIVES = {
      Primitive.RUNIC_STROKE,
      Primitive.DOT_CLUSTER,
      Primitive.CROSSED_BARS,
      Primitive.HATCHING,
      Primitive.ARC
  };
  private static final Primitive[] STACKABLE_PRIMITIVES = {
      Primitive.NOTCHED_RING,
      Primitive.RUNIC_STROKE,
      Primitive.CROWN,
      Primitive.HATCHING,
      Primitive.DOT_CLUSTER
  };
  private static volatile boolean warming = false;
  private static volatile boolean warmed = false;

  private SymbolGlyphFactory() {
  }

  /**
   * Returns a 128×128 RGBA tile for the given (symbol, word, palette) triple.
   * <p>
   * The result is cached. Mutating the returned image is undefined behaviour
   * and may corrupt other pages' renders — copy first if you need to modify.
   *
   * @param symbol  optional symbol context (may be null for orphan words)
   * @param word    poem word — case-insensitive; null falls back to the shared
   *                "?" tile
   * @param palette per-category palette
   * @return a glyph tile owned by the factory cache
   */
  @NotNull
  public static NativeImage glyph(@Nullable IAgeSymbol symbol, @Nullable String word,
                                  @NotNull SymbolPalette.Entry palette) {
    String key = cacheKey(symbol, word, palette.accentColor(),
        MystcraftConfig.proceduralSymbolGlyphsEnabled.get());
    NativeImage cached = CACHE.get(key);
    if (cached != null) return cached;
    return CACHE.computeIfAbsent(key, k -> render(symbol, word, palette));
  }

  /**
   * Pre-warms every {@code (symbol, poemWord)} pair on a daemon thread. Safe to
   * call multiple times — only the first call kicks off work. Subsequent calls
   * become no-ops once warming completes.
   */
  public static void warm() {
    if (warmed || warming) return;
    warming = true;
    Thread t = new Thread(SymbolGlyphFactory::warmAllSymbols, "Mystcraft-SymbolGlyph-Prewarm");
    t.setDaemon(true);
    t.start();
  }

  /**
   * Blocking variant of {@link #warm()} — runs the entire pre-warm loop on the
   * calling thread and returns when the cache is fully populated (or has hit
   * {@code CACHE_CAPACITY}). Used by the
   * {@code procedural_symbol_warm_completes} GameTest so it can assert cache
   * state without sleeping. Idempotent — repeat calls are no-ops after the
   * first completes.
   *
   * @return number of glyph tiles in the cache after warming
   */
  public static int warmBlocking() {
    if (warmed) return CACHE.size();

    warming = true;
    try {
      warmAllSymbols();
    } finally {
      warming = false;
    }
    return CACHE.size();
  }

  /**
   * Reset cache (resource-pack reload). Releases all stored
   * {@link NativeImage}s.
   */
  public static void reset() {
    for (NativeImage img : CACHE.values()) {
      try {
        img.close();
      } catch (Exception ignored) {

      }
    }
    CACHE.clear();
    warmed = false;
    warming = false;
  }

  /**
   * Cache size — exposed for tests / debug.
   */
  public static int cacheSize() {
    return CACHE.size();
  }

  private static String cacheKey(@Nullable IAgeSymbol symbol, @Nullable String word, int accent,
                                 boolean glyphsEnabled) {
    String id = symbol != null && symbol.getRegistryName() != null
        ? symbol.getRegistryName().toString()
        : "<null>";
    String w = word != null ? word.toLowerCase(java.util.Locale.ROOT) : "<null>";
    return id + "|" + w + "|" + Integer.toHexString(accent & 0xFFFFFF) + (glyphsEnabled ? "|g" : "|l");
  }

  @NotNull
  private static NativeImage render(@Nullable IAgeSymbol symbol, @Nullable String word,
                                    @NotNull SymbolPalette.Entry palette) {
    NativeImage image = new NativeImage(NativeImage.Format.RGBA, GLYPH_SIZE, GLYPH_SIZE, true);
    image.fillRect(0, 0, GLYPH_SIZE, GLYPH_SIZE, 0);

    if (!MystcraftConfig.proceduralSymbolGlyphsEnabled.get()) {
      String label = labelFor(symbol, word);
      SymbolLetterFallback.renderInto(image, label, palette.accentColor(), palette.inkColor());
      return image;
    }

    SymbolDisplay display = symbol != null ? symbol.getDisplay() : null;
    Integer hardOverride = display != null ? display.seedFor(word) : null;

    Integer pinnedSeed = DrawableWordManager.getCuratedSeed(word);

    int symbolMix = SymbolSeed.derive(symbol != null ? symbol.getRegistryName() : null, word, 0);

    int seed;
    if (hardOverride != null) {
      seed = hardOverride.intValue() ^ symbolMix;
    } else if (pinnedSeed != null) {
      seed = pinnedSeed.intValue() ^ symbolMix;
    } else {
      seed = symbolMix;
    }

    Integer pinnedColor = DrawableWordManager.getCuratedColor(word);
    int accent = pinnedColor != null ? (0xFF000000 | (pinnedColor.intValue() & 0xFFFFFF))
        : (0xFF000000 | (palette.accentColor() & 0xFFFFFF));
    int ink = 0xFF000000 | (palette.inkColor() & 0xFFFFFF);

    if (SymbolGlyphIcons.tryDraw(image, word, accent, ink, seed)) {
      return image;
    }

    compose(image, seed, accent, ink);
    return image;
  }

  @Nullable
  private static String labelFor(@Nullable IAgeSymbol symbol, @Nullable String word) {
    if (word != null && !word.isBlank()) return word;
    if (symbol != null && symbol.getRegistryName() != null) {
      return symbol.getRegistryName().getPath();
    }
    return null;
  }

  private static void compose(@NotNull NativeImage image, int seed, int accent, int ink) {
    int compIdx = Math.floorMod(seed, COMPOSITIONS.length);
    Composition comp = COMPOSITIONS[compIdx];
    int cx = GLYPH_SIZE / 2;
    int cy = GLYPH_SIZE / 2;

    int va = (seed >>> 4) & 0x7FFFFFFF;
    int vb = (seed >>> 12) & 0x7FFFFFFF;
    int subSeed = SymbolSeed.derive("primitive", seed);

    switch (comp) {
      case SOLO -> {
        Primitive p = SOLO_PRIMITIVES[va % SOLO_PRIMITIVES.length];
        applyPrimitive(image, p, cx, cy, 50, accent, subSeed);
      }
      case RINGED -> {

        int notches = 6 + (va & 0x7);
        SymbolGlyphPrimitives.drawNotchedRing(image, cx, cy, 50, notches, accent, subSeed);
        Primitive inner = SMALL_PRIMITIVES[vb % SMALL_PRIMITIVES.length];
        applyPrimitive(image, inner, cx, cy, 18, ink,
            SymbolSeed.derive("inner", subSeed));
      }
      case STACKED_PAIR -> {
        Primitive top = STACKABLE_PRIMITIVES[va % STACKABLE_PRIMITIVES.length];
        Primitive bot = STACKABLE_PRIMITIVES[vb % STACKABLE_PRIMITIVES.length];
        applyPrimitive(image, top, cx, cy - 24, 22, accent, subSeed);
        applyPrimitive(image, bot, cx, cy + 24, 22, ink,
            SymbolSeed.derive("bottom", subSeed));
      }
      case DIAGONAL_PAIR -> {
        Primitive a = SMALL_PRIMITIVES[va % SMALL_PRIMITIVES.length];
        Primitive b = SMALL_PRIMITIVES[vb % SMALL_PRIMITIVES.length];
        applyPrimitive(image, a, cx - 22, cy - 22, 20, accent, subSeed);
        applyPrimitive(image, b, cx + 22, cy + 22, 20, ink,
            SymbolSeed.derive("diag", subSeed));
      }
      case CROSSED_QUADRANTS -> {

        boolean caps = (va & 0x1) != 0;
        SymbolGlyphPrimitives.drawCrossedBars(image, cx, cy, 50, caps, accent, subSeed);

        int[][] corners = {{-26, -26}, {26, -26}, {-26, 26}, {26, 26}};
        for (int i = 0; i < 4; i++) {
          int dotSeed = SymbolSeed.derive("quad", subSeed * 17 + i);
          int dots = 3 + ((dotSeed >>> 5) & 0x1);
          SymbolGlyphPrimitives.drawDotCluster(image, cx + corners[i][0],
              cy + corners[i][1], 9, dots, ink, dotSeed);
        }
      }
      case SPIRAL_CORE -> {
        SymbolGlyphPrimitives.drawSpiral(image, cx, cy, 38, accent, subSeed);

        SymbolGlyphPrimitives.drawAaArc(image, cx, cy, 55, 0, 360, 1.5,
            0xFF000000 | (ink & 0xFFFFFF));
      }
    }
  }

  private static void applyPrimitive(@NotNull NativeImage image, @NotNull Primitive p,
                                     int cx, int cy, int radius, int color, int seed) {
    switch (p) {
      case RUNIC_STROKE ->
          SymbolGlyphPrimitives.drawRunicStroke(image, cx, cy, radius, color, seed);
      case DOT_CLUSTER -> {
        int dots = 3 + (seed & 0x3);
        SymbolGlyphPrimitives.drawDotCluster(image, cx, cy, radius, dots, color, seed);
      }
      case NOTCHED_RING -> {
        int notches = 3 + ((seed >>> 4) & 0x3);
        SymbolGlyphPrimitives.drawNotchedRing(image, cx, cy, radius, notches, color, seed);
      }
      case ARC -> {
        float startDeg = ((seed >>> 8) & 0xFF) * 360f / 256f;
        float sweepDeg = 90f + ((seed >>> 16) & 0xFF) * 270f / 256f;
        SymbolGlyphPrimitives.drawArc(image, cx, cy, radius, startDeg, sweepDeg, color, seed);
      }
      case SPIRAL ->
          SymbolGlyphPrimitives.drawSpiral(image, cx, cy, radius, color, seed);
      case CROSSED_BARS ->
          SymbolGlyphPrimitives.drawCrossedBars(image, cx, cy, radius,
              (seed & 0x80000000) != 0, color, seed);
      case HATCHING -> {
        int strokes = 3 + ((seed >>> 12) & 0x3);
        SymbolGlyphPrimitives.drawHatching(image, cx, cy, radius, strokes, color, seed);
      }
      case CROWN ->
          SymbolGlyphPrimitives.drawCrown(image, cx, cy, radius, color, seed);
    }
  }

  private static void warmAllSymbols() {
    AtomicBoolean ok = new AtomicBoolean(false);
    try {
      Collection<IAgeSymbol> all = SymbolRegistry.getAll();
      int generated = 0;
      for (IAgeSymbol sym : all) {
        SymbolPalette.Entry palette = SymbolPalette.get(sym.getCategory());
        String[] poem = sym.getPoem();
        if (poem == null || poem.length == 0) {
          glyph(sym, null, palette);
          generated++;
          continue;
        }
        for (String word : poem) {
          glyph(sym, word, palette);
          generated++;
          if (CACHE.size() >= CACHE_CAPACITY) {

            ok.set(true);
            Mystcraft.LOGGER.info("[SymbolGlyphFactory] Pre-warm filled cache at {} entries", CACHE.size());
            return;
          }
        }
      }
      ok.set(true);
      Mystcraft.LOGGER.info("[SymbolGlyphFactory] Pre-warmed {} glyph tiles ({} symbols)", generated, all.size());
    } catch (Exception e) {
      Mystcraft.LOGGER.error("[SymbolGlyphFactory] Pre-warm failed", e);
    } finally {
      warming = false;
      warmed = ok.get();
    }
  }

  private enum Primitive {
    RUNIC_STROKE,
    DOT_CLUSTER,
    NOTCHED_RING,
    ARC,
    SPIRAL,
    CROSSED_BARS,
    HATCHING,
    CROWN
  }

  private enum Composition {
    /**
     * One large primitive centred. Reads as a single dominant glyph.
     */
    SOLO,
    /**
     * Outer notched ring + small primitive at centre.
     */
    RINGED,
    /**
     * Two primitives stacked vertically. Reads like two stanzas.
     */
    STACKED_PAIR,
    /**
     * Two primitives on the NW-SE diagonal.
     */
    DIAGONAL_PAIR,
    /**
     * Crossed bars + four dot clusters in the corners.
     */
    CROSSED_QUADRANTS,
    /**
     * Central spiral inside a thin outer ring.
     */
    SPIRAL_CORE
  }
}
