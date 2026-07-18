package art.arcane.mystcraft.network;

import art.arcane.mystcraft.entity.LinkbookEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Packet sent from client to server to activate a book in a LinkbookEntity.
 * This is sent when the player clicks the "Link" button in the book GUI that
 * was opened from a LinkbookEntity on the ground.
 */
public class EntityBookActivatePacket implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<EntityBookActivatePacket> TYPE =
      MystcraftNetwork.type("entity_book_activate");
  public static final StreamCodec<RegistryFriendlyByteBuf, EntityBookActivatePacket> STREAM_CODEC =
      CustomPacketPayload.codec(EntityBookActivatePacket::encode, EntityBookActivatePacket::decode);

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

  @Override
  public CustomPacketPayload.Type<EntityBookActivatePacket> type() {
    return TYPE;
  }

  public static void handle(EntityBookActivatePacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getServerPlayer();
      if (player == null) {
        return;
      }

      Entity entity = player.level().getEntity(packet.entityId);
      if (!(entity instanceof LinkbookEntity bookEntity)) {
        return;
      }

      if (player.distanceToSqr(bookEntity) > MAX_INTERACTION_DISTANCE_SQ) {
        return;
      }

      bookEntity.activateBook(player);
    });
  }
}
