package art.arcane.mystcraft.network;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Packet sent from client to server to activate a linking book.
 * This is sent when the player clicks the "Link" button in the book GUI.
 * Matches legacy behavior where linking happens through GUI activation.
 */
public class LinkBookActivatePacket {

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

    public static void handle(LinkBookActivatePacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ItemStack heldItem = player.getItemInHand(packet.hand);

            // Handle linkbook activation
            if (heldItem.getItem() instanceof LinkbookItem linkbook) {
                linkbook.activate(heldItem, player.level(), player);
            }
            // Handle agebook activation - call activate() directly
            else if (heldItem.getItem() instanceof AgebookItem agebook) {
                agebook.activate(heldItem, player.level(), player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
