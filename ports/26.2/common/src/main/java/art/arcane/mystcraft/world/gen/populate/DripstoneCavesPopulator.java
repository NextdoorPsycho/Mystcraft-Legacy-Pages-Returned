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
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;

/**
 * Populates caves with dripstone features (stalactites and stalagmites).
 * Generates pointed dripstone hanging from ceilings and rising from floors, as
 * well as clusters of dripstone blocks.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk
 * loads.
 */
public class DripstoneCavesPopulator implements IPopulate {

  private static final int DEFAULT_ATTEMPTS_PER_CHUNK = 15;
  private static final int DEFAULT_MAX_Y = 60;
  private final long seed;
  private final int attemptsPerChunk;
  private final int maxY;
  private final float spawnChance;

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
      int y = world.getMinY() + random.nextInt(Math.max(1, maxY - world.getMinY() + 1));
      int z = baseZ + random.nextInt(16);

      BlockPos pos = new BlockPos(x, y, z);

      if (random.nextBoolean()) {
        tryPlaceDripstoneCluster(world, random, pos);
      } else {
        tryPlacePointedDripstone(world, random, pos);
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

  private BlockState safeGetBlockState(WorldGenLevel world, BlockPos pos) {
    if (isInChunk(pos)) {
      return world.getBlockState(pos);
    }
    return Blocks.STONE.defaultBlockState();
  }

  private void tryPlaceDripstoneCluster(WorldGenLevel world, RandomSource random, BlockPos center) {
    if (!isInChunk(center)) {
      return;
    }

    if (!world.getBlockState(center).isAir()) {
      return;
    }

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

  private void tryPlacePointedDripstone(WorldGenLevel world, RandomSource random, BlockPos pos) {
    if (!isInChunk(pos)) {
      return;
    }

    if (!world.getBlockState(pos).isAir()) {
      return;
    }

    BlockPos above = pos.above();
    BlockState aboveState = safeGetBlockState(world, above);
    if (canSupportDripstone(aboveState)) {
      placeStalactite(world, random, pos);
      return;
    }

    BlockPos below = pos.below();
    BlockState belowState = safeGetBlockState(world, below);
    if (canSupportDripstone(belowState)) {
      placeStalagmite(world, random, pos);
    }
  }

  private void placeStalactite(WorldGenLevel world, RandomSource random, BlockPos startPos) {
    int length = 1 + random.nextInt(4);

    for (int i = 0; i < length; i++) {
      BlockPos pos = startPos.below(i);

      if (!safeGetBlockState(world, pos).isAir()) {
        break;
      }

      SpeleothemThickness thickness = getSpeleothemThickness(i, length);
      BlockState state = Blocks.POINTED_DRIPSTONE.defaultBlockState()
          .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
          .setValue(PointedDripstoneBlock.THICKNESS, thickness);

      safeSetBlock(world, pos, state);
    }
  }

  private void placeStalagmite(WorldGenLevel world, RandomSource random, BlockPos startPos) {
    int length = 1 + random.nextInt(4);

    for (int i = 0; i < length; i++) {
      BlockPos pos = startPos.above(i);

      if (!safeGetBlockState(world, pos).isAir()) {
        break;
      }

      SpeleothemThickness thickness = getSpeleothemThickness(i, length);
      BlockState state = Blocks.POINTED_DRIPSTONE.defaultBlockState()
          .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
          .setValue(PointedDripstoneBlock.THICKNESS, thickness);

      safeSetBlock(world, pos, state);
    }
  }

  private SpeleothemThickness getSpeleothemThickness(int index, int totalLength) {
    if (totalLength == 1) {
      return SpeleothemThickness.TIP;
    }

    if (index == 0) {
      return SpeleothemThickness.BASE;
    } else if (index == totalLength - 1) {
      return SpeleothemThickness.TIP;
    } else if (index == 1 || index == totalLength - 2) {
      return SpeleothemThickness.FRUSTUM;
    } else {
      return SpeleothemThickness.MIDDLE;
    }
  }

  private boolean canSupportDripstone(BlockState state) {
    return state.is(Blocks.STONE) ||
        state.is(Blocks.DEEPSLATE) ||
        state.is(Blocks.DRIPSTONE_BLOCK) ||
        state.is(Blocks.CALCITE);
  }

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
