package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.instability.InstabilityBonusProvider;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks player kills to provide instability reduction bonuses (1.20.1). */
public class FabricPlayerKilledBonus implements InstabilityBonusProvider {
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
