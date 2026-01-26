package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Link Modifier.
 * Used to add modifier pages to existing linkbooks.
 */
public class LinkModifierBlockEntity extends MystcraftBlockEntity {

    // TODO: Add book slot
    // TODO: Add modifier page slots
    // TODO: Add modification logic

    public LinkModifierBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LINK_MODIFIER.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        // TODO: Save inventory
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        // TODO: Load inventory
    }
}
