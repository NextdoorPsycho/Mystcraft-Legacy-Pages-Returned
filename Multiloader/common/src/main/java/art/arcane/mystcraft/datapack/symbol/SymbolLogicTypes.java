package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.AgeDirector;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerGrid;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerNative;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerNoise;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerSingle;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerTiled;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Built-in datapack logic types derived from current symbol behaviors.
 */
public final class SymbolLogicTypes {

    private SymbolLogicTypes() {}

    // Color palettes reused by color target symbols (legacy parity).
    private static final int[] VIBRANT_COLORS = {
            0x87CEEB, 0x98FB98, 0xFFB6C1, 0xDDA0DD, 0xF0E68C, 0xADD8E6, 0x90EE90, 0xFFDAB9,
            0xE6E6FA, 0xFFFACD, 0xB0E0E6, 0xE0FFFF, 0x7FFFD4, 0xFFE4B5, 0xFAFAD2, 0xD8BFD8,
            0xFF7F50, 0x40E0D0, 0xFFD700, 0xDC143C, 0x50C878, 0xFF007F, 0xFFBF00, 0xEE82EE
    };
    private static final int[] SKY_COLORS = {
            0x87CEEB, 0x00BFFF, 0x87CEFA, 0xB0C4DE, 0x6495ED, 0x4169E1, 0xADD8E6, 0xE0FFFF,
            0xFFB6C1, 0xFFA07A, 0x98FB98, 0xDDA0DD, 0xFF6347, 0xFFD700, 0x8B0000, 0x4B0082,
            0x191970, 0x00FF88
    };
    private static final int[] FOG_COLORS = {
            0xC0C0C0, 0xD3D3D3, 0xE8E8E8, 0xB0E0E6, 0xE6E6FA, 0xFFFAF0, 0xF5F5DC, 0x98FB98
    };
    private static final int[] GRASS_COLORS = {
            0x7CFC00, 0x90EE90, 0x32CD32, 0x228B22, 0x006400, 0x9ACD32, 0x6B8E23, 0x556B2F,
            0xADFF2F, 0x98FB98, 0x8B4513, 0xFF8C00, 0xDC143C, 0x4B0082, 0x00CED1, 0xFFD700
    };
    private static final int[] FOLIAGE_COLORS = {
            0x228B22, 0x006400, 0x2E8B57, 0x3CB371, 0x20B2AA, 0x008B8B, 0x556B2F, 0x8B4513,
            0xFF4500, 0xFFD700
    };
    private static final int[] WATER_COLORS = {
            0x1E90FF, 0x00CED1, 0x40E0D0, 0x48D1CC, 0x00FFFF, 0x5F9EA0, 0x4682B4, 0x6495ED,
            0x7B68EE, 0x8A2BE2, 0xFF4500, 0x50C878, 0x191970, 0xDC143C, 0x00FF88, 0xFFD700
    };
    private static final int[] SUNSET_COLORS = {
            0xFF4500, 0xFF6347, 0xFF7F50, 0xFFA07A, 0xFFD700, 0xFF8C00, 0xDC143C, 0x8B0000,
            0xFF69B4, 0xDA70D6, 0x4B0082, 0x00FF88
    };

