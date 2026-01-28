package art.arcane.mystcraft.portal;

import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.block.CrystalBlock;
import art.arcane.mystcraft.block.LinkPortalBlock;
import art.arcane.mystcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Utility class for portal management.
 * Handles portal creation, validation, and destruction.
 */
public final class PortalUtils {

    private PortalUtils() {}

    /**
     * Gets the portal block.
     */
    public static Block getPortalBlock() {
        return ModBlocks.LINK_PORTAL.get();
    }

    /**
     * Gets the crystal frame block.
     */
    public static Block getFrameBlock() {
        return ModBlocks.CRYSTAL.get();
    }

    /**
     * Gets the receptacle block.
     */
    public static Block getReceptacleBlock() {
        return ModBlocks.BOOK_RECEPTACLE.get();
    }

    /**
     * Checks if a block state is a valid portal/frame block.
     * @return 1 if valid, 0 if not
     */
    public static int isValidLinkPortalBlock(BlockState state) {
        if (state.getBlock() == getFrameBlock()) return 1;
        if (state.getBlock() == getPortalBlock()) return 1;
        if (state.getBlock() == getReceptacleBlock()) return 1;
        return 0;
    }

    /**
     * Gets the source direction from a portal-related block state.
     */
    private static Direction getBlockFacing(BlockState state) {
        if (state.getBlock() == getFrameBlock()) {
            return state.getValue(CrystalBlock.SOURCE_DIRECTION);
        }
        if (state.getBlock() == getPortalBlock()) {
            return state.getValue(LinkPortalBlock.SOURCE_DIRECTION);
        }
        if (state.getBlock() == getReceptacleBlock()) {
            return state.getValue(BookReceptacleBlock.FACING);
        }
        return Direction.DOWN;
    }

    /**
     * Checks if a portal block is active (part of a portal).
     */
    private static boolean isBlockActive(BlockState state) {
        if (state.getBlock() == getFrameBlock()) {
            return state.getValue(CrystalBlock.ACTIVE);
        }
        if (state.getBlock() == getPortalBlock()) {
            return state.getValue(LinkPortalBlock.ACTIVE);
        }
        return false;
    }

    /**
     * Gets a state with direction and active flag set.
     */
    private static BlockState getDirectedState(BlockState state, Direction facing) {
        if (state.getBlock() == getFrameBlock()) {
            return state.setValue(CrystalBlock.ACTIVE, true)
                       .setValue(CrystalBlock.SOURCE_DIRECTION, facing);
        }
        if (state.getBlock() == getPortalBlock()) {
            return state.setValue(LinkPortalBlock.ACTIVE, true)
                       .setValue(LinkPortalBlock.SOURCE_DIRECTION, facing);
        }
        return state;
    }

    /**
     * Gets a state with active flag disabled.
     */
    private static BlockState getDisabledState(BlockState state) {
        if (state.getBlock() == getFrameBlock()) {
            return state.setValue(CrystalBlock.ACTIVE, false);
        }
        if (state.getBlock() == getPortalBlock()) {
            return state.setValue(LinkPortalBlock.ACTIVE, false);
        }
        return state;
    }

    /**
     * Validates a portal starting from the given position.
     * Removes invalid portal blocks.
     */
    public static void validatePortal(Level level, BlockPos start) {
        if (level.isClientSide) return;

        List<BlockPos> blocks = new LinkedList<>();
        blocks.add(start);
        while (!blocks.isEmpty()) {
            BlockPos pos = blocks.remove(0);
            if (level.getBlockState(pos).getBlock() != getPortalBlock()) {
                continue;
            }
            validatePortalBlock(level, pos, blocks);
        }
    }

    /**
     * Fires (activates) a portal from the receptacle position.
     * Flood fill starts from the crystal behind the receptacle, not the receptacle itself.
     */
    public static void firePortal(Level level, BlockPos receptaclePos) {
        if (level.isClientSide) return;

        BlockState receptacleState = level.getBlockState(receptaclePos);
        BlockPos crystalPos = receptaclePos;
        if (receptacleState.getBlock() == getReceptacleBlock()) {
            Direction facing = receptacleState.getValue(BookReceptacleBlock.FACING);
            crystalPos = receptaclePos.relative(facing.getOpposite());
        }

        createPortalBlocks(level, crystalPos);
        pathToReceptacle(level, receptaclePos);
    }

