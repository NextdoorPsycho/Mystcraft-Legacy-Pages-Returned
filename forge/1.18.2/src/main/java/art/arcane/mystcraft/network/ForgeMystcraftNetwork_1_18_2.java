package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Network handler for Mystcraft packets on Forge 1.18.2.
 * Uses Forge's SimpleChannel for client-server communication.
 * <p>
 * This is the 1.18.2-specific implementation using the old NetworkRegistry API.
 */
public final class ForgeMystcraftNetwork_1_18_2 {

  private static final String PROTOCOL_VERSION = "1";

  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation(Mystcraft.MOD_ID, "main"),
      () -> PROTOCOL_VERSION,
      PROTOCOL_VERSION::equals,
      PROTOCOL_VERSION::equals
  );

  private static int packetId = 0;

  private ForgeMystcraftNetwork_1_18_2() {
  }

  /**
   * Wraps a common packet handler to work with Forge's context type.
   */
  private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> wrap(BiConsumer<T, PacketContext> handler) {
    return (packet, ctxSupplier) -> {
      NetworkEvent.Context ctx = ctxSupplier.get();
      handler.accept(packet, new ForgePacketContext_1_18_2(ctx));
      ctx.setPacketHandled(true);
    };
  }

  /**
   * Registers all packets. Call this during mod setup.
   */
  public static void register() {
    // Client -> Server packets
    CHANNEL.registerMessage(packetId++, OpenBookPacket.class,
        OpenBookPacket::encode, OpenBookPacket::decode, wrap(OpenBookPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    // Server -> Client packets
    CHANNEL.registerMessage(packetId++, SyncAgeDataPacket.class,
        SyncAgeDataPacket::encode, SyncAgeDataPacket::decode, wrap(SyncAgeDataPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    CHANNEL.registerMessage(packetId++, LinkEffectPacket.class,
        LinkEffectPacket::encode, LinkEffectPacket::decode, wrap(LinkEffectPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    // 1.18.2: SymbolSyncPacket not ported
    // CHANNEL.registerMessage(packetId++, SymbolSyncPacket.class,
    //     SymbolSyncPacket::encode, SymbolSyncPacket::decode, wrap(SymbolSyncPacket::handle),
    //     Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    // Client -> Server: Container actions
    CHANNEL.registerMessage(packetId++, ContainerActionPacket.class,
        ContainerActionPacket::encode, ContainerActionPacket::decode, wrap(ContainerActionPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    // 1.18.2: ConfigSyncPacket, DimensionSyncPacket, ProfilingStatePacket, ExplosionPacket, SpawnLightningPacket not ported
    // CHANNEL.registerMessage(packetId++, ConfigSyncPacket.class,
    //     ConfigSyncPacket::encode, ConfigSyncPacket::decode, wrap(ConfigSyncPacket::handle),
    //     Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    // CHANNEL.registerMessage(packetId++, DimensionSyncPacket.class,
    //     DimensionSyncPacket::encode, DimensionSyncPacket::decode, wrap(DimensionSyncPacket::handle),
    //     Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    // CHANNEL.registerMessage(packetId++, ProfilingStatePacket.class,
    //     ProfilingStatePacket::encode, ProfilingStatePacket::decode, wrap(ProfilingStatePacket::handle),
    //     Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    // CHANNEL.registerMessage(packetId++, ExplosionPacket.class,
    //     ExplosionPacket::encode, ExplosionPacket::decode, wrap(ExplosionPacket::handle),
    //     Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    // CHANNEL.registerMessage(packetId++, SpawnLightningPacket.class,
    //     SpawnLightningPacket::encode, SpawnLightningPacket::decode, wrap(SpawnLightningPacket::handle),
    //     Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    // Client -> Server: Activate linking book from GUI (hand-based)
    CHANNEL.registerMessage(packetId++, LinkBookActivatePacket.class,
        LinkBookActivatePacket::encode, LinkBookActivatePacket::decode, wrap(LinkBookActivatePacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    // Client -> Server: Activate linking book from GUI (entity-based)
    CHANNEL.registerMessage(packetId++, EntityBookActivatePacket.class,
        EntityBookActivatePacket::encode, EntityBookActivatePacket::decode, wrap(EntityBookActivatePacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    // Client -> Server: Activate linking book from GUI (block entity-based: bookstand/lectern)
    CHANNEL.registerMessage(packetId++, BlockBookActivatePacket.class,
        BlockBookActivatePacket::encode, BlockBookActivatePacket::decode, wrap(BlockBookActivatePacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_SERVER));

    // 1.18.2: PocketHeadSyncPacket not ported
    // CHANNEL.registerMessage(packetId++, PocketHeadSyncPacket.class,
    //     PocketHeadSyncPacket::encode, PocketHeadSyncPacket::decode, wrap(PocketHeadSyncPacket::handle),
    //     Optional.of(NetworkDirection.PLAY_TO_SERVER));

    // Server -> Client: Sync Mystcraft book in vanilla lectern
    CHANNEL.registerMessage(packetId++, LecternBookSyncPacket.class,
        LecternBookSyncPacket::encode, LecternBookSyncPacket::decode, wrap(LecternBookSyncPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    // Server -> Client: Open book screen for lectern
    CHANNEL.registerMessage(packetId++, OpenLecternBookPacket.class,
        OpenLecternBookPacket::encode, OpenLecternBookPacket::decode, wrap(OpenLecternBookPacket::handle),
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    Mystcraft.LOGGER.info("Registered {} network packets", packetId);
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
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
  }

  /**
   * Sends a packet to all players.
   */
  public static void sendToAll(Object packet) {
    CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
  }

  /**
   * Sends a packet to all players tracking a position.
   */
  public static void sendToTracking(Object packet, ServerPlayer player) {
    CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
  }

  /**
   * Sends a packet to all players tracking a block position.
   */
  public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
    CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);
  }
}
