package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import net.neoforged.neoforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
/**
 * Network handler for Mystcraft packets.
 * Uses SimpleChannel for client-server communication.
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

    /**
     * Wraps a common packet handler to work with NeoForge's context type.
     */
    private static <T> net.neoforged.neoforge.network.simple.MessageFunctions.MessageConsumer<T> wrap(BiConsumer<T, PacketContext> handler) {
        return (packet, ctx) -> handler.accept(packet, new NeoForgePacketContext(ctx));
    }

    /** Registers all packets. Call during mod setup. */
    public static void register() {
        // Client -> Server packets
        CHANNEL.messageBuilder(OpenBookPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenBookPacket::encode)
                .decoder(OpenBookPacket::decode)
                .consumerMainThread(wrap(OpenBookPacket::handle))
                .add();

        // Server -> Client packets
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

        // Client -> Server: Container actions
        CHANNEL.messageBuilder(ContainerActionPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
                .encoder(ContainerActionPacket::encode)
                .decoder(ContainerActionPacket::decode)
                .consumerMainThread(wrap(ContainerActionPacket::handle))
                .add();

        // Server -> Client: Config sync
        CHANNEL.messageBuilder(ConfigSyncPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigSyncPacket::encode)
                .decoder(ConfigSyncPacket::decode)
                .consumerMainThread(wrap(ConfigSyncPacket::handle))
                .add();

        // Server -> Client: Dimension sync
        CHANNEL.messageBuilder(DimensionSyncPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
                .encoder(DimensionSyncPacket::encode)
                .decoder(DimensionSyncPacket::decode)
                .consumerMainThread(wrap(DimensionSyncPacket::handle))
                .add();

        // Server -> Client: Profiling state
        CHANNEL.messageBuilder(ProfilingStatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProfilingStatePacket::encode)
                .decoder(ProfilingStatePacket::decode)
                .consumerMainThread(wrap(ProfilingStatePacket::handle))
                .add();

        // Server -> Client: Custom explosion
        CHANNEL.messageBuilder(ExplosionPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
                .encoder(ExplosionPacket::encode)
                .decoder(ExplosionPacket::decode)
                .consumerMainThread(wrap(ExplosionPacket::handle))
                .add();

        // Server -> Client: Spawn colored lightning
        CHANNEL.messageBuilder(SpawnLightningPacket.class, packetId++, PlayNetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpawnLightningPacket::encode)
                .decoder(SpawnLightningPacket::decode)
                .consumerMainThread(wrap(SpawnLightningPacket::handle))
                .add();

        // Client -> Server: Activate linking book from GUI (hand-based)
        CHANNEL.messageBuilder(LinkBookActivatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
                .encoder(LinkBookActivatePacket::encode)
                .decoder(LinkBookActivatePacket::decode)
                .consumerMainThread(wrap(LinkBookActivatePacket::handle))
                .add();

        // Client -> Server: Activate linking book from GUI (entity-based)
        CHANNEL.messageBuilder(EntityBookActivatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
                .encoder(EntityBookActivatePacket::encode)
                .decoder(EntityBookActivatePacket::decode)
                .consumerMainThread(wrap(EntityBookActivatePacket::handle))
                .add();

        // Client -> Server: Activate linking book from GUI (block entity-based: bookstand/lectern)
        CHANNEL.messageBuilder(BlockBookActivatePacket.class, packetId++, PlayNetworkDirection.PLAY_TO_SERVER)
                .encoder(BlockBookActivatePacket::encode)
                .decoder(BlockBookActivatePacket::decode)
                .consumerMainThread(wrap(BlockBookActivatePacket::handle))
                .add();

        Mystcraft.LOGGER.info("Registered {} network packets", packetId);
    }

    /** Sends a packet to the server. */
    public static void sendToServer(Object packet) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
    }

    /** Sends a packet to a specific player. */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Sends a packet to all players. */
    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    /** Sends a packet to all players tracking a position. */
    public static void sendToTracking(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }
}
