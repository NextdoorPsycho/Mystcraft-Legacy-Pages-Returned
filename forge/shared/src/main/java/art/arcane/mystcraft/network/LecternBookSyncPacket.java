package art.arcane.mystcraft.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/**
 * Packet sent from server to client to sync Mystcraft book data in a vanilla
 * lectern. This is needed because vanilla's LecternBlockEntity doesn't sync
 * non-vanilla books.
 * <p>
 * Forge-specific implementation using direct field access via access
 * transformer.
 */
public record LecternBookSyncPacket(BlockPos pos, ItemStack book) {

  public static void encode(LecternBookSyncPacket packet, FriendlyByteBuf buf) {
    buf.writeBlockPos(packet.pos);
    buf.writeItem(packet.book);
  }

  public static LecternBookSyncPacket decode(FriendlyByteBuf buf) {
    return new LecternBookSyncPacket(buf.readBlockPos(), buf.readItem());
  }

  public static void handle(LecternBookSyncPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level == null) return;

      if (level.getBlockEntity(packet.pos) instanceof LecternBlockEntity lectern) {
        setBookOnClient(lectern, packet.book);
      }
    });
  }

  /**
   * Sets the book on a client-side lectern using direct field access. Access
   * transformer makes book and pageCount fields accessible.
   */
  public static void setBookOnClient(LecternBlockEntity lectern, ItemStack book) {
    lectern.book = book;
    lectern.pageCount = 1;
  }
}
