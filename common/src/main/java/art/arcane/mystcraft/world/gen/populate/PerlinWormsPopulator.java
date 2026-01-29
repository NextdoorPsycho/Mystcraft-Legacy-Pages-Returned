package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Perlin Worms populator that carves sinuous 3D tunnels through terrain.
 * Unlike tendrils which build outward, worms carve inward using 3D Perlin noise
 * to steer their heading, producing smooth, continuously curving tunnels that
 * feel organic and interconnected.
 * <p>
 * Uses the neighbor-seed pattern for cross-chunk deterministic generation.
 */
public class PerlinWormsPopulator implements IPopulate {

  private static final int DEFAULT_WORMS_PER_CHUNK = 2;
  private static final int DEFAULT_MIN_LENGTH = 80;
  private static final int DEFAULT_MAX_LENGTH = 220;
  private static final float DEFAULT_SPAWN_CHANCE = 0.35f;
  // Worms can drift far: heading changes smoothly so max lateral drift is bounded
  // by segment count * max turn rate. 220 segments * ~1.5 blocks lateral = ~330 blocks = ~21 chunks
  private static final int DEFAULT_NEIGHBOR_RANGE = 20;
  // Noise frequency controls how quickly the worm changes direction
  private static final double DEFAULT_NOISE_FREQUENCY = 0.03;
  private static final int DEFAULT_MIN_RADIUS = 3;
  private static final int DEFAULT_MAX_RADIUS = 6;
  private static final float DEFAULT_LEAVES_FLOOR_CHANCE = 0.5f;
  private final long seed;
  private final int wormsPerChunk;
  private final int minLength;
  private final int maxLength;
  private final float spawnChance;
  private final int neighborRange;
  private final double noiseFrequency;
  private final int minRadius;
  private final int maxRadius;
  private final float leavesFloorChance;
  private final Integer minStartYOverride;
  private final Integer maxStartYOverride;
  // Permutation table for Perlin noise (fixed, deterministic)
  private final int[] perm;

  public PerlinWormsPopulator(long seed) {
    this(seed, null);
  }

