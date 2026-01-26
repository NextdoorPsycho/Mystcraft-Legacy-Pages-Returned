package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A temporary helper entity used for position and rotation calculations.
 * Dies immediately on the first tick - never meant to persist.
 * Used by link modifiers and other calculations that need a temporary entity.
 */
public class DummyEntity extends Entity {

    public DummyEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DummyEntity(Level level) {
        this(ModEntities.DUMMY.get(), level);
    }

    /**
     * Creates a dummy entity at a specific position with rotation.
     */
    public DummyEntity(Level level, double x, double y, double z, float yaw, float pitch) {
        this(level);
        this.setPos(x, y, z);
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    /**
     * Creates a dummy entity at integer coordinates with rotation.
     */
    public DummyEntity(Level level, int x, int y, int z, int yaw, int pitch) {
        this(level, (double) x, (double) y, (double) z, (float) yaw, (float) pitch);
    }

    @Override
    protected void defineSynchedData() {
        // No synced data needed - entity is purely server-side and dies immediately
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Never saved - dies immediately
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Never saved - dies immediately
    }

    @Override
    public void tick() {
        // Die immediately on first tick
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        // Use vanilla packet - this entity dies immediately so it should never actually be sent to clients
        return new ClientboundAddEntityPacket(this);
    }
}