    public static void registerDefaults() {
        PopulatorRegistry.registerDefaults();
        TerrainAlterationRegistry.registerDefaults();

        registerSimpleString("set_terrain_type", AgeDirector::setTerrainType);
        registerSimpleString("set_terrain_mix_mode", AgeDirector::setTerrainMixMode);
        registerSimpleString("set_secondary_terrain_type", AgeDirector::setSecondaryTerrainType);
        registerSimpleString("set_biome_controller", AgeDirector::setBiomeController);
        registerSimpleString("set_weather_type", AgeDirector::setWeatherType);
        registerSimpleString("set_lighting_type", AgeDirector::setLightingType);
        registerSimpleString("set_star_type", AgeDirector::setStarType);

        registerSimpleInt("set_average_ground_level", AgeDirector::setAverageGroundLevel);
        registerSimpleInt("set_sea_level", AgeDirector::setSeaLevel);
        registerSimpleInt("set_sky_color", AgeDirector::setSkyColor, AgeDirector::setSkyColorNatural);
        registerSimpleInt("set_fog_color", AgeDirector::setFogColor, AgeDirector::setFogColorNatural);
        registerSimpleInt("set_grass_color", AgeDirector::setGrassColor, AgeDirector::setGrassColorNatural);
        registerSimpleInt("set_foliage_color", AgeDirector::setFoliageColor, AgeDirector::setFoliageColorNatural);
        registerSimpleInt("set_water_color", AgeDirector::setWaterColor, AgeDirector::setWaterColorNatural);
        registerSimpleInt("set_cloud_color", AgeDirector::setCloudColor, AgeDirector::setCloudColorNatural);
        registerSimpleInt("set_horizon_color", AgeDirector::setHorizonColor, AgeDirector::setHorizonColorNatural);
        registerSimpleInt("set_night_sky_color", AgeDirector::setNightSkyColor, null);
        registerSimpleInt("set_sunset_color", AgeDirector::setSunsetColor, null);

        registerSimpleFloat("set_cloud_height", AgeDirector::setCloudHeight);
        registerSimpleFloat("set_horizon_height", AgeDirector::setHorizonHeight);
        registerSimpleFloat("set_timescale", AgeDirector::setTimescale);

        registerSimpleBoolean("set_has_sea", AgeDirector::setHasSea);
        registerSimpleBoolean("set_sun_visible", AgeDirector::setSunVisible);
        registerSimpleBoolean("set_moon_visible", AgeDirector::setMoonVisible);
        registerSimpleBoolean("set_stars_visible", AgeDirector::setStarsVisible);

        registerSimpleInt("push_color", (director, value) -> director.pushColor(value), null);
        registerSimpleInt("push_gradient", (director, value) -> director.pushGradient(value), null);
        registerSimpleFloat("push_angle", (director, value) -> director.pushAngle(value));
        registerSimpleFloat("push_phase", (director, value) -> director.pushPhase(value));
        registerSimpleFloat("push_length", (director, value) -> director.pushLength(value));

        SymbolLogicRegistry.register(new AddInstabilityType());
        SymbolLogicRegistry.register(new SetFlagType());
        SymbolLogicRegistry.register(new SetTerrainBlockType());
        SymbolLogicRegistry.register(new SetSeaBlockType());
        SymbolLogicRegistry.register(new RegisterPopulatorType());
        SymbolLogicRegistry.register(new RegisterTerrainAlterationType());
        SymbolLogicRegistry.register(new RegisterBiomeControllerType());
        SymbolLogicRegistry.register(new AddBiomeType());
        SymbolLogicRegistry.register(new SetOreDisabledType());
        SymbolLogicRegistry.register(new SetOresDisabledType());
        SymbolLogicRegistry.register(new SetOreMultiplierType());
        SymbolLogicRegistry.register(new SetSecondaryTerrainWeightedType());
        SymbolLogicRegistry.register(new ColorFromStackType());
    }

    private static void registerSimpleString(String id, BiConsumer<AgeDirector, String> setter) {
        SymbolLogicRegistry.register(new SimpleStringType(id, setter));
    }

    private static void registerSimpleInt(String id, BiConsumer<AgeDirector, Integer> setter,
                                          BiConsumer<AgeDirector, Boolean> naturalSetter) {
        SymbolLogicRegistry.register(new SimpleIntType(id, setter, naturalSetter));
    }

    private static void registerSimpleInt(String id, BiConsumer<AgeDirector, Integer> setter) {
        registerSimpleInt(id, setter, null);
    }

    private static void registerSimpleFloat(String id, BiConsumer<AgeDirector, Float> setter) {
        SymbolLogicRegistry.register(new SimpleFloatType(id, setter));
    }

