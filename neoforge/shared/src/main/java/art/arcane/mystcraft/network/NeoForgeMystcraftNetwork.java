package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * Network handler for Mystcraft packets on NeoForge 1.20.6+.
 * Uses CustomPacketPayload with StreamCodec and IPayloadContext.
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeMystcraftNetwork {

  private NeoForgeMystcraftNetwork() {
  }

  @SubscribeEvent
  public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(Mystcraft.MOD_ID)
        .versioned("1")
        .optional();

    // Client -> Server packets
    registrar.playToServer(OpenBookPayload.TYPE, OpenBookPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleOpenBook);

    registrar.playToServer(ContainerActionPayload.TYPE, ContainerActionPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleContainerAction);

    registrar.playToServer(LinkBookActivatePayload.TYPE, LinkBookActivatePayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleLinkBookActivate);

    registrar.playToServer(EntityBookActivatePayload.TYPE, EntityBookActivatePayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleEntityBookActivate);

    registrar.playToServer(BlockBookActivatePayload.TYPE, BlockBookActivatePayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleBlockBookActivate);

    registrar.playToServer(PocketHeadSyncPayload.TYPE, PocketHeadSyncPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handlePocketHeadSync);

    // Server -> Client packets
    registrar.playToClient(SyncAgeDataPayload.TYPE, SyncAgeDataPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleSyncAgeData);

    registrar.playToClient(LinkEffectPayload.TYPE, LinkEffectPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleLinkEffect);

    registrar.playToClient(SymbolSyncPayload.TYPE, SymbolSyncPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleSymbolSync);

    registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleConfigSync);

    registrar.playToClient(DimensionSyncPayload.TYPE, DimensionSyncPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleDimensionSync);

    registrar.playToClient(ProfilingStatePayload.TYPE, ProfilingStatePayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleProfilingState);

    registrar.playToClient(ExplosionPayload.TYPE, ExplosionPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleExplosion);

    registrar.playToClient(SpawnLightningPayload.TYPE, SpawnLightningPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleSpawnLightning);

    registrar.playToClient(LecternBookSyncPayload.TYPE, LecternBookSyncPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleLecternBookSync);

    registrar.playToClient(OpenLecternBookPayload.TYPE, OpenLecternBookPayload.STREAM_CODEC,
        NeoForgeMystcraftNetwork::handleOpenLecternBook);

    Mystcraft.LOGGER.info("Registered 16 network payloads for NeoForge 1.20.6+");
  }

  /**
   * Called during mod setup to ensure network is properly initialized.
   * With the event-based registration in 1.20.6+, this is now a no-op.
   */
  public static void register() {
    // Registration happens via RegisterPayloadHandlersEvent
    Mystcraft.LOGGER.info("NeoForge network system initialized");
  }

  // ====== Packet Send Methods ======

  public static void sendToServer(Object packet) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.sendToServer(payload);
    }
  }

  public static void sendToPlayer(Object packet, ServerPlayer player) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.sendToPlayer(player, payload);
    }
  }

  public static void sendToAll(Object packet) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.sendToAllPlayers(payload);
    }
  }

  public static void sendToTracking(Object packet, ServerPlayer player) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }
  }

  public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.sendToPlayersTrackingChunk(level, level.getChunkAt(pos).getPos(), payload);
    }
  }

  /**
   * Wraps a common packet class in the appropriate NeoForge payload wrapper.
   */
  @Nullable
  private static CustomPacketPayload wrapPacket(Object packet) {
    if (packet instanceof CustomPacketPayload payload) {
      return payload;
    }
    if (packet instanceof OpenBookPacket p) {
      return new OpenBookPayload(p);
    }
    if (packet instanceof ContainerActionPacket p) {
      return new ContainerActionPayload(p);
    }
    if (packet instanceof LinkBookActivatePacket p) {
      return new LinkBookActivatePayload(p);
    }
    if (packet instanceof EntityBookActivatePacket p) {
      return new EntityBookActivatePayload(p);
    }
    if (packet instanceof BlockBookActivatePacket p) {
      return new BlockBookActivatePayload(p);
    }
    if (packet instanceof PocketHeadSyncPacket p) {
      return new PocketHeadSyncPayload(p);
    }
    if (packet instanceof SyncAgeDataPacket p) {
      return new SyncAgeDataPayload(p);
    }
    if (packet instanceof LinkEffectPacket p) {
      return new LinkEffectPayload(p);
    }
    if (packet instanceof SymbolSyncPacket p) {
      return new SymbolSyncPayload(p);
    }
    if (packet instanceof ConfigSyncPacket p) {
      return new ConfigSyncPayload(p);
    }
    if (packet instanceof DimensionSyncPacket p) {
      return new DimensionSyncPayload(p);
    }
    if (packet instanceof ProfilingStatePacket p) {
      return new ProfilingStatePayload(p);
    }
    if (packet instanceof ExplosionPacket p) {
      return new ExplosionPayload(p);
    }
    if (packet instanceof SpawnLightningPacket p) {
      return new SpawnLightningPayload(p);
    }
    if (packet instanceof LecternBookSyncPacket p) {
      return new LecternBookSyncPayload(p);
    }
    if (packet instanceof OpenLecternBookPacket p) {
      return new OpenLecternBookPayload(p);
    }
    Mystcraft.LOGGER.warn("Unknown packet type: {}", packet.getClass().getName());
    return null;
  }

  // ====== Packet Handlers ======

  private static void handleOpenBook(OpenBookPayload payload, IPayloadContext context) {
    OpenBookPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleContainerAction(ContainerActionPayload payload, IPayloadContext context) {
    ContainerActionPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleLinkBookActivate(LinkBookActivatePayload payload, IPayloadContext context) {
    LinkBookActivatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handleEntityBookActivate(EntityBookActivatePayload payload, IPayloadContext context) {
    EntityBookActivatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handleBlockBookActivate(BlockBookActivatePayload payload, IPayloadContext context) {
    BlockBookActivatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handlePocketHeadSync(PocketHeadSyncPayload payload, IPayloadContext context) {
    PocketHeadSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleSyncAgeData(SyncAgeDataPayload payload, IPayloadContext context) {
    SyncAgeDataPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleLinkEffect(LinkEffectPayload payload, IPayloadContext context) {
    LinkEffectPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleSymbolSync(SymbolSyncPayload payload, IPayloadContext context) {
    SymbolSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleConfigSync(ConfigSyncPayload payload, IPayloadContext context) {
    ConfigSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleDimensionSync(DimensionSyncPayload payload, IPayloadContext context) {
    DimensionSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleProfilingState(ProfilingStatePayload payload, IPayloadContext context) {
    ProfilingStatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handleExplosion(ExplosionPayload payload, IPayloadContext context) {
    ExplosionPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleSpawnLightning(SpawnLightningPayload payload, IPayloadContext context) {
    SpawnLightningPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleLecternBookSync(LecternBookSyncPayload payload, IPayloadContext context) {
    LecternBookSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleOpenLecternBook(OpenLecternBookPayload payload, IPayloadContext context) {
    OpenLecternBookPacket.handle(payload.packet(), new Context(context));
  }

  // ====== Helper for StreamCodec ======

  /**
   * Creates a StreamCodec that delegates to common packet encode/decode methods.
   */
  private static <T> StreamCodec<FriendlyByteBuf, T> createCodec(
      java.util.function.BiConsumer<T, FriendlyByteBuf> encoder,
      java.util.function.Function<FriendlyByteBuf, T> decoder) {
    return new StreamCodec<>() {
      @Override
      public T decode(FriendlyByteBuf buf) {
        return decoder.apply(buf);
      }

      @Override
      public void encode(FriendlyByteBuf buf, T value) {
        encoder.accept(value, buf);
      }
    };
  }

  // ====== Payload Wrapper Records ======

  public record OpenBookPayload(OpenBookPacket packet) implements CustomPacketPayload {
    public static final Type<OpenBookPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "open_book"));
    public static final StreamCodec<FriendlyByteBuf, OpenBookPayload> STREAM_CODEC = createCodec(
        (p, buf) -> OpenBookPacket.encode(p.packet(), buf),
        buf -> new OpenBookPayload(OpenBookPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record ContainerActionPayload(ContainerActionPacket packet) implements CustomPacketPayload {
    public static final Type<ContainerActionPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "container_action"));
    public static final StreamCodec<FriendlyByteBuf, ContainerActionPayload> STREAM_CODEC = createCodec(
        (p, buf) -> ContainerActionPacket.encode(p.packet(), buf),
        buf -> new ContainerActionPayload(ContainerActionPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record LinkBookActivatePayload(LinkBookActivatePacket packet) implements CustomPacketPayload {
    public static final Type<LinkBookActivatePayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "link_book_activate"));
    public static final StreamCodec<FriendlyByteBuf, LinkBookActivatePayload> STREAM_CODEC = createCodec(
        (p, buf) -> LinkBookActivatePacket.encode(p.packet(), buf),
        buf -> new LinkBookActivatePayload(LinkBookActivatePacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record EntityBookActivatePayload(EntityBookActivatePacket packet) implements CustomPacketPayload {
    public static final Type<EntityBookActivatePayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "entity_book_activate"));
    public static final StreamCodec<FriendlyByteBuf, EntityBookActivatePayload> STREAM_CODEC = createCodec(
        (p, buf) -> EntityBookActivatePacket.encode(p.packet(), buf),
        buf -> new EntityBookActivatePayload(EntityBookActivatePacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record BlockBookActivatePayload(BlockBookActivatePacket packet) implements CustomPacketPayload {
    public static final Type<BlockBookActivatePayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "block_book_activate"));
    public static final StreamCodec<FriendlyByteBuf, BlockBookActivatePayload> STREAM_CODEC = createCodec(
        (p, buf) -> BlockBookActivatePacket.encode(p.packet(), buf),
        buf -> new BlockBookActivatePayload(BlockBookActivatePacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record PocketHeadSyncPayload(PocketHeadSyncPacket packet) implements CustomPacketPayload {
    public static final Type<PocketHeadSyncPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "pocket_head_sync"));
    public static final StreamCodec<FriendlyByteBuf, PocketHeadSyncPayload> STREAM_CODEC = createCodec(
        (p, buf) -> PocketHeadSyncPacket.encode(p.packet(), buf),
        buf -> new PocketHeadSyncPayload(PocketHeadSyncPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record SyncAgeDataPayload(SyncAgeDataPacket packet) implements CustomPacketPayload {
    public static final Type<SyncAgeDataPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "sync_age_data"));
    public static final StreamCodec<FriendlyByteBuf, SyncAgeDataPayload> STREAM_CODEC = createCodec(
        (p, buf) -> SyncAgeDataPacket.encode(p.packet(), buf),
        buf -> new SyncAgeDataPayload(SyncAgeDataPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record LinkEffectPayload(LinkEffectPacket packet) implements CustomPacketPayload {
    public static final Type<LinkEffectPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "link_effect"));
    public static final StreamCodec<FriendlyByteBuf, LinkEffectPayload> STREAM_CODEC = createCodec(
        (p, buf) -> LinkEffectPacket.encode(p.packet(), buf),
        buf -> new LinkEffectPayload(LinkEffectPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record SymbolSyncPayload(SymbolSyncPacket packet) implements CustomPacketPayload {
    public static final Type<SymbolSyncPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "symbol_sync"));
    public static final StreamCodec<FriendlyByteBuf, SymbolSyncPayload> STREAM_CODEC = createCodec(
        (p, buf) -> SymbolSyncPacket.encode(p.packet(), buf),
        buf -> new SymbolSyncPayload(SymbolSyncPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record ConfigSyncPayload(ConfigSyncPacket packet) implements CustomPacketPayload {
    public static final Type<ConfigSyncPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "config_sync"));
    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = createCodec(
        (p, buf) -> ConfigSyncPacket.encode(p.packet(), buf),
        buf -> new ConfigSyncPayload(ConfigSyncPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record DimensionSyncPayload(DimensionSyncPacket packet) implements CustomPacketPayload {
    public static final Type<DimensionSyncPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "dimension_sync"));
    public static final StreamCodec<FriendlyByteBuf, DimensionSyncPayload> STREAM_CODEC = createCodec(
        (p, buf) -> DimensionSyncPacket.encode(p.packet(), buf),
        buf -> new DimensionSyncPayload(DimensionSyncPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record ProfilingStatePayload(ProfilingStatePacket packet) implements CustomPacketPayload {
    public static final Type<ProfilingStatePayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "profiling_state"));
    public static final StreamCodec<FriendlyByteBuf, ProfilingStatePayload> STREAM_CODEC = createCodec(
        (p, buf) -> ProfilingStatePacket.encode(p.packet(), buf),
        buf -> new ProfilingStatePayload(ProfilingStatePacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record ExplosionPayload(ExplosionPacket packet) implements CustomPacketPayload {
    public static final Type<ExplosionPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "explosion"));
    public static final StreamCodec<FriendlyByteBuf, ExplosionPayload> STREAM_CODEC = createCodec(
        (p, buf) -> ExplosionPacket.encode(p.packet(), buf),
        buf -> new ExplosionPayload(ExplosionPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record SpawnLightningPayload(SpawnLightningPacket packet) implements CustomPacketPayload {
    public static final Type<SpawnLightningPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "spawn_lightning"));
    public static final StreamCodec<FriendlyByteBuf, SpawnLightningPayload> STREAM_CODEC = createCodec(
        (p, buf) -> SpawnLightningPacket.encode(p.packet(), buf),
        buf -> new SpawnLightningPayload(SpawnLightningPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record LecternBookSyncPayload(LecternBookSyncPacket packet) implements CustomPacketPayload {
    public static final Type<LecternBookSyncPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "lectern_book_sync"));
    public static final StreamCodec<FriendlyByteBuf, LecternBookSyncPayload> STREAM_CODEC = createCodec(
        (p, buf) -> LecternBookSyncPacket.encode(p.packet(), buf),
        buf -> new LecternBookSyncPayload(LecternBookSyncPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record OpenLecternBookPayload(OpenLecternBookPacket packet) implements CustomPacketPayload {
    public static final Type<OpenLecternBookPayload> TYPE = new Type<>(new ResourceLocation(Mystcraft.MOD_ID, "open_lectern_book"));
    public static final StreamCodec<FriendlyByteBuf, OpenLecternBookPayload> STREAM_CODEC = createCodec(
        (p, buf) -> OpenLecternBookPacket.encode(p.packet(), buf),
        buf -> new OpenLecternBookPayload(OpenLecternBookPacket.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  // ====== Context Adapter ======

  /**
   * Adapts NeoForge's IPayloadContext to the common PacketContext interface.
   */
  public static class Context implements PacketContext {
    private final IPayloadContext ctx;

    public Context(IPayloadContext ctx) {
      this.ctx = ctx;
    }

    @Override
    @Nullable
    public Player getPlayer() {
      return ctx.player();
    }

    @Override
    @Nullable
    public ServerPlayer getServerPlayer() {
      Player player = ctx.player();
      return player instanceof ServerPlayer sp ? sp : null;
    }

    @Override
    public boolean isClientSide() {
      return ctx.flow().isClientbound();
    }

    @Override
    public void enqueueWork(Runnable work) {
      ctx.enqueueWork(work);
    }
  }
}
