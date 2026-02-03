package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Spheres populator that generates floating or embedded spherical formations.
 *
 * 1.18.2 version - uses java.util.Random instead of RandomSource.
 */
public class SpheresPopulator implements IPopulate {

  private static final int DEFAULT_SPHERES_PER_CHUNK = 1;
  private static final int DEFAULT_MIN_RADIUS = 5;
  private static final int DEFAULT_MAX_RADIUS = 15;
  private static final float DEFAULT_FLOATING_CHANCE = 0.4f;
  private static final float DEFAULT_SPAWN_CHANCE = 0.03f;
  private static final int CRACK_MIN_PLANES = 2;
  private static final int CRACK_MAX_PLANES = 4;
  private static final int DEFAULT_NEIGHBOR_RANGE = 2;
  private static final int DEFAULT_MIN_BASE_Y = 40;
  private static final int DEFAULT_MAX_BASE_Y = 99;
  private final long seed;
  private final int spheresPerChunk;
  private final int minRadius;
  private final int maxRadius;
  private final float floatingChance;
  private final float spawnChance;
  private final int neighborRange;
  private final int minBaseY;
  private final int maxBaseY;

  public SpheresPopulator(long seed) {
    this(seed, null);
  }

  public SpheresPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spheresPerChunk = PopulatorConfig.getInt(params, "count", DEFAULT_SPHERES_PER_CHUNK);
    this.minRadius = Math.max(1, PopulatorConfig.getInt(params, "min_radius", DEFAULT_MIN_RADIUS));
    this.maxRadius = Math.max(this.minRadius, PopulatorConfig.getInt(params, "max_radius", DEFAULT_MAX_RADIUS));
    this.floatingChance = PopulatorConfig.getFloat(params, "floating_chance", DEFAULT_FLOATING_CHANCE);
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.neighborRange = Math.max(1, PopulatorConfig.getInt(params, "neighbor_range", DEFAULT_NEIGHBOR_RANGE));
    this.minBaseY = PopulatorConfig.getInt(params, "min_base_y", DEFAULT_MIN_BASE_Y);
    this.maxBaseY = Math.max(this.minBaseY, PopulatorConfig.getInt(params, "max_base_y", DEFAULT_MAX_BASE_Y));
  }

  private static long positionHash(long seed, int x, int y, int z) {
    long h = seed;
    h ^= (long) x * 73856093L;
    h ^= (long) y * 19349663L;
    h ^= (long) z * 83492791L;
    h = h * 6364136223846793005L + 1442695040888963407L;
    return h;
  }

  private static double positionNoise(long seed, int x, int y, int z, double frequency) {
    int sx = (int) Math.floor(x * frequency);
    int sy = (int) Math.floor(y * frequency);
    int sz = (int) Math.floor(z * frequency);
    long h = positionHash(seed, sx, sy, sz);
    return ((h >>> 16) & 0xFFFFL) / 32767.0 - 1.0;
  }

  private static boolean shouldErode(long shapeSeed, int x, int y, int z,
                                     double distSq, double effectiveRadius) {
    double dist = Math.sqrt(distSq) / effectiveRadius;
    double noise = positionNoise(shapeSeed + 101, x, y, z, 0.25);

    if (dist > 0.8 && noise > -0.5) {
      return true;
    }
    if (dist > 0.6 && noise > 0.1) {
      return true;
    }
    return dist < 0.5 && noise > 0.75;
  }

  @Override
  public void populate(WorldGenLevel world, Random random, BlockPos chunkPos) {
    int thisChunkX = chunkPos.getX() >> 4;
    int thisChunkZ = chunkPos.getZ() >> 4;

    int chunkMinX = thisChunkX << 4;
    int chunkMaxX = chunkMinX + 15;
    int chunkMinZ = thisChunkZ << 4;
    int chunkMaxZ = chunkMinZ + 15;

    for (int ncx = thisChunkX - neighborRange; ncx <= thisChunkX + neighborRange; ncx++) {
      for (int ncz = thisChunkZ - neighborRange; ncz <= thisChunkZ + neighborRange; ncz++) {
        long chunkSeed = getChunkSeed(ncx, ncz);
        Random chunkRand = new Random(chunkSeed);

        int neighborMinX = ncx << 4;
        int neighborMinZ = ncz << 4;

        for (int i = 0; i < spheresPerChunk; i++) {
          if (chunkRand.nextFloat() >= spawnChance) {
            continue;
          }

          int cx = neighborMinX + chunkRand.nextInt(16);
          int cz = neighborMinZ + chunkRand.nextInt(16);
          boolean floating = chunkRand.nextFloat() < floatingChance;
          int radius = minRadius + chunkRand.nextInt(maxRadius - minRadius + 1);
          BlockState sphereBlock = getSphereMaterial(chunkRand, floating);
          BlockState coreBlock = getCoreBlock(sphereBlock, chunkRand);

          int yOffset = floating ? (30 + chunkRand.nextInt(70)) : chunkRand.nextInt(20);
          long irregSeed = chunkRand.nextLong();
          long decorSubSeed = chunkRand.nextLong();

          SphereShape shape = pickSphereShape(chunkRand);
          long shapeSeed = chunkRand.nextLong();

          if (cx + radius < chunkMinX || cx - radius > chunkMaxX ||
              cz + radius < chunkMinZ || cz - radius > chunkMaxZ) {
            continue;
          }

          int baseRange = Math.max(1, maxBaseY - minBaseY + 1);
          int baseY = minBaseY + (int) ((irregSeed & 0x7FL) % baseRange);
          int y = floating ? (baseY + 40 + yOffset) : baseY;

          BlockPos center = new BlockPos(cx, y, cz);
          generateSphere(world, irregSeed, decorSubSeed, shapeSeed, shape,
              center, radius, floating,
              sphereBlock, coreBlock, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
      }
    }
  }

  private long getChunkSeed(int chunkX, int chunkZ) {
    return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0x5943E5L);
  }

  private boolean isInChunk(BlockPos pos, int minX, int maxX, int minZ, int maxZ) {
    return pos.getX() >= minX && pos.getX() <= maxX &&
        pos.getZ() >= minZ && pos.getZ() <= maxZ;
  }

  private void generateSphere(WorldGenLevel world, long irregSeed, long decorSubSeed,
                              long shapeSeed, SphereShape shape,
                              BlockPos center, int radius,
                              boolean floating, BlockState sphereBlock, BlockState coreBlock,
                              int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    int coreRadius = radius > 8 ? radius / 3 : 0;
    CrackPlanes crackPlanes = null;
    if (shape == SphereShape.CRACKED) {
      crackPlanes = new CrackPlanes(shapeSeed, radius);
    }

    int startX = Math.max(-radius, chunkMinX - center.getX());
    int endX = Math.min(radius, chunkMaxX - center.getX());
    int startZ = Math.max(-radius, chunkMinZ - center.getZ());
    int endZ = Math.min(radius, chunkMaxZ - center.getZ());

    for (int dx = startX; dx <= endX; dx++) {
      for (int dy = -radius; dy <= radius; dy++) {
        for (int dz = startZ; dz <= endZ; dz++) {
          double distSq = dx * dx + dy * dy + dz * dz;

          int bx = center.getX() + dx;
          int by = center.getY() + dy;
          int bz = center.getZ() + dz;
          long posHash = positionHash(irregSeed, bx, by, bz);
          double irregularity = (posHash & 0xFFFFL) / (double) 0xFFFFL * 0.8;

          double effectiveRadius = radius + irregularity;
          if (distSq <= effectiveRadius * effectiveRadius) {
            BlockPos spherePos = new BlockPos(bx, by, bz);

            if (shape == SphereShape.CRACKED && crackPlanes != null
                && crackPlanes.isInCrack(shapeSeed, bx, by, bz, dx, dy, dz)) {
              continue;
            }
            if (shape == SphereShape.ERODED
                && shouldErode(shapeSeed, bx, by, bz, distSq, effectiveRadius)) {
              continue;
            }

            BlockState blockToPlace;
            if (coreRadius > 0 && distSq <= (double) coreRadius * coreRadius) {
              blockToPlace = coreBlock;
            } else {
              blockToPlace = sphereBlock;
            }

            if (shouldPlaceSphereBlock(world, spherePos, floating)) {
              world.setBlock(spherePos, blockToPlace, 2);
            }
          }
        }
      }
    }

    if (floating) {
      addFloatingSphereDecorations(world, decorSubSeed, center, radius,
          chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }
  }

  private SphereShape pickSphereShape(Random random) {
    int roll = random.nextInt(100);
    if (roll < 50) {
      return SphereShape.NORMAL;
    }
    if (roll < 75) {
      return SphereShape.CRACKED;
    }
    return SphereShape.ERODED;
  }

  private boolean shouldPlaceSphereBlock(WorldGenLevel world, BlockPos pos, boolean floating) {
    BlockState existing = world.getBlockState(pos);

    if (floating) {
      return existing.isAir() ||
          existing.is(BlockTags.LEAVES) ||
          existing.is(Blocks.SNOW) ||
          !existing.getMaterial().isSolid();
    } else {
      return true;
    }
  }

  private BlockState getSphereMaterial(Random random, boolean floating) {
    if (floating) {
      int choice = random.nextInt(5);
      return switch (choice) {
        case 0 -> Blocks.SANDSTONE.defaultBlockState();
        case 1 -> Blocks.SMOOTH_STONE.defaultBlockState();
        case 2 -> Blocks.PRISMARINE.defaultBlockState();
        case 3 -> Blocks.END_STONE.defaultBlockState();
        default -> Blocks.STONE.defaultBlockState();
      };
    } else {
      int choice = random.nextInt(6);
      return switch (choice) {
        case 0 -> Blocks.STONE.defaultBlockState();
        case 1 -> Blocks.DEEPSLATE.defaultBlockState();
        case 2 -> Blocks.ANDESITE.defaultBlockState();
        case 3 -> Blocks.DIORITE.defaultBlockState();
        case 4 -> Blocks.GRANITE.defaultBlockState();
        default -> Blocks.COBBLESTONE.defaultBlockState();
      };
    }
  }

  private BlockState getCoreBlock(BlockState outerBlock, Random random) {
    if (random.nextInt(3) == 0) {
      int choice = random.nextInt(8);
      return switch (choice) {
        case 0 -> Blocks.OBSIDIAN.defaultBlockState();
        case 1 -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
        case 2 -> Blocks.GLOWSTONE.defaultBlockState();
        case 3 -> Blocks.SEA_LANTERN.defaultBlockState();
        case 4 -> Blocks.GOLD_BLOCK.defaultBlockState();
        case 5 -> Blocks.IRON_BLOCK.defaultBlockState();
        case 6 -> Blocks.LAPIS_BLOCK.defaultBlockState();
        default -> Blocks.DIAMOND_BLOCK.defaultBlockState();
      };
    }
    return outerBlock;
  }

  private void addFloatingSphereDecorations(WorldGenLevel world, long decorSubSeed,
                                            BlockPos center, int radius,
                                            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    Random decorRand = new Random(decorSubSeed);
    for (int attempt = 0; attempt < radius / 2; attempt++) {
      double angle = decorRand.nextDouble() * Math.PI * 2;
      double elevation = (decorRand.nextDouble() - 0.5) * Math.PI;

      int dx = (int) (Math.cos(angle) * Math.cos(elevation) * (radius + 1));
      int dy = (int) (Math.sin(elevation) * (radius + 1));
      int dz = (int) (Math.sin(angle) * Math.cos(elevation) * (radius + 1));

      int chance = decorRand.nextInt(4);
      boolean useGlowstone = decorRand.nextBoolean();

      BlockPos decorPos = center.offset(dx, dy, dz);
      if (isInChunk(decorPos, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)
          && world.getBlockState(decorPos).isAir() && chance == 0) {
        BlockState decoration = useGlowstone
            ? Blocks.GLOWSTONE.defaultBlockState()
            : Blocks.SEA_LANTERN.defaultBlockState();
        world.setBlock(decorPos, decoration, 2);
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:spheres";
  }

  private enum SphereShape {
    NORMAL,
    CRACKED,
    ERODED
  }

  private static class CrackPlanes {
    private final double[] nx;
    private final double[] ny;
    private final double[] nz;
    private final double[] offset;
    private final double[] width;

    private CrackPlanes(long seed, int radius) {
      Random crackRand = new Random(seed);
      int count = CRACK_MIN_PLANES + crackRand.nextInt(CRACK_MAX_PLANES - CRACK_MIN_PLANES + 1);
      this.nx = new double[count];
      this.ny = new double[count];
      this.nz = new double[count];
      this.offset = new double[count];
      this.width = new double[count];

      for (int i = 0; i < count; i++) {
        double u = crackRand.nextDouble() * 2.0 - 1.0;
        double theta = crackRand.nextDouble() * Math.PI * 2.0;
        double sqrt = Math.sqrt(1.0 - u * u);
        nx[i] = sqrt * Math.cos(theta);
        ny[i] = u;
        nz[i] = sqrt * Math.sin(theta);
        offset[i] = (crackRand.nextDouble() * 2.0 - 1.0) * radius * 0.35;
        width[i] = 0.7 + crackRand.nextDouble() * 1.8;
      }
    }

    private boolean isInCrack(long shapeSeed, int x, int y, int z, int dx, int dy, int dz) {
      for (int i = 0; i < nx.length; i++) {
        double planeDist = Math.abs(nx[i] * dx + ny[i] * dy + nz[i] * dz - offset[i]);
        double rough = positionNoise(shapeSeed + 17L * (i + 1), x, y, z, 0.35) * 0.8;
        if (planeDist + rough < width[i]) {
          return true;
        }
      }
      return false;
    }
  }
}
