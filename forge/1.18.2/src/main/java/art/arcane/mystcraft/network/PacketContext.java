package art.arcane.mystcraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Common interface for packet handling context.
 * 1.18.2 Forge version.
 */
public interface PacketContext {

  /**
   * Gets the player associated with this packet.
   * For server-side handling, this is the sender.
   * For client-side handling, this is the local player.
   */
  @Nullable
  Player getPlayer();

  /**
   * Gets the server player if available.
   * Returns null on client side.
   */
  @Nullable
  ServerPlayer getServerPlayer();

  /**
   * Returns true if this packet is being handled on the client side.
   */
  boolean isClientSide();

  /**
   * Enqueues work to be executed on the main thread.
   */
  void enqueueWork(Runnable work);
}
