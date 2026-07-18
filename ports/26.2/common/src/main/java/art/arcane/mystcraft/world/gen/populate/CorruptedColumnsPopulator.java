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
 * Corrupted columns populator that generates ruined architectural columns of
 * mixed stone types, resembling a collapsed ancient structure. Clusters of 2-4
 * pillars with 3x3 base and capital plates, breakage with scattered rubble, and
 * optional iron bar railings between intact columns. Single-chunk populator
 * (max cluster radius ~8 blocks, fits within chunk).
 */
public class CorruptedColumnsPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.06f;
  private static final int DEFAULT_COUNT = 1;
  private static final int DEFAULT_MIN_COLUMNS = 2;
  private static final int DEFAULT_MAX_COLUMNS = 4;
  private static final int DEFAULT_MIN_HEIGHT = 12;
  private static final int DEFAULT_MAX_HEIGHT = 25;
  private static final int DEFAULT_CLUSTER_RADIUS = 8;
  private static final int DEFAULT_BURY_DEPTH = 2;
  private static final float DEFAULT_BREAK_CHANCE = 0.60f;
  private static final float DEFAULT_RAILING_DISTANCE = 4.0f;

  private final long seed;
  private final float spawnChance;
  private final int count;
  private final int minColumns;
  private final int maxColumns;
  private final int minHeight;
  private final int maxHeight;
  private final int clusterRadius;

  public CorruptedColumnsPopulator(long seed) {
    this(seed, null);
  }

  public CorruptedColumnsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
    this.minColumns = Math.max(1, PopulatorConfig.getInt(params, "min_columns", DEFAULT_MIN_COLUMNS));
    this.maxColumns = Math.max(this.minColumns, PopulatorConfig.getInt(params, "max_columns", DEFAULT_MAX_COLUMNS));
    this.minHeight = Math.max(3, PopulatorConfig.getInt(params, "min_height", DEFAULT_MIN_HEIGHT));
    this.maxHeight = Math.max(this.minHeight, PopulatorConfig.getInt(params, "max_height", DEFAULT_MAX_HEIGHT));
    this.clusterRadius = Math.max(1, PopulatorConfig.getInt(params, "cluster_radius", DEFAULT_CLUSTER_RADIUS));
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
    for (int i = 0; i < count; i++) {
      if (random.nextFloat() >= spawnChance) {
        continue;
      }

      int centerX = chunkPos.getX() + random.nextInt(16);
      int centerZ = chunkPos.getZ() + random.nextInt(16);
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ) - 1;

      if (surfaceY <= world.getMinY() + 1 || surfaceY >= world.getMaxY() - 30) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(centerX, surfaceY, centerZ));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int columnCount = minColumns + random.nextInt(maxColumns - minColumns + 1);
      generateColumnCluster(world, random, chunkPos, centerX, surfaceY, centerZ, columnCount);
    }
  }

  private void generateColumnCluster(WorldGenLevel world, RandomSource random, BlockPos chunkPos,
                                     int clusterCenterX, int clusterSurfaceY, int clusterCenterZ,
                                     int columnCount) {
    int[] columnX = new int[columnCount];
    int[] columnZ = new int[columnCount];
    int[] columnSurfaceY = new int[columnCount];
    int[] columnFullHeight = new int[columnCount];
    boolean[] columnBroken = new boolean[columnCount];
    int[] columnActualHeight = new int[columnCount];
    BlockState[] columnMaterial = new BlockState[columnCount];

    for (int c = 0; c < columnCount; c++) {
      int offsetX = random.nextInt(clusterRadius * 2 + 1) - clusterRadius;
      int offsetZ = random.nextInt(clusterRadius * 2 + 1) - clusterRadius;
      columnX[c] = clusterCenterX + offsetX;
      columnZ[c] = clusterCenterZ + offsetZ;

      int colSurfaceY = isInWritableColumn(columnX[c], columnZ[c], chunkPos)
          ? world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columnX[c], columnZ[c]) - 1
          : Integer.MIN_VALUE;
      columnSurfaceY[c] = colSurfaceY;

      int fullHeight = minHeight + random.nextInt(maxHeight - minHeight + 1);
      columnFullHeight[c] = fullHeight;

      boolean broken = random.nextFloat() < DEFAULT_BREAK_CHANCE;
      columnBroken[c] = broken;

      if (broken) {

        float breakFraction = 0.3f + random.nextFloat() * 0.4f;
        columnActualHeight[c] = Math.max(2, (int) (fullHeight * breakFraction));
      } else {
        columnActualHeight[c] = fullHeight;
      }

      columnMaterial[c] = getColumnMaterial(random);
    }

    for (int c = 0; c < columnCount; c++) {
      if (columnSurfaceY[c] == Integer.MIN_VALUE) {
        continue;
      }
      BlockState ground = world.getBlockState(new BlockPos(columnX[c], columnSurfaceY[c], columnZ[c]));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }
      if (columnSurfaceY[c] <= world.getMinY() + 1 || columnSurfaceY[c] >= world.getMaxY() - 30) {
        continue;
      }

      int baseY = columnSurfaceY[c] - DEFAULT_BURY_DEPTH;
      BlockState material = columnMaterial[c];
      int actualHeight = columnActualHeight[c];

      placeBasePlate(world, chunkPos, columnX[c], baseY, columnZ[c], material);

      for (int dy = 1; dy < actualHeight; dy++) {
        BlockPos pillarPos = new BlockPos(columnX[c], baseY + dy, columnZ[c]);
        if (isInWritableArea(pillarPos, chunkPos)) {
          world.setBlock(pillarPos, material, 2);
        }
      }

      if (columnBroken[c]) {

        placeRubble(world, chunkPos, columnX[c], columnSurfaceY[c], columnZ[c], material, random);
      } else {

        int topY = baseY + actualHeight;
        placeCapitalPlate(world, chunkPos, columnX[c], topY, columnZ[c], material);
      }
    }

    for (int a = 0; a < columnCount; a++) {
      if (columnBroken[a] || columnSurfaceY[a] == Integer.MIN_VALUE) {
        continue;
      }
      for (int b = a + 1; b < columnCount; b++) {
        if (columnBroken[b] || columnSurfaceY[b] == Integer.MIN_VALUE) {
          continue;
        }
        double dist = Math.sqrt(
            (double) (columnX[a] - columnX[b]) * (columnX[a] - columnX[b]) +
                (double) (columnZ[a] - columnZ[b]) * (columnZ[a] - columnZ[b])
        );
        if (dist <= DEFAULT_RAILING_DISTANCE && dist >= 2.0) {
          placeRailing(world, chunkPos, columnX[a], columnZ[a], columnSurfaceY[a],
              columnX[b], columnZ[b], columnSurfaceY[b],
              Math.min(columnActualHeight[a], columnActualHeight[b]));
        }
      }
    }
  }

  private void placeBasePlate(WorldGenLevel world, BlockPos chunkPos,
                              int centerX, int y, int centerZ, BlockState material) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
        if (isInWritableArea(pos, chunkPos)) {
          world.setBlock(pos, material, 2);
        }
      }
    }
  }

  private void placeCapitalPlate(WorldGenLevel world, BlockPos chunkPos,
                                 int centerX, int y, int centerZ, BlockState material) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
        if (isInWritableArea(pos, chunkPos)) {
          world.setBlock(pos, material, 2);
        }
      }
    }
  }

  private void placeRubble(WorldGenLevel world, BlockPos chunkPos,
                           int centerX, int surfaceY, int centerZ,
                           BlockState columnMaterial, RandomSource random) {
    int rubbleRadius = 3 + random.nextInt(3);
    int rubbleCount = 4 + random.nextInt(5);

    for (int r = 0; r < rubbleCount; r++) {
      int dx = random.nextInt(rubbleRadius * 2 + 1) - rubbleRadius;
      int dz = random.nextInt(rubbleRadius * 2 + 1) - rubbleRadius;
      int rx = centerX + dx;
      int rz = centerZ + dz;

      if (!isInWritableColumn(rx, rz, chunkPos)) {
        continue;
      }
      int rSurfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rx, rz) - 1;
      if (rSurfaceY <= world.getMinY() + 1) {
        continue;
      }

      BlockState rubbleMaterial = getRubbleMaterial(columnMaterial, random);
      int rubbleHeight = 1 + random.nextInt(3);

      for (int dy = 1; dy <= rubbleHeight; dy++) {
        BlockPos rubblePos = new BlockPos(rx, rSurfaceY + dy, rz);
        if (isInWritableArea(rubblePos, chunkPos) && world.getBlockState(rubblePos).isAir()) {
          world.setBlock(rubblePos, rubbleMaterial, 2);
        }
      }
    }
  }

  private void placeRailing(WorldGenLevel world, BlockPos chunkPos,
                            int x1, int z1, int surfaceY1,
                            int x2, int z2, int surfaceY2,
                            int minColumnHeight) {

    int railY = Math.min(surfaceY1, surfaceY2) + (int) (minColumnHeight * 0.6);
    BlockState ironBars = Blocks.IRON_BARS.defaultBlockState();

    int dx = Math.abs(x2 - x1);
    int dz = Math.abs(z2 - z1);
    int sx = x1 < x2 ? 1 : -1;
    int sz = z1 < z2 ? 1 : -1;
    int err = dx - dz;

    int cx = x1;
    int cz = z1;

    while (true) {

      if (!(cx == x1 && cz == z1) && !(cx == x2 && cz == z2)) {
        BlockPos railPos = new BlockPos(cx, railY, cz);
        if (isInWritableArea(railPos, chunkPos) && world.getBlockState(railPos).isAir()) {
          world.setBlock(railPos, ironBars, 2);
        }
      }

      if (cx == x2 && cz == z2) {
        break;
      }

      int e2 = 2 * err;
      if (e2 > -dz) {
        err -= dz;
        cx += sx;
      }
      if (e2 < dx) {
        err += dx;
        cz += sz;
      }
    }
  }

  private BlockState getColumnMaterial(RandomSource random) {
    int choice = random.nextInt(4);
    return switch (choice) {
      case 0 -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
      case 1 -> Blocks.POLISHED_ANDESITE.defaultBlockState();
      case 2 -> Blocks.QUARTZ_PILLAR.defaultBlockState();
      default -> Blocks.SMOOTH_STONE.defaultBlockState();
    };
  }

  private BlockState getRubbleMaterial(BlockState columnMaterial, RandomSource random) {
    int choice = random.nextInt(3);
    return switch (choice) {
      case 0 -> columnMaterial;
      case 1 -> Blocks.GRAVEL.defaultBlockState();
      default -> Blocks.COBBLESTONE.defaultBlockState();
    };
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:corrupted_columns";
  }
}
