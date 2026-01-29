package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/**
 * Populates caves with dripstone features (stalactites and stalagmites).
 * Generates pointed dripstone hanging from ceilings and rising from floors,
 * as well as clusters of dripstone blocks.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class DripstoneCavesPopulator implements IPopulate {

  private static final int DEFAULT_ATTEMPTS_PER_CHUNK = 15;
  private static final int DEFAULT_MAX_Y = 60;
  private final long seed;
  private final int attemptsPerChunk;
  private final int maxY;
  private final float spawnChance;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public DripstoneCavesPopulator(long seed) {
    this(seed, null);
  }

  public DripstoneCavesPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.attemptsPerChunk = PopulatorConfig.getInt(params, "attempts", DEFAULT_ATTEMPTS_PER_CHUNK);
    this.maxY = PopulatorConfig.getInt(params, "max_y", DEFAULT_MAX_Y);
    this.spawnChance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
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

    int baseX = chunkPos.getX();
    int baseZ = chunkPos.getZ();

    if (random.nextFloat() > spawnChance) {
      return;
    }
    for (int attempt = 0; attempt < attemptsPerChunk; attempt++) {
      int x = baseX + random.nextInt(16);
      int y = world.getMinBuildHeight() + random.nextInt(Math.max(1, maxY - world.getMinBuildHeight() + 1));
      int z = baseZ + random.nextInt(16);

      BlockPos pos = new BlockPos(x, y, z);

      if (random.nextBoolean()) {
        tryPlaceDripstoneCluster(world, random, pos);
      } else {
        tryPlacePointedDripstone(world, random, pos);
      }
    }
  }

  /**
   * Checks if a position is within the current chunk boundaries.
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
   */
  private BlockState safeGetBlockState(WorldGenLevel world, BlockPos pos) {
    if (isInChunk(pos)) {
      return world.getBlockState(pos);
    }
    return Blocks.STONE.defaultBlockState();
  }

  /**
   * Attempts to place a cluster of dripstone blocks.
   */
  private void tryPlaceDripstoneCluster(WorldGenLevel world, RandomSource random, BlockPos center) {
    if (!isInChunk(center)) {
      return;
    }

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

      if (safeGetBlockState(world, pos).isAir() && hasStoneAdjacent(world, pos)) {
        safeSetBlock(world, pos, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
      }
    }
  }

  /**
   * Attempts to place pointed dripstone (stalactite or stalagmite).
   */
  private void tryPlacePointedDripstone(WorldGenLevel world, RandomSource random, BlockPos pos) {
    if (!isInChunk(pos)) {
      return;
    }

    if (!world.getBlockState(pos).isAir()) {
      return;
    }

    // Try to place stalactite (hanging from ceiling)
    BlockPos above = pos.above();
    BlockState aboveState = safeGetBlockState(world, above);
    if (canSupportDripstone(aboveState)) {
      placeStalactite(world, random, pos);
      return;
    }

    // Try to place stalagmite (rising from floor)
    BlockPos below = pos.below();
    BlockState belowState = safeGetBlockState(world, below);
    if (canSupportDripstone(belowState)) {
      placeStalagmite(world, random, pos);
    }
  }

  /**
   * Places a stalactite hanging from the ceiling.
   */
  private void placeStalactite(WorldGenLevel world, RandomSource random, BlockPos startPos) {
    int length = 1 + random.nextInt(4);

    for (int i = 0; i < length; i++) {
      BlockPos pos = startPos.below(i);

      if (!safeGetBlockState(world, pos).isAir()) {
        break;
      }

      DripstoneThickness thickness = getDripstoneThickness(i, length);
      BlockState state = Blocks.POINTED_DRIPSTONE.defaultBlockState()
          .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
          .setValue(PointedDripstoneBlock.THICKNESS, thickness);

      safeSetBlock(world, pos, state);
    }
  }

  /**
   * Places a stalagmite rising from the floor.
   */
  private void placeStalagmite(WorldGenLevel world, RandomSource random, BlockPos startPos) {
    int length = 1 + random.nextInt(4);

    for (int i = 0; i < length; i++) {
      BlockPos pos = startPos.above(i);

      if (!safeGetBlockState(world, pos).isAir()) {
        break;
      }

      DripstoneThickness thickness = getDripstoneThickness(i, length);
      BlockState state = Blocks.POINTED_DRIPSTONE.defaultBlockState()
          .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
          .setValue(PointedDripstoneBlock.THICKNESS, thickness);

      safeSetBlock(world, pos, state);
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
   * Uses safe block state access to avoid triggering chunk loads.
   */
  private boolean isInCave(WorldGenLevel world, BlockPos pos) {
    int stoneCount = 0;
    int airCount = 0;

    for (Direction direction : Direction.values()) {
      BlockState state = safeGetBlockState(world, pos.relative(direction));
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
   * Uses safe block state access to avoid triggering chunk loads.
   */
  private boolean hasStoneAdjacent(WorldGenLevel world, BlockPos pos) {
    for (Direction direction : Direction.values()) {
      BlockState state = safeGetBlockState(world, pos.relative(direction));
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
