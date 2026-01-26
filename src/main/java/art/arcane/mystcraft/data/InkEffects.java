package art.arcane.mystcraft.data;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for ink effects.
 * Maps items to link property probabilities.
 * When items are added to ink, they have a chance to add properties to link panels.
 */
public final class InkEffects {

    private InkEffects() {
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

    // Property colors for each link flag
    private static final Map<String, PropertyColor> PROPERTY_COLORS = new HashMap<>();

    // Item -> (Property -> Probability) mappings
    private static final Map<Item, Map<String, Float>> ITEM_EFFECTS = new HashMap<>();

    // Tag -> (Property -> Probability) mappings
    private static final Map<TagKey<Item>, Map<String, Float>> TAG_EFFECTS = new HashMap<>();

    private static boolean initialized = false;

    /**
     * Initializes the ink effects registry.
     * Should be called during mod setup.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register property colors
        registerPropertyColor(LinkFlags.INTRA_LINKING, new PropertyColor(0f, 1f, 0f));          // Green
        registerPropertyColor(LinkFlags.INTRA_LINKING_ONLY, new PropertyColor(1f, 1f, 1f));     // White
        registerPropertyColor(LinkFlags.GENERATE_PLATFORM, new PropertyColor(0.5f, 0.5f, 0.5f)); // Gray
        registerPropertyColor(LinkFlags.MAINTAIN_MOMENTUM, new PropertyColor(0f, 0f, 1f));       // Blue
        registerPropertyColor(LinkFlags.DISARM, new PropertyColor(1f, 0f, 0f));                  // Red
        registerPropertyColor(LinkFlags.RELATIVE, new PropertyColor(0.6f, 0f, 0.6f));            // Purple
        registerPropertyColor(LinkFlags.FOLLOWING, new PropertyColor(1f, 0.5f, 0f));             // Orange

        // Register item effects
        // Gunpowder - Disarm
        addPropertyToItem(Items.GUNPOWDER, LinkFlags.DISARM, 0.2f);

        // Mushroom Stew - Disarm (weak)
        addPropertyToItem(Items.MUSHROOM_STEW, LinkFlags.DISARM, 0.05f);

        // Clay Ball - Generate Platform
        addPropertyToItem(Items.CLAY_BALL, LinkFlags.GENERATE_PLATFORM, 0.25f);

        // Experience Bottle - Intra-Linking
        addPropertyToItem(Items.EXPERIENCE_BOTTLE, LinkFlags.INTRA_LINKING, 0.15f);

        // Ender Pearl - Intra-Linking + Disarm
        addPropertyToItem(Items.ENDER_PEARL, LinkFlags.INTRA_LINKING, 0.15f);
        addPropertyToItem(Items.ENDER_PEARL, LinkFlags.DISARM, 0.15f);

        // Feather - Maintain Momentum
        addPropertyToItem(Items.FEATHER, LinkFlags.MAINTAIN_MOMENTUM, 0.15f);

        // Fire Charge - Disarm
        addPropertyToItem(Items.FIRE_CHARGE, LinkFlags.DISARM, 0.25f);

        // Ghast Tear - Relative
        addPropertyToItem(Items.GHAST_TEAR, LinkFlags.RELATIVE, 0.15f);

        // Slime Ball - Maintain Momentum
        addPropertyToItem(Items.SLIME_BALL, LinkFlags.MAINTAIN_MOMENTUM, 0.1f);

        // Magma Cream - Following
        addPropertyToItem(Items.MAGMA_CREAM, LinkFlags.FOLLOWING, 0.2f);

        // Blaze Powder - Disarm + Following
        addPropertyToItem(Items.BLAZE_POWDER, LinkFlags.DISARM, 0.1f);
        addPropertyToItem(Items.BLAZE_POWDER, LinkFlags.FOLLOWING, 0.1f);

        // Eye of Ender - Intra-Linking + Following
        addPropertyToItem(Items.ENDER_EYE, LinkFlags.INTRA_LINKING, 0.2f);
        addPropertyToItem(Items.ENDER_EYE, LinkFlags.FOLLOWING, 0.15f);

        // Chorus Fruit - Relative
        addPropertyToItem(Items.CHORUS_FRUIT, LinkFlags.RELATIVE, 0.2f);

        // Prismarine Crystals - Generate Platform
        addPropertyToItem(Items.PRISMARINE_CRYSTALS, LinkFlags.GENERATE_PLATFORM, 0.2f);

        // Nether Star - All effects (rare!)
        addPropertyToItem(Items.NETHER_STAR, LinkFlags.INTRA_LINKING, 0.1f);
        addPropertyToItem(Items.NETHER_STAR, LinkFlags.GENERATE_PLATFORM, 0.1f);
        addPropertyToItem(Items.NETHER_STAR, LinkFlags.MAINTAIN_MOMENTUM, 0.1f);
        addPropertyToItem(Items.NETHER_STAR, LinkFlags.FOLLOWING, 0.1f);

        // Gold ingots - Intra-Linking + Generate Platform
        addPropertyToItem(Items.GOLD_INGOT, LinkFlags.INTRA_LINKING, 0.1f);
        addPropertyToItem(Items.GOLD_INGOT, LinkFlags.GENERATE_PLATFORM, 0.05f);

        // Diamond - Multiple weak effects
        addPropertyToItem(Items.DIAMOND, LinkFlags.INTRA_LINKING, 0.1f);
        addPropertyToItem(Items.DIAMOND, LinkFlags.MAINTAIN_MOMENTUM, 0.05f);
        addPropertyToItem(Items.DIAMOND, LinkFlags.GENERATE_PLATFORM, 0.05f);

        // Emerald - Following
        addPropertyToItem(Items.EMERALD, LinkFlags.FOLLOWING, 0.15f);

        // Amethyst - Relative
        addPropertyToItem(Items.AMETHYST_SHARD, LinkFlags.RELATIVE, 0.1f);

        // Echo Shard - Relative + Intra-Linking
        addPropertyToItem(Items.ECHO_SHARD, LinkFlags.RELATIVE, 0.15f);
        addPropertyToItem(Items.ECHO_SHARD, LinkFlags.INTRA_LINKING, 0.15f);
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
        // Fallback to formatted property name
        return formatPropertyName(property);
    }

    /**
     * Gets the translation key for a property.
     */
    @NotNull
    public static String getTranslationKey(String property) {
        return "linkeffect.mystcraft." + property.toLowerCase().replace(' ', '_');
    }

    /**
     * Formats a property name for display (fallback when no translation exists).
     */
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

    /**
     * Adds a property probability to items matching a tag.
     */
    public static void addPropertyToTag(TagKey<Item> tag, String property, float probability) {
        Map<String, Float> tagMap = TAG_EFFECTS.computeIfAbsent(tag, k -> new HashMap<>());
        Float existing = tagMap.get(property);
        float newProb = probability + (existing != null ? existing : 0f);
        tagMap.put(property, newProb);
        validateProbabilities(tagMap, tag.location().toString());
    }

    /**
     * Validates that probabilities don't exceed 1.0.
     */
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

        // Check direct item mapping first
        Map<String, Float> effects = ITEM_EFFECTS.get(stack.getItem());
        if (effects != null) {
            return Collections.unmodifiableMap(effects);
        }

        // Check tag mappings
        for (Map.Entry<TagKey<Item>, Map<String, Float>> entry : TAG_EFFECTS.entrySet()) {
            if (stack.is(entry.getKey())) {
                return Collections.unmodifiableMap(entry.getValue());
            }
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
     * Gets all items that have registered effects.
     */
    public static Set<Item> getEffectItems() {
        return Collections.unmodifiableSet(ITEM_EFFECTS.keySet());
    }
}
