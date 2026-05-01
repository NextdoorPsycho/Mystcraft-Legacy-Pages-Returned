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
 * Sculk infestation that spreads across the surface in irregular veiny patches.
 * Replaces surface blocks with sculk, places sculk veins on exposed sides,
 * embeds sculk sensors and shriekers throughout. The corruption radiates
 * outward from a central catalyst point, growing weaker at the edges. Creates
 * an unsettling living-darkness feeling on the surface.
 */
public class SculkInfestationPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.04f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_RADIUS = 8;
  private static final int MAX_RADIUS = 24;

  private static final BlockState SCULK = Blocks.SCULK.defaultBlockState();
  private static final BlockState SCULK_VEIN = Blocks.SCULK_VEIN.defaultBlockState();
  private static final BlockState SCULK_CATALYST = Blocks.SCULK_CATALYST.defaultBlockState();
  private static final BlockState SCULK_SENSOR = Blocks.SCULK_SENSOR.defaultBlockState();
  private static final BlockState SCULK_SHRIEKER = Blocks.SCULK_SHRIEKER.defaultBlockState();
  private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public SculkInfestationPopulator(long seed) {
    this(seed, null);
  }

  public SculkInfestationPopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinBuildHeight() + 5 || surfaceY >= world.getMaxBuildHeight() - 5) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
      generateInfestation(world, chunkPos, x, surfaceY, z, radius);
    }
  }

  private void generateInfestation(WorldGenLevel world, BlockPos chunkPos,
                                   int cx, int surfaceY, int cz, int radius) {
    int radiusSq = radius * radius;

    BlockPos catalystPos = new BlockPos(cx, surfaceY, cz);
    if (isInWritableArea(catalystPos, chunkPos)) {
      world.setBlock(catalystPos, SCULK_CATALYST, 2);
    }

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        int distSq = dx * dx + dz * dz;
        if (distSq > radiusSq) {
          continue;
        }

        int bx = cx + dx;
        int bz = cz + dz;
        int bSurfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;

        if (bSurfaceY <= world.getMinBuildHeight() + 2 || bSurfaceY >= world.getMaxBuildHeight() - 2) {
          continue;
        }

        BlockPos surfacePos = new BlockPos(bx, bSurfaceY, bz);
        if (!isInWritableArea(surfacePos, chunkPos)) {
          continue;
        }

        double dist = Math.sqrt(distSq);
        double normalizedDist = dist / radius;
        long spreadHash = positionHash(seed, bx, bSurfaceY, bz);
        float spreadRoll = hashFloat(spreadHash);

        long veinHash = positionHash(seed ^ 0xBE11L, bx, 0, bz);
        float veinValue = hashFloat(veinHash);

        float threshold = (float) (normalizedDist * 0.8 + veinValue * 0.3);
        if (spreadRoll > threshold) {
          continue;
        }

        BlockState existing = world.getBlockState(surfacePos);
        if (!existing.isSolid() || existing.is(Blocks.BEDROCK)) {
          continue;
        }

        world.setBlock(surfacePos, SCULK, 2);

        int depth = 1 + (int) (hashFloat(positionHash(seed ^ 0xDE97L, bx, bSurfaceY, bz)) * 3);
        for (int dy = 1; dy <= depth; dy++) {
          BlockPos belowPos = new BlockPos(bx, bSurfaceY - dy, bz);
          if (isInWritableArea(belowPos, chunkPos)) {
            BlockState belowExisting = world.getBlockState(belowPos);
            if (belowExisting.isSolid() && !belowExisting.is(Blocks.BEDROCK)) {
              long belowHash = positionHash(seed, bx, bSurfaceY - dy, bz);
              if (hashFloat(belowHash) < 0.4f) {
                world.setBlock(belowPos, DEEPSLATE, 2);
              } else {
                world.setBlock(belowPos, SCULK, 2);
              }
            }
          }
        }

        BlockPos abovePos = surfacePos.above();
        if (isInWritableArea(abovePos, chunkPos) && world.getBlockState(abovePos).isAir()) {
          long veinPlaceHash = positionHash(seed ^ 0xBE14L, bx, bSurfaceY + 1, bz);
          if (hashFloat(veinPlaceHash) < 0.35f) {
            world.setBlock(abovePos, SCULK_VEIN, 2);
          }
        }

        if (normalizedDist < 0.7) {
          long sensorHash = positionHash(seed ^ 0x5E45L, bx, bSurfaceY, bz);
          float sensorRoll = hashFloat(sensorHash);
          if (sensorRoll < 0.03f) {
            BlockPos sensorPos = surfacePos.above();
            if (isInWritableArea(sensorPos, chunkPos) && world.getBlockState(sensorPos).isAir()) {
              world.setBlock(sensorPos, SCULK_SENSOR, 2);
            }
          }
        }

        if (normalizedDist < 0.3) {
          long shriekerHash = positionHash(seed ^ 0x5471L, bx, bSurfaceY, bz);
          float shriekerRoll = hashFloat(shriekerHash);
          if (shriekerRoll < 0.015f) {
            BlockPos shriekerPos = surfacePos.above();
            if (isInWritableArea(shriekerPos, chunkPos) && world.getBlockState(shriekerPos).isAir()) {
              world.setBlock(shriekerPos, SCULK_SHRIEKER, 2);
            }
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:sculk_infestation";
  }
}
