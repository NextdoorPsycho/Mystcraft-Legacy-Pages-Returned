package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Packet sent from server to client to sync registered Mystcraft dimensions.
 * Keeps clients informed about available Ages.
 */
public class DimensionSyncPacket {

  private final List<DimensionEntry> dimensions;

  public DimensionSyncPacket(List<DimensionEntry> dimensions) {
    this.dimensions = dimensions;
  }

  public static void encode(DimensionSyncPacket packet, FriendlyByteBuf buf) {
    buf.writeVarInt(packet.dimensions.size());
    for (DimensionEntry entry : packet.dimensions) {
      buf.writeVarInt(entry.ageUID);
      buf.writeResourceLocation(entry.dimensionId);
      buf.writeUtf(entry.ageName);
      buf.writeBoolean(entry.isUnstable);
    }
  }

  public static DimensionSyncPacket decode(FriendlyByteBuf buf) {
    int count = buf.readVarInt();
    List<DimensionEntry> dimensions = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      dimensions.add(new DimensionEntry(
          buf.readVarInt(),
          buf.readResourceLocation(),
          buf.readUtf(),
          buf.readBoolean()
      ));
    }
    return new DimensionSyncPacket(dimensions);
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
  public record DimensionEntry(int ageUID, ResourceLocation dimensionId, String ageName, boolean isUnstable) {
  }

  /**
   * Client-side cache for known Mystcraft dimensions.
   */
  public static class ClientDimensionCache {
    private static final Map<Integer, DimensionEntry> BY_UID = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, DimensionEntry> BY_ID = new ConcurrentHashMap<>();

    public static void addDimension(DimensionEntry entry) {
      BY_UID.put(entry.ageUID, entry);
      BY_ID.put(entry.dimensionId, entry);
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

    public static DimensionEntry getById(ResourceLocation id) {
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
