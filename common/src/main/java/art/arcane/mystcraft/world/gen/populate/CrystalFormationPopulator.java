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
 * Crystal formation populator that generates clusters of angled crystalline
 * shards growing from the ground at tilted angles. Each formation consists of
 * 3-7 shards radiating from a central point, leaning outward with amethyst and
 * prismarine materials. Single-chunk populator using isInWritableArea for
 * boundary safety.
 */
public class CrystalFormationPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.06f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_SHARDS = 3;
  private static final int MAX_SHARDS = 7;
  private static final int MIN_SHARD_HEIGHT = 8;
  private static final int MAX_SHARD_HEIGHT = 25;
  private static final int BURY_DEPTH = 3;

  private final long seed;
  private final float spawnChance;
  private final int count;

  public CrystalFormationPopulator(long seed) {
    this(seed, null);
  }

  public CrystalFormationPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
  }

  private static long positionHash(long seed, int x, int y, int z) {
    long h = seed;
    h ^= (long) x * 73856093L;
    h ^= (long) y * 19349663L;
    h ^= (long) z * 83492791L;
    h = h * 6364136223846793005L + 1442695040888963407L;
    return h;
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    for (int attempt = 0; attempt < count; attempt++) {
      if (random.nextFloat() >= spawnChance) {
        continue;
      }

      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

      if (surfaceY <= world.getMinBuildHeight() + BURY_DEPTH + 1
          || surfaceY >= world.getMaxBuildHeight() - MAX_SHARD_HEIGHT - 2) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      long formationSeed = positionHash(seed, x, surfaceY, z);
      generateFormation(world, chunkPos, formationSeed, x, surfaceY, z);
    }
  }

  private void generateFormation(WorldGenLevel world, BlockPos chunkPos,
                                 long formationSeed, int centerX, int surfaceY, int centerZ) {
    int shardCount = MIN_SHARDS + (int) ((formationSeed & 0xFL) % (MAX_SHARDS - MIN_SHARDS + 1));
    long shardSeed = formationSeed;

    placeBase(world, chunkPos, centerX, surfaceY, centerZ, formationSeed);

    for (int i = 0; i < shardCount; i++) {
      shardSeed = positionHash(shardSeed, i, shardCount, (int) (formationSeed >>> 16));
      generateShard(world, chunkPos, shardSeed, centerX, surfaceY, centerZ, i, shardCount);
    }
  }

  private void placeBase(WorldGenLevel world, BlockPos chunkPos,
                         int centerX, int surfaceY, int centerZ, long baseSeed) {
    BlockState baseBud = Blocks.SMALL_AMETHYST_BUD.defaultBlockState();
    BlockState darkPrismarine = Blocks.DARK_PRISMARINE.defaultBlockState();

    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        BlockPos basePos = new BlockPos(centerX + dx, surfaceY, centerZ + dz);
        if (isInWritableArea(basePos, chunkPos)) {
          world.setBlock(basePos, darkPrismarine, 2);
        }
      }
    }

    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        if (dx == 0 && dz == 0) {
          continue;
        }
        long budHash = positionHash(baseSeed, centerX + dx, surfaceY, centerZ + dz);
        if ((budHash & 0x3) == 0) {
          BlockPos budPos = new BlockPos(centerX + dx, surfaceY + 1, centerZ + dz);
          if (isInWritableArea(budPos, chunkPos) && world.getBlockState(budPos).isAir()) {
            world.setBlock(budPos, baseBud, 2);
          }
        }
      }
    }
  }

  private void generateShard(WorldGenLevel world, BlockPos chunkPos, long shardSeed,
                             int centerX, int surfaceY, int centerZ,
                             int shardIndex, int totalShards) {
    int shardHeight = MIN_SHARD_HEIGHT + (int) ((shardSeed & 0xFFL) % (MAX_SHARD_HEIGHT - MIN_SHARD_HEIGHT + 1));

    double baseAngle = (2.0 * Math.PI * shardIndex) / totalShards;
    double angleJitter = ((shardSeed >>> 8) & 0xFFL) / 255.0 * 0.6 - 0.3;
    double angle = baseAngle + angleJitter;

    int leanInterval = 3 + (int) ((shardSeed >>> 16) & 0x3);
    double leanDx = Math.cos(angle) * (0.8 + ((shardSeed >>> 20) & 0x3) * 0.3);
    double leanDz = Math.sin(angle) * (0.8 + ((shardSeed >>> 24) & 0x3) * 0.3);

    int shardWidth = 1 + (int) ((shardSeed >>> 28) & 0x1);

    int startY = surfaceY - (2 + (int) ((shardSeed >>> 30) & 0x1));

    double currentDx = 0.0;
    double currentDz = 0.0;

    for (int dy = 0; dy < shardHeight; dy++) {
      int blockY = startY + dy;
      if (blockY >= world.getMaxBuildHeight() - 1) {
        break;
      }

      if (dy > 0 && dy % leanInterval == 0) {
        currentDx += leanDx;
        currentDz += leanDz;
      }

      int blockX = centerX + (int) Math.round(currentDx);
      int blockZ = centerZ + (int) Math.round(currentDz);

      BlockState material = getShardMaterial(shardSeed, blockX, blockY, blockZ);

      if (shardWidth == 1) {
        placeSingleBlock(world, chunkPos, blockX, blockY, blockZ, material);
      } else {

        placeSingleBlock(world, chunkPos, blockX, blockY, blockZ, material);

        int extX = blockX + (Math.abs(leanDx) > Math.abs(leanDz) ? 0 : (leanDx > 0 ? 1 : -1));
        int extZ = blockZ + (Math.abs(leanDz) > Math.abs(leanDx) ? 0 : (leanDz > 0 ? 1 : -1));
        placeSingleBlock(world, chunkPos, extX, blockY, extZ, material);
      }

      if (dy == shardHeight - 1) {
        BlockPos tipPos = new BlockPos(blockX, blockY + 1, blockZ);
        if (isInWritableArea(tipPos, chunkPos) && canPlaceCrystal(world, tipPos)) {
          world.setBlock(tipPos, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);
        }
      }
    }
  }

  private void placeSingleBlock(WorldGenLevel world, BlockPos chunkPos,
                                int x, int y, int z, BlockState material) {
    BlockPos pos = new BlockPos(x, y, z);
    if (isInWritableArea(pos, chunkPos) && canPlaceCrystal(world, pos)) {
      world.setBlock(pos, material, 2);
    }
  }

  private BlockState getShardMaterial(long shardSeed, int x, int y, int z) {
    long hash = positionHash(shardSeed, x, y, z);
    int roll = (int) ((hash >>> 8) & 0xFF);

    if (roll < 13) {
      return Blocks.TINTED_GLASS.defaultBlockState();
    }

    if (roll < 39) {
      return Blocks.BUDDING_AMETHYST.defaultBlockState();
    }

    return Blocks.AMETHYST_BLOCK.defaultBlockState();
  }

  private boolean canPlaceCrystal(WorldGenLevel world, BlockPos pos) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() || existing.is(Blocks.SNOW) || existing.is(Blocks.WATER)
        || existing.is(BlockTags.LEAVES) || existing.is(Blocks.VINE)) {
      return true;
    }

    return existing.is(Blocks.STONE) ||
        existing.is(Blocks.DEEPSLATE) ||
        existing.is(Blocks.DIRT) ||
        existing.is(Blocks.GRASS_BLOCK) ||
        existing.is(Blocks.SAND) ||
        existing.is(Blocks.GRAVEL) ||
        existing.is(Blocks.CLAY) ||
        existing.is(BlockTags.DIRT);
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:crystal_formation";
  }
}
