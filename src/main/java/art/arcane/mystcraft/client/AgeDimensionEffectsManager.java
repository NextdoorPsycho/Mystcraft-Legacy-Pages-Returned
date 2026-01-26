package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages registration of custom DimensionSpecialEffects for Mystcraft Ages.
 *
 * In 1.20.2, DimensionSpecialEffects are stored in a static map indexed by ResourceLocation.
 * This manager injects Mystcraft age effects into that map dynamically.
 */
@OnlyIn(Dist.CLIENT)
public class AgeDimensionEffectsManager {

    private static final Map<Integer, AgeDimensionSpecialEffects> AGE_EFFECTS = new ConcurrentHashMap<>();

    // Cached reflection field for DimensionSpecialEffects.EFFECTS
    private static Object2ObjectMap<ResourceLocation, DimensionSpecialEffects> effectsMap;
    private static boolean reflectionInitialized = false;

    /**
     * Initializes the reflection access to the DimensionSpecialEffects map.
     */
    @SuppressWarnings("unchecked")
    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            // Find the EFFECTS field in DimensionSpecialEffects
            Field effectsField = null;
            for (Field field : DimensionSpecialEffects.class.getDeclaredFields()) {
                if (field.getType().getName().contains("Object2ObjectMap")) {
                    effectsField = field;
                    break;
                }
            }

            if (effectsField == null) {
                // Try looking for a Map type field
                for (Field field : DimensionSpecialEffects.class.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        effectsField = field;
                        break;
                    }
                }
            }

            if (effectsField != null) {
                effectsField.setAccessible(true);
                effectsMap = (Object2ObjectMap<ResourceLocation, DimensionSpecialEffects>) effectsField.get(null);
                Mystcraft.LOGGER.info("Successfully accessed DimensionSpecialEffects map for custom sky rendering");
            } else {
                Mystcraft.LOGGER.warn("Could not find DimensionSpecialEffects map field - custom sky rendering may not work");
            }
        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to access DimensionSpecialEffects map", e);
        }
    }

    /**
     * Gets or creates the DimensionSpecialEffects for an age.
     *
     * @param ageUID The age UID
     * @return The effects instance
     */
    public static AgeDimensionSpecialEffects getOrCreateEffects(int ageUID) {
        return AGE_EFFECTS.computeIfAbsent(ageUID, AgeDimensionSpecialEffects::new);
    }

    /**
     * Registers effects for an age in the vanilla effects map.
     * Call this when an age is loaded on the client.
     *
     * @param ageUID The age UID
     */
    public static void registerAgeEffects(int ageUID) {
        initReflection();

        if (effectsMap == null) {
            Mystcraft.LOGGER.warn("Cannot register age effects - DimensionSpecialEffects map not accessible");
            return;
        }

        ResourceLocation ageDimLocation = new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_age_" + ageUID);
        AgeDimensionSpecialEffects effects = getOrCreateEffects(ageUID);

        effectsMap.put(ageDimLocation, effects);
        Mystcraft.LOGGER.debug("Registered DimensionSpecialEffects for age {}", ageUID);
    }

    /**
     * Unregisters effects for an age.
     * Call this when an age is unloaded on the client.
     *
     * @param ageUID The age UID
     */
    public static void unregisterAgeEffects(int ageUID) {
        if (effectsMap == null) return;

        ResourceLocation ageDimLocation = new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_age_" + ageUID);
        effectsMap.remove(ageDimLocation);
        AGE_EFFECTS.remove(ageUID);

        Mystcraft.LOGGER.debug("Unregistered DimensionSpecialEffects for age {}", ageUID);
    }

    /**
     * Clears all registered age effects.
     * Call this on disconnect or world unload.
     */
    public static void clearAll() {
        if (effectsMap != null) {
            for (int ageUID : AGE_EFFECTS.keySet()) {
                ResourceLocation ageDimLocation = new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_age_" + ageUID);
                effectsMap.remove(ageDimLocation);
            }
        }
        AGE_EFFECTS.clear();
        Mystcraft.LOGGER.debug("Cleared all age dimension effects");
    }

    /**
     * Gets the effects for an age if registered.
     *
     * @param ageUID The age UID
     * @return The effects, or null if not registered
     */
    public static AgeDimensionSpecialEffects getEffects(int ageUID) {
        return AGE_EFFECTS.get(ageUID);
    }
}
