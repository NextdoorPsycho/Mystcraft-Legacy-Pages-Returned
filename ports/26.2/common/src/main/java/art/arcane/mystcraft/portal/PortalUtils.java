package art.arcane.mystcraft.portal;

import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.block.CrystalBlock;
import art.arcane.mystcraft.block.LinkPortalBlock;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.LinkPortalBlockEntity;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
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

  private static final int MAX_PORTAL_BLOCKS = 1024;

  private static final int MAX_DISCOVERY_VISITS = 4096;

  /**
   * Re-entry guard for the teardown cascade. {@link #shutdownPortal} clears
   * portal blocks via {@code Level.setBlock(AIR)}, which fires
   * {@link LinkPortalBlock#onRemove} on each cleared cell — and that hook
   * tries to tear down the whole portal again, causing O(N²) recursion (and
   * StackOverflow once N gets high enough). The guard makes the recursive
   * shutdownPortal calls no-ops while an outer teardown is in progress.
   * <p>
   * Thread-local because the level lock is per-thread on the main server
   * thread and chunkgen / async modders may invoke us from other threads;
   * we want each thread to have its own guard rather than serialise all
   * teardowns globally.
   */
  private static final ThreadLocal<Boolean> IN_SHUTDOWN = ThreadLocal.withInitial(() -> false);

  /**
   * Used by {@link LinkPortalBlock#onRemove} to detect "I'm being cleared by
   * an outer teardown, don't recurse."
   */
  public static boolean isShuttingDown() {
    return IN_SHUTDOWN.get();
  }

  private PortalUtils() {
  }

  public static Block getPortalBlock() {
    return ModBlocks.LINK_PORTAL.get();
  }

  /**
   * Run the same flood-fill {@link #fillPortalBlocks} would do, but without
   * placing any portal blocks. Used by {@code /mystcraft diag portal} to help
   * the player locate gaps in their frame.
   * <p>
   * Aimed crystal becomes the seed: BFS in the plane perpendicular to the
   * crystal's ACTIVE-receptacle axis (or all 3 axes tried, returning the one
   * that closes the smallest interior).
   *
   * @return diagnosis, or {@code null} if no Crystal/Receptacle context found.
   */
  public static FrameDiagnosis diagnoseFrame(Level level, BlockPos seedCrystalPos) {
    BlockState seedState = level.getBlockState(seedCrystalPos);
    if (!seedState.is(getFrameBlock())) {
      return null;
    }

    FrameDiagnosis best = null;
    for (Direction.Axis axis : Direction.Axis.values()) {
      FrameDiagnosis d = doFrameBfs(level, seedCrystalPos, axis);
      if (d == null) continue;
      if (best == null) {
        best = d;
        continue;
      }

      if (d.leaked() != best.leaked()) {
        if (!d.leaked()) best = d;
        continue;
      }

      if (!d.leaked() && d.airCellCount() < best.airCellCount() && d.airCellCount() > 0) {
        best = d;
      } else if (d.leaked() && d.airCellCount() > best.airCellCount()) {
        best = d;
      }
    }
    return best;
  }

  /**
   * Like {@link #diagnoseFrame} but for a specific {@code axis} only. Used by
   * {@code /mystcraft diag portal} to show per-axis BFS results so the player
   * can see which axis closes vs leaks at a glance.
   */
  public static FrameDiagnosis diagnoseFrameOnAxis(Level level, BlockPos seedCrystalPos, Direction.Axis axis) {
    BlockState seedState = level.getBlockState(seedCrystalPos);
    if (!seedState.is(getFrameBlock())) {
      return null;
    }
    return doFrameBfs(level, seedCrystalPos, axis);
  }

  private static FrameDiagnosis doFrameBfs(Level level, BlockPos seed, Direction.Axis axis) {

    Set<BlockPos> visited = new HashSet<>();
    Set<BlockPos> closedAir = new HashSet<>();
    Set<BlockPos> leakedAir = new HashSet<>();

    for (Direction d : Direction.values()) {
      if (d.getAxis() == axis) continue;
      BlockPos start = seed.relative(d);
      if (visited.contains(start)) continue;
      BlockState s0 = level.getBlockState(start);
      if (!s0.isAir()) continue;

      Set<BlockPos> region = new HashSet<>();
      Deque<BlockPos> frontier = new ArrayDeque<>();
      frontier.add(start);
      boolean leaked = false;
      while (!frontier.isEmpty()) {
        BlockPos pos = frontier.poll();
        if (!visited.add(pos)) continue;
        BlockState s = level.getBlockState(pos);
        if (!s.isAir()) continue;
        region.add(pos);
        if (region.size() > MAX_PORTAL_BLOCKS) {
          leaked = true;
          break;
        }
        addInPlaneNeighbors(frontier, pos, axis);
      }

      if (leaked) {
        leakedAir.addAll(region);
      } else if (!region.isEmpty()) {
        closedAir.addAll(region);
      }
    }

    boolean anyClosed = !closedAir.isEmpty();
    Set<BlockPos> reportCells = anyClosed ? closedAir : leakedAir;
    if (reportCells.isEmpty()) {
      return null;
    }

    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    for (BlockPos pos : reportCells) {
      minX = Math.min(minX, pos.getX());
      minY = Math.min(minY, pos.getY());
      minZ = Math.min(minZ, pos.getZ());
      maxX = Math.max(maxX, pos.getX());
      maxY = Math.max(maxY, pos.getY());
      maxZ = Math.max(maxZ, pos.getZ());
    }

    java.util.List<BlockPos> samples = new java.util.ArrayList<>();
    if (!anyClosed) {

      java.util.List<BlockPos> sorted = new java.util.ArrayList<>(reportCells);
      sorted.sort((a, b) -> {
        double da = a.distSqr(seed);
        double db = b.distSqr(seed);
        return Double.compare(db, da);
      });
      for (int i = 0; i < Math.min(5, sorted.size()); i++) {
        samples.add(sorted.get(i));
      }
    }
    return new FrameDiagnosis(
        reportCells.size(),
        !anyClosed,
        new BlockPos(minX, minY, minZ),
        new BlockPos(maxX, maxY, maxZ),
        samples
    );
  }

  public static Block getFrameBlock() {
    return ModBlocks.CRYSTAL.get();
  }

  public static Block getReceptacleBlock() {
    return ModBlocks.BOOK_RECEPTACLE.get();
  }

  /**
   * @return {@code true} if the block is a frame, portal, or receptacle.
   */
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
    if (level.isClientSide()) {
      return;
    }
    BlockState receptacleState = level.getBlockState(receptaclePos);
    if (!(receptacleState.getBlock() instanceof BookReceptacleBlock)) {
      art.arcane.mystcraft.Mystcraft.LOGGER.warn(
          "[Portal] firePortal at {}: block is {} not BookReceptacleBlock — aborting",
          receptaclePos, receptacleState.getBlock().getDescriptionId());
      return;
    }
    Direction facing = receptacleState.getValue(BookReceptacleBlock.FACING);
    BlockPos crystalSeed = receptaclePos.relative(facing.getOpposite());
    BlockState seedState = level.getBlockState(crystalSeed);
    if (!seedState.is(getFrameBlock())) {
      art.arcane.mystcraft.Mystcraft.LOGGER.warn(
          "[Portal] firePortal at {}: no Crystal at {} (found {}) — aborting",
          receptaclePos, crystalSeed, seedState.getBlock().getDescriptionId());
      notifyNearbyPlayers(level, receptaclePos, "mystcraft.portal.no_crystal_seed");
      return;
    }

    Direction.Axis axis = facing.getAxis();
    art.arcane.mystcraft.Mystcraft.LOGGER.info(
        "[Portal] firePortal at {}: facing={} crystalSeed={} axis={}",
        receptaclePos, facing, crystalSeed, axis);

    Set<BlockPos> placedPositions = computeAndPlacePortalBlocks(level, crystalSeed, axis);
    int placed = placedPositions == null ? -1 : placedPositions.size();
    int activated = activateConnectedCrystals(level, receptaclePos);

    art.arcane.mystcraft.Mystcraft.LOGGER.info(
        "[Portal] firePortal at {}: placed {} portal blocks (axis={}), activated {} crystals",
        receptaclePos, placed, axis, activated);

    if (placed > 0) {
      // Stamp every placed cell with the receptacle's colour, source pointer
      // and book NBT snapshot. The colour computation runs ONCE on the
      // server here — never again. Both Forge and Fabric tint providers
      // read this stamped value as their single source of truth, killing
      // the multi-colour-per-portal class of bugs.
      stampPortalCells(level, receptaclePos, placedPositions);
      return;
    }

    if (placed == -1) {
      Direction.Axis otherClosedAxis = findOtherClosingAxis(level, crystalSeed, axis);
      if (otherClosedAxis != null) {
        art.arcane.mystcraft.Mystcraft.LOGGER.info(
            "[Portal] firePortal at {}: hint — frame closes on axis {} but receptacle is on axis {}; "
                + "receptacle is likely mounted on the wrong (in-plane) face of an edge crystal.",
            receptaclePos, otherClosedAxis, axis);
        notifyNearbyPlayers(level, receptaclePos, "mystcraft.portal.receptacle_wrong_face");
        return;
      }
      notifyNearbyPlayers(level, receptaclePos, "mystcraft.portal.frame_open");
    } else if (placed == 0 && activated >= 1) {
      notifyNearbyPlayers(level, receptaclePos, "mystcraft.portal.no_interior");
    }
  }

  private static Direction.Axis findOtherClosingAxis(Level level, BlockPos seed, Direction.Axis excluded) {
    for (Direction.Axis axis : Direction.Axis.values()) {
      if (axis == excluded) continue;
      if (countClosedInterior(level, seed, axis) > 0) {
        return axis;
      }
    }
    return null;
  }

  private static int countClosedInterior(Level level, BlockPos seed, Direction.Axis axis) {
    Set<BlockPos> visited = new HashSet<>();
    int total = 0;
    for (Direction d : Direction.values()) {
      if (d.getAxis() == axis) continue;
      BlockPos start = seed.relative(d);
      if (visited.contains(start)) continue;
      BlockState s0 = level.getBlockState(start);
      if (!s0.isAir()) continue;

      Set<BlockPos> region = new HashSet<>();
      Deque<BlockPos> frontier = new ArrayDeque<>();
      frontier.add(start);
      boolean leaked = false;
      while (!frontier.isEmpty()) {
        BlockPos pos = frontier.poll();
        if (!visited.add(pos)) continue;
        BlockState s = level.getBlockState(pos);
        if (!s.isAir()) continue;
        region.add(pos);
        if (region.size() > MAX_PORTAL_BLOCKS) {
          leaked = true;
          break;
        }
        addInPlaneNeighbors(frontier, pos, axis);
      }
      if (!leaked) total += region.size();
    }
    return total;
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
    if (level.isClientSide()) {
      return;
    }
    if (IN_SHUTDOWN.get()) {
      // Outer teardown is already walking the structure. Recursive calls
      // (typically from {@link LinkPortalBlock#onRemove} during cell
      // clearing) are no-ops; the outer walk will visit every cell.
      return;
    }
    IN_SHUTDOWN.set(true);
    try {
      Set<BlockPos> visited = new HashSet<>();
      Deque<BlockPos> queue = new ArrayDeque<>();
      // Seed with the receptacle's neighbours, NOT the receptacle itself.
      // When called from BookReceptacleBlock#onRemove, the block at
      // receptaclePos is already AIR (vanilla replaces the block before
      // calling onRemove). Starting the BFS from the AIR cell would
      // immediately fail the isPortalStructure check and terminate without
      // visiting any portal cells. Seeding with the six neighbours is the
      // correct entry into the connected portal structure.
      visited.add(receptaclePos);
      enqueueAllNeighbors(queue, visited, receptaclePos);

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
    } finally {
      IN_SHUTDOWN.set(false);
    }
  }

  /**
   * Re-validate the portal containing {@code start}. Triggered from
   * {@link LinkPortalBlock#neighborChanged} and
   * {@link CrystalBlock#neighborChanged} when an adjacent block mutates
   * externally (player breaks frame, removes receptacle, …): if the controlling
   * receptacle can no longer be reached
   * <em>or</em> has no book, the entire structure is collapsed.
   */
  public static void validatePortal(Level level, BlockPos start) {
    if (level.isClientSide()) {
      return;
    }
    BlockEntity be = findReceptacle(level, start);
    if (be instanceof BookReceptacleBlockEntity receptacle && receptacle.hasBook()) {
      return;
    }

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
   * Walk neighbours of {@code crystalPos}; for any LinkPortalBlock with a
   * BE-stored receptacle pointer, shut down THAT receptacle's portal.
   * <p>
   * Used by {@link CrystalBlock#onRemove} when an active crystal is broken —
   * the BE pointer lets us name the right receptacle in O(6) without
   * re-BFS-ing through a portal structure that the player just punched a
   * hole through.
   * <p>
   * Falls back to a one-step BFS from each crystal-side neighbour if no BE
   * pointers are found (portals saved before v2 won't have them).
   */
  public static void shutdownPortalFromCrystal(Level level, BlockPos crystalPos) {
    if (level.isClientSide()) {
      return;
    }
    Set<BlockPos> seenReceptacles = new HashSet<>();
    for (Direction d : Direction.values()) {
      BlockPos neighbor = crystalPos.relative(d);
      if (level.getBlockEntity(neighbor) instanceof LinkPortalBlockEntity portalBE) {
        BlockPos rp = portalBE.getReceptaclePos();
        if (rp != null && seenReceptacles.add(rp.immutable())) {
          shutdownPortal(level, rp);
        }
      }
    }

    if (!seenReceptacles.isEmpty()) {
      return;
    }

    // Pre-v2 fallback: one-step BFS from each side. The crystal block at
    // crystalPos is now AIR (this is called from onRemove), so the BFS
    // starts from the neighbour cell.
    for (Direction d : Direction.values()) {
      BlockPos neighbor = crystalPos.relative(d);
      BlockEntity be = findReceptacle(level, neighbor);
      if (be != null) {
        shutdownPortal(level, be.getBlockPos());
        return;
      }
    }
  }

  /**
   * From a portal block: if its BE-stored receptacle pointer is dead OR the
   * receptacle has no book, tear down the entire portal it belongs to.
   * <p>
   * Replaces the legacy {@link #validatePortal} BFS for v2 portals — we
   * already know which receptacle owns this cell, no traversal required.
   * Falls back to {@link #validatePortal} when the BE has no pointer
   * (pre-v2 saved-world portals).
   */
  public static void maybeShutdownIfOrphaned(Level level, BlockPos portalBlockPos) {
    if (level.isClientSide()) {
      return;
    }
    BlockEntity be = level.getBlockEntity(portalBlockPos);
    if (!(be instanceof LinkPortalBlockEntity portalBE)) {
      validatePortal(level, portalBlockPos);
      return;
    }
    BlockPos rp = portalBE.getReceptaclePos();
    if (rp == null) {
      validatePortal(level, portalBlockPos);
      return;
    }
    if (level.getBlockEntity(rp) instanceof BookReceptacleBlockEntity recBE && recBE.hasBook()) {
      return;
    }
    shutdownPortal(level, rp);
  }

  /**
   * Like {@link #validatePortal} but reads BE pointers when available for
   * the crystal-driven path. The crystal at {@code crystalPos} hasn't been
   * removed yet (this is the {@code neighborChanged} path); it's still
   * ACTIVE. Walk its 6 neighbours: any neighbouring portal block carries a
   * BE pointer to the owning receptacle. If we can confirm the receptacle
   * still has a book, we leave the portal alone; otherwise we tear it down.
   */
  public static void validatePortalFromCrystal(Level level, BlockPos crystalPos) {
    if (level.isClientSide()) {
      return;
    }
    for (Direction d : Direction.values()) {
      BlockPos neighbor = crystalPos.relative(d);
      if (level.getBlockEntity(neighbor) instanceof LinkPortalBlockEntity portalBE) {
        BlockPos rp = portalBE.getReceptaclePos();
        if (rp != null) {
          if (level.getBlockEntity(rp) instanceof BookReceptacleBlockEntity recBE && recBE.hasBook()) {
            return;
          }
          shutdownPortal(level, rp);
          return;
        }
      }
    }
    // Pre-v2 portal blocks adjacent (or none at all): legacy BFS.
    validatePortal(level, crystalPos);
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

  /**
   * Compute the BFS-bounded interior, place portal blocks, and return the
   * set of placed positions. Returns {@code null} when no closed interior
   * is found at all; returns an empty set when the BFS completes but every
   * candidate region either leaked past {@link #MAX_PORTAL_BLOCKS} or
   * touched a non-crystal boundary (i.e. there genuinely is no enclosed
   * interior — a real "frame open" diagnosis).
   *
   * <p>The BFS runs once per cardinal in-plane direction starting from the
   * crystal seed's neighbours. Some of those neighbours are interior cells
   * (which close on crystals); others are exterior cells (which leak to the
   * void or hit barrier walls in test arenas). We discard exterior regions
   * — they're not "frame open" diagnoses, just the seed having outside
   * faces — and keep any region that is actually closed by crystals.
   *
   * <p>Regions ending in a non-crystal boundary (barrier, dirt, water…) are
   * treated as exterior leaks and discarded silently. A frame is "open"
   * only when no interior region bounded entirely by crystals/portals
   * exists. This is what stopped the gametest-arena bleed-through bug
   * where two adjacent test portals' BFSes used to claim each other's
   * cells as their own interior.
   */
  private static Set<BlockPos> computeAndPlacePortalBlocks(Level level, BlockPos crystalSeed, Direction.Axis axis) {
    Set<BlockPos> visited = new HashSet<>();
    Set<BlockPos> closedAir = new HashSet<>();
    boolean foundAnyClosedInterior = false;

    for (Direction d : Direction.values()) {
      if (d.getAxis() == axis) continue;
      BlockPos start = crystalSeed.relative(d);
      if (visited.contains(start)) continue;
      BlockState s0 = level.getBlockState(start);
      if (!s0.isAir()) continue;

      Set<BlockPos> region = new HashSet<>();
      Deque<BlockPos> frontier = new ArrayDeque<>();
      frontier.add(start);
      boolean leaked = false;
      boolean nonCrystalBoundary = false;
      while (!frontier.isEmpty()) {
        BlockPos pos = frontier.poll();
        if (!visited.add(pos)) continue;
        BlockState s = level.getBlockState(pos);
        if (!s.isAir()) {
          // Boundary cell — must be a crystal or an existing portal cell to
          // count as a sealed wall. Anything else (barrier, bedrock, dirt,
          // a different mod's block) means this region is exterior to a
          // valid portal frame and must not be lit.
          if (s.getBlock() != getFrameBlock() && s.getBlock() != getPortalBlock()) {
            nonCrystalBoundary = true;
          }
          continue;
        }
        region.add(pos);
        if (region.size() > MAX_PORTAL_BLOCKS) {
          leaked = true;
          break;
        }
        addInPlaneNeighbors(frontier, pos, axis);
      }

      // Region is valid interior iff it didn't leak AND every boundary cell
      // was a crystal or existing portal. Otherwise it's exterior — silently
      // discard.
      if (!leaked && !nonCrystalBoundary && !region.isEmpty()) {
        closedAir.addAll(region);
        foundAnyClosedInterior = true;
      }
    }

    if (!foundAnyClosedInterior) {
      art.arcane.mystcraft.Mystcraft.LOGGER.warn(
          "[Portal] computeAndPlacePortalBlocks: no closed interior found at {} on axis {} "
              + "(every BFS region either leaked past {} cells or touched a non-crystal boundary)",
          crystalSeed, axis, MAX_PORTAL_BLOCKS);
      return null;
    }

    BlockState portalState = getPortalBlock().defaultBlockState().setValue(LinkPortalBlock.AXIS, axis);
    for (BlockPos pos : closedAir) {
      level.setBlock(pos, portalState, Block.UPDATE_NONE);
    }

    for (BlockPos pos : closedAir) {
      BlockState updated = level.getBlockState(pos);
      level.sendBlockUpdated(pos, updated, updated, Block.UPDATE_ALL);
    }
    return closedAir;
  }

  /**
   * Stamp every just-placed portal cell with the receptacle's colour, the
   * receptacle pointer, and a snapshot of the book's NBT. Called from
   * {@link #firePortal} immediately after
   * {@link #computeAndPlacePortalBlocks}. Reads the colour and book stack
   * from the receptacle BE — caller has already verified the BE exists.
   */
  private static void stampPortalCells(Level level, BlockPos receptaclePos, Set<BlockPos> placedPositions) {
    if (!(level.getBlockEntity(receptaclePos) instanceof BookReceptacleBlockEntity rec)) {
      return;
    }
    int color = rec.getPortalColor();
    ItemStack book = rec.getBook();
    CompoundTag bookSnapshot = ItemStackNbt.getTag(book);
    CompoundTag stamp = bookSnapshot != null ? bookSnapshot.copy() : new CompoundTag();

    for (BlockPos p : placedPositions) {
      if (level.getBlockEntity(p) instanceof LinkPortalBlockEntity be) {
        be.stamp(receptaclePos, color, stamp);
      }
    }
  }

  private static int fillPortalBlocks(Level level, BlockPos crystalSeed, Direction.Axis axis) {
    Set<BlockPos> result = computeAndPlacePortalBlocks(level, crystalSeed, axis);
    if (result == null) return -1;
    return result.size();
  }

  private static int activateConnectedCrystals(Level level, BlockPos receptaclePos) {
    Set<BlockPos> visited = new HashSet<>();
    Deque<BlockPos> queue = new ArrayDeque<>();
    queue.add(receptaclePos);
    int activated = 0;

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
        activated++;
      }
      enqueueAllNeighbors(queue, visited, pos);
    }
    return activated;
  }

  private static void addInPlaneNeighbors(Deque<BlockPos> queue, BlockPos pos, Direction.Axis axis) {
    for (Direction d : Direction.values()) {
      if (d.getAxis() == axis) {
        continue;
      }
      queue.add(pos.relative(d));
    }
  }

  private static void enqueueAllNeighbors(Deque<BlockPos> queue, Set<BlockPos> visited, BlockPos pos) {
    for (Direction d : Direction.values()) {
      BlockPos next = pos.relative(d);
      if (!visited.contains(next)) {
        queue.add(next);
      }
    }
  }

  private static void notifyNearbyPlayers(Level level, BlockPos pos, String langKey) {
    if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
      return;
    }
    net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.translatable(langKey);
    double r2 = 8.0 * 8.0;
    for (net.minecraft.server.level.ServerPlayer p : server.players()) {
      if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r2) {
        continue;
      }

      art.arcane.mystcraft.util.PlayerMessages.send(p, msg, true);
    }
  }

  /**
   * Result of {@link #diagnoseFrame(Level, BlockPos)} — a snapshot of the
   * flood-fill the ignition path would perform from a given crystal seed.
   *
   * @param airCellCount total air cells reached (capped at
   *                     {@link #MAX_PORTAL_BLOCKS}+1)
   * @param leaked       true if the BFS hit the cap → frame is open
   * @param boundsMin    min corner of bounding box of all reached air cells
   * @param boundsMax    max corner of bounding box of all reached air cells
   * @param sampleLeaks  up to 5 cells far from the seed (helps locate the gap)
   */
  public record FrameDiagnosis(
      int airCellCount,
      boolean leaked,
      BlockPos boundsMin,
      BlockPos boundsMax,
      java.util.List<BlockPos> sampleLeaks
  ) {
  }
}
