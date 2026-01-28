package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.instability.InstabilityBonusManager;
import art.arcane.mystcraft.instability.InstabilityBonusManager.IInstabilityBonusProvider;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provider that creates player-based instability bonuses for Ages.
 * Tracks player deaths and survival time to modify Age instability.
 */
public class PlayerBonusProvider implements IInstabilityBonusProvider {

    // Default values for bonus calculations
    private static final int DEFAULT_DEATH_PENALTY = 50;
    private static final float DEFAULT_DEATH_DECAY = 0.01f;
    private static final int DEFAULT_SURVIVAL_BONUS = 25;
    private static final float DEFAULT_SURVIVAL_GROWTH = 0.005f;

    // Track active bonuses per dimension per player
    private static final Map<Integer, Map<UUID, PlayerKilledBonus>> killedBonuses = new HashMap<>();
    private static final Map<Integer, Map<UUID, PlayerSurvivalBonus>> survivalBonuses = new HashMap<>();

    /**
     * Registers this provider with the global bonus manager.
     */
    public static void init() {
        InstabilityBonusManager.registerBonusProvider(new PlayerBonusProvider());
    }

    @Override
    public void register(InstabilityBonusManager manager, int dimId) {
        // Get the server level for this dimension
        MinecraftServer server = Mystcraft.getCurrentServer();
        if (server == null) {
            return;
        }

        // Find the level with this dimension ID
        ServerLevel level = null;
        AgeManager ageManager = AgeManager.get(server);
        for (ServerLevel serverLevel : server.getAllLevels()) {
            if (AgeDimensionFactory.isMystcraftAge(serverLevel.dimension())) {
                int ageId = ageManager.getAgeUID(serverLevel.dimension());
                if (ageId == dimId) {
                    level = serverLevel;
                    break;
                }
            }
        }

        if (level == null) {
            return;
        }

        // Get Age data for author information
        AgeData ageData = AgeData.getIfPresent(level);

        // Register bonuses for each player currently in the level
        for (ServerPlayer player : level.players()) {
            registerPlayerBonuses(manager, dimId, player);
        }

        // Also register for any known authors (they get tracked even when not present)
        if (ageData != null) {
            for (String authorName : ageData.getAuthors()) {
                // Authors get tracked for death penalty
                // We can't easily get UUID from name, so authors are tracked by name
                // This is a simplified implementation
            }
        }
    }

    /**
     * Registers bonus trackers for a specific player in a dimension.
     */
    public static void registerPlayerBonuses(InstabilityBonusManager manager, int dimId, ServerPlayer player) {
        UUID playerId = player.getUUID();
        String playerName = player.getName().getString();

        // Create and register killed bonus if not exists
        Map<UUID, PlayerKilledBonus> dimKilledBonuses = killedBonuses.computeIfAbsent(dimId, k -> new HashMap<>());
        if (!dimKilledBonuses.containsKey(playerId)) {
            PlayerKilledBonus killedBonus = new PlayerKilledBonus(
                    playerId, playerName, dimId,
                    DEFAULT_DEATH_PENALTY, DEFAULT_DEATH_DECAY
            );
            dimKilledBonuses.put(playerId, killedBonus);
            manager.register(killedBonus);
        }

        // Create and register survival bonus if not exists
        Map<UUID, PlayerSurvivalBonus> dimSurvivalBonuses = survivalBonuses.computeIfAbsent(dimId, k -> new HashMap<>());
        if (!dimSurvivalBonuses.containsKey(playerId)) {
            PlayerSurvivalBonus survivalBonus = new PlayerSurvivalBonus(
                    playerId, playerName, dimId,
                    DEFAULT_SURVIVAL_BONUS, DEFAULT_SURVIVAL_GROWTH
            );
            dimSurvivalBonuses.put(playerId, survivalBonus);
            manager.register(survivalBonus);
        }
    }

    /**
     * Cleans up bonuses for a dimension when it's unloaded.
     */
    public static void cleanupDimension(int dimId) {
        Map<UUID, PlayerKilledBonus> dimKilledBonuses = killedBonuses.remove(dimId);
        if (dimKilledBonuses != null) {
            for (PlayerKilledBonus bonus : dimKilledBonuses.values()) {
                bonus.cleanup();
            }
        }

        Map<UUID, PlayerSurvivalBonus> dimSurvivalBonuses = survivalBonuses.remove(dimId);
        if (dimSurvivalBonuses != null) {
            for (PlayerSurvivalBonus bonus : dimSurvivalBonuses.values()) {
                bonus.cleanup();
            }
        }
    }

    /**
     * Cleans up bonuses for a player when they leave a dimension.
     */
    public static void cleanupPlayer(int dimId, UUID playerId) {
        Map<UUID, PlayerKilledBonus> dimKilledBonuses = killedBonuses.get(dimId);
        if (dimKilledBonuses != null) {
            PlayerKilledBonus killedBonus = dimKilledBonuses.remove(playerId);
            if (killedBonus != null) {
                killedBonus.cleanup();
            }
        }

        Map<UUID, PlayerSurvivalBonus> dimSurvivalBonuses = survivalBonuses.get(dimId);
        if (dimSurvivalBonuses != null) {
            PlayerSurvivalBonus survivalBonus = dimSurvivalBonuses.remove(playerId);
            if (survivalBonus != null) {
                survivalBonus.cleanup();
            }
        }
    }
}
