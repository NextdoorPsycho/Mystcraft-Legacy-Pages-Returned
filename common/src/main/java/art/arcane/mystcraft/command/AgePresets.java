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
 * <p>
 * Biome symbol IDs follow the pattern: mystcraft:biome_<biome_path>
 */
public final class AgePresets {

  public static final List<String> PRESET_NAMES;
  private static final Map<String, Preset> PRESETS = new HashMap<>();

  static {

    // ===================================================================
    // --- Overworld Variants ---
    // ===================================================================

    // 1. Classic Age - Vanilla-like, safe starting age
    register(new Preset("classic", "Classic Age",
        list("mystcraft:terrain_normal", "mystcraft:biome_native", "mystcraft:lighting_normal",
            "mystcraft:weather_normal", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:surface_lakes", "mystcraft:dripstone_caves", "mystcraft:color_sky_natural",
            "mystcraft:color_fog_natural", "mystcraft:color_grass_natural",
            "mystcraft:color_water_natural", "mystcraft:color_foliage_natural"),
        list(
            pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest", "mystcraft:biome_birch_forest",
                "mystcraft:biome_dark_forest", "mystcraft:biome_taiga", "mystcraft:biome_meadow",
                "mystcraft:biome_flower_forest", "mystcraft:biome_savanna", "mystcraft:biome_swamp",
                "mystcraft:biome_cherry_grove"),
            pool(4, "mystcraft:villages", "mystcraft:dungeons", "mystcraft:mineshafts",
                "mystcraft:strongholds", "mystcraft:trail_ruins", "mystcraft:shipwrecks"),
            pool(2, "mystcraft:lush_caves", "mystcraft:deep_lakes", "mystcraft:perlin_worms",
                "mystcraft:floating_islands")
        )
    ));

