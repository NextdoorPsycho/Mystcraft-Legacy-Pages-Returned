package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import net.neoforged.neoforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Network handler for Mystcraft packets on NeoForge.
 * Uses SimpleChannel for client-server communication.
 * Includes inner PacketContext adapter class.
 */
public final class NeoForgeMystcraftNetwork {

  private static final int PROTOCOL_VERSION = 1;

  public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
      .named(new ResourceLocation(Mystcraft.MOD_ID, "main"))
      .networkProtocolVersion(() -> Integer.toString(PROTOCOL_VERSION))
      .clientAcceptedVersions(version -> true)
      .serverAcceptedVersions(version -> true)
      .simpleChannel();

  private static int packetId = 0;

  private NeoForgeMystcraftNetwork() {
  }

  private static <T> net.neoforged.neoforge.network.simple.MessageFunctions.MessageConsumer<T> wrap(BiConsumer<T, PacketContext> handler) {
    return (packet, ctx) -> handler.accept(packet, new Context(ctx));
  }

  /**
   * Registers all packets. Call during mod setup.
   */
  public static void register() {
    CHANNEL.messageBuilder(OpenBookPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
        .encoder(OpenBookPacket::encode)
        .decoder(OpenBookPacket::decode)
        .consumerMainThread(wrap(OpenBookPacket::handle))
        .add();

    CHANNEL.messageBuilder(SyncAgeDataPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(SyncAgeDataPacket::encode)
        .decoder(SyncAgeDataPacket::decode)
        .consumerMainThread(wrap(SyncAgeDataPacket::handle))
        .add();

    CHANNEL.messageBuilder(LinkEffectPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(LinkEffectPacket::encode)
        .decoder(LinkEffectPacket::decode)
        .consumerMainThread(wrap(LinkEffectPacket::handle))
        .add();

    CHANNEL.messageBuilder(SymbolSyncPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(SymbolSyncPacket::encode)
        .decoder(SymbolSyncPacket::decode)
        .consumerMainThread(wrap(SymbolSyncPacket::handle))
        .add();

    CHANNEL.messageBuilder(ContainerActionPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
        .encoder(ContainerActionPacket::encode)
        .decoder(ContainerActionPacket::decode)
        .consumerMainThread(wrap(ContainerActionPacket::handle))
        .add();

    CHANNEL.messageBuilder(ConfigSyncPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(ConfigSyncPacket::encode)
        .decoder(ConfigSyncPacket::decode)
        .consumerMainThread(wrap(ConfigSyncPacket::handle))
        .add();

    CHANNEL.messageBuilder(DimensionSyncPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(DimensionSyncPacket::encode)
        .decoder(DimensionSyncPacket::decode)
        .consumerMainThread(wrap(DimensionSyncPacket::handle))
        .add();

    CHANNEL.messageBuilder(ProfilingStatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(ProfilingStatePacket::encode)
        .decoder(ProfilingStatePacket::decode)
        .consumerMainThread(wrap(ProfilingStatePacket::handle))
        .add();

    CHANNEL.messageBuilder(ExplosionPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(ExplosionPacket::encode)
        .decoder(ExplosionPacket::decode)
        .consumerMainThread(wrap(ExplosionPacket::handle))
        .add();

    CHANNEL.messageBuilder(SpawnLightningPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(SpawnLightningPacket::encode)
        .decoder(SpawnLightningPacket::decode)
        .consumerMainThread(wrap(SpawnLightningPacket::handle))
        .add();

    CHANNEL.messageBuilder(LinkBookActivatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
        .encoder(LinkBookActivatePacket::encode)
        .decoder(LinkBookActivatePacket::decode)
        .consumerMainThread(wrap(LinkBookActivatePacket::handle))
        .add();

    CHANNEL.messageBuilder(EntityBookActivatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
        .encoder(EntityBookActivatePacket::encode)
        .decoder(EntityBookActivatePacket::decode)
        .consumerMainThread(wrap(EntityBookActivatePacket::handle))
        .add();

    CHANNEL.messageBuilder(BlockBookActivatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
        .encoder(BlockBookActivatePacket::encode)
        .decoder(BlockBookActivatePacket::decode)
        .consumerMainThread(wrap(BlockBookActivatePacket::handle))
        .add();

    CHANNEL.messageBuilder(PocketHeadSyncPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
        .encoder(PocketHeadSyncPacket::encode)
        .decoder(PocketHeadSyncPacket::decode)
        .consumerMainThread(wrap(PocketHeadSyncPacket::handle))
        .add();

    CHANNEL.messageBuilder(LecternBookSyncPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(LecternBookSyncPacket::encode)
        .decoder(LecternBookSyncPacket::decode)
        .consumerMainThread(wrap(LecternBookSyncPacket::handle))
        .add();

    CHANNEL.messageBuilder(OpenLecternBookPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
        .encoder(OpenLecternBookPacket::encode)
        .decoder(OpenLecternBookPacket::decode)
        .consumerMainThread(wrap(OpenLecternBookPacket::handle))
        .add();

    Mystcraft.LOGGER.info("Registered {} network packets", packetId);
  }

  public static void sendToServer(Object packet) {
    CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
  }

  public static void sendToPlayer(Object packet, ServerPlayer player) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
  }

  public static void sendToAll(Object packet) {
    CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
  }

  public static void sendToTracking(Object packet, ServerPlayer player) {
    CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
  }

  public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
    CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);
  }

  /**
   * Adapts NeoForge's NetworkEvent.Context to the common PacketContext interface.
   */
  public static class Context implements PacketContext {
    private final NetworkEvent.Context ctx;

    public Context(NetworkEvent.Context ctx) {
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
}
