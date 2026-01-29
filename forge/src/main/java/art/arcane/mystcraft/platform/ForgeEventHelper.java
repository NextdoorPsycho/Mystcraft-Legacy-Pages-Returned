package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IEventHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;

// Forge events are registered via @SubscribeEvent annotations and the Forge event bus.
// Event handler classes register themselves in MystcraftForge.
public class ForgeEventHelper implements IEventHelper {

    @Override
    public void registerServerEvents() {
        // Forge server events are auto-registered via @Mod.EventBusSubscriber
    }

    @Override
    public void registerClientEvents() {
        // Forge client events are auto-registered via @Mod.EventBusSubscriber(Dist.CLIENT)
    }

    @Override
    public void registerCommonEvents() {
        // Forge common events are auto-registered via @Mod.EventBusSubscriber
    }

    @Override
    public void fireLevelLoadEvent(ServerLevel level) {
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }
}
