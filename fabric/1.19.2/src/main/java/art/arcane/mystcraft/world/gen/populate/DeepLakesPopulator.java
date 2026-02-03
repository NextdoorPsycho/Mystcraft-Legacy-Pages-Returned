package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deep lakes populator that generates underground water and lava pools.
 * These pools appear in caves and underground spaces, similar to vanilla underground lakes.
 * Water pools generate more frequently at higher Y levels, while lava pools are more common deep underground.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 * <p>
 * Ported to 1.19.2 - uses getMaterial().isSolid() for solidity checks.
 */
public class DeepLakesPopulator implements IPopulate {

  // Number of lake attempts per chunk
  private static final int DEFAULT_WATER_ATTEMPTS_PER_CHUNK = 4;
  private static final int DEFAULT_LAVA_ATTEMPTS_PER_CHUNK = 1;
  // Y level thresholds for lake generation
  private static final int DEFAULT_MAX_WATER_Y = 40;
  private static final int DEFAULT_MIN_Y = -60;
  private static final int DEFAULT_LAVA_PREFERRED_Y = -20;
  private final long seed;
  private final int waterAttemptsPerChunk;
  private final int lavaAttemptsPerChunk;
  private final int maxWaterY;
  private final int minY;
  private final int lavaPreferredY;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public DeepLakesPopulator(long seed) {
    this(seed, null);
  }

  public DeepLakesPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.waterAttemptsPerChunk = PopulatorConfig.getInt(params, "water_attempts", DEFAULT_WATER_ATTEMPTS_PER_CHUNK);
    this.lavaAttemptsPerChunk = PopulatorConfig.getInt(params, "lava_attempts", DEFAULT_LAVA_ATTEMPTS_PER_CHUNK);
    this.maxWaterY = PopulatorConfig.getInt(params, "max_water_y", DEFAULT_MAX_WATER_Y);
    this.minY = PopulatorConfig.getInt(params, "min_y", DEFAULT_MIN_Y);
    this.lavaPreferredY = PopulatorConfig.getInt(params, "lava_preferred_y", DEFAULT_LAVA_PREFERRED_Y);
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

    // Generate water pools
    for (int i = 0; i < waterAttemptsPerChunk; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int y = minY + random.nextInt(Math.max(1, maxWaterY - minY + 1));
      int z = chunkPos.getZ() + random.nextInt(16);

      generateLake(world, random, new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), true);
    }

    // Generate lava pools (more common at deeper levels)
    for (int i = 0; i < lavaAttemptsPerChunk; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int y = minY + random.nextInt(Math.max(1, lavaPreferredY - minY + 1));
      int z = chunkPos.getZ() + random.nextInt(16);

      generateLake(world, random, new BlockPos(x, y, z), Blocks.LAVA.defaultBlockState(), false);
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

  private boolean generateLake(WorldGenLevel world, RandomSource random, BlockPos center, BlockState liquidState, boolean isWater) {
    // Adjust center position down slightly
    center = center.below(4);

    // Create a spherical lake shape using noise
    boolean[] sphereShape = new boolean[2048];
    int sphereRadius = random.nextInt(4) + 4;

    // Generate sphere points
    for (int i = 0; i < random.nextInt(4) + 4; i++) {
      double sizeX = random.nextDouble() * 6.0 + 3.0;
      double sizeY = random.nextDouble() * 4.0 + 2.0;
      double sizeZ = random.nextDouble() * 6.0 + 3.0;
      double centerX = random.nextDouble() * (16.0 - sizeX - 2.0) + 1.0 + sizeX / 2.0;
      double centerY = random.nextDouble() * (8.0 - sizeY - 4.0) + 2.0 + sizeY / 2.0;
      double centerZ = random.nextDouble() * (16.0 - sizeZ - 2.0) + 1.0 + sizeZ / 2.0;

      for (int x = 1; x < 15; x++) {
        for (int z = 1; z < 15; z++) {
          for (int y = 1; y < 7; y++) {
            double xDist = (x - centerX) / (sizeX / 2.0);
            double yDist = (y - centerY) / (sizeY / 2.0);
            double zDist = (z - centerZ) / (sizeZ / 2.0);
            double distSq = xDist * xDist + yDist * yDist + zDist * zDist;

            if (distSq < 1.0) {
              sphereShape[(x * 16 + z) * 8 + y] = true;
            }
          }
        }
      }
    }

    // Check if location is valid (must be underground)
    boolean isUnderground = true;
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = 0; y < 8; y++) {
          boolean insideSphere = sphereShape[(x * 16 + z) * 8 + y];
          if (insideSphere) {
            BlockPos checkPos = center.offset(x, y, z);
            BlockState state = safeGetBlockState(world, checkPos);

            // Lake must be surrounded by solid blocks
            if (y >= 4 && state.isAir()) {
              return false;
            }

            // Check for invalid materials
            // 1.19.2: Use getMaterial().isSolid() instead of isSolid()
            if (y < 4 && !state.getMaterial().isSolid() && !state.is(liquidState.getBlock())) {
              return false;
            }
          }
        }
      }
    }

    // Generate the lake
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = 0; y < 8; y++) {
          boolean insideSphere = sphereShape[(x * 16 + z) * 8 + y];
          if (insideSphere) {
            BlockPos lakePos = center.offset(x, y, z);
            BlockState existing = safeGetBlockState(world, lakePos);

            // Only replace solid blocks with liquid
            // 1.19.2: Use getMaterial().isSolid() instead of isSolid()
            if (existing.getMaterial().isSolid()) {
              if (y >= 4) {
                // Place air above liquid
                safeSetBlock(world, lakePos, Blocks.AIR.defaultBlockState());
              } else {
                // Place liquid
                safeSetBlock(world, lakePos, liquidState);

                // For lava lakes, occasionally place magma blocks at the bottom
                if (!isWater && y < 2 && random.nextInt(3) == 0) {
                  safeSetBlock(world, lakePos, Blocks.MAGMA_BLOCK.defaultBlockState());
                }
              }
            }
          }
        }
      }
    }

    // Add surrounding blocks (gravel for water, stone for lava)
    BlockState surroundingBlock = isWater ? Blocks.GRAVEL.defaultBlockState() : Blocks.STONE.defaultBlockState();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = 4; y < 8; y++) {
          boolean insideSphere = sphereShape[(x * 16 + z) * 8 + y];
          if (insideSphere) {
            BlockPos surroundPos = center.offset(x, y, z);
            BlockState existing = safeGetBlockState(world, surroundPos);

            // Check if this block is adjacent to liquid
            boolean adjacentToLiquid = false;
            for (int dx = -1; dx <= 1; dx++) {
              for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = surroundPos.offset(dx, 0, dz);
                if (safeGetBlockState(world, checkPos).is(liquidState.getBlock())) {
                  adjacentToLiquid = true;
                  break;
                }
              }
              if (adjacentToLiquid) break;
            }

            // Place surrounding blocks on the floor adjacent to liquid
            // 1.19.2: Use getMaterial().isSolid() instead of isSolid()
            if (adjacentToLiquid && existing.getMaterial().isSolid() && safeGetBlockState(world, surroundPos.below()).is(liquidState.getBlock())) {
              safeSetBlock(world, surroundPos, surroundingBlock);
            }
          }
        }
      }
    }

    return true;
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:deep_lakes";
  }
}
