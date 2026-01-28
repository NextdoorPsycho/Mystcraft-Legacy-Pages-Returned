package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IEventHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge event helper. Events are auto-registered via @Mod.EventBusSubscriber. */
public class NeoForgeEventHelper implements IEventHelper {

    @Override
    public void registerServerEvents() {
    }

    @Override
    public void registerClientEvents() {
    }

    @Override
    public void registerCommonEvents() {
    }

    @Override
    public void fireLevelLoadEvent(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }
}
