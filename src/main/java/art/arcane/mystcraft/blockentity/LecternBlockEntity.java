package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Mystcraft Lectern.
 * Similar to bookstand but with different functionality for Age writing.
 */
public class LecternBlockEntity extends MystcraftBlockEntity {

    // TODO: Add book item storage
    // TODO: Add page viewing state

    public LecternBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LECTERN.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        // TODO: Save held book
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        // TODO: Load held book
    }
}
