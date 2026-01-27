package art.arcane.mystcraft.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Curated Age presets that produce well-structured dimensions with high variance.
 * Each preset defines a theme with fixed symbols and pools of random picks,
 * covering the full symbol catalog including timescale, ore control, environment
 * effects, perlin worms, and the complete color/modifier palette.
 *
 * Biome symbol IDs follow the pattern: mystcraft:biome_<biome_path>
 */
public final class AgePresets {

    private AgePresets() {}

    private static final Map<String, Preset> PRESETS = new HashMap<>();

    public static final List<String> PRESET_NAMES;

    static {

        // ===================================================================
        // --- Overworld Variants ---
        // ===================================================================

        register(new Preset("classic", "Classic Age",
                list("mystcraft:terrain_normal", "mystcraft:biome_native",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:surface_lakes", "mystcraft:dripstone_caves",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_grass_natural", "mystcraft:color_water_natural",
                        "mystcraft:color_foliage_natural"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_dark_forest",
                                "mystcraft:biome_taiga", "mystcraft:biome_meadow",
                                "mystcraft:biome_flower_forest", "mystcraft:biome_savanna",
                                "mystcraft:biome_swamp", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_jungle", "mystcraft:biome_desert",
                                "mystcraft:biome_cherry_grove"),
                        pool(1, "mystcraft:sun_normal", "mystcraft:sun_large",
                                "mystcraft:sun_small", "mystcraft:sun_slow"),
                        pool(1, "mystcraft:moon_normal", "mystcraft:moon_large",
                                "mystcraft:moon_full", "mystcraft:moon_slow"),
                        pool(1, "mystcraft:stars_normal", "mystcraft:stars_twinkle",
                                "mystcraft:stars_dense", "mystcraft:stars_sparse"),
                        pool(4, "mystcraft:villages", "mystcraft:dungeons", "mystcraft:mineshafts",
                                "mystcraft:strongholds", "mystcraft:ruined_portals",
                                "mystcraft:pillager_outposts", "mystcraft:witch_huts",
                                "mystcraft:igloos", "mystcraft:buried_treasure",
                                "mystcraft:shipwrecks", "mystcraft:trail_ruins"),
                        pool(2, "mystcraft:dense_ores", "mystcraft:huge_trees",
                                "mystcraft:deep_lakes", "mystcraft:lush_caves",
                                "mystcraft:perlin_worms", "mystcraft:floating_islands"),
                        pool(2, "mystcraft:extra_iron_ore", "mystcraft:extra_coal_ore",
                                "mystcraft:extra_copper_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:extra_lapis_ore"),
                        pool(1, "mystcraft:gradient_sunset", "mystcraft:gradient_dawn",
                                "mystcraft:gradient_dusk", "mystcraft:color_sunset_natural")
                )
        ));

