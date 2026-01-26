package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
 * Dungeon populator that generates mob spawner dungeons.
 * Dungeons consist of a cobblestone/mossy cobblestone room with a spawner and chests.
 */
public class DungeonPopulator implements IPopulate {

    private final long seed;

    // Number of dungeon attempts per chunk
    private static final int ATTEMPTS_PER_CHUNK = 8;

    // Mob types that can spawn in dungeons
    private static final EntityType<?>[] DUNGEON_MOBS = {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER
    };

    public DungeonPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        for (int i = 0; i < ATTEMPTS_PER_CHUNK; i++) {
            int x = chunkPos.getX() + random.nextInt(16) + 8;
            int y = random.nextInt(world.getHeight() - 16) + 8;
            int z = chunkPos.getZ() + random.nextInt(16) + 8;

            // Keep y in valid range
            y = Math.max(-60, Math.min(y, 48));

            generateDungeon(world, random, new BlockPos(x, y, z));
        }
    }

    private boolean generateDungeon(ServerLevel world, RandomSource random, BlockPos pos) {
        // Random dungeon dimensions
        int sizeX = random.nextInt(2) + 2;
        int sizeY = 3;
        int sizeZ = random.nextInt(2) + 2;

        // Check if location is valid (has enough air space and is underground)
        int airBlocks = 0;
        int solidBlocks = 0;

        for (int x = pos.getX() - sizeX - 1; x <= pos.getX() + sizeX + 1; x++) {
            for (int y = pos.getY() - 1; y <= pos.getY() + sizeY + 1; y++) {
                for (int z = pos.getZ() - sizeZ - 1; z <= pos.getZ() + sizeZ + 1; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(checkPos);

                    if (y == pos.getY() - 1) {
                        // Floor must be solid
                        if (!state.isSolid()) {
                            return false;
                        }
                    }

                    // Count walls
                    if ((x == pos.getX() - sizeX - 1 || x == pos.getX() + sizeX + 1 ||
                            z == pos.getZ() - sizeZ - 1 || z == pos.getZ() + sizeZ + 1) &&
                            y == pos.getY() && state.isAir()) {
                        airBlocks++;
                    }

                    // Count interior solid blocks (should be mostly solid = underground)
                    if (x > pos.getX() - sizeX && x < pos.getX() + sizeX &&
                            z > pos.getZ() - sizeZ && z < pos.getZ() + sizeZ &&
                            y >= pos.getY() && y <= pos.getY() + sizeY - 1) {
                        if (state.isSolid()) {
                            solidBlocks++;
                        }
                    }
                }
            }
        }

        // Must have 1-5 openings to caves/air and be mostly underground
        if (airBlocks < 1 || airBlocks > 5) {
            return false;
        }

        int interior = (sizeX * 2 - 1) * sizeY * (sizeZ * 2 - 1);
        if (solidBlocks < interior * 0.5) {
            return false;
        }

        // Build the dungeon
        // Floor and ceiling
        for (int x = pos.getX() - sizeX - 1; x <= pos.getX() + sizeX + 1; x++) {
            for (int z = pos.getZ() - sizeZ - 1; z <= pos.getZ() + sizeZ + 1; z++) {
                // Floor
                BlockPos floorPos = new BlockPos(x, pos.getY() - 1, z);
                if (world.getBlockState(floorPos).isSolid()) {
                    world.setBlock(floorPos, getFloorBlock(random), 2);
                }

                // Ceiling
                BlockPos ceilingPos = new BlockPos(x, pos.getY() + sizeY, z);
                if (world.getBlockState(ceilingPos).isSolid()) {
                    world.setBlock(ceilingPos, getWallBlock(random), 2);
                }
            }
        }

        // Walls and interior
        for (int x = pos.getX() - sizeX - 1; x <= pos.getX() + sizeX + 1; x++) {
            for (int y = pos.getY(); y <= pos.getY() + sizeY - 1; y++) {
                for (int z = pos.getZ() - sizeZ - 1; z <= pos.getZ() + sizeZ + 1; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);

                    // Is this position a wall?
                    boolean isWall = x == pos.getX() - sizeX - 1 || x == pos.getX() + sizeX + 1 ||
                            z == pos.getZ() - sizeZ - 1 || z == pos.getZ() + sizeZ + 1;

                    if (isWall) {
                        // Only replace solid blocks for walls
                        if (world.getBlockState(blockPos).isSolid()) {
                            world.setBlock(blockPos, getWallBlock(random), 2);
                        }
                    } else {
                        // Clear interior
                        world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Place chests (up to 2)
        int chestsPlaced = 0;
        for (int attempts = 0; attempts < 6 && chestsPlaced < 2; attempts++) {
            int cx = pos.getX() + random.nextInt(sizeX * 2 + 1) - sizeX;
            int cy = pos.getY();
            int cz = pos.getZ() + random.nextInt(sizeZ * 2 + 1) - sizeZ;

            BlockPos chestPos = new BlockPos(cx, cy, cz);

            // Check if position is valid for chest (air with solid wall adjacent)
            if (world.getBlockState(chestPos).isAir()) {
                int adjacentWalls = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    if (world.getBlockState(chestPos.relative(dir)).isSolid()) {
                        adjacentWalls++;
                    }
                }

                if (adjacentWalls == 1) {
                    world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
                    BlockEntity be = world.getBlockEntity(chestPos);
                    if (be instanceof ChestBlockEntity chest) {
                        chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, random.nextLong());
                    }
                    chestsPlaced++;
                }
            }
        }

        // Place spawner in center
        BlockPos spawnerPos = pos;
        world.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(spawnerPos);
        if (be instanceof SpawnerBlockEntity spawner) {
            EntityType<?> mobType = DUNGEON_MOBS[random.nextInt(DUNGEON_MOBS.length)];
            spawner.setEntityId(mobType, random);
        }

        return true;
    }

    private BlockState getWallBlock(RandomSource random) {
        return random.nextInt(4) == 0 ?
                Blocks.MOSSY_COBBLESTONE.defaultBlockState() :
                Blocks.COBBLESTONE.defaultBlockState();
    }

    private BlockState getFloorBlock(RandomSource random) {
        return random.nextInt(4) == 0 ?
                Blocks.MOSSY_COBBLESTONE.defaultBlockState() :
                Blocks.COBBLESTONE.defaultBlockState();
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:dungeons";
    }
}
