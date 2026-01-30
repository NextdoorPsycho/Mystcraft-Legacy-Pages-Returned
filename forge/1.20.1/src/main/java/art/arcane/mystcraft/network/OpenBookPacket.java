package art.arcane.mystcraft.network;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Packet sent from client to server to request opening a book.
 * Used when the client wants to trigger a server-side book action.
 */
public record OpenBookPacket(InteractionHand hand, boolean performLink) {

  public static void encode(OpenBookPacket packet, FriendlyByteBuf buf) {
    buf.writeEnum(packet.hand);
    buf.writeBoolean(packet.performLink);
  }

  public static OpenBookPacket decode(FriendlyByteBuf buf) {
    return new OpenBookPacket(buf.readEnum(InteractionHand.class), buf.readBoolean());
  }

  public static void handle(OpenBookPacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getServerPlayer();
      if (player == null) return;

      ItemStack stack = player.getItemInHand(packet.hand);
      if (stack.isEmpty()) return;

      // Validate item type
      if (!(stack.getItem() instanceof AgebookItem) &&
          !(stack.getItem() instanceof LinkbookItem)) {
        return;
      }

      if (packet.performLink) {
        // Use the item to perform the link
        stack.getItem().use(player.level(), player, packet.hand);
      }
      // If not performing link, the client handles displaying the book UI
    });
  }
}
