package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Village populator that generates simplified villages.
 * Villages consist of small clusters (3-6 houses) with simple wood plank and cobblestone construction,
 * wells, and dirt paths connecting structures.
 */
public class VillagesPopulator implements IPopulate {

    private final long seed;

    private static final int VILLAGE_RARITY = 32;
    private static final int MIN_HOUSES = 3;
    private static final int MAX_HOUSES = 6;
    private static final int FLATNESS_CHECK_RADIUS = 8;

    public VillagesPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        if (random.nextInt(VILLAGE_RARITY) != 0) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16) + 8;
        int z = chunkPos.getZ() + random.nextInt(16) + 8;

        int surfaceY = findSurfaceY(world, x, z);
        if (surfaceY < world.getMinBuildHeight() || surfaceY > world.getMaxBuildHeight() - 10) {
            return;
        }

        BlockPos centerPos = new BlockPos(x, surfaceY, z);

        if (!isAreaSuitableForVillage(world, centerPos)) {
            return;
        }

        generateVillage(world, random, centerPos);
    }

    private boolean isAreaSuitableForVillage(ServerLevel world, BlockPos center) {
        int totalHeightDiff = 0;
        int samples = 0;
        int baseHeight = center.getY();

        for (int x = -FLATNESS_CHECK_RADIUS; x <= FLATNESS_CHECK_RADIUS; x += 2) {
            for (int z = -FLATNESS_CHECK_RADIUS; z <= FLATNESS_CHECK_RADIUS; z += 2) {
                int y = findSurfaceY(world, center.getX() + x, center.getZ() + z);
                totalHeightDiff += Math.abs(y - baseHeight);
                samples++;
            }
        }

        double avgHeightDiff = (double) totalHeightDiff / samples;
        return avgHeightDiff < 2.0;
    }

    private void generateVillage(ServerLevel world, RandomSource random, BlockPos center) {
        int houseCount = MIN_HOUSES + random.nextInt(MAX_HOUSES - MIN_HOUSES + 1);

        generateWell(world, center);

        double angleStep = 2 * Math.PI / houseCount;
        for (int i = 0; i < houseCount; i++) {
            double angle = i * angleStep + random.nextDouble() * 0.5;
            int distance = 10 + random.nextInt(8);

            int houseX = center.getX() + (int) (Math.cos(angle) * distance);
            int houseZ = center.getZ() + (int) (Math.sin(angle) * distance);
            int houseY = findSurfaceY(world, houseX, houseZ);

            BlockPos housePos = new BlockPos(houseX, houseY, houseZ);
            generateHouse(world, random, housePos);

            generatePath(world, center, housePos);
        }
    }

    private void generateHouse(ServerLevel world, RandomSource random, BlockPos pos) {
        int width = 5 + random.nextInt(2);
        int depth = 5 + random.nextInt(2);
        int height = 4;

        BlockState wallBlock = random.nextBoolean() ?
                Blocks.OAK_PLANKS.defaultBlockState() :
                Blocks.COBBLESTONE.defaultBlockState();
        BlockState roofBlock = Blocks.OAK_PLANKS.defaultBlockState();

        flattenGround(world, pos, width, depth);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    boolean isWall = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                    boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);

                    if (y == 0) {
                        world.setBlock(blockPos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                    } else if (y < height - 1) {
                        if (isWall) {
                            if (!isCorner && y == 1 && random.nextInt(3) == 0) {
                                world.setBlock(blockPos, Blocks.GLASS_PANE.defaultBlockState(), 2);
                            } else {
                                world.setBlock(blockPos, wallBlock, 2);
                            }
                        } else {
                            world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                if (x >= 0 && x < width && z >= 0 && z < depth) continue;

                BlockPos roofPos = pos.offset(x, height - 1, z);
                world.setBlock(roofPos, roofBlock, 2);
            }
        }

        BlockPos doorPos = pos.offset(width / 2, 1, 0);
        world.setBlock(doorPos, Blocks.AIR.defaultBlockState(), 2);
        world.setBlock(doorPos.above(), Blocks.AIR.defaultBlockState(), 2);

        if (random.nextInt(3) == 0) {
            BlockPos torchPos = pos.offset(1, 2, 1);
            world.setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 2);
        }
    }

    private void generateWell(ServerLevel world, BlockPos pos) {
        flattenGround(world, pos.offset(-1, 0, -1), 3, 3);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                boolean isEdge = x == -1 || x == 1 || z == -1 || z == 1;
                boolean isCorner = (x == -1 || x == 1) && (z == -1 || z == 1);

                BlockPos basePos = pos.offset(x, 0, z);

                if (x == 0 && z == 0) {
                    world.setBlock(basePos.below(), Blocks.WATER.defaultBlockState(), 2);
                    world.setBlock(basePos, Blocks.WATER.defaultBlockState(), 2);
                } else if (isCorner) {
                    world.setBlock(basePos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                    world.setBlock(basePos.above(), Blocks.OAK_FENCE.defaultBlockState(), 2);
                    world.setBlock(basePos.above(2), Blocks.OAK_FENCE.defaultBlockState(), 2);
                } else if (isEdge) {
                    world.setBlock(basePos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                }
            }
        }

        world.setBlock(pos.offset(-1, 3, 0), Blocks.OAK_PLANKS.defaultBlockState(), 2);
        world.setBlock(pos.offset(1, 3, 0), Blocks.OAK_PLANKS.defaultBlockState(), 2);
        world.setBlock(pos.offset(0, 3, -1), Blocks.OAK_PLANKS.defaultBlockState(), 2);
        world.setBlock(pos.offset(0, 3, 1), Blocks.OAK_PLANKS.defaultBlockState(), 2);
    }

    private void generatePath(ServerLevel world, BlockPos from, BlockPos to) {
        int steps = (int) Math.sqrt(from.distSqr(to));
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            int x = (int) (from.getX() + (to.getX() - from.getX()) * progress);
            int z = (int) (from.getZ() + (to.getZ() - from.getZ()) * progress);
            int y = findSurfaceY(world, x, z);

            BlockPos pathPos = new BlockPos(x, y, z);
            if (world.getBlockState(pathPos).isSolid()) {
                world.setBlock(pathPos, Blocks.DIRT_PATH.defaultBlockState(), 2);
            }
        }
    }

    private void flattenGround(ServerLevel world, BlockPos pos, int width, int depth) {
        int baseY = pos.getY();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos groundPos = new BlockPos(pos.getX() + x, baseY, pos.getZ() + z);

                for (int dy = -3; dy < 5; dy++) {
                    BlockPos checkPos = groundPos.offset(0, dy, 0);
                    if (dy < 0) {
                        if (!world.getBlockState(checkPos).isSolid()) {
                            world.setBlock(checkPos, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    } else if (dy > 0) {
                        if (!world.getBlockState(checkPos).isAir()) {
                            world.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private int findSurfaceY(ServerLevel world, int x, int z) {
        for (int y = world.getMaxBuildHeight() - 1; y > world.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos).isSolid() &&
                world.getBlockState(pos.above()).isAir()) {
                return y + 1;
            }
        }
        return world.getSeaLevel();
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:villages";
    }
}
