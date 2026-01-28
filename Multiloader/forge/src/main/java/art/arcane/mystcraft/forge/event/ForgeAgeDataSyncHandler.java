package art.arcane.mystcraft.forge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.event.AgeDataSyncHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge event wrapper that delegates to the common AgeDataSyncHandler.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class ForgeAgeDataSyncHandler {

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AgeDataSyncHandler.onPlayerChangeDimension(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AgeDataSyncHandler.onPlayerLoggedIn(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AgeDataSyncHandler.onPlayerRespawn(player);
        }
    }
}
