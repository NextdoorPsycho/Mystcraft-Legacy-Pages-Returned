package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.AgePerformanceTracker;
import art.arcane.mystcraft.world.AgeTrackingData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class AgeLifecycleHandler {
    private static final Map<UUID, AccessSession> accessSessions = new ConcurrentHashMap<>();
    private static int cleanupTickCounter = 0;

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ResourceKey<Level> from = event.getFrom();
        ResourceKey<Level> to = player.serverLevel().dimension();

        if (AgeDimensionFactory.isMystcraftAge(from)) {
            endAccessSession(player.getUUID());
        }

        if (AgeDimensionFactory.isMystcraftAge(to)) {
            startAccessSession(player, to);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceKey<Level> levelKey = player.serverLevel().dimension();
        if (AgeDimensionFactory.isMystcraftAge(levelKey)) {
            startAccessSession(player, levelKey);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        endAccessSession(player.getUUID());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level)) return;
        if (event.phase == TickEvent.Phase.START) {
            AgePerformanceTracker.onLevelTickStart(level);
        } else {
            AgePerformanceTracker.onLevelTickEnd(level);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!MystcraftConfig.cleanupEnabled.get()) return;

        int interval = Math.max(1, MystcraftConfig.ageCleanupCheckIntervalTicks.get());
        cleanupTickCounter++;
        if (cleanupTickCounter < interval) return;
        cleanupTickCounter = 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        runCleanup(server);
    }

    private static void startAccessSession(ServerPlayer player, ResourceKey<Level> levelKey) {
        AgeManager ageManager = AgeManager.get(player.serverLevel());
        int uid = ageManager.getAgeUID(levelKey);
        if (uid <= 0) return;

        long now = System.currentTimeMillis();
        AgeTrackingData tracking = AgeTrackingData.get(player.getServer());
        tracking.ensureCreated(uid, now);

        accessSessions.put(player.getUUID(), new AccessSession(uid, now));
    }

    private static void endAccessSession(UUID playerId) {
        AccessSession session = accessSessions.remove(playerId);
        if (session == null) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long now = System.currentTimeMillis();
        long duration = Math.max(0L, now - session.startMillis());
        AgeTrackingData tracking = AgeTrackingData.get(server);
        tracking.recordAccessDuration(session.uid(), duration, now);
    }

    private static void runCleanup(MinecraftServer server) {
        AgeManager ageManager = AgeManager.get(server);
        AgeTrackingData tracking = AgeTrackingData.get(server);

        long now = System.currentTimeMillis();
        long graceMillis = MystcraftConfig.ageCleanupGraceMinutes.get() * 60_000L;
        long minAccessMillis = MystcraftConfig.ageCleanupMinAccessMinutes.get() * 60_000L;

        var uids = new ArrayList<Integer>();
        for (int uid : ageManager.getAllAgeUIDs()) {
            uids.add(uid);
        }

        for (int uid : uids) {
            AgeTrackingData.AgeStats stats = tracking.getStats(uid);
            if (stats == null || stats.getCreatedAtMillis() <= 0L) {
                continue;
            }

            if (now - stats.getCreatedAtMillis() < graceMillis) continue;
            if (stats.getTotalAccessMillis() >= minAccessMillis) continue;

            if (deleteAge(server, ageManager, tracking, uid)) {
                Mystcraft.LOGGER.info("[AgeCleanup] Deleted Age {} (access {}ms within {}ms grace)",
                        uid, stats.getTotalAccessMillis(), graceMillis);
            }
        }
    }

    private static boolean deleteAge(MinecraftServer server, AgeManager ageManager, AgeTrackingData tracking, int uid) {
        ResourceLocation dimension = ageManager.getDimension(uid);
        if (dimension == null) {
            tracking.removeAge(uid);
            return false;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension);
        ServerLevel level = server.getLevel(dimensionKey);
        if (level != null && !level.players().isEmpty()) {
            return false;
        }

        if (level != null) {
            boolean unloaded = AgeDimensionFactory.unloadAgeDimension(server, dimensionKey);
            if (!unloaded) return false;
        }

        ageManager.removeAge(uid);
        tracking.removeAge(uid);
        deleteAgeDirectory(server, uid);
        return true;
    }

    private static void deleteAgeDirectory(MinecraftServer server, int uid) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path ageDir = worldDir.resolve("dimensions").resolve(Mystcraft.MOD_ID).resolve("mystcraft_age_" + uid);
        if (!Files.isDirectory(ageDir)) return;

        try (Stream<Path> walk = Files.walk(ageDir)) {
            walk.sorted((a, b) -> b.compareTo(a))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            Mystcraft.LOGGER.error("[AgeCleanup] Failed to delete age directory {}", ageDir, e);
        }
    }

    private record AccessSession(int uid, long startMillis) {
    }
}
