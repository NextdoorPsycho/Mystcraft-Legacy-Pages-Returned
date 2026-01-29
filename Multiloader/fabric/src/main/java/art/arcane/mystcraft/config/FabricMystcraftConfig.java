package art.arcane.mystcraft.config;

import art.arcane.mystcraft.Mystcraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fabric-side configuration for Mystcraft.
 * Uses JSON file storage since Fabric has no ForgeConfigSpec equivalent.
 * Fields expose .get() methods to match the ForgeConfigSpec API used by common code.
 */
public class FabricMystcraftConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mystcraft-common.json");
    private static final List<String> DISABLED_SYMBOLS_SAMPLE = List.of(
            "mystcraft:example_symbol_a",
            "mystcraft:example_symbol_b"
    );

    // --- General ---
    public static final BooleanValue giveGuidebookOnFirstSpawn = new BooleanValue(true);
    public static final IntValue maxSymbolsPerBook = new IntValue(50);
    public static final BooleanValue deleteAgesOnStartup = new BooleanValue(false);

    // --- Instability ---
    public static final BooleanValue instabilityEnabled = new BooleanValue(true);
    public static final BooleanValue deathEffectsEnabled = new BooleanValue(true);
    public static final BooleanValue allowUnstableAges = new BooleanValue(true);
    public static final DoubleValue instabilityMultiplier = new DoubleValue(1.0);
    public static final DoubleValue maxAllowedInstability = new DoubleValue(150.0);

    // --- Instability Thresholds ---
    public static final DoubleValue thresholdDecay = new DoubleValue(40.0);
    public static final DoubleValue thresholdTransmute = new DoubleValue(50.0);
    public static final DoubleValue thresholdLightning = new DoubleValue(70.0);
    public static final DoubleValue thresholdMeteor = new DoubleValue(70.0);
    public static final DoubleValue thresholdPoison = new DoubleValue(80.0);
    public static final DoubleValue thresholdWither = new DoubleValue(100.0);

    // --- Effect Chances ---
    public static final DoubleValue chanceDecay = new DoubleValue(0.001);
    public static final DoubleValue chanceTransmute = new DoubleValue(0.002);
    public static final DoubleValue chanceLightning = new DoubleValue(0.0005);
    public static final DoubleValue chanceMeteor = new DoubleValue(0.0002);
    public static final DoubleValue chancePlayerEffect = new DoubleValue(0.0001);

    // --- Symbols ---
    // Ore block terrain symbols are disabled by default as they are overpowered
    private static final List<String> DEFAULT_DISABLED_SYMBOLS = List.of(
            // Ore storage blocks
            "mystcraft:block_minecraft_coal_block",
            "mystcraft:block_minecraft_copper_block",
            "mystcraft:block_minecraft_diamond_block",
            "mystcraft:block_minecraft_emerald_block",
            "mystcraft:block_minecraft_gold_block",
            "mystcraft:block_minecraft_iron_block",
            "mystcraft:block_minecraft_lapis_block",
            "mystcraft:block_minecraft_netherite_block",
            "mystcraft:block_minecraft_raw_copper_block",
            "mystcraft:block_minecraft_raw_gold_block",
            "mystcraft:block_minecraft_raw_iron_block",
            "mystcraft:block_minecraft_redstone_block",
            // Ancient debris
            "mystcraft:block_minecraft_ancient_debris"
    );
    public static final StringListValue disabledSymbols = new StringListValue(new ArrayList<>(DEFAULT_DISABLED_SYMBOLS));

    private FabricMystcraftConfig() {}

    /** Loads configuration from disk. Creates default file if it does not exist. */
    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            Mystcraft.LOGGER.info("[FabricMystcraftConfig] Created default config at {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                Mystcraft.LOGGER.warn("[FabricMystcraftConfig] Config file was empty, using defaults");
                return;
            }

            // General
            readBoolean(json, "giveGuidebookOnFirstSpawn", giveGuidebookOnFirstSpawn);
            readInt(json, "maxSymbolsPerBook", maxSymbolsPerBook);
            readBoolean(json, "deleteAgesOnStartup", deleteAgesOnStartup);

            // Instability
            readBoolean(json, "instabilityEnabled", instabilityEnabled);
            readBoolean(json, "deathEffectsEnabled", deathEffectsEnabled);
            readBoolean(json, "allowUnstableAges", allowUnstableAges);
            readDouble(json, "instabilityMultiplier", instabilityMultiplier);
            readDouble(json, "maxAllowedInstability", maxAllowedInstability);

            // Thresholds
            readDouble(json, "thresholdDecay", thresholdDecay);
            readDouble(json, "thresholdTransmute", thresholdTransmute);
            readDouble(json, "thresholdLightning", thresholdLightning);
            readDouble(json, "thresholdMeteor", thresholdMeteor);
            readDouble(json, "thresholdPoison", thresholdPoison);
            readDouble(json, "thresholdWither", thresholdWither);

            // Chances
            readDouble(json, "chanceDecay", chanceDecay);
            readDouble(json, "chanceTransmute", chanceTransmute);
            readDouble(json, "chanceLightning", chanceLightning);
            readDouble(json, "chanceMeteor", chanceMeteor);
            readDouble(json, "chancePlayerEffect", chancePlayerEffect);

            // Symbols
            readStringList(json, "disabledSymbols", disabledSymbols);

            Mystcraft.LOGGER.info("[FabricMystcraftConfig] Loaded config from {}", CONFIG_PATH);
        } catch (IOException e) {
            Mystcraft.LOGGER.error("[FabricMystcraftConfig] Failed to read config file, using defaults", e);
        }
    }

    /** Saves current configuration values to disk. */
    public static void save() {
        JsonObject json = new JsonObject();

        // General
        json.addProperty("giveGuidebookOnFirstSpawn", giveGuidebookOnFirstSpawn.get());
        json.addProperty("maxSymbolsPerBook", maxSymbolsPerBook.get());
        json.addProperty("deleteAgesOnStartup", deleteAgesOnStartup.get());

        // Instability
        json.addProperty("instabilityEnabled", instabilityEnabled.get());
        json.addProperty("deathEffectsEnabled", deathEffectsEnabled.get());
        json.addProperty("allowUnstableAges", allowUnstableAges.get());
        json.addProperty("instabilityMultiplier", instabilityMultiplier.get());
        json.addProperty("maxAllowedInstability", maxAllowedInstability.get());

        // Thresholds
        json.addProperty("thresholdDecay", thresholdDecay.get());
        json.addProperty("thresholdTransmute", thresholdTransmute.get());
        json.addProperty("thresholdLightning", thresholdLightning.get());
        json.addProperty("thresholdMeteor", thresholdMeteor.get());
        json.addProperty("thresholdPoison", thresholdPoison.get());
        json.addProperty("thresholdWither", thresholdWither.get());

        // Chances
        json.addProperty("chanceDecay", chanceDecay.get());
        json.addProperty("chanceTransmute", chanceTransmute.get());
        json.addProperty("chanceLightning", chanceLightning.get());
        json.addProperty("chanceMeteor", chanceMeteor.get());
        json.addProperty("chancePlayerEffect", chancePlayerEffect.get());

        // Symbols
        json.add("disabledSymbols", GSON.toJsonTree(disabledSymbols.get()));
        json.add("disabledSymbolsSample", GSON.toJsonTree(DISABLED_SYMBOLS_SAMPLE));

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            Mystcraft.LOGGER.error("[FabricMystcraftConfig] Failed to write config file", e);
        }
    }

    // --- JSON read helpers ---

    private static void readBoolean(JsonObject json, String key, BooleanValue value) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive(key);
            if (prim.isBoolean()) {
                value.set(prim.getAsBoolean());
            }
        }
    }

    private static void readInt(JsonObject json, String key, IntValue value) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive(key);
            if (prim.isNumber()) {
                value.set(prim.getAsInt());
            }
        }
    }

    private static void readDouble(JsonObject json, String key, DoubleValue value) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive(key);
            if (prim.isNumber()) {
                value.set(prim.getAsDouble());
            }
        }
    }

    private static void readStringList(JsonObject json, String key, StringListValue value) {
        if (json.has(key) && json.get(key).isJsonArray()) {
            List<String> items = new ArrayList<>();
            json.getAsJsonArray(key).forEach(element -> {
                if (element.isJsonPrimitive()) {
                    JsonPrimitive prim = element.getAsJsonPrimitive();
                    if (prim.isString()) {
                        items.add(prim.getAsString());
                    }
                }
            });
            value.set(items);
        }
    }

    // --- Value wrapper types matching ForgeConfigSpec API ---

    /** Boolean config value with .get()/.set() matching ForgeConfigSpec.BooleanValue. */
    public static final class BooleanValue {
        private boolean value;

        public BooleanValue(boolean defaultValue) {
            this.value = defaultValue;
        }

        public boolean get() {
            return value;
        }

        public void set(boolean value) {
            this.value = value;
        }
    }

    /** Integer config value with .get()/.set() matching ForgeConfigSpec.IntValue. */
    public static final class IntValue {
        private int value;

        public IntValue(int defaultValue) {
            this.value = defaultValue;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }

    /** Double config value with .get()/.set() matching ForgeConfigSpec.DoubleValue. */
    public static final class DoubleValue {
        private double value;

        public DoubleValue(double defaultValue) {
            this.value = defaultValue;
        }

        public Double get() {
            return value;
        }

        public void set(double value) {
            this.value = value;
        }
    }

    /** List config value for string lists. */
    public static final class StringListValue {
        private List<String> value;

        public StringListValue(List<String> defaultValue) {
            this.value = defaultValue;
        }

        public List<String> get() {
            return value;
        }

        public void set(List<String> value) {
            this.value = value;
        }
    }
}
