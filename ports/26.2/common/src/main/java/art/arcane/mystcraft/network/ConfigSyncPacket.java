package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet sent from server to client to sync server configuration. Ensures
 * clients have consistent gameplay settings with the server.
 */
public record ConfigSyncPacket(CompoundTag configData) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ConfigSyncPacket> TYPE =
      MystcraftNetwork.type("config_sync");
  public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPacket> STREAM_CODEC =
      CustomPacketPayload.codec(ConfigSyncPacket::encode, ConfigSyncPacket::decode);

  public static void encode(ConfigSyncPacket packet, FriendlyByteBuf buf) {
    buf.writeNbt(packet.configData);
  }

  public static ConfigSyncPacket decode(FriendlyByteBuf buf) {
    return new ConfigSyncPacket(buf.readNbt());
  }

  @Override
  public CustomPacketPayload.Type<ConfigSyncPacket> type() {
    return TYPE;
  }

  public static void handle(ConfigSyncPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      if (ClientAccess.getClientLevel() == null) return;

      ClientConfigCache.setConfig(packet.configData);
      Mystcraft.LOGGER.info("Received server config sync with {} keys",
          packet.configData != null ? packet.configData.keySet().size() : 0);
    });
  }

  /**
   * Creates a config sync packet with the current server settings.
   */
  public static ConfigSyncPacket create() {
    CompoundTag config = new CompoundTag();

    config.putBoolean("allowWorldGen", true);
    config.putBoolean("allowInstabilityEffects", true);
    config.putBoolean("allowLinking", true);
    config.putInt("maxAgesPerPlayer", -1);
    config.putInt("maxPagesPerBooster", 3);
    config.putFloat("instabilityMultiplier", 1.0f);

    config.putBoolean("requireInk", true);
    config.putBoolean("consumePages", true);

    config.putBoolean("allowIntraLinking", true);
    config.putBoolean("generatePlatforms", true);
    config.putInt("linkCooldownTicks", 100);

    config.putFloat("decaySpreadRate", 1.0f);
    config.putBoolean("decayAffectsOverworld", false);

    return new ConfigSyncPacket(config);
  }

  /**
   * Client-side cache for server configuration.
   */
  public static class ClientConfigCache {
    private static CompoundTag config = new CompoundTag();

    public static CompoundTag getConfig() {
      return config;
    }

    public static void setConfig(CompoundTag data) {
      config = data != null ? data : new CompoundTag();
    }

    public static void clear() {
      config = new CompoundTag();
    }

    public static boolean allowWorldGen() {
      return config.getBooleanOr("allowWorldGen", false);
    }

    public static boolean allowInstabilityEffects() {
      return !config.contains("allowInstabilityEffects") || config.getBooleanOr("allowInstabilityEffects", false);
    }

    public static boolean allowLinking() {
      return !config.contains("allowLinking") || config.getBooleanOr("allowLinking", false);
    }

    public static int maxAgesPerPlayer() {
      return config.contains("maxAgesPerPlayer") ? config.getIntOr("maxAgesPerPlayer", 0) : -1;
    }

    public static float instabilityMultiplier() {
      return config.contains("instabilityMultiplier") ? config.getFloatOr("instabilityMultiplier", 0.0F) : 1.0f;
    }

    public static boolean requireInk() {
      return !config.contains("requireInk") || config.getBooleanOr("requireInk", false);
    }

    public static boolean consumePages() {
      return !config.contains("consumePages") || config.getBooleanOr("consumePages", false);
    }

    public static int linkCooldownTicks() {
      return config.contains("linkCooldownTicks") ? config.getIntOr("linkCooldownTicks", 0) : 100;
    }

    public static float decaySpreadRate() {
      return config.contains("decaySpreadRate") ? config.getFloatOr("decaySpreadRate", 0.0F) : 1.0f;
    }

    public static boolean decayAffectsOverworld() {
      return config.contains("decayAffectsOverworld") && config.getBooleanOr("decayAffectsOverworld", false);
    }
  }
}
