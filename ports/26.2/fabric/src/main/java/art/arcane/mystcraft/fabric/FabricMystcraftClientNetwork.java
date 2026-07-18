package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.network.ConfigSyncPacket;
import art.arcane.mystcraft.network.DimensionSyncPacket;
import art.arcane.mystcraft.network.ExplosionPacket;
import art.arcane.mystcraft.network.LecternBookSyncPacket;
import art.arcane.mystcraft.network.LinkEffectPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.OpenLecternBookPacket;
import art.arcane.mystcraft.network.PacketContext;
import art.arcane.mystcraft.network.ProfilingStatePacket;
import art.arcane.mystcraft.network.SpawnLightningPacket;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.network.SyncAgeDataPacket;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Client-only receivers and the C2S sender bridge. */
final class FabricMystcraftClientNetwork {

  private static boolean registered;

  private FabricMystcraftClientNetwork() {
  }

  static synchronized void register() {
    if (registered) {
      return;
    }

    registerReceiver(SyncAgeDataPacket.TYPE, SyncAgeDataPacket::handle);
    registerReceiver(LinkEffectPacket.TYPE, LinkEffectPacket::handle);
    registerReceiver(SymbolSyncPacket.TYPE, SymbolSyncPacket::handle);
    registerReceiver(ConfigSyncPacket.TYPE, ConfigSyncPacket::handle);
    registerReceiver(DimensionSyncPacket.TYPE, DimensionSyncPacket::handle);
    registerReceiver(ProfilingStatePacket.TYPE, ProfilingStatePacket::handle);
    registerReceiver(ExplosionPacket.TYPE, ExplosionPacket::handle);
    registerReceiver(SpawnLightningPacket.TYPE, SpawnLightningPacket::handle);
    registerReceiver(LecternBookSyncPacket.TYPE, LecternBookSyncPacket::handle);
    registerReceiver(OpenLecternBookPacket.TYPE, OpenLecternBookPacket::handle);

    MystcraftNetwork.sendToServerHandler = ClientPlayNetworking::send;
    registered = true;
  }

  private static <T extends CustomPacketPayload> void registerReceiver(
      CustomPacketPayload.Type<T> type,
      BiConsumer<T, PacketContext> handler
  ) {
    ClientPlayNetworking.registerGlobalReceiver(type, (packet, context) ->
        handler.accept(packet, new ClientContext(context.client())));
  }

  private record ClientContext(Minecraft client) implements PacketContext {

    @Override
    @Nullable
    public Player getPlayer() {
      return client.player;
    }

    @Override
    @Nullable
    public ServerPlayer getServerPlayer() {
      return null;
    }

    @Override
    public boolean isClientSide() {
      return true;
    }

    @Override
    public void enqueueWork(Runnable work) {
      client.execute(work);
    }
  }
}
