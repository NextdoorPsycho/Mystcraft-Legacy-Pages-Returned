package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Mineshaft populator that generates underground tunnel networks.
 * Mineshafts consist of oak fence supports, rails, cobwebs, and occasional cave spider spawners.
 */
public class MineshaftsPopulator implements IPopulate {

    private final long seed;
    private final int rarity;

    private static final int DEFAULT_MINESHAFT_RARITY = 8;
    private static final int MIN_Y = 10;
    private static final int MAX_Y = 50;
    private static final int TUNNEL_LENGTH = 20;
    private static final int MAX_BRANCHES = 4;

    public MineshaftsPopulator(long seed) {
        this(seed, null);
    }

    public MineshaftsPopulator(long seed, JsonObject params) {
        this.seed = seed;
        this.rarity = PopulatorConfig.rarityFrom(params, DEFAULT_MINESHAFT_RARITY);
    }

    @Override
    public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
        if (random.nextInt(rarity) != 0) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16);
        int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y);
        int z = chunkPos.getZ() + random.nextInt(16);

        BlockPos startPos = new BlockPos(x, y, z);

        if (!isUnderground(world, startPos)) {
            return;
        }

        generateMineshaft(world, random, startPos, Direction.NORTH, 0, chunkPos);
    }

    private void generateMineshaft(WorldGenLevel world, RandomSource random, BlockPos pos, Direction direction, int depth, BlockPos chunkPos) {
        if (depth > MAX_BRANCHES) {
            return;
        }

        int length = TUNNEL_LENGTH + random.nextInt(10);

        for (int i = 0; i < length; i++) {
            BlockPos currentPos = pos.relative(direction, i);

            if (random.nextInt(20) == 0) {
                int yChange = random.nextInt(3) - 1;
                currentPos = currentPos.above(yChange);
            }

            // Skip blocks outside the writable area
            if (!isInWritableArea(currentPos, chunkPos)) {
                break;
            }

            if (!isValidMineshaftPosition(world, currentPos)) {
                break;
            }

            generateTunnelSection(world, random, currentPos, direction, chunkPos);

            if (random.nextInt(8) == 0) {
                Direction branchDir = random.nextBoolean() ?
                        direction.getClockWise() : direction.getCounterClockWise();
                generateMineshaft(world, random, currentPos, branchDir, depth + 1, chunkPos);
            }

            if (random.nextInt(60) == 0) {
                generateSpawnerRoom(world, random, currentPos, chunkPos);
            }
        }
    }

    private void generateTunnelSection(WorldGenLevel world, RandomSource random, BlockPos pos, Direction direction, BlockPos chunkPos) {
        boolean isNorthSouth = direction == Direction.NORTH || direction == Direction.SOUTH;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos blockPos = pos.offset(dx, dy, dz);

                    if (!isInWritableArea(blockPos, chunkPos)) {
                        continue;
                    }

                    boolean isCeiling = dy == 2;
                    boolean isFloor = dy == 0;
                    boolean isWall = (isNorthSouth && dx != 0) || (!isNorthSouth && dz != 0);

                    if (isCeiling || isWall) {
                        if (random.nextInt(10) == 0) {
                            world.setBlock(blockPos, Blocks.COBWEB.defaultBlockState(), 2);
                        } else {
                            world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    } else if (isFloor) {
                        BlockState groundState = world.getBlockState(blockPos);
                        if (!groundState.isSolid()) {
                            world.setBlock(blockPos, Blocks.OAK_PLANKS.defaultBlockState(), 2);
                        }

                        if (dx == 0 && dz == 0 && random.nextInt(3) == 0) {
                            BlockPos railPos = blockPos.above();
                            if (isInWritableArea(railPos, chunkPos) && world.getBlockState(railPos).isAir()) {
                                RailShape shape = isNorthSouth ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
                                world.setBlock(railPos, Blocks.RAIL.defaultBlockState()
                                        .setValue(RailBlock.SHAPE, shape), 2);
                            }
                        }
                    } else {
                        world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        if (random.nextInt(5) == 0) {
            generateSupport(world, pos, chunkPos);
        }
    }

    private void generateSupport(WorldGenLevel world, BlockPos pos, BlockPos chunkPos) {
        BlockPos floorPos = pos;
        BlockPos ceilingPos = pos.above(2);

        safeSetBlock(world, floorPos.north().above(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);
        safeSetBlock(world, floorPos.south().above(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);
        safeSetBlock(world, floorPos.east().above(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);
        safeSetBlock(world, floorPos.west().above(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);

        safeSetBlock(world, ceilingPos.north(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);
        safeSetBlock(world, ceilingPos.south(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);
        safeSetBlock(world, ceilingPos.east(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);
        safeSetBlock(world, ceilingPos.west(), Blocks.OAK_FENCE.defaultBlockState(), chunkPos);

        safeSetBlock(world, floorPos.north(), Blocks.OAK_PLANKS.defaultBlockState(), chunkPos);
        safeSetBlock(world, floorPos.south(), Blocks.OAK_PLANKS.defaultBlockState(), chunkPos);
        safeSetBlock(world, floorPos.east(), Blocks.OAK_PLANKS.defaultBlockState(), chunkPos);
        safeSetBlock(world, floorPos.west(), Blocks.OAK_PLANKS.defaultBlockState(), chunkPos);
    }

    private void generateSpawnerRoom(WorldGenLevel world, RandomSource random, BlockPos pos, BlockPos chunkPos) {
        int radius = 2;

        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos roomPos = pos.offset(x, y, z);

                    if (!isInWritableArea(roomPos, chunkPos)) {
                        continue;
                    }

                    boolean isEdge = Math.abs(x) == radius || Math.abs(z) == radius;
                    boolean isCeiling = y == 3;
                    boolean isFloor = y == 0;

                    if (isCeiling || isEdge) {
                        if (random.nextInt(3) == 0) {
                            world.setBlock(roomPos, Blocks.COBWEB.defaultBlockState(), 2);
                        }
                    } else if (!isFloor) {
                        world.setBlock(roomPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        BlockPos spawnerPos = pos.above();
        if (isInWritableArea(spawnerPos, chunkPos)) {
            world.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
            BlockEntity be = world.getBlockEntity(spawnerPos);
            if (be instanceof SpawnerBlockEntity spawner) {
                spawner.setEntityId(EntityType.CAVE_SPIDER, random);
            }
        }

        if (random.nextBoolean()) {
            BlockPos chestPos = pos.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
            if (isInWritableArea(chestPos, chunkPos) && world.getBlockState(chestPos).isAir()) {
                world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            }
        }
    }

    private boolean isUnderground(WorldGenLevel world, BlockPos pos) {
        int solidBlocksAbove = 0;
        for (int y = pos.getY() + 1; y < pos.getY() + 20; y++) {
            if (world.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).isSolid()) {
                solidBlocksAbove++;
            }
        }
        return solidBlocksAbove > 15;
    }

    private boolean isValidMineshaftPosition(WorldGenLevel world, BlockPos pos) {
        if (pos.getY() < world.getMinBuildHeight() + 5 || pos.getY() > world.getMaxBuildHeight() - 5) {
            return false;
        }

        return isUnderground(world, pos);
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:mineshafts";
    }
}