    private static void registerSimpleBoolean(String id, BiConsumer<AgeDirector, Boolean> setter) {
        SymbolLogicRegistry.register(new SimpleBooleanType(id, setter));
    }

    private static class SimpleStringType implements SymbolLogicType {
        private final ResourceLocation id;
        private final BiConsumer<AgeDirector, String> setter;

        private SimpleStringType(String path, BiConsumer<AgeDirector, String> setter) {
            this.id = new ResourceLocation(Mystcraft.MOD_ID, path);
            this.setter = setter;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String value = GsonHelper.getAsString(json, "value");
            return (director, seed) -> setter.accept(director, value);
        }
    }

    private static class SimpleIntType implements SymbolLogicType {
        private final ResourceLocation id;
        private final BiConsumer<AgeDirector, Integer> setter;
        private final BiConsumer<AgeDirector, Boolean> naturalSetter;

        private SimpleIntType(String path, BiConsumer<AgeDirector, Integer> setter,
                              BiConsumer<AgeDirector, Boolean> naturalSetter) {
            this.id = new ResourceLocation(Mystcraft.MOD_ID, path);
            this.setter = setter;
            this.naturalSetter = naturalSetter;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            int value = GsonHelper.getAsInt(json, "value");
            boolean hasNatural = naturalSetter != null && json.has("natural");
            boolean natural = hasNatural && GsonHelper.getAsBoolean(json, "natural");
            return (director, seed) -> {
                setter.accept(director, value);
                if (naturalSetter != null && hasNatural) {
                    naturalSetter.accept(director, natural);
                }
            };
        }
    }

    private static class SimpleFloatType implements SymbolLogicType {
        private final ResourceLocation id;
        private final BiConsumer<AgeDirector, Float> setter;

        private SimpleFloatType(String path, BiConsumer<AgeDirector, Float> setter) {
            this.id = new ResourceLocation(Mystcraft.MOD_ID, path);
            this.setter = setter;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            float value = GsonHelper.getAsFloat(json, "value");
            return (director, seed) -> setter.accept(director, value);
        }
    }

    private static class SimpleBooleanType implements SymbolLogicType {
        private final ResourceLocation id;
        private final BiConsumer<AgeDirector, Boolean> setter;

        private SimpleBooleanType(String path, BiConsumer<AgeDirector, Boolean> setter) {
            this.id = new ResourceLocation(Mystcraft.MOD_ID, path);
            this.setter = setter;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            boolean value = GsonHelper.getAsBoolean(json, "value");
            return (director, seed) -> setter.accept(director, value);
        }
    }

