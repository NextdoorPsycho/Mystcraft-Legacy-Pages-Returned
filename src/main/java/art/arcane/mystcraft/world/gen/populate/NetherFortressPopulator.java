package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Generates Nether fortresses with bridges, corridors, and blaze spawner rooms.
 * Spawns approximately 1 per 32 chunks.
 */
public class NetherFortressPopulator implements IPopulate {

    private final long seed;

    private static final int CHUNKS_BETWEEN_FORTRESSES = 32;
    private static final BlockState NETHER_BRICK = Blocks.NETHER_BRICKS.defaultBlockState();
    private static final BlockState NETHER_BRICK_FENCE = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
    private static final BlockState NETHER_BRICK_STAIRS = Blocks.NETHER_BRICK_STAIRS.defaultBlockState();
    private static final BlockState NETHER_WART = Blocks.NETHER_WART.defaultBlockState();
    private static final BlockState SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();

    public NetherFortressPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;

        // Only generate in certain chunks (spacing control)
        if (chunkX % CHUNKS_BETWEEN_FORTRESSES == 0 && chunkZ % CHUNKS_BETWEEN_FORTRESSES == 0) {
            // Random chance to generate
            if (random.nextInt(100) < 10) {
                int x = chunkPos.getX() + random.nextInt(16);
                int y = 50 + random.nextInt(30);
                int z = chunkPos.getZ() + random.nextInt(16);

                BlockPos fortressPos = new BlockPos(x, y, z);
                generateFortress(world, random, fortressPos);
            }
        }
    }

    private void generateFortress(ServerLevel world, RandomSource random, BlockPos pos) {
        // Generate a main hall
        generateMainHall(world, random, pos);

        // Generate bridges in random directions
        int bridgeCount = 2 + random.nextInt(3);
        for (int i = 0; i < bridgeCount; i++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int bridgeLength = 8 + random.nextInt(16);
            generateBridge(world, random, pos, dir, bridgeLength);
        }

        // Generate a blaze spawner room
        Direction spawnerDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos spawnerRoomPos = pos.relative(spawnerDir, 12);
        generateBlazeSpawnerRoom(world, random, spawnerRoomPos);

        // Generate a nether wart farm
        Direction farmDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos farmPos = pos.relative(farmDir, 16).below(2);
        generateNetherWartFarm(world, random, farmPos);
    }

    private void generateMainHall(ServerLevel world, RandomSource random, BlockPos pos) {
        // 9x7x9 main hall
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                // Floor
                world.setBlock(pos.offset(x, -1, z), NETHER_BRICK, 2);

                // Ceiling
                world.setBlock(pos.offset(x, 6, z), NETHER_BRICK, 2);

                // Clear interior
                for (int y = 0; y < 6; y++) {
                    world.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Walls
        for (int y = 0; y < 6; y++) {
            for (int x = -4; x <= 4; x++) {
                world.setBlock(pos.offset(x, y, -4), NETHER_BRICK, 2);
                world.setBlock(pos.offset(x, y, 4), NETHER_BRICK, 2);
            }
            for (int z = -4; z <= 4; z++) {
                world.setBlock(pos.offset(-4, y, z), NETHER_BRICK, 2);
                world.setBlock(pos.offset(4, y, z), NETHER_BRICK, 2);
            }
        }

        // Place chests
        BlockPos chestPos = pos.offset(3, 0, 3);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.NETHER_BRIDGE, random.nextLong());
        }
    }

    private void generateBridge(ServerLevel world, RandomSource random, BlockPos start, Direction dir, int length) {
        BlockPos current = start;

        for (int i = 0; i < length; i++) {
            current = current.relative(dir);

            // Floor (3 blocks wide)
            for (int side = -1; side <= 1; side++) {
                BlockPos floorPos = getOffsetPos(current, dir, side);
                world.setBlock(floorPos, NETHER_BRICK, 2);

                // Clear above floor
                for (int y = 1; y <= 4; y++) {
                    world.setBlock(floorPos.above(y), Blocks.AIR.defaultBlockState(), 2);
                }
            }

            // Fences on sides (railings)
            if (i % 2 == 0) {
                BlockPos leftFence = getOffsetPos(current, dir, -1).above();
                BlockPos rightFence = getOffsetPos(current, dir, 1).above();
                world.setBlock(leftFence, NETHER_BRICK_FENCE, 2);
                world.setBlock(rightFence, NETHER_BRICK_FENCE, 2);
            }

            // Occasional support pillars
            if (i % 6 == 0) {
                for (int y = -1; y >= -8; y--) {
                    world.setBlock(current.offset(0, y, 0), NETHER_BRICK, 2);
                }
            }
        }
    }

    private void generateBlazeSpawnerRoom(ServerLevel world, RandomSource random, BlockPos pos) {
        // 7x6x7 room
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                // Floor
                world.setBlock(pos.offset(x, -1, z), NETHER_BRICK, 2);

                // Ceiling
                world.setBlock(pos.offset(x, 5, z), NETHER_BRICK, 2);

                // Clear interior
                for (int y = 0; y < 5; y++) {
                    world.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Walls
        for (int y = 0; y < 5; y++) {
            for (int x = -3; x <= 3; x++) {
                world.setBlock(pos.offset(x, y, -3), NETHER_BRICK, 2);
                world.setBlock(pos.offset(x, y, 3), NETHER_BRICK, 2);
            }
            for (int z = -3; z <= 3; z++) {
                world.setBlock(pos.offset(-3, y, z), NETHER_BRICK, 2);
                world.setBlock(pos.offset(3, y, z), NETHER_BRICK, 2);
            }
        }

        // Place blaze spawner in center
        BlockPos spawnerPos = pos.above();
        world.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(spawnerPos);
        if (be instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(EntityType.BLAZE, random);
        }

        // Add stairs leading up to spawner
        world.setBlock(pos.offset(-1, 0, 0), NETHER_BRICK_STAIRS, 2);
    }

    private void generateNetherWartFarm(ServerLevel world, RandomSource random, BlockPos pos) {
        // 5x1x5 farm
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos farmPos = pos.offset(x, 0, z);

                // Soul sand base
                world.setBlock(farmPos, SOUL_SAND, 2);

                // Nether wart on top
                world.setBlock(farmPos.above(), NETHER_WART, 2);
            }
        }

        // Add fences around the farm
        for (int x = -3; x <= 3; x++) {
            world.setBlock(pos.offset(x, 1, -3), NETHER_BRICK_FENCE, 2);
            world.setBlock(pos.offset(x, 1, 3), NETHER_BRICK_FENCE, 2);
        }
        for (int z = -3; z <= 3; z++) {
            world.setBlock(pos.offset(-3, 1, z), NETHER_BRICK_FENCE, 2);
            world.setBlock(pos.offset(3, 1, z), NETHER_BRICK_FENCE, 2);
        }
    }

    private BlockPos getOffsetPos(BlockPos pos, Direction dir, int offset) {
        Direction perpendicular = dir.getClockWise();
        return pos.relative(perpendicular, offset);
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:nether_fortress";
    }
}
