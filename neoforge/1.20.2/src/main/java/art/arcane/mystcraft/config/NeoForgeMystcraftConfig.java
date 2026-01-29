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

    // Instability thresholds
    public static final ModConfigSpec.DoubleValue thresholdDecay;
    public static final ModConfigSpec.DoubleValue thresholdTransmute;
    public static final ModConfigSpec.DoubleValue thresholdLightning;
    public static final ModConfigSpec.DoubleValue thresholdMeteor;
    public static final ModConfigSpec.DoubleValue thresholdPoison;
    public static final ModConfigSpec.DoubleValue thresholdWither;

    // Effect chances
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
                .comment("Maximum number of symbol pages allowed in a single Agebook. Default: 50")
                .defineInRange("maxSymbolsPerBook", 50, -1, 1000);

        deleteAgesOnStartup = COMMON_BUILDER
                .comment("If true, all Mystcraft Ages will be deleted every time the server starts.")
                .define("deleteAgesOnStartup", false);

        microDimensionsEnabled = COMMON_BUILDER
                .comment("If true, newly created Ages are limited to a small chunk radius around the spawn chunk.")
                .define("microDimensionsEnabled", false);

        microDimensionRadiusChunks = COMMON_BUILDER
                .comment("Radius in chunks from the spawn chunk center for micro dimensions.")
                .defineInRange("microDimensionRadiusChunks", 0, 0, 2048);

        microDimensionExtraChunks = COMMON_BUILDER
                .comment("Additional chunk rings generated beyond the border for visual continuity.")
                .defineInRange("microDimensionExtraChunks", 1, 0, 16);

        safeStories = COMMON_BUILDER
                .comment("If true, players who die or fall into the void in Mystcraft Ages are returned to their link origin.")
                .define("safeStories", true);

        disabledSymbols = COMMON_BUILDER
                .comment("List of symbol IDs to disable.")
                .defineListAllowEmpty("disabledSymbols", List.of(
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
                        "mystcraft:block_minecraft_ancient_debris"
                ), NeoForgeMystcraftConfig::isValidSymbolId);

        COMMON_BUILDER.pop();

        // --- Personal Pocket Dimension ---
        COMMON_BUILDER.push("personal_pocket");

        pocketInnerHalfSizeXZ = COMMON_BUILDER
                .comment("Half the inner void space horizontally in blocks. Default: 24")
                .defineInRange("innerHalfSizeXZ", 24, 2, 4096);

        pocketInnerHalfSizeY = COMMON_BUILDER
                .comment("Half the inner void space vertically in blocks. Default: 24")
                .defineInRange("innerHalfSizeY", 24, 2, 4096);

        pocketInnerThickness = COMMON_BUILDER
                .comment("Thickness of the inner shell. Default: 3")
                .defineInRange("innerThickness", 3, 1, 32);

        pocketOuterThickness = COMMON_BUILDER
                .comment("Thickness of the outer shell. Default: 5")
                .defineInRange("outerThickness", 5, 1, 32);

        pocketCenterY = COMMON_BUILDER
                .comment("Y coordinate of the pocket center. Default: 0")
                .defineInRange("centerY", 0, -2032, 2032);

        pocketInnerBlockPalette = COMMON_BUILDER
                .comment("List of block IDs for the inner shell layer.")
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
                .comment("Block ID for the outer shell. Default: minecraft:bedrock")
                .define("outerBlock", "minecraft:bedrock");

        COMMON_BUILDER.pop();

        // --- Instability ---
        COMMON_BUILDER.push("instability");

        instabilityEnabled = COMMON_BUILDER
                .comment("Master switch for the instability system.")
                .define("enabled", true);

        deathEffectsEnabled = COMMON_BUILDER
                .comment("Whether thematic death effects occur when players die in Ages.")
                .define("deathEffectsEnabled", true);

        allowUnstableAges = COMMON_BUILDER
                .comment("Whether to allow creation of Ages that exceed the maximum instability threshold.")
                .define("allowUnstableAges", true);

        instabilityMultiplier = COMMON_BUILDER
                .comment("Global multiplier for all instability effect chances.")
                .defineInRange("effectMultiplier", 1.0, 0.0, 10.0);

        maxAllowedInstability = COMMON_BUILDER
                .comment("Maximum instability allowed when 'allowUnstableAges' is false. Default: 150.0")
                .defineInRange("maxAllowedInstability", 150.0, 0.0, 1000.0);

        COMMON_BUILDER.pop();

        // --- Instability Thresholds ---
        COMMON_BUILDER.push("instability_thresholds");

        thresholdDecay = COMMON_BUILDER.defineInRange("decay", 40.0, 0.0, 500.0);
        thresholdTransmute = COMMON_BUILDER.defineInRange("transmute", 50.0, 0.0, 500.0);
        thresholdLightning = COMMON_BUILDER.defineInRange("lightning", 70.0, 0.0, 500.0);
        thresholdMeteor = COMMON_BUILDER.defineInRange("meteor", 70.0, 0.0, 500.0);
        thresholdPoison = COMMON_BUILDER.defineInRange("poison", 80.0, 0.0, 500.0);
        thresholdWither = COMMON_BUILDER.defineInRange("wither", 100.0, 0.0, 500.0);

        COMMON_BUILDER.pop();

        // --- Effect Chances ---
        COMMON_BUILDER.push("instability_chances");

        chanceDecay = COMMON_BUILDER.defineInRange("decay", 0.001, 0.0, 1.0);
        chanceTransmute = COMMON_BUILDER.defineInRange("transmute", 0.002, 0.0, 1.0);
        chanceLightning = COMMON_BUILDER.defineInRange("lightning", 0.0005, 0.0, 1.0);
        chanceMeteor = COMMON_BUILDER.defineInRange("meteor", 0.0002, 0.0, 1.0);
        chancePlayerEffect = COMMON_BUILDER.defineInRange("playerEffect", 0.0001, 0.0, 1.0);

        COMMON_BUILDER.pop();

        COMMON_SPEC = COMMON_BUILDER.build();
    }

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
