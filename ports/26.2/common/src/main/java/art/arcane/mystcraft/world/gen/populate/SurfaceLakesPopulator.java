package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Surface lakes populator that generates water and lava lakes on the surface.
 * Water lakes are common and generate at the surface level, while lava lakes
 * are rare. Similar to vanilla lake generation but controlled by the
 * SurfaceLakes symbol.
 */
public class SurfaceLakesPopulator implements IPopulate {

  private static final int DEFAULT_WATER_ATTEMPTS_PER_CHUNK = 4;
  private static final int DEFAULT_LAVA_ATTEMPTS_PER_CHUNK = 1;

  private static final int DEFAULT_LAVA_RARITY = 10;
  private static final float DEFAULT_WATER_ATTEMPT_CHANCE = 0.25f;
  private final long seed;
  private final int waterAttemptsPerChunk;
  private final int lavaAttemptsPerChunk;
  private final int lavaRarity;
  private final float waterAttemptChance;
  private BlockPos currentChunkPos;

  public SurfaceLakesPopulator(long seed) {
    this(seed, null);
  }

  public SurfaceLakesPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.waterAttemptsPerChunk = PopulatorConfig.getInt(params, "water_attempts", DEFAULT_WATER_ATTEMPTS_PER_CHUNK);
    this.lavaAttemptsPerChunk = PopulatorConfig.getInt(params, "lava_attempts", DEFAULT_LAVA_ATTEMPTS_PER_CHUNK);
    this.lavaRarity = PopulatorConfig.rarityFrom(params, DEFAULT_LAVA_RARITY);
    this.waterAttemptChance = PopulatorConfig.getFloat(params, "water_attempt_chance", DEFAULT_WATER_ATTEMPT_CHANCE);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    this.currentChunkPos = chunkPos;

    for (int i = 0; i < waterAttemptsPerChunk; i++) {
      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

      if (random.nextFloat() < waterAttemptChance) {
        generateSurfaceLake(world, random, new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), true);
      }
    }

    for (int i = 0; i < lavaAttemptsPerChunk; i++) {
      if (random.nextInt(lavaRarity) == 0) {
        int x = chunkPos.getX() + random.nextInt(16);
        int z = chunkPos.getZ() + random.nextInt(16);
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        generateSurfaceLake(world, random, new BlockPos(x, y, z), Blocks.LAVA.defaultBlockState(), false);
      }
    }
  }

  private boolean generateSurfaceLake(WorldGenLevel world, RandomSource random, BlockPos center, BlockState liquidState, boolean isWater) {

    center = center.below(4);

    boolean[] lakeShape = new boolean[2048];

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
              lakeShape[(x * 16 + z) * 8 + y] = true;
            }
          }
        }
      }
    }

    int supportCount = 0;
    int voidCount = 0;
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = 4; y < 8; y++) {
          if (lakeShape[(x * 16 + z) * 8 + y]) {
            BlockPos checkPos = center.offset(x, y, z);
            if (!isInWritableArea(checkPos, currentChunkPos)) continue;
            BlockState state = world.getBlockState(checkPos);

            if (!state.getFluidState().isEmpty() && y >= 4) {
              return false;
            }

            if (!isWater) {
              BlockState aboveState = world.getBlockState(checkPos.above());
              if (aboveState.is(BlockTags.LOGS) || aboveState.is(BlockTags.LEAVES) ||
                  aboveState.is(BlockTags.PLANKS)) {
                return false;
              }
            }
          }
        }
      }
    }

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = 0; y < 4; y++) {
          if (!lakeShape[(x * 16 + z) * 8 + y]) continue;
          BlockPos lakePos = center.offset(x, y, z);
          if (!isInWritableArea(lakePos, currentChunkPos)) continue;
          BlockState state = world.getBlockState(lakePos);
          BlockState below = world.getBlockState(lakePos.below());
          boolean supported = state.isSolid() || below.isSolid();
          if (supported) {
            supportCount++;
          } else if (state.isAir() && below.isAir()) {
            voidCount++;
          }
        }
      }
    }
    if (voidCount > supportCount) {
      return false;
    }

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = 0; y < 8; y++) {
          boolean inLake = lakeShape[(x * 16 + z) * 8 + y];
          if (inLake) {
            BlockPos lakePos = center.offset(x, y, z);
            if (!isInWritableArea(lakePos, currentChunkPos)) continue;

            if (y >= 4) {

              world.setBlock(lakePos, Blocks.AIR.defaultBlockState(), 2);
            } else {

              world.setBlock(lakePos, liquidState, 2);
            }
          }
        }
      }
    }

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = 0; y < 8; y++) {
          boolean inLake = lakeShape[(x * 16 + z) * 8 + y];
          if (!inLake) {
            continue;
          }

          BlockPos edgePos = center.offset(x, y, z);

          boolean isEdge = x < 15 && !lakeShape[((x + 1) * 16 + z) * 8 + y];
          if (x > 0 && !lakeShape[((x - 1) * 16 + z) * 8 + y]) isEdge = true;
          if (z < 15 && !lakeShape[(x * 16 + (z + 1)) * 8 + y]) isEdge = true;
          if (z > 0 && !lakeShape[(x * 16 + (z - 1)) * 8 + y]) isEdge = true;

          if (isEdge && y < 4 && isInWritableArea(edgePos, currentChunkPos)) {

            BlockPos aboveEdge = edgePos.above();
            BlockState aboveState = world.getBlockState(aboveEdge);

            if (isWater) {

              if (aboveState.isSolid() && isInWritableArea(aboveEdge, currentChunkPos)) {
                if (random.nextInt(2) == 0) {
                  world.setBlock(aboveEdge, Blocks.SAND.defaultBlockState(), 2);
                } else {
                  world.setBlock(aboveEdge, Blocks.GRAVEL.defaultBlockState(), 2);
                }
              }

              if (y == 0 && random.nextInt(3) == 0) {
                world.setBlock(edgePos, Blocks.CLAY.defaultBlockState(), 2);
              }
            } else {

              if (aboveState.isSolid() && isInWritableArea(aboveEdge, currentChunkPos)) {
                world.setBlock(aboveEdge, Blocks.STONE.defaultBlockState(), 2);
              }

              if (y == 0 && random.nextInt(4) == 0) {
                world.setBlock(edgePos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
              }
            }
          }
        }
      }
    }

    if (!isWater) {
      for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
          for (int y = 4; y < 8; y++) {
            boolean inLake = lakeShape[(x * 16 + z) * 8 + y];
            if (inLake) {
              BlockPos burnPos = center.offset(x, y, z);
              if (!isInWritableArea(burnPos, currentChunkPos)) continue;
              BlockState burnState = world.getBlockState(burnPos);

              if (burnState.is(BlockTags.LOGS) || burnState.is(BlockTags.LEAVES) ||
                  burnState.is(BlockTags.PLANKS) || burnState.is(BlockTags.FLOWERS)) {
                world.setBlock(burnPos, Blocks.AIR.defaultBlockState(), 2);
              }
            }
          }
        }
      }
    }

    return true;
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:surface_lakes";
  }
}
