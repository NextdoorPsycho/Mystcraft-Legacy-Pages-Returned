package art.arcane.mystcraft.platform.services;

import net.minecraft.server.level.ServerPlayer;

/**
 * Factory for creating advancement criterion triggers for the supported 1.20.1
 * API.
 */
public interface IAdvancementTriggerFactory {

  /**
   * Registers all advancement triggers with the vanilla CriteriaTriggers
   * registry. Uses version-appropriate registration method.
   */
  void registerTriggers();

  /**
   * Triggers the safe-entry criterion for a player who carried a return book.
   */
  void triggerEnterMystDimensionSafe(ServerPlayer player);

  /**
   * Triggers the Quinn-entry criterion for a player without a return book.
   */
  void triggerEnterMystDimensionQuinn(ServerPlayer player);

  /**
   * Triggers the writing-desk criterion after a successful page write.
   */
  void triggerWritingDeskWrite(ServerPlayer player);
}
