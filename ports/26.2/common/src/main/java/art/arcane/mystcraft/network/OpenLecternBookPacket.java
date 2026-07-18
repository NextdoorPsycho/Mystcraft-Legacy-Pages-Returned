package art.arcane.mystcraft.network;

import art.arcane.mystcraft.util.ClientAccess;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/**
 * Packet sent from server to client to open the BookScreen for a lectern.
 * Includes the book data so the client doesn't need to rely on synced
 * BlockEntity state.
 */
public record OpenLecternBookPacket(BlockPos pos, ItemStack book) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<OpenLecternBookPacket> TYPE =
      MystcraftNetwork.type("open_lectern_book");
  public static final StreamCodec<RegistryFriendlyByteBuf, OpenLecternBookPacket> STREAM_CODEC =
      CustomPacketPayload.codec(OpenLecternBookPacket::encode, OpenLecternBookPacket::decode);

  public static void encode(OpenLecternBookPacket packet, RegistryFriendlyByteBuf buf) {
    buf.writeBlockPos(packet.pos);
    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.book);
  }

  public static OpenLecternBookPacket decode(RegistryFriendlyByteBuf buf) {
    return new OpenLecternBookPacket(buf.readBlockPos(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
  }

  @Override
  public CustomPacketPayload.Type<OpenLecternBookPacket> type() {
    return TYPE;
  }

  public static void handle(OpenLecternBookPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      Level level = ClientAccess.getClientLevel();
      if (level == null) return;

      if (level.getBlockEntity(packet.pos) instanceof LecternBlockEntity lectern) {
        LecternBookSyncPacket.setBookOnClient(lectern, packet.book);
      }

      if (MystcraftLecternHelper.isMystcraftBook(packet.book)) {
        MystcraftLecternHelper.openBookScreenForBlock(packet.book, packet.pos);
      }
    });
  }
}
