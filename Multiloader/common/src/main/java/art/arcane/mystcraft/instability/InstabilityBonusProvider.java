package art.arcane.mystcraft.instability;

import net.minecraft.server.level.ServerPlayer;

/**
 * Legacy per-player instability bonus interface used by platform code.
 */
public interface InstabilityBonusProvider {
  float getBonus(ServerPlayer player);

  void reset(ServerPlayer player);
}
