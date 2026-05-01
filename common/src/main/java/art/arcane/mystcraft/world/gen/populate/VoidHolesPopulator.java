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
 * Generates clean cylindrical shafts punched straight through terrain down to
 * bedrock level. Perfectly circular with smooth stone/crying obsidian rim at
 * the surface. Something drilled these out. The walls are unnervingly smooth
 * and the bottom is just... void darkness.
 */
public class VoidHolesPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.03f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_RADIUS = 3;
  private static final int MAX_RADIUS = 8;

  private static final BlockState AIR = Blocks.AIR.defaultBlockState();
  private static final BlockState SMOOTH_STONE = Blocks.SMOOTH_STONE.defaultBlockState();
  private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();
  private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
  private static final BlockState DEEPSLATE = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
  private static final BlockState TINTED_GLASS = Blocks.TINTED_GLASS.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public VoidHolesPopulator(long seed) {
    this(seed, null);
  }

  public VoidHolesPopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinBuildHeight() + 10) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
      generateVoidHole(world, chunkPos, x, surfaceY, z, radius);
    }
  }

  private void generateVoidHole(WorldGenLevel world, BlockPos chunkPos,
                                int cx, int surfaceY, int cz, int radius) {
    int radiusSq = radius * radius;
    int rimRadiusSq = (radius + 1) * (radius + 1);
    int bottomY = world.getMinBuildHeight() + 1;

    for (int dx = -(radius + 2); dx <= radius + 2; dx++) {
      for (int dz = -(radius + 2); dz <= radius + 2; dz++) {
        int distSq = dx * dx + dz * dz;
        int bx = cx + dx;
        int bz = cz + dz;

        if (distSq > radiusSq && distSq <= rimRadiusSq + 4) {
          for (int dy = 0; dy <= 1; dy++) {
            BlockPos rimPos = new BlockPos(bx, surfaceY + dy, bz);
            if (!isInWritableArea(rimPos, chunkPos)) {
              continue;
            }
            long rimHash = positionHash(seed, bx, surfaceY + dy, bz);
            float roll = hashFloat(rimHash);
            BlockState rimBlock;
            if (roll < 0.25f) {
              rimBlock = CRYING_OBSIDIAN;
            } else if (roll < 0.5f) {
              rimBlock = OBSIDIAN;
            } else if (roll < 0.75f) {
              rimBlock = DEEPSLATE;
            } else {
              rimBlock = SMOOTH_STONE;
            }
            world.setBlock(rimPos, rimBlock, 2);
          }

          if (distSq <= rimRadiusSq) {
            long glassHash = positionHash(seed ^ 0x6A55L, bx, surfaceY, bz);
            if (hashFloat(glassHash) < 0.15f) {
              BlockPos glassPos = new BlockPos(bx, surfaceY + 2, bz);
              if (isInWritableArea(glassPos, chunkPos)) {
                world.setBlock(glassPos, TINTED_GLASS, 2);
              }
            }
          }
        }
      }
    }

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        int distSq = dx * dx + dz * dz;
        if (distSq > radiusSq) {
          continue;
        }

        int bx = cx + dx;
        int bz = cz + dz;

        for (int y = surfaceY + 1; y >= bottomY; y--) {
          BlockPos pos = new BlockPos(bx, y, bz);
          if (!isInWritableArea(pos, chunkPos)) {
            continue;
          }

          boolean isEdge = (dx * dx + dz * dz) >= (radius - 1) * (radius - 1);
          if (isEdge) {
            long wallHash = positionHash(seed, bx, y, bz);
            float roll = hashFloat(wallHash);
            if (y > 0) {
              world.setBlock(pos, roll < 0.5f ? SMOOTH_STONE : DEEPSLATE, 2);
            } else {
              world.setBlock(pos, OBSIDIAN, 2);
            }
          } else {
            world.setBlock(pos, AIR, 2);
          }
        }
      }
    }

    for (int dx = -(radius - 2); dx <= radius - 2; dx++) {
      for (int dz = -(radius - 2); dz <= radius - 2; dz++) {
        int distSq = dx * dx + dz * dz;
        if (distSq > (radius - 2) * (radius - 2)) {
          continue;
        }
        int bx = cx + dx;
        int bz = cz + dz;
        long bottomHash = positionHash(seed ^ 0xB077L, bx, bottomY, bz);
        if (hashFloat(bottomHash) < 0.08f) {
          BlockPos glowPos = new BlockPos(bx, bottomY, bz);
          if (isInWritableArea(glowPos, chunkPos)) {
            world.setBlock(glowPos, Blocks.SHROOMLIGHT.defaultBlockState(), 2);
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:void_holes";
  }
}
