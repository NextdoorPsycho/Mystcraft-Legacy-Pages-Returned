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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds 64×64 D'ni-style glyph tiles for a given symbol + poem-word
 * combination. The result is deterministic — same symbol id + same word
 * always yield identical pixels — so wikis, screenshots, and saved
 * worlds stay visually consistent across runs and machines.
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
 * Phase 1 ships the factory + a primitive-selection DSL. Phase 4 hooks
 * the optional datapack {@code display.glyph_seeds} pin into the same
 * derive function.
 */
public final class SymbolGlyphFactory {

  /** Tile size in pixels. Picked to match the legacy in-book draw scale. */
  public static final int GLYPH_SIZE = 64;

  /** Cache capacity (per-tile). 256 × 64×64 RGBA ≈ 4 MB. */
  private static final int CACHE_CAPACITY = 256;

  private static final ConcurrentHashMap<String, NativeImage> CACHE = new ConcurrentHashMap<>();

  /**
   * Primitive types selectable by {@link #compose}. Weight is duplicated
   * in {@link #PRIMITIVE_WEIGHTS} so popular primitives (runic strokes,
   * dot clusters) appear more often.
   */
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

  private static final Primitive[] PRIMITIVE_WEIGHTS = {
      Primitive.RUNIC_STROKE, Primitive.RUNIC_STROKE, Primitive.RUNIC_STROKE,
      Primitive.DOT_CLUSTER, Primitive.DOT_CLUSTER,
      Primitive.NOTCHED_RING,
      Primitive.ARC, Primitive.ARC,
      Primitive.SPIRAL,
      Primitive.CROSSED_BARS,
      Primitive.HATCHING,
      Primitive.CROWN
  };

  private static volatile boolean warming = false;
  private static volatile boolean warmed = false;

  private SymbolGlyphFactory() {
  }

  /**
   * Returns a 64×64 RGBA tile for the given (symbol, word, palette) triple.
   * <p>
   * The result is cached. Mutating the returned image is undefined
   * behaviour and may corrupt other pages' renders — copy first if you
   * need to modify.
   *
   * @param symbol  optional symbol context (may be null for orphan words)
   * @param word    poem word — case-insensitive; null falls back to the
   *                shared "?" tile
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
   * Pre-warms every {@code (symbol, poemWord)} pair on a daemon thread.
   * Safe to call multiple times — only the first call kicks off work.
   * Subsequent calls become no-ops once warming completes.
   */
  public static void warm() {
    if (warmed || warming) return;
    warming = true;
    Thread t = new Thread(SymbolGlyphFactory::warmAllSymbols, "Mystcraft-SymbolGlyph-Prewarm");
    t.setDaemon(true);
    t.start();
  }

  /**
   * Blocking variant of {@link #warm()} — runs the entire pre-warm
   * loop on the calling thread and returns when the cache is fully
   * populated (or has hit {@code CACHE_CAPACITY}). Used by the
   * {@code procedural_symbol_warm_completes} GameTest so it can assert
   * cache state without sleeping. Idempotent — repeat calls are no-ops
   * after the first completes.
   *
   * @return number of glyph tiles in the cache after warming
   */
  public static int warmBlocking() {
    if (warmed) return CACHE.size();
    // We intentionally don't gate on `warming` here — that flag is for
    // the async path. Tests want a deterministic synchronous result.
    warming = true;
    try {
      warmAllSymbols();
    } finally {
      warming = false;
    }
    return CACHE.size();
  }

  /** Reset cache (resource-pack reload). Releases all stored {@link NativeImage}s. */
  public static void reset() {
    for (NativeImage img : CACHE.values()) {
      try {
        img.close();
      } catch (Exception ignored) {
        // NativeImage#close swallows GL errors when no context is bound; we don't care.
      }
    }
    CACHE.clear();
    warmed = false;
    warming = false;
  }

  /** Cache size — exposed for tests / debug. */
  public static int cacheSize() {
    return CACHE.size();
  }

  // ---------------------------------------------------------------------
  // Internals
  // ---------------------------------------------------------------------

  private static String cacheKey(@Nullable IAgeSymbol symbol, @Nullable String word, int accent,
                                 boolean glyphsEnabled) {
    String id = symbol != null && symbol.getRegistryName() != null
        ? symbol.getRegistryName().toString()
        : "<null>";
    String w = word != null ? word.toLowerCase(java.util.Locale.ROOT) : "<null>";
    return id + "|" + w + "|" + Integer.toHexString(accent & 0xFFFFFF) + (glyphsEnabled ? "|g" : "|l");
  }

