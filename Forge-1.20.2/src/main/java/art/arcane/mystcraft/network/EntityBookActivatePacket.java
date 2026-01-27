package art.arcane.mystcraft.network;

import art.arcane.mystcraft.entity.LinkbookEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Packet sent from client to server to activate a book in a LinkbookEntity.
 * This is sent when the player clicks the "Link" button in the book GUI
 * that was opened from a LinkbookEntity on the ground.
 */
public class EntityBookActivatePacket {

    private static final double MAX_INTERACTION_DISTANCE = 6.0;
    private static final double MAX_INTERACTION_DISTANCE_SQ = MAX_INTERACTION_DISTANCE * MAX_INTERACTION_DISTANCE;

    private final int entityId;

    public EntityBookActivatePacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(EntityBookActivatePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static EntityBookActivatePacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        return new EntityBookActivatePacket(entityId);
    }

    public static void handle(EntityBookActivatePacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            // Find the entity by ID
            Entity entity = player.level().getEntity(packet.entityId);
            if (!(entity instanceof LinkbookEntity bookEntity)) {
                return;
            }

            // Validate distance - player must be close enough to interact
            if (player.distanceToSqr(bookEntity) > MAX_INTERACTION_DISTANCE_SQ) {
                return;
            }

            // Activate the book
            bookEntity.activateBook(player);
        });
        ctx.setPacketHandled(true);
    }
}
