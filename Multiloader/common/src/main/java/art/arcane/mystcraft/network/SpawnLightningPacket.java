package art.arcane.mystcraft.network;

import art.arcane.mystcraft.entity.ColoredLightningEntity;
import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

/**
 * Packet sent from server to client to spawn a colored lightning bolt.
 * Used to synchronize custom lightning entity creation with visual effects.
 */
public record SpawnLightningPacket(
        int entityId,
        double x, double y, double z,
        int color,
        boolean visualOnly
) {

    public static void encode(SpawnLightningPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeVarInt(packet.color);
        buf.writeBoolean(packet.visualOnly);
    }

    public static SpawnLightningPacket decode(FriendlyByteBuf buf) {
        return new SpawnLightningPacket(
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public static void handle(SpawnLightningPacket packet, PacketContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            // Create the colored lightning entity on client
            ColoredLightningEntity lightning = new ColoredLightningEntity(
                    ModEntities.COLORED_LIGHTNING.get(), level);
            lightning.setId(packet.entityId);
            lightning.setPos(packet.x, packet.y, packet.z);
            lightning.setColor(packet.color);
            lightning.setVisualOnly(packet.visualOnly);

            level.addFreshEntity(lightning);
        });
    }

    /**
     * Creates a standard white lightning packet.
     */
    public static SpawnLightningPacket white(int entityId, double x, double y, double z) {
        return new SpawnLightningPacket(entityId, x, y, z, 0xFFFFFF, false);
    }

    /**
     * Creates a colored lightning packet.
     */
    public static SpawnLightningPacket colored(int entityId, double x, double y, double z, int color) {
        return new SpawnLightningPacket(entityId, x, y, z, color, false);
    }

    /**
     * Creates a visual-only lightning packet (no damage/fire).
     */
    public static SpawnLightningPacket visualOnly(int entityId, double x, double y, double z, int color) {
        return new SpawnLightningPacket(entityId, x, y, z, color, true);
    }

    /**
     * Creates a red instability lightning packet.
     */
    public static SpawnLightningPacket instability(int entityId, double x, double y, double z) {
        return new SpawnLightningPacket(entityId, x, y, z, 0xFF4444, false);
    }

    /**
     * Creates a blue/purple decay lightning packet.
     */
    public static SpawnLightningPacket decay(int entityId, double x, double y, double z) {
        return new SpawnLightningPacket(entityId, x, y, z, 0x8844FF, false);
    }
}
