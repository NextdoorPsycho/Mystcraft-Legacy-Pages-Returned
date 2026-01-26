package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Writing Desk.
 * Used for writing symbols onto pages using ink.
 */
public class WritingDeskBlockEntity extends MystcraftBlockEntity {

    // TODO: Add page inventory
    // TODO: Add ink tank
    // TODO: Add writing progress/state

    public WritingDeskBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WRITING_DESK.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        // TODO: Save inventory and ink
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        // TODO: Load inventory and ink
    }
}
