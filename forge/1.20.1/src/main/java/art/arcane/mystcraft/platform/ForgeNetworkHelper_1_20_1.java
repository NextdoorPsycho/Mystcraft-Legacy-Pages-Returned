package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Forge 1.20.1 networking helper.
 * Forge networking is handled by ForgeMystcraftNetwork_1_20_1. This stub delegates to it.
 */
public class ForgeNetworkHelper_1_20_1 implements INetworkHelper {

    @Override
    public void register() {
        // Forge networking is registered in ForgeMystcraftNetwork_1_20_1.register()
    }

    @Override
    public void sendToServer(Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork_1_20_1 directly for Forge networking");
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork_1_20_1 directly for Forge networking");
    }

    @Override
    public void sendToAllTracking(Entity entity, Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork_1_20_1 directly for Forge networking");
    }

    @Override
    public void sendToAll(Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork_1_20_1 directly for Forge networking");
    }
}
