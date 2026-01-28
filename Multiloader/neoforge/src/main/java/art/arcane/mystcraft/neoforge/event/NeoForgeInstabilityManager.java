package art.arcane.mystcraft.neoforge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.instability.InstabilityManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** NeoForge event wrapper that delegates to the common InstabilityManager. */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class NeoForgeInstabilityManager {

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        InstabilityManager.onLevelTick(level);
    }
}
