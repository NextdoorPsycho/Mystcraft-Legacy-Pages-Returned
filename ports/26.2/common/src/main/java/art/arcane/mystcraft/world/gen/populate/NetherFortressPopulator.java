package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Generates Nether fortresses with bridges, corridors, and blaze spawner rooms.
 * Spawns approximately 1 per 32 chunks.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk
 * loads.
 */
public class NetherFortressPopulator implements IPopulate {

  private static final int CHUNKS_BETWEEN_FORTRESSES = 32;
  private static final BlockState NETHER_BRICK = Blocks.NETHER_BRICKS.defaultBlockState();
  private static final BlockState NETHER_BRICK_FENCE = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
  private static final BlockState NETHER_BRICK_STAIRS = Blocks.NETHER_BRICK_STAIRS.defaultBlockState();
  private static final BlockState NETHER_WART = Blocks.NETHER_WART.defaultBlockState();
  private static final BlockState SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();
  private final long seed;
  private final float spawnChance;

  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public NetherFortressPopulator(long seed) {
    this(seed, null);
  }

  public NetherFortressPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.getFloat(params, "spawn_chance", 0.10f);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;

    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    if (chunkX % CHUNKS_BETWEEN_FORTRESSES == 0 && chunkZ % CHUNKS_BETWEEN_FORTRESSES == 0) {

      if (random.nextFloat() < spawnChance) {
        int x = chunkPos.getX() + random.nextInt(16);
        int y = 50 + random.nextInt(30);
        int z = chunkPos.getZ() + random.nextInt(16);

        BlockPos fortressPos = new BlockPos(x, y, z);
        generateFortress(world, random, fortressPos);
      }
    }
  }

  private boolean isInChunk(BlockPos pos) {
    return pos.getX() >= chunkMinX && pos.getX() <= chunkMaxX &&
        pos.getZ() >= chunkMinZ && pos.getZ() <= chunkMaxZ;
  }

  private void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state) {
    if (isInChunk(pos)) {
      world.setBlock(pos, state, 2);
    }
  }

  private void generateFortress(WorldGenLevel world, RandomSource random, BlockPos pos) {

    generateMainHall(world, random, pos);

    int bridgeCount = 2 + random.nextInt(3);
    for (int i = 0; i < bridgeCount; i++) {
      Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      int bridgeLength = 8 + random.nextInt(16);
      generateBridge(world, random, pos, dir, bridgeLength);
    }

    Direction spawnerDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
    BlockPos spawnerRoomPos = pos.relative(spawnerDir, 12);
    generateBlazeSpawnerRoom(world, random, spawnerRoomPos);

    Direction farmDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
    BlockPos farmPos = pos.relative(farmDir, 16).below(2);
    generateNetherWartFarm(world, random, farmPos);
  }

  private void generateMainHall(WorldGenLevel world, RandomSource random, BlockPos pos) {

    for (int x = -4; x <= 4; x++) {
      for (int z = -4; z <= 4; z++) {

        safeSetBlock(world, pos.offset(x, -1, z), NETHER_BRICK);

        safeSetBlock(world, pos.offset(x, 6, z), NETHER_BRICK);

        for (int y = 0; y < 6; y++) {
          safeSetBlock(world, pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }
    }

    for (int y = 0; y < 6; y++) {
      for (int x = -4; x <= 4; x++) {
        safeSetBlock(world, pos.offset(x, y, -4), NETHER_BRICK);
        safeSetBlock(world, pos.offset(x, y, 4), NETHER_BRICK);
      }
      for (int z = -4; z <= 4; z++) {
        safeSetBlock(world, pos.offset(-4, y, z), NETHER_BRICK);
        safeSetBlock(world, pos.offset(4, y, z), NETHER_BRICK);
      }
    }

    BlockPos chestPos = pos.offset(3, 0, 3);
    if (isInChunk(chestPos)) {
      world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
      BlockEntity be = world.getBlockEntity(chestPos);
      if (be instanceof ChestBlockEntity chest) {
        chest.setLootTable(BuiltInLootTables.NETHER_BRIDGE, random.nextLong());
      }
    }
  }

  private void generateBridge(WorldGenLevel world, RandomSource random, BlockPos start, Direction dir, int length) {
    BlockPos current = start;

    for (int i = 0; i < length; i++) {
      current = current.relative(dir);

      for (int side = -1; side <= 1; side++) {
        BlockPos floorPos = getOffsetPos(current, dir, side);
        safeSetBlock(world, floorPos, NETHER_BRICK);

        for (int y = 1; y <= 4; y++) {
          safeSetBlock(world, floorPos.above(y), Blocks.AIR.defaultBlockState());
        }
      }

      if (i % 2 == 0) {
        BlockPos leftFence = getOffsetPos(current, dir, -1).above();
        BlockPos rightFence = getOffsetPos(current, dir, 1).above();
        safeSetBlock(world, leftFence, NETHER_BRICK_FENCE);
        safeSetBlock(world, rightFence, NETHER_BRICK_FENCE);
      }

      if (i % 6 == 0) {
        for (int y = -1; y >= -8; y--) {
          safeSetBlock(world, current.offset(0, y, 0), NETHER_BRICK);
        }
      }
    }
  }

  private void generateBlazeSpawnerRoom(WorldGenLevel world, RandomSource random, BlockPos pos) {

    for (int x = -3; x <= 3; x++) {
      for (int z = -3; z <= 3; z++) {

        safeSetBlock(world, pos.offset(x, -1, z), NETHER_BRICK);

        safeSetBlock(world, pos.offset(x, 5, z), NETHER_BRICK);

        for (int y = 0; y < 5; y++) {
          safeSetBlock(world, pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }
    }

    for (int y = 0; y < 5; y++) {
      for (int x = -3; x <= 3; x++) {
        safeSetBlock(world, pos.offset(x, y, -3), NETHER_BRICK);
        safeSetBlock(world, pos.offset(x, y, 3), NETHER_BRICK);
      }
      for (int z = -3; z <= 3; z++) {
        safeSetBlock(world, pos.offset(-3, y, z), NETHER_BRICK);
        safeSetBlock(world, pos.offset(3, y, z), NETHER_BRICK);
      }
    }

    BlockPos spawnerPos = pos.above();
    if (isInChunk(spawnerPos)) {
      world.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
      BlockEntity be = world.getBlockEntity(spawnerPos);
      if (be instanceof SpawnerBlockEntity spawner) {
        spawner.setEntityId(EntityTypes.BLAZE, random);
      }
    }

    safeSetBlock(world, pos.offset(-1, 0, 0), NETHER_BRICK_STAIRS);
  }

  private void generateNetherWartFarm(WorldGenLevel world, RandomSource random, BlockPos pos) {

    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        BlockPos farmPos = pos.offset(x, 0, z);

        safeSetBlock(world, farmPos, SOUL_SAND);

        safeSetBlock(world, farmPos.above(), NETHER_WART);
      }
    }

    for (int x = -3; x <= 3; x++) {
      safeSetBlock(world, pos.offset(x, 1, -3), NETHER_BRICK_FENCE);
      safeSetBlock(world, pos.offset(x, 1, 3), NETHER_BRICK_FENCE);
    }
    for (int z = -3; z <= 3; z++) {
      safeSetBlock(world, pos.offset(-3, 1, z), NETHER_BRICK_FENCE);
      safeSetBlock(world, pos.offset(3, 1, z), NETHER_BRICK_FENCE);
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
