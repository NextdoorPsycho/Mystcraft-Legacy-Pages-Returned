package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Meteor entity.
 * Falls from the sky in unstable Ages, causing destruction on impact.
 */
public class MeteorEntity extends Entity {

    private int size = 1;
    private boolean explodeOnImpact = true;

    public MeteorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public MeteorEntity(Level level, double x, double y, double z, int size) {
        this(ModEntities.METEOR.get(), level);
        setPos(x, y, z);
        this.size = size;
        setDeltaMovement(0, -0.5, 0);
    }

    @Override
    protected void defineSynchedData() {
        // TODO: Add synched size for rendering
    }

    @Override
    public void tick() {
        super.tick();

        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.05, 0));
        }

        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.98, motion.y, motion.z * 0.98);

        move(MoverType.SELF, getDeltaMovement());

        if (onGround() && !level().isClientSide) {
            impact();
        }

        // Remove if fallen too far
        if (getY() < level().getMinBuildHeight() - 64) {
            discard();
        }
    }

    private void impact() {
        // TODO: Create explosion based on size
        // TODO: Create crater
        // TODO: Spawn fire
        // TODO: Play meteor impact sound
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        size = tag.getInt("Size");
        explodeOnImpact = tag.getBoolean("ExplodeOnImpact");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Size", size);
        tag.putBoolean("ExplodeOnImpact", explodeOnImpact);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
