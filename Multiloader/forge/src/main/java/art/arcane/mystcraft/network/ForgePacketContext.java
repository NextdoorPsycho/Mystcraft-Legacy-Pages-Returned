package art.arcane.mystcraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Adapts Forge's CustomPayloadEvent.Context to the common PacketContext interface.
 */
public class ForgePacketContext implements PacketContext {

    private final CustomPayloadEvent.Context ctx;

    public ForgePacketContext(CustomPayloadEvent.Context ctx) {
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
        return ctx.isClientSide();
    }

    @Override
    public void enqueueWork(Runnable work) {
        ctx.enqueueWork(work);
    }
}
