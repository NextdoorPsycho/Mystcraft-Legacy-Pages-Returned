package art.arcane.mystcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Configuration options for Mystcraft.
 */
public class MystcraftConfig {

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ForgeConfigSpec.BooleanValue giveGuidebookOnFirstSpawn;
    public static final ForgeConfigSpec.BooleanValue allowUnstableAges;
    public static final ForgeConfigSpec.DoubleValue maxInstabilityMultiplier;

    static {
        COMMON_BUILDER.comment("Mystcraft Common Configuration");
        COMMON_BUILDER.push("general");

        giveGuidebookOnFirstSpawn = COMMON_BUILDER
                .comment("Whether to give new players a copy of the Mystcraft Guidebook when they first join the world.")
                .define("giveGuidebookOnFirstSpawn", true);

        allowUnstableAges = COMMON_BUILDER
                .comment("Whether to allow creation of Ages with high instability. If false, very unstable Ages will fail to generate.")
                .define("allowUnstableAges", true);

        maxInstabilityMultiplier = COMMON_BUILDER
                .comment("Multiplier for instability effects. Higher values make unstable Ages more dangerous.")
                .defineInRange("maxInstabilityMultiplier", 1.0, 0.0, 10.0);

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
}
