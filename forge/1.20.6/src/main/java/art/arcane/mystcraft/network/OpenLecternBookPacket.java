package art.arcane.mystcraft.network;

import art.arcane.mystcraft.util.ClientAccess;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/**
 * Packet sent from server to client to open the BookScreen for a lectern.
 * Includes the book data so the client doesn't need to rely on synced BlockEntity state.
 * NeoForge 1.20.6+ version using StreamCodecs for ItemStack serialization.
 */
public record OpenLecternBookPacket(BlockPos pos, ItemStack book) {

  public static void encode(OpenLecternBookPacket packet, FriendlyByteBuf buf) {
    buf.writeBlockPos(packet.pos);
    // In 1.20.6+, use StreamCodec for ItemStack
    if (buf instanceof RegistryFriendlyByteBuf registryBuf) {
      ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf, packet.book);
    } else {
      // Fallback - write as NBT compound
      buf.writeNbt(ItemStackNbt.save(packet.book));
    }
  }

  public static OpenLecternBookPacket decode(FriendlyByteBuf buf) {
    BlockPos pos = buf.readBlockPos();
    ItemStack book;
    // In 1.20.6+, use StreamCodec for ItemStack
    if (buf instanceof RegistryFriendlyByteBuf registryBuf) {
      book = ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuf);
    } else {
      // Fallback - read as NBT compound
      book = ItemStackNbt.load(buf.readNbt());
    }
    return new OpenLecternBookPacket(pos, book);
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
