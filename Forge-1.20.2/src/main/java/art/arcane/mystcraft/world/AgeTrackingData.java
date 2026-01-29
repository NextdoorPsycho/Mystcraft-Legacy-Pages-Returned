package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.datafix.DataFixTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent access and performance tracking for Mystcraft Ages.
 */
public class AgeTrackingData extends SavedData {
    private static final String DATA_NAME = Mystcraft.MOD_ID + "_age_tracking";

    private static final String TAG_AGES = "Ages";
    private static final String TAG_UID = "UID";
    private static final String TAG_CREATED_AT = "CreatedAt";
    private static final String TAG_LAST_ACCESS = "LastAccess";
    private static final String TAG_TOTAL_ACCESS = "TotalAccess";
    private static final String TAG_TOTAL_TICK_NANOS = "TotalTickNanos";
    private static final String TAG_TICK_COUNT = "TickCount";
    private static final String TAG_MAX_TICK_NANOS = "MaxTickNanos";

    private final Map<Integer, AgeStats> statsByUid = new HashMap<>();

    public static SavedData.Factory<AgeTrackingData> factory() {
        return new SavedData.Factory<>(AgeTrackingData::new, AgeTrackingData::load, DataFixTypes.LEVEL);
    }

    public static AgeTrackingData load(CompoundTag tag) {
        AgeTrackingData data = new AgeTrackingData();
        data.loadFromTag(tag);
        return data;
    }

    private void loadFromTag(CompoundTag tag) {
        statsByUid.clear();
        ListTag agesList = tag.getList(TAG_AGES, Tag.TAG_COMPOUND);
        for (int i = 0; i < agesList.size(); i++) {
            CompoundTag ageTag = agesList.getCompound(i);
            int uid = ageTag.getInt(TAG_UID);
            if (uid <= 0) continue;

            AgeStats stats = new AgeStats();
            stats.createdAtMillis = ageTag.getLong(TAG_CREATED_AT);
            stats.lastAccessMillis = ageTag.getLong(TAG_LAST_ACCESS);
            stats.totalAccessMillis = ageTag.getLong(TAG_TOTAL_ACCESS);
            stats.totalTickNanos = ageTag.getLong(TAG_TOTAL_TICK_NANOS);
            stats.tickCount = ageTag.getLong(TAG_TICK_COUNT);
            stats.maxTickNanos = ageTag.getLong(TAG_MAX_TICK_NANOS);
            statsByUid.put(uid, stats);
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag agesList = new ListTag();
        for (Map.Entry<Integer, AgeStats> entry : statsByUid.entrySet()) {
            CompoundTag ageTag = new CompoundTag();
            AgeStats stats = entry.getValue();
            ageTag.putInt(TAG_UID, entry.getKey());
            ageTag.putLong(TAG_CREATED_AT, stats.createdAtMillis);
            ageTag.putLong(TAG_LAST_ACCESS, stats.lastAccessMillis);
            ageTag.putLong(TAG_TOTAL_ACCESS, stats.totalAccessMillis);
            ageTag.putLong(TAG_TOTAL_TICK_NANOS, stats.totalTickNanos);
            ageTag.putLong(TAG_TICK_COUNT, stats.tickCount);
            ageTag.putLong(TAG_MAX_TICK_NANOS, stats.maxTickNanos);
            agesList.add(ageTag);
        }
        tag.put(TAG_AGES, agesList);
        return tag;
    }

    public static AgeTrackingData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not available");
        }
        return overworld.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    public void ensureCreated(int uid, long nowMillis) {
        AgeStats stats = statsByUid.computeIfAbsent(uid, k -> new AgeStats());
        if (stats.createdAtMillis <= 0L) {
            stats.createdAtMillis = nowMillis;
            setDirty();
        }
    }

    public void recordAccessDuration(int uid, long durationMillis, long accessTimeMillis) {
        AgeStats stats = statsByUid.computeIfAbsent(uid, k -> new AgeStats());
        if (stats.createdAtMillis <= 0L) {
            stats.createdAtMillis = accessTimeMillis;
        }
        if (durationMillis > 0L) {
            stats.totalAccessMillis += durationMillis;
        }
        stats.lastAccessMillis = Math.max(stats.lastAccessMillis, accessTimeMillis);
        setDirty();
    }

    public void recordTickDuration(int uid, long nanos) {
        AgeStats stats = statsByUid.computeIfAbsent(uid, k -> new AgeStats());
        if (stats.createdAtMillis <= 0L) {
            stats.createdAtMillis = System.currentTimeMillis();
        }
        stats.totalTickNanos += nanos;
        stats.tickCount++;
        if (nanos > stats.maxTickNanos) {
            stats.maxTickNanos = nanos;
        }
        setDirty();
    }

    public AgeStats getStats(int uid) {
        return statsByUid.get(uid);
    }

    public void removeAge(int uid) {
        if (statsByUid.remove(uid) != null) {
            setDirty();
        }
    }

    public List<AgePerformanceEntry> getWorstByAverageTick(int limit) {
        if (limit <= 0) return List.of();
        List<AgePerformanceEntry> entries = new ArrayList<>();
        for (Map.Entry<Integer, AgeStats> entry : statsByUid.entrySet()) {
            AgeStats stats = entry.getValue();
            if (stats.tickCount <= 0L) continue;
            double avgNanos = (double) stats.totalTickNanos / (double) stats.tickCount;
            entries.add(new AgePerformanceEntry(entry.getKey(), avgNanos, stats.tickCount, stats.maxTickNanos));
        }
        entries.sort(Comparator.comparingDouble(AgePerformanceEntry::avgTickNanos).reversed());
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    public static final class AgeStats {
        private long createdAtMillis;
        private long lastAccessMillis;
        private long totalAccessMillis;
        private long totalTickNanos;
        private long tickCount;
        private long maxTickNanos;

        public long getCreatedAtMillis() {
            return createdAtMillis;
        }

        public long getLastAccessMillis() {
            return lastAccessMillis;
        }

        public long getTotalAccessMillis() {
            return totalAccessMillis;
        }

        public long getTotalTickNanos() {
            return totalTickNanos;
        }

        public long getTickCount() {
            return tickCount;
        }

        public long getMaxTickNanos() {
            return maxTickNanos;
        }
    }

    public record AgePerformanceEntry(int uid, double avgTickNanos, long tickCount, long maxTickNanos) {
    }
}
