package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Book Receptacle.
 * Holds a descriptive book and creates a portal when powered by a crystal.
 */
public class BookReceptacleBlockEntity extends MystcraftBlockEntity {

    // TODO: Add book item storage
    // TODO: Add portal state tracking
    // TODO: Add crystal link logic

    public BookReceptacleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BOOK_RECEPTACLE.get(), pos, blockState);
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
