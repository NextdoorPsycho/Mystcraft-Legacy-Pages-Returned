package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.instability.InstabilityBonusManager.IInstabilityBonus;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * Instability bonus that increases instability when a player dies in an Age.
 * Death caused by another player gives full penalty, other deaths give half.
 * The penalty decays over time.
 */
public class PlayerKilledBonus implements IInstabilityBonus {

    private final String playerName;
    private final UUID playerId;
    private final int dimensionId;

    private final int maxPenalty;
    private final float decayRate;
    private float currentPenalty;

    /**
     * Creates a player killed bonus tracker.
     *
     * @param playerId    The player's UUID
     * @param playerName  The player's name
     * @param dimensionId The dimension ID of the Age
     * @param maxPenalty  Maximum penalty when player is killed
     * @param decayRate   How fast the penalty decays per tick
     */
    public PlayerKilledBonus(UUID playerId, String playerName, int dimensionId, int maxPenalty, float decayRate) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.dimensionId = dimensionId;
        this.maxPenalty = maxPenalty;
        this.decayRate = decayRate;
        this.currentPenalty = 0;

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public String getName() {
        return "Player Killed: " + playerName;
    }

    @Override
    public int getValue() {
        // Return negative value (penalty = increases instability)
        return -(int) currentPenalty;
    }

    @Override
    public void tick(ServerLevel level) {
        // Decay the penalty over time
        if (currentPenalty > 0) {
            currentPenalty = Math.max(0, currentPenalty - decayRate);
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

        // Check if killed by another player (PvP death)
        if (event.getSource().getEntity() instanceof Player killer && !killer.getUUID().equals(playerId)) {
            // Full penalty for PvP death
            currentPenalty = maxPenalty;
            announceToAge(player.level(), "instability.bonus.death",
                    playerName, killer.getName().getString());
        } else {
            // Half penalty for other deaths
            currentPenalty = Math.max(currentPenalty, maxPenalty / 2.0f);
            announceToAge(player.level(), "instability.bonus.death.partial", playerName);
        }
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
        if (ageId == dimensionId && currentPenalty > 0) {
            player.sendSystemMessage(Component.translatable("instability.bonus.death.alert", playerName));
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

        if (!AgeDimensionFactory.isMystcraftAge(event.getTo())) {
            return;
        }

        int ageId = getAgeUIDFromKey(event.getTo());
        if (ageId == dimensionId && currentPenalty > 0) {
            player.sendSystemMessage(Component.translatable("instability.bonus.death.alert", playerName));
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
    private int getAgeUIDFromKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
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
     * Unregisters event handlers.
     */
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
