package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Network handler for Mystcraft packets on Forge 1.20.2.
 * Uses Forge's SimpleChannel for client-server communication.
 * <p>
 * Client-bound packets are handled specially to avoid loading client-dependent
 * classes on dedicated servers during registration.
 */
public final class ForgeMystcraftNetwork {

  private static final int PROTOCOL_VERSION = 1;

  public static final SimpleChannel CHANNEL = ChannelBuilder
      .named(new ResourceLocation(Mystcraft.MOD_ID, "main"))
      .networkProtocolVersion(PROTOCOL_VERSION)
      .clientAcceptedVersions((status, version) -> true)
      .serverAcceptedVersions((status, version) -> true)
      .simpleChannel();

  private static int packetId = 0;

  // Maps packet class names to their wrapper classes (for server-side sending)
  private static final Map<String, java.util.function.Function<Object, ServerPacketWrapper>> PACKET_WRAPPERS = new HashMap<>();

  private ForgeMystcraftNetwork() {
  }

  private static <T> BiConsumer<T, CustomPayloadEvent.Context> wrap(BiConsumer<T, PacketContext> handler) {
    return (packet, ctx) -> handler.accept(packet, new Context(ctx));
  }

  public static void register() {
    // Client -> Server packets (safe to register normally)
    CHANNEL.messageBuilder(OpenBookPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(OpenBookPacket::encode)
        .decoder(OpenBookPacket::decode)
        .consumerMainThread(wrap(OpenBookPacket::handle))
        .add();

    CHANNEL.messageBuilder(ContainerActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(ContainerActionPacket::encode)
        .decoder(ContainerActionPacket::decode)
        .consumerMainThread(wrap(ContainerActionPacket::handle))
        .add();

    CHANNEL.messageBuilder(LinkBookActivatePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(LinkBookActivatePacket::encode)
        .decoder(LinkBookActivatePacket::decode)
        .consumerMainThread(wrap(LinkBookActivatePacket::handle))
        .add();

    CHANNEL.messageBuilder(EntityBookActivatePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(EntityBookActivatePacket::encode)
        .decoder(EntityBookActivatePacket::decode)
        .consumerMainThread(wrap(EntityBookActivatePacket::handle))
        .add();

    CHANNEL.messageBuilder(BlockBookActivatePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(BlockBookActivatePacket::encode)
        .decoder(BlockBookActivatePacket::decode)
        .consumerMainThread(wrap(BlockBookActivatePacket::handle))
        .add();

    CHANNEL.messageBuilder(PocketHeadSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
        .encoder(PocketHeadSyncPacket::encode)
        .decoder(PocketHeadSyncPacket::decode)
        .consumerMainThread(wrap(PocketHeadSyncPacket::handle))
        .add();

    // Server -> Client packets
    // On dedicated server: register with unique wrapper classes to avoid loading client classes
    // On client: register normally with full handler
    registerClientBoundPacket("art.arcane.mystcraft.network.SyncAgeDataPacket", W0.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.LinkEffectPacket", W1.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.SymbolSyncPacket", W2.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.ConfigSyncPacket", W3.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.DimensionSyncPacket", W4.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.ProfilingStatePacket", W5.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.ExplosionPacket", W6.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.SpawnLightningPacket", W7.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.LecternBookSyncPacket", W8.class);
    registerClientBoundPacket("art.arcane.mystcraft.network.OpenLecternBookPacket", W9.class);

    Mystcraft.LOGGER.info("Registered {} network packets", packetId);
  }

  /**
   * Registers a client-bound packet.
   * On dedicated servers, uses a unique wrapper class to avoid loading the actual packet class.
   * On clients, loads the class and registers it normally via reflection.
   */
  @SuppressWarnings("unchecked")
  private static <W extends ServerPacketWrapper> void registerClientBoundPacket(String className, Class<W> wrapperClass) {
    int id = packetId++;
    // Store the wrapper constructor for server-side sending
    PACKET_WRAPPERS.put(className, packet -> {
      try {
        return wrapperClass.getConstructor(Object.class).newInstance(packet);
      } catch (ReflectiveOperationException e) {
        Mystcraft.LOGGER.error("[Network] Failed to create wrapper", e);
        return new ServerPacketWrapper(packet);
      }
    });

    if (FMLEnvironment.dist.isDedicatedServer()) {
      // On dedicated server: register unique wrapper class for each packet
      Function<FriendlyByteBuf, W> decoder = buf -> {
        try {
          return wrapperClass.getConstructor(Object.class).newInstance((Object) null);
        } catch (ReflectiveOperationException e) {
          Mystcraft.LOGGER.error("[Network] Failed to create wrapper for {}", className, e);
          return null;
        }
      };
      CHANNEL.messageBuilder(wrapperClass, id, NetworkDirection.PLAY_TO_CLIENT)
          .encoder((wrapper, buf) -> wrapper.encode(buf))
          .decoder(decoder)
          .consumerMainThread((wrapper, ctx) -> {}) // No-op on server
          .add();
    } else {
      // On client: register the actual packet class
      try {
        Class<?> packetClass = Class.forName(className);

        java.lang.reflect.Method encodeMethod = packetClass.getMethod("encode", packetClass, FriendlyByteBuf.class);
        java.lang.reflect.Method decodeMethod = packetClass.getMethod("decode", FriendlyByteBuf.class);

        BiConsumer<Object, FriendlyByteBuf> encoder = (packet, buf) -> {
          try {
            encodeMethod.invoke(null, packet, buf);
          } catch (ReflectiveOperationException e) {
            Mystcraft.LOGGER.error("[Network] Failed to encode {}", className, e);
          }
        };

        Function<FriendlyByteBuf, Object> decoder = buf -> {
          try {
            return decodeMethod.invoke(null, buf);
          } catch (ReflectiveOperationException e) {
            Mystcraft.LOGGER.error("[Network] Failed to decode {}", className, e);
            return null;
          }
        };

        BiConsumer<Object, CustomPayloadEvent.Context> handler = (packet, ctx) -> {
          if (!ctx.isClientSide()) {
            return;
          }
          ctx.enqueueWork(() -> {
            try {
              java.lang.reflect.Method handleMethod = packetClass.getMethod("handle", packetClass, PacketContext.class);
              handleMethod.invoke(null, packet, new Context(ctx));
            } catch (ReflectiveOperationException e) {
              Mystcraft.LOGGER.error("[Network] Failed to handle {}", className, e);
            }
          });
        };

        CHANNEL.messageBuilder((Class<Object>) packetClass, id, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(encoder)
            .decoder(decoder)
            .consumerMainThread(handler)
            .add();
      } catch (ReflectiveOperationException e) {
        Mystcraft.LOGGER.error("[Network] Failed to register {}", className, e);
      }
    }
  }

  public static void sendToServer(Object packet) {
    CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
  }

  public static void sendToPlayer(Object packet, ServerPlayer player) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(toSend, PacketDistributor.PLAYER.with(player));
  }

  public static void sendToAll(Object packet) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(toSend, PacketDistributor.ALL.noArg());
  }

  public static void sendToTracking(Object packet, ServerPlayer player) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(toSend, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
  }

  public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(toSend, PacketDistributor.TRACKING_CHUNK.with(level.getChunkAt(pos)));
  }

  /**
   * Wraps a packet in its corresponding wrapper class if we're on a dedicated server.
   */
  private static Object wrapForServer(Object packet) {
    if (FMLEnvironment.dist.isDedicatedServer()) {
      String className = packet.getClass().getName();
      java.util.function.Function<Object, ServerPacketWrapper> wrapperFactory = PACKET_WRAPPERS.get(className);
      if (wrapperFactory != null) {
        return wrapperFactory.apply(packet);
      }
    }
    return packet;
  }

  /**
   * Base wrapper class for client-bound packets on dedicated servers.
   */
  public static class ServerPacketWrapper {
    public final Object packet;
    private java.lang.reflect.Method encodeMethod;

    public ServerPacketWrapper(Object packet) {
      this.packet = packet;
    }

    public void encode(FriendlyByteBuf buf) {
      if (packet == null) return;
      try {
        if (encodeMethod == null) {
          Class<?> packetClass = packet.getClass();
          encodeMethod = packetClass.getMethod("encode", packetClass, FriendlyByteBuf.class);
        }
        encodeMethod.invoke(null, packet, buf);
      } catch (ReflectiveOperationException e) {
        Mystcraft.LOGGER.error("[Network] Failed to encode wrapped packet {}", packet.getClass().getName(), e);
      }
    }
  }

  // Unique wrapper classes for each client-bound packet (required by Forge 1.20.2's SimpleChannel)
  public static class W0 extends ServerPacketWrapper { public W0(Object p) { super(p); } }
  public static class W1 extends ServerPacketWrapper { public W1(Object p) { super(p); } }
  public static class W2 extends ServerPacketWrapper { public W2(Object p) { super(p); } }
  public static class W3 extends ServerPacketWrapper { public W3(Object p) { super(p); } }
  public static class W4 extends ServerPacketWrapper { public W4(Object p) { super(p); } }
  public static class W5 extends ServerPacketWrapper { public W5(Object p) { super(p); } }
  public static class W6 extends ServerPacketWrapper { public W6(Object p) { super(p); } }
  public static class W7 extends ServerPacketWrapper { public W7(Object p) { super(p); } }
  public static class W8 extends ServerPacketWrapper { public W8(Object p) { super(p); } }
  public static class W9 extends ServerPacketWrapper { public W9(Object p) { super(p); } }

  /**
   * Adapts Forge's CustomPayloadEvent.Context to the common PacketContext interface.
   */
  public static class Context implements PacketContext {
    private final CustomPayloadEvent.Context ctx;

    public Context(CustomPayloadEvent.Context ctx) {
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
}
