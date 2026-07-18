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
 * Generates tall vertical walls of dark stone that "weep" water or lava from
 * cracks in their surface. Made of deepslate, basalt, and dripstone with
 * pointed dripstone stalactites hanging from the top. Water/lava source blocks
 * seep from gaps, flowing down the face. Moss and glow lichen cling to the damp
 * surface. Eerie and primordial.
 */
public class WeepingWallsPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.035f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_HEIGHT = 20;
  private static final int MAX_HEIGHT = 45;
  private static final int MIN_LENGTH = 25;
  private static final int MAX_LENGTH = 60;
  private static final int WALL_THICKNESS = 3;

  private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
  private static final BlockState POLISHED_DEEPSLATE = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
  private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
  private static final BlockState POLISHED_BASALT = Blocks.POLISHED_BASALT.defaultBlockState();
  private static final BlockState DRIPSTONE = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
  private static final BlockState POINTED_DRIPSTONE = Blocks.POINTED_DRIPSTONE.defaultBlockState();
  private static final BlockState WATER = Blocks.WATER.defaultBlockState();
  private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
  private static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
  private static final BlockState GLOW_LICHEN = Blocks.GLOW_LICHEN.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public WeepingWallsPopulator(long seed) {
    this(seed, null);
  }

  public WeepingWallsPopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinBuildHeight() + 5 || surfaceY >= world.getMaxBuildHeight() - MAX_HEIGHT - 5) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
      int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
      double angle = random.nextDouble() * Math.PI * 2.0;
      boolean weepsLava = random.nextInt(5) == 0;

      generateWall(world, chunkPos, x, surfaceY, z, height, length, angle, weepsLava);
    }
  }

  private void generateWall(WorldGenLevel world, BlockPos chunkPos,
                            int startX, int surfaceY, int startZ,
                            int height, int length, double angle, boolean weepsLava) {
    double dx = Math.cos(angle);
    double dz = Math.sin(angle);

    double nx = -dz;
    double nz = dx;

    BlockState fluid = weepsLava ? LAVA : WATER;
    int buryDepth = 3;

    for (int step = 0; step < length; step++) {
      int bx = startX + (int) Math.round(dx * step);
      int bz = startZ + (int) Math.round(dz * step);
      if (!isInWritableColumn(bx, bz, chunkPos)) {
        continue;
      }
      int localSurfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;

      double lengthProgress = (double) step / length;
      double heightMultiplier = 1.0 - 0.5 * Math.pow(2.0 * lengthProgress - 1.0, 2);
      int localHeight = (int) (height * heightMultiplier);
      if (localHeight < 5) continue;

      for (int thick = 0; thick < WALL_THICKNESS; thick++) {
        int wx = bx + (int) Math.round(nx * thick);
        int wz = bz + (int) Math.round(nz * thick);

        for (int dy = -buryDepth; dy < localHeight; dy++) {
          int wy = localSurfaceY + dy;
          BlockPos pos = new BlockPos(wx, wy, wz);
          if (!isInWritableArea(pos, chunkPos)) {
            continue;
          }

          long blockHash = positionHash(seed, wx, wy, wz);
          float roll = hashFloat(blockHash);
          BlockState wallBlock;

          if (roll < 0.3f) {
            wallBlock = DEEPSLATE;
          } else if (roll < 0.5f) {
            wallBlock = POLISHED_DEEPSLATE;
          } else if (roll < 0.7f) {
            wallBlock = BASALT;
          } else if (roll < 0.85f) {
            wallBlock = POLISHED_BASALT;
          } else if (roll < 0.92f) {
            wallBlock = DRIPSTONE;
          } else {
            wallBlock = MOSS;
          }

          world.setBlock(pos, wallBlock, 2);
        }
      }

      long weepHash = positionHash(seed ^ 0xEE9L, bx, localSurfaceY, bz);
      if (hashFloat(weepHash) < 0.12f) {

        int weepY = localSurfaceY + 3 + (int) (hashFloat(positionHash(weepHash, bx, 0, bz)) * (localHeight - 6));

        int fx = bx + (int) Math.round(nx * WALL_THICKNESS);
        int fz = bz + (int) Math.round(nz * WALL_THICKNESS);
        BlockPos fluidPos = new BlockPos(fx, weepY, fz);
        if (isInWritableArea(fluidPos, chunkPos)) {
          BlockState existing = world.getBlockState(fluidPos);
          if (existing.isAir() || !existing.isSolid()) {
            world.setBlock(fluidPos, fluid, 2);
          }
        }
      }

      long lichenHash = positionHash(seed ^ 0x41CAL, bx, localSurfaceY, bz);
      if (hashFloat(lichenHash) < 0.15f) {
        int lichenY = localSurfaceY + 1 + (int) (hashFloat(positionHash(lichenHash, bx, 0, bz)) * (localHeight - 2));
        int fx = bx + (int) Math.round(nx * WALL_THICKNESS);
        int fz = bz + (int) Math.round(nz * WALL_THICKNESS);
        BlockPos lichenPos = new BlockPos(fx, lichenY, fz);
        if (isInWritableArea(lichenPos, chunkPos)) {
          BlockState existing = world.getBlockState(lichenPos);
          if (existing.isAir()) {
            world.setBlock(lichenPos, GLOW_LICHEN, 2);
          }
        }
      }

      if (step % 2 == 0) {
        long dripHash = positionHash(seed ^ 0xD71BL, bx, localSurfaceY + localHeight, bz);
        if (hashFloat(dripHash) < 0.35f) {
          int dripLen = 1 + (int) (hashFloat(positionHash(dripHash, bx, 0, bz)) * 3);
          for (int d = 0; d < dripLen; d++) {
            BlockPos dripPos = new BlockPos(bx, localSurfaceY + localHeight - d, bz);
            if (isInWritableArea(dripPos, chunkPos)) {
              world.setBlock(dripPos, POINTED_DRIPSTONE, 2);
            }
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:weeping_walls";
  }
}
