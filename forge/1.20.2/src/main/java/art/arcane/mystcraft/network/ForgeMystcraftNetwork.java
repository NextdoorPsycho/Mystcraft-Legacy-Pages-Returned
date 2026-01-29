package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Network handler for Mystcraft packets on Forge.
 * Uses Forge's SimpleChannel for client-server communication.
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

    private ForgeMystcraftNetwork() {
    }

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

    private static <T> BiConsumer<T, CustomPayloadEvent.Context> wrap(BiConsumer<T, PacketContext> handler) {
        return (packet, ctx) -> handler.accept(packet, new Context(ctx));
    }

    public static void register() {
        CHANNEL.messageBuilder(OpenBookPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenBookPacket::encode)
                .decoder(OpenBookPacket::decode)
                .consumerMainThread(wrap(OpenBookPacket::handle))
                .add();

        CHANNEL.messageBuilder(SyncAgeDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAgeDataPacket::encode)
                .decoder(SyncAgeDataPacket::decode)
                .consumerMainThread(wrap(SyncAgeDataPacket::handle))
                .add();

        CHANNEL.messageBuilder(LinkEffectPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LinkEffectPacket::encode)
                .decoder(LinkEffectPacket::decode)
                .consumerMainThread(wrap(LinkEffectPacket::handle))
                .add();

        CHANNEL.messageBuilder(SymbolSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SymbolSyncPacket::encode)
                .decoder(SymbolSyncPacket::decode)
                .consumerMainThread(wrap(SymbolSyncPacket::handle))
                .add();

        CHANNEL.messageBuilder(ContainerActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ContainerActionPacket::encode)
                .decoder(ContainerActionPacket::decode)
                .consumerMainThread(wrap(ContainerActionPacket::handle))
                .add();

        CHANNEL.messageBuilder(ConfigSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigSyncPacket::encode)
                .decoder(ConfigSyncPacket::decode)
                .consumerMainThread(wrap(ConfigSyncPacket::handle))
                .add();

        CHANNEL.messageBuilder(DimensionSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DimensionSyncPacket::encode)
                .decoder(DimensionSyncPacket::decode)
                .consumerMainThread(wrap(DimensionSyncPacket::handle))
                .add();

        CHANNEL.messageBuilder(ProfilingStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProfilingStatePacket::encode)
                .decoder(ProfilingStatePacket::decode)
                .consumerMainThread(wrap(ProfilingStatePacket::handle))
                .add();

        CHANNEL.messageBuilder(ExplosionPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ExplosionPacket::encode)
                .decoder(ExplosionPacket::decode)
                .consumerMainThread(wrap(ExplosionPacket::handle))
                .add();

        CHANNEL.messageBuilder(SpawnLightningPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpawnLightningPacket::encode)
                .decoder(SpawnLightningPacket::decode)
                .consumerMainThread(wrap(SpawnLightningPacket::handle))
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

        CHANNEL.messageBuilder(LecternBookSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LecternBookSyncPacket::encode)
                .decoder(LecternBookSyncPacket::decode)
                .consumerMainThread(wrap(LecternBookSyncPacket::handle))
                .add();

        CHANNEL.messageBuilder(OpenLecternBookPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenLecternBookPacket::encode)
                .decoder(OpenLecternBookPacket::decode)
                .consumerMainThread(wrap(OpenLecternBookPacket::handle))
                .add();

        Mystcraft.LOGGER.info("Registered {} network packets", packetId);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(packet, PacketDistributor.ALL.noArg());
    }

    public static void sendToTracking(Object packet, ServerPlayer player) {
        CHANNEL.send(packet, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    public static void sendToTrackingBlock(Object packet, ServerLevel level, BlockPos pos) {
        CHANNEL.send(packet, PacketDistributor.TRACKING_CHUNK.with(level.getChunkAt(pos)));
    }
}
