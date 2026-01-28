package art.arcane.mystcraft.network;

import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Packet sent from server to client to display custom explosion effects.
 * Allows for colored or modified explosion visuals without actual block damage.
 */
public record ExplosionPacket(
        double x, double y, double z,
        float power,
        int color,
        ExplosionType type,
        boolean playSound
) {

    public enum ExplosionType {
        NORMAL,      // Standard explosion particles
        METEOR,      // Fiery impact with more fire particles
        DECAY,       // Purple/dark particles
        INSTABILITY, // Red-tinted chaotic particles
        SILENT       // Visual only, no sound
    }

    public static void encode(ExplosionPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.power);
        buf.writeVarInt(packet.color);
        buf.writeEnum(packet.type);
        buf.writeBoolean(packet.playSound);
    }

    public static ExplosionPacket decode(FriendlyByteBuf buf) {
        return new ExplosionPacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readEnum(ExplosionType.class),
                buf.readBoolean()
        );
    }

    public static void handle(ExplosionPacket packet, PacketContext ctx) {
        if (!ctx.isClientSide()) {
            return;
        }
        ctx.enqueueWork(() -> {
            Level level = (Level) ClientAccess.getClientLevel();
            if (level == null) return;

            spawnExplosionParticles(level, packet);

            if (packet.playSound && packet.type != ExplosionType.SILENT) {
                float volume = Math.min(packet.power, 4.0f);
                level.playLocalSound(packet.x, packet.y, packet.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                        volume, 1.0f + (level.random.nextFloat() - 0.5f) * 0.2f, false);
            }
        });
    }

    private static void spawnExplosionParticles(Level level, ExplosionPacket packet) {
        int particleCount = (int) (packet.power * 10);
        Vec3 pos = new Vec3(packet.x, packet.y, packet.z);

        switch (packet.type) {
            case NORMAL -> spawnNormalExplosion(level, pos, particleCount);
            case METEOR -> spawnMeteorExplosion(level, pos, particleCount);
            case DECAY -> spawnDecayExplosion(level, pos, particleCount, packet.color);
            case INSTABILITY -> spawnInstabilityExplosion(level, pos, particleCount);
            case SILENT -> spawnNormalExplosion(level, pos, particleCount);
        }
    }

    private static void spawnNormalExplosion(Level level, Vec3 pos, int count) {
        // Large smoke cloud
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1.0, 0.0, 0.0);

        // Surrounding smoke and flame
        for (int i = 0; i < count; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    offsetX * 0.05, offsetY * 0.05, offsetZ * 0.05);

            if (level.random.nextFloat() < 0.3f) {
                level.addParticle(ParticleTypes.FLAME,
                        pos.x + offsetX * 0.5, pos.y + offsetY * 0.5, pos.z + offsetZ * 0.5,
                        offsetX * 0.02, 0.05, offsetZ * 0.02);
            }
        }
    }

    private static void spawnMeteorExplosion(Level level, Vec3 pos, int count) {
        // Core explosion
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1.0, 0.0, 0.0);

        // Heavy fire particles
        for (int i = 0; i < count * 2; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 3.0;
            double offsetY = level.random.nextDouble() * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 3.0;

            level.addParticle(ParticleTypes.FLAME,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    offsetX * 0.1, 0.1 + level.random.nextDouble() * 0.1, offsetZ * 0.1);

            if (level.random.nextFloat() < 0.5f) {
                level.addParticle(ParticleTypes.LAVA,
                        pos.x + offsetX * 0.5, pos.y, pos.z + offsetZ * 0.5,
                        0, 0, 0);
            }
        }

        // Smoke plume
        for (int i = 0; i < count; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.x + offsetX, pos.y + 0.5, pos.z + offsetZ,
                    0, 0.07 + level.random.nextDouble() * 0.05, 0);
        }
    }

    private static void spawnDecayExplosion(Level level, Vec3 pos, int count, int color) {
        // Purple/dark explosion effect
        for (int i = 0; i < count; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

            // Use portal particles for that otherworldly decay feel
            level.addParticle(ParticleTypes.PORTAL,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    offsetX * 0.5, offsetY * 0.5, offsetZ * 0.5);

            if (level.random.nextFloat() < 0.3f) {
                level.addParticle(ParticleTypes.WITCH,
                        pos.x + offsetX * 0.5, pos.y + offsetY * 0.5, pos.z + offsetZ * 0.5,
                        0, 0.02, 0);
            }
        }

        // Dark smoke
        for (int i = 0; i < count / 2; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;

            level.addParticle(ParticleTypes.SQUID_INK,
                    pos.x + offsetX, pos.y, pos.z + offsetZ,
                    0, 0.05, 0);
        }
    }

    private static void spawnInstabilityExplosion(Level level, Vec3 pos, int count) {
        // Chaotic red-tinted explosion
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1.0, 0.0, 0.0);

        for (int i = 0; i < count; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 3.0;
            double offsetY = (level.random.nextDouble() - 0.5) * 3.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 3.0;

            // Mix of angry particles
            if (level.random.nextFloat() < 0.4f) {
                level.addParticle(ParticleTypes.ANGRY_VILLAGER,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        0, 0, 0);
            }

            level.addParticle(ParticleTypes.SMOKE,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1);

            if (level.random.nextFloat() < 0.2f) {
                level.addParticle(ParticleTypes.CRIT,
                        pos.x + offsetX * 0.5, pos.y + offsetY * 0.5, pos.z + offsetZ * 0.5,
                        offsetX * 0.3, offsetY * 0.3, offsetZ * 0.3);
            }
        }

        // Occasional sparks
        for (int i = 0; i < count / 3; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double speed = 0.2 + level.random.nextDouble() * 0.3;

            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y + 0.5, pos.z,
                    Math.cos(angle) * speed, 0.1 + level.random.nextDouble() * 0.2, Math.sin(angle) * speed);
        }
    }

    /**
     * Creates a standard explosion packet.
     */
    public static ExplosionPacket normal(double x, double y, double z, float power) {
        return new ExplosionPacket(x, y, z, power, 0xFFFFFF, ExplosionType.NORMAL, true);
    }

    /**
     * Creates a meteor impact explosion packet.
     */
    public static ExplosionPacket meteor(double x, double y, double z, float power) {
        return new ExplosionPacket(x, y, z, power, 0xFF4400, ExplosionType.METEOR, true);
    }

    /**
     * Creates a decay explosion packet.
     */
    public static ExplosionPacket decay(double x, double y, double z, float power, int color) {
        return new ExplosionPacket(x, y, z, power, color, ExplosionType.DECAY, true);
    }

    /**
     * Creates an instability explosion packet.
     */
    public static ExplosionPacket instability(double x, double y, double z, float power) {
        return new ExplosionPacket(x, y, z, power, 0xFF0000, ExplosionType.INSTABILITY, true);
    }
}
