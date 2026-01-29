package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Dungeon populator that generates mob spawner dungeons.
 * Dungeons consist of a cobblestone/mossy cobblestone room with a spawner and chests.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class DungeonPopulator implements IPopulate {

  // Number of dungeon attempts per chunk
  private static final int DEFAULT_ATTEMPTS_PER_CHUNK = 8;
  // Mob types that can spawn in dungeons
  private static final EntityType<?>[] DUNGEON_MOBS = {
      EntityType.ZOMBIE,
      EntityType.SKELETON,
      EntityType.SPIDER
  };
  private final long seed;
  private final int attemptsPerChunk;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public DungeonPopulator(long seed) {
    this(seed, null);
  }

  public DungeonPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.attemptsPerChunk = PopulatorConfig.getInt(params, "attempts", DEFAULT_ATTEMPTS_PER_CHUNK);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    // Set chunk boundaries for this population run
    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    for (int i = 0; i < attemptsPerChunk; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int y = random.nextInt(world.getHeight() - 16) + 8;
      int z = chunkPos.getZ() + random.nextInt(16);

      // Keep y in valid range
      y = Math.max(-60, Math.min(y, 48));

      generateDungeon(world, random, new BlockPos(x, y, z));
    }
  }

  /**
   * Checks if a position is within the current chunk boundaries.
   * This prevents cascade chunk loading when dungeons extend beyond chunk edges.
   */
  private boolean isInChunk(BlockPos pos) {
    return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
        pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
  }

  /**
   * Safe setBlock that only places blocks within current chunk boundaries.
   */
  private void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state) {
    if (isInChunk(pos)) {
      world.setBlock(pos, state, 2);
    }
  }

  /**
   * Safe getBlockState that returns stone for positions outside chunk boundaries.
   * This allows validation logic to work without triggering chunk loads.
   */
  private BlockState safeGetBlockState(WorldGenLevel world, BlockPos pos) {
    if (isInChunk(pos)) {
      return world.getBlockState(pos);
    }
    // Return stone as a "safe" default for out-of-chunk positions
    return Blocks.STONE.defaultBlockState();
  }

  private boolean generateDungeon(WorldGenLevel world, RandomSource random, BlockPos pos) {
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
          BlockState state = safeGetBlockState(world, checkPos);

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
        if (safeGetBlockState(world, floorPos).isSolid()) {
          safeSetBlock(world, floorPos, getFloorBlock(random));
        }

        // Ceiling
        BlockPos ceilingPos = new BlockPos(x, pos.getY() + sizeY, z);
        if (safeGetBlockState(world, ceilingPos).isSolid()) {
          safeSetBlock(world, ceilingPos, getWallBlock(random));
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
            if (safeGetBlockState(world, blockPos).isSolid()) {
              safeSetBlock(world, blockPos, getWallBlock(random));
            }
          } else {
            // Clear interior
            safeSetBlock(world, blockPos, Blocks.AIR.defaultBlockState());
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

      // Skip if outside chunk
      if (!isInChunk(chestPos)) {
        continue;
      }

      // Check if position is valid for chest (air with solid wall adjacent)
      if (world.getBlockState(chestPos).isAir()) {
        int adjacentWalls = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
          BlockPos adjPos = chestPos.relative(dir);
          if (isInChunk(adjPos) && world.getBlockState(adjPos).isSolid()) {
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

    // Place spawner in center (only if in chunk)
    BlockPos spawnerPos = pos;
    if (isInChunk(spawnerPos)) {
      world.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
      BlockEntity be = world.getBlockEntity(spawnerPos);
      if (be instanceof SpawnerBlockEntity spawner) {
        EntityType<?> mobType = DUNGEON_MOBS[random.nextInt(DUNGEON_MOBS.length)];
        spawner.setEntityId(mobType, random);
      }
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
