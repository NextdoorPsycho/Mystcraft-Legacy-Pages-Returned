package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/**
 * Populates caves with dripstone features (stalactites and stalagmites).
 * Generates pointed dripstone hanging from ceilings and rising from floors,
 * as well as clusters of dripstone blocks.
 */
public class DripstoneCavesPopulator implements IPopulate {

    private final long seed;
    private static final int ATTEMPTS_PER_CHUNK = 15;
    private static final int MAX_Y = 60;

    public DripstoneCavesPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX();
        int chunkZ = chunkPos.getZ();

        for (int attempt = 0; attempt < ATTEMPTS_PER_CHUNK; attempt++) {
            int x = chunkX + random.nextInt(16);
            int y = world.getMinBuildHeight() + random.nextInt(MAX_Y - world.getMinBuildHeight());
            int z = chunkZ + random.nextInt(16);

            BlockPos pos = new BlockPos(x, y, z);

            if (random.nextBoolean()) {
                tryPlaceDripstoneCluster(world, random, pos);
            } else {
                tryPlacePointedDripstone(world, random, pos);
            }
        }
    }

    /**
     * Attempts to place a cluster of dripstone blocks.
     */
    private void tryPlaceDripstoneCluster(ServerLevel world, RandomSource random, BlockPos center) {
        if (!world.getBlockState(center).isAir()) {
            return;
        }

        // Check if we're in a cave (air surrounded by stone)
        if (!isInCave(world, center)) {
            return;
        }

        int clusterSize = 3 + random.nextInt(4);

        for (int i = 0; i < clusterSize; i++) {
            int offsetX = random.nextInt(5) - 2;
            int offsetY = random.nextInt(5) - 2;
            int offsetZ = random.nextInt(5) - 2;

            BlockPos pos = center.offset(offsetX, offsetY, offsetZ);

            if (world.getBlockState(pos).isAir() && hasStoneAdjacent(world, pos)) {
                world.setBlock(pos, Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 2);
            }
        }
    }

    /**
     * Attempts to place pointed dripstone (stalactite or stalagmite).
     */
    private void tryPlacePointedDripstone(ServerLevel world, RandomSource random, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) {
            return;
        }

        // Try to place stalactite (hanging from ceiling)
        BlockPos above = pos.above();
        BlockState aboveState = world.getBlockState(above);
        if (canSupportDripstone(aboveState)) {
            placeStalactite(world, random, pos);
            return;
        }

        // Try to place stalagmite (rising from floor)
        BlockPos below = pos.below();
        BlockState belowState = world.getBlockState(below);
        if (canSupportDripstone(belowState)) {
            placeStalagmite(world, random, pos);
        }
    }

    /**
     * Places a stalactite hanging from the ceiling.
     */
    private void placeStalactite(ServerLevel world, RandomSource random, BlockPos startPos) {
        int length = 1 + random.nextInt(4);

        for (int i = 0; i < length; i++) {
            BlockPos pos = startPos.below(i);

            if (!world.getBlockState(pos).isAir()) {
                break;
            }

            DripstoneThickness thickness = getDripstoneThickness(i, length);
            BlockState state = Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                    .setValue(PointedDripstoneBlock.THICKNESS, thickness);

            world.setBlock(pos, state, 2);
        }
    }

    /**
     * Places a stalagmite rising from the floor.
     */
    private void placeStalagmite(ServerLevel world, RandomSource random, BlockPos startPos) {
        int length = 1 + random.nextInt(4);

        for (int i = 0; i < length; i++) {
            BlockPos pos = startPos.above(i);

            if (!world.getBlockState(pos).isAir()) {
                break;
            }

            DripstoneThickness thickness = getDripstoneThickness(i, length);
            BlockState state = Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
                    .setValue(PointedDripstoneBlock.THICKNESS, thickness);

            world.setBlock(pos, state, 2);
        }
    }

    /**
     * Determines the thickness of a dripstone segment based on its position.
     */
    private DripstoneThickness getDripstoneThickness(int index, int totalLength) {
        if (totalLength == 1) {
            return DripstoneThickness.TIP;
        }

        if (index == 0) {
            return DripstoneThickness.BASE;
        } else if (index == totalLength - 1) {
            return DripstoneThickness.TIP;
        } else if (index == 1 || index == totalLength - 2) {
            return DripstoneThickness.FRUSTUM;
        } else {
            return DripstoneThickness.MIDDLE;
        }
    }

    /**
     * Checks if the block state can support dripstone growth.
     */
    private boolean canSupportDripstone(BlockState state) {
        return state.is(Blocks.STONE) ||
                state.is(Blocks.DEEPSLATE) ||
                state.is(Blocks.DRIPSTONE_BLOCK) ||
                state.is(Blocks.CALCITE);
    }

    /**
     * Checks if the position is in a cave (air with stone nearby).
     */
    private boolean isInCave(ServerLevel world, BlockPos pos) {
        int stoneCount = 0;
        int airCount = 0;

        for (Direction direction : Direction.values()) {
            BlockState state = world.getBlockState(pos.relative(direction));
            if (state.isAir()) {
                airCount++;
            } else if (state.isSolid()) {
                stoneCount++;
            }
        }

        return airCount >= 2 && stoneCount >= 2;
    }

    /**
     * Checks if there is stone adjacent to the position.
     */
    private boolean hasStoneAdjacent(ServerLevel world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState state = world.getBlockState(pos.relative(direction));
            if (state.isSolid()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:dripstone_caves";
    }
}
