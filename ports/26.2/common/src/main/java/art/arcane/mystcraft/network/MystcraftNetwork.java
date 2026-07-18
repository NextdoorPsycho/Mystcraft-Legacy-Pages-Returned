package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Common network abstraction for Mystcraft packets. Platform modules set the
 * delegate functions during initialization.
 */
public final class MystcraftNetwork {

  public static Consumer<CustomPacketPayload> sendToServerHandler;
  public static BiConsumer<CustomPacketPayload, ServerPlayer> sendToPlayerHandler;
  public static Consumer<CustomPacketPayload> sendToAllHandler;
  public static BiConsumer<CustomPacketPayload, ServerPlayer> sendToTrackingHandler;
  public static TrackingBlockSender sendToTrackingBlockHandler;

  private MystcraftNetwork() {
  }

  /**
   * Sends a packet to the server (client -> server).
   */
  public static void sendToServer(CustomPacketPayload packet) {
    sendToServerHandler.accept(packet);
  }

  /**
   * Sends a packet to a specific player (server -> client).
   */
  public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
    if (player == null || player.connection == null || sendToPlayerHandler == null) {
      return;
    }
    sendToPlayerHandler.accept(packet, player);
  }

  /**
   * Sends a packet to all connected players (server -> client).
   */
  public static void sendToAll(CustomPacketPayload packet) {
    sendToAllHandler.accept(packet);
  }

  /**
   * Sends a packet to all players tracking the given player (server ->
   * client).
   */
  public static void sendToTracking(CustomPacketPayload packet, ServerPlayer player) {
    sendToTrackingHandler.accept(packet, player);
  }

  /**
   * Sends a packet to all players tracking a block position (server ->
   * client).
   */
  public static void sendToTrackingBlock(CustomPacketPayload packet, ServerLevel level, BlockPos pos) {
    if (sendToTrackingBlockHandler != null) {
      sendToTrackingBlockHandler.send(packet, level, pos);
    }
  }

  /**
   * Platform-specific handler for sending packets to players tracking a block
   * position. BiConsumer takes (packet, level, pos).
   */
  @FunctionalInterface
  public interface TrackingBlockSender {
    void send(CustomPacketPayload packet, ServerLevel level, BlockPos pos);
  }

  static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    return new CustomPacketPayload.Type<>(id);
  }
}
