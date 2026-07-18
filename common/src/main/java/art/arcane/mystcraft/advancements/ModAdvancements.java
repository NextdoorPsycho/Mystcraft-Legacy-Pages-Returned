package art.arcane.mystcraft.advancements;

import art.arcane.mystcraft.platform.Services;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers and dispatches Mystcraft custom advancement criteria through the
 * loader-specific service implementation.
 */
public final class ModAdvancements {

  private ModAdvancements() {
  }

  /**
   * Registers all custom criteria triggers with the vanilla registry. Delegates
   * to version-specific factory loaded via ServiceLoader. Must be called during
   * common setup.
   */
  public static void register() {
    Services.ADVANCEMENTS.registerTriggers();
  }

  /**
   * Triggers the WritingDeskWrite advancement for a player.
   */
  public static void triggerWritingDeskWrite(ServerPlayer player) {
    Services.ADVANCEMENTS.triggerWritingDeskWrite(player);
  }

  /**
   * Triggers the EnterMystDimensionSafe advancement for a player.
   */
  public static void triggerEnterMystDimensionSafe(ServerPlayer player) {
    Services.ADVANCEMENTS.triggerEnterMystDimensionSafe(player);
  }

  /**
   * Triggers the EnterMystDimensionQuinn advancement for a player.
   */
  public static void triggerEnterMystDimensionQuinn(ServerPlayer player) {
    Services.ADVANCEMENTS.triggerEnterMystDimensionQuinn(player);
  }
}
