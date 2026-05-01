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
 * Network handler for Mystcraft packets on Forge 1.20.1. Uses Forge's
 * SimpleChannel for client-server communication.
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
  private static final Map<String, Integer> CLIENT_BOUND_PACKET_IDS = new HashMap<>();
  private static int packetId = 0;

  private ForgeMystcraftNetwork_1_20_1() {
  }

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

  @SuppressWarnings("unchecked")
  private static void registerClientBoundPacket(String className) {
    int id = packetId++;
    CLIENT_BOUND_PACKET_IDS.put(className, id);

    if (FMLEnvironment.dist.isDedicatedServer()) {

      CHANNEL.registerMessage(id, ServerPacketWrapper.class,
          (wrapper, buf) -> wrapper.encode(buf),
          buf -> new ServerPacketWrapper(className, null),
          (wrapper, ctxSupplier) -> ctxSupplier.get().setPacketHandled(true),
          Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    } else {

      try {
        Class<?> packetClass = Class.forName(className);

        java.lang.reflect.Method encodeMethod = packetClass.getMethod("encode", packetClass, FriendlyByteBuf.class);
        java.lang.reflect.Method decodeMethod = packetClass.getMethod("decode", FriendlyByteBuf.class);

        BiConsumer<Object, FriendlyByteBuf> encoder = (packet, buf) -> {
          try {

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
   * Wrapper class for client-bound packets on dedicated servers. Allows
   * encoding packets without loading the original packet class during
   * registration.
   */
  public static class ServerPacketWrapper {
    public final Object packet;
    private final String className;
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
