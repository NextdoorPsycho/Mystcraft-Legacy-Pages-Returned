package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A custom falling block entity for Mystcraft.
 * Used for special falling block effects in Ages.
 */
public class MystcraftFallingBlockEntity extends Entity {

    private BlockState blockState = Blocks.STONE.defaultBlockState();
    private int time;

    public MystcraftFallingBlockEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public MystcraftFallingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.FALLING_BLOCK.get(), level);
        setPos(x, y, z);
        this.blockState = state;
        this.blocksBuilding = true;
        setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        setStartPos(blockPosition());
    }

    @Override
    protected void defineSynchedData() {
        // No synched data needed
    }

    @Override
    public void tick() {
        if (blockState.isAir()) {
            discard();
            return;
        }

        time++;
        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        move(MoverType.SELF, getDeltaMovement());

        // TODO: Handle landing
        // TODO: Handle collision with blocks/entities

        if (time > 600) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        blockState = NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("BlockState"));
        time = tag.getInt("Time");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(blockState));
        tag.putInt("Time", time);
    }

    public BlockState getBlockState() {
        return blockState;
    }

    private void setStartPos(BlockPos pos) {
        // TODO: Store for block entity data preservation
    }
}
