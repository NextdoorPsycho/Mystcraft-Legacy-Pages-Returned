package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handlers for network-related events (1.20.1 version).
 * Handles syncing data to players when they join.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class NetworkEvents_1_20_1 {

    private NetworkEvents_1_20_1() {
    }

    /**
     * Syncs Mystcraft data to players when they join the server.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync symbol registry to the joining player
            ForgeMystcraftNetwork_1_20_1.sendToPlayer(new SymbolSyncPacket(), player);
            Mystcraft.LOGGER.debug("Sent symbol sync packet to player {}", player.getName().getString());
        }
    }

    /**
     * Syncs Mystcraft data to players when they change dimensions.
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Could sync age-specific data here if needed
            Mystcraft.LOGGER.debug("Player {} changed dimension from {} to {}",
                    player.getName().getString(), event.getFrom(), event.getTo());
        }
    }
}
