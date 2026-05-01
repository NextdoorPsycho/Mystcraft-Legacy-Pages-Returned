package art.arcane.mystcraft.data;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for ink effects. Maps items to link property probabilities. When
 * items are added to ink, they have a chance to add properties to link panels.
 */
public final class InkEffects {

  private static final Map<String, PropertyColor> PROPERTY_COLORS = new HashMap<>();

  private static final Map<Item, Map<String, Float>> ITEM_EFFECTS = new HashMap<>();

  private static final Map<TagKey<Item>, Map<String, Float>> TAG_EFFECTS = new HashMap<>();
  private static boolean initialized = false;

  private InkEffects() {
  }

  /**
   * Initializes the ink effects registry. Should be called during mod setup.
   * <p>
   * Item-level effects are now data-driven via the {@link InkAffinity} datapack
   * loader (JSONs under {@code data/mystcraft/mystcraft/ink_affinity/}). This
   * method only registers the property colors used to render link panels and
   * the procedural Book UI.
   */
  public static void init() {
    if (initialized) return;
    initialized = true;

    registerPropertyColor(LinkFlags.INTRA_LINKING, new PropertyColor(0f, 1f, 0f));
    registerPropertyColor(LinkFlags.INTRA_LINKING_ONLY, new PropertyColor(1f, 1f, 1f));
    registerPropertyColor(LinkFlags.GENERATE_PLATFORM, new PropertyColor(0.5f, 0.5f, 0.5f));
    registerPropertyColor(LinkFlags.MAINTAIN_MOMENTUM, new PropertyColor(0f, 0f, 1f));
    registerPropertyColor(LinkFlags.DISARM, new PropertyColor(1f, 0f, 0f));
    registerPropertyColor(LinkFlags.RELATIVE, new PropertyColor(0.6f, 0f, 0.6f));
    registerPropertyColor(LinkFlags.FOLLOWING, new PropertyColor(1f, 0.5f, 0f));
  }

  /**
   * Registers a color for a link property.
   */
  public static void registerPropertyColor(String property, PropertyColor color) {
    PROPERTY_COLORS.put(property, color);
  }

  /**
   * Gets all registered property names.
   */
  public static Set<String> getProperties() {
    return Collections.unmodifiableSet(PROPERTY_COLORS.keySet());
  }

  /**
   * Gets the color for a property.
   */
  @Nullable
  public static PropertyColor getPropertyColor(String property) {
    return PROPERTY_COLORS.get(property);
  }

  /**
   * Gets the RGB color for a property.
   */
  public static int getPropertyColorRGB(String property) {
    PropertyColor color = PROPERTY_COLORS.get(property);
    return color != null ? color.toRGB() : 0xFFFFFF;
  }

  /**
   * Gets the localized name for a property.
   */
  @NotNull
  public static String getLocalizedName(String property) {
    String key = getTranslationKey(property);
    if (I18n.exists(key)) {
      return I18n.get(key);
    }

    return formatPropertyName(property);
  }

  /**
   * Gets the translation key for a property.
   */
  @NotNull
  public static String getTranslationKey(String property) {
    return "linkeffect.mystcraft." + property.toLowerCase().replace(' ', '_');
  }

  @NotNull
  private static String formatPropertyName(String property) {
    if (property == null || property.isEmpty()) return "";
    StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;
    for (char c : property.toCharArray()) {
      if (Character.isLetter(c)) {
        if (capitalizeNext) {
          result.append(Character.toUpperCase(c));
          capitalizeNext = false;
        } else {
          result.append(c);
        }
      } else {
        result.append(' ');
        capitalizeNext = true;
      }
    }
    return result.toString().trim();
  }

  /**
   * Adds a property probability to an item.
   */
  public static void addPropertyToItem(Item item, String property, float probability) {
    Map<String, Float> itemMap = ITEM_EFFECTS.computeIfAbsent(item, k -> new HashMap<>());
    Float existing = itemMap.get(property);
    float newProb = probability + (existing != null ? existing : 0f);
    itemMap.put(property, newProb);
    validateProbabilities(itemMap, item.toString());
  }

  private static void validateProbabilities(Map<String, Float> probMap, String source) {
    float total = 0f;
    for (Float f : probMap.values()) {
      total += f;
    }
    if (total > 1.0f) {
      throw new IllegalStateException(
          "Total ink property probabilities from " + source + " exceed 1.0: " + total);
    }
  }

  /**
   * Gets the property effects for an item.
   *
   * @param stack The item stack
   * @return Map of property names to probabilities, or null if no effects
   */
  @Nullable
  public static Map<String, Float> getItemEffects(ItemStack stack) {
    if (stack.isEmpty()) return null;

    Map<String, Float> effects = ITEM_EFFECTS.get(stack.getItem());
    if (effects != null && !effects.isEmpty()) {
      return Collections.unmodifiableMap(effects);
    }

    for (Map.Entry<TagKey<Item>, Map<String, Float>> entry : TAG_EFFECTS.entrySet()) {
      if (stack.is(entry.getKey()) && !entry.getValue().isEmpty()) {
        return Collections.unmodifiableMap(entry.getValue());
      }
    }

    InkAffinity.Entry affinity = InkAffinity.getAffinity(stack);
    Map<String, Float> linkEffects = affinity.linkPropertyWeights();
    if (linkEffects != null && !linkEffects.isEmpty()) {
      return Collections.unmodifiableMap(linkEffects);
    }

    return null;
  }

  /**
   * Checks if an item has any ink effects.
   */
  public static boolean hasEffects(ItemStack stack) {
    return getItemEffects(stack) != null;
  }

  /**
   * Represents a color for a link property.
   */
  public record PropertyColor(float r, float g, float b) {
    public int toRGB() {
      int ri = (int) (r * 255) & 0xFF;
      int gi = (int) (g * 255) & 0xFF;
      int bi = (int) (b * 255) & 0xFF;
      return (ri << 16) | (gi << 8) | bi;
    }
  }

}
