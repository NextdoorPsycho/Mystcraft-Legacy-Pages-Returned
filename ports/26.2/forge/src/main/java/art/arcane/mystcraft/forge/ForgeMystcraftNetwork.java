package art.arcane.mystcraft.forge;

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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadFlow;
import net.minecraftforge.network.payload.PayloadProtocol;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Typed Forge 65 payload channel for all Mystcraft play packets. */
public final class ForgeMystcraftNetwork {

  private static Channel<CustomPacketPayload> channel;
  private static Function<CustomPayloadEvent.Context, PacketContext> clientContextFactory;

  private ForgeMystcraftNetwork() {
  }

  public static synchronized void register() {
    if (channel != null) {
      return;
    }

    Identifier channelId = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "main");
    PayloadProtocol<RegistryFriendlyByteBuf, CustomPacketPayload> play =
        ChannelBuilder.named(channelId)
            .networkProtocolVersion(1)
            .payloadChannel()
            .play();

    PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow = play.serverbound();
    flow = serverbound(flow, OpenBookPacket.TYPE, OpenBookPacket.STREAM_CODEC,
        OpenBookPacket::handle);
    flow = serverbound(flow, ContainerActionPacket.TYPE, ContainerActionPacket.STREAM_CODEC,
        ContainerActionPacket::handle);
    flow = serverbound(flow, LinkBookActivatePacket.TYPE, LinkBookActivatePacket.STREAM_CODEC,
        LinkBookActivatePacket::handle);
    flow = serverbound(flow, EntityBookActivatePacket.TYPE, EntityBookActivatePacket.STREAM_CODEC,
        EntityBookActivatePacket::handle);
    flow = serverbound(flow, BlockBookActivatePacket.TYPE, BlockBookActivatePacket.STREAM_CODEC,
        BlockBookActivatePacket::handle);
    flow = serverbound(flow, PocketHeadSyncPacket.TYPE, PocketHeadSyncPacket.STREAM_CODEC,
        PocketHeadSyncPacket::handle);

    flow = flow.clientbound();
    flow = clientbound(flow, SyncAgeDataPacket.TYPE, SyncAgeDataPacket.STREAM_CODEC,
        SyncAgeDataPacket::handle);
    flow = clientbound(flow, LinkEffectPacket.TYPE, LinkEffectPacket.STREAM_CODEC,
        LinkEffectPacket::handle);
    flow = clientbound(flow, SymbolSyncPacket.TYPE, SymbolSyncPacket.STREAM_CODEC,
        SymbolSyncPacket::handle);
    flow = clientbound(flow, ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC,
        ConfigSyncPacket::handle);
    flow = clientbound(flow, DimensionSyncPacket.TYPE, DimensionSyncPacket.STREAM_CODEC,
        DimensionSyncPacket::handle);
    flow = clientbound(flow, ProfilingStatePacket.TYPE, ProfilingStatePacket.STREAM_CODEC,
        ProfilingStatePacket::handle);
    flow = clientbound(flow, ExplosionPacket.TYPE, ExplosionPacket.STREAM_CODEC,
        ExplosionPacket::handle);
    flow = clientbound(flow, SpawnLightningPacket.TYPE, SpawnLightningPacket.STREAM_CODEC,
        SpawnLightningPacket::handle);
    flow = clientbound(flow, LecternBookSyncPacket.TYPE, LecternBookSyncPacket.STREAM_CODEC,
        LecternBookSyncPacket::handle);
    flow = clientbound(flow, OpenLecternBookPacket.TYPE, OpenLecternBookPacket.STREAM_CODEC,
        OpenLecternBookPacket::handle);
    channel = flow.build();

    MystcraftNetwork.sendToServerHandler = payload -> requireChannel().send(
        payload, PacketDistributor.SERVER.noArg());
    MystcraftNetwork.sendToPlayerHandler = (payload, player) -> requireChannel().send(
        payload, PacketDistributor.PLAYER.with(player));
    MystcraftNetwork.sendToAllHandler = payload -> requireChannel().send(
        payload, PacketDistributor.ALL.noArg());
    MystcraftNetwork.sendToTrackingHandler = (payload, player) -> requireChannel().send(
        payload, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    MystcraftNetwork.sendToTrackingBlockHandler = (payload, level, pos) -> requireChannel().send(
        payload, PacketDistributor.TRACKING_CHUNK.with(level.getChunkAt(pos)));

    Mystcraft.LOGGER.info("[Mystcraft] Registered 16 Forge play payloads");
  }

  static void installClientContextFactory(
      Function<CustomPayloadEvent.Context, PacketContext> factory) {
    clientContextFactory = Objects.requireNonNull(factory, "factory");
  }

  private static <T extends CustomPacketPayload>
  PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> serverbound(
      PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow,
      CustomPacketPayload.Type<T> type,
      StreamCodec<RegistryFriendlyByteBuf, T> codec,
      BiConsumer<T, PacketContext> handler) {
    return flow.add(type, codec, (payload, context) -> {
      handler.accept(payload, new ServerPacketContext(context));
      context.setPacketHandled(true);
    });
  }

  private static <T extends CustomPacketPayload>
  PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> clientbound(
      PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow,
      CustomPacketPayload.Type<T> type,
      StreamCodec<RegistryFriendlyByteBuf, T> codec,
      BiConsumer<T, PacketContext> handler) {
    return flow.add(type, codec, (payload, context) -> {
      Function<CustomPayloadEvent.Context, PacketContext> factory = clientContextFactory;
      if (factory == null) {
        throw new IllegalStateException("Client packet context was not installed");
      }
      handler.accept(payload, factory.apply(context));
      context.setPacketHandled(true);
    });
  }

  private static Channel<CustomPacketPayload> requireChannel() {
    Channel<CustomPacketPayload> current = channel;
    if (current == null) {
      throw new IllegalStateException("Mystcraft network channel is not registered");
    }
    return current;
  }

  private record ServerPacketContext(CustomPayloadEvent.Context forgeContext)
      implements PacketContext {

    @Nullable
    @Override
    public Player getPlayer() {
      return forgeContext.getSender();
    }

    @Nullable
    @Override
    public ServerPlayer getServerPlayer() {
      return forgeContext.getSender();
    }

    @Override
    public boolean isClientSide() {
      return false;
    }

    @Override
    public void enqueueWork(Runnable work) {
      forgeContext.enqueueWork(work);
    }
  }
}