    // 2. Lush Paradise - Bright, rainbow, many biomes
    register(new Preset("paradise", "Lush Paradise",
        list("mystcraft:terrain_normal", "mystcraft:biome_medium",
            "mystcraft:lighting_bright", "mystcraft:weather_normal",
            "mystcraft:caves", "mystcraft:lush_caves", "mystcraft:huge_trees",
            "mystcraft:surface_lakes", "mystcraft:deep_lakes", "mystcraft:color_sky_natural",
            "mystcraft:color_grass_natural", "mystcraft:color_foliage_natural",
            "mystcraft:color_water_natural", "mystcraft:rainbow", "mystcraft:cloud_high",
            "mystcraft:env_longer_days"),
        list(
            pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_jungle", "mystcraft:biome_meadow",
                "mystcraft:biome_birch_forest", "mystcraft:biome_cherry_grove",
                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_bamboo_jungle",
                "mystcraft:biome_forest"),
            pool(3, "mystcraft:villages", "mystcraft:trail_ruins", "mystcraft:dungeons",
                "mystcraft:ruined_portals", "mystcraft:jungle_temples"),
            pool(2, "mystcraft:extra_iron_ore", "mystcraft:extra_copper_ore",
                "mystcraft:extra_emerald_ore", "mystcraft:dense_ores"),
            pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:star_fissure_feature")
        )
    ));

    // 3. Towering Peaks - Mountain peaks, snow, aurora, amplified
    register(new Preset("alpine", "Towering Peaks",
        list("mystcraft:terrain_amplified", "mystcraft:biome_large",
            "mystcraft:lighting_normal",
            "mystcraft:weather_normal", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:deep_lakes", "mystcraft:dripstone_caves", "mystcraft:cloud_high",
            "mystcraft:color_sky_natural", "mystcraft:color_fog_natural", "mystcraft:perlin_worms"),
        list(
            pool(7, "mystcraft:biome_meadow", "mystcraft:biome_stony_peaks",
                "mystcraft:biome_jagged_peaks", "mystcraft:biome_frozen_peaks", "mystcraft:biome_grove",
                "mystcraft:biome_snowy_slopes", "mystcraft:biome_windswept_hills",
                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_cherry_grove"),
            pool(3, "mystcraft:villages", "mystcraft:trail_ruins", "mystcraft:mineshafts",
                "mystcraft:strongholds", "mystcraft:dungeons", "mystcraft:igloos"),
            pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_iron_ore",
                "mystcraft:extra_copper_ore", "mystcraft:extra_diamond_ore"),
            pool(2, "mystcraft:spikes", "mystcraft:vertical_tendrils", "mystcraft:floating_islands",
                "mystcraft:huge_trees")
        )
    ));

    // 4. Drowned World - Ocean-heavy with islands
    register(new Preset("archipelago", "Drowned World",
        list("mystcraft:terrain_normal", "mystcraft:biome_large",
            "mystcraft:lighting_normal",
            "mystcraft:weather_rain", "mystcraft:caves", "mystcraft:deep_lakes",
            "mystcraft:surface_lakes", "mystcraft:color_sky_natural", "mystcraft:color_turquoise",
            "mystcraft:color_water", "mystcraft:color_sapphire", "mystcraft:color_fog"),
        list(
            pool(6, "mystcraft:biome_ocean", "mystcraft:biome_deep_ocean", "mystcraft:biome_warm_ocean",
                "mystcraft:biome_lukewarm_ocean", "mystcraft:biome_cold_ocean", "mystcraft:biome_beach",
                "mystcraft:biome_mushroom_fields", "mystcraft:biome_stony_shore"),
            pool(4, "mystcraft:ocean_monuments", "mystcraft:shipwrecks", "mystcraft:ocean_ruins",
                "mystcraft:buried_treasure", "mystcraft:dungeons", "mystcraft:ruined_portals"),
            pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                "mystcraft:extra_lapis_ore", "mystcraft:dense_ores"),
            pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks", "mystcraft:spheres",
                "mystcraft:perlin_worms")
        )
    ));

    // 5. Scorched Sands - Hot, dry, desert/badlands mix
    register(new Preset("savanna", "Scorched Sands",
        list("mystcraft:terrain_normal", "mystcraft:biome_large",
            "mystcraft:lighting_bright",
            "mystcraft:weather_off", "mystcraft:caves", "mystcraft:ravines", "mystcraft:color_amber",
            "mystcraft:color_sky", "mystcraft:color_gold", "mystcraft:color_fog",
            "mystcraft:env_scorched", "mystcraft:cloud_none", "mystcraft:env_shorter_days"),
        list(
            pool(5, "mystcraft:biome_desert", "mystcraft:biome_badlands",
                "mystcraft:biome_eroded_badlands", "mystcraft:biome_savanna",
                "mystcraft:biome_windswept_savanna", "mystcraft:biome_wooded_badlands",
                "mystcraft:biome_stony_shore"),
            pool(3, "mystcraft:villages", "mystcraft:desert_temples", "mystcraft:dungeons",
                "mystcraft:mineshafts", "mystcraft:pillager_outposts", "mystcraft:trail_ruins"),
            pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_emerald_ore",
                "mystcraft:extra_diamond_ore", "mystcraft:extra_copper_ore"),
            pool(2, "mystcraft:spikes", "mystcraft:obelisks", "mystcraft:tendrils",
                "mystcraft:spheres", "mystcraft:dripstone_caves")
        )
    ));

    // 6. Ancient Woodland - Dense forests, dark forest, mushroom fields
    register(new Preset("woodland", "Ancient Woodland",
        list("mystcraft:terrain_normal", "mystcraft:biome_huge",
            "mystcraft:lighting_normal",
            "mystcraft:weather_rain", "mystcraft:caves", "mystcraft:lush_caves",
            "mystcraft:huge_trees", "mystcraft:surface_lakes", "mystcraft:color_sky_natural",
            "mystcraft:color_fog_natural", "mystcraft:color_jade", "mystcraft:color_grass",
            "mystcraft:color_emerald", "mystcraft:color_foliage", "mystcraft:gradient_dawn",
            "mystcraft:env_longer_days"),
        list(
            pool(5, "mystcraft:biome_dark_forest", "mystcraft:biome_old_growth_spruce_taiga",
                "mystcraft:biome_forest", "mystcraft:biome_birch_forest",
                "mystcraft:biome_mushroom_fields", "mystcraft:biome_mangrove_swamp",
                "mystcraft:biome_jungle", "mystcraft:biome_taiga"),
            pool(3, "mystcraft:villages", "mystcraft:witch_huts", "mystcraft:trail_ruins",
                "mystcraft:woodland_mansions", "mystcraft:jungle_temples", "mystcraft:dungeons"),
            pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_iron_ore",
                "mystcraft:extra_copper_ore", "mystcraft:dense_ores"),
            pool(1, "mystcraft:spheres", "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:perlin_worms")
        )
    ));

    // ===================================================================
    // --- Exotic Variants ---
    // ===================================================================

    // 7. Sky Archipelago - Floating islands, bright sky, no sea
    register(new Preset("skylands", "Sky Archipelago",
        list("mystcraft:terrain_skylands", "mystcraft:biome_small",
            "mystcraft:lighting_bright", "mystcraft:weather_off",
            "mystcraft:floating_islands", "mystcraft:cloud_high", "mystcraft:color_light_blue",
            "mystcraft:color_sky", "mystcraft:color_ivory", "mystcraft:color_fog",
            "mystcraft:color_silver", "mystcraft:color_cloud", "mystcraft:rainbow",
            "mystcraft:env_longer_days"),
        list(
            pool(6, "mystcraft:biome_plains", "mystcraft:biome_flower_forest", "mystcraft:biome_meadow",
                "mystcraft:biome_birch_forest", "mystcraft:biome_forest", "mystcraft:biome_cherry_grove",
                "mystcraft:biome_mushroom_fields"),
            pool(2, "mystcraft:dense_ores", "mystcraft:huge_trees", "mystcraft:extra_diamond_ore",
                "mystcraft:extra_emerald_ore"),
            pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:star_fissure_feature", "mystcraft:spheres", "mystcraft:spikes"),
            pool(1, "mystcraft:gradient_aurora", "mystcraft:gradient_dawn", "mystcraft:gradient_sunset")
        )
    ));

    // 8. Vast Caverns - Underground cave terrain, dark, sculk
    register(new Preset("cavern", "Vast Caverns",
        list("mystcraft:terrain_cave", "mystcraft:biome_medium",
            "mystcraft:lighting_dark", "mystcraft:weather_off", "mystcraft:caves",
            "mystcraft:dripstone_caves", "mystcraft:lush_caves", "mystcraft:deep_dark",
            "mystcraft:dense_ores", "mystcraft:perlin_worms", "mystcraft:color_dark_gray",
            "mystcraft:color_fog", "mystcraft:cloud_none", "mystcraft:env_static_time"),
        list(
            pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_taiga",
                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_swamp",
                "mystcraft:biome_mushroom_fields", "mystcraft:biome_deep_dark",
                "mystcraft:biome_mangrove_swamp"),
            pool(3, "mystcraft:dungeons", "mystcraft:mineshafts", "mystcraft:ancient_cities",
                "mystcraft:strongholds", "mystcraft:trail_ruins"),
            pool(3, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                "mystcraft:extra_lapis_ore", "mystcraft:extra_redstone_ore",
                "mystcraft:extra_iron_ore"),
            pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils", "mystcraft:spheres",
                "mystcraft:spikes", "mystcraft:crystal_formation")
        )
    ));

    // 9. Ethereal Realm - Lavender/jade fantasy, crystal formations
    register(new Preset("ethereal", "Ethereal Realm",
        list("mystcraft:terrain_amplified", "mystcraft:biome_small",
            "mystcraft:lighting_bright",
            "mystcraft:weather_off", "mystcraft:floating_islands", "mystcraft:cloud_high",
            "mystcraft:color_lavender", "mystcraft:color_sky", "mystcraft:color_ivory",
            "mystcraft:color_fog", "mystcraft:color_violet", "mystcraft:color_cloud",
            "mystcraft:color_jade", "mystcraft:color_grass", "mystcraft:color_emerald",
            "mystcraft:color_foliage", "mystcraft:color_turquoise", "mystcraft:color_water",
            "mystcraft:rainbow", "mystcraft:crystal_formation", "mystcraft:gradient_aurora",
            "mystcraft:env_slow_time"),
        list(
            pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_meadow",
                "mystcraft:biome_cherry_grove", "mystcraft:biome_birch_forest",
                "mystcraft:biome_mushroom_fields", "mystcraft:biome_plains", "mystcraft:biome_forest",
                "mystcraft:biome_grove"),
            pool(2, "mystcraft:villages", "mystcraft:trail_ruins", "mystcraft:ruined_portals",
                "mystcraft:dungeons"),
            pool(2, "mystcraft:obelisks", "mystcraft:star_fissure_feature", "mystcraft:spheres",
                "mystcraft:huge_trees"),
            pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                "mystcraft:extra_lapis_ore", "mystcraft:dense_ores")
        )
    ));

    // 10. Eternal Twilight - Perpetual dusk, slow time, eerie
    register(new Preset("twilight", "Eternal Twilight",
        list("mystcraft:terrain_normal", "mystcraft:biome_medium",
            "mystcraft:lighting_normal",
            "mystcraft:weather_cloudy", "mystcraft:caves", "mystcraft:lush_caves",
            "mystcraft:surface_lakes", "mystcraft:color_indigo", "mystcraft:color_sky",
            "mystcraft:color_lavender", "mystcraft:color_fog", "mystcraft:color_olive",
            "mystcraft:color_grass", "mystcraft:color_teal", "mystcraft:color_foliage",
            "mystcraft:color_sapphire", "mystcraft:color_water", "mystcraft:gradient_dusk",
            "mystcraft:env_slow_time"),
        list(
            pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_forest", "mystcraft:biome_taiga",
                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_meadow",
                "mystcraft:biome_swamp", "mystcraft:biome_cherry_grove"),
            pool(3, "mystcraft:villages", "mystcraft:trail_ruins", "mystcraft:witch_huts",
                "mystcraft:dungeons", "mystcraft:woodland_mansions"),
            pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks", "mystcraft:tendrils",
                "mystcraft:vertical_tendrils", "mystcraft:perlin_worms"),
            pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_emerald_ore",
                "mystcraft:extra_gold_ore", "mystcraft:dense_ores")
        )
    ));

    // 11. Fungal Kingdom - Mushroom biomes, huge trees, teal/lime
    register(new Preset("fungal", "Fungal Kingdom",
        list("mystcraft:terrain_normal", "mystcraft:biome_huge",
            "mystcraft:lighting_normal",
            "mystcraft:weather_rain", "mystcraft:caves", "mystcraft:lush_caves",
            "mystcraft:huge_trees", "mystcraft:surface_lakes", "mystcraft:deep_lakes",
            "mystcraft:color_sky_natural", "mystcraft:color_fog_natural", "mystcraft:color_lime",
            "mystcraft:color_grass", "mystcraft:color_teal", "mystcraft:color_foliage",
            "mystcraft:gradient_dawn", "mystcraft:env_longer_days"),
        list(
            pool(5, "mystcraft:biome_mushroom_fields", "mystcraft:biome_dark_forest",
                "mystcraft:biome_swamp", "mystcraft:biome_mangrove_swamp", "mystcraft:biome_jungle",
                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_forest"),
            pool(3, "mystcraft:villages", "mystcraft:witch_huts", "mystcraft:jungle_temples",
                "mystcraft:woodland_mansions", "mystcraft:trail_ruins", "mystcraft:dungeons"),
            pool(2, "mystcraft:extra_emerald_ore", "mystcraft:extra_iron_ore",
                "mystcraft:extra_copper_ore", "mystcraft:dense_ores"),
            pool(1, "mystcraft:spheres", "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:perlin_worms")
        )
    ));

    // 12. Ancient Civilization - Ancient cities, dark, bronze/olive theme
    register(new Preset("ancient", "Ancient Civilization",
        list("mystcraft:terrain_normal", "mystcraft:biome_large",
            "mystcraft:lighting_dark",
            "mystcraft:weather_cloudy", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:deep_dark", "mystcraft:dripstone_caves", "mystcraft:perlin_worms",
            "mystcraft:color_bronze", "mystcraft:color_sky", "mystcraft:color_olive",
            "mystcraft:color_fog", "mystcraft:color_maroon", "mystcraft:color_grass",
            "mystcraft:color_brown", "mystcraft:color_foliage", "mystcraft:gradient_dusk",
            "mystcraft:env_longer_days"),
        list(
            pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_old_growth_spruce_taiga",
                "mystcraft:biome_taiga", "mystcraft:biome_plains", "mystcraft:biome_swamp",
                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_deep_dark",
                "mystcraft:biome_meadow"),
            pool(4, "mystcraft:ancient_cities", "mystcraft:trail_ruins", "mystcraft:strongholds",
                "mystcraft:dungeons", "mystcraft:woodland_mansions", "mystcraft:mineshafts"),
            pool(2, "mystcraft:obelisks", "mystcraft:crystal_formation", "mystcraft:tendrils",
                "mystcraft:vertical_tendrils"),
            pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_emerald_ore",
                "mystcraft:extra_lapis_ore", "mystcraft:dense_ores")
        )
    ));

    // ===================================================================
    // --- Hostile Variants ---
    // ===================================================================

    // 13. Infernal Wastes - Nether terrain, lava sea, crimson
    register(new Preset("infernal", "Infernal Wastes",
        list("mystcraft:terrain_nether", "mystcraft:biome_medium",
            "mystcraft:lighting_nether",
            "mystcraft:weather_off", "mystcraft:sea_lava", "mystcraft:dense_ores",
            "mystcraft:color_crimson", "mystcraft:color_sky", "mystcraft:color_maroon",
            "mystcraft:color_fog", "mystcraft:env_scorched", "mystcraft:gradient_blood_sky",
            "mystcraft:cloud_none", "mystcraft:env_shorter_days"),
        list(
            pool(4, "mystcraft:biome_nether_wastes", "mystcraft:biome_soul_sand_valley",
                "mystcraft:biome_crimson_forest", "mystcraft:biome_warped_forest",
                "mystcraft:biome_basalt_deltas"),
            pool(3, "mystcraft:nether_fortress", "mystcraft:bastion_remnants", "mystcraft:dungeons",
                "mystcraft:ruined_portals", "mystcraft:ancient_cities"),
            pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                "mystcraft:extra_redstone_ore"),
            pool(2, "mystcraft:tendrils", "mystcraft:spikes", "mystcraft:spheres",
                "mystcraft:vertical_tendrils", "mystcraft:obelisks"),
            pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning", "mystcraft:env_explosions")
        )
    ));

    // 14. Void Rift - End terrain with void, floating islands
    register(new Preset("void_rift", "Void Rift",
        list("mystcraft:terrain_end", "mystcraft:biome_medium",
            "mystcraft:lighting_dark", "mystcraft:weather_off", "mystcraft:color_indigo",
            "mystcraft:color_sky", "mystcraft:color_violet", "mystcraft:color_fog",
            "mystcraft:hide_horizon", "mystcraft:cloud_none", "mystcraft:env_static_time"),
        list(
            pool(3, "mystcraft:biome_the_end", "mystcraft:biome_end_midlands",
                "mystcraft:biome_end_highlands", "mystcraft:biome_end_barrens",
                "mystcraft:biome_small_end_islands"),
            pool(2, "mystcraft:end_cities", "mystcraft:ancient_cities", "mystcraft:dungeons",
                "mystcraft:strongholds"),
            pool(3, "mystcraft:crystal_formation", "mystcraft:obelisks", "mystcraft:spikes",
                "mystcraft:spheres", "mystcraft:floating_islands", "mystcraft:star_fissure_feature"),
            pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning", "mystcraft:env_accelerated")
        )
    ));

    // 15. Blood Moon Rising - Crimson everything, lightning, creepy tendrils
    register(new Preset("blood_moon", "Blood Moon Rising",
        list("mystcraft:terrain_normal", "mystcraft:biome_large",
            "mystcraft:lighting_dark",
            "mystcraft:weather_storm", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:env_lightning", "mystcraft:cloud_low", "mystcraft:color_crimson",
            "mystcraft:color_sky", "mystcraft:color_maroon", "mystcraft:color_fog",
            "mystcraft:color_ruby", "mystcraft:color_grass", "mystcraft:color_crimson",
            "mystcraft:color_foliage", "mystcraft:color_ruby", "mystcraft:color_water",
            "mystcraft:gradient_blood_sky", "mystcraft:horizon_low", "mystcraft:env_shorter_days"),
        list(
            pool(6, "mystcraft:biome_dark_forest", "mystcraft:biome_swamp",
                "mystcraft:biome_mangrove_swamp", "mystcraft:biome_taiga",
                "mystcraft:biome_old_growth_spruce_taiga", "mystcraft:biome_deep_dark",
                "mystcraft:biome_plains"),
            pool(3, "mystcraft:woodland_mansions", "mystcraft:ancient_cities", "mystcraft:witch_huts",
                "mystcraft:dungeons", "mystcraft:strongholds"),
            pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils", "mystcraft:obelisks",
                "mystcraft:spikes", "mystcraft:crystal_formation"),
            pool(1, "mystcraft:env_meteors", "mystcraft:env_scorched", "mystcraft:env_explosions")
        )
    ));

    // 16. Blighted Wasteland - Wasteland, scorched, meteors, no ores
    register(new Preset("desolation", "Blighted Wasteland",
        list("mystcraft:terrain_flat", "mystcraft:biome_single",
            "mystcraft:lighting_dark",
            "mystcraft:weather_storm", "mystcraft:no_ores", "mystcraft:biome_badlands",
            "mystcraft:env_scorched", "mystcraft:env_lightning", "mystcraft:color_maroon",
            "mystcraft:color_sky", "mystcraft:color_brown", "mystcraft:color_fog",
            "mystcraft:color_olive", "mystcraft:color_grass", "mystcraft:color_maroon",
            "mystcraft:color_foliage", "mystcraft:hide_horizon", "mystcraft:cloud_low",
            "mystcraft:gradient_blood_sky", "mystcraft:env_shorter_days"),
        list(
            pool(2, "mystcraft:dungeons", "mystcraft:ruined_portals", "mystcraft:ancient_cities",
                "mystcraft:strongholds"),
            pool(2, "mystcraft:obelisks", "mystcraft:spikes", "mystcraft:tendrils",
                "mystcraft:crystal_formation", "mystcraft:spheres"),
            pool(1, "mystcraft:env_meteors", "mystcraft:env_explosions", "mystcraft:env_accelerated")
        )
    ));

    // 17. Shattered Realm - Mixed biomes from all dimensions, unstable
    register(new Preset("chaos", "Shattered Realm",
        list("mystcraft:terrain_checkerboard", "mystcraft:biome_grid",
            "mystcraft:lighting_normal",
            "mystcraft:weather_fast", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:dense_ores", "mystcraft:floating_islands", "mystcraft:perlin_worms",
            "mystcraft:color_coral", "mystcraft:color_sky", "mystcraft:color_amber",
            "mystcraft:color_fog", "mystcraft:color_jade", "mystcraft:color_grass",
            "mystcraft:color_violet", "mystcraft:color_foliage", "mystcraft:color_turquoise",
            "mystcraft:color_water", "mystcraft:gradient_aurora", "mystcraft:env_shorter_days"),
        list(
            pool(6, "mystcraft:biome_nether_wastes", "mystcraft:biome_plains", "mystcraft:biome_the_end",
                "mystcraft:biome_desert", "mystcraft:biome_mushroom_fields",
                "mystcraft:biome_ice_spikes", "mystcraft:biome_jungle", "mystcraft:biome_deep_dark",
                "mystcraft:biome_dark_forest", "mystcraft:biome_badlands"),
            pool(3, "mystcraft:tendrils", "mystcraft:vertical_tendrils", "mystcraft:spheres",
                "mystcraft:spikes", "mystcraft:obelisks", "mystcraft:crystal_formation"),
            pool(4, "mystcraft:nether_fortress", "mystcraft:end_cities", "mystcraft:ancient_cities",
                "mystcraft:woodland_mansions", "mystcraft:strongholds", "mystcraft:dungeons",
                "mystcraft:ocean_monuments"),
            pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning", "mystcraft:env_scorched",
                "mystcraft:env_explosions")
        )
    ));

    // ===================================================================
    // --- Themed Variants ---
    // ===================================================================

    // 18. Miner's Paradise - Dense ores, all extra ores, caves everywhere
    register(new Preset("miner", "Miner's Paradise",
        list("mystcraft:terrain_normal", "mystcraft:biome_native",
            "mystcraft:lighting_normal",
            "mystcraft:weather_normal", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:dripstone_caves", "mystcraft:lush_caves", "mystcraft:dense_ores",
            "mystcraft:perlin_worms", "mystcraft:extra_diamond_ore", "mystcraft:extra_iron_ore",
            "mystcraft:extra_gold_ore", "mystcraft:extra_emerald_ore", "mystcraft:color_sky_natural",
            "mystcraft:color_fog_natural", "mystcraft:env_longer_days"),
        list(
            pool(3, "mystcraft:mineshafts", "mystcraft:strongholds", "mystcraft:dungeons",
                "mystcraft:ancient_cities", "mystcraft:trail_ruins"),
            pool(2, "mystcraft:extra_copper_ore", "mystcraft:extra_redstone_ore",
                "mystcraft:extra_lapis_ore", "mystcraft:extra_coal_ore"),
            pool(2, "mystcraft:deep_dark", "mystcraft:deep_lakes", "mystcraft:tendrils",
                "mystcraft:vertical_tendrils"),
            pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:star_fissure_feature")
        )
    ));

    // 19. Hoard of Ages - Many structures, huge biomes, long days
    register(new Preset("explorer", "Hoard of Ages",
        list("mystcraft:terrain_normal", "mystcraft:biome_medium",
            "mystcraft:lighting_normal",
            "mystcraft:weather_normal", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:dense_ores", "mystcraft:perlin_worms", "mystcraft:extra_diamond_ore",
            "mystcraft:extra_gold_ore", "mystcraft:extra_emerald_ore", "mystcraft:color_sky_natural",
            "mystcraft:color_fog_natural", "mystcraft:color_gold", "mystcraft:color_water"),
        list(
            pool(6, "mystcraft:biome_plains", "mystcraft:biome_forest", "mystcraft:biome_birch_forest",
                "mystcraft:biome_taiga", "mystcraft:biome_meadow", "mystcraft:biome_savanna",
                "mystcraft:biome_jungle", "mystcraft:biome_desert"),
            pool(5, "mystcraft:villages", "mystcraft:dungeons", "mystcraft:mineshafts",
                "mystcraft:strongholds", "mystcraft:trail_ruins", "mystcraft:desert_temples",
                "mystcraft:jungle_temples", "mystcraft:buried_treasure", "mystcraft:shipwrecks",
                "mystcraft:woodland_mansions"),
            pool(2, "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:star_fissure_feature", "mystcraft:lush_caves", "mystcraft:dripstone_caves")
        )
    ));

    // 20. Glacial Tomb - Ice age, blizzard, packed ice
    register(new Preset("frozen", "Glacial Tomb",
        list("mystcraft:terrain_normal", "mystcraft:biome_large",
            "mystcraft:lighting_bright", "mystcraft:weather_blizzard",
            "mystcraft:block_minecraft_packed_ice", "mystcraft:sea_blue_ice", "mystcraft:caves",
            "mystcraft:cloud_low", "mystcraft:color_light_blue", "mystcraft:color_sky",
            "mystcraft:color_silver", "mystcraft:color_fog", "mystcraft:color_sapphire",
            "mystcraft:color_water", "mystcraft:env_longer_days"),
        list(
            pool(6, "mystcraft:biome_frozen_peaks", "mystcraft:biome_ice_spikes",
                "mystcraft:biome_snowy_plains", "mystcraft:biome_frozen_river",
                "mystcraft:biome_snowy_taiga", "mystcraft:biome_grove", "mystcraft:biome_snowy_slopes",
                "mystcraft:biome_snowy_beach"),
            pool(3, "mystcraft:spikes", "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:spheres", "mystcraft:dripstone_caves"),
            pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_iron_ore",
                "mystcraft:extra_emerald_ore", "mystcraft:dense_ores"),
            pool(3, "mystcraft:villages", "mystcraft:strongholds", "mystcraft:dungeons",
                "mystcraft:igloos", "mystcraft:mineshafts"),
            pool(1, "mystcraft:gradient_aurora", "mystcraft:gradient_dawn", "mystcraft:gradient_dusk")
        )
    ));

    // 21. Sanctuary - Anti-PVP, star fissure, rainbow, lush
    register(new Preset("peaceful", "Sanctuary",
        list("mystcraft:terrain_normal", "mystcraft:biome_medium",
            "mystcraft:lighting_bright",
            "mystcraft:weather_normal", "mystcraft:caves", "mystcraft:lush_caves",
            "mystcraft:surface_lakes", "mystcraft:deep_lakes", "mystcraft:huge_trees",
            "mystcraft:anti_pvp", "mystcraft:star_fissure", "mystcraft:rainbow",
            "mystcraft:color_sky_natural", "mystcraft:color_fog_natural",
            "mystcraft:color_grass_natural", "mystcraft:color_foliage_natural",
            "mystcraft:color_water_natural", "mystcraft:gradient_dawn", "mystcraft:env_longer_days"),
        list(
            pool(6, "mystcraft:biome_flower_forest", "mystcraft:biome_meadow", "mystcraft:biome_plains",
                "mystcraft:biome_birch_forest", "mystcraft:biome_cherry_grove",
                "mystcraft:biome_forest", "mystcraft:biome_grove", "mystcraft:biome_bamboo_jungle"),
            pool(3, "mystcraft:villages", "mystcraft:trail_ruins", "mystcraft:ruined_portals",
                "mystcraft:dungeons"),
            pool(2, "mystcraft:extra_iron_ore", "mystcraft:extra_copper_ore",
                "mystcraft:extra_coal_ore", "mystcraft:dense_ores"),
            pool(1, "mystcraft:crystal_formation", "mystcraft:obelisks",
                "mystcraft:star_fissure_feature")
        )
    ));

    // 22. Temporal Rift - Fast time, accelerated, short days, blend terrain
    register(new Preset("temporal", "Temporal Rift",
        list("mystcraft:terrain_blend", "mystcraft:biome_small",
            "mystcraft:lighting_normal",
            "mystcraft:weather_fast", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:env_shorter_days", "mystcraft:env_accelerated", "mystcraft:color_amber",
            "mystcraft:color_sky", "mystcraft:color_gold", "mystcraft:color_fog",
            "mystcraft:gradient_aurora", "mystcraft:floating_islands"),
        list(
            pool(6, "mystcraft:biome_plains", "mystcraft:biome_desert", "mystcraft:biome_forest",
                "mystcraft:biome_jungle", "mystcraft:biome_badlands",
                "mystcraft:biome_mushroom_fields", "mystcraft:biome_dark_forest",
                "mystcraft:biome_taiga"),
            pool(3, "mystcraft:tendrils", "mystcraft:spheres", "mystcraft:spikes",
                "mystcraft:vertical_tendrils", "mystcraft:perlin_worms",
                "mystcraft:crystal_formation"),
            pool(3, "mystcraft:dungeons", "mystcraft:strongholds", "mystcraft:trail_ruins",
                "mystcraft:ancient_cities", "mystcraft:ruined_portals"),
            pool(2, "mystcraft:extra_diamond_ore", "mystcraft:extra_gold_ore",
                "mystcraft:extra_emerald_ore", "mystcraft:dense_ores")
        )
    ));

    // ===================================================================
    // --- Extreme Variants ---
    // ===================================================================

    // 23. Horrible Instability - All negative effects, horrible instability
    register(new Preset("unstable", "Horrible Instability",
        list("mystcraft:terrain_normal", "mystcraft:biome_small",
            "mystcraft:lighting_dark",
            "mystcraft:weather_storm", "mystcraft:caves", "mystcraft:ravines",
            "mystcraft:env_meteors", "mystcraft:env_lightning", "mystcraft:env_scorched",
            "mystcraft:env_explosions", "mystcraft:env_accelerated", "mystcraft:dense_ores",
            "mystcraft:floating_islands", "mystcraft:perlin_worms", "mystcraft:color_crimson",
            "mystcraft:color_sky", "mystcraft:color_maroon", "mystcraft:color_fog",
            "mystcraft:color_ruby", "mystcraft:color_grass", "mystcraft:color_crimson",
            "mystcraft:color_foliage", "mystcraft:color_navy", "mystcraft:color_water",
            "mystcraft:color_dark_gray", "mystcraft:color_cloud", "mystcraft:gradient_blood_sky",
            "mystcraft:cloud_low", "mystcraft:hide_horizon", "mystcraft:env_shorter_days"),
        list(
            pool(8, "mystcraft:biome_nether_wastes", "mystcraft:biome_the_end",
                "mystcraft:biome_desert", "mystcraft:biome_mushroom_fields",
                "mystcraft:biome_ice_spikes", "mystcraft:biome_jungle", "mystcraft:biome_deep_dark",
                "mystcraft:biome_dark_forest", "mystcraft:biome_badlands",
                "mystcraft:biome_soul_sand_valley", "mystcraft:biome_basalt_deltas",
                "mystcraft:biome_crimson_forest"),
            pool(4, "mystcraft:tendrils", "mystcraft:vertical_tendrils", "mystcraft:spheres",
                "mystcraft:spikes", "mystcraft:obelisks", "mystcraft:crystal_formation"),
            pool(4, "mystcraft:nether_fortress", "mystcraft:end_cities", "mystcraft:ancient_cities",
                "mystcraft:woodland_mansions", "mystcraft:strongholds", "mystcraft:dungeons",
                "mystcraft:bastion_remnants")
        )
    ));

    // 24. The Empty - Empty void, dark, star fissures only
    register(new Preset("void", "The Empty",
        list("mystcraft:terrain_void", "mystcraft:biome_single",
            "mystcraft:lighting_dark", "mystcraft:weather_off",
            "mystcraft:no_ores", "mystcraft:biome_plains", "mystcraft:color_black",
            "mystcraft:color_sky", "mystcraft:color_dark_gray", "mystcraft:color_fog",
            "mystcraft:hide_horizon", "mystcraft:cloud_none", "mystcraft:env_static_time"),
        list(
            pool(3, "mystcraft:star_fissure_feature", "mystcraft:crystal_formation",
                "mystcraft:obelisks", "mystcraft:floating_islands", "mystcraft:spheres"),
            pool(1, "mystcraft:env_meteors", "mystcraft:env_lightning", "mystcraft:rainbow")
        )
    ));

    // 25. Corrupted Overworld - Nether biomes on overworld terrain, storms, lava
    register(new Preset("corrupted", "Corrupted Overworld",
        list("mystcraft:terrain_normal", "mystcraft:biome_medium",
            "mystcraft:lighting_nether",
            "mystcraft:weather_storm", "mystcraft:block_minecraft_netherrack", "mystcraft:sea_lava",
            "mystcraft:caves", "mystcraft:ravines", "mystcraft:perlin_worms",
            "mystcraft:env_lightning", "mystcraft:env_scorched", "mystcraft:color_crimson",
            "mystcraft:color_sky", "mystcraft:color_maroon", "mystcraft:color_fog",
            "mystcraft:color_ruby", "mystcraft:color_grass", "mystcraft:color_crimson",
            "mystcraft:color_foliage", "mystcraft:gradient_blood_sky", "mystcraft:cloud_low",
            "mystcraft:env_shorter_days"),
        list(
            pool(4, "mystcraft:biome_crimson_forest", "mystcraft:biome_warped_forest",
                "mystcraft:biome_soul_sand_valley", "mystcraft:biome_basalt_deltas",
                "mystcraft:biome_nether_wastes"),
            pool(3, "mystcraft:nether_fortress", "mystcraft:bastion_remnants", "mystcraft:dungeons",
                "mystcraft:ruined_portals", "mystcraft:ancient_cities"),
            pool(2, "mystcraft:tendrils", "mystcraft:vertical_tendrils", "mystcraft:spikes",
                "mystcraft:spheres", "mystcraft:obelisks"),
            pool(2, "mystcraft:extra_gold_ore", "mystcraft:extra_diamond_ore",
                "mystcraft:extra_redstone_ore", "mystcraft:dense_ores")
        )
    ));

    PRESET_NAMES = List.copyOf(PRESETS.keySet());
  }

  private AgePresets() {
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
  public record RandomPool(int pickCount, List<String> options) {
  }

  /**
   * A curated preset definition.
   */
  public record Preset(String name, String displayName, List<String> fixedSymbols, List<RandomPool> randomPools) {
  }
}
