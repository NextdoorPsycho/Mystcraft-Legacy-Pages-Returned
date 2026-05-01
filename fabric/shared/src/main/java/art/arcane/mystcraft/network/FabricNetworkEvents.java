package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric network event handlers. Syncs data to players on login, equivalent to
 * Forge's NetworkEvents.
 */
public final class FabricNetworkEvents {

  private FabricNetworkEvents() {
  }

  /**
   * Registers network-related event callbacks. Call from
   * MystcraftFabric.onInitialize().
   */
  public static void register() {
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayer player = handler.getPlayer();

      FabricMystcraftNetwork.sendToPlayer(new SymbolSyncPacket(), player);
      Mystcraft.LOGGER.debug("[FabricNetworkEvents] Sent symbol sync packet to player {}", player.getName().getString());
    });
  }
}
