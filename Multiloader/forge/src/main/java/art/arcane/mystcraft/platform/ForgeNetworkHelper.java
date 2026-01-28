package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

// Forge networking is handled by ForgeMystcraftNetwork. This stub delegates to it.
public class ForgeNetworkHelper implements INetworkHelper {

    @Override
    public void register() {
        // Forge networking is registered in ForgeMystcraftNetwork.register()
    }

    @Override
    public void sendToServer(Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork directly for Forge networking");
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork directly for Forge networking");
    }

    @Override
    public void sendToAllTracking(Entity entity, Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork directly for Forge networking");
    }

    @Override
    public void sendToAll(Object packet) {
        throw new UnsupportedOperationException("Use ForgeMystcraftNetwork directly for Forge networking");
    }
}
