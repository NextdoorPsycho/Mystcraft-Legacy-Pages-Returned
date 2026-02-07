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
 * Obelisks populator that generates tall narrow monoliths jutting from the ground.
 * Four-sided tapering pillars made of deepslate variants with obsidian accents,
 * crying_obsidian tear streaks, and redstone_block rune dots.
 * Single-chunk populator (max width ~7 blocks, no neighbor-seed pattern needed).
 */
public class ObelisksPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.05f;
  private static final int DEFAULT_COUNT = 1;
  private static final int DEFAULT_MIN_HEIGHT = 15;
  private static final int DEFAULT_MAX_HEIGHT = 40;
  private static final int DEFAULT_MIN_BASE_WIDTH = 3;
  private static final int DEFAULT_MAX_BASE_WIDTH = 5;
  private static final int DEFAULT_BURY_DEPTH = 3;
  private static final float TEAR_STREAK_CHANCE = 0.10f;
  private static final float RUNE_DOT_CHANCE = 0.03f;

  private final long seed;
  private final float spawnChance;
  private final int count;
  private final int minHeight;
  private final int maxHeight;
  private final int minBaseWidth;
  private final int maxBaseWidth;

  public ObelisksPopulator(long seed) {
    this(seed, null);
  }

  public ObelisksPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
    this.minHeight = Math.max(5, PopulatorConfig.getInt(params, "min_height", DEFAULT_MIN_HEIGHT));
    this.maxHeight = Math.max(this.minHeight, PopulatorConfig.getInt(params, "max_height", DEFAULT_MAX_HEIGHT));
    this.minBaseWidth = Math.max(1, PopulatorConfig.getInt(params, "min_base_width", DEFAULT_MIN_BASE_WIDTH));
    this.maxBaseWidth = Math.max(this.minBaseWidth, PopulatorConfig.getInt(params, "max_base_width", DEFAULT_MAX_BASE_WIDTH));
  }

  /**
   * Position-deterministic hash for per-block decisions (tear streaks, runes, cap style).
   */
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

      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

      if (surfaceY <= world.getMinBuildHeight() + 1 || surfaceY >= world.getMaxBuildHeight() - 50) {
        continue;
      }

      // Verify ground is solid and not leaves
      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int height = minHeight + random.nextInt(maxHeight - minHeight + 1);
      int baseWidth = minBaseWidth + random.nextInt(maxBaseWidth - minBaseWidth + 1);

      generateObelisk(world, chunkPos, x, surfaceY, z, height, baseWidth);
    }
  }

  private void generateObelisk(WorldGenLevel world, BlockPos chunkPos,
                                int centerX, int surfaceY, int centerZ,
                                int height, int baseWidth) {
    int startY = surfaceY - DEFAULT_BURY_DEPTH;

    // Determine cap style from position hash
    long capHash = positionHash(seed, centerX, surfaceY, centerZ);
    int capRoll = (int) ((capHash >>> 16) & 0xFF) % 100;
    CapStyle capStyle;
    if (capRoll < 50) {
      capStyle = CapStyle.FLAT;
    } else if (capRoll < 80) {
      capStyle = CapStyle.INVERTED_PYRAMID;
    } else {
      capStyle = CapStyle.POINTED;
    }

    // Determine base material from position hash
    long matHash = positionHash(seed ^ 0x4D4154L, centerX, surfaceY, centerZ);
    BlockState baseMaterial = getBaseMaterial((int) ((matHash >>> 8) & 0xFF) % 3);

    int totalHeight = height + DEFAULT_BURY_DEPTH;

    for (int dy = 0; dy < totalHeight; dy++) {
      int y = startY + dy;
      float progress = (float) dy / totalHeight;

      // Linear taper from baseWidth at bottom to 1 at top
      int halfWidth = (int) Math.round(((float) baseWidth / 2.0f) * (1.0f - progress));
      if (halfWidth < 0) {
        halfWidth = 0;
      }

      for (int dx = -halfWidth; dx <= halfWidth; dx++) {
        for (int dz = -halfWidth; dz <= halfWidth; dz++) {
          int bx = centerX + dx;
          int bz = centerZ + dz;

          BlockPos pos = new BlockPos(bx, y, bz);
          if (!isInWritableArea(pos, chunkPos)) {
            continue;
          }

          // Choose block based on position hash
          long blockHash = positionHash(seed, bx, y, bz);
          float blockRoll = ((blockHash >>> 16) & 0xFFFFL) / (float) 0xFFFFL;

          BlockState blockToPlace;
          if (blockRoll < RUNE_DOT_CHANCE) {
            blockToPlace = Blocks.REDSTONE_BLOCK.defaultBlockState();
          } else if (blockRoll < RUNE_DOT_CHANCE + TEAR_STREAK_CHANCE) {
            blockToPlace = Blocks.CRYING_OBSIDIAN.defaultBlockState();
          } else {
            // Mix base material with obsidian accents
            long accentHash = positionHash(seed ^ 0xACC37L, bx, y, bz);
            float accentRoll = ((accentHash >>> 16) & 0xFFFFL) / (float) 0xFFFFL;
            if (accentRoll < 0.15f) {
              blockToPlace = Blocks.OBSIDIAN.defaultBlockState();
            } else {
              blockToPlace = baseMaterial;
            }
          }

          world.setBlock(pos, blockToPlace, 2);
        }
      }
    }

    // Generate cap
    int topY = startY + totalHeight;
    generateCap(world, chunkPos, centerX, topY, centerZ, baseWidth, capStyle);
  }

  private void generateCap(WorldGenLevel world, BlockPos chunkPos,
                            int centerX, int topY, int centerZ,
                            int baseWidth, CapStyle style) {
    // The width at the top of the main pillar body is ~1 block
    // Caps add extra layers above that
    switch (style) {
      case FLAT:
        // Simple flat cap: 1 layer slightly wider than the tip
        for (int dx = -1; dx <= 1; dx++) {
          for (int dz = -1; dz <= 1; dz++) {
            BlockPos pos = new BlockPos(centerX + dx, topY, centerZ + dz);
            if (isInWritableArea(pos, chunkPos)) {
              long blockHash = positionHash(seed, pos.getX(), pos.getY(), pos.getZ());
              BlockState capBlock = getCapBlock(blockHash);
              world.setBlock(pos, capBlock, 2);
            }
          }
        }
        break;

      case INVERTED_PYRAMID:
        // Stair-step outward going up: 3 layers, each wider
        for (int layer = 0; layer < 3; layer++) {
          int halfWidth = 1 + layer;
          for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
              BlockPos pos = new BlockPos(centerX + dx, topY + layer, centerZ + dz);
              if (isInWritableArea(pos, chunkPos)) {
                long blockHash = positionHash(seed, pos.getX(), pos.getY(), pos.getZ());
                BlockState capBlock = getCapBlock(blockHash);
                world.setBlock(pos, capBlock, 2);
              }
            }
          }
        }
        break;

      case POINTED:
        // Single pointed block above the tip
        BlockPos pointPos = new BlockPos(centerX, topY, centerZ);
        if (isInWritableArea(pointPos, chunkPos)) {
          world.setBlock(pointPos, Blocks.OBSIDIAN.defaultBlockState(), 2);
        }
        BlockPos tipPos = new BlockPos(centerX, topY + 1, centerZ);
        if (isInWritableArea(tipPos, chunkPos)) {
          world.setBlock(tipPos, Blocks.OBSIDIAN.defaultBlockState(), 2);
        }
        break;
    }
  }

  private BlockState getBaseMaterial(int index) {
    return switch (index) {
      case 0 -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
      case 1 -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
      default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
    };
  }

  private BlockState getCapBlock(long blockHash) {
    float roll = ((blockHash >>> 16) & 0xFFFFL) / (float) 0xFFFFL;
    if (roll < RUNE_DOT_CHANCE) {
      return Blocks.REDSTONE_BLOCK.defaultBlockState();
    } else if (roll < RUNE_DOT_CHANCE + TEAR_STREAK_CHANCE) {
      return Blocks.CRYING_OBSIDIAN.defaultBlockState();
    } else if (roll < 0.3f) {
      return Blocks.OBSIDIAN.defaultBlockState();
    } else {
      return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:obelisks";
  }

  private enum CapStyle {
    FLAT,
    INVERTED_PYRAMID,
    POINTED
  }
}
