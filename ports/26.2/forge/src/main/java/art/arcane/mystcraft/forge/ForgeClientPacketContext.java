package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.network.PacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

/** Client-only packet context adapter. */
final class ForgeClientPacketContext implements PacketContext {

  private final CustomPayloadEvent.Context forgeContext;

  ForgeClientPacketContext(CustomPayloadEvent.Context forgeContext) {
    this.forgeContext = forgeContext;
  }

  @Nullable
  @Override
  public Player getPlayer() {
    return Minecraft.getInstance().player;
  }

  @Nullable
  @Override
  public ServerPlayer getServerPlayer() {
    return null;
  }

  @Override
  public boolean isClientSide() {
    return true;
  }

  @Override
  public void enqueueWork(Runnable work) {
    forgeContext.enqueueWork(work);
  }
}
