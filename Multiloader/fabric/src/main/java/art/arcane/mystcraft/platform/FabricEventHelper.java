package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IEventHelper;
import net.minecraft.server.level.ServerLevel;

/** Fabric event helper. Events are registered directly via Fabric API callbacks. */
public class FabricEventHelper implements IEventHelper {

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
        // Fabric has no event bus to post to. Level load logic is called directly
        // from MystcraftFabric when dimensions are loaded.
    }
}
