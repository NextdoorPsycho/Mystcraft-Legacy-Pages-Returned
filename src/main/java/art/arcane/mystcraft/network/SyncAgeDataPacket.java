package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Packet sent from server to client to sync Age data.
 * Used to keep clients informed about Age properties.
 */
public record SyncAgeDataPacket(int ageUID, CompoundTag data) {

    public static void encode(SyncAgeDataPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.ageUID);
        buf.writeNbt(packet.data);
    }

    public static SyncAgeDataPacket decode(FriendlyByteBuf buf) {
        int ageUID = buf.readVarInt();
        CompoundTag data = buf.readNbt();
        return new SyncAgeDataPacket(ageUID, data);
    }

    public static void handle(SyncAgeDataPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            // Store the age data in client-side cache
            ClientAgeDataCache.setAgeData(packet.ageUID, packet.data);

            Mystcraft.LOGGER.debug("Received Age data for UID {}", packet.ageUID);
        });
        ctx.setPacketHandled(true);
    }

    /**
     * Client-side cache for Age data.
     * Stores synced Age information for rendering and display.
     */
    public static class ClientAgeDataCache {
        private static final java.util.Map<Integer, CompoundTag> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

        public static void setAgeData(int ageUID, CompoundTag data) {
            if (data != null) {
                CACHE.put(ageUID, data);
            } else {
                CACHE.remove(ageUID);
            }
        }

        public static CompoundTag getAgeData(int ageUID) {
            return CACHE.get(ageUID);
        }

        public static void clear() {
            CACHE.clear();
        }

        public static String getAgeName(int ageUID) {
            CompoundTag data = CACHE.get(ageUID);
            if (data != null && data.contains("AgeName")) {
                return data.getString("AgeName");
            }
            return "Age " + ageUID;
        }

        public static float getInstability(int ageUID) {
            CompoundTag data = CACHE.get(ageUID);
            if (data != null && data.contains("Instability")) {
                return data.getFloat("Instability");
            }
            return 0.0f;
        }
    }
}
