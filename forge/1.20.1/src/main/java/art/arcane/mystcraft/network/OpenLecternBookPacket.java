package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/**
 * Packet sent from server to client to open the BookScreen for a lectern.
 * Includes the book data so the client doesn't need to rely on synced
 * BlockEntity state.
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
    Mystcraft.LOGGER.debug("[OpenLecternBookPacket] Received packet for pos={}, book={}, isClientSide={}",
        packet.pos, packet.book, ctx.isClientSide());
    if (!ctx.isClientSide()) {
      Mystcraft.LOGGER.debug("[OpenLecternBookPacket] Not client side, ignoring");
      return;
    }
    ctx.enqueueWork(() -> {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level == null) {
        Mystcraft.LOGGER.warn("[OpenLecternBookPacket] Client level is null!");
        return;
      }

      if (level.getBlockEntity(packet.pos) instanceof LecternBlockEntity lectern) {
        LecternBookSyncPacket.setBookOnClient(lectern, packet.book);
      }

      boolean isMystBook = MystcraftLecternHelper.isMystcraftBook(packet.book);
      Mystcraft.LOGGER.debug("[OpenLecternBookPacket] isMystcraftBook={}, book item={}",
          isMystBook, packet.book.getItem().getClass().getName());
      if (isMystBook) {
        Mystcraft.LOGGER.debug("[OpenLecternBookPacket] Opening book screen for block at {}", packet.pos);
        MystcraftLecternHelper.openBookScreenForBlock(packet.book, packet.pos);
      }
    });
  }
}
