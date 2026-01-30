package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.instability.InstabilityBonusProvider;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consolidated Fabric instability bonuses for Mystcraft 1.20.2.
 * Combines FabricPlayerKilledBonus and FabricPlayerSurvivalBonus.
 */
public final class FabricInstabilityBonuses {

  private FabricInstabilityBonuses() {
  }

  // ========== PLAYER KILLED BONUS ==========
  public static class PlayerKilledBonus implements InstabilityBonusProvider {
    private final Map<UUID, Integer> killCounts = new ConcurrentHashMap<>();

    public void register() {
      ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
        if (source.getEntity() instanceof ServerPlayer player) {
          killCounts.merge(player.getUUID(), 1, Integer::sum);
        }
      });
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
