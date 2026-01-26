package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Ink Mixer.
 * Handles mixing of inks and dyes to create colored inks for page writing.
 */
public class InkMixerBlockEntity extends MystcraftBlockEntity {

    // TODO: Add fluid tank for ink storage
    // TODO: Add inventory for dye items
    // TODO: Add mixing logic

    public InkMixerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INK_MIXER.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        // TODO: Save fluid tank and inventory
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        // TODO: Load fluid tank and inventory
    }
}
