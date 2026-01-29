package art.arcane.mystcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Configuration options for Mystcraft.
 *
 * The instability system works like the original Mystcraft:
 * - Ages with missing or conflicting symbols accumulate instability points
 * - When instability exceeds certain thresholds, negative effects begin
 * - Higher instability means more frequent and severe effects
 * - Effects include decay spreading, block transmutation, lightning, meteors, and player debuffs
 */
public class ForgeMystcraftConfig {

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_SPEC;

    // General settings
    public static final ForgeConfigSpec.BooleanValue giveGuidebookOnFirstSpawn;
    public static final ForgeConfigSpec.IntValue maxSymbolsPerBook;
    public static final ForgeConfigSpec.BooleanValue deleteAgesOnStartup;
    public static final ForgeConfigSpec.BooleanValue enablePersonalLinkBooks;
    public static final ForgeConfigSpec.BooleanValue allowGravityBlocksInAges;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledSymbols;
    // Instability settings
    public static final ForgeConfigSpec.BooleanValue instabilityEnabled;
    public static final ForgeConfigSpec.BooleanValue deathEffectsEnabled;
    public static final ForgeConfigSpec.BooleanValue allowUnstableAges;
    public static final ForgeConfigSpec.DoubleValue instabilityMultiplier;
    public static final ForgeConfigSpec.DoubleValue maxAllowedInstability;

    // Instability thresholds (instability level required to trigger each effect tier)
    public static final ForgeConfigSpec.DoubleValue thresholdDecay;
    public static final ForgeConfigSpec.DoubleValue thresholdTransmute;
    public static final ForgeConfigSpec.DoubleValue thresholdLightning;
    public static final ForgeConfigSpec.DoubleValue thresholdMeteor;
    public static final ForgeConfigSpec.DoubleValue thresholdPoison;
    public static final ForgeConfigSpec.DoubleValue thresholdWither;

    // Effect chances (base chance per tick, scaled by instability)
    public static final ForgeConfigSpec.DoubleValue chanceDecay;
    public static final ForgeConfigSpec.DoubleValue chanceTransmute;
    public static final ForgeConfigSpec.DoubleValue chanceLightning;
    public static final ForgeConfigSpec.DoubleValue chanceMeteor;
    public static final ForgeConfigSpec.DoubleValue chancePlayerEffect;

