package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.SyncAgeDataPacket;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles syncing Age data to clients when they enter/leave Age dimensions.
 */
public class AgeDataSyncHandler {

  /**
   * Syncs Age data when a player changes dimensions.
   */
  public static void onPlayerChangeDimension(ServerPlayer player) {
    ServerLevel toLevel = player.serverLevel();

    if (AgeDimensionFactory.isMystcraftAge(toLevel.dimension())) {
      syncAgeDataToPlayer(player, toLevel);
    }
  }

  /**
   * Syncs Age data when a player logs in to an Age dimension.
   */
  public static void onPlayerLoggedIn(ServerPlayer player) {
    ServerLevel level = player.serverLevel();

    if (AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      syncAgeDataToPlayer(player, level);
    }
  }

  /**
   * Syncs Age data when a player respawns in an Age dimension.
   */
  public static void onPlayerRespawn(ServerPlayer player) {
    ServerLevel level = player.serverLevel();

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
    java.util.List<Integer> grassColors = ageData.getGrassColors();
    if (!grassColors.isEmpty()) {
      int[] arr = new int[grassColors.size()];
      for (int i = 0; i < grassColors.size(); i++) {
        arr[i] = grassColors.get(i);
      }
      config.putIntArray("GrassColors", arr);
    }
    if (ageData.getFoliageColor() != -1) config.putInt("FoliageColor", ageData.getFoliageColor());
    if (ageData.getWaterColor() != -1) config.putInt("WaterColor", ageData.getWaterColor());
    if (ageData.getCloudColor() != -1) config.putInt("CloudColor", ageData.getCloudColor());
    if (ageData.getNightSkyColor() != -1) config.putInt("NightSkyColor", ageData.getNightSkyColor());
    if (ageData.getHorizonColor() != -1) config.putInt("HorizonColor", ageData.getHorizonColor());
    if (ageData.getSunsetColor() != -1) config.putInt("SunsetColor", ageData.getSunsetColor());

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

    // Log color pipeline trace for debugging
    Mystcraft.LOGGER.info("[AgeSync] Age {} -> player {}: sky=0x{}, fog=0x{}, grass=0x{}, foliage=0x{}, water=0x{}, cloud=0x{}, nightSky=0x{}, sunset=0x{}",
        ageUID, player.getName().getString(),
        ageData.getSkyColor() != -1 ? Integer.toHexString(ageData.getSkyColor()) : "none",
        ageData.getFogColor() != -1 ? Integer.toHexString(ageData.getFogColor()) : "none",
        !ageData.getGrassColors().isEmpty() ? ageData.getGrassColors().size() + " colors" : "none",
        ageData.getFoliageColor() != -1 ? Integer.toHexString(ageData.getFoliageColor()) : "none",
        ageData.getWaterColor() != -1 ? Integer.toHexString(ageData.getWaterColor()) : "none",
        ageData.getCloudColor() != -1 ? Integer.toHexString(ageData.getCloudColor()) : "none",
        ageData.getNightSkyColor() != -1 ? Integer.toHexString(ageData.getNightSkyColor()) : "none",
        ageData.getSunsetColor() != -1 ? Integer.toHexString(ageData.getSunsetColor()) : "none");
  }
}
