package art.arcane.mystcraft.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Common network abstraction for Mystcraft packets.
 * Platform modules set the delegate functions during initialization.
 */
public final class MystcraftNetwork {

  public static Consumer<Object> sendToServerHandler;
  public static BiConsumer<Object, ServerPlayer> sendToPlayerHandler;
  public static Consumer<Object> sendToAllHandler;
  public static BiConsumer<Object, ServerPlayer> sendToTrackingHandler;

  private MystcraftNetwork() {
  }

  /**
   * Sends a packet to the server (client -> server).
   */
  public static void sendToServer(Object packet) {
    sendToServerHandler.accept(packet);
  }

  /**
   * Sends a packet to a specific player (server -> client).
   */
  public static void sendToPlayer(Object packet, ServerPlayer player) {
    sendToPlayerHandler.accept(packet, player);
  }

  /**
   * Sends a packet to all connected players (server -> client).
   */
  public static void sendToAll(Object packet) {
    sendToAllHandler.accept(packet);
  }

  /**
   * Sends a packet to all players tracking the given player (server -> client).
   */
  public static void sendToTracking(Object packet, ServerPlayer player) {
    sendToTrackingHandler.accept(packet, player);
  }
}
