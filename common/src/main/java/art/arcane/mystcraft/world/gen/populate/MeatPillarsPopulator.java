package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Meat Pillars populator that generates fleshy organic columns. Crimson stem
 * cores wrapped in nether wart block flesh, with red mushroom block tumors,
 * shroomlight veins, weeping vines, and crimson fungus at the base. Something
 * alive grew wrong. Uses the neighbor-seed pattern for multi-chunk safety.
 */
public class MeatPillarsPopulator implements IPopulate {

  private static final int DEFAULT_COUNT = 1;
  private static final float DEFAULT_SPAWN_CHANCE = 0.04f;
  private static final int DEFAULT_NEIGHBOR_RANGE = 1;
  private static final int DEFAULT_MIN_HEIGHT = 10;
  private static final int DEFAULT_MAX_HEIGHT = 30;
  private static final int DEFAULT_MIN_WIDTH = 2;
  private static final int DEFAULT_MAX_WIDTH = 4;

  private final long seed;
  private final int count;
  private final float spawnChance;
  private final int neighborRange;
  private final int minHeight;
  private final int maxHeight;
  private final int minWidth;
  private final int maxWidth;

  public MeatPillarsPopulator(long seed) {
    this(seed, null);
  }

  public MeatPillarsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.neighborRange = Math.max(1, PopulatorConfig.getInt(params, "neighbor_range", DEFAULT_NEIGHBOR_RANGE));
    this.minHeight = Math.max(3, PopulatorConfig.getInt(params, "min_height", DEFAULT_MIN_HEIGHT));
    this.maxHeight = Math.max(this.minHeight, PopulatorConfig.getInt(params, "max_height", DEFAULT_MAX_HEIGHT));
    this.minWidth = Math.max(1, PopulatorConfig.getInt(params, "min_width", DEFAULT_MIN_WIDTH));
    this.maxWidth = Math.max(this.minWidth, PopulatorConfig.getInt(params, "max_width", DEFAULT_MAX_WIDTH));
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
    int thisChunkX = chunkPos.getX() >> 4;
    int thisChunkZ = chunkPos.getZ() >> 4;

    int chunkMinX = thisChunkX << 4;
    int chunkMaxX = chunkMinX + 15;
    int chunkMinZ = thisChunkZ << 4;
    int chunkMaxZ = chunkMinZ + 15;

    for (int ncx = thisChunkX - neighborRange; ncx <= thisChunkX + neighborRange; ncx++) {
      for (int ncz = thisChunkZ - neighborRange; ncz <= thisChunkZ + neighborRange; ncz++) {
        long chunkSeed = getChunkSeed(ncx, ncz);
        Random chunkRand = new Random(chunkSeed);

        int neighborMinX = ncx << 4;
        int neighborMinZ = ncz << 4;

        for (int i = 0; i < count; i++) {

          if (chunkRand.nextFloat() >= spawnChance) {
            continue;
          }

          int cx = neighborMinX + chunkRand.nextInt(16);
          int cz = neighborMinZ + chunkRand.nextInt(16);
          int height = minHeight + chunkRand.nextInt(maxHeight - minHeight + 1);
          int width = minWidth + chunkRand.nextInt(maxWidth - minWidth + 1);
          long pillarSeed = chunkRand.nextLong();

          if (cx + width < chunkMinX || cx - width > chunkMaxX ||
              cz + width < chunkMinZ || cz - width > chunkMaxZ) {
            continue;
          }

          int baseY = 40 + (int) ((pillarSeed & 0x7FL) % 60);

          generatePillar(world, pillarSeed, cx, baseY, cz, height, width,
              chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
      }
    }
  }

  private long getChunkSeed(int chunkX, int chunkZ) {
    return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0xAE47B1L);
  }