        register(new Preset("lush", "Lush Paradise",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_normal", "mystcraft:moon_normal",
                        "mystcraft:lighting_bright", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:lush_caves", "mystcraft:huge_trees",
                        "mystcraft:surface_lakes", "mystcraft:deep_lakes",
                        "mystcraft:color_sky_natural", "mystcraft:color_grass_natural",
                        "mystcraft:color_foliage_natural", "mystcraft:color_water_natural",
                        "mystcraft:rainbow", "mystcraft:cloud_high",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_jungle",
                                "mystcraft:biome_meadow", "mystcraft:biome_birch_forest",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_forest",
                                "mystcraft:biome_taiga", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_cherry_grove", "mystcraft:biome_mangrove_swamp",
                                "mystcraft:biome_bamboo_jungle"),
                        pool(1, "mystcraft:stars_twinkle", "mystcraft:stars_dense",
                                "mystcraft:stars_normal"),
                        pool(3, "mystcraft:villages", "mystcraft:ruined_portals",
                                "mystcraft:trail_ruins", "mystcraft:dungeons",
                                "mystcraft:witch_huts", "mystcraft:jungle_temples",
                                "mystcraft:shipwrecks", "mystcraft:buried_treasure"),
                        pool(2, "mystcraft:extra_iron_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:extra_coal_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:crystal_formation", "mystcraft:star_fissure_feature",
                                "mystcraft:obelisks", "mystcraft:perlin_worms"),
                        pool(1, "mystcraft:gradient_dawn", "mystcraft:gradient_sunset",
                                "mystcraft:color_sunset_natural")
                )
        ));

        register(new Preset("desert", "Scorched Sands",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_large", "mystcraft:moon_small", "mystcraft:stars_sparse",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:color_amber", "mystcraft:color_sky",
                        "mystcraft:color_gold", "mystcraft:color_fog",
                        "mystcraft:env_scorched", "mystcraft:cloud_none",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(5, "mystcraft:biome_desert", "mystcraft:biome_badlands",
                                "mystcraft:biome_eroded_badlands", "mystcraft:biome_savanna",
                                "mystcraft:biome_windswept_savanna", "mystcraft:biome_wooded_badlands",
                                "mystcraft:biome_stony_shore", "mystcraft:biome_windswept_hills"),
                        pool(3, "mystcraft:villages", "mystcraft:desert_temples",
                                "mystcraft:dungeons", "mystcraft:mineshafts",
                                "mystcraft:pillager_outposts", "mystcraft:ruined_portals",
                                "mystcraft:buried_treasure", "mystcraft:trail_ruins"),
                        pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_diamond_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:extra_iron_ore"),
                        pool(2, "mystcraft:spikes", "mystcraft:obelisks",
                                "mystcraft:tendrils", "mystcraft:spheres",
                                "mystcraft:dripstone_caves", "mystcraft:deep_lakes",
                                "mystcraft:perlin_worms"),
                        pool(1, "mystcraft:star_fissure_feature", "mystcraft:crystal_formation",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:gradient_blood_sky", "mystcraft:gradient_sunset",
                                "mystcraft:color_crimson", "mystcraft:color_sunset")
                )
        ));

        register(new Preset("arctic", "Frozen Expanse",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_normal", "mystcraft:moon_full",
                        "mystcraft:lighting_normal", "mystcraft:weather_snow",
                        "mystcraft:caves", "mystcraft:dripstone_caves",
                        "mystcraft:surface_lakes", "mystcraft:deep_lakes",
                        "mystcraft:color_sky_natural", "mystcraft:color_sapphire", "mystcraft:color_water",
                        "mystcraft:color_light_blue", "mystcraft:color_fog",
                        "mystcraft:cloud_low",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_snowy_plains", "mystcraft:biome_snowy_taiga",
                                "mystcraft:biome_frozen_peaks", "mystcraft:biome_ice_spikes",
                                "mystcraft:biome_grove", "mystcraft:biome_frozen_river",
                                "mystcraft:biome_snowy_beach", "mystcraft:biome_snowy_slopes",
                                "mystcraft:biome_windswept_hills"),
                        pool(1, "mystcraft:stars_twinkle", "mystcraft:stars_dense",
                                "mystcraft:stars_normal"),
                        pool(4, "mystcraft:villages", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:pillager_outposts",
                                "mystcraft:mineshafts", "mystcraft:trail_ruins",
                                "mystcraft:igloos"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:extra_coal_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:extra_emerald_ore"),
                        pool(2, "mystcraft:spikes", "mystcraft:lush_caves",
                                "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:huge_trees", "mystcraft:perlin_worms"),
                        pool(1, "mystcraft:gradient_dawn", "mystcraft:gradient_dusk",
                                "mystcraft:color_sunset_natural", "mystcraft:gradient_aurora")
                )
        ));

        register(new Preset("mountains", "Towering Peaks",
                list("mystcraft:terrain_amplified", "mystcraft:biome_large",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines", "mystcraft:deep_lakes",
                        "mystcraft:dripstone_caves", "mystcraft:cloud_high",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:perlin_worms"),
                list(
                        pool(7, "mystcraft:biome_meadow", "mystcraft:biome_stony_peaks",
                                "mystcraft:biome_jagged_peaks", "mystcraft:biome_frozen_peaks",
                                "mystcraft:biome_grove", "mystcraft:biome_snowy_slopes",
                                "mystcraft:biome_windswept_hills", "mystcraft:biome_windswept_forest",
                                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_taiga",
                                "mystcraft:biome_cherry_grove"),
                        pool(3, "mystcraft:villages", "mystcraft:pillager_outposts",
                                "mystcraft:trail_ruins", "mystcraft:mineshafts",
                                "mystcraft:strongholds", "mystcraft:dungeons",
                                "mystcraft:igloos"),
                        pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:extra_copper_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_coal_ore"),
                        pool(2, "mystcraft:spikes", "mystcraft:vertical_tendrils",
                                "mystcraft:floating_islands", "mystcraft:huge_trees",
                                "mystcraft:lush_caves", "mystcraft:surface_lakes"),
                        pool(1, "mystcraft:gradient_dawn", "mystcraft:gradient_dusk",
                                "mystcraft:color_sunset_natural")
                )
        ));

        register(new Preset("ocean", "Drowned World",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_rain",
                        "mystcraft:caves", "mystcraft:deep_lakes", "mystcraft:surface_lakes",
                        "mystcraft:color_sky_natural",
                        "mystcraft:color_turquoise", "mystcraft:color_water",
                        "mystcraft:color_sapphire", "mystcraft:color_fog",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_ocean", "mystcraft:biome_deep_ocean",
                                "mystcraft:biome_warm_ocean", "mystcraft:biome_lukewarm_ocean",
                                "mystcraft:biome_cold_ocean", "mystcraft:biome_frozen_ocean",
                                "mystcraft:biome_beach", "mystcraft:biome_mushroom_fields",
                                "mystcraft:biome_stony_shore"),
                        pool(4, "mystcraft:ocean_monuments", "mystcraft:ruined_portals",
                                "mystcraft:dungeons", "mystcraft:strongholds",
                                "mystcraft:trail_ruins", "mystcraft:shipwrecks",
                                "mystcraft:ocean_ruins", "mystcraft:buried_treasure"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:spheres", "mystcraft:rainbow",
                                "mystcraft:perlin_worms"),
                        pool(1, "mystcraft:gradient_dawn", "mystcraft:gradient_sunset",
                                "mystcraft:color_sunset_natural")
                )
        ));

        register(new Preset("tropical", "Tropical Paradise",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_large", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_bright", "mystcraft:weather_rain",
                        "mystcraft:caves", "mystcraft:lush_caves",
                        "mystcraft:surface_lakes", "mystcraft:deep_lakes",
                        "mystcraft:huge_trees", "mystcraft:cloud_high",
                        "mystcraft:color_sky_natural",
                        "mystcraft:color_turquoise", "mystcraft:color_water",
                        "mystcraft:color_fog_natural",
                        "mystcraft:rainbow", "mystcraft:gradient_sunset",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_jungle", "mystcraft:biome_warm_ocean",
                                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_beach",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_forest",
                                "mystcraft:biome_bamboo_jungle", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_meadow"),
                        pool(4, "mystcraft:jungle_temples", "mystcraft:ocean_monuments",
                                "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:dungeons", "mystcraft:ruined_portals",
                                "mystcraft:shipwrecks", "mystcraft:ocean_ruins",
                                "mystcraft:buried_treasure"),
                        pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature", "mystcraft:perlin_worms")
                )
        ));

        // ===================================================================
        // --- Exotic / Fantasy ---
        // ===================================================================

        register(new Preset("skylands", "Sky Archipelago",
                list("mystcraft:terrain_skylands", "mystcraft:biome_small",
                        "mystcraft:sun_large", "mystcraft:moon_small",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:floating_islands", "mystcraft:cloud_high",
                        "mystcraft:color_light_blue", "mystcraft:color_sky",
                        "mystcraft:color_ivory", "mystcraft:color_fog",
                        "mystcraft:color_silver", "mystcraft:color_cloud",
                        "mystcraft:rainbow",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_meadow", "mystcraft:biome_birch_forest",
                                "mystcraft:biome_forest", "mystcraft:biome_jungle",
                                "mystcraft:biome_taiga", "mystcraft:biome_cherry_grove",
                                "mystcraft:biome_mushroom_fields"),
                        pool(1, "mystcraft:stars_twinkle", "mystcraft:stars_dense",
                                "mystcraft:stars_sparse"),
                        pool(2, "mystcraft:dense_ores", "mystcraft:huge_trees",
                                "mystcraft:extra_diamond_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:extra_emerald_ore"),
                        pool(2, "mystcraft:villages", "mystcraft:ruined_portals",
                                "mystcraft:trail_ruins", "mystcraft:dungeons"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature", "mystcraft:spheres",
                                "mystcraft:spikes"),
                        pool(1, "mystcraft:gradient_aurora", "mystcraft:gradient_dawn",
                                "mystcraft:gradient_sunset", "mystcraft:color_gold", "mystcraft:color_sunset")
                )
        ));

        register(new Preset("cavern", "Vast Caverns",
                list("mystcraft:terrain_cave", "mystcraft:biome_medium",
                        "mystcraft:stars_dark", "mystcraft:lighting_dark",
                        "mystcraft:weather_off",
                        "mystcraft:caves", "mystcraft:dripstone_caves", "mystcraft:lush_caves",
                        "mystcraft:deep_dark", "mystcraft:dense_ores",
                        "mystcraft:perlin_worms",
                        "mystcraft:color_dark_gray", "mystcraft:color_fog",
                        "mystcraft:cloud_none",
                        "mystcraft:env_static_time"),
                list(
                        pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_taiga",
                                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_swamp",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_plains",
                                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_deep_dark"),
                        pool(3, "mystcraft:dungeons", "mystcraft:mineshafts",
                                "mystcraft:ancient_cities", "mystcraft:strongholds",
                                "mystcraft:trail_ruins"),
                        pool(3, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_redstone_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:extra_emerald_ore"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spheres", "mystcraft:spikes",
                                "mystcraft:crystal_formation")
                )
        ));

        register(new Preset("ethereal", "Ethereal Realm",
                list("mystcraft:terrain_amplified", "mystcraft:biome_small",
                        "mystcraft:sun_small", "mystcraft:moon_small", "mystcraft:stars_dense",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:floating_islands", "mystcraft:cloud_high",
                        "mystcraft:color_lavender", "mystcraft:color_sky",
                        "mystcraft:color_ivory", "mystcraft:color_fog",
                        "mystcraft:color_violet", "mystcraft:color_cloud",
                        "mystcraft:color_jade", "mystcraft:color_grass",
                        "mystcraft:color_emerald", "mystcraft:color_foliage",
                        "mystcraft:color_turquoise", "mystcraft:color_water",
                        "mystcraft:rainbow", "mystcraft:crystal_formation",
                        "mystcraft:gradient_aurora", "mystcraft:horizon_high",
                        "mystcraft:env_slow_time"),
                list(
                        pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_meadow",
                                "mystcraft:biome_cherry_grove", "mystcraft:biome_birch_forest",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_plains",
                                "mystcraft:biome_forest", "mystcraft:biome_grove"),
                        pool(2, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:ruined_portals", "mystcraft:dungeons"),
                        pool(2, "mystcraft:obelisks", "mystcraft:star_fissure_feature",
                                "mystcraft:spikes", "mystcraft:spheres",
                                "mystcraft:huge_trees"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:caves", "mystcraft:lush_caves",
                                "mystcraft:dripstone_caves")
                )
        ));

        register(new Preset("crystal", "Crystal Expanse",
                list("mystcraft:terrain_amplified", "mystcraft:biome_small",
                        "mystcraft:sun_large", "mystcraft:moon_normal", "mystcraft:stars_dense",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:block_minecraft_amethyst_block",
                        "mystcraft:caves", "mystcraft:lush_caves",
                        "mystcraft:crystal_formation", "mystcraft:cloud_high",
                        "mystcraft:color_violet", "mystcraft:color_sky",
                        "mystcraft:color_lavender", "mystcraft:color_fog",
                        "mystcraft:color_jade", "mystcraft:color_grass",
                        "mystcraft:color_indigo", "mystcraft:color_water",
                        "mystcraft:rainbow", "mystcraft:gradient_aurora",
                        "mystcraft:env_slow_time"),
                list(
                        pool(6, "mystcraft:biome_meadow", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_cherry_grove",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_forest",
                                "mystcraft:biome_plains", "mystcraft:biome_old_growth_spruce_taiga"),
                        pool(2, "mystcraft:spikes", "mystcraft:obelisks",
                                "mystcraft:spheres", "mystcraft:floating_islands",
                                "mystcraft:star_fissure_feature"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:dense_ores"),
                        pool(2, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:ruined_portals", "mystcraft:dungeons")
                )
        ));

        register(new Preset("mushroom", "Fungal Kingdom",
                list("mystcraft:terrain_normal", "mystcraft:biome_huge",
                        "mystcraft:sun_normal", "mystcraft:moon_full", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_rain",
                        "mystcraft:caves", "mystcraft:lush_caves", "mystcraft:huge_trees",
                        "mystcraft:surface_lakes", "mystcraft:deep_lakes",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_lime", "mystcraft:color_grass",
                        "mystcraft:color_teal", "mystcraft:color_foliage",
                        "mystcraft:gradient_dawn",
                        "mystcraft:env_longer_days"),
                list(
                        pool(5, "mystcraft:biome_mushroom_fields", "mystcraft:biome_dark_forest",
                                "mystcraft:biome_swamp", "mystcraft:biome_mangrove_swamp",
                                "mystcraft:biome_jungle", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_forest"),
                        pool(3, "mystcraft:villages", "mystcraft:witch_huts",
                                "mystcraft:trail_ruins", "mystcraft:dungeons",
                                "mystcraft:jungle_temples", "mystcraft:woodland_mansions"),
                        pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:extra_copper_ore", "mystcraft:extra_coal_ore",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:spheres", "mystcraft:crystal_formation",
                                "mystcraft:obelisks", "mystcraft:rainbow",
                                "mystcraft:perlin_worms")
                )
        ));

        register(new Preset("ancient", "Ancient Civilization",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_dark", "mystcraft:moon_full", "mystcraft:stars_normal",
                        "mystcraft:lighting_dark", "mystcraft:weather_cloudy",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:deep_dark", "mystcraft:dripstone_caves",
                        "mystcraft:perlin_worms",
                        "mystcraft:color_bronze", "mystcraft:color_sky",
                        "mystcraft:color_olive", "mystcraft:color_fog",
                        "mystcraft:color_maroon", "mystcraft:color_grass",
                        "mystcraft:color_brown", "mystcraft:color_foliage",
                        "mystcraft:gradient_dusk",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_taiga", "mystcraft:biome_plains",
                                "mystcraft:biome_swamp", "mystcraft:biome_mangrove_swamp",
                                "mystcraft:biome_forest", "mystcraft:biome_deep_dark",
                                "mystcraft:biome_meadow"),
                        pool(4, "mystcraft:ancient_cities", "mystcraft:trail_ruins",
                                "mystcraft:strongholds", "mystcraft:dungeons",
                                "mystcraft:woodland_mansions", "mystcraft:mineshafts"),
                        pool(2, "mystcraft:obelisks", "mystcraft:crystal_formation",
                                "mystcraft:star_fissure_feature", "mystcraft:tendrils",
                                "mystcraft:vertical_tendrils"),
                        pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:dense_ores")
                )
        ));

        register(new Preset("twilight", "Eternal Twilight",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_slow", "mystcraft:moon_slow", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_cloudy",
                        "mystcraft:caves", "mystcraft:lush_caves",
                        "mystcraft:surface_lakes", "mystcraft:deep_lakes",
                        "mystcraft:color_indigo", "mystcraft:color_sky",
                        "mystcraft:color_lavender", "mystcraft:color_fog",
                        "mystcraft:color_olive", "mystcraft:color_grass",
                        "mystcraft:color_teal", "mystcraft:color_foliage",
                        "mystcraft:color_sapphire", "mystcraft:color_water",
                        "mystcraft:gradient_dusk",
                        "mystcraft:env_slow_time"),
                list(
                        pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_forest",
                                "mystcraft:biome_taiga", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_meadow", "mystcraft:biome_birch_forest",
                                "mystcraft:biome_swamp", "mystcraft:biome_cherry_grove"),
                        pool(3, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:witch_huts", "mystcraft:dungeons",
                                "mystcraft:woodland_mansions", "mystcraft:strongholds"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature", "mystcraft:huge_trees",
                                "mystcraft:spheres", "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_gold_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:dense_ores")
                )
        ));

        // ===================================================================
        // --- Nether Variants ---
        // ===================================================================

        register(new Preset("nether", "Infernal Wastes",
                list("mystcraft:terrain_nether", "mystcraft:biome_medium",
                        "mystcraft:sun_dark", "mystcraft:moon_dark", "mystcraft:stars_end",
                        "mystcraft:lighting_nether", "mystcraft:weather_off",
                        "mystcraft:sea_lava", "mystcraft:dense_ores",
                        "mystcraft:color_crimson", "mystcraft:color_sky",
                        "mystcraft:color_maroon", "mystcraft:color_fog",
                        "mystcraft:env_scorched", "mystcraft:gradient_blood_sky",
                        "mystcraft:cloud_none",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(4, "mystcraft:biome_nether_wastes", "mystcraft:biome_soul_sand_valley",
                                "mystcraft:biome_crimson_forest", "mystcraft:biome_warped_forest",
                                "mystcraft:biome_basalt_deltas"),
                        pool(3, "mystcraft:nether_fortress", "mystcraft:bastion_remnants",
                                "mystcraft:dungeons", "mystcraft:ruined_portals",
                                "mystcraft:ancient_cities", "mystcraft:nether_fossils"),
                        pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:extra_redstone_ore"),
                        pool(2, "mystcraft:tendrils", "mystcraft:spikes",
                                "mystcraft:spheres", "mystcraft:vertical_tendrils",
                                "mystcraft:obelisks", "mystcraft:perlin_worms"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning",
                                "mystcraft:env_explosions")
                )
        ));

        register(new Preset("nether_flat", "Obsidian Expanse",
                list("mystcraft:terrain_flat", "mystcraft:biome_single",
                        "mystcraft:sun_dark", "mystcraft:moon_dark", "mystcraft:stars_end",
                        "mystcraft:lighting_nether", "mystcraft:weather_off",
                        "mystcraft:block_minecraft_netherrack",
                        "mystcraft:biome_nether_wastes",
                        "mystcraft:color_ruby", "mystcraft:color_sky",
                        "mystcraft:color_crimson", "mystcraft:color_fog",
                        "mystcraft:env_scorched", "mystcraft:cloud_none",
                        "mystcraft:gradient_blood_sky"),
                list(
                        pool(3, "mystcraft:nether_fortress", "mystcraft:bastion_remnants",
                                "mystcraft:ruined_portals", "mystcraft:dungeons",
                                "mystcraft:ancient_cities", "mystcraft:nether_fossils"),
                        pool(2, "mystcraft:spikes", "mystcraft:obelisks",
                                "mystcraft:crystal_formation", "mystcraft:tendrils",
                                "mystcraft:spheres"),
                        pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:dense_ores"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning",
                                "mystcraft:env_explosions")
                )
        ));

        register(new Preset("nether_corrupted", "Corrupted Overworld",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_dark", "mystcraft:moon_dark", "mystcraft:stars_end",
                        "mystcraft:lighting_nether", "mystcraft:weather_storm",
                        "mystcraft:block_minecraft_netherrack", "mystcraft:sea_lava",
                        "mystcraft:caves", "mystcraft:ravines", "mystcraft:perlin_worms",
                        "mystcraft:env_lightning", "mystcraft:env_scorched",
                        "mystcraft:color_crimson", "mystcraft:color_sky",
                        "mystcraft:color_maroon", "mystcraft:color_fog",
                        "mystcraft:color_ruby", "mystcraft:color_grass",
                        "mystcraft:color_crimson", "mystcraft:color_foliage",
                        "mystcraft:gradient_blood_sky", "mystcraft:cloud_low",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(4, "mystcraft:biome_crimson_forest", "mystcraft:biome_warped_forest",
                                "mystcraft:biome_soul_sand_valley", "mystcraft:biome_basalt_deltas",
                                "mystcraft:biome_nether_wastes"),
                        pool(3, "mystcraft:nether_fortress", "mystcraft:bastion_remnants",
                                "mystcraft:dungeons", "mystcraft:ruined_portals",
                                "mystcraft:ancient_cities", "mystcraft:nether_fossils"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spikes", "mystcraft:spheres",
                                "mystcraft:obelisks"),
                        pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:dense_ores")
                )
        ));

        register(new Preset("lava_world", "Molten Core",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_dark", "mystcraft:moon_dark", "mystcraft:stars_dark",
                        "mystcraft:lighting_nether", "mystcraft:weather_off",
                        "mystcraft:block_minecraft_basalt", "mystcraft:sea_lava",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:env_scorched", "mystcraft:cloud_none",
                        "mystcraft:color_ruby", "mystcraft:color_sky",
                        "mystcraft:color_crimson", "mystcraft:color_fog",
                        "mystcraft:gradient_blood_sky", "mystcraft:horizon_low",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(5, "mystcraft:biome_basalt_deltas", "mystcraft:biome_nether_wastes",
                                "mystcraft:biome_soul_sand_valley", "mystcraft:biome_badlands",
                                "mystcraft:biome_eroded_badlands", "mystcraft:biome_crimson_forest",
                                "mystcraft:biome_warped_forest"),
                        pool(3, "mystcraft:nether_fortress", "mystcraft:bastion_remnants",
                                "mystcraft:dungeons", "mystcraft:ruined_portals",
                                "mystcraft:ancient_cities", "mystcraft:nether_fossils"),
                        pool(2, "mystcraft:tendrils", "mystcraft:spikes",
                                "mystcraft:spheres", "mystcraft:vertical_tendrils",
                                "mystcraft:obelisks", "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:dense_ores"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning",
                                "mystcraft:env_explosions")
                )
        ));

        // ===================================================================
        // --- End Variants ---
        // ===================================================================

        register(new Preset("end", "Outer End",
                list("mystcraft:terrain_end", "mystcraft:biome_medium",
                        "mystcraft:stars_end", "mystcraft:lighting_dark",
                        "mystcraft:weather_off",
                        "mystcraft:color_indigo", "mystcraft:color_sky",
                        "mystcraft:color_violet", "mystcraft:color_fog",
                        "mystcraft:hide_horizon", "mystcraft:cloud_none",
                        "mystcraft:env_static_time"),
                list(
                        pool(3, "mystcraft:biome_the_end", "mystcraft:biome_end_midlands",
                                "mystcraft:biome_end_highlands", "mystcraft:biome_end_barrens",
                                "mystcraft:biome_small_end_islands"),
                        pool(2, "mystcraft:end_cities", "mystcraft:dungeons",
                                "mystcraft:ancient_cities", "mystcraft:strongholds"),
                        pool(3, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:spikes", "mystcraft:spheres",
                                "mystcraft:floating_islands", "mystcraft:star_fissure_feature"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning",
                                "mystcraft:env_accelerated")
                )
        ));

        register(new Preset("end_corrupted", "Void-Touched Overworld",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:stars_end", "mystcraft:lighting_dark",
                        "mystcraft:weather_off",
                        "mystcraft:block_minecraft_end_stone",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:color_indigo", "mystcraft:color_sky",
                        "mystcraft:color_violet", "mystcraft:color_fog",
                        "mystcraft:color_navy", "mystcraft:color_grass",
                        "mystcraft:color_indigo", "mystcraft:color_foliage",
                        "mystcraft:gradient_dusk", "mystcraft:cloud_none",
                        "mystcraft:env_slow_time"),
                list(
                        pool(4, "mystcraft:biome_end_midlands", "mystcraft:biome_end_highlands",
                                "mystcraft:biome_the_end", "mystcraft:biome_end_barrens",
                                "mystcraft:biome_small_end_islands"),
                        pool(3, "mystcraft:end_cities", "mystcraft:ancient_cities",
                                "mystcraft:strongholds", "mystcraft:dungeons",
                                "mystcraft:mineshafts"),
                        pool(3, "mystcraft:spikes", "mystcraft:obelisks",
                                "mystcraft:floating_islands", "mystcraft:crystal_formation",
                                "mystcraft:spheres", "mystcraft:vertical_tendrils"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:dense_ores")
                )
        ));

        register(new Preset("end_skylands", "Fractured Ender Sky",
                list("mystcraft:terrain_skylands", "mystcraft:biome_small",
                        "mystcraft:stars_end", "mystcraft:lighting_dark",
                        "mystcraft:weather_off",
                        "mystcraft:block_minecraft_end_stone",
                        "mystcraft:floating_islands",
                        "mystcraft:color_violet", "mystcraft:color_sky",
                        "mystcraft:color_indigo", "mystcraft:color_fog",
                        "mystcraft:hide_horizon", "mystcraft:cloud_none",
                        "mystcraft:env_static_time"),
                list(
                        pool(3, "mystcraft:biome_end_highlands", "mystcraft:biome_end_midlands",
                                "mystcraft:biome_the_end", "mystcraft:biome_end_barrens"),
                        pool(2, "mystcraft:end_cities", "mystcraft:ancient_cities",
                                "mystcraft:dungeons", "mystcraft:strongholds"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:spikes", "mystcraft:spheres"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning",
                                "mystcraft:env_accelerated")
                )
        ));

        // ===================================================================
        // --- Atmospheric ---
        // ===================================================================

        register(new Preset("aurora", "Aurora Borealis",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_slow", "mystcraft:moon_large", "mystcraft:stars_dense",
                        "mystcraft:lighting_normal", "mystcraft:weather_snow",
                        "mystcraft:caves", "mystcraft:lush_caves",
                        "mystcraft:surface_lakes", "mystcraft:cloud_high",
                        "mystcraft:color_turquoise", "mystcraft:color_sky",
                        "mystcraft:color_lavender", "mystcraft:color_fog",
                        "mystcraft:color_sapphire", "mystcraft:color_water",
                        "mystcraft:color_jade", "mystcraft:color_cloud",
                        "mystcraft:gradient_aurora", "mystcraft:rainbow",
                        "mystcraft:env_longer_days"),
                list(
                        pool(5, "mystcraft:biome_snowy_taiga", "mystcraft:biome_grove",
                                "mystcraft:biome_frozen_peaks", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_taiga", "mystcraft:biome_meadow",
                                "mystcraft:biome_snowy_plains", "mystcraft:biome_forest"),
                        pool(3, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:dungeons", "mystcraft:strongholds",
                                "mystcraft:igloos", "mystcraft:mineshafts"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:spikes", "mystcraft:huge_trees",
                                "mystcraft:star_fissure_feature"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:dense_ores")
                )
        ));

        register(new Preset("blood_moon", "Blood Moon Rising",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_dark", "mystcraft:moon_large", "mystcraft:stars_sparse",
                        "mystcraft:lighting_dark", "mystcraft:weather_storm",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:env_lightning", "mystcraft:cloud_low",
                        "mystcraft:color_crimson", "mystcraft:color_sky",
                        "mystcraft:color_maroon", "mystcraft:color_fog",
                        "mystcraft:color_ruby", "mystcraft:color_grass",
                        "mystcraft:color_crimson", "mystcraft:color_foliage",
                        "mystcraft:color_ruby", "mystcraft:color_water",
                        "mystcraft:gradient_blood_sky", "mystcraft:horizon_low",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_swamp",
                                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_taiga",
                                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_deep_dark",
                                "mystcraft:biome_plains", "mystcraft:biome_forest"),
                        pool(3, "mystcraft:woodland_mansions", "mystcraft:ancient_cities",
                                "mystcraft:witch_huts", "mystcraft:dungeons",
                                "mystcraft:strongholds", "mystcraft:trail_ruins"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:obelisks", "mystcraft:spikes",
                                "mystcraft:crystal_formation", "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:dense_ores"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_scorched",
                                "mystcraft:env_explosions")
                )
        ));

        register(new Preset("storm", "Eternal Storm",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_dark", "mystcraft:moon_normal", "mystcraft:stars_dark",
                        "mystcraft:lighting_dark", "mystcraft:weather_storm",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:env_lightning", "mystcraft:cloud_low",
                        "mystcraft:color_dark_gray", "mystcraft:color_sky",
                        "mystcraft:color_gray", "mystcraft:color_fog",
                        "mystcraft:color_olive", "mystcraft:color_grass",
                        "mystcraft:color_navy", "mystcraft:color_water",
                        "mystcraft:gradient_blood_sky",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_swamp",
                                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_taiga",
                                "mystcraft:biome_windswept_hills", "mystcraft:biome_windswept_forest",
                                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_deep_dark",
                                "mystcraft:biome_forest"),
                        pool(3, "mystcraft:woodland_mansions", "mystcraft:witch_huts",
                                "mystcraft:dungeons", "mystcraft:ancient_cities",
                                "mystcraft:strongholds", "mystcraft:mineshafts"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spikes", "mystcraft:deep_dark",
                                "mystcraft:dripstone_caves", "mystcraft:lush_caves",
                                "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:dense_ores"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_scorched",
                                "mystcraft:env_explosions")
                )
        ));

        register(new Preset("rainbow", "Rainbow Prism",
                list("mystcraft:terrain_stripes", "mystcraft:biome_small",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:caves", "mystcraft:surface_lakes",
                        "mystcraft:color_red", "mystcraft:color_grass",
                        "mystcraft:color_orange", "mystcraft:color_grass",
                        "mystcraft:color_yellow", "mystcraft:color_grass",
                        "mystcraft:color_lime", "mystcraft:color_grass",
                        "mystcraft:color_cyan", "mystcraft:color_grass",
                        "mystcraft:color_blue", "mystcraft:color_grass",
                        "mystcraft:color_purple", "mystcraft:color_grass",
                        "mystcraft:color_magenta", "mystcraft:color_grass",
                        "mystcraft:color_cyan", "mystcraft:color_foliage",
                        "mystcraft:color_pink", "mystcraft:color_sky",
                        "mystcraft:color_magenta", "mystcraft:color_fog",
                        "mystcraft:color_turquoise", "mystcraft:color_water",
                        "mystcraft:color_gold", "mystcraft:color_cloud",
                        "mystcraft:rainbow", "mystcraft:gradient_aurora",
                        "mystcraft:cloud_high",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_meadow", "mystcraft:biome_cherry_grove",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_forest",
                                "mystcraft:biome_jungle", "mystcraft:biome_mushroom_fields",
                                "mystcraft:biome_savanna", "mystcraft:biome_taiga"),
                        pool(3, "mystcraft:tendrils", "mystcraft:spheres",
                                "mystcraft:spikes", "mystcraft:vertical_tendrils",
                                "mystcraft:floating_islands", "mystcraft:crystal_formation"),
                        pool(2, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:ruined_portals", "mystcraft:dungeons"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_gold_ore", "mystcraft:dense_ores")
                )
        ));

        // ===================================================================
        // --- Mixed / Pattern Terrains ---
        // ===================================================================

        register(new Preset("checkerboard", "Patchwork Realm",
                list("mystcraft:terrain_checkerboard", "mystcraft:biome_grid",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:surface_lakes"),
                list(
                        pool(7, "mystcraft:biome_plains", "mystcraft:biome_desert",
                                "mystcraft:biome_jungle", "mystcraft:biome_snowy_plains",
                                "mystcraft:biome_forest", "mystcraft:biome_badlands",
                                "mystcraft:biome_swamp", "mystcraft:biome_dark_forest",
                                "mystcraft:biome_taiga", "mystcraft:biome_birch_forest",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_meadow"),
                        pool(3, "mystcraft:villages", "mystcraft:dungeons",
                                "mystcraft:mineshafts", "mystcraft:strongholds",
                                "mystcraft:desert_temples", "mystcraft:jungle_temples",
                                "mystcraft:pillager_outposts"),
                        pool(2, "mystcraft:dense_ores", "mystcraft:tendrils",
                                "mystcraft:spheres", "mystcraft:spikes",
                                "mystcraft:dripstone_caves", "mystcraft:lush_caves",
                                "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:extra_emerald_ore")
                )
        ));

        register(new Preset("blend", "Shifting Lands",
                list("mystcraft:terrain_blend", "mystcraft:biome_tiled",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_fast",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:color_amber", "mystcraft:color_sky",
                        "mystcraft:color_silver", "mystcraft:color_fog",
                        "mystcraft:surface_lakes"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest",
                                "mystcraft:biome_taiga", "mystcraft:biome_savanna",
                                "mystcraft:biome_jungle", "mystcraft:biome_meadow",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_swamp"),
                        pool(3, "mystcraft:villages", "mystcraft:ruined_portals",
                                "mystcraft:trail_ruins", "mystcraft:mineshafts",
                                "mystcraft:pillager_outposts", "mystcraft:dungeons"),
                        pool(3, "mystcraft:floating_islands", "mystcraft:tendrils",
                                "mystcraft:vertical_tendrils", "mystcraft:spheres",
                                "mystcraft:spikes", "mystcraft:huge_trees",
                                "mystcraft:dripstone_caves", "mystcraft:lush_caves",
                                "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:dense_ores"),
                        pool(1, "mystcraft:gradient_sunset", "mystcraft:gradient_dusk",
                                "mystcraft:color_gold", "mystcraft:color_sunset")
                )
        ));

        register(new Preset("stripes", "Banded World",
                list("mystcraft:terrain_stripes", "mystcraft:biome_large",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:dense_ores",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:surface_lakes", "mystcraft:dripstone_caves"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_desert",
                                "mystcraft:biome_forest", "mystcraft:biome_snowy_plains",
                                "mystcraft:biome_jungle", "mystcraft:biome_badlands",
                                "mystcraft:biome_savanna", "mystcraft:biome_mushroom_fields",
                                "mystcraft:biome_taiga", "mystcraft:biome_dark_forest"),
                        pool(3, "mystcraft:villages", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:mineshafts",
                                "mystcraft:pillager_outposts", "mystcraft:ruined_portals",
                                "mystcraft:desert_temples"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_copper_ore"),
                        pool(2, "mystcraft:tendrils", "mystcraft:spikes",
                                "mystcraft:spheres", "mystcraft:lush_caves",
                                "mystcraft:huge_trees", "mystcraft:perlin_worms")
                )
        ));

        register(new Preset("flat", "The Flats",
                list("mystcraft:terrain_flat", "mystcraft:biome_single",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_grass_natural"),
                list(
                        pool(1, "mystcraft:biome_plains", "mystcraft:biome_desert",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_meadow"),
                        pool(3, "mystcraft:villages", "mystcraft:pillager_outposts",
                                "mystcraft:witch_huts", "mystcraft:ruined_portals",
                                "mystcraft:dungeons", "mystcraft:trail_ruins"),
                        pool(2, "mystcraft:obelisks", "mystcraft:crystal_formation",
                                "mystcraft:star_fissure_feature", "mystcraft:spikes",
                                "mystcraft:spheres"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:dense_ores",
                                "mystcraft:extra_emerald_ore")
                )
        ));

        // ===================================================================
        // --- Chaotic / Unstable ---
        // ===================================================================

        register(new Preset("chaos", "Shattered Realm",
                list("mystcraft:terrain_checkerboard", "mystcraft:biome_grid",
                        "mystcraft:sun_fast", "mystcraft:moon_fast", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_fast",
                        "mystcraft:caves", "mystcraft:ravines", "mystcraft:dense_ores",
                        "mystcraft:floating_islands", "mystcraft:perlin_worms",
                        "mystcraft:color_coral", "mystcraft:color_sky",
                        "mystcraft:color_amber", "mystcraft:color_fog",
                        "mystcraft:color_jade", "mystcraft:color_grass",
                        "mystcraft:color_violet", "mystcraft:color_foliage",
                        "mystcraft:color_turquoise", "mystcraft:color_water",
                        "mystcraft:gradient_aurora",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(6, "mystcraft:biome_nether_wastes", "mystcraft:biome_plains",
                                "mystcraft:biome_the_end", "mystcraft:biome_desert",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_ice_spikes",
                                "mystcraft:biome_jungle", "mystcraft:biome_deep_dark",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_badlands"),
                        pool(3, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spheres", "mystcraft:spikes",
                                "mystcraft:obelisks", "mystcraft:crystal_formation"),
                        pool(4, "mystcraft:nether_fortress", "mystcraft:end_cities",
                                "mystcraft:ancient_cities", "mystcraft:woodland_mansions",
                                "mystcraft:ocean_monuments", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:shipwrecks",
                                "mystcraft:igloos", "mystcraft:nether_fossils",
                                "mystcraft:ocean_ruins", "mystcraft:buried_treasure"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_emerald_ore", "mystcraft:extra_redstone_ore"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning",
                                "mystcraft:env_scorched", "mystcraft:env_explosions")
                )
        ));

        register(new Preset("unstable", "Horrible Instability",
                list("mystcraft:terrain_normal", "mystcraft:biome_small",
                        "mystcraft:sun_fast", "mystcraft:moon_fast", "mystcraft:stars_end",
                        "mystcraft:lighting_dark", "mystcraft:weather_storm",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:env_meteors", "mystcraft:env_lightning",
                        "mystcraft:env_scorched", "mystcraft:env_explosions",
                        "mystcraft:env_accelerated",
                        "mystcraft:dense_ores",
                        "mystcraft:floating_islands", "mystcraft:perlin_worms",
                        "mystcraft:color_crimson", "mystcraft:color_sky",
                        "mystcraft:color_maroon", "mystcraft:color_fog",
                        "mystcraft:color_ruby", "mystcraft:color_grass",
                        "mystcraft:color_crimson", "mystcraft:color_foliage",
                        "mystcraft:color_navy", "mystcraft:color_water",
                        "mystcraft:color_dark_gray", "mystcraft:color_cloud",
                        "mystcraft:gradient_blood_sky", "mystcraft:cloud_low",
                        "mystcraft:hide_horizon",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(8, "mystcraft:biome_nether_wastes", "mystcraft:biome_the_end",
                                "mystcraft:biome_desert", "mystcraft:biome_mushroom_fields",
                                "mystcraft:biome_ice_spikes", "mystcraft:biome_jungle",
                                "mystcraft:biome_deep_dark", "mystcraft:biome_dark_forest",
                                "mystcraft:biome_badlands", "mystcraft:biome_soul_sand_valley",
                                "mystcraft:biome_basalt_deltas", "mystcraft:biome_crimson_forest"),
                        pool(4, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spheres", "mystcraft:spikes",
                                "mystcraft:obelisks", "mystcraft:crystal_formation"),
                        pool(4, "mystcraft:nether_fortress", "mystcraft:end_cities",
                                "mystcraft:ancient_cities", "mystcraft:woodland_mansions",
                                "mystcraft:ocean_monuments", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:bastion_remnants"),
                        pool(3, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_emerald_ore", "mystcraft:extra_redstone_ore",
                                "mystcraft:extra_lapis_ore")
                )
        ));

        // ===================================================================
        // --- Void Variants ---
        // ===================================================================

        register(new Preset("void", "The Empty",
                list("mystcraft:terrain_void", "mystcraft:biome_single",
                        "mystcraft:sun_dark", "mystcraft:moon_dark",
                        "mystcraft:lighting_dark", "mystcraft:weather_off",
                        "mystcraft:no_ores",
                        "mystcraft:biome_plains",
                        "mystcraft:color_black", "mystcraft:color_sky",
                        "mystcraft:color_dark_gray", "mystcraft:color_fog",
                        "mystcraft:hide_horizon", "mystcraft:cloud_none",
                        "mystcraft:env_static_time"),
                list(
                        pool(1, "mystcraft:stars_end", "mystcraft:stars_sparse",
                                "mystcraft:stars_dark"),
                        pool(3, "mystcraft:star_fissure_feature", "mystcraft:crystal_formation",
                                "mystcraft:obelisks", "mystcraft:floating_islands",
                                "mystcraft:spheres"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning",
                                "mystcraft:rainbow")
                )
        ));

        register(new Preset("void_islands", "Shattered Void",
                list("mystcraft:terrain_void", "mystcraft:biome_small",
                        "mystcraft:sun_normal", "mystcraft:moon_normal",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:floating_islands", "mystcraft:no_ores",
                        "mystcraft:color_light_blue", "mystcraft:color_sky",
                        "mystcraft:color_ivory", "mystcraft:color_fog",
                        "mystcraft:color_silver", "mystcraft:color_cloud",
                        "mystcraft:rainbow",
                        "mystcraft:cloud_high"),
                list(
                        pool(1, "mystcraft:stars_twinkle", "mystcraft:stars_dense",
                                "mystcraft:stars_sparse"),
                        pool(5, "mystcraft:biome_plains", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_meadow", "mystcraft:biome_birch_forest",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_cherry_grove",
                                "mystcraft:biome_forest"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature", "mystcraft:spheres",
                                "mystcraft:spikes"),
                        pool(2, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:ruined_portals", "mystcraft:dungeons"),
                        pool(1, "mystcraft:gradient_aurora", "mystcraft:gradient_dawn",
                                "mystcraft:color_gold", "mystcraft:color_sunset")
                )
        ));

        // ===================================================================
        // --- Deep / Underground ---
        // ===================================================================

        register(new Preset("deep_dark", "Sculk Depths",
                list("mystcraft:terrain_cave", "mystcraft:biome_single",
                        "mystcraft:stars_dark", "mystcraft:lighting_dark",
                        "mystcraft:weather_off",
                        "mystcraft:block_minecraft_deepslate",
                        "mystcraft:deep_dark", "mystcraft:caves",
                        "mystcraft:dense_ores", "mystcraft:perlin_worms",
                        "mystcraft:biome_deep_dark",
                        "mystcraft:dripstone_caves",
                        "mystcraft:color_dark_gray", "mystcraft:color_fog",
                        "mystcraft:cloud_none",
                        "mystcraft:env_static_time"),
                list(
                        pool(3, "mystcraft:ancient_cities", "mystcraft:dungeons",
                                "mystcraft:strongholds", "mystcraft:mineshafts",
                                "mystcraft:trail_ruins"),
                        pool(3, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_redstone_ore",
                                "mystcraft:extra_emerald_ore", "mystcraft:extra_iron_ore"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spheres", "mystcraft:spikes",
                                "mystcraft:crystal_formation"),
                        pool(1, "mystcraft:env_scorched", "mystcraft:env_lightning",
                                "mystcraft:env_meteors")
                )
        ));

        register(new Preset("inverted", "Upside Down",
                list("mystcraft:terrain_cave", "mystcraft:biome_medium",
                        "mystcraft:sun_dark", "mystcraft:moon_dark", "mystcraft:stars_dark",
                        "mystcraft:lighting_dark", "mystcraft:weather_off",
                        "mystcraft:caves", "mystcraft:dripstone_caves", "mystcraft:lush_caves",
                        "mystcraft:dense_ores", "mystcraft:cloud_none",
                        "mystcraft:perlin_worms",
                        "mystcraft:color_dark_gray", "mystcraft:color_fog",
                        "mystcraft:deep_dark",
                        "mystcraft:env_slow_time"),
                list(
                        pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_swamp",
                                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_mushroom_fields", "mystcraft:biome_taiga",
                                "mystcraft:biome_deep_dark", "mystcraft:biome_plains"),
                        pool(3, "mystcraft:mineshafts", "mystcraft:ancient_cities",
                                "mystcraft:dungeons", "mystcraft:strongholds",
                                "mystcraft:trail_ruins"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spheres", "mystcraft:spikes",
                                "mystcraft:crystal_formation"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_iron_ore")
                )
        ));

        register(new Preset("mining", "Miner's Paradise",
                list("mystcraft:terrain_normal", "mystcraft:biome_native",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:dripstone_caves", "mystcraft:lush_caves",
                        "mystcraft:dense_ores", "mystcraft:perlin_worms",
                        "mystcraft:extra_diamond_ore", "mystcraft:extra_iron_ore",
                        "mystcraft:extra_gold_ore", "mystcraft:extra_emerald_ore",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:env_longer_days"),
                list(
                        pool(3, "mystcraft:mineshafts", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:ancient_cities",
                                "mystcraft:trail_ruins"),
                        pool(2, "mystcraft:extra_copper_ore", "mystcraft:extra_redstone_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_coal_ore"),
                        pool(2, "mystcraft:deep_dark", "mystcraft:deep_lakes",
                                "mystcraft:surface_lakes", "mystcraft:tendrils",
                                "mystcraft:vertical_tendrils"),
                        pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature")
                )
        ));

        // ===================================================================
        // --- Frozen Variants ---
        // ===================================================================

        register(new Preset("ice_age", "Glacial Tomb",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_small", "mystcraft:moon_large",
                        "mystcraft:lighting_bright", "mystcraft:weather_blizzard",
                        "mystcraft:block_minecraft_packed_ice", "mystcraft:sea_blue_ice",
                        "mystcraft:caves", "mystcraft:cloud_low",
                        "mystcraft:color_light_blue", "mystcraft:color_sky",
                        "mystcraft:color_silver", "mystcraft:color_fog",
                        "mystcraft:color_sapphire", "mystcraft:color_water",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_frozen_peaks", "mystcraft:biome_ice_spikes",
                                "mystcraft:biome_snowy_plains", "mystcraft:biome_frozen_river",
                                "mystcraft:biome_snowy_taiga", "mystcraft:biome_grove",
                                "mystcraft:biome_snowy_slopes", "mystcraft:biome_snowy_beach"),
                        pool(1, "mystcraft:stars_twinkle", "mystcraft:stars_dense",
                                "mystcraft:stars_normal"),
                        pool(3, "mystcraft:spikes", "mystcraft:crystal_formation",
                                "mystcraft:obelisks", "mystcraft:spheres",
                                "mystcraft:dripstone_caves", "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:extra_emerald_ore", "mystcraft:extra_coal_ore",
                                "mystcraft:dense_ores"),
                        pool(3, "mystcraft:villages", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:mineshafts",
                                "mystcraft:pillager_outposts", "mystcraft:igloos"),
                        pool(1, "mystcraft:gradient_dawn", "mystcraft:gradient_dusk",
                                "mystcraft:color_sunset_natural", "mystcraft:gradient_aurora")
                )
        ));

        // ===================================================================
        // --- Desolation ---
        // ===================================================================

        register(new Preset("wasteland", "Blighted Wasteland",
                list("mystcraft:terrain_flat", "mystcraft:biome_single",
                        "mystcraft:sun_dark", "mystcraft:moon_dark", "mystcraft:stars_dark",
                        "mystcraft:lighting_dark", "mystcraft:weather_storm",
                        "mystcraft:no_ores",
                        "mystcraft:biome_badlands",
                        "mystcraft:env_scorched", "mystcraft:env_lightning",
                        "mystcraft:color_maroon", "mystcraft:color_sky",
                        "mystcraft:color_brown", "mystcraft:color_fog",
                        "mystcraft:color_olive", "mystcraft:color_grass",
                        "mystcraft:color_maroon", "mystcraft:color_foliage",
                        "mystcraft:hide_horizon", "mystcraft:cloud_low",
                        "mystcraft:gradient_blood_sky",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(2, "mystcraft:dungeons", "mystcraft:ruined_portals",
                                "mystcraft:ancient_cities", "mystcraft:strongholds",
                                "mystcraft:mineshafts"),
                        pool(2, "mystcraft:obelisks", "mystcraft:spikes",
                                "mystcraft:tendrils", "mystcraft:crystal_formation",
                                "mystcraft:spheres"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_explosions",
                                "mystcraft:env_accelerated")
                )
        ));

        register(new Preset("giant_sun", "Solar Dominion",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_large", "mystcraft:moon_small", "mystcraft:stars_sparse",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:surface_lakes", "mystcraft:cloud_none",
                        "mystcraft:color_gold", "mystcraft:color_sky",
                        "mystcraft:color_amber", "mystcraft:color_fog",
                        "mystcraft:color_gold", "mystcraft:color_grass",
                        "mystcraft:color_bronze", "mystcraft:color_water",
                        "mystcraft:gradient_sunset", "mystcraft:env_scorched",
                        "mystcraft:env_shorter_days"),
                list(
                        pool(6, "mystcraft:biome_desert", "mystcraft:biome_badlands",
                                "mystcraft:biome_savanna", "mystcraft:biome_eroded_badlands",
                                "mystcraft:biome_stony_shore", "mystcraft:biome_plains",
                                "mystcraft:biome_windswept_hills", "mystcraft:biome_meadow"),
                        pool(3, "mystcraft:villages", "mystcraft:desert_temples",
                                "mystcraft:pillager_outposts", "mystcraft:dungeons",
                                "mystcraft:ruined_portals", "mystcraft:trail_ruins"),
                        pool(2, "mystcraft:spikes", "mystcraft:obelisks",
                                "mystcraft:crystal_formation", "mystcraft:tendrils",
                                "mystcraft:star_fissure_feature"),
                        pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_copper_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:dense_ores")
                )
        ));

        // ===================================================================
        // --- New Thematic Presets ---
        // ===================================================================

        register(new Preset("frozen_time", "Moment Suspended",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_bright", "mystcraft:weather_off",
                        "mystcraft:caves", "mystcraft:lush_caves",
                        "mystcraft:surface_lakes", "mystcraft:cloud_high",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_grass_natural", "mystcraft:color_water_natural",
                        "mystcraft:color_foliage_natural",
                        "mystcraft:gradient_sunset",
                        "mystcraft:env_static_time"),
                list(
                        pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_meadow",
                                "mystcraft:biome_plains", "mystcraft:biome_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_cherry_grove",
                                "mystcraft:biome_taiga", "mystcraft:biome_grove"),
                        pool(3, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:dungeons", "mystcraft:ruined_portals",
                                "mystcraft:strongholds"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature", "mystcraft:huge_trees"),
                        pool(2, "mystcraft:extra_iron_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:extra_coal_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:dense_ores")
                )
        ));

        register(new Preset("accelerated", "Time Vortex",
                list("mystcraft:terrain_normal", "mystcraft:biome_small",
                        "mystcraft:sun_fast", "mystcraft:moon_fast", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_fast",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:color_amber", "mystcraft:color_sky",
                        "mystcraft:color_gold", "mystcraft:color_fog",
                        "mystcraft:env_accelerated", "mystcraft:env_shorter_days",
                        "mystcraft:gradient_aurora"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_meadow", "mystcraft:biome_savanna",
                                "mystcraft:biome_taiga", "mystcraft:biome_jungle"),
                        pool(3, "mystcraft:villages", "mystcraft:dungeons",
                                "mystcraft:trail_ruins", "mystcraft:pillager_outposts",
                                "mystcraft:ruined_portals", "mystcraft:strongholds"),
                        pool(2, "mystcraft:dense_ores", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_gold_ore", "mystcraft:extra_iron_ore"),
                        pool(2, "mystcraft:tendrils", "mystcraft:spheres",
                                "mystcraft:spikes", "mystcraft:floating_islands",
                                "mystcraft:perlin_worms"),
                        pool(1, "mystcraft:env_lightning", "mystcraft:env_meteors",
                                "mystcraft:crystal_formation")
                )
        ));

        register(new Preset("worm_tunnels", "Wormsign",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:dripstone_caves", "mystcraft:lush_caves",
                        "mystcraft:perlin_worms",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_grass_natural", "mystcraft:color_water_natural"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_taiga",
                                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_meadow",
                                "mystcraft:biome_swamp", "mystcraft:biome_desert",
                                "mystcraft:biome_badlands"),
                        pool(3, "mystcraft:mineshafts", "mystcraft:dungeons",
                                "mystcraft:strongholds", "mystcraft:trail_ruins",
                                "mystcraft:ancient_cities"),
                        pool(3, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_iron_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:extra_lapis_ore"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:deep_dark", "mystcraft:deep_lakes",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:sun_normal", "mystcraft:sun_slow",
                                "mystcraft:sun_large")
                )
        ));

        register(new Preset("treasure_hunt", "Hoard of Ages",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:dense_ores", "mystcraft:perlin_worms",
                        "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                        "mystcraft:extra_emerald_ore", "mystcraft:extra_lapis_ore",
                        "mystcraft:extra_iron_ore", "mystcraft:extra_copper_ore",
                        "mystcraft:extra_redstone_ore", "mystcraft:extra_coal_ore",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_gold", "mystcraft:color_water"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_taiga",
                                "mystcraft:biome_meadow", "mystcraft:biome_savanna",
                                "mystcraft:biome_jungle", "mystcraft:biome_desert"),
                        pool(5, "mystcraft:villages", "mystcraft:dungeons",
                                "mystcraft:mineshafts", "mystcraft:strongholds",
                                "mystcraft:trail_ruins", "mystcraft:desert_temples",
                                "mystcraft:jungle_temples", "mystcraft:buried_treasure",
                                "mystcraft:shipwrecks", "mystcraft:ruined_portals",
                                "mystcraft:ocean_ruins", "mystcraft:woodland_mansions"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature", "mystcraft:lush_caves",
                                "mystcraft:dripstone_caves", "mystcraft:deep_dark")
                )
        ));

        register(new Preset("peaceful_haven", "Sanctuary",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_bright", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:lush_caves",
                        "mystcraft:surface_lakes", "mystcraft:deep_lakes",
                        "mystcraft:huge_trees",
                        "mystcraft:anti_pvp", "mystcraft:star_fissure",
                        "mystcraft:rainbow",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_grass_natural", "mystcraft:color_foliage_natural",
                        "mystcraft:color_water_natural",
                        "mystcraft:gradient_dawn",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_meadow",
                                "mystcraft:biome_plains", "mystcraft:biome_birch_forest",
                                "mystcraft:biome_cherry_grove", "mystcraft:biome_forest",
                                "mystcraft:biome_grove", "mystcraft:biome_bamboo_jungle"),
                        pool(3, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:ruined_portals", "mystcraft:dungeons",
                                "mystcraft:jungle_temples", "mystcraft:witch_huts"),
                        pool(2, "mystcraft:extra_iron_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:extra_coal_ore", "mystcraft:extra_emerald_ore",
                                "mystcraft:dense_ores"),
                        pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature")
                )
        ));

        register(new Preset("scarlet_dusk", "Scarlet Dominion",
                list("mystcraft:terrain_normal", "mystcraft:biome_medium",
                        "mystcraft:sun_slow", "mystcraft:moon_large", "mystcraft:stars_sparse",
                        "mystcraft:lighting_dark", "mystcraft:weather_cloudy",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:color_crimson", "mystcraft:color_sky",
                        "mystcraft:color_ruby", "mystcraft:color_fog",
                        "mystcraft:color_maroon", "mystcraft:color_grass",
                        "mystcraft:color_crimson", "mystcraft:color_foliage",
                        "mystcraft:color_ruby", "mystcraft:color_water",
                        "mystcraft:color_rose", "mystcraft:color_cloud",
                        "mystcraft:gradient_blood_sky",
                        "mystcraft:env_slow_time"),
                list(
                        pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_swamp",
                                "mystcraft:biome_taiga", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_forest", "mystcraft:biome_plains",
                                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_deep_dark"),
                        pool(3, "mystcraft:woodland_mansions", "mystcraft:witch_huts",
                                "mystcraft:ancient_cities", "mystcraft:dungeons",
                                "mystcraft:strongholds"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:obelisks", "mystcraft:crystal_formation",
                                "mystcraft:perlin_worms"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:dense_ores")
                )
        ));

        register(new Preset("jade_empire", "Jade Dominion",
                list("mystcraft:terrain_amplified", "mystcraft:biome_large",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_rain",
                        "mystcraft:caves", "mystcraft:ravines", "mystcraft:lush_caves",
                        "mystcraft:surface_lakes", "mystcraft:deep_lakes",
                        "mystcraft:huge_trees",
                        "mystcraft:color_jade", "mystcraft:color_sky",
                        "mystcraft:color_emerald", "mystcraft:color_fog",
                        "mystcraft:color_jade", "mystcraft:color_grass",
                        "mystcraft:color_emerald", "mystcraft:color_foliage",
                        "mystcraft:color_jade", "mystcraft:color_water",
                        "mystcraft:gradient_dawn",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_jungle", "mystcraft:biome_bamboo_jungle",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_forest",
                                "mystcraft:biome_meadow", "mystcraft:biome_taiga",
                                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_cherry_grove"),
                        pool(3, "mystcraft:villages", "mystcraft:jungle_temples",
                                "mystcraft:trail_ruins", "mystcraft:dungeons",
                                "mystcraft:strongholds", "mystcraft:woodland_mansions"),
                        pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_gold_ore", "mystcraft:extra_copper_ore",
                                "mystcraft:dense_ores"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:spikes", "mystcraft:spheres",
                                "mystcraft:perlin_worms")
                )
        ));

        register(new Preset("sapphire_depths", "Sapphire Abyss",
                list("mystcraft:terrain_cave", "mystcraft:biome_medium",
                        "mystcraft:stars_dark", "mystcraft:lighting_dark",
                        "mystcraft:weather_off",
                        "mystcraft:caves", "mystcraft:dripstone_caves",
                        "mystcraft:lush_caves", "mystcraft:perlin_worms",
                        "mystcraft:dense_ores",
                        "mystcraft:color_sapphire", "mystcraft:color_fog",
                        "mystcraft:cloud_none",
                        "mystcraft:env_static_time"),
                list(
                        pool(6, "mystcraft:biome_ocean", "mystcraft:biome_deep_ocean",
                                "mystcraft:biome_cold_ocean", "mystcraft:biome_lukewarm_ocean",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_swamp",
                                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_deep_dark"),
                        pool(3, "mystcraft:ocean_monuments", "mystcraft:ancient_cities",
                                "mystcraft:dungeons", "mystcraft:mineshafts",
                                "mystcraft:strongholds"),
                        pool(3, "mystcraft:extra_diamond_ore", "mystcraft:extra_lapis_ore",
                                "mystcraft:extra_gold_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:extra_emerald_ore"),
                        pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils",
                                "mystcraft:spheres", "mystcraft:crystal_formation",
                                "mystcraft:deep_dark")
                )
        ));

        register(new Preset("golden_age", "Auric Prosperity",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_large", "mystcraft:moon_normal", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_bright", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:surface_lakes", "mystcraft:cloud_high",
                        "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                        "mystcraft:extra_emerald_ore", "mystcraft:dense_ores",
                        "mystcraft:color_gold", "mystcraft:color_sky",
                        "mystcraft:color_amber", "mystcraft:color_fog",
                        "mystcraft:color_gold", "mystcraft:color_grass",
                        "mystcraft:color_amber", "mystcraft:color_foliage",
                        "mystcraft:color_gold", "mystcraft:color_water",
                        "mystcraft:color_amber", "mystcraft:color_cloud",
                        "mystcraft:gradient_sunset",
                        "mystcraft:env_longer_days"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_savanna",
                                "mystcraft:biome_meadow", "mystcraft:biome_flower_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_forest",
                                "mystcraft:biome_desert", "mystcraft:biome_badlands"),
                        pool(4, "mystcraft:villages", "mystcraft:desert_temples",
                                "mystcraft:trail_ruins", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:ruined_portals",
                                "mystcraft:buried_treasure", "mystcraft:woodland_mansions"),
                        pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                                "mystcraft:star_fissure_feature", "mystcraft:huge_trees"),
                        pool(2, "mystcraft:extra_copper_ore", "mystcraft:extra_iron_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_redstone_ore")
                )
        ));

        register(new Preset("obsidian_fortress", "Obsidian Citadel",
                list("mystcraft:terrain_amplified", "mystcraft:biome_medium",
                        "mystcraft:sun_dark", "mystcraft:moon_dark", "mystcraft:stars_end",
                        "mystcraft:lighting_dark", "mystcraft:weather_thunder",
                        "mystcraft:block_minecraft_obsidian",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:deep_dark", "mystcraft:perlin_worms",
                        "mystcraft:color_black", "mystcraft:color_sky",
                        "mystcraft:color_dark_gray", "mystcraft:color_fog",
                        "mystcraft:color_navy", "mystcraft:color_grass",
                        "mystcraft:color_dark_gray", "mystcraft:color_foliage",
                        "mystcraft:cloud_low", "mystcraft:horizon_low",
                        "mystcraft:env_lightning",
                        "mystcraft:env_longer_days"),
                list(
                        pool(5, "mystcraft:biome_dark_forest", "mystcraft:biome_deep_dark",
                                "mystcraft:biome_soul_sand_valley", "mystcraft:biome_basalt_deltas",
                                "mystcraft:biome_taiga", "mystcraft:biome_old_growth_spruce_taiga",
                                "mystcraft:biome_swamp"),
                        pool(4, "mystcraft:nether_fortress", "mystcraft:bastion_remnants",
                                "mystcraft:ancient_cities", "mystcraft:strongholds",
                                "mystcraft:dungeons", "mystcraft:woodland_mansions"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_redstone_ore", "mystcraft:extra_lapis_ore",
                                "mystcraft:dense_ores"),
                        pool(2, "mystcraft:spikes", "mystcraft:obelisks",
                                "mystcraft:vertical_tendrils", "mystcraft:crystal_formation",
                                "mystcraft:spheres"),
                        pool(1, "mystcraft:env_meteors", "mystcraft:env_explosions",
                                "mystcraft:env_scorched")
                )
        ));

        register(new Preset("enchanted_grove", "Enchanted Grove",
                list("mystcraft:terrain_normal", "mystcraft:biome_small",
                        "mystcraft:sun_slow", "mystcraft:moon_large", "mystcraft:stars_dense",
                        "mystcraft:lighting_bright", "mystcraft:weather_cloudy",
                        "mystcraft:caves", "mystcraft:lush_caves",
                        "mystcraft:huge_trees", "mystcraft:surface_lakes",
                        "mystcraft:color_sky_natural",
                        "mystcraft:color_emerald", "mystcraft:color_grass",
                        "mystcraft:color_jade", "mystcraft:color_foliage",
                        "mystcraft:color_turquoise", "mystcraft:color_water",
                        "mystcraft:color_ivory", "mystcraft:color_fog",
                        "mystcraft:color_silver", "mystcraft:color_cloud",
                        "mystcraft:rainbow", "mystcraft:crystal_formation",
                        "mystcraft:gradient_dawn",
                        "mystcraft:env_slow_time"),
                list(
                        pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_meadow",
                                "mystcraft:biome_cherry_grove", "mystcraft:biome_mushroom_fields",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_grove"),
                        pool(3, "mystcraft:villages", "mystcraft:trail_ruins",
                                "mystcraft:witch_huts", "mystcraft:jungle_temples",
                                "mystcraft:ruined_portals", "mystcraft:dungeons"),
                        pool(2, "mystcraft:obelisks", "mystcraft:star_fissure_feature",
                                "mystcraft:spheres", "mystcraft:spikes"),
                        pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_diamond_ore",
                                "mystcraft:extra_lapis_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:dense_ores")
                )
        ));

        register(new Preset("barren_ore", "Depleted World",
                list("mystcraft:terrain_normal", "mystcraft:biome_large",
                        "mystcraft:sun_normal", "mystcraft:moon_normal", "mystcraft:stars_normal",
                        "mystcraft:lighting_normal", "mystcraft:weather_normal",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:no_diamond_ore", "mystcraft:no_gold_ore",
                        "mystcraft:no_emerald_ore", "mystcraft:no_lapis_ore",
                        "mystcraft:no_redstone_ore",
                        "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
                        "mystcraft:color_grass_natural"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest",
                                "mystcraft:biome_birch_forest", "mystcraft:biome_taiga",
                                "mystcraft:biome_meadow", "mystcraft:biome_savanna",
                                "mystcraft:biome_desert", "mystcraft:biome_dark_forest"),
                        pool(4, "mystcraft:villages", "mystcraft:dungeons",
                                "mystcraft:mineshafts", "mystcraft:strongholds",
                                "mystcraft:trail_ruins", "mystcraft:pillager_outposts",
                                "mystcraft:ruined_portals"),
                        pool(2, "mystcraft:perlin_worms", "mystcraft:dripstone_caves",
                                "mystcraft:lush_caves", "mystcraft:deep_dark",
                                "mystcraft:surface_lakes"),
                        pool(1, "mystcraft:star_fissure_feature", "mystcraft:obelisks",
                                "mystcraft:crystal_formation")
                )
        ));

        register(new Preset("double_time", "Temporal Rift",
                list("mystcraft:terrain_blend", "mystcraft:biome_small",
                        "mystcraft:sun_fast", "mystcraft:moon_fast", "mystcraft:stars_twinkle",
                        "mystcraft:lighting_normal", "mystcraft:weather_fast",
                        "mystcraft:caves", "mystcraft:ravines",
                        "mystcraft:env_shorter_days", "mystcraft:env_accelerated",
                        "mystcraft:color_amber", "mystcraft:color_sky",
                        "mystcraft:color_gold", "mystcraft:color_fog",
                        "mystcraft:color_bronze", "mystcraft:color_grass",
                        "mystcraft:gradient_aurora",
                        "mystcraft:floating_islands"),
                list(
                        pool(6, "mystcraft:biome_plains", "mystcraft:biome_desert",
                                "mystcraft:biome_forest", "mystcraft:biome_jungle",
                                "mystcraft:biome_badlands", "mystcraft:biome_mushroom_fields",
                                "mystcraft:biome_dark_forest", "mystcraft:biome_taiga"),
                        pool(3, "mystcraft:tendrils", "mystcraft:spheres",
                                "mystcraft:spikes", "mystcraft:vertical_tendrils",
                                "mystcraft:perlin_worms", "mystcraft:crystal_formation"),
                        pool(3, "mystcraft:dungeons", "mystcraft:strongholds",
                                "mystcraft:trail_ruins", "mystcraft:ancient_cities",
                                "mystcraft:ruined_portals"),
                        pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                                "mystcraft:extra_emerald_ore", "mystcraft:dense_ores")
                )
        ));

        PRESET_NAMES = List.copyOf(PRESETS.keySet());
    }

    private static void register(Preset preset) {
        PRESETS.put(preset.name, preset);
    }

    public static Preset getPreset(String name) {
        return PRESETS.get(name);
    }

    private static List<String> list(String... items) {
        return Arrays.asList(items);
    }

    private static List<RandomPool> list(RandomPool... pools) {
        return Arrays.asList(pools);
    }

    static RandomPool pool(int pickCount, String... options) {
        return new RandomPool(pickCount, Arrays.asList(options));
    }

    /**
     * A pool of symbols to randomly pick from.
     */
    public static class RandomPool {
        public final int pickCount;
        public final List<String> options;

        public RandomPool(int pickCount, List<String> options) {
            this.pickCount = pickCount;
            this.options = options;
        }
    }

    /**
     * A curated preset definition.
     */
    public static class Preset {
        public final String name;
        public final String displayName;
        public final List<String> fixedSymbols;
        public final List<RandomPool> randomPools;

        public Preset(String name, String displayName, List<String> fixedSymbols, List<RandomPool> randomPools) {
            this.name = name;
            this.displayName = displayName;
            this.fixedSymbols = fixedSymbols;
            this.randomPools = randomPools;
        }
    }
}
