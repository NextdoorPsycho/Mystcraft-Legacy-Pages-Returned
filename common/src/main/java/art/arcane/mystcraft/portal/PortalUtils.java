package art.arcane.mystcraft.portal;

import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.block.CrystalBlock;
import art.arcane.mystcraft.block.LinkPortalBlock;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Portal management — frame discovery, ignition, and teardown.
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>The portal lies in the plane perpendicular to the receptacle's facing
 *       axis: {@code FACING=UP/DOWN → AXIS=Y} (horizontal portal),
 *       {@code FACING=NORTH/SOUTH → AXIS=Z}, {@code FACING=EAST/WEST → AXIS=X}.</li>
 *   <li>Portal blocks are placed by a 2-D flood-fill bounded by Crystal frame
 *       blocks. Frame blocks act as walls — the BFS does not propagate
 *       through them — so any closed loop of Crystals encloses a fillable
 *       interior of any shape (rectangular, L-shaped, donut-shaped, …).</li>
 *   <li>If the flood-fill exceeds {@link #MAX_PORTAL_BLOCKS}, the frame is
 *       considered open and ignition silently aborts (player keeps the book in
 *       the receptacle but no portal blocks are placed).</li>
 *   <li>Receptacle discovery from any portal/crystal block is a 6-direction
 *       BFS through connected portal-structure blocks. There is no per-block
 *       "back-pointer" to track.</li>
 * </ul>
 *
 * <p>All operations are server-side and bail early on the client.
 */
public final class PortalUtils {

  /** Hard cap on portal blocks placed in a single ignition. Larger frames are rejected. */
  private static final int MAX_PORTAL_BLOCKS = 1024;

  /** Hard cap on BFS visits during receptacle discovery / teardown. */
  private static final int MAX_DISCOVERY_VISITS = 4096;

  private PortalUtils() {
  }

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  public static Block getPortalBlock() {
    return ModBlocks.LINK_PORTAL.get();
  }

  public static Block getFrameBlock() {
    return ModBlocks.CRYSTAL.get();
  }

  public static Block getReceptacleBlock() {
    return ModBlocks.BOOK_RECEPTACLE.get();
  }

  /** @return {@code true} if the block is a frame, portal, or receptacle. */
  public static boolean isPortalStructure(BlockState state) {
    Block b = state.getBlock();
    return b == getFrameBlock() || b == getPortalBlock() || b == getReceptacleBlock();
  }

  /**
   * Light up the portal driven by the receptacle at {@code receptaclePos}.
   * <p>
   * Steps:
   * <ol>
   *   <li>Locate the supporting Crystal under the receptacle's mount face.</li>
   *   <li>Flood-fill air cells in the plane perpendicular to the receptacle's
   *       facing axis. Crystal frame blocks act as walls.</li>
   *   <li>Mark every Crystal reachable from the receptacle as ACTIVE.</li>
   * </ol>
   * Safe to call repeatedly; redundant invocations are no-ops.
   */
  public static void firePortal(Level level, BlockPos receptaclePos) {
    if (level.isClientSide) {
      return;
    }
    BlockState receptacleState = level.getBlockState(receptaclePos);
    if (!(receptacleState.getBlock() instanceof BookReceptacleBlock)) {
      return;
    }
    Direction facing = receptacleState.getValue(BookReceptacleBlock.FACING);
    BlockPos crystalSeed = receptaclePos.relative(facing.getOpposite());
    if (!level.getBlockState(crystalSeed).is(getFrameBlock())) {
      return; // Receptacle has lost its supporting crystal; ignition impossible.
    }

    fillPortalBlocks(level, crystalSeed, facing.getAxis());
    activateConnectedCrystals(level, receptaclePos);
  }

  /**
   * Tear down the portal owned by the receptacle at {@code receptaclePos}.
   * <p>
   * BFS from the receptacle through every connected portal-structure block.
   * Portal blocks are removed; Crystals are deactivated but kept; the
   * receptacle itself is left untouched (it's the source of truth).
   * <p>
   * Safe to call when no portal is currently lit.
   */
  public static void shutdownPortal(Level level, BlockPos receptaclePos) {
    if (level.isClientSide) {
      return;
    }
    Set<BlockPos> visited = new HashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();
    queue.add(receptaclePos);

    while (!queue.isEmpty() && visited.size() < MAX_DISCOVERY_VISITS) {
      BlockPos pos = queue.poll();
      if (!visited.add(pos)) {
        continue;
      }
      BlockState state = level.getBlockState(pos);
      if (!isPortalStructure(state)) {
        continue;
      }
      if (state.getBlock() == getFrameBlock()) {
        if (state.getValue(CrystalBlock.ACTIVE)) {
          level.setBlock(pos, state.setValue(CrystalBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
        }
      } else if (state.getBlock() == getPortalBlock()) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
      }
      enqueueAllNeighbors(queue, visited, pos);
    }
  }

  /**
   * Re-validate the portal containing {@code start}. Triggered from
   * {@link LinkPortalBlock#neighborChanged} and {@link CrystalBlock#neighborChanged}
   * when an adjacent block mutates externally (player breaks frame, removes
   * receptacle, …): if the controlling receptacle can no longer be reached
   * <em>or</em> has no book, the entire structure is collapsed.
   */
  public static void validatePortal(Level level, BlockPos start) {
    if (level.isClientSide) {
      return;
    }
    BlockEntity be = findReceptacle(level, start);
    if (be instanceof BookReceptacleBlockEntity receptacle && receptacle.hasBook()) {
      return; // Still anchored to a live receptacle — leave alone.
    }
    // No live receptacle reachable: tear everything down from this point.
    Set<BlockPos> visited = new HashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();
    queue.add(start);
    while (!queue.isEmpty() && visited.size() < MAX_DISCOVERY_VISITS) {
      BlockPos pos = queue.poll();
      if (!visited.add(pos)) {
        continue;
      }
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() == getPortalBlock()) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        enqueueAllNeighbors(queue, visited, pos);
      } else if (state.getBlock() == getFrameBlock() && state.getValue(CrystalBlock.ACTIVE)) {
        level.setBlock(pos, state.setValue(CrystalBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
        enqueueAllNeighbors(queue, visited, pos);
      }
    }
  }

  /**
   * Find the controlling receptacle for any portal/crystal block via BFS
   * through connected portal-structure blocks. Returns {@code null} if no
   * receptacle is reachable within {@link #MAX_DISCOVERY_VISITS} blocks.
   * <p>
   * Used by {@link LinkPortalBlock#entityInside} (teleport routing) and the
   * client-side portal tint colour handler.
   */
  public static BlockEntity findReceptacle(Level level, BlockPos start) {
    Set<BlockPos> visited = new HashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();
    queue.add(start);

    while (!queue.isEmpty() && visited.size() < MAX_DISCOVERY_VISITS) {
      BlockPos pos = queue.poll();
      if (!visited.add(pos)) {
        continue;
      }
      BlockState state = level.getBlockState(pos);
      if (!isPortalStructure(state)) {
        continue;
      }
      if (state.getBlock() == getReceptacleBlock()) {
        return level.getBlockEntity(pos);
      }
      enqueueAllNeighbors(queue, visited, pos);
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Internal: ignition pipeline
  // ---------------------------------------------------------------------------

  /**
   * 2-D flood-fill of air cells in the plane perpendicular to {@code axis},
   * seeded from the in-plane neighbors of {@code crystalSeed}.
   * <p>
   * Frame blocks (and existing portal/receptacle blocks) act as walls: the
   * BFS does not enter them. The air cells reached form the portal interior.
   * <p>
   * If the fill exceeds {@link #MAX_PORTAL_BLOCKS} the frame is considered
   * open (leaks to outside) and the entire ignition is aborted — no portal
   * blocks are placed.
   */
  private static void fillPortalBlocks(Level level, BlockPos crystalSeed, Direction.Axis axis) {
    Set<BlockPos> tentative = new HashSet<>();
    Set<BlockPos> visited = new HashSet<>();
    Deque<BlockPos> frontier = new ArrayDeque<>();
    addInPlaneNeighbors(frontier, crystalSeed, axis);

    while (!frontier.isEmpty()) {
      BlockPos pos = frontier.poll();
      if (!visited.add(pos)) {
        continue;
      }
      BlockState s = level.getBlockState(pos);
      if (!s.isAir()) {
        continue; // Walls (frame, existing portal, anything solid) stop the fill.
      }
      tentative.add(pos);
      if (tentative.size() > MAX_PORTAL_BLOCKS) {
        // Frame is open — air leaks to the outside world. Abort silently.
        return;
      }
      addInPlaneNeighbors(frontier, pos, axis);
    }

    if (tentative.isEmpty()) {
      return;
    }

    BlockState portalState = getPortalBlock().defaultBlockState().setValue(LinkPortalBlock.AXIS, axis);
    for (BlockPos pos : tentative) {
      level.setBlock(pos, portalState, Block.UPDATE_NONE);
    }
    // Single batched client update.
    for (BlockPos pos : tentative) {
      BlockState updated = level.getBlockState(pos);
      level.sendBlockUpdated(pos, updated, updated, Block.UPDATE_ALL);
    }
  }

  /**
   * BFS from the receptacle through connected portal-structure blocks and mark
   * every Crystal as ACTIVE. Crystals that aren't part of the lit network keep
   * their default unlit state.
   */
  private static void activateConnectedCrystals(Level level, BlockPos receptaclePos) {
    Set<BlockPos> visited = new HashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();
    queue.add(receptaclePos);

    while (!queue.isEmpty() && visited.size() < MAX_DISCOVERY_VISITS) {
      BlockPos pos = queue.poll();
      if (!visited.add(pos)) {
        continue;
      }
      BlockState state = level.getBlockState(pos);
      if (!isPortalStructure(state)) {
        continue;
      }
      if (state.getBlock() == getFrameBlock() && !state.getValue(CrystalBlock.ACTIVE)) {
        level.setBlock(pos, state.setValue(CrystalBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
      }
      enqueueAllNeighbors(queue, visited, pos);
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Add the four neighbors of {@code pos} that lie in the plane perpendicular
   * to {@code axis}. Used by the planar flood-fill so propagation never
   * leaves the portal plane.
   */
  private static void addInPlaneNeighbors(Deque<BlockPos> queue, BlockPos pos, Direction.Axis axis) {
    for (Direction d : Direction.values()) {
      if (d.getAxis() == axis) {
        continue;
      }
      queue.add(pos.relative(d));
    }
  }

  /** Add all six face-neighbors of {@code pos} (BFS over the structure). */
  private static void enqueueAllNeighbors(Deque<BlockPos> queue, Set<BlockPos> visited, BlockPos pos) {
    for (Direction d : Direction.values()) {
      BlockPos next = pos.relative(d);
      if (!visited.contains(next)) {
        queue.add(next);
      }
    }
  }
}
