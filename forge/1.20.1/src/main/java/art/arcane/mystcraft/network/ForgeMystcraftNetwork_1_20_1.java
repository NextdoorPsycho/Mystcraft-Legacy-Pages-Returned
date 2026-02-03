package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Network handler for Mystcraft packets on Forge 1.20.1.
 * Uses Forge's SimpleChannel for client-server communication.
 * <p>
 * Client-bound packets are handled specially to avoid loading client-dependent
 * classes on dedicated servers during registration.
 */
public final class ForgeMystcraftNetwork_1_20_1 {

  private static final String PROTOCOL_VERSION = "1";

  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation(Mystcraft.MOD_ID, "main"),
      () -> PROTOCOL_VERSION,
      PROTOCOL_VERSION::equals,
      PROTOCOL_VERSION::equals
  );

  private static int packetId = 0;

  // Maps packet class names to their registered IDs (for server-side sending)
  private static final Map<String, Integer> CLIENT_BOUND_PACKET_IDS = new HashMap<>();

  private ForgeMystcraftNetwork_1_20_1() {
  }

  /**
   * Wraps a common packet handler to work with Forge's context type.
   */
  private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> wrap(BiConsumer<T, PacketContext> handler) {
    return (packet, ctxSupplier) -> {
      NetworkEvent.Context ctx = ctxSupplier.get();
      handler.accept(packet, new ForgePacketContext_1_20_1(ctx));
      ctx.setPacketHandled(true);
    };
  }

  /**
   * Registers all packets. Call this during mod common setup.
   */
  public static void register() {
    // Client -> Server packets (safe to register normally)
    CHANNEL.registerMessage(packetId++, OpenBookPacket.class,
        OpenBookPacket::encode, OpenBookPacket::decode, wrap(OpenBookPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    CHANNEL.registerMessage(packetId++, ContainerActionPacket.class,
        ContainerActionPacket::encode, ContainerActionPacket::decode, wrap(ContainerActionPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    CHANNEL.registerMessage(packetId++, LinkBookActivatePacket.class,
        LinkBookActivatePacket::encode, LinkBookActivatePacket::decode, wrap(LinkBookActivatePacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    CHANNEL.registerMessage(packetId++, EntityBookActivatePacket.class,
        EntityBookActivatePacket::encode, EntityBookActivatePacket::decode, wrap(EntityBookActivatePacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    CHANNEL.registerMessage(packetId++, BlockBookActivatePacket.class,
        BlockBookActivatePacket::encode, BlockBookActivatePacket::decode, wrap(BlockBookActivatePacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    CHANNEL.registerMessage(packetId++, PocketHeadSyncPacket.class,
        PocketHeadSyncPacket::encode, PocketHeadSyncPacket::decode, wrap(PocketHeadSyncPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    // Server -> Client packets
    // On dedicated server: register with ServerPacketWrapper to avoid loading client classes
    // On client: register normally with full handler
    registerClientBoundPacket("art.arcane.mystcraft.network.SyncAgeDataPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.LinkEffectPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.SymbolSyncPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.ConfigSyncPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.DimensionSyncPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.ProfilingStatePacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.ExplosionPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.SpawnLightningPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.LecternBookSyncPacket");
    registerClientBoundPacket("art.arcane.mystcraft.network.OpenLecternBookPacket");

    Mystcraft.LOGGER.info("Registered {} network packets", packetId);
  }

  /**
   * Registers a client-bound packet.
   * On dedicated servers, uses a wrapper class to avoid loading the actual packet class.
   * On clients, loads the class and registers it normally.
   */
  @SuppressWarnings("unchecked")
  private static void registerClientBoundPacket(String className) {
    int id = packetId++;
    CLIENT_BOUND_PACKET_IDS.put(className, id);

    if (FMLEnvironment.dist.isDedicatedServer()) {
      // On dedicated server: register ServerPacketWrapper
      // The wrapper uses reflection for encoding and doesn't load the packet class now
      CHANNEL.registerMessage(id, ServerPacketWrapper.class,
          (wrapper, buf) -> wrapper.encode(buf),
          buf -> new ServerPacketWrapper(className, null), // Decode is never called on server
          (wrapper, ctxSupplier) -> ctxSupplier.get().setPacketHandled(true),
          Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    } else {
      // On client: register the actual packet class
      try {
        Class<?> packetClass = Class.forName(className);

        java.lang.reflect.Method encodeMethod = packetClass.getMethod("encode", packetClass, FriendlyByteBuf.class);
        java.lang.reflect.Method decodeMethod = packetClass.getMethod("decode", FriendlyByteBuf.class);

        BiConsumer<Object, FriendlyByteBuf> encoder = (packet, buf) -> {
          try {
            // Handle both direct packets and wrapped packets
            Object actualPacket = packet instanceof ServerPacketWrapper ? ((ServerPacketWrapper) packet).packet : packet;
            encodeMethod.invoke(null, actualPacket, buf);
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

        BiConsumer<Object, Supplier<NetworkEvent.Context>> handler = (packet, ctxSupplier) -> {
          NetworkEvent.Context ctx = ctxSupplier.get();
          ctx.setPacketHandled(true);
          if (!ctx.getDirection().getReceptionSide().isClient()) {
            return;
          }
          ctx.enqueueWork(() -> {
            try {
              java.lang.reflect.Method handleMethod = packetClass.getMethod("handle", packetClass, PacketContext.class);
              handleMethod.invoke(null, packet, new ForgePacketContext_1_20_1(ctx));
            } catch (ReflectiveOperationException e) {
              Mystcraft.LOGGER.error("[Network] Failed to handle {}", className, e);
            }
          });
        };

        CHANNEL.registerMessage(id, (Class<Object>) packetClass, encoder, decoder, handler,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
      } catch (ReflectiveOperationException e) {
        Mystcraft.LOGGER.error("[Network] Failed to register {}", className, e);
      }
    }
  }

  /**
   * Sends a packet to the server.
   */
  public static void sendToServer(Object packet) {
    CHANNEL.sendToServer(packet);
  }

  /**
   * Sends a packet to a specific player.
   */
  public static void sendToPlayer(Object packet, ServerPlayer player) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), toSend);
  }

  /**
   * Sends a packet to all players.
   */
  public static void sendToAll(Object packet) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(PacketDistributor.ALL.noArg(), toSend);
  }

  /**
   * Sends a packet to all players tracking a position.
   */
  public static void sendToTracking(Object packet, ServerPlayer player) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), toSend);
  }

  /**
   * Sends a packet to all players tracking a block position.
   */
  public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
    Object toSend = wrapForServer(packet);
    CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), toSend);
  }

  /**
   * Wraps a packet in ServerPacketWrapper if we're on a dedicated server.
   * This allows the packet to be sent without the original class being registered.
   */
  private static Object wrapForServer(Object packet) {
    if (FMLEnvironment.dist.isDedicatedServer()) {
      String className = packet.getClass().getName();
      if (CLIENT_BOUND_PACKET_IDS.containsKey(className)) {
        return new ServerPacketWrapper(className, packet);
      }
    }
    return packet;
  }

  /**
   * Wrapper class for client-bound packets on dedicated servers.
   * Allows encoding packets without loading the original packet class during registration.
   */
  public static class ServerPacketWrapper {
    private final String className;
    public final Object packet;
    private java.lang.reflect.Method encodeMethod;

    public ServerPacketWrapper(String className, Object packet) {
      this.className = className;
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
        Mystcraft.LOGGER.error("[Network] Failed to encode wrapped packet {}", className, e);
      }
    }
  }
}
