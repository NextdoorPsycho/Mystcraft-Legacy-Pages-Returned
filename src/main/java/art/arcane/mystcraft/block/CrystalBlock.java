package art.arcane.mystcraft.block;

import art.arcane.mystcraft.portal.PortalUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The Crystal block.
 * Powers book receptacles to create portals.
 * When active, becomes part of the portal structure.
 */
public class CrystalBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final DirectionProperty SOURCE_DIRECTION = BlockStateProperties.FACING;

    public CrystalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(ACTIVE, false)
                .setValue(SOURCE_DIRECTION, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, SOURCE_DIRECTION);
    }

    /**
     * Sets this crystal as part of an active portal.
     */
    public void setActive(Level level, BlockPos pos, Direction sourceDirection) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() == this) {
            level.setBlock(pos, current
                    .setValue(ACTIVE, true)
                    .setValue(SOURCE_DIRECTION, sourceDirection), 2);
        }
    }

    /**
     * Deactivates this crystal's portal state.
     */
    public void setInactive(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() == this) {
            level.setBlock(pos, defaultBlockState(), 2);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }

        if (!state.getValue(ACTIVE)) {
            return;
        }

        // Follow the full direction chain to verify the receptacle is still reachable.
        if (PortalUtils.findReceptacle(level, pos) == null) {
            setInactive(level, pos);
            PortalUtils.validatePortal(level, pos);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(ACTIVE) ? 15 : 0;
    }

    /**
     * Gets the light level based on active state.
     * This is handled via block properties in 1.20.
     */
    public static int getLightLevel(BlockState state) {
        return state.getValue(ACTIVE) ? 8 : 0;
    }
}
