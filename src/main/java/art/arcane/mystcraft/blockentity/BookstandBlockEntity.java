package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Bookstand.
 * Holds a linkbook for display and use.
 */
public class BookstandBlockEntity extends MystcraftBlockEntity {

    // TODO: Add book item storage
    // TODO: Add rotation/display state

    public BookstandBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BOOKSTAND.get(), pos, blockState);
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
