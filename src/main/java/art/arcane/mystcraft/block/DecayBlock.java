package art.arcane.mystcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Decay block.
 * Spreads through unstable Ages, destroying blocks in its path.
 */
public class DecayBlock extends Block {

    public DecayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // TODO: Implement decay spreading logic
        // - Check adjacent blocks
        // - Convert to decay if not immune
        // - Consider stability rating of the Age
    }

    // TODO: Add decay immunity checking
    // TODO: Add visual effects
}
