package art.arcane.mystcraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Platform-agnostic packet handling context.
 */
public interface PacketContext {
  /**
   * Returns the player that sent/received the packet. May be null on server for client-sent packets.
   */
  @Nullable
  Player getPlayer();

  /**
   * Returns the server player, or null if this is a client-side context.
   */
  @Nullable
  ServerPlayer getServerPlayer();

  /**
   * Returns true if this packet is being handled on the client side.
   */
  boolean isClientSide();

  /**
   * Enqueues work on the main thread.
   */
  void enqueueWork(Runnable work);
}
