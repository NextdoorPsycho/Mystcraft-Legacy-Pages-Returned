package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.registry.ModBlocks;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Star Fissure populator that generates star fissures (escape routes from
 * ages). A star fissure is a deep crack in the terrain with a starry void at
 * the bottom. Falling into it teleports entities back to the overworld spawn.
 * Very rare - only generates in approximately 1 out of 16 chunks.
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk
 * loads.
 */
public class StarFissurePopulator implements IPopulate {

  private static final int DEFAULT_RARITY = 16;

  private static final int DEFAULT_MIN_WIDTH = 3;
  private static final int DEFAULT_MAX_WIDTH = 5;
  private static final int DEFAULT_MIN_LENGTH = 10;
  private static final int DEFAULT_MAX_LENGTH = 20;
  private final long seed;
  private final int rarity;
  private final int minWidth;
  private final int maxWidth;
  private final int minLength;
  private final int maxLength;

  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public StarFissurePopulator(long seed) {
    this(seed, null);
  }

  public StarFissurePopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.rarity = PopulatorConfig.rarityFrom(params, DEFAULT_RARITY);
    this.minWidth = Math.max(1, PopulatorConfig.getInt(params, "min_width", DEFAULT_MIN_WIDTH));
    this.maxWidth = Math.max(this.minWidth, PopulatorConfig.getInt(params, "max_width", DEFAULT_MAX_WIDTH));
    this.minLength = Math.max(2, PopulatorConfig.getInt(params, "min_length", DEFAULT_MIN_LENGTH));
    this.maxLength = Math.max(this.minLength, PopulatorConfig.getInt(params, "max_length", DEFAULT_MAX_LENGTH));
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {

    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    RandomSource chunkRandom = chunkRandom(chunkX, chunkZ);
    if (chunkRandom.nextInt(rarity) != 0) {
      return;
    }

    int x = chunkPos.getX() + chunkRandom.nextInt(16);
    int z = chunkPos.getZ() + chunkRandom.nextInt(16);
    int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

    if (surfaceY < world.getMinBuildHeight() + 20) {
      return;
    }

    BlockPos centerPos = new BlockPos(x, surfaceY, z);

    if (!canGenerateFissure(world, centerPos)) {
      return;
    }

    generateStarFissure(world, chunkRandom, centerPos);
  }

