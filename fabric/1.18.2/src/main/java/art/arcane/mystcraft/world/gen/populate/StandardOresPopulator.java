package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Standard ore populator that provides baseline ore generation for all ages.
 * Generates ores at approximately vanilla rates (slightly reduced).
 * <p>
 * Uses chunk boundary checking to prevent cascade loading - blocks outside the
 * current chunk are simply skipped rather than triggering neighbor chunk loads.
 * <p>
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public class StandardOresPopulator implements IPopulate {

  // Ore configurations: blockState, veinSize, veinsPerChunk, minY, maxY
  private static final OreConfig[] ORE_CONFIGS = {
      new OreConfig(Blocks.COAL_ORE.defaultBlockState(), Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(),
          17, 10, -64, 192),
      new OreConfig(Blocks.IRON_ORE.defaultBlockState(), Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
          9, 10, -64, 72),
      new OreConfig(Blocks.COPPER_ORE.defaultBlockState(), Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState(),
          10, 8, -16, 112),
      new OreConfig(Blocks.GOLD_ORE.defaultBlockState(), Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(),
          9, 2, -64, 32),
      new OreConfig(Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(),
          8, 4, -64, 16),
      new OreConfig(Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(),
          8, 1, -64, 16),
      new OreConfig(Blocks.LAPIS_ORE.defaultBlockState(), Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState(),
          7, 1, -64, 64),
      new OreConfig(Blocks.EMERALD_ORE.defaultBlockState(), Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState(),
          3, 3, -16, 64)
  };
  private final long seed;
  private final float veinMultiplier;
  // Chunk boundaries for current population
  private int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ;

  public StandardOresPopulator(long seed) {
    this(seed, null);
  }

  public StandardOresPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.veinMultiplier = PopulatorConfig.getFloat(params, "vein_multiplier", 1.0f);
  }

  @Override
  public void populate(WorldGenLevel world, Random random, BlockPos chunkPos) {
    int chunkX = chunkPos.getX() >> 4;
    int chunkZ = chunkPos.getZ() >> 4;

    // Set chunk boundaries for this population run
    chunkMinX = chunkX << 4;
    chunkMaxX = chunkMinX + 15;
    chunkMinZ = chunkZ << 4;
    chunkMaxZ = chunkMinZ + 15;

    for (OreConfig config : ORE_CONFIGS) {
      generateOre(world, random, chunkPos, config);
    }
  }

  /**
   * Checks if a position is within the current chunk boundaries.
   * This prevents cascade chunk loading when ore veins extend beyond chunk edges.
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

  private void generateOre(WorldGenLevel world, Random random, BlockPos chunkPos, OreConfig config) {
    int chunkX = chunkPos.getX();
    int chunkZ = chunkPos.getZ();

    int veins = Math.max(0, Math.round(config.veinsPerChunk * veinMultiplier));
    for (int i = 0; i < veins; i++) {
      int x = chunkX + random.nextInt(16);
      int y = config.minY + random.nextInt(config.maxY - config.minY);
      int z = chunkZ + random.nextInt(16);

      generateVein(world, random, new BlockPos(x, y, z), config);
    }
  }

  private void generateVein(WorldGenLevel world, Random random, BlockPos center, OreConfig config) {
    int numberOfBlocks = config.veinSize;

    float angle = random.nextFloat() * (float) Math.PI;
    double xSpread = Math.sin(angle) * numberOfBlocks / 8.0;
    double zSpread = Math.cos(angle) * numberOfBlocks / 8.0;

    double startX = center.getX() + 8 + xSpread;
    double endX = center.getX() + 8 - xSpread;
    double startZ = center.getZ() + 8 + zSpread;
    double endZ = center.getZ() + 8 - zSpread;

    double startY = center.getY() + random.nextInt(3) - 2;
    double endY = center.getY() + random.nextInt(3) - 2;

    for (int block = 0; block < numberOfBlocks; block++) {
      float progress = (float) block / (float) numberOfBlocks;
      double interpolatedX = startX + (endX - startX) * progress;
      double interpolatedY = startY + (endY - startY) * progress;
      double interpolatedZ = startZ + (endZ - startZ) * progress;

      double radius = random.nextDouble() * numberOfBlocks / 16.0;
      double xzRadius = (Math.sin((float) Math.PI * progress) + 1.0) * radius + 1.0;
      double yRadius = (Math.sin((float) Math.PI * progress) + 1.0) * radius + 1.0;

      int minX = Mth.floor(interpolatedX - xzRadius / 2.0);
      int minY = Mth.floor(interpolatedY - yRadius / 2.0);
      int minZ = Mth.floor(interpolatedZ - xzRadius / 2.0);
      int maxX = Mth.floor(interpolatedX + xzRadius / 2.0);
      int maxY = Mth.floor(interpolatedY + yRadius / 2.0);
      int maxZ = Mth.floor(interpolatedZ + xzRadius / 2.0);

      for (int x = minX; x <= maxX; x++) {
        double xDist = (x + 0.5 - interpolatedX) / (xzRadius / 2.0);
        if (xDist * xDist < 1.0) {
          for (int y = minY; y <= maxY; y++) {
            double yDist = (y + 0.5 - interpolatedY) / (yRadius / 2.0);
            if (xDist * xDist + yDist * yDist < 1.0) {
              for (int z = minZ; z <= maxZ; z++) {
                double zDist = (z + 0.5 - interpolatedZ) / (xzRadius / 2.0);
                if (xDist * xDist + yDist * yDist + zDist * zDist < 1.0) {
                  BlockPos pos = new BlockPos(x, y, z);
                  tryPlaceOre(world, pos, config);
                }
              }
            }
          }
        }
      }
    }
  }

  private void tryPlaceOre(WorldGenLevel world, BlockPos pos, OreConfig config) {
    if (!isInChunk(pos)) {
      return;
    }

    BlockState existing = world.getBlockState(pos);

    // Replace any solid opaque block (supports custom terrain blocks)
    if (existing.isAir() || !existing.getMaterial().isSolid() || !existing.canOcclude()) {
      return;
    }

    // Use deepslate variant below Y=0, regular ore otherwise
    if (pos.getY() < 0) {
      safeSetBlock(world, pos, config.deepslateOreBlock);
    } else {
      safeSetBlock(world, pos, config.oreBlock);
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:standard_ores";
  }

  private record OreConfig(BlockState oreBlock, BlockState deepslateOreBlock, int veinSize, int veinsPerChunk, int minY, int maxY) {
  }
}
