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
 * Generates terrain formations shaped like ocean waves frozen mid-crash. Made
 * of ice, packed ice, and blue ice with powder snow spray at the crest. Sea
 * lanterns embedded inside create an eerie inner glow. The waves curl forward
 * as if time stopped in the middle of a storm.
 */
public class FrozenWavesPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.035f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_HEIGHT = 12;
  private static final int MAX_HEIGHT = 28;
  private static final int MIN_LENGTH = 20;
  private static final int MAX_LENGTH = 50;

  private static final BlockState ICE = Blocks.ICE.defaultBlockState();
  private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
  private static final BlockState BLUE_ICE = Blocks.BLUE_ICE.defaultBlockState();
  private static final BlockState POWDER_SNOW = Blocks.POWDER_SNOW.defaultBlockState();
  private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
  private static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.defaultBlockState();
  private static final BlockState PRISMARINE = Blocks.PRISMARINE.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public FrozenWavesPopulator(long seed) {
    this(seed, null);
  }

  public FrozenWavesPopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinBuildHeight() + 5 || surfaceY >= world.getMaxBuildHeight() - MAX_HEIGHT - 10) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int waveHeight = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
      int waveLength = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
      double angle = random.nextDouble() * Math.PI * 2.0;

      generateWave(world, chunkPos, x, surfaceY, z, waveHeight, waveLength, angle);
    }
  }

  private void generateWave(WorldGenLevel world, BlockPos chunkPos,
                            int startX, int surfaceY, int startZ,
                            int waveHeight, int waveLength, double angle) {
    double dx = Math.cos(angle);
    double dz = Math.sin(angle);

    double crossX = -dz;
    double crossZ = dx;

    int thickness = 4 + waveHeight / 6;

    for (int step = 0; step < waveLength; step++) {
      int bx = startX + (int) Math.round(dx * step);
      int bz = startZ + (int) Math.round(dz * step);

      double lengthProgress = (double) step / waveLength;
      double heightMultiplier = Math.sin(lengthProgress * Math.PI);
      int localWaveHeight = (int) (waveHeight * heightMultiplier);
      if (localWaveHeight < 3) continue;

      for (int crossStep = -2; crossStep < thickness; crossStep++) {
        int wx = bx + (int) Math.round(crossX * crossStep);
        int wz = bz + (int) Math.round(crossZ * crossStep);

        double crossProgress = (double) (crossStep + 2) / (thickness + 2);
        int columnHeight;

        if (crossProgress < 0.3) {

          columnHeight = (int) (localWaveHeight * crossProgress / 0.3);
        } else if (crossProgress < 0.7) {

          columnHeight = localWaveHeight;
        } else {

          double curlProgress = (crossProgress - 0.7) / 0.3;
          columnHeight = (int) (localWaveHeight * (1.0 - curlProgress * 0.6));
        }

        int overhangStart = -1;
        if (crossProgress > 0.6) {
          overhangStart = (int) (localWaveHeight * 0.7);
        }

        for (int dy = 0; dy < columnHeight; dy++) {
          int wy = surfaceY + dy;
          BlockPos pos = new BlockPos(wx, wy, wz);
          if (!isInWritableArea(pos, chunkPos)) continue;

          long blockHash = positionHash(seed, wx, wy, wz);
          float roll = hashFloat(blockHash);
          BlockState material;

          if (roll < 0.02f) {
            material = SEA_LANTERN;
          } else if (roll < 0.05f) {
            material = PRISMARINE;
          } else if (dy < columnHeight * 0.3) {

            material = BLUE_ICE;
          } else if (dy < columnHeight * 0.7) {

            material = roll < 0.6f ? PACKED_ICE : BLUE_ICE;
          } else {

            material = roll < 0.5f ? ICE : PACKED_ICE;
          }

          world.setBlock(pos, material, 2);
        }

        if (overhangStart >= 0 && crossProgress > 0.7) {
          double curlAngle = (crossProgress - 0.7) / 0.3 * Math.PI * 0.6;
          int curlOffsetY = (int) (Math.cos(curlAngle) * localWaveHeight * 0.3);
          int curlOffsetCross = (int) (Math.sin(curlAngle) * localWaveHeight * 0.4);
          int curlX = bx + (int) Math.round(crossX * (crossStep + curlOffsetCross));
          int curlZ = bz + (int) Math.round(crossZ * (crossStep + curlOffsetCross));
          int curlY = surfaceY + overhangStart + curlOffsetY;

          for (int curl = 0; curl < 2; curl++) {
            BlockPos curlPos = new BlockPos(curlX, curlY + curl, curlZ);
            if (isInWritableArea(curlPos, chunkPos)) {
              long curlHash = positionHash(seed ^ 0xCE1L, curlX, curlY + curl, curlZ);
              world.setBlock(curlPos, hashFloat(curlHash) < 0.5f ? ICE : PACKED_ICE, 2);
            }
          }
        }
      }

      if (lengthProgress > 0.2 && lengthProgress < 0.8) {
        int sprayCount = 2 + (int) (hashFloat(positionHash(seed ^ 0x5B4AL, bx, 0, bz)) * 4);
        for (int s = 0; s < sprayCount; s++) {
          long sprayHash = positionHash(seed ^ 0x5B4AL, bx + s, localWaveHeight, bz + s);
          int sx = bx + (int) Math.round(crossX * (thickness + 1 + hashFloat(sprayHash) * 5));
          int sz = bz + (int) Math.round(crossZ * (thickness + 1 + hashFloat(sprayHash) * 5));
          int sy = surfaceY + localWaveHeight - (int) (hashFloat(positionHash(sprayHash, 0, 0, 0)) * 4);

          BlockPos sprayPos = new BlockPos(sx, sy, sz);
          if (isInWritableArea(sprayPos, chunkPos) && world.getBlockState(sprayPos).isAir()) {
            world.setBlock(sprayPos, hashFloat(sprayHash) < 0.4f ? POWDER_SNOW : SNOW_BLOCK, 2);
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:frozen_waves";
  }
}
