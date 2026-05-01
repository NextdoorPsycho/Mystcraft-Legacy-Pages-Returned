package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Packet sent from server to client to sync server configuration. Ensures
 * clients have consistent gameplay settings with the server.
 */
public record ConfigSyncPacket(CompoundTag configData) {

  public static void encode(ConfigSyncPacket packet, FriendlyByteBuf buf) {
    buf.writeNbt(packet.configData);
  }

  public static ConfigSyncPacket decode(FriendlyByteBuf buf) {
    return new ConfigSyncPacket(buf.readNbt());
  }

  public static void handle(ConfigSyncPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      if (ClientAccess.getClientLevel() == null) return;

      ClientConfigCache.setConfig(packet.configData);
      Mystcraft.LOGGER.info("Received server config sync with {} keys",
          packet.configData != null ? packet.configData.getAllKeys().size() : 0);
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
      return config.getBoolean("allowWorldGen");
    }

    public static boolean allowInstabilityEffects() {
      return !config.contains("allowInstabilityEffects") || config.getBoolean("allowInstabilityEffects");
    }

    public static boolean allowLinking() {
      return !config.contains("allowLinking") || config.getBoolean("allowLinking");
    }

    public static int maxAgesPerPlayer() {
      return config.contains("maxAgesPerPlayer") ? config.getInt("maxAgesPerPlayer") : -1;
    }

    public static float instabilityMultiplier() {
      return config.contains("instabilityMultiplier") ? config.getFloat("instabilityMultiplier") : 1.0f;
    }

    public static boolean requireInk() {
      return !config.contains("requireInk") || config.getBoolean("requireInk");
    }

    public static boolean consumePages() {
      return !config.contains("consumePages") || config.getBoolean("consumePages");
    }

    public static int linkCooldownTicks() {
      return config.contains("linkCooldownTicks") ? config.getInt("linkCooldownTicks") : 100;
    }

    public static float decaySpreadRate() {
      return config.contains("decaySpreadRate") ? config.getFloat("decaySpreadRate") : 1.0f;
    }

    public static boolean decayAffectsOverworld() {
      return config.contains("decayAffectsOverworld") && config.getBoolean("decayAffectsOverworld");
    }
  }
}
