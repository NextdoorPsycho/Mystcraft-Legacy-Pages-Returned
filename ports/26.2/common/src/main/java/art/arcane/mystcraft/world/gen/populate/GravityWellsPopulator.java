package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Generates inverted terrain bubbles - hollow spheres where the ground curves
 * upward into enclosed chambers with grass growing on the inside surface.
 * Includes internal vegetation, glowstone light clusters, and vine curtains
 * hanging inward. Truly alien geometry.
 */
public class GravityWellsPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.03f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_RADIUS = 12;
  private static final int MAX_RADIUS = 28;
  private static final int SHELL_THICKNESS = 4;

  private static final BlockState STONE = Blocks.STONE.defaultBlockState();
  private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
  private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
  private static final BlockState GLOWSTONE = Blocks.GLOWSTONE.defaultBlockState();
  private static final BlockState VINE = Blocks.VINE.defaultBlockState();
  private static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();
  private static final BlockState WATER = Blocks.WATER.defaultBlockState();
  private static final BlockState CLAY = Blocks.CLAY.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public GravityWellsPopulator(long seed) {
    this(seed, null);
  }

  public GravityWellsPopulator(long seed, JsonObject params) {
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
      int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);

      int centerY = surfaceY + radius - (radius / 4);

      if (centerY - radius <= world.getMinY() + 2
          || centerY + radius >= world.getMaxY() - 2) {
        continue;
      }

      generateWell(world, chunkPos, x, centerY, z, radius);
    }
  }

  private void generateWell(WorldGenLevel world, BlockPos chunkPos,
                            int cx, int cy, int cz, int radius) {
    int radiusSq = radius * radius;
    int innerRadius = radius - SHELL_THICKNESS;
    int innerRadiusSq = innerRadius * innerRadius;

    int pondRadius = Math.max(2, innerRadius / 3);
    int pondRadiusSq = pondRadius * pondRadius;
    int pondY = cy - innerRadius + 1;

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dy = -radius; dy <= radius; dy++) {
        for (int dz = -radius; dz <= radius; dz++) {
          int bx = cx + dx;
          int by = cy + dy;
          int bz = cz + dz;

          BlockPos pos = new BlockPos(bx, by, bz);
          if (!isInWritableArea(pos, chunkPos)) {
            continue;
          }

          double distSq = (double) dx * dx + (double) dy * dy + (double) dz * dz;

          if (distSq > radiusSq) {

            continue;
          }

          if (distSq <= innerRadiusSq) {

            if (by <= pondY && (dx * dx + dz * dz) <= pondRadiusSq) {
              if (by == pondY) {
                world.setBlock(pos, WATER, 2);
              } else if (by == pondY - 1) {
                world.setBlock(pos, CLAY, 2);
              }
              continue;
            }

            BlockState existing = world.getBlockState(pos);
            if (!existing.isAir()) {
              world.setBlock(pos, AIR, 2);
            }

            if (dy > innerRadius / 2) {
              long vineHash = positionHash(seed ^ 0xB1E5L, bx, by, bz);
              if (hashFloat(vineHash) < 0.02f) {

                for (int vy = 0; vy < 3 + (int) (hashFloat(positionHash(vineHash, bx, by, bz)) * 5); vy++) {
                  BlockPos vinePos = new BlockPos(bx, by - vy, bz);
                  if (isInWritableArea(vinePos, chunkPos)) {
                    double vineDistSq = (double) dx * dx + (double) (dy - vy) * (dy - vy) + (double) dz * dz;
                    if (vineDistSq < innerRadiusSq) {
                      world.setBlock(vinePos, VINE, 2);
                    }
                  }
                }
              }
            }
            continue;
          }

          long blockHash = positionHash(seed, bx, by, bz);
          float roll = hashFloat(blockHash);

          if (distSq <= (innerRadius + 1.5) * (innerRadius + 1.5)) {

            if (roll < 0.04f) {
              world.setBlock(pos, GLOWSTONE, 2);
            } else if (roll < 0.12f) {
              world.setBlock(pos, MOSS, 2);
            } else if (dy < 0) {

              world.setBlock(pos, GRASS, 2);
            } else {
              world.setBlock(pos, DIRT, 2);
            }
          } else if (distSq > (radius - 1.5) * (radius - 1.5)) {

            if (roll < 0.08f) {
              world.setBlock(pos, MOSS, 2);
            } else {
              world.setBlock(pos, STONE, 2);
            }
          } else {

            if (distSq < (innerRadius + 2.5) * (innerRadius + 2.5)) {
              world.setBlock(pos, DIRT, 2);
            } else {
              world.setBlock(pos, STONE, 2);
            }
          }
        }
      }
    }

    long entranceHash = positionHash(seed ^ 0xE477L, cx, cy, cz);
    double entranceAngle = hashFloat(entranceHash) * Math.PI * 2.0;
    int tunnelDx = (int) Math.round(Math.cos(entranceAngle));
    int tunnelDz = (int) Math.round(Math.sin(entranceAngle));

    for (int step = 0; step <= radius + 2; step++) {
      int tx = cx + tunnelDx * step;
      int tz = cz + tunnelDz * step;
      int ty = cy - innerRadius + 2;
      for (int tdx = -1; tdx <= 1; tdx++) {
        for (int tdy = 0; tdy <= 2; tdy++) {
          BlockPos tunnelPos = new BlockPos(tx + (tunnelDz != 0 ? tdx : 0), ty + tdy,
              tz + (tunnelDx != 0 ? tdx : 0));
          if (isInWritableArea(tunnelPos, chunkPos)) {
            world.setBlock(tunnelPos, AIR, 2);
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:gravity_wells";
  }
}