    static {
        COMMON_BUILDER.comment("Mystcraft Common Configuration");

        // --- General ---
        COMMON_BUILDER.push("general");

        giveGuidebookOnFirstSpawn = COMMON_BUILDER
                .comment("Whether to give new players a copy of the Mystcraft Guidebook when they first join the world.")
                .define("giveGuidebookOnFirstSpawn", true);

        maxSymbolsPerBook = COMMON_BUILDER
                .comment(
                        "Maximum number of symbol pages allowed in a single Agebook.",
                        "Higher values allow more complex ages but may increase server load.",
                        "Set to -1 for unlimited (not recommended).",
                        "Default: 50"
                )
                .defineInRange("maxSymbolsPerBook", 50, -1, 1000);

        deleteAgesOnStartup = COMMON_BUILDER
                .comment("If true, all Mystcraft Ages will be deleted every time the server starts. Use for development/testing.")
                .define("deleteAgesOnStartup", false);

        enablePersonalLinkBooks = COMMON_BUILDER
                .comment("If true, personal link books and personal pocket dimensions are enabled.")
                .define("enablePersonalLinkBooks", true);

        allowGravityBlocksInAges = COMMON_BUILDER
                .comment("If true, gravity blocks (sand, gravel, anvils, concrete powder) can fall in Mystcraft Ages.")
                .define("allowGravityBlocksInAges", false);

        disabledSymbols = COMMON_BUILDER
                .comment(
                        "List of symbol IDs to disable, e.g. [\"mystcraft:example_symbol_a\", \"mystcraft:example_symbol_b\"].",
                        "Disabled symbols are hidden from books and not registered at runtime.",
                        "By default, ore block terrain symbols are disabled as they are overpowered."
                )
                .defineListAllowEmpty("disabledSymbols", List.of(
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
                ), ForgeMystcraftConfig::isValidSymbolId);

        COMMON_BUILDER.pop();

        // --- Instability ---
        COMMON_BUILDER.comment(
                "Instability Settings",
                "The instability system is core to Mystcraft's balance.",
                "Ages with incomplete or greedy symbol combinations become unstable.",
                "Unstable ages experience increasingly severe negative effects."
        ).push("instability");

        instabilityEnabled = COMMON_BUILDER
                .comment("Master switch for the instability system. If false, no instability effects occur.")
                .define("enabled", true);

        deathEffectsEnabled = COMMON_BUILDER
                .comment("Whether thematic death effects (messages, debuffs, instability surge) occur when players die in Ages.")
                .define("deathEffectsEnabled", true);

        allowUnstableAges = COMMON_BUILDER
                .comment(
                        "Whether to allow creation of Ages that exceed the maximum instability threshold.",
                        "If false, attempting to link to an Age with instability above 'maxAllowedInstability' will fail.",
                        "The player will be warned and the link will not activate."
                )
                .define("allowUnstableAges", true);

        instabilityMultiplier = COMMON_BUILDER
                .comment(
                        "Global multiplier for all instability effect chances.",
                        "0.0 = no effects ever trigger (but instability still accumulates)",
                        "1.0 = normal effect frequency (default)",
                        "2.0 = double frequency, etc."
                )
                .defineInRange("effectMultiplier", 1.0, 0.0, 10.0);

        maxAllowedInstability = COMMON_BUILDER
                .comment(
                        "Maximum instability allowed when 'allowUnstableAges' is false.",
                        "Ages at or below this value can be created freely.",
                        "Default: 150.0 (allows moderately unstable ages)"
                )
                .defineInRange("maxAllowedInstability", 150.0, 0.0, 1000.0);

        COMMON_BUILDER.pop();

        // --- Instability Thresholds ---
        COMMON_BUILDER.comment(
                "Instability Thresholds",
                "The instability level required before each effect type begins.",
                "Lower values = effects start sooner. Higher values = more lenient."
        ).push("instability_thresholds");

        thresholdDecay = COMMON_BUILDER
                .comment("Instability threshold for decay blocks to start spreading.")
                .defineInRange("decay", 40.0, 0.0, 500.0);

        thresholdTransmute = COMMON_BUILDER
                .comment("Instability threshold for random block transmutation to begin.")
                .defineInRange("transmute", 50.0, 0.0, 500.0);

        thresholdLightning = COMMON_BUILDER
                .comment("Instability threshold for random lightning strikes.")
                .defineInRange("lightning", 70.0, 0.0, 500.0);

        thresholdMeteor = COMMON_BUILDER
                .comment("Instability threshold for meteor falls.")
                .defineInRange("meteor", 70.0, 0.0, 500.0);

        thresholdPoison = COMMON_BUILDER
                .comment("Instability threshold for poison/hunger effects on players.")
                .defineInRange("poison", 80.0, 0.0, 500.0);

        thresholdWither = COMMON_BUILDER
                .comment("Instability threshold for wither effects on players (most severe).")
                .defineInRange("wither", 100.0, 0.0, 500.0);

        COMMON_BUILDER.pop();

        // --- Effect Chances ---
        COMMON_BUILDER.comment(
                "Effect Base Chances",
                "Base probability per tick for each effect type.",
                "Actual chance scales with instability above threshold.",
                "Values are decimals: 0.001 = 0.1% chance per tick"
        ).push("instability_chances");

        chanceDecay = COMMON_BUILDER
                .comment("Base chance per tick for decay to spread (default: 0.001 = 0.1%)")
                .defineInRange("decay", 0.001, 0.0, 1.0);

        chanceTransmute = COMMON_BUILDER
                .comment("Base chance per tick for block transmutation (default: 0.002 = 0.2%)")
                .defineInRange("transmute", 0.002, 0.0, 1.0);

        chanceLightning = COMMON_BUILDER
                .comment("Base chance per tick for lightning strikes (default: 0.0005 = 0.05%)")
                .defineInRange("lightning", 0.0005, 0.0, 1.0);

        chanceMeteor = COMMON_BUILDER
                .comment("Base chance per tick for meteor spawns (default: 0.0002 = 0.02%)")
                .defineInRange("meteor", 0.0002, 0.0, 1.0);

        chancePlayerEffect = COMMON_BUILDER
                .comment("Base chance per tick for player debuffs (default: 0.0001 = 0.01%)")
                .defineInRange("playerEffect", 0.0001, 0.0, 1.0);

        COMMON_BUILDER.pop();

        COMMON_SPEC = COMMON_BUILDER.build();
    }

    /**
     * Register the config with Forge.
     * Call this from the mod constructor.
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "mystcraft-common.toml");
    }

    private static boolean isValidSymbolId(Object value) {
        if (!(value instanceof String string)) {
            return false;
        }
        return ResourceLocation.isValidResourceLocation(string);
    }
}
