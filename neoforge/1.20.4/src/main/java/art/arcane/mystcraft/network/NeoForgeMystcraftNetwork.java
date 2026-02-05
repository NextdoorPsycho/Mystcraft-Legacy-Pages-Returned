package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * Network handler for Mystcraft packets on NeoForge 1.20.4.
 * Uses CustomPacketPayload with FriendlyByteBuf.Reader and PlayPayloadContext.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NeoForgeMystcraftNetwork {

  private NeoForgeMystcraftNetwork() {
  }

  @SubscribeEvent
  public static void onRegisterPayloadHandlers(RegisterPayloadHandlerEvent event) {
    IPayloadRegistrar registrar = event.registrar(Mystcraft.MOD_ID)
        .versioned("1")
        .optional();

    // Client -> Server packets
    registrar.play(OpenBookPayload.ID, OpenBookPayload::read, builder -> builder.server(NeoForgeMystcraftNetwork::handleOpenBook));
    registrar.play(ContainerActionPayload.ID, ContainerActionPayload::read, builder -> builder.server(NeoForgeMystcraftNetwork::handleContainerAction));
    registrar.play(LinkBookActivatePayload.ID, LinkBookActivatePayload::read, builder -> builder.server(NeoForgeMystcraftNetwork::handleLinkBookActivate));
    registrar.play(EntityBookActivatePayload.ID, EntityBookActivatePayload::read, builder -> builder.server(NeoForgeMystcraftNetwork::handleEntityBookActivate));
    registrar.play(BlockBookActivatePayload.ID, BlockBookActivatePayload::read, builder -> builder.server(NeoForgeMystcraftNetwork::handleBlockBookActivate));
    registrar.play(PocketHeadSyncPayload.ID, PocketHeadSyncPayload::read, builder -> builder.server(NeoForgeMystcraftNetwork::handlePocketHeadSync));

    // Server -> Client packets
    registrar.play(SyncAgeDataPayload.ID, SyncAgeDataPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleSyncAgeData));
    registrar.play(LinkEffectPayload.ID, LinkEffectPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleLinkEffect));
    registrar.play(SymbolSyncPayload.ID, SymbolSyncPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleSymbolSync));
    registrar.play(ConfigSyncPayload.ID, ConfigSyncPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleConfigSync));
    registrar.play(DimensionSyncPayload.ID, DimensionSyncPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleDimensionSync));
    registrar.play(ProfilingStatePayload.ID, ProfilingStatePayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleProfilingState));
    registrar.play(ExplosionPayload.ID, ExplosionPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleExplosion));
    registrar.play(SpawnLightningPayload.ID, SpawnLightningPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleSpawnLightning));
    registrar.play(LecternBookSyncPayload.ID, LecternBookSyncPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleLecternBookSync));
    registrar.play(OpenLecternBookPayload.ID, OpenLecternBookPayload::read, builder -> builder.client(NeoForgeMystcraftNetwork::handleOpenLecternBook));

    Mystcraft.LOGGER.info("Registered 16 network payloads for NeoForge 1.20.4");
  }

  /**
   * Called during mod setup to ensure network is properly initialized.
   */
  public static void register() {
    Mystcraft.LOGGER.info("NeoForge network system initialized");
  }

  // ====== Packet Send Methods ======

  public static void sendToServer(Object packet) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.SERVER.noArg().send(payload);
    }
  }

  public static void sendToPlayer(Object packet, ServerPlayer player) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.PLAYER.with(player).send(payload);
    }
  }

  public static void sendToAll(Object packet) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.ALL.noArg().send(payload);
    }
  }

  public static void sendToTracking(Object packet, ServerPlayer player) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player).send(payload);
    }
  }

  public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
    CustomPacketPayload payload = wrapPacket(packet);
    if (payload != null) {
      PacketDistributor.TRACKING_CHUNK.with(level.getChunkAt(pos)).send(payload);
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

  private static void handleOpenBook(OpenBookPayload payload, PlayPayloadContext context) {
    OpenBookPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleContainerAction(ContainerActionPayload payload, PlayPayloadContext context) {
    ContainerActionPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleLinkBookActivate(LinkBookActivatePayload payload, PlayPayloadContext context) {
    LinkBookActivatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handleEntityBookActivate(EntityBookActivatePayload payload, PlayPayloadContext context) {
    EntityBookActivatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handleBlockBookActivate(BlockBookActivatePayload payload, PlayPayloadContext context) {
    BlockBookActivatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handlePocketHeadSync(PocketHeadSyncPayload payload, PlayPayloadContext context) {
    PocketHeadSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleSyncAgeData(SyncAgeDataPayload payload, PlayPayloadContext context) {
    SyncAgeDataPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleLinkEffect(LinkEffectPayload payload, PlayPayloadContext context) {
    LinkEffectPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleSymbolSync(SymbolSyncPayload payload, PlayPayloadContext context) {
    SymbolSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleConfigSync(ConfigSyncPayload payload, PlayPayloadContext context) {
    ConfigSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleDimensionSync(DimensionSyncPayload payload, PlayPayloadContext context) {
    DimensionSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleProfilingState(ProfilingStatePayload payload, PlayPayloadContext context) {
    ProfilingStatePacket.handle(payload.packet(), new Context(context));
  }

  private static void handleExplosion(ExplosionPayload payload, PlayPayloadContext context) {
    ExplosionPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleSpawnLightning(SpawnLightningPayload payload, PlayPayloadContext context) {
    SpawnLightningPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleLecternBookSync(LecternBookSyncPayload payload, PlayPayloadContext context) {
    LecternBookSyncPacket.handle(payload.packet(), new Context(context));
  }

  private static void handleOpenLecternBook(OpenLecternBookPayload payload, PlayPayloadContext context) {
    OpenLecternBookPacket.handle(payload.packet(), new Context(context));
  }

  // ====== Payload Definitions ======

  public record OpenBookPayload(OpenBookPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "open_book");
    public static OpenBookPayload read(FriendlyByteBuf buf) {
      return new OpenBookPayload(OpenBookPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      OpenBookPacket.encode(packet, buf);
    }
  }

  public record ContainerActionPayload(ContainerActionPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "container_action");
    public static ContainerActionPayload read(FriendlyByteBuf buf) {
      return new ContainerActionPayload(ContainerActionPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      ContainerActionPacket.encode(packet, buf);
    }
  }

  public record LinkBookActivatePayload(LinkBookActivatePacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "link_book_activate");
    public static LinkBookActivatePayload read(FriendlyByteBuf buf) {
      return new LinkBookActivatePayload(LinkBookActivatePacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      LinkBookActivatePacket.encode(packet, buf);
    }
  }

  public record EntityBookActivatePayload(EntityBookActivatePacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "entity_book_activate");
    public static EntityBookActivatePayload read(FriendlyByteBuf buf) {
      return new EntityBookActivatePayload(EntityBookActivatePacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      EntityBookActivatePacket.encode(packet, buf);
    }
  }

  public record BlockBookActivatePayload(BlockBookActivatePacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "block_book_activate");
    public static BlockBookActivatePayload read(FriendlyByteBuf buf) {
      return new BlockBookActivatePayload(BlockBookActivatePacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      BlockBookActivatePacket.encode(packet, buf);
    }
  }

  public record PocketHeadSyncPayload(PocketHeadSyncPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "pocket_head_sync");
    public static PocketHeadSyncPayload read(FriendlyByteBuf buf) {
      return new PocketHeadSyncPayload(PocketHeadSyncPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      PocketHeadSyncPacket.encode(packet, buf);
    }
  }

  public record SyncAgeDataPayload(SyncAgeDataPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "sync_age_data");
    public static SyncAgeDataPayload read(FriendlyByteBuf buf) {
      return new SyncAgeDataPayload(SyncAgeDataPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      SyncAgeDataPacket.encode(packet, buf);
    }
  }

  public record LinkEffectPayload(LinkEffectPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "link_effect");
    public static LinkEffectPayload read(FriendlyByteBuf buf) {
      return new LinkEffectPayload(LinkEffectPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      LinkEffectPacket.encode(packet, buf);
    }
  }

  public record SymbolSyncPayload(SymbolSyncPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "symbol_sync");
    public static SymbolSyncPayload read(FriendlyByteBuf buf) {
      return new SymbolSyncPayload(SymbolSyncPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      SymbolSyncPacket.encode(packet, buf);
    }
  }

  public record ConfigSyncPayload(ConfigSyncPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "config_sync");
    public static ConfigSyncPayload read(FriendlyByteBuf buf) {
      return new ConfigSyncPayload(ConfigSyncPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      ConfigSyncPacket.encode(packet, buf);
    }
  }

  public record DimensionSyncPayload(DimensionSyncPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "dimension_sync");
    public static DimensionSyncPayload read(FriendlyByteBuf buf) {
      return new DimensionSyncPayload(DimensionSyncPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      DimensionSyncPacket.encode(packet, buf);
    }
  }

  public record ProfilingStatePayload(ProfilingStatePacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "profiling_state");
    public static ProfilingStatePayload read(FriendlyByteBuf buf) {
      return new ProfilingStatePayload(ProfilingStatePacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      ProfilingStatePacket.encode(packet, buf);
    }
  }

  public record ExplosionPayload(ExplosionPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "explosion");
    public static ExplosionPayload read(FriendlyByteBuf buf) {
      return new ExplosionPayload(ExplosionPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      ExplosionPacket.encode(packet, buf);
    }
  }

  public record SpawnLightningPayload(SpawnLightningPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "spawn_lightning");
    public static SpawnLightningPayload read(FriendlyByteBuf buf) {
      return new SpawnLightningPayload(SpawnLightningPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      SpawnLightningPacket.encode(packet, buf);
    }
  }

  public record LecternBookSyncPayload(LecternBookSyncPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "lectern_book_sync");
    public static LecternBookSyncPayload read(FriendlyByteBuf buf) {
      return new LecternBookSyncPayload(LecternBookSyncPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      LecternBookSyncPacket.encode(packet, buf);
    }
  }

  public record OpenLecternBookPayload(OpenLecternBookPacket packet) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "open_lectern_book");
    public static OpenLecternBookPayload read(FriendlyByteBuf buf) {
      return new OpenLecternBookPayload(OpenLecternBookPacket.decode(buf));
    }

    @Override
    public ResourceLocation id() {
      return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
      OpenLecternBookPacket.encode(packet, buf);
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
      return ctx.player().orElse(null);
    }

    @Override
    @Nullable
    public ServerPlayer getServerPlayer() {
      Player player = ctx.player().orElse(null);
      return player instanceof ServerPlayer sp ? sp : null;
    }

    @Override
    public boolean isClientSide() {
      return ctx.flow().isClientbound();
    }

    @Override
    public void enqueueWork(Runnable work) {
      ctx.workHandler().execute(work);
    }
  }
}
