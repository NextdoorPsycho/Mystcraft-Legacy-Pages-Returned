package art.arcane.mystcraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
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
public class NeoForgeMystcraftConfig {

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec COMMON_SPEC;

    // General settings
    public static final ModConfigSpec.BooleanValue giveGuidebookOnFirstSpawn;
    public static final ModConfigSpec.IntValue maxSymbolsPerBook;
    public static final ModConfigSpec.BooleanValue deleteAgesOnStartup;
    public static final ModConfigSpec.BooleanValue microDimensionsEnabled;
    public static final ModConfigSpec.IntValue microDimensionRadiusChunks;
    public static final ModConfigSpec.IntValue microDimensionExtraChunks;
    public static final ModConfigSpec.BooleanValue safeStories;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> disabledSymbols;

    // Personal pocket dimension settings
    public static final ModConfigSpec.IntValue pocketInnerHalfSizeXZ;
    public static final ModConfigSpec.IntValue pocketInnerHalfSizeY;
    public static final ModConfigSpec.IntValue pocketInnerThickness;
    public static final ModConfigSpec.IntValue pocketOuterThickness;
    public static final ModConfigSpec.IntValue pocketCenterY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> pocketInnerBlockPalette;
    public static final ModConfigSpec.ConfigValue<String> pocketOuterBlock;

    // Instability settings
    public static final ModConfigSpec.BooleanValue instabilityEnabled;
    public static final ModConfigSpec.BooleanValue deathEffectsEnabled;
    public static final ModConfigSpec.BooleanValue allowUnstableAges;
    public static final ModConfigSpec.DoubleValue instabilityMultiplier;
    public static final ModConfigSpec.DoubleValue maxAllowedInstability;

    // Instability thresholds (instability level required to trigger each effect tier)
    public static final ModConfigSpec.DoubleValue thresholdDecay;
    public static final ModConfigSpec.DoubleValue thresholdTransmute;
    public static final ModConfigSpec.DoubleValue thresholdLightning;
    public static final ModConfigSpec.DoubleValue thresholdMeteor;
    public static final ModConfigSpec.DoubleValue thresholdPoison;
    public static final ModConfigSpec.DoubleValue thresholdWither;

    // Effect chances (base chance per tick, scaled by instability)
    public static final ModConfigSpec.DoubleValue chanceDecay;
    public static final ModConfigSpec.DoubleValue chanceTransmute;
    public static final ModConfigSpec.DoubleValue chanceLightning;
    public static final ModConfigSpec.DoubleValue chanceMeteor;
    public static final ModConfigSpec.DoubleValue chancePlayerEffect;

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

        microDimensionsEnabled = COMMON_BUILDER
                .comment(
                        "If true, newly created Ages are limited to a small chunk radius around the spawn chunk.",
                        "Uses a world border as a hard cutoff and prevents chunk generation past the limit."
                )
                .define("microDimensionsEnabled", false);

        microDimensionRadiusChunks = COMMON_BUILDER
                .comment(
                        "Radius in chunks from the spawn chunk center for micro dimensions.",
                        "0 = single chunk. Border aligns to chunk edges."
                )
                .defineInRange("microDimensionRadiusChunks", 0, 0, 2048);

        microDimensionExtraChunks = COMMON_BUILDER
                .comment(
                        "Additional chunk rings generated beyond the border for visual continuity.",
                        "Default 1 generates one extra ring outside the border."
                )
                .defineInRange("microDimensionExtraChunks", 1, 0, 16);

        safeStories = COMMON_BUILDER
                .comment(
                        "If true, players who die or fall into the void in Mystcraft Ages are returned",
                        "to the location they linked from (fallback to server spawn)."
                )
                .define("safeStories", true);

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
                ), NeoForgeMystcraftConfig::isValidSymbolId);

        COMMON_BUILDER.pop();

        // --- Personal Pocket Dimension ---
        COMMON_BUILDER.comment(
                "Personal Pocket Dimension Settings",
                "Configure the size and materials of personal pocket dimensions.",
                "The pocket is a hollow rectangular box: inner void surrounded by inner shell then outer shell.",
                "XZ (horizontal) and Y (vertical) sizes are configured separately.",
                "XZ can be up to 8192 blocks (half-size 4096).",
                "Y is limited to ~4000 blocks due to Minecraft dimension height limits."
        ).push("personal_pocket");

        pocketInnerHalfSizeXZ = COMMON_BUILDER
                .comment(
                        "Half the inner void space horizontally (X and Z axes) in blocks.",
                        "Default: 24 (48 block diameter, 3 chunks).",
                        "Min: 2 (4 block diameter). Max: 4096 (8192 block diameter)."
                )
                .defineInRange("innerHalfSizeXZ", 24, 2, 4096);

        pocketInnerHalfSizeY = COMMON_BUILDER
                .comment(
                        "Half the inner void space vertically (Y axis) in blocks.",
                        "Default: 24 (48 block height).",
                        "Min: 2 (4 block height). Max: 4096 (8192 block height).",
                        "Note: Minecraft 1.20.2 dimension height is 4064, so ~4048 is the practical max."
                )
                .defineInRange("innerHalfSizeY", 24, 2, 4096);

        pocketInnerThickness = COMMON_BUILDER
                .comment(
                        "Thickness of the inner shell surrounding the void.",
                        "Default: 3 blocks."
                )
                .defineInRange("innerThickness", 3, 1, 32);

        pocketOuterThickness = COMMON_BUILDER
                .comment(
                        "Thickness of the outer shell surrounding the inner layer.",
                        "Default: 5 blocks."
                )
                .defineInRange("outerThickness", 5, 1, 32);

        pocketCenterY = COMMON_BUILDER
                .comment(
                        "Y coordinate of the pocket center.",
                        "Default: 0 (centered in dimension).",
                        "Valid range: -2032 to 2032 (full dimension height).",
                        "Pocket will be auto-adjusted to fit within dimension limits."
                )
                .defineInRange("centerY", 0, -2032, 2032);

        pocketInnerBlockPalette = COMMON_BUILDER
                .comment(
                        "List of block IDs for the inner shell layer.",
                        "Multiple blocks will be randomly selected during generation using a simplex-like pattern.",
                        "Default includes all wood plank types for a cozy varied appearance."
                )
                .defineListAllowEmpty("innerBlockPalette", List.of(
                        "minecraft:oak_planks",
                        "minecraft:spruce_planks",
                        "minecraft:birch_planks",
                        "minecraft:jungle_planks",
                        "minecraft:acacia_planks",
                        "minecraft:dark_oak_planks",
                        "minecraft:mangrove_planks",
                        "minecraft:cherry_planks"
                ), NeoForgeMystcraftConfig::isValidBlockId);

        pocketOuterBlock = COMMON_BUILDER
                .comment(
                        "Block ID for the outer shell.",
                        "Default: minecraft:bedrock"
                )
                .define("outerBlock", "minecraft:bedrock");

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

    /** Register the config with NeoForge. Call from the mod constructor. */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "mystcraft-common.toml");
    }

    private static boolean isValidSymbolId(Object value) {
        if (!(value instanceof String string)) {
            return false;
        }
        return ResourceLocation.isValidResourceLocation(string);
    }

    private static boolean isValidBlockId(Object value) {
        if (!(value instanceof String string)) {
            return false;
        }
        return ResourceLocation.isValidResourceLocation(string);
    }
}
