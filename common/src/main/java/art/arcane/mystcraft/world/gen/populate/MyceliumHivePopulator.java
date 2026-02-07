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
 * Generates a honeycomb-like fungal hive structure of interconnected
 * hexagonal chambers. Made of mushroom blocks, mycelium, and shroomlight
 * with connecting tunnels between cells. The structure emerges from
 * underground and breaches the surface, revealing the alien fungal
 * architecture inside.
 */
public class MyceliumHivePopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.025f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_CELLS = 5;
  private static final int MAX_CELLS = 12;
  private static final int MIN_CELL_RADIUS = 4;
  private static final int MAX_CELL_RADIUS = 7;

  private static final BlockState BROWN_MUSHROOM_BLOCK = Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
  private static final BlockState RED_MUSHROOM_BLOCK = Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
  private static final BlockState MUSHROOM_STEM = Blocks.MUSHROOM_STEM.defaultBlockState();
  private static final BlockState MYCELIUM = Blocks.MYCELIUM.defaultBlockState();
  private static final BlockState SHROOMLIGHT = Blocks.SHROOMLIGHT.defaultBlockState();
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();
  private static final BlockState BROWN_MUSHROOM = Blocks.BROWN_MUSHROOM.defaultBlockState();
  private static final BlockState RED_MUSHROOM = Blocks.RED_MUSHROOM.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public MyceliumHivePopulator(long seed) {
    this(seed, null);
  }

  public MyceliumHivePopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinBuildHeight() + 15 || surfaceY >= world.getMaxBuildHeight() - 20) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int cellCount = MIN_CELLS + random.nextInt(MAX_CELLS - MIN_CELLS + 1);
      generateHive(world, chunkPos, random, x, surfaceY, z, cellCount);
    }
  }

  private void generateHive(WorldGenLevel world, BlockPos chunkPos, RandomSource random,
                             int cx, int surfaceY, int cz, int cellCount) {
    // Generate cell centers in a rough hex grid pattern
    int[][] cellCenters = new int[cellCount][3]; // x, y, z
    int[] cellRadii = new int[cellCount];

    // First cell at center, below surface
    cellCenters[0] = new int[]{cx, surfaceY - 4, cz};
    cellRadii[0] = MIN_CELL_RADIUS + (int) (hashFloat(positionHash(seed, cx, surfaceY, cz))
        * (MAX_CELL_RADIUS - MIN_CELL_RADIUS + 1));

    // Subsequent cells branch outward
    for (int i = 1; i < cellCount; i++) {
      int parentIdx = random.nextInt(i); // Connect to a random existing cell
      int[] parent = cellCenters[parentIdx];
      int parentR = cellRadii[parentIdx];

      double angle = random.nextDouble() * Math.PI * 2.0;
      int cellRadius = MIN_CELL_RADIUS + random.nextInt(MAX_CELL_RADIUS - MIN_CELL_RADIUS + 1);
      int spacing = parentR + cellRadius + 2; // Cells touch/overlap slightly

      int newX = parent[0] + (int) Math.round(Math.cos(angle) * spacing);
      int newZ = parent[2] + (int) Math.round(Math.sin(angle) * spacing);
      // Slight vertical offset: some cells above, some below
      int newY = parent[1] + random.nextInt(5) - 2;
      newY = Math.max(world.getMinBuildHeight() + cellRadius + 2, newY);

      cellCenters[i] = new int[]{newX, newY, newZ};
      cellRadii[i] = cellRadius;

      // Generate tunnel connecting to parent
      generateTunnel(world, chunkPos, parent[0], parent[1], parent[2],
          newX, newY, newZ);
    }

    // Generate each cell chamber
    for (int i = 0; i < cellCount; i++) {
      generateCell(world, chunkPos, cellCenters[i][0], cellCenters[i][1],
          cellCenters[i][2], cellRadii[i]);
    }

    // Surface breach: mycelium patch and mushrooms where hive nears surface
    for (int dx = -12; dx <= 12; dx++) {
      for (int dz = -12; dz <= 12; dz++) {
        int distSq = dx * dx + dz * dz;
        if (distSq > 144) continue;
        int bx = cx + dx;
        int bz = cz + dz;
        int bSurfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;

        BlockPos surfacePos = new BlockPos(bx, bSurfaceY, bz);
        if (!isInWritableArea(surfacePos, chunkPos)) continue;

        long mycelHash = positionHash(seed ^ 0x4E3EL, bx, bSurfaceY, bz);
        double normalizedDist = Math.sqrt(distSq) / 12.0;
        if (hashFloat(mycelHash) < 0.6 - normalizedDist * 0.5) {
          BlockState existing = world.getBlockState(surfacePos);
          if (existing.is(Blocks.GRASS_BLOCK) || existing.is(BlockTags.DIRT)) {
            world.setBlock(surfacePos, MYCELIUM, 2);

            // Small mushrooms on surface
            BlockPos mushPos = surfacePos.above();
            if (isInWritableArea(mushPos, chunkPos) && world.getBlockState(mushPos).isAir()) {
              long mushHash = positionHash(seed ^ 0xF77AL, bx, bSurfaceY + 1, bz);
              float mushRoll = hashFloat(mushHash);
              if (mushRoll < 0.08f) {
                world.setBlock(mushPos, mushRoll < 0.04f ? RED_MUSHROOM : BROWN_MUSHROOM, 2);
              }
            }
          }
        }
      }
    }
  }

  private void generateCell(WorldGenLevel world, BlockPos chunkPos,
                             int cx, int cy, int cz, int radius) {
    int radiusSq = radius * radius;
    int shellThickness = 2;
    int innerRadiusSq = (radius - shellThickness) * (radius - shellThickness);

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dy = -radius; dy <= radius; dy++) {
        for (int dz = -radius; dz <= radius; dz++) {
          int distSq = dx * dx + dy * dy + dz * dz;
          if (distSq > radiusSq) continue;

          int bx = cx + dx;
          int by = cy + dy;
          int bz = cz + dz;
          BlockPos pos = new BlockPos(bx, by, bz);
          if (!isInWritableArea(pos, chunkPos)) continue;

          if (distSq <= innerRadiusSq) {
            // Interior: air with occasional shroomlight
            long innerHash = positionHash(seed, bx, by, bz);
            if (hashFloat(innerHash) < 0.015f) {
              world.setBlock(pos, SHROOMLIGHT, 2);
            } else {
              world.setBlock(pos, AIR, 2);
            }
          } else {
            // Shell: mushroom blocks and stems
            long shellHash = positionHash(seed, bx, by, bz);
            float shellRoll = hashFloat(shellHash);
            if (shellRoll < 0.1f) {
              world.setBlock(pos, SHROOMLIGHT, 2);
            } else if (shellRoll < 0.35f) {
              world.setBlock(pos, MUSHROOM_STEM, 2);
            } else if (shellRoll < 0.65f) {
              world.setBlock(pos, BROWN_MUSHROOM_BLOCK, 2);
            } else {
              world.setBlock(pos, RED_MUSHROOM_BLOCK, 2);
            }
          }
        }
      }
    }

    // Floor: mycelium
    for (int dx = -(radius - shellThickness); dx <= radius - shellThickness; dx++) {
      for (int dz = -(radius - shellThickness); dz <= radius - shellThickness; dz++) {
        if (dx * dx + dz * dz > innerRadiusSq) continue;
        BlockPos floorPos = new BlockPos(cx + dx, cy - radius + shellThickness, cz + dz);
        if (isInWritableArea(floorPos, chunkPos)) {
          world.setBlock(floorPos, MYCELIUM, 2);
        }
      }
    }
  }

  private void generateTunnel(WorldGenLevel world, BlockPos chunkPos,
                               int x1, int y1, int z1, int x2, int y2, int z2) {
    int tunnelRadius = 2;
    double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
    int steps = (int) Math.ceil(dist);

    for (int step = 0; step <= steps; step++) {
      double t = (double) step / steps;
      int tx = x1 + (int) Math.round((x2 - x1) * t);
      int ty = y1 + (int) Math.round((y2 - y1) * t);
      int tz = z1 + (int) Math.round((z2 - z1) * t);

      // Carve a circular tunnel cross-section
      for (int dx = -tunnelRadius; dx <= tunnelRadius; dx++) {
        for (int dy = -tunnelRadius; dy <= tunnelRadius; dy++) {
          for (int dz = -tunnelRadius; dz <= tunnelRadius; dz++) {
            if (dx * dx + dy * dy + dz * dz > tunnelRadius * tunnelRadius) continue;
            BlockPos pos = new BlockPos(tx + dx, ty + dy, tz + dz);
            if (isInWritableArea(pos, chunkPos)) {
              // Edge of tunnel: mushroom stem walls
              if (dx * dx + dy * dy + dz * dz >= (tunnelRadius - 1) * (tunnelRadius - 1)) {
                world.setBlock(pos, MUSHROOM_STEM, 2);
              } else {
                world.setBlock(pos, AIR, 2);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:mycelium_hive";
  }
}
