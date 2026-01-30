package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

import java.lang.reflect.Field;

/**
 * Packet sent from server to client to sync Mystcraft book data in a vanilla lectern.
 * This is needed because vanilla's LecternBlockEntity doesn't sync non-vanilla books.
 */
public record LecternBookSyncPacket(BlockPos pos, ItemStack book) {

  private static Field bookField;
  private static Field pageCountField;

  static {
    try {
      bookField = LecternBlockEntity.class.getDeclaredField("book");
      bookField.setAccessible(true);
      pageCountField = LecternBlockEntity.class.getDeclaredField("pageCount");
      pageCountField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      Mystcraft.LOGGER.error("[LecternBookSyncPacket] Failed to find LecternBlockEntity fields", e);
    }
  }

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
      Level level = (Level) ClientAccess.getClientLevel();
      if (level == null) return;

      if (level.getBlockEntity(packet.pos) instanceof LecternBlockEntity lectern) {
        setBookOnClient(lectern, packet.book);
      }
    });
  }

  /**
   * Sets the book on a client-side lectern using reflection.
   */
  public static void setBookOnClient(LecternBlockEntity lectern, ItemStack book) {
    try {
      if (bookField != null && pageCountField != null) {
        bookField.set(lectern, book);
        pageCountField.set(lectern, 1);
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.error("[LecternBookSyncPacket] Failed to set book on client", e);
    }
  }
}