  private boolean canGenerateFissure(WorldGenLevel world, BlockPos pos) {
    BlockState surface = world.getBlockState(pos.below());
    return surface.is(BlockTags.DIRT) ||
        surface.is(Blocks.GRASS_BLOCK) ||
        surface.is(Blocks.STONE) ||
        surface.is(Blocks.DEEPSLATE) ||
        surface.is(Blocks.SAND) ||
        surface.is(Blocks.SANDSTONE);
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

  private void generateStarFissure(WorldGenLevel world, RandomSource random, BlockPos centerPos) {
    int width = minWidth + random.nextInt(maxWidth - minWidth + 1);
    int length = minLength + random.nextInt(maxLength - minLength + 1);

    double angle = random.nextDouble() * Math.PI * 2.0;
    double cosAngle = Math.cos(angle);
    double sinAngle = Math.sin(angle);

    int surfaceY = centerPos.getY();

    int bottomY = world.getMinBuildHeight() + 1;

    for (int lPos = 0; lPos < length; lPos++) {
      double progress = (double) lPos / (double) length;

      int segmentWidth = (int) (width * (1.0 - progress * 0.3 + random.nextDouble() * 0.3));
      segmentWidth = Math.max(2, segmentWidth);

      int baseX = (int) (centerPos.getX() + lPos * cosAngle);
      int baseZ = (int) (centerPos.getZ() + lPos * sinAngle);

      int jaggedness = random.nextInt(2) - random.nextInt(2);
      baseX += jaggedness;
      baseZ += jaggedness;

      for (int wPos = -segmentWidth / 2; wPos <= segmentWidth / 2; wPos++) {
        int offsetX = (int) (wPos * -sinAngle);
        int offsetZ = (int) (wPos * cosAngle);

        int x = baseX + offsetX;
        int z = baseZ + offsetZ;

        double distFromCenter = Math.abs(wPos) / (double) (segmentWidth / 2);
        int depthReduction = (int) (distFromCenter * distFromCenter * (surfaceY - bottomY) * 0.3);

        int fissureTop = surfaceY;
        int fissureBottom = bottomY + depthReduction;

        for (int y = fissureTop; y >= fissureBottom; y--) {
          BlockPos pos = new BlockPos(x, y, z);

          if (!isInChunk(pos)) {
            continue;
          }

          BlockState existing = world.getBlockState(pos);

          if (!existing.is(Blocks.BEDROCK)) {
            if (y == fissureBottom) {
              safeSetBlock(world, pos, ModBlocks.STAR_FISSURE.get().defaultBlockState());
            } else if (y == fissureBottom + 1) {
              safeSetBlock(world, pos, Blocks.AIR.defaultBlockState());
            } else {
              safeSetBlock(world, pos, Blocks.CAVE_AIR.defaultBlockState());
            }
          }
        }

        if (random.nextInt(8) == 0) {
          addJaggedEdge(world, random, new BlockPos(x, fissureTop, z), fissureTop, fissureBottom);
        }
      }
    }

    addStarFissureBlocks(world, random, centerPos, length, angle, bottomY);
  }

  private void addJaggedEdge(WorldGenLevel world, RandomSource random, BlockPos edgePos, int top, int bottom) {
    int edgeDepth = 1 + random.nextInt(3);
    int edgeHeight = top - random.nextInt((top - bottom) / 3);

    for (int y = edgeHeight; y >= bottom && y >= edgeHeight - edgeDepth; y--) {
      BlockPos pos = new BlockPos(edgePos.getX(), y, edgePos.getZ());
      if (!isInChunk(pos)) {
        continue;
      }
      if (!world.getBlockState(pos).is(Blocks.BEDROCK)) {
        safeSetBlock(world, pos, Blocks.CAVE_AIR.defaultBlockState());
      }
    }
  }

  private void addStarFissureBlocks(WorldGenLevel world, RandomSource random, BlockPos centerPos,
                                    int length, double angle, int bottomY) {
    double cosAngle = Math.cos(angle);
    double sinAngle = Math.sin(angle);

    int fissureBlocks = 3 + random.nextInt(5);

    for (int i = 0; i < fissureBlocks; i++) {
      int lPos = random.nextInt(length);
      int x = (int) (centerPos.getX() + lPos * cosAngle + (random.nextDouble() - 0.5) * 2);
      int z = (int) (centerPos.getZ() + lPos * sinAngle + (random.nextDouble() - 0.5) * 2);

      BlockPos fissurePos = new BlockPos(x, bottomY, z);

      if (!isInChunk(fissurePos)) {
        continue;
      }

      if (world.getBlockState(fissurePos).is(Blocks.CAVE_AIR) ||
          world.getBlockState(fissurePos).isAir()) {
        safeSetBlock(world, fissurePos, ModBlocks.STAR_FISSURE.get().defaultBlockState());
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:star_fissure";
  }

  public BlockPos findCandidateInChunk(WorldGenLevel world, int chunkX, int chunkZ) {
    RandomSource chunkRandom = chunkRandom(chunkX, chunkZ);
    if (chunkRandom.nextInt(rarity) != 0) {
      return null;
    }
    int chunkBlockX = chunkX << 4;
    int chunkBlockZ = chunkZ << 4;
    int x = chunkBlockX + chunkRandom.nextInt(16);
    int z = chunkBlockZ + chunkRandom.nextInt(16);
    int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    if (surfaceY < world.getMinBuildHeight() + 20) {
      return null;
    }
    BlockPos centerPos = new BlockPos(x, surfaceY, z);
    return canGenerateFissure(world, centerPos) ? centerPos : null;
  }

  public void forceGenerate(WorldGenLevel world, BlockPos centerPos) {
    int chunkX = centerPos.getX() >> 4;
    int chunkZ = centerPos.getZ() >> 4;
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;
    RandomSource rand = chunkRandom(chunkX, chunkZ);
    generateStarFissure(world, rand, centerPos);
  }

  private RandomSource chunkRandom(int chunkX, int chunkZ) {
    long chunkSeed = seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0x5A7F1D3L);
    return RandomSource.create(chunkSeed);
  }
}
