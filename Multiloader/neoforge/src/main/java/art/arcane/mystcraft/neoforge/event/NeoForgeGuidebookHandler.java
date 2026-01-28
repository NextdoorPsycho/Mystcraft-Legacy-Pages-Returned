package art.arcane.mystcraft.neoforge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.event.GuidebookHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** NeoForge event wrapper that delegates to the common GuidebookHandler. */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class NeoForgeGuidebookHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuidebookHandler.onPlayerLoggedIn(player);
        }
    }
}
