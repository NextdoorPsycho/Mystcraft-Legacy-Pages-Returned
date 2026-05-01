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
 * Generates the fossilized skeleton of a colossal creature partially buried in
 * the terrain. A long curving spine of bone blocks (40-100 blocks), ribcage
 * arches every 5-7 blocks, a skull at one end with hollow eye sockets, and a
 * tapering tail at the other. Soul fire accents. Whatever this was, it was
 * enormous. And ancient.
 */
public class FossilizedLeviathanPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.02f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_LENGTH = 40;
  private static final int MAX_LENGTH = 100;

  private static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();
  private static final BlockState COAL = Blocks.COAL_BLOCK.defaultBlockState();
  private static final BlockState SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();
  private static final BlockState SOUL_FIRE = Blocks.SOUL_FIRE.defaultBlockState();
  private static final BlockState SOUL_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();
  private static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public FossilizedLeviathanPopulator(long seed) {
    this(seed, null);
  }

  public FossilizedLeviathanPopulator(long seed, JsonObject params) {
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

  private static float hashFloat(long hash) {
    return ((hash >>> 16) & 0xFFFFL) / 65536.0f;
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    for (int i = 0; i < count; i++) {
      if (random.nextFloat() >= spawnChance) {
        continue;
      }

      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

      if (surfaceY <= world.getMinBuildHeight() + 10 || surfaceY >= world.getMaxBuildHeight() - 20) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
      double angle = random.nextDouble() * Math.PI * 2.0;
      int buryDepth = 2 + random.nextInt(3);

      generateLeviathan(world, chunkPos, x, surfaceY, z, length, angle, buryDepth);
    }
  }

  private void generateLeviathan(WorldGenLevel world, BlockPos chunkPos,
                                 int startX, int surfaceY, int startZ,
                                 int length, double angle, int buryDepth) {
    double dx = Math.cos(angle);
    double dz = Math.sin(angle);
    int ribInterval = 5 + (int) (hashFloat(positionHash(seed, startX, surfaceY, startZ)) * 3);
    int ribCounter = 0;

    int skullLength = 8 + (int) (hashFloat(positionHash(seed ^ 0x5C11L, startX, surfaceY, startZ)) * 5);

    for (int step = 0; step < length; step++) {
      int bx = startX + (int) Math.round(dx * step);
      int bz = startZ + (int) Math.round(dz * step);

      double undulation = Math.sin(step * 0.08) * 3.0;

      double curve = Math.sin(step * 0.04) * 5.0;
      bx += (int) Math.round(-dz * curve);
      bz += (int) Math.round(dx * curve);

      int localSurfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
      int spineY = localSurfaceY - buryDepth + (int) undulation;

      double progress = (double) step / length;

      int spineWidth;
      if (progress < 0.1) {

        spineWidth = 3;
      } else if (progress < 0.7) {

        spineWidth = 2;
      } else {

        spineWidth = Math.max(1, (int) (2 * (1.0 - (progress - 0.7) / 0.3)));
      }

      for (int sw = -spineWidth / 2; sw <= spineWidth / 2; sw++) {
        int spX = bx + (int) Math.round(-dz * sw);
        int spZ = bz + (int) Math.round(dx * sw);
        BlockPos spinePos = new BlockPos(spX, spineY, spZ);
        if (isInWritableArea(spinePos, chunkPos)) {
          placeBone(world, spinePos);
        }

        if (step % 2 == 0) {
          BlockPos bumpPos = new BlockPos(spX, spineY + 1, spZ);
          if (isInWritableArea(bumpPos, chunkPos)) {
            placeBone(world, bumpPos);
          }
        }
      }

      if (step < skullLength) {
        generateSkullSection(world, chunkPos, bx, spineY, bz, step, skullLength, dx, dz);
      }

      if (progress >= 0.15 && progress <= 0.70) {
        ribCounter++;
        if (ribCounter >= ribInterval) {
          ribCounter = 0;

          int ribHeight = 6 + (int) (4 * (1.0 - Math.abs(progress - 0.4) / 0.3));
          generateRib(world, chunkPos, bx, spineY, bz, ribHeight, dx, dz);
        }
      }

      long decorHash = positionHash(seed ^ 0xDE03L, bx, spineY, bz);
      if (hashFloat(decorHash) < 0.06f) {

        for (int sdx = -2; sdx <= 2; sdx++) {
          for (int sdz = -2; sdz <= 2; sdz++) {
            if (sdx * sdx + sdz * sdz > 4) continue;
            int sx = bx + sdx;
            int sz = bz + sdz;
            int sy = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz) - 1;
            BlockPos soulPos = new BlockPos(sx, sy, sz);
            if (isInWritableArea(soulPos, chunkPos)) {
              BlockState existing = world.getBlockState(soulPos);
              if (existing.isSolid() && !existing.is(Blocks.BONE_BLOCK)) {
                world.setBlock(soulPos, SOUL_SAND, 2);

                BlockPos firePos = soulPos.above();
                if (isInWritableArea(firePos, chunkPos) && world.getBlockState(firePos).isAir()) {
                  long fireHash = positionHash(seed, sx, sy + 1, sz);
                  if (hashFloat(fireHash) < 0.3f) {
                    world.setBlock(firePos, SOUL_FIRE, 2);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void generateSkullSection(WorldGenLevel world, BlockPos chunkPos,
                                    int bx, int spineY, int bz,
                                    int step, int skullLength,
                                    double dx, double dz) {

    double skullProgress = (double) step / skullLength;
    int skullWidth = (int) (5 - 2 * skullProgress);
    int skullHeight = (int) (6 - 2 * skullProgress);

    for (int sw = -skullWidth / 2; sw <= skullWidth / 2; sw++) {
      for (int sh = 0; sh < skullHeight; sh++) {
        int sx = bx + (int) Math.round(-dz * sw);
        int sz = bz + (int) Math.round(dx * sw);
        BlockPos skullPos = new BlockPos(sx, spineY + sh, sz);
        if (!isInWritableArea(skullPos, chunkPos)) {
          continue;
        }

        boolean isShell = Math.abs(sw) == skullWidth / 2
            || sh == 0 || sh == skullHeight - 1;

        if (isShell) {
          placeBone(world, skullPos);
        } else if (sh == skullHeight - 2 && step <= 2) {

          if (sw == -1 || sw == 1) {
            world.setBlock(skullPos, COAL, 2);
          } else {
            world.setBlock(skullPos, AIR, 2);
          }
        } else {
          world.setBlock(skullPos, AIR, 2);
        }
      }
    }

    if (step < skullLength / 2) {
      BlockPos jawPos = new BlockPos(bx, spineY - 1, bz);
      if (isInWritableArea(jawPos, chunkPos)) {
        placeBone(world, jawPos);
      }

      if (step <= 2) {
        for (int tw = -skullWidth / 2; tw <= skullWidth / 2; tw += 2) {
          int tx = bx + (int) Math.round(-dz * tw);
          int tz = bz + (int) Math.round(dx * tw);
          BlockPos toothPos = new BlockPos(tx, spineY - 2, tz);
          if (isInWritableArea(toothPos, chunkPos)) {
            world.setBlock(toothPos, CALCITE, 2);
          }
        }
      }
    }
  }

  private void generateRib(WorldGenLevel world, BlockPos chunkPos,
                           int bx, int spineY, int bz,
                           int ribHeight, double dx, double dz) {

    for (int side = -1; side <= 1; side += 2) {
      for (int r = 0; r <= ribHeight; r++) {

        double normalizedR = (double) r / ribHeight;
        double archWidth = ribHeight * 0.6 * Math.sqrt(1.0 - normalizedR * normalizedR);

        int ribX = bx + (int) Math.round(-dz * side * archWidth);
        int ribZ = bz + (int) Math.round(dx * side * archWidth);
        int ribY = spineY + r;

        BlockPos ribPos = new BlockPos(ribX, ribY, ribZ);
        if (isInWritableArea(ribPos, chunkPos)) {
          placeBone(world, ribPos);
        }
      }
    }
  }

  private void placeBone(WorldGenLevel world, BlockPos pos) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() || existing.is(BlockTags.LEAVES) || existing.is(Blocks.SNOW)
        || existing.is(Blocks.VINE) || existing.is(Blocks.WATER)
        || existing.is(Blocks.STONE) || existing.is(Blocks.DEEPSLATE)
        || existing.is(Blocks.DIRT) || existing.is(Blocks.GRASS_BLOCK)
        || existing.is(Blocks.SAND) || existing.is(Blocks.GRAVEL)
        || existing.is(BlockTags.DIRT)) {
      world.setBlock(pos, BONE, 2);
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:fossilized_leviathan";
  }
}
