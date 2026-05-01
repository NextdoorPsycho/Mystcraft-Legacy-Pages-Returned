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
 * Generates smooth obsidian slabs jutting from the ground at impossible angles.
 * Flat slab shapes (wide but thin) that are 15-35 blocks tall, 8-15 blocks
 * wide, and 2-3 blocks thick, tilted at ~15-30 degrees. Obsidian with crying
 * obsidian accents and occasional amethyst veins. Partially buried, giving the
 * impression of something massive beneath the surface. Deeply mysterious
 * monolith-like structures.
 */
public class ObsidianMonolithsPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.035f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_HEIGHT = 15;
  private static final int MAX_HEIGHT = 35;
  private static final int MIN_WIDTH = 8;
  private static final int MAX_WIDTH = 15;
  private static final int SLAB_THICKNESS = 2;
  private static final int BURY_DEPTH = 4;

  private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
  private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();
  private static final BlockState AMETHYST = Blocks.AMETHYST_BLOCK.defaultBlockState();
  private static final BlockState BUDDING_AMETHYST = Blocks.BUDDING_AMETHYST.defaultBlockState();
  private static final BlockState TINTED_GLASS = Blocks.TINTED_GLASS.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public ObsidianMonolithsPopulator(long seed) {
    this(seed, null);
  }

  public ObsidianMonolithsPopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinBuildHeight() + BURY_DEPTH + 2
          || surfaceY >= world.getMaxBuildHeight() - MAX_HEIGHT - 5) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int slabHeight = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
      int slabWidth = MIN_WIDTH + random.nextInt(MAX_WIDTH - MIN_WIDTH + 1);
      double facing = random.nextDouble() * Math.PI * 2.0;

      double tiltAngle = (15.0 + random.nextDouble() * 15.0) * Math.PI / 180.0;

      generateMonolith(world, chunkPos, x, surfaceY, z,
          slabHeight, slabWidth, facing, tiltAngle);
    }
  }

  private void generateMonolith(WorldGenLevel world, BlockPos chunkPos,
                                int cx, int surfaceY, int cz,
                                int height, int width, double facing, double tilt) {

    double faceDx = Math.cos(facing);
    double faceDz = Math.sin(facing);

    double widthDx = -faceDz;
    double widthDz = faceDx;

    int halfWidth = width / 2;
    int startY = surfaceY - BURY_DEPTH;

    long veinSeed = positionHash(seed ^ 0xAE7L, cx, surfaceY, cz);
    boolean hasVeins = hashFloat(veinSeed) < 0.4f;

    double veinCenterProgress = hashFloat(positionHash(veinSeed, 1, 0, 0));
    double veinCenterHeight = hashFloat(positionHash(veinSeed, 0, 1, 0));

    for (int dy = 0; dy < height + BURY_DEPTH; dy++) {
      for (int dw = -halfWidth; dw <= halfWidth; dw++) {
        for (int dt = 0; dt < SLAB_THICKNESS; dt++) {

          double tiltOffset = (dy - BURY_DEPTH) * Math.sin(tilt);

          double worldDx = widthDx * dw + faceDx * (dt + tiltOffset);
          double worldDz = widthDz * dw + faceDz * (dt + tiltOffset);

          int bx = cx + (int) Math.round(worldDx);
          int bz = cz + (int) Math.round(worldDz);
          int by = startY + dy;

          BlockPos pos = new BlockPos(bx, by, bz);
          if (!isInWritableArea(pos, chunkPos)) continue;

          double heightProgress = (double) dy / (height + BURY_DEPTH);
          double widthFactor = 1.0 - heightProgress * 0.3;
          if (Math.abs(dw) > halfWidth * widthFactor) continue;

          long blockHash = positionHash(seed, bx, by, bz);
          float roll = hashFloat(blockHash);

          BlockState material;

          if (hasVeins) {
            double veinDistW = Math.abs((double) dw / halfWidth - (veinCenterProgress * 2.0 - 1.0));
            double veinDistH = Math.abs(heightProgress - veinCenterHeight);
            double veinDist = Math.sqrt(veinDistW * veinDistW + veinDistH * veinDistH);

            if (veinDist < 0.2) {

              if (roll < 0.3f) {
                material = BUDDING_AMETHYST;
              } else {
                material = AMETHYST;
              }
              world.setBlock(pos, material, 2);
              continue;
            } else if (veinDist < 0.35) {

              if (roll < 0.4f) {
                material = AMETHYST;
                world.setBlock(pos, material, 2);
                continue;
              }
            }
          }

          if (roll < 0.12f) {
            material = CRYING_OBSIDIAN;
          } else if (roll < 0.15f && dt == 0) {

            material = TINTED_GLASS;
          } else {
            material = OBSIDIAN;
          }

          world.setBlock(pos, material, 2);
        }
      }
    }

    for (int r = 0; r < 8; r++) {
      long rubbleHash = positionHash(seed ^ 0x4DBL, cx + r, surfaceY, cz + r);
      double rAngle = hashFloat(rubbleHash) * Math.PI * 2.0;
      int rDist = 2 + (int) (hashFloat(positionHash(rubbleHash, 0, 0, 0)) * (halfWidth + 3));
      int rx = cx + (int) Math.round(Math.cos(rAngle) * rDist);
      int rz = cz + (int) Math.round(Math.sin(rAngle) * rDist);
      int ry = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rx, rz);

      BlockPos rubblePos = new BlockPos(rx, ry, rz);
      if (isInWritableArea(rubblePos, chunkPos) && world.getBlockState(rubblePos).isAir()) {
        world.setBlock(rubblePos, hashFloat(rubbleHash) < 0.7f ? OBSIDIAN : CRYING_OBSIDIAN, 2);
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:obsidian_monoliths";
  }
}
