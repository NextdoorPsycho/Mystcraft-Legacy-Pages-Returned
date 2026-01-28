package art.arcane.mystcraft.forge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.event.AgeDeathHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge event wrapper that delegates to the common AgeDeathHandler.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class ForgeAgeDeathHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level() instanceof ServerLevel serverLevel) {
                AgeDeathHandler.onPlayerDeath(player, event.getSource(), serverLevel);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level() instanceof ServerLevel serverLevel) {
                AgeDeathHandler.onPlayerRespawn(player, serverLevel);
            }
        }
    }
}