    /**
     * Shuts down (deactivates) a portal from the receptacle position.
     */
    public static void shutdownPortal(Level level, BlockPos receptaclePos) {
        if (level.isClientSide) return;

        depolarizeAll(level, receptaclePos);
    }

    /**
     * Creates portal blocks in valid air spaces near the receptacle.
     */
    private static void createPortalBlocks(Level level, BlockPos start) {
        LinkedList<BlockPos> toCheck = new LinkedList<>();
        Stack<BlockPos> created = new Stack<>();
        addSurrounding(toCheck, start);

        while (!toCheck.isEmpty()) {
            BlockPos pos = toCheck.remove(0);
            expandPortal(level, pos, toCheck, created);
        }

        // Validate newly created blocks
        while (!created.isEmpty()) {
            BlockPos pos = created.pop();
            if (!checkPortalTension(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
            }
        }
    }

    /**
     * Paths all portal/crystal blocks to point toward the receptacle.
     */
    private static void pathToReceptacle(Level level, BlockPos receptaclePos) {
        List<BlockPos> crystals = new LinkedList<>();
        List<BlockPos> portals = new LinkedList<>();
        List<BlockPos> repath = new LinkedList<>();
        List<BlockPos> redraw = new LinkedList<>();
        crystals.add(receptaclePos);

        while (!portals.isEmpty() || !crystals.isEmpty()) {
            while (!crystals.isEmpty()) {
                BlockPos pos = crystals.remove(0);
                directPortal(level, pos.east(), Direction.WEST, crystals, portals);
                directPortal(level, pos.above(), Direction.DOWN, crystals, portals);
                directPortal(level, pos.south(), Direction.NORTH, crystals, portals);
                directPortal(level, pos.west(), Direction.EAST, crystals, portals);
                directPortal(level, pos.below(), Direction.UP, crystals, portals);
                directPortal(level, pos.north(), Direction.SOUTH, crystals, portals);
                redraw.add(pos);
            }
            if (!portals.isEmpty()) {
                BlockPos pos = portals.remove(0);
                directPortal(level, pos.east(), Direction.WEST, crystals, portals);
                directPortal(level, pos.above(), Direction.DOWN, crystals, portals);
                directPortal(level, pos.south(), Direction.NORTH, crystals, portals);
                directPortal(level, pos.west(), Direction.EAST, crystals, portals);
                directPortal(level, pos.below(), Direction.UP, crystals, portals);
                directPortal(level, pos.north(), Direction.SOUTH, crystals, portals);
                if (level.getBlockState(pos).getBlock() == getPortalBlock()) {
                    repath.add(pos);
                }
            }
        }

        // Validate repathable blocks
        while (!repath.isEmpty()) {
            BlockPos pos = repath.remove(0);
            if (level.getBlockState(pos).getBlock() == getPortalBlock()) {
                if (!isPortalBlockStable(level, pos)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
                    addSurrounding(repath, pos);
                } else {
                    redraw.add(pos);
                }
            }
        }

        // Update all affected blocks
        for (BlockPos pos : redraw) {
            if (level.hasChunkAt(pos)) {
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Depolarizes all portal blocks connected to the receptacle.
     */
    private static void depolarizeAll(Level level, BlockPos start) {
        List<BlockPos> blocks = new LinkedList<>();
        List<BlockPos> notify = new LinkedList<>();
        blocks.add(start);

        while (!blocks.isEmpty()) {
            BlockPos pos = blocks.remove(0);
            depolarize(level, pos.east(), blocks);
            depolarize(level, pos.above(), blocks);
            depolarize(level, pos.south(), blocks);
            depolarize(level, pos.west(), blocks);
            depolarize(level, pos.below(), blocks);
            depolarize(level, pos.north(), blocks);
            notify.add(pos);
        }

        for (BlockPos pos : notify) {
            if (level.hasChunkAt(pos)) {
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Checks if a portal block is stable (has tension and connects to receptacle).
     */
    private static boolean isPortalBlockStable(Level level, BlockPos pos) {
        if (level.isClientSide) return true;
        if (!checkPortalTension(level, pos)) return false;
        return findReceptacle(level, pos) != null;
    }

    /**
     * Checks if a portal block has enough support (tension).
     * A portal block needs at least 2 axes with valid neighbors on opposite sides.
     */
    private static boolean checkPortalTension(Level level, BlockPos pos) {
        if (level.isClientSide) return true;

        int score = 0;
        // X axis
        if (isValidLinkPortalBlock(level.getBlockState(pos.east())) > 0 &&
            isValidLinkPortalBlock(level.getBlockState(pos.west())) > 0) {
            ++score;
        }
        // Y axis
        if (isValidLinkPortalBlock(level.getBlockState(pos.above())) > 0 &&
            isValidLinkPortalBlock(level.getBlockState(pos.below())) > 0) {
            ++score;
        }
        // Z axis
        if (isValidLinkPortalBlock(level.getBlockState(pos.south())) > 0 &&
            isValidLinkPortalBlock(level.getBlockState(pos.north())) > 0) {
            ++score;
        }
        return score > 1;
    }

    /**
     * Validates a single portal block and removes it if invalid.
     */
    private static void validatePortalBlock(Level level, BlockPos pos, Collection<BlockPos> blocks) {
        if (!isPortalBlockStable(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            addSurrounding(blocks, pos);
        }
    }

    /**
     * Adds all surrounding positions (6 direct + 12 edge neighbors) to a collection.
     */
    private static void addSurrounding(Collection<BlockPos> set, BlockPos pos) {
        // Direct neighbors
        set.add(pos.east());
        set.add(pos.west());
        set.add(pos.above());
        set.add(pos.below());
        set.add(pos.south());
        set.add(pos.north());

        // Edge neighbors
        set.add(pos.east().above());
        set.add(pos.west().above());
        set.add(pos.east().below());
        set.add(pos.west().below());
        set.add(pos.south().above());
        set.add(pos.north().above());
        set.add(pos.south().below());
        set.add(pos.north().below());
        set.add(pos.east().south());
        set.add(pos.west().south());
        set.add(pos.east().north());
        set.add(pos.west().north());
    }

    /**
     * Expands portal by creating a portal block at an air position if valid.
     */
    private static void expandPortal(Level level, BlockPos pos, Collection<BlockPos> set, Stack<BlockPos> created) {
        if (!level.getBlockState(pos).isAir()) return;

        int score = isValidLinkPortalBlock(level.getBlockState(pos.east())) +
                    isValidLinkPortalBlock(level.getBlockState(pos.west())) +
                    isValidLinkPortalBlock(level.getBlockState(pos.above())) +
                    isValidLinkPortalBlock(level.getBlockState(pos.below())) +
                    isValidLinkPortalBlock(level.getBlockState(pos.south())) +
                    isValidLinkPortalBlock(level.getBlockState(pos.north()));

        if (score > 1) {
            level.setBlock(pos, getPortalBlock().defaultBlockState(), Block.UPDATE_NONE);
            created.push(pos);
            addSurrounding(set, pos);
        }
    }

    /**
     * Directs a portal/crystal block toward the receptacle.
     */
    private static void directPortal(Level level, BlockPos pos, Direction facing, List<BlockPos> crystals, List<BlockPos> portals) {
        BlockState state = level.getBlockState(pos);
        if (isValidLinkPortalBlock(state) == 0) return;
        if (isBlockActive(state)) return;

        level.setBlock(pos, getDirectedState(state, facing), Block.UPDATE_NONE);
        if (state.getBlock() == getPortalBlock()) {
            portals.add(pos);
        } else {
            crystals.add(pos);
        }
    }

    /**
     * Depolarizes (deactivates) a portal/crystal block.
     */
    private static void depolarize(Level level, BlockPos pos, List<BlockPos> blocks) {
        BlockState state = level.getBlockState(pos);
        if (isValidLinkPortalBlock(state) == 0) return;
        if (!isBlockActive(state)) return;

        level.setBlock(pos, state.getBlock().defaultBlockState(), Block.UPDATE_NONE);
        if (state.getBlock() == getPortalBlock() && !isPortalBlockStable(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        blocks.add(pos);
    }

    /**
     * Finds the receptacle block entity by following the direction chain from a portal block.
     */
    public static BlockEntity findReceptacle(Level level, BlockPos pos) {
        HashSet<BlockPos> visited = new HashSet<>();
        BlockState state = level.getBlockState(pos);

        while (state.getBlock() != getReceptacleBlock()) {
            if (isValidLinkPortalBlock(state) == 0) return null;
            if (!isBlockActive(state)) return null;
            if (!visited.add(pos)) return null; // Cycle detection

            pos = pos.relative(getBlockFacing(state));
            state = level.getBlockState(pos);
        }
        return level.getBlockEntity(pos);
    }
}
