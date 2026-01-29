package art.arcane.mystcraft.network;

import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Packet sent from client to server to activate a book on a block entity (bookstand or lectern).
 * This is sent when the player clicks the "Link" button in the book GUI
 * that was opened from a bookstand or lectern.
 */
public class BlockBookActivatePacket {

  private static final double MAX_INTERACTION_DISTANCE = 6.0;
  private static final double MAX_INTERACTION_DISTANCE_SQ = MAX_INTERACTION_DISTANCE * MAX_INTERACTION_DISTANCE;

  private final BlockPos blockPos;

  public BlockBookActivatePacket(BlockPos blockPos) {
    this.blockPos = blockPos;
  }

  public static void encode(BlockBookActivatePacket packet, FriendlyByteBuf buf) {
    buf.writeBlockPos(packet.blockPos);
  }

  public static BlockBookActivatePacket decode(FriendlyByteBuf buf) {
    BlockPos pos = buf.readBlockPos();
    return new BlockBookActivatePacket(pos);
  }

  public static void handle(BlockBookActivatePacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getServerPlayer();
      if (player == null) {
        return;
      }

      // Validate distance - player must be close enough to interact
      if (player.blockPosition().distSqr(packet.blockPos) > MAX_INTERACTION_DISTANCE_SQ) {
        return;
      }

      // Get the block entity (LecternBlockEntity extends BookstandBlockEntity)
      BlockEntity be = player.level().getBlockEntity(packet.blockPos);
      if (!(be instanceof BookstandBlockEntity bookstand)) {
        return;
      }

      ItemStack book = bookstand.getBook();
      if (book.isEmpty()) {
        return;
      }

      // Activate the book
      if (book.getItem() instanceof LinkbookItem linkbook) {
        linkbook.activate(book, player.level(), player);
      } else if (book.getItem() instanceof AgebookItem agebook) {
        agebook.activate(book, player.level(), player);
      }
    });
  }
}
