package art.arcane.mystcraft.world.gen.populate;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * Helper for reading optional populator parameters from datapack JSON.
 */
public final class PopulatorConfig {

    public static final int UNSET_INT = -999999;

    private PopulatorConfig() {
    }

    public static int getInt(JsonObject json, String key, int fallback) {
        if (json == null || !json.has(key)) {
            return fallback;
        }
        return GsonHelper.getAsInt(json, key, fallback);
    }

    public static float getFloat(JsonObject json, String key, float fallback) {
        if (json == null || !json.has(key)) {
            return fallback;
        }
        return GsonHelper.getAsFloat(json, key, fallback);
    }

    public static double getDouble(JsonObject json, String key, double fallback) {
        if (json == null || !json.has(key)) {
            return fallback;
        }
        return GsonHelper.getAsDouble(json, key, fallback);
    }

    public static boolean getBool(JsonObject json, String key, boolean fallback) {
        if (json == null || !json.has(key)) {
            return fallback;
        }
        return GsonHelper.getAsBoolean(json, key, fallback);
    }

    public static Integer getOptionalInt(JsonObject json, String key, int sentinel) {
        if (json == null || !json.has(key)) {
            return null;
        }
        int value = GsonHelper.getAsInt(json, key, sentinel);
        return value == sentinel ? null : value;
    }

    public static float chanceFrom(JsonObject json, float defaultChance, int defaultRarity) {
        if (json != null) {
            if (json.has("chance")) {
                return clampChance(GsonHelper.getAsFloat(json, "chance", defaultChance));
            }
            if (json.has("rarity")) {
                int rarity = Math.max(1, GsonHelper.getAsInt(json, "rarity", defaultRarity));
                return clampChance(1.0f / rarity);
            }
        }
        if (defaultChance > 0.0f) {
            return clampChance(defaultChance);
        }
        return clampChance(1.0f / Math.max(1, defaultRarity));
    }

    public static int rarityFrom(JsonObject json, int defaultRarity) {
        if (json != null && json.has("rarity")) {
            return Math.max(1, GsonHelper.getAsInt(json, "rarity", defaultRarity));
        }
        return Math.max(1, defaultRarity);
    }

    private static float clampChance(float chance) {
        if (chance < 0.0f) return 0.0f;
        if (chance > 1.0f) return 1.0f;
        return chance;
    }
}