  /**
   * Renders a single tile. Uses {@link SymbolSeed#derive} for the seed
   * (deterministic across JVMs) and consults
   * {@link DrawableWordManager#getCuratedSeed} / {@code getCuratedColor}
   * (added in task 1.3) so pinned vocabulary keeps its hand-tuned look.
   *
   * <p>Datapack {@code display.glyph_seeds} pins (added in task 4.4) take
   * highest priority — a hero symbol can claim e.g. "Stone" with its
   * own seed even when the curated D'ni vocabulary already has a pin
   * for the same word.
   */
  @NotNull
  private static NativeImage render(@Nullable IAgeSymbol symbol, @Nullable String word,
                                    @NotNull SymbolPalette.Entry palette) {
    NativeImage image = new NativeImage(NativeImage.Format.RGBA, GLYPH_SIZE, GLYPH_SIZE, true);
    image.fillRect(0, 0, GLYPH_SIZE, GLYPH_SIZE, 0);

    // Letter-fallback escape hatch (config 5.2). Routes through the
    // hand-rolled 5x7 bitmap font in SymbolLetterFallback so the
    // fallback works on the daemon pre-warm thread (no GL context) as
    // well as on the main render thread.
    if (!MystcraftConfig.proceduralSymbolGlyphsEnabled.get()) {
      String label = labelFor(symbol, word);
      SymbolLetterFallback.renderInto(image, label, palette.accentColor(), palette.inkColor());
      return image;
    }

    // 1. Datapack hero pin (highest priority).
    SymbolDisplay display = symbol != null ? symbol.getDisplay() : null;
    Integer hardOverride = display != null ? display.seedFor(word) : null;

    // 2. Curated D'ni vocabulary pin.
    Integer pinnedSeed = DrawableWordManager.getCuratedSeed(word);

    // 3. Symbol mix (always applied so two distinct symbols pinning the same
    //    word still render distinctly).
    int symbolMix = SymbolSeed.derive(symbol != null ? symbol.getRegistryName() : null, word, 0);

    int seed;
    if (hardOverride != null) {
      seed = hardOverride.intValue() ^ symbolMix;
    } else if (pinnedSeed != null) {
      seed = pinnedSeed.intValue() ^ symbolMix;
    } else {
      seed = symbolMix;
    }

    // 4. Curated color pin (if any) — overrides palette accent for this tile.
    Integer pinnedColor = DrawableWordManager.getCuratedColor(word);
    int accent = pinnedColor != null ? (0xFF000000 | (pinnedColor.intValue() & 0xFFFFFF))
        : (0xFF000000 | (palette.accentColor() & 0xFFFFFF));
    int ink = 0xFF000000 | (palette.inkColor() & 0xFFFFFF);

    compose(image, seed, accent, ink);
    return image;
  }

  /**
   * Picks the most informative label for the letter fallback: prefers
   * the poem word (carries semantic meaning) but falls back to the
   * symbol's registry path so the fallback always shows something
   * meaningful even for orphan word lookups.
   */
  @Nullable
  private static String labelFor(@Nullable IAgeSymbol symbol, @Nullable String word) {
    if (word != null && !word.isBlank()) return word;
    if (symbol != null && symbol.getRegistryName() != null) {
      return symbol.getRegistryName().getPath();
    }
    return null;
  }

  /**
   * Primitive-selection DSL: picks 2-5 primitives from the weighted
   * table and applies each at a small jittered offset. Centre at
   * (32, 32). Each primitive consumes its own slice of the seed via
   * sub-stream salting so adding a primitive doesn't rotate the others.
   */
  private static void compose(@NotNull NativeImage image, int seed, int accent, int ink) {
    Random rng = new Random(((long) seed) << 32 | (seed & 0xFFFFFFFFL));
    int count = 2 + rng.nextInt(4); // 2-5 primitives
    int cx = GLYPH_SIZE / 2;
    int cy = GLYPH_SIZE / 2;

    for (int i = 0; i < count; i++) {
      Primitive p = PRIMITIVE_WEIGHTS[rng.nextInt(PRIMITIVE_WEIGHTS.length)];
      int radius = 8 + rng.nextInt(10);   // 8..17
      int offsetX = -4 + rng.nextInt(9);  // -4..4
      int offsetY = -4 + rng.nextInt(9);  // -4..4
      int subSeed = SymbolSeed.derive("primitive", p.ordinal() * 31 + i + seed);
      int color = (i % 2 == 0) ? accent : ink;
      applyPrimitive(image, p, cx + offsetX, cy + offsetY, radius, color, subSeed);
    }
  }

  private static void applyPrimitive(@NotNull NativeImage image, @NotNull Primitive p,
                                     int cx, int cy, int radius, int color, int seed) {
    switch (p) {
      case RUNIC_STROKE -> SymbolGlyphPrimitives.drawRunicStroke(image, cx, cy, radius, color, seed);
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
      case SPIRAL -> SymbolGlyphPrimitives.drawSpiral(image, cx, cy, radius, color, seed);
      case CROSSED_BARS -> SymbolGlyphPrimitives.drawCrossedBars(image, cx, cy, radius,
          (seed & 0x80000000) != 0, color, seed);
      case HATCHING -> {
        int strokes = 3 + ((seed >>> 12) & 0x3);
        SymbolGlyphPrimitives.drawHatching(image, cx, cy, radius, strokes, color, seed);
      }
      case CROWN -> SymbolGlyphPrimitives.drawCrown(image, cx, cy, radius, color, seed);
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
            // Already reached the cap — additional pre-warms would just thrash.
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
}
