package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Populates caves with lush cave vegetation features.
 * Generates moss, glow berries, azalea bushes, spore blossoms, and dripleaf plants.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class LushCavesPopulator implements IPopulate {

  private static final int DEFAULT_ATTEMPTS_PER_CHUNK = 20;
  private static final int DEFAULT_MAX_Y = 60;
  private final long seed;
  private final int attemptsPerChunk;
  private final int maxY;
  private final float spawnChance;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public LushCavesPopulator(long seed) {
    this(seed, null);
  }

  public LushCavesPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.attemptsPerChunk = PopulatorConfig.getInt(params, "attempts", DEFAULT_ATTEMPTS_PER_CHUNK);
    this.maxY = PopulatorConfig.getInt(params, "max_y", DEFAULT_MAX_Y);
    this.spawnChance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;

    // Set chunk boundaries for this population run
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    if (random.nextFloat() > spawnChance) {
      return;
    }
    for (int attempt = 0; attempt < attemptsPerChunk; attempt++) {
      int x = chunkMinX + random.nextInt(16);
      int y = world.getMinBuildHeight() + random.nextInt(Math.max(1, maxY - world.getMinBuildHeight() + 1));
      int z = chunkMinZ + random.nextInt(16);

      BlockPos pos = new BlockPos(x, y, z);

      if (!isInChunk(pos) || !world.getBlockState(pos).isAir()) {
        continue;
      }

      // Check if we're in a cave
      if (!isInCave(world, pos)) {
        continue;
      }

      int featureType = random.nextInt(10);

      if (featureType < 3) {
        tryPlaceMoss(world, random, pos);
      } else if (featureType < 5) {
        tryPlaceGlowBerries(world, random, pos);
      } else if (featureType < 7) {
        tryPlaceAzalea(world, random, pos);
      } else if (featureType < 8) {
        tryPlaceSporeBlossoms(world, random, pos);
      } else {
        tryPlaceDripleaf(world, random, pos);
      }
    }
  }

  /**
   * Checks if a position is within the current chunk boundaries.
   * This prevents cascade chunk loading when structures extend beyond chunk edges.
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
   * Safe getBlockState that returns air if position is outside chunk boundaries.
   * This prevents cascade chunk loading when checking blocks near chunk edges.
   */
  private BlockState safeGetBlockState(WorldGenLevel world, BlockPos pos) {
    if (!isInChunk(pos)) {
      return Blocks.AIR.defaultBlockState();
    }
    return world.getBlockState(pos);
  }

  /**
   * Attempts to place moss blocks on surfaces.
   */
  private void tryPlaceMoss(WorldGenLevel world, RandomSource random, BlockPos center) {
    // Try placing moss on floor
    BlockPos below = center.below();
    if (isInChunk(below) && canReplaceMoss(world.getBlockState(below))) {
      safeSetBlock(world, below, Blocks.MOSS_BLOCK.defaultBlockState());

      // Spread moss to nearby blocks
      int spread = 2 + random.nextInt(3);
      for (int i = 0; i < spread; i++) {
        int offsetX = random.nextInt(3) - 1;
        int offsetZ = random.nextInt(3) - 1;
        BlockPos mossPos = below.offset(offsetX, 0, offsetZ);

        if (isInChunk(mossPos) && isInChunk(mossPos.above()) &&
            safeGetBlockState(world, mossPos.above()).isAir() &&
            canReplaceMoss(safeGetBlockState(world, mossPos))) {
          safeSetBlock(world, mossPos, Blocks.MOSS_BLOCK.defaultBlockState());

          // Occasionally add moss carpet on top
          if (random.nextFloat() < 0.4f) {
            safeSetBlock(world, mossPos.above(), Blocks.MOSS_CARPET.defaultBlockState());
          }
        }
      }
    }

    // Try placing moss on ceiling
    BlockPos above = center.above();
    if (isInChunk(above) && canReplaceMoss(world.getBlockState(above))) {
      safeSetBlock(world, above, Blocks.MOSS_BLOCK.defaultBlockState());
    }
  }

  /**
   * Attempts to place glow berry vines hanging from the ceiling.
   */
  private void tryPlaceGlowBerries(WorldGenLevel world, RandomSource random, BlockPos pos) {
    BlockPos above = pos.above();
    if (!isInChunk(above)) {
      return;
    }
    BlockState aboveState = world.getBlockState(above);

    if (!aboveState.isSolid()) {
      return;
    }

    int length = 1 + random.nextInt(4);

    for (int i = 0; i < length; i++) {
      BlockPos vinePos = pos.below(i);

      if (!isInChunk(vinePos)) {
        break;
      }

      if (!world.getBlockState(vinePos).isAir()) {
        break;
      }

      boolean hasBerries = random.nextFloat() < 0.3f;
      BlockState vineState = Blocks.CAVE_VINES.defaultBlockState()
          .setValue(CaveVinesBlock.BERRIES, hasBerries);

      safeSetBlock(world, vinePos, vineState);
    }
  }

  /**
   * Attempts to place azalea bushes.
   */
  private void tryPlaceAzalea(WorldGenLevel world, RandomSource random, BlockPos pos) {
    BlockPos below = pos.below();
    if (!isInChunk(below)) {
      return;
    }
    BlockState belowState = world.getBlockState(below);

    if (!canPlantOnMoss(belowState)) {
      return;
    }

    BlockState azaleaState = random.nextBoolean()
        ? Blocks.AZALEA.defaultBlockState()
        : Blocks.FLOWERING_AZALEA.defaultBlockState();

    safeSetBlock(world, pos, azaleaState);
  }

  /**
   * Attempts to place spore blossoms on the ceiling.
   */
  private void tryPlaceSporeBlossoms(WorldGenLevel world, RandomSource random, BlockPos pos) {
    BlockPos above = pos.above();
    if (!isInChunk(above)) {
      return;
    }
    BlockState aboveState = world.getBlockState(above);

    if (!aboveState.isSolid()) {
      return;
    }

    safeSetBlock(world, pos, Blocks.SPORE_BLOSSOM.defaultBlockState());
  }

  /**
   * Attempts to place dripleaf plants in water.
   */
  private void tryPlaceDripleaf(WorldGenLevel world, RandomSource random, BlockPos pos) {
    BlockPos below = pos.below();
    if (!isInChunk(below)) {
      return;
    }
    BlockState belowState = world.getBlockState(below);

    // Check for clay or moss in water
    if (!(belowState.is(Blocks.CLAY) || belowState.is(Blocks.MOSS_BLOCK))) {
      return;
    }

    // Check if there's water above the base block
    if (!isInChunk(pos) || !world.getBlockState(pos).is(Blocks.WATER)) {
      return;
    }

    // Place small dripleaf
    if (random.nextBoolean()) {
      safeSetBlock(world, pos, Blocks.SMALL_DRIPLEAF.defaultBlockState());
    } else {
      // Place big dripleaf (2-3 blocks tall)
      int height = 2 + random.nextInt(2);
      for (int i = 0; i < height; i++) {
        BlockPos leafPos = pos.above(i);
        if (!isInChunk(leafPos)) {
          break;
        }
        BlockState leafState = world.getBlockState(leafPos);
        if (leafState.is(Blocks.WATER) || leafState.isAir()) {
          safeSetBlock(world, leafPos, Blocks.BIG_DRIPLEAF.defaultBlockState());
        }
      }
    }
  }

  /**
   * Checks if the position is in a cave (air with stone nearby).
   * Uses chunk boundary checking to prevent cascade loading.
   */
  private boolean isInCave(WorldGenLevel world, BlockPos pos) {
    int stoneCount = 0;
    int airCount = 0;

    for (Direction direction : Direction.values()) {
      BlockPos neighborPos = pos.relative(direction);
      // Skip neighbors outside chunk to prevent cascade loading
      if (!isInChunk(neighborPos)) {
        continue;
      }
      BlockState state = world.getBlockState(neighborPos);
      if (state.isAir()) {
        airCount++;
      } else if (state.isSolid()) {
        stoneCount++;
      }
    }

    return airCount >= 2 && stoneCount >= 2;
  }

  /**
   * Checks if moss can replace this block.
   */
  private boolean canReplaceMoss(BlockState state) {
    return state.is(Blocks.STONE) ||
        state.is(Blocks.DEEPSLATE) ||
        state.is(Blocks.GRANITE) ||
        state.is(Blocks.DIORITE) ||
        state.is(Blocks.ANDESITE) ||
        state.is(Blocks.DIRT) ||
        state.is(Blocks.GRAVEL);
  }

  /**
   * Checks if plants can grow on this block.
   */
  private boolean canPlantOnMoss(BlockState state) {
    return state.is(Blocks.MOSS_BLOCK) ||
        state.is(Blocks.DIRT) ||
        state.is(Blocks.GRASS_BLOCK);
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:lush_caves";
  }
}
