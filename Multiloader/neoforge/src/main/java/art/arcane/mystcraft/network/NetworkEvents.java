package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Event handlers for network-related events.
 * Handles syncing data to players when they join.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class NetworkEvents {

    private NetworkEvents() {
    }

    /** Syncs Mystcraft data to players when they join the server. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeoForgeMystcraftNetwork.sendToPlayer(new SymbolSyncPacket(), player);
            Mystcraft.LOGGER.debug("Sent symbol sync packet to player {}", player.getName().getString());
        }
    }

    /** Syncs Mystcraft data to players when they change dimensions. */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Mystcraft.LOGGER.debug("Player {} changed dimension from {} to {}",
                    player.getName().getString(), event.getFrom(), event.getTo());
        }
    }
}
