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
   * Light level used by the block properties — bright when active, dim
   * otherwise.
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

    // BE-aware validation: walk our 6 neighbours, read any LinkPortal BE
    // pointers, and consult the named receptacle directly. No BFS through
    // portal cells required — much faster and avoids cascading-update
    // ordering issues that plagued the legacy {@link PortalUtils#validatePortal}
    // path during cluster crystal breaks.
    PortalUtils.validatePortalFromCrystal(level, pos);
  }

  /**
   * When an ACTIVE crystal is broken (or replaced with anything that isn't
   * the same crystal block, e.g. AIR from a player break or a fill command),
   * propagate the teardown to every connected portal. Without this, breaking
   * a frame crystal leaves the portal blocks lit forever — the legacy
   * {@code neighborChanged} path doesn't fire when the block is removed
   * outright (it fires for OTHER blocks' updates, not for the crystal's own
   * removal), so the BE-pointer-driven shutdown is the only deterministic
   * way to catch this.
   */
  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos,
                       BlockState newState, boolean movedByPiston) {
    if (!state.is(newState.getBlock())) {
      if (state.getValue(ACTIVE)) {
        PortalUtils.shutdownPortalFromCrystal(level, pos);
      }
      super.onRemove(state, level, pos, newState, movedByPiston);
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
