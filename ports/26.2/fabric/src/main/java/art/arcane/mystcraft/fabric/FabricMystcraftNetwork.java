package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.BlockBookActivatePacket;
import art.arcane.mystcraft.network.ConfigSyncPacket;
import art.arcane.mystcraft.network.ContainerActionPacket;
import art.arcane.mystcraft.network.DimensionSyncPacket;
import art.arcane.mystcraft.network.EntityBookActivatePacket;
import art.arcane.mystcraft.network.ExplosionPacket;
import art.arcane.mystcraft.network.LecternBookSyncPacket;
import art.arcane.mystcraft.network.LinkBookActivatePacket;
import art.arcane.mystcraft.network.LinkEffectPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.OpenBookPacket;
import art.arcane.mystcraft.network.OpenLecternBookPacket;
import art.arcane.mystcraft.network.PacketContext;
import art.arcane.mystcraft.network.PocketHeadSyncPacket;
import art.arcane.mystcraft.network.ProfilingStatePacket;
import art.arcane.mystcraft.network.SpawnLightningPacket;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.network.SyncAgeDataPacket;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Fabric payload registration and server-side transport. */
public final class FabricMystcraftNetwork {

  private static boolean payloadTypesRegistered;
  private static boolean serverReceiversRegistered;

  private FabricMystcraftNetwork() {
  }

  public static synchronized void registerPayloadTypes() {
    if (payloadTypesRegistered) {
      return;
    }

    PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> serverbound =
        PayloadTypeRegistry.serverboundPlay();
    register(serverbound, OpenBookPacket.TYPE, OpenBookPacket.STREAM_CODEC);
    register(serverbound, ContainerActionPacket.TYPE, ContainerActionPacket.STREAM_CODEC);
    register(serverbound, LinkBookActivatePacket.TYPE, LinkBookActivatePacket.STREAM_CODEC);
    register(serverbound, EntityBookActivatePacket.TYPE, EntityBookActivatePacket.STREAM_CODEC);
    register(serverbound, BlockBookActivatePacket.TYPE, BlockBookActivatePacket.STREAM_CODEC);
    register(serverbound, PocketHeadSyncPacket.TYPE, PocketHeadSyncPacket.STREAM_CODEC);

    PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> clientbound =
        PayloadTypeRegistry.clientboundPlay();
    register(clientbound, SyncAgeDataPacket.TYPE, SyncAgeDataPacket.STREAM_CODEC);
    register(clientbound, LinkEffectPacket.TYPE, LinkEffectPacket.STREAM_CODEC);
    register(clientbound, SymbolSyncPacket.TYPE, SymbolSyncPacket.STREAM_CODEC);
    register(clientbound, ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC);
    register(clientbound, DimensionSyncPacket.TYPE, DimensionSyncPacket.STREAM_CODEC);
    register(clientbound, ProfilingStatePacket.TYPE, ProfilingStatePacket.STREAM_CODEC);
    register(clientbound, ExplosionPacket.TYPE, ExplosionPacket.STREAM_CODEC);
    register(clientbound, SpawnLightningPacket.TYPE, SpawnLightningPacket.STREAM_CODEC);
    register(clientbound, LecternBookSyncPacket.TYPE, LecternBookSyncPacket.STREAM_CODEC);
    register(clientbound, OpenLecternBookPacket.TYPE, OpenLecternBookPacket.STREAM_CODEC);

    payloadTypesRegistered = true;
  }

  public static synchronized void registerServerReceivers() {
    if (serverReceiversRegistered) {
      return;
    }

    registerServerReceiver(OpenBookPacket.TYPE, OpenBookPacket::handle);
    registerServerReceiver(ContainerActionPacket.TYPE, ContainerActionPacket::handle);
    registerServerReceiver(LinkBookActivatePacket.TYPE, LinkBookActivatePacket::handle);
    registerServerReceiver(EntityBookActivatePacket.TYPE, EntityBookActivatePacket::handle);
    registerServerReceiver(BlockBookActivatePacket.TYPE, BlockBookActivatePacket::handle);
    registerServerReceiver(PocketHeadSyncPacket.TYPE, PocketHeadSyncPacket::handle);
    serverReceiversRegistered = true;
  }

  public static void installSenders() {
    MystcraftNetwork.sendToPlayerHandler = (packet, player) ->
        ServerPlayNetworking.send(player, packet);
    MystcraftNetwork.sendToAllHandler = packet -> {
      MinecraftServer server = Mystcraft.getCurrentServer();
      if (server != null) {
        for (ServerPlayer player : PlayerLookup.all(server)) {
          ServerPlayNetworking.send(player, packet);
        }
      }
    };
    MystcraftNetwork.sendToTrackingHandler = (packet, trackedPlayer) -> {
      for (ServerPlayer player : PlayerLookup.tracking(trackedPlayer)) {
        ServerPlayNetworking.send(player, packet);
      }
      ServerPlayNetworking.send(trackedPlayer, packet);
    };
    MystcraftNetwork.sendToTrackingBlockHandler = FabricMystcraftNetwork::sendToTrackingBlock;
  }

  private static void sendToTrackingBlock(
      CustomPacketPayload packet,
      ServerLevel level,
      BlockPos position
  ) {
    for (ServerPlayer player : PlayerLookup.tracking(level, position)) {
      ServerPlayNetworking.send(player, packet);
    }
  }

  private static <T extends CustomPacketPayload> void register(
      PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> registry,
      CustomPacketPayload.Type<T> type,
      StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, T> codec
  ) {
    registry.register(type, codec);
  }

  private static <T extends CustomPacketPayload> void registerServerReceiver(
      CustomPacketPayload.Type<T> type,
      BiConsumer<T, PacketContext> handler
  ) {
    ServerPlayNetworking.registerGlobalReceiver(type, (packet, context) ->
        handler.accept(packet, new ServerContext(context.server(), context.player())));
  }

  private record ServerContext(MinecraftServer server, ServerPlayer player)
      implements PacketContext {

    @Override
    public Player getPlayer() {
      return player;
    }

    @Override
    public ServerPlayer getServerPlayer() {
      return player;
    }

    @Override
    public boolean isClientSide() {
      return false;
    }

    @Override
    public void enqueueWork(Runnable work) {
      server.execute(work);
    }
  }
}
