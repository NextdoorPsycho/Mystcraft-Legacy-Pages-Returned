package art.arcane.mystcraft.platform.services;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Abstracts platform-specific networking operations. Implementations handle
 * SimpleChannel (Forge), custom payloads (Fabric), etc.
 */
public interface INetworkHelper {

  /**
   * Registers all network channels and packet handlers.
   */
  void register();

  /**
   * Sends a packet from client to server.
   */
  void sendToServer(Object packet);

  /**
   * Sends a packet from server to a specific player.
   */
  void sendToPlayer(ServerPlayer player, Object packet);

  /**
   * Sends a packet to all players tracking the given entity.
   */
  void sendToAllTracking(Entity entity, Object packet);

  /**
   * Sends a packet to all connected players.
   */
  void sendToAll(Object packet);
}
