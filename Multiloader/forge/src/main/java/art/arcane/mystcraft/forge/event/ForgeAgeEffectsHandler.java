package art.arcane.mystcraft.forge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.event.AgeEffectsHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge event wrapper that delegates to the common AgeEffectsHandler.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class ForgeAgeEffectsHandler {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        AgeEffectsHandler.onLevelTick(serverLevel);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        boolean cancel = AgeEffectsHandler.onLivingAttack(entity, event.getSource());
        if (cancel) {
            event.setCanceled(true);
        }
    }
}