  public PerlinWormsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.wormsPerChunk = PopulatorConfig.getInt(params, "count", DEFAULT_WORMS_PER_CHUNK);
    this.minLength = Math.max(10, PopulatorConfig.getInt(params, "min_length", DEFAULT_MIN_LENGTH));
    this.maxLength = Math.max(this.minLength, PopulatorConfig.getInt(params, "max_length", DEFAULT_MAX_LENGTH));
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.neighborRange = Math.max(1, PopulatorConfig.getInt(params, "neighbor_range", DEFAULT_NEIGHBOR_RANGE));
    this.noiseFrequency = PopulatorConfig.getDouble(params, "noise_frequency", DEFAULT_NOISE_FREQUENCY);
    this.minRadius = Math.max(1, PopulatorConfig.getInt(params, "min_radius", DEFAULT_MIN_RADIUS));
    this.maxRadius = Math.max(this.minRadius, PopulatorConfig.getInt(params, "max_radius", DEFAULT_MAX_RADIUS));
    this.leavesFloorChance = PopulatorConfig.getFloat(params, "leaves_floor_chance", DEFAULT_LEAVES_FLOOR_CHANCE);
    this.minStartYOverride = PopulatorConfig.getOptionalInt(params, "min_start_y", PopulatorConfig.UNSET_INT);
    this.maxStartYOverride = PopulatorConfig.getOptionalInt(params, "max_start_y", PopulatorConfig.UNSET_INT);
    this.perm = buildPermutationTable(seed);
  }

  private static double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private static double lerp(double t, double a, double b) {
    return a + t * (b - a);
  }

  private static double grad(int hash, double x, double y, double z) {
    int h = hash & 15;
    double u = h < 8 ? x : y;
    double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
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

        for (int i = 0; i < wormsPerChunk; i++) {
          if (chunkRand.nextFloat() >= spawnChance) {
            continue;
          }

          int startX = neighborMinX + chunkRand.nextInt(16);
          int startZ = neighborMinZ + chunkRand.nextInt(16);
          int length = minLength + chunkRand.nextInt(maxLength - minLength + 1);

          // Worm type determines behavior
          int wormType = chunkRand.nextInt(4);
          int baseRadius = minRadius + chunkRand.nextInt(maxRadius - minRadius + 1);
          boolean leavesFloor = chunkRand.nextFloat() < leavesFloorChance;

          // Initial heading angles (yaw and pitch in radians)
          double initialYaw = chunkRand.nextDouble() * Math.PI * 2.0;
          double initialPitch = (chunkRand.nextDouble() - 0.5) * 0.6;

          // Noise offset so each worm samples different noise space
          double noiseOffsetX = chunkRand.nextDouble() * 1000.0;
          double noiseOffsetY = chunkRand.nextDouble() * 1000.0;
          double noiseOffsetZ = chunkRand.nextDouble() * 1000.0;

          long pathSeed = chunkRand.nextLong();

          generateWorm(world, pathSeed, startX, startZ, length,
              wormType, baseRadius, leavesFloor,
              initialYaw, initialPitch,
              noiseOffsetX, noiseOffsetY, noiseOffsetZ,
              chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
      }
    }
  }

  private long getChunkSeed(int chunkX, int chunkZ) {
    return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0xA3F_91DL);
  }

  // --- Perlin Noise Implementation ---

  private void generateWorm(WorldGenLevel world, long pathSeed,
                            int startX, int startZ, int length,
                            int wormType, int baseRadius, boolean leavesFloor,
                            double initialYaw, double initialPitch,
                            double noiseOffX, double noiseOffY, double noiseOffZ,
                            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    // Deterministic start Y from path seed
    Random yRand = new Random(pathSeed ^ 0xB4E_7C02L);
    int startY;
    if (minStartYOverride != null && maxStartYOverride != null) {
      int minY = Math.min(minStartYOverride, maxStartYOverride);
      int maxY = Math.max(minStartYOverride, maxStartYOverride);
      startY = minY + yRand.nextInt(Math.max(1, maxY - minY + 1));
    } else {
      switch (wormType) {
        case 0 -> startY = 20 + yRand.nextInt(30);  // Deep worm (20-49)
        case 1 -> startY = 40 + yRand.nextInt(40);  // Mid worm (40-79)
        case 2 -> startY = 10 + yRand.nextInt(50);  // Full-range worm (10-59)
        default -> startY = 50 + yRand.nextInt(30);  // Shallow worm (50-79)
      }
    }

    double currentX = startX;
    double currentY = startY;
    double currentZ = startZ;
    double yaw = initialYaw;
    double pitch = initialPitch;

    for (int segment = 0; segment < length; segment++) {
      double progress = (double) segment / length;

      // Sample 3D Perlin noise at the worm's current position to steer heading
      double noiseX = (currentX * noiseFrequency) + noiseOffX;
      double noiseY = (currentY * noiseFrequency) + noiseOffY;
      double noiseZ = (currentZ * noiseFrequency) + noiseOffZ;

      double yawDelta = perlinNoise(noiseX, noiseY, noiseZ) * 0.6;
      double pitchDelta = perlinNoise(noiseX + 100, noiseY + 100, noiseZ + 100) * 0.3;

      yaw += yawDelta;

      // Clamp pitch to prevent worms from going too vertical
      pitch += pitchDelta;
      pitch = Math.max(-0.5, Math.min(0.5, pitch));

      // Move forward along the heading
      double speed = 1.2 + 0.3 * Math.sin(segment * 0.15);
      currentX += Math.cos(yaw) * Math.cos(pitch) * speed;
      currentY += Math.sin(pitch) * speed;
      currentZ += Math.sin(yaw) * Math.cos(pitch) * speed;

      // Keep Y in bounds
      if (currentY < -60) {
        pitch = Math.abs(pitch);
        currentY = -60;
      }
      if (currentY > 100) {
        pitch = -Math.abs(pitch);
        currentY = 100;
      }

      // Radius varies along the worm: wider in the middle, tapers at ends
      double taperFactor = 1.0 - Math.pow(2.0 * progress - 1.0, 4);
      int radius = (int) Math.max(1, Math.round(baseRadius * (0.5 + 0.5 * taperFactor)));

      // Occasional widening for chambers
      long chamberHash = positionHash(pathSeed, (int) currentX, (int) currentY, (int) currentZ);
      if ((chamberHash & 0xF) == 0) {
        radius += 3 + (int) ((chamberHash >>> 4) & 0x3);
      }

      int centerBx = (int) Math.floor(currentX);
      int centerBy = (int) Math.floor(currentY);
      int centerBz = (int) Math.floor(currentZ);

      // Carve a sphere at each segment position
      double radiusSq = (double) radius * radius;
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dy = -radius; dy <= radius; dy++) {
          for (int dz = -radius; dz <= radius; dz++) {
            double distSq = (double) dx * dx + (double) dy * dy + (double) dz * dz;
            if (distSq > radiusSq) {
              continue;
            }

            int bx = centerBx + dx;
            int by = centerBy + dy;
            int bz = centerBz + dz;

            if (bx < chunkMinX || bx > chunkMaxX || bz < chunkMinZ || bz > chunkMaxZ) {
              continue;
            }

            // Leave floor blocks for walkability
            if (leavesFloor && dy == -radius && distSq > radiusSq * 0.5) {
              continue;
            }

            BlockPos pos = new BlockPos(bx, by, bz);
            if (shouldCarve(world, pos)) {
              // Bottom of tunnel gets a different treatment
              if (leavesFloor && dy == -radius + 1) {
                // Flatten the floor slightly
                continue;
              }
              world.setBlock(pos, getCarveResult(world, pos, by), 2);
            }
          }
        }
      }

      // Position-deterministic decorations along the tunnel walls
      long decorHash = positionHash(pathSeed ^ 0xDE_C0L, centerBx, centerBy, centerBz);
      if ((decorHash & 0xF) == 0 && segment > 5 && segment < length - 5) {
        placeDecoration(world, centerBx, centerBy, centerBz, radius, decorHash,
            chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
      }
    }
  }

  private boolean shouldCarve(WorldGenLevel world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    if (state.isAir() || state.is(Blocks.BEDROCK)) {
      return false;
    }
    // Carve through solid natural terrain
    return state.is(Blocks.STONE) ||
        state.is(Blocks.DEEPSLATE) ||
        state.is(Blocks.COBBLED_DEEPSLATE) ||
        state.is(Blocks.DIRT) ||
        state.is(Blocks.GRAVEL) ||
        state.is(Blocks.SAND) ||
        state.is(Blocks.SANDSTONE) ||
        state.is(Blocks.ANDESITE) ||
        state.is(Blocks.DIORITE) ||
        state.is(Blocks.GRANITE) ||
        state.is(Blocks.TUFF) ||
        state.is(Blocks.CALCITE) ||
        state.is(Blocks.CLAY) ||
        state.is(Blocks.NETHERRACK) ||
        state.is(Blocks.END_STONE) ||
        state.is(Blocks.GRASS_BLOCK) ||
        state.is(Blocks.COARSE_DIRT) ||
        state.is(BlockTags.TERRACOTTA) ||
        state.is(BlockTags.DIRT) ||
        state.is(BlockTags.BASE_STONE_OVERWORLD) ||
        state.is(BlockTags.BASE_STONE_NETHER);
  }

  private BlockState getCarveResult(WorldGenLevel world, BlockPos pos, int y) {
    // Below sea level, fill with water; otherwise air
    if (y < world.getSeaLevel()) {
      return Blocks.WATER.defaultBlockState();
    }
    return Blocks.AIR.defaultBlockState();
  }

  private void placeDecoration(WorldGenLevel world, int cx, int cy, int cz, int radius,
                               long hash, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    int decorType = (int) ((hash >> 4) & 0x7);
    int dir = (int) ((hash >> 7) & 0x3);

    // Place decoration on tunnel walls
    int dx = (dir == 0) ? radius + 1 : (dir == 1) ? -(radius + 1) : 0;
    int dz = (dir == 2) ? radius + 1 : (dir == 3) ? -(radius + 1) : 0;
    int bx = cx + dx;
    int bz = cz + dz;

    if (bx < chunkMinX || bx > chunkMaxX || bz < chunkMinZ || bz > chunkMaxZ) {
      return;
    }

    BlockPos decorPos = new BlockPos(bx, cy, bz);
    if (!world.getBlockState(decorPos).isAir()) {
      return;
    }

    BlockState decoration = switch (decorType) {
      case 0 -> Blocks.GLOW_LICHEN.defaultBlockState();
      case 1 -> Blocks.MOSS_CARPET.defaultBlockState();
      case 2 -> Blocks.POINTED_DRIPSTONE.defaultBlockState();
      case 3 -> Blocks.SCULK_VEIN.defaultBlockState();
      default -> null;
    };

    if (decoration != null) {
      world.setBlock(decorPos, decoration, 2);
    }
  }

  private int[] buildPermutationTable(long seed) {
    int[] p = new int[512];
    int[] base = new int[256];
    for (int i = 0; i < 256; i++) {
      base[i] = i;
    }
    Random r = new Random(seed ^ 0x9E3779B97F4A7C15L);
    for (int i = 255; i > 0; i--) {
      int j = r.nextInt(i + 1);
      int tmp = base[i];
      base[i] = base[j];
      base[j] = tmp;
    }
    for (int i = 0; i < 256; i++) {
      p[i] = base[i];
      p[i + 256] = base[i];
    }
    return p;
  }

  private double perlinNoise(double x, double y, double z) {
    int xi = (int) Math.floor(x) & 255;
    int yi = (int) Math.floor(y) & 255;
    int zi = (int) Math.floor(z) & 255;

    double xf = x - Math.floor(x);
    double yf = y - Math.floor(y);
    double zf = z - Math.floor(z);

    double u = fade(xf);
    double v = fade(yf);
    double w = fade(zf);

    int aaa = perm[perm[perm[xi] + yi] + zi];
    int aba = perm[perm[perm[xi] + yi + 1] + zi];
    int aab = perm[perm[perm[xi] + yi] + zi + 1];
    int abb = perm[perm[perm[xi] + yi + 1] + zi + 1];
    int baa = perm[perm[perm[xi + 1] + yi] + zi];
    int bba = perm[perm[perm[xi + 1] + yi + 1] + zi];
    int bab = perm[perm[perm[xi + 1] + yi] + zi + 1];
    int bbb = perm[perm[perm[xi + 1] + yi + 1] + zi + 1];

    double x1 = lerp(u, grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf));
    double x2 = lerp(u, grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf));
    double y1 = lerp(v, x1, x2);

    double x3 = lerp(u, grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1));
    double x4 = lerp(u, grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1));
    double y2 = lerp(v, x3, x4);

    return lerp(w, y1, y2);
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:perlin_worms";
  }
}
