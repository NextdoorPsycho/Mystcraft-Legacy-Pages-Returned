package art.arcane.mystcraft.neoforge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.event.AgeDeathHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** NeoForge event wrapper that delegates to the common AgeDeathHandler. */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class NeoForgeAgeDeathHandler {

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
