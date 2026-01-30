package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Packet sent from server to client to sync Mystcraft book data in a vanilla lectern.
 * This is needed because vanilla's LecternBlockEntity doesn't sync non-vanilla books.
 *
 * Forge-specific implementation using ObfuscationReflectionHelper for proper SRG name handling.
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
      Level level = (Level) ClientAccess.getClientLevel();
      if (level == null) return;

      if (level.getBlockEntity(packet.pos) instanceof LecternBlockEntity lectern) {
        setBookOnClient(lectern, packet.book);
      }
    });
  }

  /**
   * Sets the book on a client-side lectern using ObfuscationReflectionHelper.
   * Uses SRG field names for production environment compatibility.
   */
  public static void setBookOnClient(LecternBlockEntity lectern, ItemStack book) {
    try {
      ObfuscationReflectionHelper.setPrivateValue(
          LecternBlockEntity.class, lectern, book, "f_59527_");
      ObfuscationReflectionHelper.setPrivateValue(
          LecternBlockEntity.class, lectern, 1, "f_59529_");
    } catch (Exception e) {
      Mystcraft.LOGGER.error("[LecternBookSyncPacket] Failed to set book on client", e);
    }
  }
}
