package art.arcane.mystcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Crystal block.
 * Powers book receptacles to create portals.
 */
public class CrystalBlock extends Block {

    public CrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // TODO: Notify nearby book receptacles
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // TODO: Notify nearby book receptacles to deactivate
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // TODO: Add crystal power range
    // TODO: Add visual effects
}
