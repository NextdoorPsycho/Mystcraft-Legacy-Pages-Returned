package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks per-Age tick durations and aggregates them into persistent stats.
 */
public final class AgePerformanceTracker {
    private static final Map<Integer, Long> tickStartNanos = new HashMap<>();

    private AgePerformanceTracker() {
    }

    public static void onLevelTickStart(ServerLevel level) {
        if (!MystcraftConfig.trackAgePerformance.get()) return;
        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) return;

        Integer uid = getAgeUid(level);
        if (uid == null || uid <= 0) return;
        tickStartNanos.put(uid, System.nanoTime());
    }

    public static void onLevelTickEnd(ServerLevel level) {
        if (!MystcraftConfig.trackAgePerformance.get()) return;
        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) return;

        Integer uid = getAgeUid(level);
        if (uid == null || uid <= 0) return;

        Long start = tickStartNanos.remove(uid);
        if (start == null) return;

        long elapsed = System.nanoTime() - start;
        AgeTrackingData tracking = AgeTrackingData.get(level.getServer());
        tracking.recordTickDuration(uid, elapsed);
    }

    @Nullable
    private static Integer getAgeUid(ServerLevel level) {
        AgeData ageData = AgeData.getIfPresent(level);
        if (ageData != null && ageData.getAgeUID() > 0) {
            return ageData.getAgeUID();
        }
        return AgeManager.get(level).getAgeUID(level.dimension());
    }

    public static void logWorstAges(MinecraftServer server) {
        if (!MystcraftConfig.trackAgePerformance.get()) return;
        int limit = MystcraftConfig.performanceReportLimit.get();
        if (limit <= 0) return;

        AgeTrackingData tracking = AgeTrackingData.get(server);
        var worst = tracking.getWorstByAverageTick(limit);
        if (worst.isEmpty()) {
            Mystcraft.LOGGER.info("[AgePerf] No age performance data available");
            return;
        }

        Mystcraft.LOGGER.info("[AgePerf] Worst Ages by average tick time (ns):");
        for (var entry : worst) {
            Mystcraft.LOGGER.info(
                    "[AgePerf] Age {} avg={}ns max={}ns ticks={}",
                    entry.uid(),
                    (long) entry.avgTickNanos(),
                    entry.maxTickNanos(),
                    entry.tickCount()
            );
        }
    }
}
