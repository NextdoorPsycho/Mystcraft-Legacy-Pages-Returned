package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Collections;

/**
 * Packet sent from server to client to sync Age data. Used to keep clients
 * informed about Age properties.
 */
public record SyncAgeDataPacket(int ageUID, CompoundTag data) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncAgeDataPacket> TYPE =
      MystcraftNetwork.type("sync_age_data");
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncAgeDataPacket> STREAM_CODEC =
      CustomPacketPayload.codec(SyncAgeDataPacket::encode, SyncAgeDataPacket::decode);

  public static void encode(SyncAgeDataPacket packet, FriendlyByteBuf buf) {
    buf.writeVarInt(packet.ageUID);
    buf.writeNbt(packet.data);
  }

  public static SyncAgeDataPacket decode(FriendlyByteBuf buf) {
    int ageUID = buf.readVarInt();
    CompoundTag data = buf.readNbt();
    return new SyncAgeDataPacket(ageUID, data);
  }

  @Override
  public CustomPacketPayload.Type<SyncAgeDataPacket> type() {
    return TYPE;
  }

  public static void handle(SyncAgeDataPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {

      ClientAgeDataCache.setAgeData(packet.ageUID, packet.data);

      CompoundTag config = packet.data.contains("AgeConfig") ? packet.data.getCompoundOrEmpty("AgeConfig") : null;
      if (config != null) {
        String[] colorKeys = {"SkyColor", "FogColor", "GrassColors", "FoliageColor", "WaterColor", "CloudColor", "NightSkyColor", "SunsetColor"};
        StringBuilder colorLog = new StringBuilder();
        for (String key : colorKeys) {
          if (config.contains(key)) {
            if (colorLog.length() > 0) colorLog.append(", ");
            colorLog.append(key).append("=0x").append(Integer.toHexString(config.getIntOr(key, 0)));
          }
        }
        Mystcraft.LOGGER.info("[AgeSync Client] Age {} received: {}", packet.ageUID,
            colorLog.length() > 0 ? colorLog.toString() : "no custom colors");
      }
    });
  }

  /**
   * Client-side cache for Age data. Stores synced Age information for rendering
   * and display.
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
        return data.getStringOr("AgeName", "");
      }
      return "Age " + ageUID;
    }

    public static float getInstability(int ageUID) {
      CompoundTag data = CACHE.get(ageUID);
      if (data != null && data.contains("Instability")) {
        return data.getFloatOr("Instability", 0.0F);
      }
      return 0.0f;
    }

    private static CompoundTag getConfig(int ageUID) {
      CompoundTag data = CACHE.get(ageUID);
      if (data != null && data.contains("AgeConfig")) {
        return data.getCompoundOrEmpty("AgeConfig");
      }
      return null;
    }

    public static int getSkyColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("SkyColor") ? config.getIntOr("SkyColor", 0) : -1;
    }

    public static int getFogColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("FogColor") ? config.getIntOr("FogColor", 0) : -1;
    }

    public static int getGrassColor(int ageUID) {
      java.util.List<Integer> colors = getGrassColors(ageUID);
      return colors.isEmpty() ? -1 : colors.get(0);
    }

    public static java.util.List<Integer> getGrassColors(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      if (config == null) return Collections.emptyList();
      if (config.contains("GrassColors")) {
        int[] arr = config.getIntArray("GrassColors").orElseGet(() -> new int[0]);
        java.util.List<Integer> result = new java.util.ArrayList<>(arr.length);
        for (int c : arr) {
          result.add(c);
        }
        return result;
      }
      if (config.contains("GrassColor")) {
        return Collections.singletonList(config.getIntOr("GrassColor", 0));
      }
      return Collections.emptyList();
    }

    public static int getFoliageColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("FoliageColor") ? config.getIntOr("FoliageColor", 0) : -1;
    }

    public static int getWaterColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("WaterColor") ? config.getIntOr("WaterColor", 0) : -1;
    }

    public static int getCloudColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("CloudColor") ? config.getIntOr("CloudColor", 0) : -1;
    }

    public static int getNightSkyColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("NightSkyColor") ? config.getIntOr("NightSkyColor", 0) : -1;
    }

    public static int getHorizonColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("HorizonColor") ? config.getIntOr("HorizonColor", 0) : -1;
    }

    public static String getLightingType(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      if (config != null && config.contains("LightingType")) {
        String type = config.getStringOr("LightingType", "");
        return type.isEmpty() ? "normal" : type;
      }
      return "normal";
    }

    public static boolean isHorizonHidden(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBooleanOr("HorizonHidden", false);
    }

    public static boolean isRainbowEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBooleanOr("RainbowEnabled", false);
    }

    public static boolean areMeteorsEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBooleanOr("MeteorsEnabled", false);
    }

    public static boolean isLightningEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBooleanOr("LightningEnabled", false);
    }

    public static boolean areExplosionsEnabled(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.getBooleanOr("ExplosionsEnabled", false);
    }

    public static int getSunsetColor(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("SunsetColor") ? config.getIntOr("SunsetColor", 0) : -1;
    }

    public static float getCloudHeight(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("CloudHeight") ? config.getFloatOr("CloudHeight", 0.0F) : 192.0f;
    }

    public static float getHorizonHeight(int ageUID) {
      CompoundTag config = getConfig(ageUID);
      return config != null && config.contains("HorizonHeight") ? config.getFloatOr("HorizonHeight", 0.0F) : 0.0f;
    }

  }
}
