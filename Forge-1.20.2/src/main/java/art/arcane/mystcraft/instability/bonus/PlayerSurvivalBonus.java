package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.instability.InstabilityBonusManager.IInstabilityBonus;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * Instability bonus that rewards players for surviving in an Age.
 * The longer a player survives in the Age, the more stability bonus they provide.
 * Death resets the bonus to zero.
 *
 * This is the inverse of PlayerKilledBonus - it rewards survival with stability.
 */
public class PlayerSurvivalBonus implements IInstabilityBonus {

    private final String playerName;
    private final UUID playerId;
    private final int dimensionId;

    private final int maxBonus;
    private final float growthRate;
    private float currentBonus;
    private boolean playerInWorld;

    /**
     * Creates a player survival bonus tracker.
     *
     * @param playerId    The player's UUID
     * @param playerName  The player's name
     * @param dimensionId The dimension ID of the Age
     * @param maxBonus    Maximum stability bonus when player survives long enough
     * @param growthRate  How fast the bonus grows per tick while player is in world
     */
    public PlayerSurvivalBonus(UUID playerId, String playerName, int dimensionId, int maxBonus, float growthRate) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.dimensionId = dimensionId;
        this.maxBonus = maxBonus;
        this.growthRate = growthRate;
        this.currentBonus = 0;
        this.playerInWorld = false;

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public String getName() {
        return "Player Survival: " + playerName;
    }

    @Override
    public int getValue() {
        // Return positive value (bonus = reduces instability)
        return (int) currentBonus;
    }

    @Override
    public void tick(ServerLevel level) {
        if (playerInWorld) {
            // Increase bonus while player is in the world
            currentBonus = Math.min(maxBonus, currentBonus + growthRate);
        } else {
            // Slowly decay bonus when player is not present (but slower than growth)
            currentBonus = Math.max(0, currentBonus - (growthRate * 0.1f));
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Check if this is our tracked player
        if (!player.getUUID().equals(playerId)) {
            return;
        }

        // Check if death occurred in our dimension
        if (!AgeDimensionFactory.isMystcraftAge(player.level().dimension())) {
            return;
        }

        int ageId = getAgeUID(player.level());
        if (ageId != dimensionId) {
            return;
        }

        // Reset bonus on death
        currentBonus = 0;
        announceToAge(player.level(), "instability.bonus.survival.death", playerName);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getUUID().equals(playerId)) {
            return;
        }

        if (!AgeDimensionFactory.isMystcraftAge(player.level().dimension())) {
            return;
        }

        int ageId = getAgeUID(player.level());
        if (ageId == dimensionId) {
            playerInWorld = true;
            announceToAge(player.level(), "instability.bonus.survival.enter", playerName);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getUUID().equals(playerId)) {
            return;
        }

        if (AgeDimensionFactory.isMystcraftAge(player.level().dimension())) {
            int ageId = getAgeUID(player.level());
            if (ageId == dimensionId) {
                playerInWorld = false;
                announceToAge(player.level(), "instability.bonus.survival.leave", playerName);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getUUID().equals(playerId)) {
            return;
        }

        // Check if leaving our dimension
        if (AgeDimensionFactory.isMystcraftAge(event.getFrom())) {
            int fromAgeId = getAgeUIDFromKey(event.getFrom());
            if (fromAgeId == dimensionId) {
                playerInWorld = false;
                announceToAge(player.level(), "instability.bonus.survival.leave", playerName);
            }
        }

        // Check if entering our dimension
        if (AgeDimensionFactory.isMystcraftAge(event.getTo())) {
            int toAgeId = getAgeUIDFromKey(event.getTo());
            if (toAgeId == dimensionId) {
                playerInWorld = true;
                announceToAge(player.level(), "instability.bonus.survival.enter", playerName);
            }
        }
    }

    /**
     * Gets the Age UID from a level.
     */
    private int getAgeUID(net.minecraft.world.level.Level level) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return -1;
        return AgeManager.get(server).getAgeUID(level.dimension());
    }

    /**
     * Gets the Age UID from a dimension key.
     */
    private int getAgeUIDFromKey(ResourceKey<Level> key) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return -1;
        return AgeManager.get(server).getAgeUID(key);
    }

    private void announceToAge(net.minecraft.world.level.Level level, String key, Object... args) {
        if (level instanceof ServerLevel serverLevel) {
            Component message = Component.translatable(key, args);
            for (ServerPlayer player : serverLevel.players()) {
                player.sendSystemMessage(message);
            }
        }
    }

    /**
     * Checks if the tracked player is currently in the world.
     */
    public boolean isPlayerInWorld() {
        return playerInWorld;
    }

    /**
     * Unregisters event handlers.
     */
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
