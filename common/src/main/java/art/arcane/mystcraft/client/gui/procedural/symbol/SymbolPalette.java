package art.arcane.mystcraft.client.gui.procedural.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-{@link SymbolCategory} color palette + default motif used by the
 * procedural symbol-page renderer.
 * <p>
 * Defaults are baked in (matching the plan's §5.3.4 table). Resource packs can
 * override per-category palettes by shipping
 * <code>assets/mystcraft/gui/symbol_palette.json</code>:
 *
 * <pre>{@code
 * {
 *   "biome": {
 *     "base":   "#FF4A8A4D",
 *     "accent": "#FF254F2A",
 *     "ink":    "#FF0E2A12",
 *     "halo":   "#FF9CE19F"
 *   }
 * }
 * }</pre>
 * <p>
 * Unspecified keys keep their built-in default. The optional {@code motif}
 * field in a JSON entry can re-bind the default motif for the category.
 * <p>
 * Symbol-level overrides (per-symbol motif / palette pin) live on the datapack
 * {@code display} block (phase 4) and resolve through the same lookup path via
 * {@link #styleFor}.
 */
public final class SymbolPalette {

  private static final ResourceLocation PALETTE_LOCATION =
      new ResourceLocation(Mystcraft.MOD_ID, "gui/symbol_palette.json");
  private static final Map<SymbolCategory, Entry> DEFAULTS = buildDefaults();
  private static final Entry FALLBACK_ENTRY = new Entry(
      0x707070, 0x404040, 0x141414, 0xC0C0C0, SymbolMotif.DIAMOND);
  private static volatile Map<SymbolCategory, Entry> active = new EnumMap<>(DEFAULTS);

  private SymbolPalette() {
  }

  private static Map<SymbolCategory, Entry> buildDefaults() {
    EnumMap<SymbolCategory, Entry> m = new EnumMap<>(SymbolCategory.class);

    for (SymbolCategory c : SymbolCategory.values()) {
      m.put(c, paletteFor(c));
    }
    return m;
  }

  private static Entry paletteFor(@NotNull SymbolCategory c) {
    SymbolMotif motif = SymbolMotif.defaultFor(c);
    return switch (c) {
      case TERRAIN -> new Entry(0x6B4F2C, 0x3A2614, 0x1A0F08, 0xC79A4D, motif);
      case BIOME_CONTROLLER ->
          new Entry(0x2F5A3E, 0x1B3725, 0x081A12, 0x86C97A, motif);
      case BIOME -> new Entry(0x4A8A4D, 0x254F2A, 0x0E2A12, 0x9CE19F, motif);
      case WEATHER -> new Entry(0x4A6A8C, 0x223A56, 0x0C1A2A, 0xB8D4F0, motif);
      case LIGHTING -> new Entry(0xC9A055, 0x6E521D, 0x2C1F08, 0xFFE9A8, motif);
      case COLOR -> new Entry(0x8C5C9C, 0x4F2C5E, 0x1F0E26, 0xE6BFFA, motif);
      case VISUAL_EFFECT ->
          new Entry(0xC95A8C, 0x712C4F, 0x2C0E1F, 0xFAB0D2, motif);
      case ENVIRONMENT ->
          new Entry(0x603F8C, 0x331A56, 0x0E0826, 0xC9B0FA, motif);
      case FEATURE_LARGE ->
          new Entry(0x8E5A2C, 0x4F2E12, 0x1F0E04, 0xE6B080, motif);
      case FEATURE_MEDIUM ->
          new Entry(0xA07033, 0x583A14, 0x241405, 0xF0C58E, motif);
      case FEATURE_SMALL ->
          new Entry(0xB39764, 0x6E5A2C, 0x2C2008, 0xF7E4B0, motif);
      case STRUCTURE ->
          new Entry(0x35499D, 0x1A256B, 0x080F2A, 0xA8B7FA, motif);
      case ANGLE -> new Entry(0x9D3535, 0x6B1A1A, 0x2A0808, 0xFAA8A8, motif);
      case PHASE -> new Entry(0x1F2A6B, 0x0E1538, 0x05081C, 0x8E9CFA, motif);
      case LENGTH -> new Entry(0x4A357A, 0x261A4F, 0x0E0826, 0xB0A8FA, motif);
      case SEA -> new Entry(0x2A8C8C, 0x144F4F, 0x051F1F, 0x80E6E6, motif);
      case MODIFIER -> new Entry(0x707070, 0x404040, 0x141414, 0xC0C0C0, motif);
      case SPECIAL -> new Entry(0xC79A4D, 0x6E521D, 0x2C1F08, 0xFFE9A8, motif);
    };
  }

  /**
   * Looks up the palette entry for the given category. Returns the built-in
   * fallback (neutral grey + DIAMOND motif) when the category is null or has no
   * registered entry.
   */
  @NotNull
  public static Entry get(@Nullable SymbolCategory category) {
    if (category == null) return FALLBACK_ENTRY;
    Entry entry = active.get(category);
    return entry != null ? entry : FALLBACK_ENTRY;
  }

  /**
   * Loads palette overrides from the active resource pack. Missing or malformed
   * entries fall back to the baked-in defaults so generation never fails. Safe
   * to call repeatedly; idempotent for the same resource state.
   */
  public static void reload() {
    Map<SymbolCategory, Entry> next = new EnumMap<>(DEFAULTS);
    Minecraft mc = Minecraft.getInstance();
    if (mc == null) {
      active = next;
      return;
    }
    Optional<Resource> resource = mc.getResourceManager().getResource(PALETTE_LOCATION);
    if (resource.isEmpty()) {
      active = next;
      return;
    }
    try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!root.isJsonObject()) {
        Mystcraft.LOGGER.warn("[SymbolPalette] symbol_palette.json is not a JSON object; ignoring");
        active = next;
        return;
      }
      JsonObject obj = root.getAsJsonObject();
      for (Map.Entry<String, JsonElement> categoryEntry : obj.entrySet()) {
        SymbolCategory category = SymbolCategory.fromName(categoryEntry.getKey());
        if (category == null) {
          Mystcraft.LOGGER.warn("[SymbolPalette] Unknown category '{}' in symbol_palette.json", categoryEntry.getKey());
          continue;
        }
        if (!categoryEntry.getValue().isJsonObject()) {
          Mystcraft.LOGGER.warn("[SymbolPalette] Category '{}' override is not a JSON object", categoryEntry.getKey());
          continue;
        }
        Entry merged = mergeEntry(next.get(category), categoryEntry.getValue().getAsJsonObject());
        if (merged != null) {
          next.put(category, merged);
        }
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[SymbolPalette] Failed to load symbol_palette.json: {}", e.toString());
    }
    active = next;
  }

  @Nullable
  private static Entry mergeEntry(@Nullable Entry base, @NotNull JsonObject obj) {
    if (base == null) {
      base = FALLBACK_ENTRY;
    }
    Integer baseColor = parseColor(obj, "base");
    Integer accentColor = parseColor(obj, "accent");
    Integer inkColor = parseColor(obj, "ink");
    Integer haloColor = parseColor(obj, "halo");
    SymbolMotif motif = parseMotif(obj, "motif");
    return base.merge(baseColor, accentColor, inkColor, haloColor, motif);
  }

  @Nullable
  private static Integer parseColor(@NotNull JsonObject obj, @NotNull String key) {
    if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) return null;
    String raw = obj.get(key).getAsString();
    if (raw == null) return null;
    String s = raw.trim();
    if (s.startsWith("#")) {
      s = s.substring(1);
    }
    try {
      long parsed = Long.parseLong(s, 16);

      return (int) (parsed & 0xFFFFFFL);
    } catch (NumberFormatException e) {
      Mystcraft.LOGGER.warn("[SymbolPalette] Could not parse color '{}' for key '{}'", raw, key);
      return null;
    }
  }

  @Nullable
  private static SymbolMotif parseMotif(@NotNull JsonObject obj, @NotNull String key) {
    if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) return null;
    String raw = obj.get(key).getAsString();
    SymbolMotif resolved = SymbolMotif.fromName(raw);
    if (resolved == null) {
      Mystcraft.LOGGER.warn("[SymbolPalette] Unknown motif '{}' for key '{}'", raw, key);
    }
    return resolved;
  }

  /**
   * Immutable per-category palette descriptor.
   *
   * @param baseColor    Page-edge tint, multiplied with the parchment.
   * @param accentColor  Primary glyph body color.
   * @param inkColor     Glyph shadow / cap dots / detail strokes.
   * @param haloColor    Rank-5 halo + warning halo for high-instability
   *                     symbols.
   * @param defaultMotif Layout template for symbols in this category.
   */
  public record Entry(int baseColor, int accentColor, int inkColor,
                      int haloColor, SymbolMotif defaultMotif) {

    public Entry {

      baseColor &= 0xFFFFFF;
      accentColor &= 0xFFFFFF;
      inkColor &= 0xFFFFFF;
      haloColor &= 0xFFFFFF;
      if (defaultMotif == null) defaultMotif = SymbolMotif.DIAMOND;
    }

    /**
     * Returns a copy with selected fields replaced; nulls keep current values.
     */
    public Entry merge(@Nullable Integer base, @Nullable Integer accent, @Nullable Integer ink,
                       @Nullable Integer halo, @Nullable SymbolMotif motif) {
      return new Entry(
          base != null ? base : baseColor,
          accent != null ? accent : accentColor,
          ink != null ? ink : inkColor,
          halo != null ? halo : haloColor,
          motif != null ? motif : defaultMotif
      );
    }
  }
}
