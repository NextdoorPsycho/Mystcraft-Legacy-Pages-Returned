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
 * Shattered Grid populator that generates a geometric grid pattern of thin walls
 * at regular intervals, like a broken matrix or simulation. Grid lines are
 * partially missing to create the "shattered" look, with intersections forming
 * short columns and edges forming wall segments.
 *
 * This is a simple single-chunk populator. When triggered, it applies the grid
 * pattern across the entire chunk using deterministic position hashing so the
 * grid aligns seamlessly across chunk boundaries.
 */
public class ShatteredGridPopulator implements IPopulate {

  private static final int DEFAULT_COUNT = 1;
  private static final float DEFAULT_SPAWN_CHANCE = 0.05f;
  private static final int DEFAULT_MIN_GRID_SPACING = 4;
  private static final int DEFAULT_MAX_GRID_SPACING = 6;
  private static final float INTERSECTION_PLACE_CHANCE = 0.70f;
  private static final float EDGE_PLACE_CHANCE = 0.50f;
  private static final int MIN_COLUMN_HEIGHT = 1;
  private static final int MAX_COLUMN_HEIGHT = 3;

  private static final BlockState[] MATERIALS = new BlockState[]{
      Blocks.IRON_BARS.defaultBlockState(),
      Blocks.GLASS_PANE.defaultBlockState(),
      Blocks.CHAIN.defaultBlockState(),
      Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.defaultBlockState()
  };

  private final long seed;
  private final int count;
  private final float spawnChance;
  private final int minGridSpacing;
  private final int maxGridSpacing;

  public ShatteredGridPopulator(long seed) {
    this(seed, null);
  }

  public ShatteredGridPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.minGridSpacing = Math.max(2, PopulatorConfig.getInt(params, "min_grid_spacing", DEFAULT_MIN_GRID_SPACING));
    this.maxGridSpacing = Math.max(this.minGridSpacing, PopulatorConfig.getInt(params, "max_grid_spacing", DEFAULT_MAX_GRID_SPACING));
  }

  /**
   * Position-deterministic hash. Same position always produces the same value
   * regardless of which chunk is being populated.
   */
  private static long positionHash(long seed, int x, int y, int z) {
    long h = seed;
    h ^= (long) x * 73856093L;
    h ^= (long) y * 19349663L;
    h ^= (long) z * 83492791L;
    h = h * 6364136223846793005L + 1442695040888963407L;
    return h;
  }

  /**
   * Returns a deterministic float in [0, 1) from a position hash.
   */
  private static float hashToFloat(long hash) {
    return (float) ((hash >>> 16) & 0xFFFFL) / 65536.0f;
  }

  /**
   * Selects a material deterministically based on position hash.
   */
  private static BlockState getMaterial(long hash) {
    int index = (int) ((hash >>> 32) & 0x3);
    return MATERIALS[index];
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    int chunkMinX = chunkPos.getX();
    int chunkMaxX = chunkMinX + 15;
    int chunkMinZ = chunkPos.getZ();
    int chunkMaxZ = chunkMinZ + 15;

    for (int i = 0; i < count; i++) {
      if (random.nextFloat() >= spawnChance) {
        continue;
      }

      // Derive grid spacing deterministically from seed + chunk coordinates
      int chunkX = chunkPos.getX() >> 4;
      int chunkZ = chunkPos.getZ() >> 4;
      long spacingSeed = positionHash(seed, chunkX, i, chunkZ);
      int spacingRange = maxGridSpacing - minGridSpacing + 1;
      int gridSpacing = minGridSpacing + (int) (((spacingSeed >>> 48) & 0xFFFFL) % spacingRange);

      generateGrid(world, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, gridSpacing, chunkPos);
    }
  }

  private void generateGrid(WorldGenLevel world,
                             int chunkMinX, int chunkMaxX,
                             int chunkMinZ, int chunkMaxZ,
                             int gridSpacing, BlockPos chunkPos) {
    // Iterate all block columns in this chunk and check if they lie on a grid line
    for (int bx = chunkMinX; bx <= chunkMaxX; bx++) {
      for (int bz = chunkMinZ; bz <= chunkMaxZ; bz++) {
        // Determine alignment with the global grid
        // Use Math.floorMod for consistent behavior with negative coordinates
        int modX = Math.floorMod(bx, gridSpacing);
        int modZ = Math.floorMod(bz, gridSpacing);

        boolean onXLine = modX == 0;
        boolean onZLine = modZ == 0;

        if (!onXLine && !onZLine) {
          continue;
        }

        boolean isIntersection = onXLine && onZLine;

        // Get surface height for this position
        int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
        if (surfaceY <= world.getMinBuildHeight() + 1 || surfaceY >= world.getMaxBuildHeight() - 2) {
          continue;
        }

        // Use position hash to determine if this segment is "shattered" (missing)
        long placeHash = positionHash(seed ^ 0x54A77L, bx, surfaceY, bz);
        float placeChance = hashToFloat(placeHash);

        if (isIntersection) {
          // Intersections have a higher chance of surviving the shatter
          if (placeChance >= INTERSECTION_PLACE_CHANCE) {
            continue;
          }

          // Place a short column at the intersection
          long heightHash = positionHash(seed ^ 0xC01A9BL, bx, surfaceY, bz);
          int columnHeight = MIN_COLUMN_HEIGHT + (int) (((heightHash >>> 8) & 0xFFL) % (MAX_COLUMN_HEIGHT - MIN_COLUMN_HEIGHT + 1));
          BlockState material = getMaterial(placeHash);

          for (int dy = 0; dy < columnHeight; dy++) {
            BlockPos pos = new BlockPos(bx, surfaceY + dy, bz);
            if (isInWritableArea(pos, chunkPos)) {
              placeGridBlock(world, pos, material);
            }
          }
        } else {
          // Edge segments between intersections
          if (placeChance >= EDGE_PLACE_CHANCE) {
            continue;
          }

          BlockState material = getMaterial(placeHash);
          BlockPos pos = new BlockPos(bx, surfaceY, bz);
          if (isInWritableArea(pos, chunkPos)) {
            placeGridBlock(world, pos, material);
          }
        }
      }
    }
  }

  /**
   * Places a grid block at the given position, embedding 1 block into terrain.
   * Only places if the target position is air or replaceable.
   */
  private void placeGridBlock(WorldGenLevel world, BlockPos pos, BlockState material) {
    // Embed 1 block into terrain (place at surface - 1)
    BlockPos embeddedPos = pos.below();
    BlockState existingBelow = world.getBlockState(embeddedPos);
    if (existingBelow.isSolid()) {
      world.setBlock(embeddedPos, material, 2);
    }

    // Place at surface level
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() || !existing.isSolid()) {
      world.setBlock(pos, material, 2);
    }

    // Place above surface for visibility
    BlockPos abovePos = pos.above();
    BlockState existingAbove = world.getBlockState(abovePos);
    if (existingAbove.isAir()) {
      world.setBlock(abovePos, material, 2);
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:shattered_grid";
  }
}
