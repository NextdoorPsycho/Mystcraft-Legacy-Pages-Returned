package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.instability.InstabilityBonusProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consolidated Fabric instability bonuses for Mystcraft 1.18.2.
 * Combines FabricPlayerKilledBonus and FabricPlayerSurvivalBonus.
 * Note: ServerLivingEntityEvents.AFTER_DEATH doesn't exist in 1.18.2 Fabric API,
 * so kill tracking is not implemented in this version.
 */
public final class FabricInstabilityBonuses {

  private FabricInstabilityBonuses() {
  }

  // ========== PLAYER KILLED BONUS ==========
  public static class PlayerKilledBonus implements InstabilityBonusProvider {
    private final Map<UUID, Integer> killCounts = new ConcurrentHashMap<>();

    public void register() {
      // Note: ServerLivingEntityEvents.AFTER_DEATH doesn't exist in 1.18.2 Fabric API
      // Kill tracking would need to be implemented via mixins or other means
    }

    public void onPlayerKill(ServerPlayer player) {
      // Called manually if needed
      killCounts.merge(player.getUUID(), 1, Integer::sum);
    }

    @Override
    public float getBonus(ServerPlayer player) {
      int kills = killCounts.getOrDefault(player.getUUID(), 0);
      return -kills * 0.5f;
    }

    @Override
    public void reset(ServerPlayer player) {
      killCounts.remove(player.getUUID());
    }
  }

  // ========== PLAYER SURVIVAL BONUS ==========
  public static class PlayerSurvivalBonus implements InstabilityBonusProvider {
    private final Map<UUID, Long> survivalStartTimes = new ConcurrentHashMap<>();

    public void onPlayerJoin(ServerPlayer player) {
      survivalStartTimes.putIfAbsent(player.getUUID(), System.currentTimeMillis());
    }

    public void onPlayerDeath(ServerPlayer player) {
      survivalStartTimes.remove(player.getUUID());
    }

    @Override
    public float getBonus(ServerPlayer player) {
      Long startTime = survivalStartTimes.get(player.getUUID());
      if (startTime == null) return 0;
      long minutes = (System.currentTimeMillis() - startTime) / 60000;
      return -minutes * 0.1f;
    }

    @Override
    public void reset(ServerPlayer player) {
      survivalStartTimes.remove(player.getUUID());
    }
  }
}
