package art.arcane.mystcraft.network;

import art.arcane.mystcraft.util.ClientAccess;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/**
 * Packet sent from server to client to open the BookScreen for a lectern.
 * Includes the book data so the client doesn't need to rely on synced BlockEntity state.
 */
public record OpenLecternBookPacket(BlockPos pos, ItemStack book) {

    public static void encode(OpenLecternBookPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeItem(packet.book);
    }

    public static OpenLecternBookPacket decode(FriendlyByteBuf buf) {
        return new OpenLecternBookPacket(buf.readBlockPos(), buf.readItem());
    }

    public static void handle(OpenLecternBookPacket packet, PacketContext ctx) {
        if (!ctx.isClientSide()) {
            return;
        }
        ctx.enqueueWork(() -> {
            Level level = (Level) ClientAccess.getClientLevel();
            if (level == null) return;

            // Also update the local lectern's book for consistency
            if (level.getBlockEntity(packet.pos) instanceof LecternBlockEntity lectern) {
                LecternBookSyncPacket.setBookOnClient(lectern, packet.book);
            }

            // Open the book screen
            if (MystcraftLecternHelper.isMystcraftBook(packet.book)) {
                MystcraftLecternHelper.openBookScreenForBlock(packet.book, packet.pos);
            }
        });
    }
}
