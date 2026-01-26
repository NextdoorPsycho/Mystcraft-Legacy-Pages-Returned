package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Mystcraft Lectern.
 * Extends BookstandBlockEntity as it has the same functionality.
 * Holds a linkbook or agebook for display and use.
 */
public class LecternBlockEntity extends BookstandBlockEntity {

    public LecternBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LECTERN.get(), pos, blockState);
    }
}
