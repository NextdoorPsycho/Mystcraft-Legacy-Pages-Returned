package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.Mystcraft;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;

import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Built-in datapack logic types derived from current symbol behaviors.
 */
public final class SymbolLogicTypes {

    private SymbolLogicTypes() {}

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
        SymbolLogicRegistry.register(new AddBiomeType());
        SymbolLogicRegistry.register(new SetOreDisabledType());
        SymbolLogicRegistry.register(new SetOresDisabledType());
        SymbolLogicRegistry.register(new SetOreMultiplierType());
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
}
