package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Star Fissure.
 * The gateway out of an Age back to the overworld.
 */
public class StarFissureBlockEntity extends MystcraftBlockEntity {

    // TODO: Add fissure state (open/closed/forming)
    // TODO: Add animation state for rendering

    public StarFissureBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STAR_FISSURE.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        // TODO: Save fissure state
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        // TODO: Load fissure state
    }
}
