package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Tendrils populator that generates vine-like terrain formations.
 * Tendrils twist, curve, and creep across multiple chunks.
 * Uses the neighbor-seed pattern: each chunk deterministically replays
 * nearby tendril paths and only places blocks within its own 16x16 area.
 * <p>
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public class TendrilsPopulator implements IPopulate {

  private static final int DEFAULT_TENDRILS_PER_CHUNK = 1;
  private static final int DEFAULT_MIN_LENGTH = 35;
  private static final int DEFAULT_MAX_LENGTH = 90;
  // ~12% of chunks spawn a surface root
  private static final float DEFAULT_SPAWN_CHANCE = 0.12f;
  // Max lateral drift: curvature +/-2.0 per segment * 50 segments = 100 blocks = 7 chunks
  private static final int DEFAULT_NEIGHBOR_RANGE = 8;
  private static final int DEFAULT_MIN_THICKNESS = 2;
  private static final int DEFAULT_MAX_THICKNESS = 5;
  private final long seed;
  private final int tendrilsPerChunk;
  private final int minLength;
  private final int maxLength;
  private final float spawnChance;
  private final int neighborRange;
  private final int minThickness;
  private final int maxThickness;

  public TendrilsPopulator(long seed) {
    this(seed, null);
  }

  public TendrilsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.tendrilsPerChunk = PopulatorConfig.getInt(params, "count", DEFAULT_TENDRILS_PER_CHUNK);
    this.minLength = Math.max(5, PopulatorConfig.getInt(params, "min_length", DEFAULT_MIN_LENGTH));
    this.maxLength = Math.max(this.minLength, PopulatorConfig.getInt(params, "max_length", DEFAULT_MAX_LENGTH));
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.neighborRange = Math.max(1, PopulatorConfig.getInt(params, "neighbor_range", DEFAULT_NEIGHBOR_RANGE));
    this.minThickness = Math.max(1, PopulatorConfig.getInt(params, "min_thickness", DEFAULT_MIN_THICKNESS));
    this.maxThickness = Math.max(this.minThickness, PopulatorConfig.getInt(params, "max_thickness", DEFAULT_MAX_THICKNESS));
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

  @Override
  public void populate(WorldGenLevel world, Random random, BlockPos chunkPos) {
    int thisChunkX = chunkPos.getX() >> 4;
    int thisChunkZ = chunkPos.getZ() >> 4;

    int chunkMinX = thisChunkX << 4;
    int chunkMaxX = chunkMinX + 15;
    int chunkMinZ = thisChunkZ << 4;
    int chunkMaxZ = chunkMinZ + 15;

    // Check this chunk and neighbors for tendrils that might reach us
    for (int ncx = thisChunkX - neighborRange; ncx <= thisChunkX + neighborRange; ncx++) {
      for (int ncz = thisChunkZ - neighborRange; ncz <= thisChunkZ + neighborRange; ncz++) {
        long chunkSeed = getChunkSeed(ncx, ncz);
        Random chunkRand = new Random(chunkSeed);

        int neighborMinX = ncx << 4;
        int neighborMinZ = ncz << 4;

        for (int i = 0; i < tendrilsPerChunk; i++) {
          // Deterministic spawn chance - skip most chunks
          if (chunkRand.nextFloat() >= spawnChance) {
            continue;
          }

          // Deterministically compute tendril origin
          int startX = neighborMinX + chunkRand.nextInt(16);
          int startZ = neighborMinZ + chunkRand.nextInt(16);
          // Consume all random state for this tendril deterministically
          // (material, decoration, length, curvature params, etc.)
          BlockState tendrilBlock = getTendrilMaterial(chunkRand);
          BlockState decorationBlock = getDecorationBlock(tendrilBlock, chunkRand);
          int length = minLength + chunkRand.nextInt(maxLength - minLength + 1);

          // Initial curvature - surface crawling bias
          double curvature = (chunkRand.nextDouble() - 0.5) * 0.6;
          // Base thickness: 2-5 blocks
          int baseThickness = minThickness + chunkRand.nextInt(maxThickness - minThickness + 1);

          // Pre-consume all random calls for the tendril path so that
          // the path is fully deterministic regardless of which chunk visits it.
          // We store the path positions and then filter to our chunk.
          long pathSeed = chunkRand.nextLong();
          long decorSeed = chunkRand.nextLong();

          // Now replay the tendril path and place only blocks in our chunk
          generateTendril(world, pathSeed, decorSeed, startX, startZ,
              tendrilBlock, decorationBlock, length, curvature,
              baseThickness, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
      }
    }
  }

  private long getChunkSeed(int chunkX, int chunkZ) {
    return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0x7E4D51L);
  }

  private void generateTendril(WorldGenLevel world, long pathSeed, long decorSeed,
                               int startX, int startZ,
                               BlockState tendrilBlock, BlockState decorationBlock,
                               int length, double curvature,
                               int baseThickness,
                               int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    Random pathRand = new Random(pathSeed);
    double currentX = startX;
    double currentZ = startZ;
    double angle = pathRand.nextDouble() * Math.PI * 2.0;
    double curv = curvature;

    int reachTimer = 0;
    int reachHeight = 0;

    for (int segment = 0; segment < length; segment++) {
      double progress = (double) segment / length;

      if (segment % 4 == 0) {
        curv += (pathRand.nextDouble() - 0.5) * 0.35;
        curv = Math.max(-0.7, Math.min(0.7, curv));
      }
      angle += curv + (pathRand.nextDouble() - 0.5) * 0.15;

      double speed = 0.9 + pathRand.nextDouble() * 0.6;
      currentX += Math.cos(angle) * speed;
      currentZ += Math.sin(angle) * speed;

      int centerBx = (int) Math.floor(currentX);
      int centerBz = (int) Math.floor(currentZ);

      // Skip if center is outside our chunk bounds to avoid accessing unloaded chunks
      if (centerBx < chunkMinX - 1 || centerBx > chunkMaxX + 1 || centerBz < chunkMinZ - 1 || centerBz > chunkMaxZ + 1) {
        continue;
      }

      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerBx, centerBz) - 1;
      if (surfaceY <= world.getMinBuildHeight() + 1 || surfaceY >= world.getMaxBuildHeight() - 2) {
        continue;
      }

      double taper = 1.0 - (progress * 0.5);
      int thickness = (int) Math.max(1, Math.round(baseThickness * taper));

      placeRootDisk(world, centerBx, surfaceY, centerBz, thickness,
          tendrilBlock, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);

      // occasional upward reach
      if (reachTimer <= 0) {
        long reachHash = positionHash(pathSeed ^ 0xABCD42L, centerBx, surfaceY, centerBz);
        if ((reachHash & 0x1F) == 0) {
          reachTimer = 4 + (int) ((reachHash >>> 5) & 0x7);
          reachHeight = 4 + (int) ((reachHash >>> 9) & 0x7);
        }
      }
      if (reachTimer > 0) {
        placeVerticalShoot(world, centerBx, surfaceY, centerBz, reachHeight,
            tendrilBlock, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        reachTimer--;
      }

      // occasional branching
      if (segment > 6 && segment < length - 8) {
        long branchHash = positionHash(pathSeed ^ 0xC0FFEE1L, centerBx, surfaceY, centerBz);
        if ((branchHash & 0x3F) == 0) {
          int branchLength = 8 + (int) ((branchHash >>> 6) & 0x7);
          double branchAngle = angle + ((branchHash & 0x100) == 0 ? 1.0 : -1.0) * 0.8;
          generateBranch(world, branchHash, centerBx, centerBz, branchAngle,
              tendrilBlock, branchLength, Math.max(1, thickness - 1),
              chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
      }

      // surface decorations
      long decorHash = positionHash(decorSeed, centerBx, surfaceY, centerBz);
      if ((decorHash & 0x7) == 0) {
        int dir = (int) ((decorHash >> 3) & 0x3);
        int ddx = (dir == 0) ? 1 : (dir == 1) ? -1 : 0;
        int ddz = (dir == 2) ? 1 : (dir == 3) ? -1 : 0;
        int dbx = centerBx + ddx;
        int dbz = centerBz + ddz;
        if (dbx >= chunkMinX && dbx <= chunkMaxX && dbz >= chunkMinZ && dbz <= chunkMaxZ) {
          BlockPos decorPos = new BlockPos(dbx, surfaceY + 1, dbz);
          if (world.getBlockState(decorPos).isAir()) {
            world.setBlock(decorPos, decorationBlock, 2);
          }
        }
      }
    }
  }

  private void placeRootDisk(WorldGenLevel world, int centerBx, int centerBy, int centerBz, int thickness,
                             BlockState tendrilBlock,
                             int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    double thicknessSq = (double) thickness * thickness;
    for (int dx = -thickness; dx <= thickness; dx++) {
      for (int dz = -thickness; dz <= thickness; dz++) {
        double distSq = (double) dx * dx + (double) dz * dz;
        if (distSq > thicknessSq) continue;
        int bx = centerBx + dx;
        int bz = centerBz + dz;
        if (bx < chunkMinX || bx > chunkMaxX || bz < chunkMinZ || bz > chunkMaxZ) {
          continue;
        }
        BlockPos rootPos = new BlockPos(bx, centerBy, bz);
        if (shouldPlaceTendrilBlock(world, rootPos)) {
          world.setBlock(rootPos, tendrilBlock, 2);
        }
        BlockPos embedPos = rootPos.below();
        if (shouldPlaceTendrilBlock(world, embedPos)) {
          world.setBlock(embedPos, tendrilBlock, 2);
        }
      }
    }
  }

  private void placeVerticalShoot(WorldGenLevel world, int centerBx, int baseY, int centerBz, int height,
                                  BlockState tendrilBlock,
                                  int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    for (int dy = 1; dy <= height; dy++) {
      int by = baseY + dy;
      if (by >= world.getMaxBuildHeight()) break;
      if (centerBx < chunkMinX || centerBx > chunkMaxX || centerBz < chunkMinZ || centerBz > chunkMaxZ) {
        return;
      }
      BlockPos pos = new BlockPos(centerBx, by, centerBz);
      if (shouldPlaceTendrilBlock(world, pos)) {
        world.setBlock(pos, tendrilBlock, 2);
      }
    }
  }

  private void generateBranch(WorldGenLevel world, long branchSeed, int startX, int startZ, double angle,
                              BlockState tendrilBlock, int length, int thickness,
                              int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    Random rand = new Random(branchSeed);
    double currentX = startX;
    double currentZ = startZ;
    double curv = (rand.nextDouble() - 0.5) * 0.4;

    for (int segment = 0; segment < length; segment++) {
      angle += curv + (rand.nextDouble() - 0.5) * 0.2;
      currentX += Math.cos(angle) * (0.8 + rand.nextDouble() * 0.4);
      currentZ += Math.sin(angle) * (0.8 + rand.nextDouble() * 0.4);

      int bx = (int) Math.floor(currentX);
      int bz = (int) Math.floor(currentZ);

      // Skip if outside chunk bounds to avoid accessing unloaded chunks
      if (bx < chunkMinX - 1 || bx > chunkMaxX + 1 || bz < chunkMinZ - 1 || bz > chunkMaxZ + 1) {
        continue;
      }

      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
      if (surfaceY <= world.getMinBuildHeight() + 1 || surfaceY >= world.getMaxBuildHeight() - 2) {
        continue;
      }
      placeRootDisk(world, bx, surfaceY, bz, thickness, tendrilBlock,
          chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }
  }

  private boolean shouldPlaceTendrilBlock(WorldGenLevel world, BlockPos pos) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() ||
        existing.is(BlockTags.LEAVES) ||
        existing.is(Blocks.SNOW) ||
        existing.is(Blocks.VINE) ||
        existing.is(Blocks.WATER)) {
      return true;
    }
    // Allow replacing natural terrain so tendrils root into the ground
    return existing.is(Blocks.STONE) ||
        existing.is(Blocks.DEEPSLATE) ||
        existing.is(Blocks.DIRT) ||
        existing.is(Blocks.GRASS_BLOCK) ||
        existing.is(Blocks.SAND) ||
        existing.is(Blocks.SANDSTONE) ||
        existing.is(Blocks.GRAVEL) ||
        existing.is(Blocks.CLAY) ||
        existing.is(Blocks.NETHERRACK) ||
        existing.is(Blocks.END_STONE) ||
        existing.is(BlockTags.TERRACOTTA) ||
        existing.is(BlockTags.DIRT);
  }

  private BlockState getTendrilMaterial(Random random) {
    int choice = random.nextInt(8);
    return switch (choice) {
      case 0 -> Blocks.STONE.defaultBlockState();
      case 1 -> Blocks.COBBLESTONE.defaultBlockState();
      case 2 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
      case 3 -> Blocks.ANDESITE.defaultBlockState();
      case 4 -> Blocks.DRIPSTONE_BLOCK.defaultBlockState();
      case 5 -> Blocks.BASALT.defaultBlockState();
      case 6 -> Blocks.BLACKSTONE.defaultBlockState();
      default -> Blocks.STONE_BRICKS.defaultBlockState();
    };
  }

  private BlockState getDecorationBlock(BlockState baseBlock, Random random) {
    if (random.nextInt(3) == 0) {
      int choice = random.nextInt(5);
      return switch (choice) {
        case 0 -> Blocks.MOSS_CARPET.defaultBlockState();
        case 1 -> Blocks.HANGING_ROOTS.defaultBlockState();
        case 2 -> Blocks.GLOW_LICHEN.defaultBlockState();
        case 3 -> Blocks.SMALL_DRIPLEAF.defaultBlockState();
        default -> Blocks.FERN.defaultBlockState();
      };
    }
    return baseBlock;
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:tendrils";
  }
}