  private void generatePillar(WorldGenLevel world, long pillarSeed, int cx, int baseY, int cz,
                              int height, int width,
                              int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {

    int startY = baseY - 2;
    int endY = baseY + height;

    if (endY >= world.getMaxBuildHeight()) {
      endY = world.getMaxBuildHeight() - 1;
    }
    if (startY <= world.getMinBuildHeight()) {
      startY = world.getMinBuildHeight() + 1;
    }

    BlockState fleshBlock = Blocks.NETHER_WART_BLOCK.defaultBlockState();
    BlockState coreBlock = Blocks.CRIMSON_STEM.defaultBlockState();
    BlockState tumorBlock = Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
    BlockState veinBlock = Blocks.SHROOMLIGHT.defaultBlockState();

    long veinIntervalHash = positionHash(pillarSeed, cx, 0, cz);
    int veinInterval = 5 + (int) ((veinIntervalHash & 0x3L));

    for (int y = startY; y <= endY; y++) {
      double progress = (double) (y - startY) / (endY - startY);

      double taper = 1.0 - (progress * progress * 0.3);
      int effectiveWidth = (int) Math.max(1, Math.round(width * taper));

      for (int dx = -effectiveWidth; dx <= effectiveWidth; dx++) {
        for (int dz = -effectiveWidth; dz <= effectiveWidth; dz++) {
          int bx = cx + dx;
          int bz = cz + dz;

          if (bx < chunkMinX || bx > chunkMaxX || bz < chunkMinZ || bz > chunkMaxZ) {
            continue;
          }

          double distSq = (double) dx * dx + (double) dz * dz;
          double radiusSq = (double) effectiveWidth * effectiveWidth;

          if (distSq > radiusSq) {
            continue;
          }

          BlockPos pos = new BlockPos(bx, y, bz);

          if (!shouldPlaceBlock(world, pos)) {
            continue;
          }

          boolean isCore = (dx == 0 && dz == 0) || (effectiveWidth <= 1 && distSq == 0);
          if (isCore) {
            world.setBlock(pos, coreBlock, 2);
          } else {
            world.setBlock(pos, fleshBlock, 2);
          }
        }
      }

      if (y > baseY && (y - baseY) % veinInterval == 0) {

        if (cx >= chunkMinX && cx <= chunkMaxX && cz >= chunkMinZ && cz <= chunkMaxZ) {
          BlockPos veinPos = new BlockPos(cx, y, cz);
          world.setBlock(veinPos, veinBlock, 2);
        }
      }

      long tumorHash = positionHash(pillarSeed ^ 0xDA0C1FL, cx, y, cz);
      boolean hasTumor = ((tumorHash & 0xFFFF) % 100) < 15;

      if (hasTumor && y > baseY && y < endY - 2) {

        int tumorDir = (int) ((tumorHash >>> 16) & 0x3);
        int tdx = (tumorDir == 0) ? 1 : (tumorDir == 1) ? -1 : 0;
        int tdz = (tumorDir == 2) ? 1 : (tumorDir == 3) ? -1 : 0;

        int tumorBaseX = cx + tdx * (width + 1);
        int tumorBaseZ = cz + tdz * (width + 1);

        for (int tx = 0; tx <= 1; tx++) {
          for (int tz = 0; tz <= 1; tz++) {
            for (int ty = 0; ty <= 1; ty++) {
              int tbx = tumorBaseX + tx;
              int tbz = tumorBaseZ + tz;
              int tby = y + ty;

              if (tbx < chunkMinX || tbx > chunkMaxX || tbz < chunkMinZ || tbz > chunkMaxZ) {
                continue;
              }
              if (tby >= world.getMaxBuildHeight()) {
                continue;
              }

              BlockPos tumorPos = new BlockPos(tbx, tby, tbz);
              if (shouldPlaceBlock(world, tumorPos)) {
                world.setBlock(tumorPos, tumorBlock, 2);
              }
            }
          }
        }

        long vineHash = positionHash(pillarSeed ^ 0xF1DE5L, tumorBaseX, y, tumorBaseZ);
        int vineLength = 2 + (int) ((vineHash & 0x3));

        for (int vx = 0; vx <= 1; vx++) {
          for (int vz = 0; vz <= 1; vz++) {
            int vbx = tumorBaseX + vx;
            int vbz = tumorBaseZ + vz;

            if (vbx < chunkMinX || vbx > chunkMaxX || vbz < chunkMinZ || vbz > chunkMaxZ) {
              continue;
            }

            long vineBlockHash = positionHash(vineHash, vbx, y, vbz);
            if ((vineBlockHash & 0x1) == 0) {
              continue;
            }

            for (int vy = 1; vy <= vineLength; vy++) {
              int vby = y - vy;
              if (vby <= world.getMinBuildHeight()) {
                break;
              }

              BlockPos vinePos = new BlockPos(vbx, vby, vbz);
              if (world.getBlockState(vinePos).isAir()) {
                world.setBlock(vinePos, Blocks.WEEPING_VINES.defaultBlockState(), 2);
              } else {
                break;
              }
            }
          }
        }
      }
    }

    long baseFungusHash = positionHash(pillarSeed ^ 0xBA5E1L, cx, baseY, cz);
    int fungusSpread = 2 + (int) ((baseFungusHash & 0x1));

    for (int dx = -fungusSpread; dx <= fungusSpread; dx++) {
      for (int dz = -fungusSpread; dz <= fungusSpread; dz++) {
        if (dx == 0 && dz == 0) {
          continue;
        }

        int bx = cx + dx;
        int bz = cz + dz;

        if (bx < chunkMinX || bx > chunkMaxX || bz < chunkMinZ || bz > chunkMaxZ) {
          continue;
        }

        long fungusHash = positionHash(baseFungusHash, bx, baseY, bz);
        if ((fungusHash & 0x3) != 0) {
          continue;
        }

        BlockPos fungusPos = new BlockPos(bx, baseY + 1, bz);
        if (world.getBlockState(fungusPos).isAir()) {
          BlockState below = world.getBlockState(new BlockPos(bx, baseY, bz));
          if (below.isSolid()) {
            world.setBlock(fungusPos, Blocks.CRIMSON_FUNGUS.defaultBlockState(), 2);
          }
        }
      }
    }
  }

  private boolean shouldPlaceBlock(WorldGenLevel world, BlockPos pos) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() ||
        existing.is(BlockTags.LEAVES) ||
        existing.is(Blocks.SNOW) ||
        existing.is(Blocks.VINE) ||
        existing.is(Blocks.WATER)) {
      return true;
    }
    return existing.is(Blocks.STONE) ||
        existing.is(Blocks.DEEPSLATE) ||
        existing.is(Blocks.DIRT) ||
        existing.is(Blocks.GRASS_BLOCK) ||
        existing.is(Blocks.SAND) ||
        existing.is(Blocks.SANDSTONE) ||
        existing.is(Blocks.GRAVEL) ||
        existing.is(Blocks.CLAY) ||
        existing.is(Blocks.NETHERRACK) ||
        existing.is(Blocks.END_STONE) ||
        existing.is(BlockTags.TERRACOTTA) ||
        existing.is(BlockTags.DIRT);
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:meat_pillars";
  }
}
