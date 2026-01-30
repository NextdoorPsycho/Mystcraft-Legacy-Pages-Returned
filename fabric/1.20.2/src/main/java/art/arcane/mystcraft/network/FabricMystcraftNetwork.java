package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Fabric networking implementation for Mystcraft packets.
 * Registers channel IDs and provides send helpers for all packet types.
 */
public final class FabricMystcraftNetwork {

  // --- Channel IDs ---

  // Client -> Server
  public static final ResourceLocation OPEN_BOOK = new ResourceLocation(Mystcraft.MOD_ID, "open_book");
  public static final ResourceLocation CONTAINER_ACTION = new ResourceLocation(Mystcraft.MOD_ID, "container_action");
  public static final ResourceLocation LINK_BOOK_ACTIVATE = new ResourceLocation(Mystcraft.MOD_ID, "link_book_activate");
  public static final ResourceLocation ENTITY_BOOK_ACTIVATE = new ResourceLocation(Mystcraft.MOD_ID, "entity_book_activate");
  public static final ResourceLocation BLOCK_BOOK_ACTIVATE = new ResourceLocation(Mystcraft.MOD_ID, "block_book_activate");

  // Server -> Client
  public static final ResourceLocation SYNC_AGE_DATA = new ResourceLocation(Mystcraft.MOD_ID, "sync_age_data");
  public static final ResourceLocation LINK_EFFECT = new ResourceLocation(Mystcraft.MOD_ID, "link_effect");
  public static final ResourceLocation SYMBOL_SYNC = new ResourceLocation(Mystcraft.MOD_ID, "symbol_sync");
  public static final ResourceLocation CONFIG_SYNC = new ResourceLocation(Mystcraft.MOD_ID, "config_sync");
  public static final ResourceLocation DIMENSION_SYNC = new ResourceLocation(Mystcraft.MOD_ID, "dimension_sync");
  public static final ResourceLocation PROFILING_STATE = new ResourceLocation(Mystcraft.MOD_ID, "profiling_state");
  public static final ResourceLocation EXPLOSION = new ResourceLocation(Mystcraft.MOD_ID, "explosion");
  public static final ResourceLocation SPAWN_LIGHTNING = new ResourceLocation(Mystcraft.MOD_ID, "spawn_lightning");
  public static final ResourceLocation LECTERN_BOOK_SYNC = new ResourceLocation(Mystcraft.MOD_ID, "lectern_book_sync");
  public static final ResourceLocation OPEN_LECTERN_BOOK = new ResourceLocation(Mystcraft.MOD_ID, "open_lectern_book");

  private FabricMystcraftNetwork() {
  }

  /**
   * Registers all server-side packet receivers. Call from MystcraftFabric.onInitialize().
   */
  public static void register() {
    // Client -> Server receivers
    ServerPlayNetworking.registerGlobalReceiver(OPEN_BOOK, (server, player, handler, buf, responseSender) -> {
      OpenBookPacket packet = OpenBookPacket.decode(buf);
      PacketContext ctx = createServerContext(server, player);
      OpenBookPacket.handle(packet, ctx);
    });

    ServerPlayNetworking.registerGlobalReceiver(CONTAINER_ACTION, (server, player, handler, buf, responseSender) -> {
      ContainerActionPacket packet = ContainerActionPacket.decode(buf);
      PacketContext ctx = createServerContext(server, player);
      ContainerActionPacket.handle(packet, ctx);
    });

    ServerPlayNetworking.registerGlobalReceiver(LINK_BOOK_ACTIVATE, (server, player, handler, buf, responseSender) -> {
      LinkBookActivatePacket packet = LinkBookActivatePacket.decode(buf);
      PacketContext ctx = createServerContext(server, player);
      LinkBookActivatePacket.handle(packet, ctx);
    });

    ServerPlayNetworking.registerGlobalReceiver(ENTITY_BOOK_ACTIVATE, (server, player, handler, buf, responseSender) -> {
      EntityBookActivatePacket packet = EntityBookActivatePacket.decode(buf);
      PacketContext ctx = createServerContext(server, player);
      EntityBookActivatePacket.handle(packet, ctx);
    });

    ServerPlayNetworking.registerGlobalReceiver(BLOCK_BOOK_ACTIVATE, (server, player, handler, buf, responseSender) -> {
      BlockBookActivatePacket packet = BlockBookActivatePacket.decode(buf);
      PacketContext ctx = createServerContext(server, player);
      BlockBookActivatePacket.handle(packet, ctx);
    });

    Mystcraft.LOGGER.info("[FabricMystcraftNetwork] Registered 13 network channels");
  }

