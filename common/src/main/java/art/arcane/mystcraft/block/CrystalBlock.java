package art.arcane.mystcraft.block;

import art.arcane.mystcraft.portal.PortalUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Crystal block. Forms the frame of a Mystcraft portal.
 * <p>
 * The crystal is non-directional; orientation is irrelevant since the visual
 * model is symmetric. Only the {@link #ACTIVE} property is tracked, which:
 * <ul>
 *   <li>Lights the block when part of a live portal frame.</li>
 *   <li>Drives the analog redstone signal.</li>
 *   <li>Marks crystals participating in portal generation/teardown.</li>
 * </ul>
 * Receptacle attachment, frame discovery, and chain validation are all handled
 * by {@link PortalUtils} via breadth-first traversal — no per-block direction
 * pointer is required.
 */
public class CrystalBlock extends Block {

  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

  public CrystalBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
  }

  /**
   * Light level used by the block properties — bright when active, dim otherwise.
   */
  public static int getLightLevel(BlockState state) {
    return state.getValue(ACTIVE) ? 8 : 0;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(ACTIVE);
  }

  @Override
  public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                              BlockPos neighborPos, boolean movedByPiston) {
    if (level.isClientSide || !state.getValue(ACTIVE)) {
      return;
    }

    // If the live portal we belong to can no longer reach a receptacle,
    // tear it down. validatePortal() handles the cascading cleanup.
    if (PortalUtils.findReceptacle(level, pos) == null) {
      level.setBlock(pos, defaultBlockState(), Block.UPDATE_CLIENTS);
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
}
