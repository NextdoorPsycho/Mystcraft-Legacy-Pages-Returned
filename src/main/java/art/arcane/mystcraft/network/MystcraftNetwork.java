package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Network handler for Mystcraft packets.
 * Uses Forge's SimpleChannel for client-server communication.
 */
public final class MystcraftNetwork {

    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(new ResourceLocation(Mystcraft.MOD_ID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

    private static int packetId = 0;

    private MystcraftNetwork() {
    }

    /**
     * Registers all packets. Call this during mod setup.
     */
    public static void register() {
        // Client -> Server packets
        CHANNEL.messageBuilder(OpenBookPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenBookPacket::encode)
                .decoder(OpenBookPacket::decode)
                .consumerMainThread(OpenBookPacket::handle)
                .add();

        // Server -> Client packets
        CHANNEL.messageBuilder(SyncAgeDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAgeDataPacket::encode)
                .decoder(SyncAgeDataPacket::decode)
                .consumerMainThread(SyncAgeDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(LinkEffectPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LinkEffectPacket::encode)
                .decoder(LinkEffectPacket::decode)
                .consumerMainThread(LinkEffectPacket::handle)
                .add();

        CHANNEL.messageBuilder(SymbolSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SymbolSyncPacket::encode)
                .decoder(SymbolSyncPacket::decode)
                .consumerMainThread(SymbolSyncPacket::handle)
                .add();

        Mystcraft.LOGGER.info("Registered {} network packets", packetId);
    }

    /**
     * Sends a packet to the server.
     */
    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    /**
     * Sends a packet to a specific player.
     */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    /**
     * Sends a packet to all players.
     */
    public static void sendToAll(Object packet) {
        CHANNEL.send(packet, PacketDistributor.ALL.noArg());
    }

    /**
     * Sends a packet to all players tracking a position.
     */
    public static void sendToTracking(Object packet, ServerPlayer player) {
        CHANNEL.send(packet, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }
}
