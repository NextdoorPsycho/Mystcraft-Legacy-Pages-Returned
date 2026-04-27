package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collections;

/**
 * Packet sent from server to client to sync Age data.
 * Used to keep clients informed about Age properties.
 */
public record SyncAgeDataPacket(int ageUID, CompoundTag data) {

  public static void encode(SyncAgeDataPacket packet, FriendlyByteBuf buf) {
    buf.writeVarInt(packet.ageUID);
    buf.writeNbt(packet.data);
  }

  public static SyncAgeDataPacket decode(FriendlyByteBuf buf) {
    int ageUID = buf.readVarInt();
    CompoundTag data = buf.readNbt();
    return new SyncAgeDataPacket(ageUID, data);
  }

  public static void handle(SyncAgeDataPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      // Store the age data in client-side cache even if the client level isn't ready yet.
      ClientAgeDataCache.setAgeData(packet.ageUID, packet.data);

      // Log received color values for pipeline tracing
      CompoundTag config = packet.data.contains("AgeConfig") ? packet.data.getCompound("AgeConfig") : null;
      if (config != null) {
        String[] colorKeys = {"SkyColor", "FogColor", "GrassColors", "FoliageColor", "WaterColor", "CloudColor", "NightSkyColor", "SunsetColor"};
        StringBuilder colorLog = new StringBuilder();
        for (String key : colorKeys) {
          if (config.contains(key)) {
            if (colorLog.length() > 0) colorLog.append(", ");
            colorLog.append(key).append("=0x").append(Integer.toHexString(config.getInt(key)));
          }
        }
        Mystcraft.LOGGER.info("[AgeSync Client] Age {} received: {}", packet.ageUID,
            colorLog.length() > 0 ? colorLog.toString() : "no custom colors");
      }
    });
  }

  /**
   * Client-side cache for Age data.
   * Stores synced Age information for rendering and display.
   */
  public static class ClientAgeDataCache {
    private static final java.util.Map<Integer, CompoundTag> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setAgeData(int ageUID, CompoundTag data) {
      if (data != null) {
        CACHE.put(ageUID, data);
      } else {
        CACHE.remove(ageUID);
      }
    }

    public static CompoundTag getAgeData(int ageUID) {
      return CACHE.get(ageUID);
    }

    public static void clear() {
      CACHE.clear();
    }

    public static String getAgeName(int ageUID) {
      CompoundTag data = CACHE.get(ageUID);
      if (data != null && data.contains("AgeName")) {
        return data.getString("AgeName");
      }
      return "Age " + ageUID;
    }

    public static float getInstability(int ageUID) {
      CompoundTag data = CACHE.get(ageUID);
      if (data != null && data.contains("Instability")) {
        return data.getFloat("Instability");
      }
      return 0.0f;
    }

    // --- Rendering Configuration Getters ---

    private static CompoundTag getConfig(int ageUID) {
      CompoundTag data = CACHE.get(ageUID);
      if (data != null && data.contains("AgeConfig")) {
        return data.getCompound("AgeConfig");
      }
      return null;
    }

    public static int getSkyColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("SkyColor") ? config.getInt("SkyColor") : -1;
    }

    public static int getFogColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("FogColor") ? config.getInt("FogColor") : -1;
    }

    public static int getGrassColor(int ageUID) {
      java.util.List<Integer> colors = getGrassColors(ageUID);
      return colors.isEmpty() ? -1 : colors.get(0);
    }

    public static java.util.List<Integer> getGrassColors(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      if (config == null) return Collections.emptyList();
      if (config.contains("GrassColors")) {
        int[] arr = config.getIntArray("GrassColors");
        java.util.List<Integer> result = new java.util.ArrayList<>(arr.length);
        for (int c : arr) {
          result.add(c);
        }
        return result;
      }
      if (config.contains("GrassColor")) {
        return Collections.singletonList(config.getInt("GrassColor"));
      }
      return Collections.emptyList();
    }

    public static int getFoliageColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("FoliageColor") ? config.getInt("FoliageColor") : -1;
    }

    public static int getWaterColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("WaterColor") ? config.getInt("WaterColor") : -1;
    }

    public static int getCloudColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("CloudColor") ? config.getInt("CloudColor") : -1;
    }

    public static int getNightSkyColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("NightSkyColor") ? config.getInt("NightSkyColor") : -1;
    }

    public static int getHorizonColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("HorizonColor") ? config.getInt("HorizonColor") : -1;
    }

    public static String getLightingType(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      if (config != null && config.contains("LightingType")) {
        String type = config.getString("LightingType");
        return type.isEmpty() ? "normal" : type;
      }
      return "normal";
    }

    public static boolean isHorizonHidden(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBoolean("HorizonHidden");
    }

    public static boolean isRainbowEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBoolean("RainbowEnabled");
    }

    public static boolean areMeteorsEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBoolean("MeteorsEnabled");
    }

    public static boolean isLightningEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBoolean("LightningEnabled");
    }

    public static boolean areExplosionsEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBoolean("ExplosionsEnabled");
    }

    public static int getSunsetColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("SunsetColor") ? config.getInt("SunsetColor") : -1;
    }

    public static float getCloudHeight(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("CloudHeight") ? config.getFloat("CloudHeight") : 192.0f;
    }

    public static float getHorizonHeight(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("HorizonHeight") ? config.getFloat("HorizonHeight") : 0.0f;
    }

  }
}
