package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stronghold populator that generates underground stone brick structures.
 * Strongholds consist of libraries with bookshelves, prison cells with iron bars,
 * corridors, and very rarely an end portal room.
 */
public class StrongholdsPopulator implements IPopulate {

    private final long seed;

    private static final int STRONGHOLD_RARITY = 128;
    private static final int MIN_Y = -20;
    private static final int MAX_Y = 20;
    private static final int MAX_ROOMS = 12;

    private enum RoomType {
        CORRIDOR,
        LIBRARY,
        PRISON,
        END_PORTAL
    }

    public StrongholdsPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        if (random.nextInt(STRONGHOLD_RARITY) != 0) {
            return;
        }

        int x = chunkPos.getX() + random.nextInt(16);
        int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y);
        int z = chunkPos.getZ() + random.nextInt(16);

        BlockPos startPos = new BlockPos(x, y, z);

        if (!isUnderground(world, startPos)) {
            return;
        }

        generateStronghold(world, random, startPos);
    }

    private void generateStronghold(ServerLevel world, RandomSource random, BlockPos startPos) {
        int roomCount = 6 + random.nextInt(MAX_ROOMS - 6);
        boolean hasEndPortal = random.nextInt(10) == 0;

        BlockPos currentPos = startPos;
        Direction currentDir = Direction.NORTH;

        for (int i = 0; i < roomCount; i++) {
            RoomType roomType;

            if (hasEndPortal && i == roomCount - 1) {
                roomType = RoomType.END_PORTAL;
            } else {
                int roll = random.nextInt(10);
                if (roll < 5) {
                    roomType = RoomType.CORRIDOR;
                } else if (roll < 8) {
                    roomType = RoomType.LIBRARY;
                } else {
                    roomType = RoomType.PRISON;
                }
            }

            generateRoom(world, random, currentPos, roomType);

            int distance = 8 + random.nextInt(8);
            currentPos = currentPos.relative(currentDir, distance);

            if (random.nextInt(3) == 0) {
                currentDir = random.nextBoolean() ?
                        currentDir.getClockWise() : currentDir.getCounterClockWise();
            }
        }
    }

    private void generateRoom(ServerLevel world, RandomSource random, BlockPos pos, RoomType type) {
        switch (type) {
            case CORRIDOR -> generateCorridor(world, random, pos);
            case LIBRARY -> generateLibrary(world, random, pos);
            case PRISON -> generatePrison(world, random, pos);
            case END_PORTAL -> generateEndPortalRoom(world, random, pos);
        }
    }

    private void generateCorridor(ServerLevel world, RandomSource random, BlockPos pos) {
        int length = 5 + random.nextInt(5);
        int width = 3;
        int height = 4;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    boolean isWall = x == 0 || x == width - 1;
                    boolean isFloor = y == 0;
                    boolean isCeiling = y == height - 1;

                    if (isWall || isFloor || isCeiling) {
                        world.setBlock(blockPos, getStrongholdBlock(random), 2);
                    } else {
                        world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        if (random.nextInt(3) == 0) {
            BlockPos torchPos = pos.offset(1, 2, length / 2);
            world.setBlock(torchPos, Blocks.WALL_TORCH.defaultBlockState(), 2);
        }
    }

    private void generateLibrary(ServerLevel world, RandomSource random, BlockPos pos) {
        int size = 7;
        int height = 5;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    boolean isWall = x == 0 || x == size - 1 || z == 0 || z == size - 1;
                    boolean isFloor = y == 0;
                    boolean isCeiling = y == height - 1;

                    if (isWall || isFloor || isCeiling) {
                        world.setBlock(blockPos, getStrongholdBlock(random), 2);
                    } else {
                        world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        for (int x = 1; x < size - 1; x++) {
            for (int z = 1; z < size - 1; z++) {
                if ((x == 1 || x == size - 2 || z == 1 || z == size - 2) && random.nextInt(3) != 0) {
                    BlockPos shelfPos = pos.offset(x, 1, z);
                    world.setBlock(shelfPos, Blocks.BOOKSHELF.defaultBlockState(), 2);
                    if (random.nextBoolean()) {
                        world.setBlock(shelfPos.above(), Blocks.BOOKSHELF.defaultBlockState(), 2);
                    }
                }
            }
        }

        BlockPos centerPos = pos.offset(size / 2, 1, size / 2);
        world.setBlock(centerPos, Blocks.CHEST.defaultBlockState(), 2);

        for (int i = 0; i < 4; i++) {
            BlockPos torchPos = pos.offset(
                    i < 2 ? 1 : size - 2,
                    3,
                    i % 2 == 0 ? 1 : size - 2
            );
            world.setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 2);
        }
    }

    private void generatePrison(ServerLevel world, RandomSource random, BlockPos pos) {
        int width = 8;
        int depth = 6;
        int height = 4;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    boolean isWall = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                    boolean isFloor = y == 0;
                    boolean isCeiling = y == height - 1;

                    if (isWall || isFloor || isCeiling) {
                        world.setBlock(blockPos, getStrongholdBlock(random), 2);
                    } else {
                        world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        int cellCount = 2 + random.nextInt(2);
        for (int i = 0; i < cellCount; i++) {
            int cellX = 2 + (i * 3);
            if (cellX >= width - 2) break;

            for (int y = 1; y < height - 1; y++) {
                world.setBlock(pos.offset(cellX, y, 1), Blocks.IRON_BARS.defaultBlockState(), 2);
                world.setBlock(pos.offset(cellX + 1, y, 1), Blocks.IRON_BARS.defaultBlockState(), 2);
            }

            world.setBlock(pos.offset(cellX, 1, 1), Blocks.IRON_DOOR.defaultBlockState(), 2);
        }
    }

    private void generateEndPortalRoom(ServerLevel world, RandomSource random, BlockPos pos) {
        int size = 11;
        int height = 6;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    boolean isWall = x == 0 || x == size - 1 || z == 0 || z == size - 1;
                    boolean isFloor = y == 0;
                    boolean isCeiling = y == height - 1;

                    if (isWall || isFloor || isCeiling) {
                        world.setBlock(blockPos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    } else {
                        world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        BlockPos portalCenter = pos.offset(size / 2, 1, size / 2);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos framePos = portalCenter.offset(x, 0, z);
                if (x == 0 || z == 0) {
                    world.setBlock(framePos, Blocks.END_PORTAL_FRAME.defaultBlockState(), 2);
                } else {
                    world.setBlock(framePos, Blocks.LAVA.defaultBlockState(), 2);
                }
            }
        }

        BlockPos silverfish = portalCenter.offset(2, 0, 2);
        world.setBlock(silverfish, Blocks.INFESTED_STONE_BRICKS.defaultBlockState(), 2);

        for (int i = 0; i < 4; i++) {
            BlockPos torchPos = pos.offset(
                    i < 2 ? 2 : size - 3,
                    2,
                    i % 2 == 0 ? 2 : size - 3
            );
            world.setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 2);
        }
    }

    private BlockState getStrongholdBlock(RandomSource random) {
        int roll = random.nextInt(10);
        if (roll < 7) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        } else if (roll < 9) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        } else {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
    }

    private boolean isUnderground(ServerLevel world, BlockPos pos) {
        int solidBlocksAbove = 0;
        for (int y = pos.getY() + 1; y < pos.getY() + 30; y++) {
            if (world.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).isSolid()) {
                solidBlocksAbove++;
            }
        }
        return solidBlocksAbove > 20;
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:strongholds";
    }
}
