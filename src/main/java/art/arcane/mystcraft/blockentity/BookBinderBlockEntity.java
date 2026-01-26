package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Book Binder.
 * Used to create descriptive books and linkbooks from pages.
 */
public class BookBinderBlockEntity extends MystcraftBlockEntity {

    // TODO: Add inventory for pages
    // TODO: Add inventory for leather/materials
    // TODO: Add book creation logic

    public BookBinderBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BOOK_BINDER.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        // TODO: Save inventory contents
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        // TODO: Load inventory contents
    }
}
