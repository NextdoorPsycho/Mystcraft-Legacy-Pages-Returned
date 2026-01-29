package art.arcane.mystcraft.event;

import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.event.PersonalPocketEscapeHandler;
import art.arcane.mystcraft.event.AgeReturnHandler;
import art.arcane.mystcraft.instability.InstabilityManager;
import art.arcane.mystcraft.network.FabricNetworkEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Centralizes all Fabric event callback registration (1.20.1).
 * Delegates to common handler classes. Called from MystcraftFabric.onInitialize().
 */
public final class FabricEventRegistration {

    private FabricEventRegistration() {}

    /** Registers all event callbacks for the Fabric platform. */
    public static void registerAll() {

        // World tick: instability and age effects processing
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            InstabilityManager.onLevelTick(level);
            AgeEffectsHandler.onLevelTick(level);
        });

        // Command registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MystcraftCommands.registerCommands(dispatcher);
        });

        // Living entity death: age death effects
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer player) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    if (PersonalPocketEscapeHandler.handleDeath(player, damageSource)) {
                        return false;
                    }
                    if (AgeReturnHandler.handleDeath(player, damageSource)) {
                        return false;
                    }
                    return true;
                }
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    AgeDeathHandler.onPlayerDeath(player, damageSource, serverLevel);
                }
            }
        });

        // Player login: guidebook delivery and age data sync
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            AgeDataSyncHandler.onPlayerLoggedIn(player);
            GuidebookHandler.onPlayerLoggedIn(player);
        });

        // Player respawn: death handler and data re-sync
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (newPlayer.level() instanceof ServerLevel serverLevel) {
                AgeDeathHandler.onPlayerRespawn(newPlayer, serverLevel);
            }
            AgeDataSyncHandler.onPlayerRespawn(newPlayer);
        });

        // Player dimension change: age data sync
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            AgeDataSyncHandler.onPlayerChangeDimension(player);
        });

        // Network events (symbol sync on join)
        FabricNetworkEvents.register();
    }
}
