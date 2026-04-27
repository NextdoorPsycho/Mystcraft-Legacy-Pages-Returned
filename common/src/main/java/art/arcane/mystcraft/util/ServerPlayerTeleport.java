package art.arcane.mystcraft.util;

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-player teleport helpers that also work with GameTest mock players.
 */
public final class ServerPlayerTeleport {

  private ServerPlayerTeleport() {
  }

  public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z,
                              float yaw, float pitch) {
    if (player.connection != null) {
      player.teleportTo(level, x, y, z, yaw, pitch);
      return;
    }

    // GameTest mock players have no network connection. Move their server-side
    // state directly so production travel handlers can still be tested.
    player.setServerLevel(level);
    player.gameMode.setLevel(level);
    player.moveTo(x, y, z, yaw, pitch);
  }

  public static void syncAbilitiesIfConnected(ServerPlayer player) {
    if (player.connection != null) {
      player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }
  }
}
