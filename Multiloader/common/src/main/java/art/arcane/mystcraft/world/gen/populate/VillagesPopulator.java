package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Village populator that generates simplified villages.
 * Villages consist of small clusters (3-6 houses) with simple wood plank and cobblestone construction,
 * wells, and dirt paths connecting structures.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 */
public class VillagesPopulator implements IPopulate {

  private static final int DEFAULT_VILLAGE_RARITY = 32;
  private static final int DEFAULT_MIN_HOUSES = 3;
  private static final int DEFAULT_MAX_HOUSES = 6;
  private static final int DEFAULT_FLATNESS_CHECK_RADIUS = 8;
  private final long seed;
  private final int villageRarity;
  private final int minHouses;
  private final int maxHouses;
  private final int flatnessCheckRadius;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public VillagesPopulator(long seed) {
    this(seed, null);
  }

  public VillagesPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.villageRarity = PopulatorConfig.rarityFrom(params, DEFAULT_VILLAGE_RARITY);
    this.minHouses = Math.max(1, PopulatorConfig.getInt(params, "min_houses", DEFAULT_MIN_HOUSES));
    this.maxHouses = Math.max(this.minHouses, PopulatorConfig.getInt(params, "max_houses", DEFAULT_MAX_HOUSES));
    this.flatnessCheckRadius = Math.max(1, PopulatorConfig.getInt(params, "flatness_radius", DEFAULT_FLATNESS_CHECK_RADIUS));
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

    if (random.nextInt(villageRarity) != 0) {
      return;
    }

    // Keep village center within current chunk to avoid cascading
    int x = chunkPos.getX() + 4 + random.nextInt(8);
    int z = chunkPos.getZ() + 4 + random.nextInt(8);

    int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    if (surfaceY < world.getMinBuildHeight() || surfaceY > world.getMaxBuildHeight() - 10) {
      return;
    }

    BlockPos centerPos = new BlockPos(x, surfaceY, z);

    if (!isAreaSuitableForVillage(world, centerPos)) {
      return;
    }

