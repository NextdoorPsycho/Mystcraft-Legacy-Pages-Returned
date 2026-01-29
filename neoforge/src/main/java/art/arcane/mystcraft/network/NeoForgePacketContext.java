package art.arcane.mystcraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Adapts NeoForge's NetworkEvent.Context to the common PacketContext interface.
 */
public class NeoForgePacketContext implements PacketContext {

    private final NetworkEvent.Context ctx;

    public NeoForgePacketContext(NetworkEvent.Context ctx) {
        this.ctx = ctx;
    }

    @Override
    @Nullable
    public Player getPlayer() {
        return ctx.getSender();
    }

    @Override
    @Nullable
    public ServerPlayer getServerPlayer() {
        return ctx.getSender();
    }

    @Override
    public boolean isClientSide() {
        return ctx.getDirection().getReceptionSide().isClient();
    }

    @Override
    public void enqueueWork(Runnable work) {
        ctx.enqueueWork(work);
    }
}
