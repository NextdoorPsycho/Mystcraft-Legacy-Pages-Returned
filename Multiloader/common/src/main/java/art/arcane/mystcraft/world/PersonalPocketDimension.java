package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Helper for creating and managing personal pocket dimensions.
 */
public final class PersonalPocketDimension {

    private static final int PERSONAL_UID_OFFSET = 1_000_000_000;
    private static final int PERSONAL_UID_RANGE = 1_000_000_000;
    private static final int BORDER_SIZE_BLOCKS = 160;
    private static final BlockPos POCKET_SPAWN = new BlockPos(0, 64, 0);

    private PersonalPocketDimension() {
    }

    public static int getPersonalAgeUid(UUID playerId) {
        int hash = playerId.hashCode();
        int uid = PERSONAL_UID_OFFSET + Math.floorMod(hash, PERSONAL_UID_RANGE);
        if (uid == 0 || uid == 1) {
            uid = PERSONAL_UID_OFFSET + 2;
        }
        return uid;
    }

    public static BlockPos getPocketSpawn() {
        return POCKET_SPAWN;
    }

    public static boolean isPersonalPocket(ServerLevel level) {
        AgeData ageData = AgeData.getIfPresent(level);
        return ageData != null && ageData.isPersonalPocket();
    }

    @Nullable
    public static ServerLevel getOrCreate(MinecraftServer server, UUID owner) {
        int ageUID = getPersonalAgeUid(owner);
        AgeManager ageManager = AgeManager.get(server);
        ResourceLocation dimLoc = ageManager.getDimension(ageUID);
        if (dimLoc != null) {
            ServerLevel existing = server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimLoc));
            if (existing != null) {
                Mystcraft.LOGGER.debug("[PersonalPocket] Using loaded pocket {} for {}", dimLoc, owner);
                return existing;
            }
            ServerLevel loaded = AgeDimensionFactory.getOrCreateAgeDimension(server, ageUID);
            if (loaded != null) {
                Mystcraft.LOGGER.info("[PersonalPocket] Loaded pocket {} for {}", dimLoc, owner);
                return loaded;
            }
            Mystcraft.LOGGER.warn("[PersonalPocket] Failed to load existing pocket {} for {}, recreating", dimLoc, owner);
        }

        AgeDirectorImpl director = buildPersonalDirector(server);
        UUID ageUUID = owner;

        Mystcraft.LOGGER.info("[PersonalPocket] Creating new pocket dimension for {}", owner);
        ServerLevel level = AgeDimensionFactory.createAgeDimension(server, ageUID, ageUUID, director);
        if (level == null) {
            return null;
        }

        ResourceLocation newDimLoc = new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_age_" + ageUID);
        ageManager.registerAge(ageUID, newDimLoc, ageUUID);

        AgeData ageData = AgeData.get(level);
        ageData.setAgeUID(ageUID);
        ageData.setAgeUUID(ageUUID);
        ageData.setAgeName("Personal Pocket");
        ageData.copyFromDirector(director);
        ageData.setSpawn(POCKET_SPAWN.getX(), POCKET_SPAWN.getY(), POCKET_SPAWN.getZ());

        configurePersonalRules(level);
        enforceBorder(level);

        return level;
    }

    private static AgeDirectorImpl buildPersonalDirector(MinecraftServer server) {
        AgeDirectorImpl director = new AgeDirectorImpl(0L);
        director.setPersonalPocket(true);
        director.setTerrainType("personal");
        director.setTerrainBlock(Blocks.SMOOTH_STONE.defaultBlockState());
        director.setWeatherType("off");
        director.setLightingType("bright");
        director.setSunVisible(true);
        director.setMoonVisible(false);
        director.setStarsVisible(false);
        director.setTimescale(0.0f);
        director.setCavesEnabled(false);
        director.setRavinesEnabled(false);
        director.setFloatingIslandsEnabled(false);
        director.setSkylandsEnabled(false);
        director.setVillagesEnabled(false);
        director.setDungeonsEnabled(false);
        director.setMineshaftsEnabled(false);
        director.setStrongholdsEnabled(false);
        director.setPillagerOutpostsEnabled(false);
        director.setRuinedPortalsEnabled(false);
        director.setAncientCitiesEnabled(false);
        director.setTrailRuinsEnabled(false);
        director.setOceanMonumentsEnabled(false);
        director.setWitchHutsEnabled(false);
        director.setDesertTemplesEnabled(false);
        director.setJungleTemplesEnabled(false);
        director.setWoodlandMansionsEnabled(false);
        director.setEndCitiesEnabled(false);
        director.setBastionRemnantsEnabled(false);
        director.setIgloosEnabled(false);
        director.setShipwrecksEnabled(false);
        director.setOceanRuinsEnabled(false);
        director.setBuriedTreasureEnabled(false);
        director.setNetherFossilsEnabled(false);
        director.setDripstoneCavesEnabled(false);
        director.setLushCavesEnabled(false);
        director.setDeepDarkEnabled(false);
        director.setSurfaceLakesEnabled(false);
        director.setDeepLakesEnabled(false);
        director.setCrystalsEnabled(false);
        director.setDenseOresEnabled(false);
        director.setExplosionsEnabled(false);
        director.setMeteorsEnabled(false);
        director.setLightningEnabled(false);
        director.setScorchedEnabled(false);
        director.setObelisksEnabled(false);
        director.setRainbowEnabled(false);
        director.setSpheresEnabled(false);
        director.setSpikesEnabled(false);
        director.setStarFissureEnabled(false);
        director.setStarFissureExplicit(true);
        director.setStarFissureParams(new JsonObject());
        director.setTendrilsEnabled(false);
        director.setVerticalTendrilsEnabled(false);
        director.setPerlinWormsEnabled(false);
        director.setPvPEnabled(true);

        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> voidBiome = biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
        director.setBiomeController("single");
        director.addBiome(voidBiome);

        return director;
    }

    public static void configurePersonalRules(ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.setDayTime(18000L);
    }

    public static void enforceBorder(ServerLevel level) {
        WorldBorder border = level.getWorldBorder();
        boolean needsUpdate = false;

        if (border.getCenterX() != 0.0 || border.getCenterZ() != 0.0) {
            border.setCenter(0.0, 0.0);
            needsUpdate = true;
        }
        if (border.getSize() != BORDER_SIZE_BLOCKS) {
            border.setSize(BORDER_SIZE_BLOCKS);
            needsUpdate = true;
        }
        // Set warning distance so border is visible (15 blocks from edge)
        if (border.getWarningBlocks() != 15) {
            border.setWarningBlocks(15);
            needsUpdate = true;
        }
        // No damage from border in personal pocket (player gets teleported back instead)
        if (border.getDamagePerBlock() != 0.0) {
            border.setDamagePerBlock(0.0);
            needsUpdate = true;
        }
        if (border.getDamageSafeZone() != 0.0) {
            border.setDamageSafeZone(0.0);
            needsUpdate = true;
        }
    }
}
