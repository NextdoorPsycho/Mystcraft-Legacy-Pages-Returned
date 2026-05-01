package art.arcane.mystcraft.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Small helper that wraps {@link Player#displayClientMessage} with the safety
 * guards needed for synthetic players (e.g. GameTest mock players that have no
 * network connection). Delegates straight through for real clients.
 */
public final class PlayerMessages {

  private PlayerMessages() {
  }

  /**
   * Safely sends a client message. No-op if the player is null, the player is a
   * {@link ServerPlayer} with a null connection (synthetic / mocked), or if any
   * unexpected exception fires inside the dispatch (preserves the calling code
   * path against partially-initialised players).
   */
  public static void send(Player player, Component message, boolean actionBar) {
    if (player == null || message == null) {
      return;
    }
    if (player instanceof ServerPlayer sp && sp.connection == null) {
      return;
    }
    try {
      player.displayClientMessage(message, actionBar);
    } catch (NullPointerException ignored) {

    }
  }
}