  /**
   * Registers client-side packet receivers. Call from MystcraftFabricClient.onInitializeClient().
   */
  public static void registerClient() {
    ClientPlayNetworking.registerGlobalReceiver(SYNC_AGE_DATA, (client, handler, buf, responseSender) -> {
      SyncAgeDataPacket packet = SyncAgeDataPacket.decode(buf);
      PacketContext ctx = createClientContext();
      SyncAgeDataPacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(LINK_EFFECT, (client, handler, buf, responseSender) -> {
      LinkEffectPacket packet = LinkEffectPacket.decode(buf);
      PacketContext ctx = createClientContext();
      LinkEffectPacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(SYMBOL_SYNC, (client, handler, buf, responseSender) -> {
      SymbolSyncPacket packet = SymbolSyncPacket.decode(buf);
      PacketContext ctx = createClientContext();
      packet.handle(ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(CONFIG_SYNC, (client, handler, buf, responseSender) -> {
      ConfigSyncPacket packet = ConfigSyncPacket.decode(buf);
      PacketContext ctx = createClientContext();
      ConfigSyncPacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(DIMENSION_SYNC, (client, handler, buf, responseSender) -> {
      DimensionSyncPacket packet = DimensionSyncPacket.decode(buf);
      PacketContext ctx = createClientContext();
      DimensionSyncPacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(PROFILING_STATE, (client, handler, buf, responseSender) -> {
      ProfilingStatePacket packet = ProfilingStatePacket.decode(buf);
      PacketContext ctx = createClientContext();
      ProfilingStatePacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(EXPLOSION, (client, handler, buf, responseSender) -> {
      ExplosionPacket packet = ExplosionPacket.decode(buf);
      PacketContext ctx = createClientContext();
      ExplosionPacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(SPAWN_LIGHTNING, (client, handler, buf, responseSender) -> {
      SpawnLightningPacket packet = SpawnLightningPacket.decode(buf);
      PacketContext ctx = createClientContext();
      SpawnLightningPacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(LECTERN_BOOK_SYNC, (client, handler, buf, responseSender) -> {
      LecternBookSyncPacket packet = LecternBookSyncPacket.decode(buf);
      PacketContext ctx = createClientContext();
      LecternBookSyncPacket.handle(packet, ctx);
    });

    ClientPlayNetworking.registerGlobalReceiver(OPEN_LECTERN_BOOK, (client, handler, buf, responseSender) -> {
      OpenLecternBookPacket packet = OpenLecternBookPacket.decode(buf);
      PacketContext ctx = createClientContext();
      OpenLecternBookPacket.handle(packet, ctx);
    });
  }

  // --- Send helpers ---

  /**
   * Sends a C->S OpenBookPacket.
   */
  public static void sendToServer(OpenBookPacket packet) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    OpenBookPacket.encode(packet, buf);
    ClientPlayNetworking.send(OPEN_BOOK, buf);
  }

  /**
   * Sends a C->S ContainerActionPacket.
   */
  public static void sendToServer(ContainerActionPacket packet) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    ContainerActionPacket.encode(packet, buf);
    ClientPlayNetworking.send(CONTAINER_ACTION, buf);
  }

  /**
   * Sends a C->S LinkBookActivatePacket.
   */
  public static void sendToServer(LinkBookActivatePacket packet) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    LinkBookActivatePacket.encode(packet, buf);
    ClientPlayNetworking.send(LINK_BOOK_ACTIVATE, buf);
  }

  /**
   * Sends a C->S EntityBookActivatePacket.
   */
  public static void sendToServer(EntityBookActivatePacket packet) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    EntityBookActivatePacket.encode(packet, buf);
    ClientPlayNetworking.send(ENTITY_BOOK_ACTIVATE, buf);
  }

  /**
   * Sends a C->S BlockBookActivatePacket.
   */
  public static void sendToServer(BlockBookActivatePacket packet) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    BlockBookActivatePacket.encode(packet, buf);
    ClientPlayNetworking.send(BLOCK_BOOK_ACTIVATE, buf);
  }

  /**
   * Sends a SyncAgeDataPacket to a specific player.
   */
  public static void sendToPlayer(SyncAgeDataPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    SyncAgeDataPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, SYNC_AGE_DATA, buf);
  }

  /**
   * Sends a LinkEffectPacket to a specific player.
   */
  public static void sendToPlayer(LinkEffectPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    LinkEffectPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, LINK_EFFECT, buf);
  }

  /**
   * Sends a SymbolSyncPacket to a specific player.
   */
  public static void sendToPlayer(SymbolSyncPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    packet.encode(buf);
    ServerPlayNetworking.send(player, SYMBOL_SYNC, buf);
  }

  /**
   * Sends a ConfigSyncPacket to a specific player.
   */
  public static void sendToPlayer(ConfigSyncPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    ConfigSyncPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, CONFIG_SYNC, buf);
  }

  /**
   * Sends a DimensionSyncPacket to a specific player.
   */
  public static void sendToPlayer(DimensionSyncPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    DimensionSyncPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, DIMENSION_SYNC, buf);
  }

  /**
   * Sends a ProfilingStatePacket to a specific player.
   */
  public static void sendToPlayer(ProfilingStatePacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    ProfilingStatePacket.encode(packet, buf);
    ServerPlayNetworking.send(player, PROFILING_STATE, buf);
  }

  /**
   * Sends an ExplosionPacket to a specific player.
   */
  public static void sendToPlayer(ExplosionPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    ExplosionPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, EXPLOSION, buf);
  }

  /**
   * Sends a SpawnLightningPacket to a specific player.
   */
  public static void sendToPlayer(SpawnLightningPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    SpawnLightningPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, SPAWN_LIGHTNING, buf);
  }

  /**
   * Sends a LecternBookSyncPacket to a specific player.
   */
  public static void sendToPlayer(LecternBookSyncPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    LecternBookSyncPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, LECTERN_BOOK_SYNC, buf);
  }

  /**
   * Sends an OpenLecternBookPacket to a specific player.
   */
  public static void sendToPlayer(OpenLecternBookPacket packet, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    OpenLecternBookPacket.encode(packet, buf);
    ServerPlayNetworking.send(player, OPEN_LECTERN_BOOK, buf);
  }

  /**
   * Sends a packet to all players tracking a block position.
   */
  public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
    for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
      sendToPlayerGeneric(packet, player);
    }
  }

  /**
   * Sends a packet to all connected players.
   */
  public static void sendToAll(Object packet, MinecraftServer server) {
    for (ServerPlayer player : PlayerLookup.all(server)) {
      sendToPlayerGeneric(packet, player);
    }
  }

  /**
   * Sends a packet to all players tracking a given entity.
   */
  public static void sendToTracking(Object packet, Entity entity) {
    for (ServerPlayer player : PlayerLookup.tracking(entity)) {
      sendToPlayerGeneric(packet, player);
    }
    // Also send to the entity itself if it's a player
    if (entity instanceof ServerPlayer self) {
      sendToPlayerGeneric(packet, self);
    }
  }

  /**
   * Routes a generic packet object to the correct typed sendToServer overload.
   */
  public static void sendToServerGeneric(Object packet) {
    if (packet instanceof OpenBookPacket p) sendToServer(p);
    else if (packet instanceof ContainerActionPacket p) sendToServer(p);
    else if (packet instanceof LinkBookActivatePacket p) sendToServer(p);
    else if (packet instanceof EntityBookActivatePacket p) sendToServer(p);
    else if (packet instanceof BlockBookActivatePacket p) sendToServer(p);
    else {
      Mystcraft.LOGGER.warn("[FabricMystcraftNetwork] Unknown C->S packet type: {}", packet.getClass().getName());
    }
  }

  /**
   * Routes a generic packet object to the correct typed sendToPlayer overload.
   */
  public static void sendToPlayerGeneric(Object packet, ServerPlayer player) {
    if (packet instanceof SyncAgeDataPacket p) sendToPlayer(p, player);
    else if (packet instanceof LinkEffectPacket p) sendToPlayer(p, player);
    else if (packet instanceof SymbolSyncPacket p) sendToPlayer(p, player);
    else if (packet instanceof ConfigSyncPacket p) sendToPlayer(p, player);
    else if (packet instanceof DimensionSyncPacket p) sendToPlayer(p, player);
    else if (packet instanceof ProfilingStatePacket p) sendToPlayer(p, player);
    else if (packet instanceof ExplosionPacket p) sendToPlayer(p, player);
    else if (packet instanceof SpawnLightningPacket p) sendToPlayer(p, player);
    else if (packet instanceof LecternBookSyncPacket p) sendToPlayer(p, player);
    else if (packet instanceof OpenLecternBookPacket p) sendToPlayer(p, player);
    else {
      Mystcraft.LOGGER.warn("[FabricMystcraftNetwork] Unknown packet type: {}", packet.getClass().getName());
    }
  }

  // --- PacketContext implementations ---

  private static PacketContext createServerContext(MinecraftServer server, ServerPlayer player) {
    return new PacketContext() {
      @Override
      public net.minecraft.world.entity.player.Player getPlayer() {
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
    };
  }

  private static PacketContext createClientContext() {
    return new PacketContext() {
      @Override
      public net.minecraft.world.entity.player.Player getPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
      }

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
        net.minecraft.client.Minecraft.getInstance().execute(work);
      }
    };
  }
}
