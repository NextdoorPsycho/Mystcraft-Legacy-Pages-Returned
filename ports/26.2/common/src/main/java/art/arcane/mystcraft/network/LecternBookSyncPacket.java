package art.arcane.mystcraft.network;

import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/**
 * Packet sent from server to client to sync Mystcraft book data in a vanilla
 * lectern. This is needed because vanilla's LecternBlockEntity doesn't sync
 * non-vanilla books.
 */
public record LecternBookSyncPacket(BlockPos pos, ItemStack book) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<LecternBookSyncPacket> TYPE =
      MystcraftNetwork.type("lectern_book_sync");
  public static final StreamCodec<RegistryFriendlyByteBuf, LecternBookSyncPacket> STREAM_CODEC =
      CustomPacketPayload.codec(LecternBookSyncPacket::encode, LecternBookSyncPacket::decode);

  public static void encode(LecternBookSyncPacket packet, RegistryFriendlyByteBuf buf) {
    buf.writeBlockPos(packet.pos);
    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.book);
  }

  public static LecternBookSyncPacket decode(RegistryFriendlyByteBuf buf) {
    return new LecternBookSyncPacket(buf.readBlockPos(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
  }

  @Override
  public CustomPacketPayload.Type<LecternBookSyncPacket> type() {
    return TYPE;
  }

  public static void handle(LecternBookSyncPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      Level level = ClientAccess.getClientLevel();
      if (level == null) return;

      if (level.getBlockEntity(packet.pos) instanceof LecternBlockEntity lectern) {
        setBookOnClient(lectern, packet.book);
      }
    });
  }

  public static void setBookOnClient(LecternBlockEntity lectern, ItemStack book) {
    lectern.setBook(book.copy());
  }
}