    private static class AddInstabilityType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "add_instability");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            float value = GsonHelper.getAsFloat(json, "value");
            return (director, seed) -> director.addInstability(value);
        }
    }

    private static class SetFlagType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_flag");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String flag = GsonHelper.getAsString(json, "flag").toLowerCase(Locale.ROOT);
            boolean value = GsonHelper.getAsBoolean(json, "value");
            return (director, seed) -> applyFlag(director, flag, value);
        }

        private void applyFlag(AgeDirector director, String flag, boolean value) {
            switch (flag) {
                case "caves_enabled" -> director.setCavesEnabled(value);
                case "ravines_enabled" -> director.setRavinesEnabled(value);
                case "floating_islands_enabled" -> director.setFloatingIslandsEnabled(value);
                case "skylands_enabled" -> director.setSkylandsEnabled(value);
                case "villages_enabled" -> director.setVillagesEnabled(value);
                case "dungeons_enabled" -> director.setDungeonsEnabled(value);
                case "mineshafts_enabled" -> director.setMineshaftsEnabled(value);
                case "strongholds_enabled" -> director.setStrongholdsEnabled(value);
                case "accelerated_enabled" -> director.setAcceleratedEnabled(value);
                case "meteors_enabled" -> director.setMeteorsEnabled(value);
                case "lightning_enabled" -> director.setLightningEnabled(value);
                case "scorched_enabled" -> director.setScorchedEnabled(value);
                case "nether_fort_enabled" -> director.setNetherFortEnabled(value);
                case "dense_ores_enabled" -> director.setDenseOresEnabled(value);
                case "huge_trees_enabled" -> director.setHugeTreesEnabled(value);
                case "deep_lakes_enabled" -> director.setDeepLakesEnabled(value);
                case "surface_lakes_enabled" -> director.setSurfaceLakesEnabled(value);
                case "spikes_enabled" -> director.setSpikesEnabled(value);
                case "spheres_enabled" -> director.setSpheresEnabled(value);
                case "tendrils_enabled" -> director.setTendrilsEnabled(value);
                case "vertical_tendrils_enabled" -> director.setVerticalTendrilsEnabled(value);
                case "perlin_worms_enabled" -> director.setPerlinWormsEnabled(value);
                case "crystals_enabled" -> director.setCrystalsEnabled(value);
                case "horizon_hidden" -> director.setHorizonHidden(value);
                case "rainbow_enabled" -> director.setRainbowEnabled(value);
                case "obelisks_enabled" -> director.setObelisksEnabled(value);
                case "star_fissure_enabled" -> director.setStarFissureEnabled(value);
                case "explosions_enabled" -> director.setExplosionsEnabled(value);
                case "pvp_enabled" -> director.setPvPEnabled(value);
                case "pillager_outposts_enabled" -> director.setPillagerOutpostsEnabled(value);
                case "ruined_portals_enabled" -> director.setRuinedPortalsEnabled(value);
                case "ancient_cities_enabled" -> director.setAncientCitiesEnabled(value);
                case "trail_ruins_enabled" -> director.setTrailRuinsEnabled(value);
                case "ocean_monuments_enabled" -> director.setOceanMonumentsEnabled(value);
                case "witch_huts_enabled" -> director.setWitchHutsEnabled(value);
                case "desert_temples_enabled" -> director.setDesertTemplesEnabled(value);
                case "jungle_temples_enabled" -> director.setJungleTemplesEnabled(value);
                case "woodland_mansions_enabled" -> director.setWoodlandMansionsEnabled(value);
                case "end_cities_enabled" -> director.setEndCitiesEnabled(value);
                case "bastion_remnants_enabled" -> director.setBastionRemnantsEnabled(value);
                case "igloos_enabled" -> director.setIgloosEnabled(value);
                case "shipwrecks_enabled" -> director.setShipwrecksEnabled(value);
                case "ocean_ruins_enabled" -> director.setOceanRuinsEnabled(value);
                case "buried_treasure_enabled" -> director.setBuriedTreasureEnabled(value);
                case "nether_fossils_enabled" -> director.setNetherFossilsEnabled(value);
                case "dripstone_caves_enabled" -> director.setDripstoneCavesEnabled(value);
                case "lush_caves_enabled" -> director.setLushCavesEnabled(value);
                case "deep_dark_enabled" -> director.setDeepDarkEnabled(value);
                default -> Mystcraft.LOGGER.warn("[SymbolLogic] Unknown flag {}", flag);
            }
        }
    }

    private static class SetTerrainBlockType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_terrain_block");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            ResourceLocation blockId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "block"));
            return (director, seed) -> {
                if (blockId == null) return;
                Block block = BuiltInRegistries.BLOCK.get(blockId);
                if (block != null) {
                    director.setTerrainBlock(block.defaultBlockState());
                }
            };
        }
    }

    private static class SetSeaBlockType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_sea_block");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            ResourceLocation blockId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "block"));
            return (director, seed) -> {
                if (blockId == null) return;
                Block block = BuiltInRegistries.BLOCK.get(blockId);
                if (block != null) {
                    director.setSeaBlock(block.defaultBlockState());
                }
            };
        }
    }

    private static class RegisterPopulatorType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "register_populator");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String rawId = GsonHelper.getAsString(json, "id");
            ResourceLocation popId = ResourceLocation.tryParse(rawId);
            JsonObject params = json.has("params") && json.get("params").isJsonObject()
                    ? json.getAsJsonObject("params")
                    : json;
            return (director, seed) -> {
                if (popId == null) return;
                var populator = PopulatorRegistry.create(popId, params, seed);
                if (populator != null) {
                    director.registerInterface(populator);
                }
            };
        }
    }

    private static class RegisterTerrainAlterationType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "register_terrain_alteration");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String rawId = GsonHelper.getAsString(json, "id");
            ResourceLocation altId = ResourceLocation.tryParse(rawId);
            JsonObject params = json.has("params") && json.get("params").isJsonObject()
                    ? json.getAsJsonObject("params")
                    : json;
            return (director, seed) -> {
                if (altId == null) return;
                var alteration = TerrainAlterationRegistry.create(altId, params, seed);
                if (alteration != null) {
                    director.registerInterface(alteration);
                }
            };
        }
    }

    private static class RegisterBiomeControllerType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "register_biome_controller");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String type = GsonHelper.getAsString(json, "type", "medium").toLowerCase(Locale.ROOT);
            return (director, seed) -> {
                List<Holder<Biome>> biomes = collectBiomes(director);
                IBiomeController controller = switch (type) {
                    case "single" -> new BiomeControllerSingle(biomes, seed);
                    case "native" -> new BiomeControllerNative(seed);
                    case "tiny" -> new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.TINY);
                    case "small" -> new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.SMALL);
                    case "medium" -> new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.MEDIUM);
                    case "large" -> new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.LARGE);
                    case "huge" -> new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.HUGE);
                    case "tiled" -> new BiomeControllerTiled(biomes, seed);
                    case "grid" -> new BiomeControllerGrid(biomes, seed);
                    default -> new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.MEDIUM);
                };
                director.registerInterface(controller);
            };
        }

        private List<Holder<Biome>> collectBiomes(AgeDirector director) {
            List<Holder<Biome>> biomes = new ArrayList<>();
            Holder<Biome> biome;
            while ((biome = director.popBiome()) != null) {
                biomes.add(biome);
            }
            biomes.addAll(director.getBiomes());
            return biomes;
        }
    }

    private static class AddBiomeType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "add_biome");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String rawId = GsonHelper.getAsString(json, "biome");
            ResourceLocation biomeId = ResourceLocation.tryParse(rawId);
            return (director, seed) -> {
                if (biomeId == null) return;
                var server = Mystcraft.getCurrentServer();
                if (server == null) return;
                server.registryAccess().registry(Registries.BIOME).ifPresent(registry -> {
                    ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, biomeId);
                    Holder<Biome> holder = registry.getHolderOrThrow(key);
                    director.pushBiome(holder);
                    director.addBiome(holder);
                });
            };
        }
    }

    private static class SetOreDisabledType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_ore_disabled");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String ore = GsonHelper.getAsString(json, "ore");
            boolean disabled = GsonHelper.getAsBoolean(json, "disabled", true);
            return (director, seed) -> director.setOreDisabled(ore, disabled);
        }
    }

    private static class SetOresDisabledType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_ores_disabled");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            boolean disabled = GsonHelper.getAsBoolean(json, "disabled", true);
            return (director, seed) -> director.setOresDisabled(disabled);
        }
    }

    private static class SetOreMultiplierType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_ore_multiplier");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String ore = GsonHelper.getAsString(json, "ore");
            float multiplier = GsonHelper.getAsFloat(json, "multiplier", 1.0f);
            return (director, seed) -> director.setOreMultiplier(ore, multiplier);
        }
    }

    private static class SetSecondaryTerrainWeightedType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_secondary_terrain_weighted");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            List<String> options = new ArrayList<>();
            if (json.has("options")) {
                for (var element : GsonHelper.getAsJsonArray(json, "options")) {
                    options.add(element.getAsString());
                }
            }
            List<Integer> weights = new ArrayList<>();
            if (json.has("weights")) {
                for (var element : GsonHelper.getAsJsonArray(json, "weights")) {
                    weights.add(element.getAsInt());
                }
            }
            long salt = json.has("salt") ? GsonHelper.getAsLong(json, "salt") : 0xDEADBEEFL;
            return (director, seed) -> {
                if (options.isEmpty()) return;
                RandomSource random = RandomSource.create(seed ^ salt);
                int chosenIndex = pickWeightedIndex(random, options.size(), weights);
                director.setSecondaryTerrainType(options.get(chosenIndex));
            };
        }

        private int pickWeightedIndex(RandomSource random, int size, List<Integer> weights) {
            if (weights == null || weights.isEmpty()) {
                return random.nextInt(size);
            }
            int total = 0;
            for (int i = 0; i < size; i++) {
                int weight = i < weights.size() ? Math.max(0, weights.get(i)) : 0;
                total += weight;
            }
            if (total <= 0) {
                return random.nextInt(size);
            }
            int roll = random.nextInt(total);
            int cumulative = 0;
            for (int i = 0; i < size; i++) {
                int weight = i < weights.size() ? Math.max(0, weights.get(i)) : 0;
                cumulative += weight;
                if (roll < cumulative) {
                    return i;
                }
            }
            return 0;
        }
    }

    private static class ColorFromStackType implements SymbolLogicType {
        private final ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, "set_color_from_stack");

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public SymbolLogic parse(JsonObject json) {
            String target = GsonHelper.getAsString(json, "target", "").toLowerCase(Locale.ROOT);
            String palette = GsonHelper.getAsString(json, "palette", "vibrant").toLowerCase(Locale.ROOT);
            float randomInstability = GsonHelper.getAsFloat(json, "random_instability", 0.0f);
            long salt = json.has("salt") ? GsonHelper.getAsLong(json, "salt") : 0L;
            return (director, seed) -> {
                int color = director.popColor();
                if (color == -1) {
                    color = pickColor(palette, seed ^ salt);
                    if (randomInstability != 0.0f) {
                        director.addInstability(randomInstability);
                    }
                }
                applyTarget(director, target, color, seed ^ salt, randomInstability);
            };
        }

        private int pickColor(String palette, long seed) {
            if ("night_sky_random".equals(palette)) {
                RandomSource random = RandomSource.create(seed);
                int r = random.nextInt(40);
                int g = random.nextInt(40);
                int b = 20 + random.nextInt(60);
                return (r << 16) | (g << 8) | b;
            }
            int[] values = switch (palette) {
                case "sky" -> SKY_COLORS;
                case "fog" -> FOG_COLORS;
                case "grass" -> GRASS_COLORS;
                case "foliage" -> FOLIAGE_COLORS;
                case "water" -> WATER_COLORS;
                case "sunset" -> SUNSET_COLORS;
                case "vibrant" -> VIBRANT_COLORS;
                default -> VIBRANT_COLORS;
            };
            RandomSource random = RandomSource.create(seed);
            return values[random.nextInt(values.length)];
        }

        private void applyTarget(AgeDirector director, String target, int color, long seed, float randomInstability) {
            switch (target) {
                case "sky" -> director.setSkyColor(color);
                case "fog" -> director.setFogColor(color);
                case "grass" -> director.setGrassColor(color);
                case "foliage" -> director.setFoliageColor(color);
                case "water" -> director.setWaterColor(color);
                case "cloud" -> director.setCloudColor(color);
                case "horizon" -> director.setHorizonColor(color);
                case "sunset" -> director.setSunsetColor(color);
                case "night_sky" -> director.setNightSkyColor(color);
                default -> Mystcraft.LOGGER.warn("[SymbolLogic] Unknown color target {}", target);
            }
        }
    }
}
