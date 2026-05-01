package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Optional per-symbol presentation override loaded from datapack JSON. Pack
 * authors point hero symbols at a non-default motif, override specific palette
 * swatches, or pin curated D'ni words to a chosen seed value while leaving the
 * other 500+ symbols on the auto-generated path.
 *
 * <p>Schema (all fields optional):
 * <pre>
 *   "display": {
 *     "motif": "wreath",
 *     "palette_override": {
 *       "base":   "#FF6B4F2C",
 *       "accent": "#FFE8B070",
 *       "ink":    "#FF1A0F08",
 *       "halo":   "#FFFFD9A0"
 *     },
 *     "glyph_seeds": {
 *       "Dripstone": -889275714
 *     }
 *   }
 * </pre>
 * Color fields accept 8-digit ARGB or 6-digit RGB hex strings. Glyph seeds
 * accept signed int32 values or hex strings such as {@code "0xCAFEBABE"}.
 * <p>
 * Resolution happens at render time:
 * <ul>
 *   <li>{@link #motifName()} — looked up via
 *       {@code SymbolMotif.byName}; falls back to category default if
 *       the name doesn't resolve.</li>
 *   <li>{@link #paletteOverride()} — only the keys present in the JSON
 *       replace the corresponding {@code SymbolPalette.Entry} fields.</li>
 *   <li>{@link #glyphSeeds()} — XOR'd into the symbol-mix seed for the
 *       matching word, bypassing the curated D'ni vocabulary pin so a
 *       hero symbol can claim e.g. "Stone" for itself.</li>
 * </ul>
 */
public record SymbolDisplay(@Nullable String motifName,
                            @Nullable PaletteOverride paletteOverride,
                            @NotNull Map<String, Integer> glyphSeeds) {

  /**
   * Empty-but-valid display block — convenience for callers that need a
   * non-null.
   */
  public static final SymbolDisplay EMPTY =
      new SymbolDisplay(null, null, Collections.emptyMap());

  /**
   * Parses a {@code display} JSON object. Returns {@code null} when the block
   * is structurally invalid (missing object, etc.) so callers can cleanly fall
   * back to category defaults. Logs a warning for individual field failures
   * rather than aborting the whole symbol load.
   */
  @Nullable
  public static SymbolDisplay fromJson(@NotNull ResourceLocation symbolId, @Nullable JsonElement element) {
    if (element == null || !element.isJsonObject()) {
      return null;
    }
    JsonObject json = element.getAsJsonObject();

    String motifName = null;
    if (json.has("motif")) {
      motifName = GsonHelper.getAsString(json, "motif", null);
      if (motifName != null && motifName.isBlank()) {
        motifName = null;
      }
    }

    PaletteOverride paletteOverride = null;
    if (json.has("palette_override")) {
      JsonElement po = json.get("palette_override");
      if (po.isJsonObject()) {
        paletteOverride = parsePaletteOverride(symbolId, po.getAsJsonObject());
        if (paletteOverride != null && !paletteOverride.hasAny()) {
          paletteOverride = null;
        }
      }
    }

    Map<String, Integer> glyphSeeds = Collections.emptyMap();
    if (json.has("glyph_seeds")) {
      JsonElement gs = json.get("glyph_seeds");
      if (gs.isJsonObject()) {
        glyphSeeds = parseGlyphSeeds(symbolId, gs.getAsJsonObject());
      }
    }

    if (motifName == null && paletteOverride == null && glyphSeeds.isEmpty()) {

      return null;
    }

    return new SymbolDisplay(motifName, paletteOverride, glyphSeeds);
  }

  @Nullable
  private static PaletteOverride parsePaletteOverride(@NotNull ResourceLocation symbolId, @NotNull JsonObject json) {
    Integer base = parseColor(symbolId, json, "base");
    Integer accent = parseColor(symbolId, json, "accent");
    Integer ink = parseColor(symbolId, json, "ink");
    Integer halo = parseColor(symbolId, json, "halo");
    return new PaletteOverride(base, accent, ink, halo);
  }

  @Nullable
  private static Integer parseColor(@NotNull ResourceLocation symbolId,
                                    @NotNull JsonObject json, @NotNull String key) {
    if (!json.has(key)) return null;
    JsonElement element = json.get(key);
    if (!element.isJsonPrimitive()) {
      Mystcraft.LOGGER.warn("[SymbolDisplay] {} display.palette_override.{} must be a hex string, got {}",
          symbolId, key, element);
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (primitive.isString()) {
      String raw = primitive.getAsString().trim();
      Integer parsed = parseHexColor(raw);
      if (parsed == null) {
        Mystcraft.LOGGER.warn("[SymbolDisplay] {} display.palette_override.{} invalid hex color: {}",
            symbolId, key, raw);
      }
      return parsed;
    }
    if (primitive.isNumber()) {

      return primitive.getAsInt();
    }
    Mystcraft.LOGGER.warn("[SymbolDisplay] {} display.palette_override.{} unsupported value: {}",
        symbolId, key, primitive);
    return null;
  }

  /**
   * Parses {@code "#RRGGBB"}, {@code "#AARRGGBB"}, or {@code "RRGGBB"} /
   * {@code "AARRGGBB"} (without leading {@code #}). Returns {@code null} on
   * malformed input.
   */
  @Nullable
  static Integer parseHexColor(@NotNull String raw) {
    String s = raw.startsWith("#") ? raw.substring(1) : raw;
    if (s.length() != 6 && s.length() != 8) return null;
    try {
      long parsed = Long.parseLong(s, 16);
      if (s.length() == 6) {

        return (int) (0xFF000000L | parsed);
      }
      return (int) parsed;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @NotNull
  private static Map<String, Integer> parseGlyphSeeds(@NotNull ResourceLocation symbolId, @NotNull JsonObject json) {
    Map<String, Integer> out = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
      String word = entry.getKey();
      JsonElement value = entry.getValue();
      Integer seed = parseSeedValue(symbolId, word, value);
      if (seed != null) {
        out.put(word, seed);
      }
    }
    return out;
  }

  @Nullable
  private static Integer parseSeedValue(@NotNull ResourceLocation symbolId,
                                        @NotNull String word, @Nullable JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      Mystcraft.LOGGER.warn("[SymbolDisplay] {} display.glyph_seeds.{} must be a number or hex string, got {}",
          symbolId, word, element);
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (primitive.isNumber()) {
      return primitive.getAsInt();
    }
    if (primitive.isString()) {
      String raw = primitive.getAsString().trim();
      String normalised = raw.toLowerCase(Locale.ROOT);
      try {
        if (normalised.startsWith("0x")) {
          return (int) Long.parseLong(normalised.substring(2), 16);
        }
        if (normalised.startsWith("#")) {
          return (int) Long.parseLong(normalised.substring(1), 16);
        }
        return Integer.parseInt(raw);
      } catch (NumberFormatException e) {
        Mystcraft.LOGGER.warn("[SymbolDisplay] {} display.glyph_seeds.{} invalid integer: {}",
            symbolId, word, raw);
        return null;
      }
    }
    Mystcraft.LOGGER.warn("[SymbolDisplay] {} display.glyph_seeds.{} unsupported value: {}",
        symbolId, word, primitive);
    return null;
  }

  /**
   * Returns the seed override for {@code word}, or {@code null} if unset.
   */
  @Nullable
  public Integer seedFor(@Nullable String word) {
    if (word == null || glyphSeeds.isEmpty()) return null;
    Integer pinned = glyphSeeds.get(word);
    if (pinned != null) return pinned;

    for (Map.Entry<String, Integer> e : glyphSeeds.entrySet()) {
      if (e.getKey().equalsIgnoreCase(word)) {
        return e.getValue();
      }
    }
    return null;
  }

  /**
   * Per-channel palette override. Each field is nullable so pack authors can
   * override e.g. just the accent without touching the base / ink / halo. Color
   * values are stored as ARGB ints; alpha defaults to {@code 0xFF} when the
   * JSON value is 6-digit RGB.
   */
  public record PaletteOverride(@Nullable Integer base,
                                @Nullable Integer accent,
                                @Nullable Integer ink,
                                @Nullable Integer halo) {

    /**
     * True when at least one override channel is non-null.
     */
    public boolean hasAny() {
      return base != null || accent != null || ink != null || halo != null;
    }
  }
}
