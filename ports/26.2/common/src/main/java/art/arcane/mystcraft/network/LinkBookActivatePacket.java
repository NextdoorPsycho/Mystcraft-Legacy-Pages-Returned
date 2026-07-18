package art.arcane.mystcraft.network;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Packet sent from client to server to activate a linking book. Sent when the
 * player clicks the "Link" button in the book GUI.
 */
public class LinkBookActivatePacket implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<LinkBookActivatePacket> TYPE =
      MystcraftNetwork.type("link_book_activate");
  public static final StreamCodec<RegistryFriendlyByteBuf, LinkBookActivatePacket> STREAM_CODEC =
      CustomPacketPayload.codec(LinkBookActivatePacket::encode, LinkBookActivatePacket::decode);

  private final InteractionHand hand;

  public LinkBookActivatePacket(InteractionHand hand) {
    this.hand = hand;
  }

  public static void encode(LinkBookActivatePacket packet, FriendlyByteBuf buf) {
    buf.writeEnum(packet.hand);
  }

  public static LinkBookActivatePacket decode(FriendlyByteBuf buf) {
    InteractionHand hand = buf.readEnum(InteractionHand.class);
    return new LinkBookActivatePacket(hand);
  }

  @Override
  public CustomPacketPayload.Type<LinkBookActivatePacket> type() {
    return TYPE;
  }

  public static void handle(LinkBookActivatePacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getServerPlayer();
      if (player == null) return;

      ItemStack heldItem = player.getItemInHand(packet.hand);

      if (heldItem.getItem() instanceof LinkbookItem linkbook) {
        linkbook.activate(heldItem, player.level(), player);
      } else if (heldItem.getItem() instanceof AgebookItem agebook) {
        agebook.activate(heldItem, player.level(), player);
      }
    });
  }
}
