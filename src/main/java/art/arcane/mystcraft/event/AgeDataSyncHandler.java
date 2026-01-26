package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.SyncAgeDataPacket;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles syncing Age data to clients when they enter/leave Age dimensions.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class AgeDataSyncHandler {

    /**
     * Sync Age data when a player changes dimensions.
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel toLevel = player.serverLevel();

        // Check if the destination is a Mystcraft Age
        if (AgeDimensionFactory.isMystcraftAge(toLevel.dimension())) {
            syncAgeDataToPlayer(player, toLevel);
        }
    }

    /**
     * Sync Age data when a player logs in to an Age dimension.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();

        // Check if player is in a Mystcraft Age
        if (AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            syncAgeDataToPlayer(player, level);
        }
    }

    /**
     * Sync Age data when a player respawns in an Age dimension.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();

        // Check if player is in a Mystcraft Age
        if (AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            syncAgeDataToPlayer(player, level);
        }
    }

    /**
     * Sends the Age data to a specific player.
     */
    public static void syncAgeDataToPlayer(ServerPlayer player, ServerLevel ageLevel) {
        AgeData ageData = AgeData.getIfPresent(ageLevel);
        if (ageData == null) return;

        int ageUID = ageData.getAgeUID();
        if (ageUID <= 0) return;

        // Create a compound tag with all the data we need on the client
        CompoundTag syncData = new CompoundTag();
        syncData.putString("AgeName", ageData.getAgeName());
        syncData.putFloat("Instability", ageData.getInstability());

        // Include the Age configuration for rendering
        CompoundTag config = new CompoundTag();
        config.putString("WeatherType", ageData.getWeatherType());
        config.putString("LightingType", ageData.getLightingType());
        config.putString("BiomeController", ageData.getBiomeController());

        // Colors (-1 means not set, so only include if set)
        if (ageData.getSkyColor() != -1) config.putInt("SkyColor", ageData.getSkyColor());
        if (ageData.getFogColor() != -1) config.putInt("FogColor", ageData.getFogColor());
        if (ageData.getGrassColor() != -1) config.putInt("GrassColor", ageData.getGrassColor());
        if (ageData.getFoliageColor() != -1) config.putInt("FoliageColor", ageData.getFoliageColor());
        if (ageData.getWaterColor() != -1) config.putInt("WaterColor", ageData.getWaterColor());
        if (ageData.getCloudColor() != -1) config.putInt("CloudColor", ageData.getCloudColor());
        if (ageData.getNightSkyColor() != -1) config.putInt("NightSkyColor", ageData.getNightSkyColor());

        // Celestials
        config.putBoolean("SunVisible", ageData.isSunVisible());
        config.putBoolean("MoonVisible", ageData.isMoonVisible());
        config.putBoolean("StarsVisible", ageData.areStarsVisible());
        config.putString("StarType", ageData.getStarType());

        // Special
        config.putBoolean("HorizonHidden", ageData.isHorizonHidden());

        // World heights
        config.putFloat("CloudHeight", ageData.getCloudHeight());
        config.putFloat("HorizonHeight", ageData.getHorizonHeight());

        syncData.put("AgeConfig", config);

        // Send the packet
        SyncAgeDataPacket packet = new SyncAgeDataPacket(ageUID, syncData);
        MystcraftNetwork.sendToPlayer(packet, player);

        // Log what we're sending for debugging
        int configKeys = config.getAllKeys().size();
        Mystcraft.LOGGER.info("Synced Age {} data to player {} ({} config keys, instability: {})",
                ageUID, player.getName().getString(), configKeys, ageData.getInstability());
    }

    /**
     * Syncs Age data to all players in a specific Age.
     * Call this after modifying Age configuration.
     */
    public static void syncAgeDataToAllInAge(ServerLevel ageLevel) {
        if (!AgeDimensionFactory.isMystcraftAge(ageLevel.dimension())) return;

        for (ServerPlayer player : ageLevel.players()) {
            syncAgeDataToPlayer(player, ageLevel);
        }
    }
}
