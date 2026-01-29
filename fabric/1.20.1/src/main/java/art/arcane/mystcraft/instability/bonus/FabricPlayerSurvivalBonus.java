package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.instability.InstabilityBonusProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks player survival time to provide instability reduction bonuses (1.20.1). */
public class FabricPlayerSurvivalBonus implements InstabilityBonusProvider {
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
