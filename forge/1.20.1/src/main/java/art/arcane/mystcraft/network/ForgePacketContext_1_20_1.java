package art.arcane.mystcraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Adapts Forge's NetworkEvent.Context to the common PacketContext interface.
 * This is the 1.20.1-specific implementation.
 */
public class ForgePacketContext_1_20_1 implements PacketContext {

  private final NetworkEvent.Context ctx;

  public ForgePacketContext_1_20_1(NetworkEvent.Context ctx) {
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
