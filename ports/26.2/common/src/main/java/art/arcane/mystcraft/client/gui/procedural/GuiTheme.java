package art.arcane.mystcraft.client.gui.procedural;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the color palette used by procedural UI rendering.
 * <p>
 * Defaults are baked in. Resource packs can override accent colors by shipping
 * <code>assets/mystcraft/gui/theme.json</code>:
 *
 * <pre>{@code
 * {
 *   "panel_bg": "#FFC6C6C6",
 *   "panel_border_dark": "#FF373737",
 *   "accent_ink": "#FF101030"
 * }
 * }</pre>
 * <p>
 * Colors are stored as ARGB ints. Use {@link #color(String)} to look up a value
 * with a default fallback.
 */
public final class GuiTheme {

  private static final Identifier THEME_LOCATION =
      Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/theme.json");

  private static final Map<String, Integer> DEFAULTS = buildDefaults();

  private static volatile Map<String, Integer> active = new HashMap<>(DEFAULTS);

  private GuiTheme() {
  }

  private static Map<String, Integer> buildDefaults() {
    Map<String, Integer> m = new HashMap<>();

    m.put("panel_bg", 0xFFC6C6C6);
    m.put("panel_border_dark", 0xFF373737);
    m.put("panel_border_light", 0xFFFFFFFF);
    m.put("slot_bg", 0xFF8B8B8B);
    m.put("slot_inner_shadow", 0xFF555555);

    m.put("title_bg", 0xFFA0A0A0);
    m.put("title_text", 0xFF202020);

    m.put("text_primary", 0xFF202020);
    m.put("text_secondary", 0xFF606060);
    m.put("text_muted", 0xFF888888);
    m.put("text_warning", 0xFFFF4444);
    m.put("text_warning_dark", 0xFFAA0000);
    m.put("text_ok", 0xFF40A040);

    m.put("ink_base", 0xFF101030);
    m.put("ink_basin_rim", 0xFF606080);

    m.put("page_bg", 0xFFF5F0E6);
    m.put("page_line", 0xFFDCD2C8);
    m.put("page_shadow", 0xFFC8BEB4);
    m.put("page_corner", 0xFF3C3228);

    m.put("cover_leather_base", 0xFF6B432A);
    m.put("cover_leather_dark", 0xFF3C2614);
    m.put("cover_leather_light", 0xFF8B5C36);
    m.put("cover_gold_border", 0xFFD4A845);
    m.put("cover_spine_shadow", 0xFF1A0E06);

    m.put("accent_link", 0xFF0040A0);
    m.put("accent_link_hover", 0xFF0060D0);
    return m;
  }

  /**
   * Loads the theme from the active resource pack, falling back to defaults for
   * any keys not present in the JSON.
   * <p>
   * If {@link MystcraftConfig#proceduralUiEnabled} is {@code false} the
   * resource-pack theme overrides are skipped and the active palette is forced
   * to the built-in defaults. This is the documented "fallback" behaviour:
   * server admins / players who dislike themed UIs can disable customisation
   * without affecting the procedural draw paths themselves (which have no PNG
   * fallback anymore).
   * <p>
   * Safe to call repeatedly; idempotent for the same resource state.
   */
  public static void reload() {
    Map<String, Integer> next = new HashMap<>(DEFAULTS);
    if (!MystcraftConfig.proceduralUiEnabled.get()) {
      active = next;
      return;
    }
    Minecraft mc = Minecraft.getInstance();
    if (mc == null) {
      active = next;
      return;
    }
    Optional<Resource> resource = mc.getResourceManager().getResource(THEME_LOCATION);
    if (resource.isEmpty()) {
      active = next;
      return;
    }
    try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!root.isJsonObject()) {
        Mystcraft.LOGGER.warn("[GuiTheme] theme.json is not a JSON object; ignoring");
        active = next;
        return;
      }
      JsonObject obj = root.getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
        Integer parsed = parseColor(entry.getValue().getAsString());
        if (parsed != null) {
          next.put(entry.getKey(), parsed);
        }
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[GuiTheme] Failed to load theme.json: {}", e.toString());
    }
    active = next;
  }

  /**
   * Returns the ARGB color for the given key, or the default if missing. Falls
   * back to bright magenta (0xFFFF00FF) if neither active nor defaults contain
   * the key, so missing keys are visually obvious in development.
   */
  public static int color(@NotNull String key) {
    Integer value = active.get(key);
    if (value == null) {
      value = DEFAULTS.get(key);
    }
    if (value == null) {
      Mystcraft.LOGGER.warn("[GuiTheme] Unknown color key: {}", key);
      return 0xFFFF00FF;
    }
    return value;
  }

  private static Integer parseColor(String raw) {
    if (raw == null) return null;
    String s = raw.trim();
    if (s.startsWith("#")) {
      s = s.substring(1);
    }
    try {
      long parsed = Long.parseLong(s, 16);
      if (s.length() == 6) {

        parsed |= 0xFF000000L;
      }
      return (int) parsed;
    } catch (NumberFormatException e) {
      Mystcraft.LOGGER.warn("[GuiTheme] Could not parse color '{}'", raw);
      return null;
    }
  }
}