    generateVillage(world, random, centerPos);
  }

  private boolean isAreaSuitableForVillage(WorldGenLevel world, BlockPos center) {
    int totalHeightDiff = 0;
    int samples = 0;
    int baseHeight = center.getY();

    // Only check within writable area to avoid cascading chunk loads
    for (int x = -flatnessCheckRadius; x <= flatnessCheckRadius; x += 2) {
      for (int z = -flatnessCheckRadius; z <= flatnessCheckRadius; z += 2) {
        int checkX = center.getX() + x;
        int checkZ = center.getZ() + z;
        BlockPos checkPos = new BlockPos(checkX, 0, checkZ);
        if (!isInWritableArea(checkPos, new BlockPos(chunkMinX, 0, chunkMinZ))) continue;

        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, checkX, checkZ);
        totalHeightDiff += Math.abs(y - baseHeight);
        samples++;
      }
    }

    if (samples == 0) return false;
    double avgHeightDiff = (double) totalHeightDiff / samples;
    return avgHeightDiff < 2.0;
  }

  /**
   * Checks if a position is within the writable area for this chunk.
   * Uses the IPopulate writable area (±16 blocks) rather than strict chunk boundaries.
   */
  private boolean isInBounds(BlockPos pos) {
    return isInWritableArea(pos, new BlockPos(chunkMinX, 0, chunkMinZ));
  }

  /**
   * Safe setBlock that only places blocks within writable area.
   */
  private void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state) {
    if (isInBounds(pos)) {
      world.setBlock(pos, state, 2);
    }
  }

  private void generateVillage(WorldGenLevel world, RandomSource random, BlockPos center) {
    int houseCount = minHouses + random.nextInt(maxHouses - minHouses + 1);

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

  private void generateHouse(WorldGenLevel world, RandomSource random, BlockPos pos) {
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
            safeSetBlock(world, blockPos, Blocks.COBBLESTONE.defaultBlockState());
          } else if (y < height - 1) {
            if (isWall) {
              if (!isCorner && y == 1 && random.nextInt(3) == 0) {
                safeSetBlock(world, blockPos, Blocks.GLASS_PANE.defaultBlockState());
              } else {
                safeSetBlock(world, blockPos, wallBlock);
              }
            } else {
              safeSetBlock(world, blockPos, Blocks.AIR.defaultBlockState());
            }
          }
        }
      }
    }

    for (int x = -1; x <= width; x++) {
      for (int z = -1; z <= depth; z++) {
        if (x >= 0 && x < width && z >= 0 && z < depth) continue;

        BlockPos roofPos = pos.offset(x, height - 1, z);
        safeSetBlock(world, roofPos, roofBlock);
      }
    }

    BlockPos doorPos = pos.offset(width / 2, 1, 0);
    safeSetBlock(world, doorPos, Blocks.AIR.defaultBlockState());
    safeSetBlock(world, doorPos.above(), Blocks.AIR.defaultBlockState());

    if (random.nextInt(3) == 0) {
      BlockPos torchPos = pos.offset(1, 2, 1);
      safeSetBlock(world, torchPos, Blocks.TORCH.defaultBlockState());
    }
  }

  private void generateWell(WorldGenLevel world, BlockPos pos) {
    flattenGround(world, pos.offset(-1, 0, -1), 3, 3);

    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        boolean isEdge = x == -1 || x == 1 || z == -1 || z == 1;
        boolean isCorner = (x == -1 || x == 1) && (z == -1 || z == 1);

        BlockPos basePos = pos.offset(x, 0, z);

        if (x == 0 && z == 0) {
          safeSetBlock(world, basePos.below(), Blocks.WATER.defaultBlockState());
          safeSetBlock(world, basePos, Blocks.WATER.defaultBlockState());
        } else if (isCorner) {
          safeSetBlock(world, basePos, Blocks.COBBLESTONE.defaultBlockState());
          safeSetBlock(world, basePos.above(), Blocks.OAK_FENCE.defaultBlockState());
          safeSetBlock(world, basePos.above(2), Blocks.OAK_FENCE.defaultBlockState());
        } else if (isEdge) {
          safeSetBlock(world, basePos, Blocks.COBBLESTONE.defaultBlockState());
        }
      }
    }

    safeSetBlock(world, pos.offset(-1, 3, 0), Blocks.OAK_PLANKS.defaultBlockState());
    safeSetBlock(world, pos.offset(1, 3, 0), Blocks.OAK_PLANKS.defaultBlockState());
    safeSetBlock(world, pos.offset(0, 3, -1), Blocks.OAK_PLANKS.defaultBlockState());
    safeSetBlock(world, pos.offset(0, 3, 1), Blocks.OAK_PLANKS.defaultBlockState());
  }

  private void generatePath(WorldGenLevel world, BlockPos from, BlockPos to) {
    int steps = (int) Math.sqrt(from.distSqr(to));
    for (int i = 0; i <= steps; i++) {
      double progress = (double) i / steps;
      int x = (int) (from.getX() + (to.getX() - from.getX()) * progress);
      int z = (int) (from.getZ() + (to.getZ() - from.getZ()) * progress);
      int y = findSurfaceY(world, x, z);

      BlockPos pathPos = new BlockPos(x, y, z);
      if (isInBounds(pathPos) && world.getBlockState(pathPos).isSolid()) {
        safeSetBlock(world, pathPos, Blocks.DIRT_PATH.defaultBlockState());
      }
    }
  }

  private void flattenGround(WorldGenLevel world, BlockPos pos, int width, int depth) {
    int baseY = pos.getY();
    for (int x = 0; x < width; x++) {
      for (int z = 0; z < depth; z++) {
        BlockPos groundPos = new BlockPos(pos.getX() + x, baseY, pos.getZ() + z);

        for (int dy = -3; dy < 5; dy++) {
          BlockPos checkPos = groundPos.offset(0, dy, 0);
          if (isInBounds(checkPos)) {
            if (dy < 0) {
              if (!world.getBlockState(checkPos).isSolid()) {
                safeSetBlock(world, checkPos, Blocks.DIRT.defaultBlockState());
              }
            } else if (dy > 0) {
              if (!world.getBlockState(checkPos).isAir()) {
                safeSetBlock(world, checkPos, Blocks.AIR.defaultBlockState());
              }
            }
          }
        }
      }
    }
  }

  private int findSurfaceY(WorldGenLevel world, int x, int z) {
    return world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:villages";
  }
}
