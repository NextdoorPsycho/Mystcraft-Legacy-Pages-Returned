package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Packet sent from server to client to sync registered Mystcraft dimensions.
 * Keeps clients informed about available Ages.
 */
public class DimensionSyncPacket implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<DimensionSyncPacket> TYPE =
      MystcraftNetwork.type("dimension_sync");
  public static final StreamCodec<RegistryFriendlyByteBuf, DimensionSyncPacket> STREAM_CODEC =
      CustomPacketPayload.codec(DimensionSyncPacket::encode, DimensionSyncPacket::decode);

  private static final int MAX_SYNCED_DIMENSIONS = 16_384;
  private static final int MAX_AGE_NAME_LENGTH = 128;

  private final List<DimensionEntry> dimensions;

  public DimensionSyncPacket(List<DimensionEntry> dimensions) {
    this.dimensions = List.copyOf(dimensions);
  }

  public static void encode(DimensionSyncPacket packet, FriendlyByteBuf buf) {
    validateCount(packet.dimensions.size());
    buf.writeVarInt(packet.dimensions.size());
    for (DimensionEntry entry : packet.dimensions) {
      buf.writeVarInt(entry.ageUID);
      buf.writeIdentifier(entry.dimensionId);
      buf.writeUtf(entry.ageName, MAX_AGE_NAME_LENGTH);
      buf.writeBoolean(entry.isUnstable);
    }
  }

  public static DimensionSyncPacket decode(FriendlyByteBuf buf) {
    int count = buf.readVarInt();
    validateCount(count);
    List<DimensionEntry> dimensions = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      dimensions.add(new DimensionEntry(
          buf.readVarInt(),
          buf.readIdentifier(),
          buf.readUtf(MAX_AGE_NAME_LENGTH),
          buf.readBoolean()
      ));
    }
    return new DimensionSyncPacket(dimensions);
  }

  @Override
  public CustomPacketPayload.Type<DimensionSyncPacket> type() {
    return TYPE;
  }

  private static void validateCount(int count) {
    if (count < 0 || count > MAX_SYNCED_DIMENSIONS) {
      throw new IllegalArgumentException("Invalid Mystcraft dimension sync count: " + count);
    }
  }

  public static void handle(DimensionSyncPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      if (ClientAccess.getClientLevel() == null) return;

      ClientDimensionCache.clear();
      for (DimensionEntry entry : packet.dimensions) {
        ClientDimensionCache.addDimension(entry);
      }

      Mystcraft.LOGGER.info("Received dimension sync with {} Ages", packet.dimensions.size());
    });
  }

  /**
   * Represents a registered Mystcraft dimension.
   */
  public record DimensionEntry(int ageUID, Identifier dimensionId,
                               String ageName, boolean isUnstable) {
  }

  /**
   * Client-side cache for known Mystcraft dimensions.
   */
  public static class ClientDimensionCache {
    private static final Map<Integer, DimensionEntry> BY_UID = new ConcurrentHashMap<>();
    private static final Map<Identifier, DimensionEntry> BY_ID = new ConcurrentHashMap<>();

    public static void addDimension(DimensionEntry entry) {
      DimensionEntry previousUid = BY_UID.put(entry.ageUID, entry);
      if (previousUid != null && !previousUid.dimensionId.equals(entry.dimensionId)) {
        BY_ID.remove(previousUid.dimensionId, previousUid);
      }
      DimensionEntry previousId = BY_ID.put(entry.dimensionId, entry);
      if (previousId != null && previousId.ageUID != entry.ageUID) {
        BY_UID.remove(previousId.ageUID, previousId);
      }
    }

    public static void removeDimension(int ageUID) {
      DimensionEntry removed = BY_UID.remove(ageUID);
      if (removed != null) {
        BY_ID.remove(removed.dimensionId);
      }
    }

    public static void clear() {
      BY_UID.clear();
      BY_ID.clear();
    }

    public static DimensionEntry getByUID(int ageUID) {
      return BY_UID.get(ageUID);
    }

    public static DimensionEntry getById(Identifier id) {
      return BY_ID.get(id);
    }

    public static List<DimensionEntry> getAll() {
      return new ArrayList<>(BY_UID.values());
    }

    public static int getCount() {
      return BY_UID.size();
    }

    public static boolean exists(int ageUID) {
      return BY_UID.containsKey(ageUID);
    }

    public static String getAgeName(int ageUID) {
      DimensionEntry entry = BY_UID.get(ageUID);
      return entry != null ? entry.ageName : "Age " + ageUID;
    }

    public static boolean isUnstable(int ageUID) {
      DimensionEntry entry = BY_UID.get(ageUID);
      return entry != null && entry.isUnstable;
    }
  }
}
