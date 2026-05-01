package art.arcane.mystcraft.client.render;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages the curated D'ni vocabulary used in Narayan Poems.
 * <p>
 * <b>Post-procedural-symbol-pages refactor</b>: words are pinned by
 * <em>(thematic colour, deterministic seed)</em> instead of sprite-atlas
 * indices. The procedural glyph generator in
 * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory}
 * consults {@link #getCuratedSeed(String)} and {@link #getCuratedColor(String)}
 * before falling back to a name-derived seed for novel words.
 * <p>
 * <b>Why pin?</b> Curated words like "Fire", "Water", "Forest" have
 * canonical visual identities in the original Mystcraft. Pinning the seed keeps
 * that identity stable across runs while letting unknown words remain
 * procedurally generated.
 * <p>
 * <b>How seeds were chosen</b>: each curated word's seed is the FNV-1a
 * hash of its name XOR a fixed nonce ({@link #SEED_NONCE}). The hash is
 * deterministic, has no special meaning, and avoids collisions for the 90+
 * words below. Colours were hand-picked per theme.
 */
public class DrawableWordManager {

  private static final int SEED_NONCE = 0x7E5E3C01;

  private static final Map<String, DrawableWord> WORDS = new HashMap<>();

  /**
   * Unmodifiable view of every registered word.
   */
  public static Map<String, DrawableWord> getWords() {
    return Collections.unmodifiableMap(WORDS);
  }

  /**
   * Registers a curated word with a thematic colour + seed pin.
   *
   * @param name  the word, case-insensitive
   * @param color thematic 0xRRGGBB colour pin
   * @param seed  deterministic seed (any int)
   */
  public static void registerWord(@NotNull String name, int color, int seed) {
    if (name.isEmpty()) return;
    WORDS.put(name.toLowerCase(Locale.ROOT), new DrawableWord(seed, color));
  }

  /**
   * Convenience overload — derives the seed from the word name (FNV-1a XOR
   * nonce). Use this when you only want to pin the colour.
   */
  public static void registerWord(@NotNull String name, int color) {
    registerWord(name, color, deriveSeed(name));
  }

  /**
   * Backwards-compatible wrapper kept for any external (third-party) caller.
   */
  public static void registerWord(@NotNull String name, @Nullable DrawableWord word) {
    if (word == null || name.isEmpty()) return;
    WORDS.putIfAbsent(name.toLowerCase(Locale.ROOT), word);
  }

  /**
   * Returns the {@link DrawableWord} for {@code name}.
   * <p>
   * If the word isn't curated, a fallback {@link DrawableWord} with a
   * name-derived seed (no colour pin) is created and cached so the same unknown
   * word always renders identically.
   */
  @NotNull
  public static DrawableWord getDrawableWord(@Nullable String name) {
    if (name == null) return WORDS.computeIfAbsent("<null>",
        k -> new DrawableWord(deriveSeed(k), null));
    String key = name.toLowerCase(Locale.ROOT);
    return WORDS.computeIfAbsent(key, k -> new DrawableWord(deriveSeed(k), null));
  }

  /**
   * Returns the curated seed pin for {@code word}, or {@code null} if the word
   * isn't in the curated vocabulary.
   * <p>
   * Used by {@code SymbolGlyphFactory} to honour pinned identities.
   */
  @Nullable
  public static Integer getCuratedSeed(@Nullable String word) {
    if (word == null) return null;
    DrawableWord w = WORDS.get(word.toLowerCase(Locale.ROOT));
    if (w == null) return null;
    int s = w.seed();
    return s == 0 ? null : Integer.valueOf(s);
  }

  /**
   * Returns the curated thematic colour pin for {@code word}, or {@code null}
   * if the word isn't pinned.
   */
  @Nullable
  public static Integer getCuratedColor(@Nullable String word) {
    if (word == null) return null;
    DrawableWord w = WORDS.get(word.toLowerCase(Locale.ROOT));
    return w == null ? null : w.pinnedColor();
  }

  /**
   * Resets the curated vocabulary — used by tests and resource-pack reloads.
   */
  public static void reset() {
    WORDS.clear();
  }

  /**
   * Initialises built-in D'ni words. Called from the loader-specific client
   * setup ({@code MystcraftFabricClient}, {@code MystcraftForge}).
   * <p>
   * Each entry pins a <em>thematic colour</em>; the seed is derived from the
   * name unless overridden. Colours are hand-picked, not procedural.
   */
  public static void initialize() {
    if (!WORDS.isEmpty()) return;

    registerWord("system", 0xC0B6A8);
    registerWord("motion", 0xD9A86C);
    registerWord("cycle", 0x8C9AAA);
    registerWord("form", 0xA8907E);
    registerWord("force", 0xC2452D);
    registerWord("change", 0x6F4E91);
    registerWord("flow", 0x4D8FB3);
    registerWord("order", 0xD4C58A);
    registerWord("chaos", 0x6E2C2C);
    registerWord("time", 0x5C4E7A);
    registerWord("space", 0x1F2645);
    registerWord("energy", 0xE6B040);
    registerWord("matter", 0x7C5C44);
    registerWord("void", 0x12101A);

    registerWord("terrain", 0x8E7A5A);
    registerWord("sky", 0x6FA4D9);
    registerWord("water", 0x2E6FA8);
    registerWord("fire", 0xC2452D);
    registerWord("earth", 0x6B4D2E);
    registerWord("air", 0xCCDCEA);
    registerWord("life", 0x6FA85A);
    registerWord("death", 0x2E2823);
    registerWord("growth", 0x4F8C3A);
    registerWord("decay", 0x4A4023);
    registerWord("light", 0xF1E6B3);
    registerWord("dark", 0x1A1820);
    registerWord("color", 0xB3779E);

    registerWord("image", 0xA8B6C2);
    registerWord("stimulate", 0xD49C49);
    registerWord("reflect", 0x9CB7C9);
    registerWord("radiate", 0xE6A847);

    registerWord("weather", 0x808FA0);
    registerWord("storm", 0x4A4F5C);
    registerWord("rain", 0x6F94B3);
    registerWord("snow", 0xE5ECF3);
    registerWord("wind", 0xB3C2CC);
    registerWord("cloud", 0xCFD6DC);
    registerWord("lightning", 0xF2E27A);
    registerWord("thunder", 0x6A5A8C);

    registerWord("biome", 0x7A8C5E);
    registerWord("forest", 0x3E6B2C);
    registerWord("desert", 0xC9A368);
    registerWord("ocean", 0x214A78);
    registerWord("mountain", 0x6E6E70);
    registerWord("plains", 0x9CB05C);
    registerWord("swamp", 0x4A5538);
    registerWord("jungle", 0x2C5520);
    registerWord("tundra", 0xB8C9CF);
    registerWord("cave", 0x3A2F26);

    registerWord("large", 0xC2B7A0);
    registerWord("small", 0xA89F8C);
    registerWord("dense", 0x4D4633);
    registerWord("sparse", 0xC4BFA4);
    registerWord("bright", 0xF7EDB8);
    registerWord("dim", 0x4A4533);
    registerWord("fast", 0xE0723C);
    registerWord("slow", 0x6A6E63);
    registerWord("normal", 0xB0A88C);
    registerWord("extreme", 0xC2452D);

    registerWord("village", 0xC0935A);
    registerWord("ruin", 0x7A6A52);
    registerWord("portal", 0x6F4E91);
    registerWord("tower", 0x9C8C7A);
    registerWord("dungeon", 0x3A2F2C);
    registerWord("library", 0x8C5E33);

    registerWord("red", 0xC2452D);
    registerWord("blue", 0x2E6FA8);
    registerWord("green", 0x4F8C3A);
    registerWord("yellow", 0xE6B040);
    registerWord("orange", 0xE0723C);
    registerWord("purple", 0x6F4E91);
    registerWord("white", 0xF1ECDD);
    registerWord("black", 0x1A1820);
    registerWord("gray", 0x8C8C8C);
    registerWord("brown", 0x6B4D2E);
    registerWord("pink", 0xD58FB3);
    registerWord("cyan", 0x4FA8B3);

    registerWord("random", 0xA8809C);
    registerWord("gradient", 0xCC9F6D);
    registerWord("standard", 0xB0A88C);
    registerWord("special", 0xE6B040);
    registerWord("instability", 0x6E2C2C);
    registerWord("stability", 0x4D7A8C);

    registerWord("easter", 0xE5C9F0);
  }

  private static int deriveSeed(@NotNull String name) {
    int h = 0x811C9DC5;
    String key = name.toLowerCase(Locale.ROOT);
    for (int i = 0; i < key.length(); i++) {
      h ^= key.charAt(i);
      h *= 0x01000193;
    }
    return h ^ SEED_NONCE;
  }
}
