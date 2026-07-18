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

import java.util.Random;

/**
 * Vertical tendrils populator that generates tall, mostly-straight columns
 * extending from ground or ceiling. These are the classic Mystcraft pillar-like
 * formations that grow vertically with only minor wobble. Uses the
 * neighbor-seed pattern for chunk-safe generation.
 */
public class VerticalTendrilsPopulator implements IPopulate {

  private static final int DEFAULT_TENDRILS_PER_CHUNK = 1;
  private static final int DEFAULT_MIN_LENGTH = 25;
  private static final int DEFAULT_MAX_LENGTH = 80;
  private static final float DEFAULT_CEILING_CHANCE = 0.25f;

  private static final float DEFAULT_SPAWN_CHANCE = 0.08f;

  private static final int DEFAULT_NEIGHBOR_RANGE = 2;
  private static final int DEFAULT_MIN_THICKNESS = 2;
  private static final int DEFAULT_MAX_THICKNESS = 6;
  private static final double DEFAULT_WOBBLE_RANGE = 0.4;
  private final long seed;
  private final int tendrilsPerChunk;
  private final int minLength;
  private final int maxLength;
  private final float ceilingChance;
  private final float spawnChance;
  private final int neighborRange;
  private final int minThickness;
  private final int maxThickness;
  private final double wobbleRange;

  public VerticalTendrilsPopulator(long seed) {
    this(seed, null);
  }

  public VerticalTendrilsPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.tendrilsPerChunk = PopulatorConfig.getInt(params, "count", DEFAULT_TENDRILS_PER_CHUNK);
    this.minLength = Math.max(5, PopulatorConfig.getInt(params, "min_length", DEFAULT_MIN_LENGTH));
    this.maxLength = Math.max(this.minLength, PopulatorConfig.getInt(params, "max_length", DEFAULT_MAX_LENGTH));
    this.ceilingChance = PopulatorConfig.getFloat(params, "ceiling_chance", DEFAULT_CEILING_CHANCE);
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.neighborRange = Math.max(1, PopulatorConfig.getInt(params, "neighbor_range", DEFAULT_NEIGHBOR_RANGE));
    this.minThickness = Math.max(1, PopulatorConfig.getInt(params, "min_thickness", DEFAULT_MIN_THICKNESS));
    this.maxThickness = Math.max(this.minThickness, PopulatorConfig.getInt(params, "max_thickness", DEFAULT_MAX_THICKNESS));
    this.wobbleRange = PopulatorConfig.getDouble(params, "wobble_range", DEFAULT_WOBBLE_RANGE);
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

        for (int i = 0; i < tendrilsPerChunk; i++) {

          if (chunkRand.nextFloat() >= spawnChance) {
            continue;
          }

          int startX = neighborMinX + chunkRand.nextInt(16);
          int startZ = neighborMinZ + chunkRand.nextInt(16);
          boolean fromCeiling = chunkRand.nextFloat() < ceilingChance;

          BlockState tendrilBlock = getTendrilMaterial(chunkRand);
          BlockState decorationBlock = getDecorationBlock(tendrilBlock, chunkRand);
          int length = minLength + chunkRand.nextInt(maxLength - minLength + 1);
          int baseThickness = minThickness + chunkRand.nextInt(maxThickness - minThickness + 1);

          double wobbleX = (chunkRand.nextDouble() - 0.5) * wobbleRange;
          double wobbleZ = (chunkRand.nextDouble() - 0.5) * wobbleRange;
          long pathSeed = chunkRand.nextLong();
          long decorSeed = chunkRand.nextLong();

          if (startX < chunkMinX - baseThickness - 1 || startX > chunkMaxX + baseThickness + 1 ||
              startZ < chunkMinZ - baseThickness - 1 || startZ > chunkMaxZ + baseThickness + 1) {
            continue;
          }

          generateTendril(world, pathSeed, decorSeed, startX, startZ, fromCeiling,
              tendrilBlock, decorationBlock, length, wobbleX, wobbleZ,
              baseThickness, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
      }
    }
  }

  private long getChunkSeed(int chunkX, int chunkZ) {
    return seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + 0xBE47L);
  }

  private void generateTendril(WorldGenLevel world, long pathSeed, long decorSeed,
                               int startX, int startZ, boolean fromCeiling,
                               BlockState tendrilBlock, BlockState decorationBlock,
                               int length, double wobbleX, double wobbleZ,
                               int baseThickness,
                               int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
    if (startX < chunkMinX - 16 || startX > chunkMaxX + 16
        || startZ < chunkMinZ - 16 || startZ > chunkMaxZ + 16) {
      return;
    }
    int startY;
    if (fromCeiling) {
      startY = findCeilingPosition(world, startX, startZ, pathSeed, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
      if (startY == -1) {
        return;
      }
      BlockPos checkPos = new BlockPos(startX, startY, startZ);
      if (!world.getBlockState(checkPos).isSolid() || !world.getBlockState(checkPos.below()).isAir()) {
        return;
      }

      startY += baseThickness + 3;
    } else {
      startY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, startX, startZ);
      BlockState ground = world.getBlockState(new BlockPos(startX, startY - 1, startZ));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        return;
      }

      startY -= (baseThickness + 3);
    }

    int direction = fromCeiling ? -1 : 1;
    Random pathRand = new Random(pathSeed);

    double currentX = startX;
    double currentY = startY;
    double currentZ = startZ;

    for (int segment = 0; segment < length; segment++) {
      float progress = (float) segment / length;

      if (segment % 5 == 0) {
        wobbleX += (pathRand.nextDouble() - 0.5) * 0.08;
        wobbleZ += (pathRand.nextDouble() - 0.5) * 0.08;
        wobbleX = Math.max(-0.2, Math.min(0.2, wobbleX));
        wobbleZ = Math.max(-0.2, Math.min(0.2, wobbleZ));
      }

      currentX += wobbleX;
      currentY += direction;
      currentZ += wobbleZ;

      long pillarHash = positionHash(pathSeed, (int) currentX, (int) currentY, (int) currentZ);
      int bulge = (int) ((pillarHash >>> 4) & 0x3) - 1;
      int thickness = baseThickness + bulge - (int) (progress * 1.4f);

      if (progress > 0.85f) {
        float flareProgress = (progress - 0.85f) / 0.15f;
        thickness += (int) (1 + flareProgress * 2);
      }

      if (progress < 0.10f) {
        float rootProgress = 1.0f - (progress / 0.10f);
        thickness += (int) (rootProgress * 2);
      }
      thickness = Math.max(1, Math.min(baseThickness + 4, thickness));

      int centerBx = (int) Math.floor(currentX);
      int centerBy = (int) Math.floor(currentY);
      int centerBz = (int) Math.floor(currentZ);

      double thicknessSq = (double) thickness * thickness;
      for (int dx = -thickness; dx <= thickness; dx++) {
        for (int dz = -thickness; dz <= thickness; dz++) {
          double distSq = (double) dx * dx + (double) dz * dz;
          if (distSq <= thicknessSq) {
            int bx = centerBx + dx;
            int bz = centerBz + dz;

            if (bx >= chunkMinX && bx <= chunkMaxX && bz >= chunkMinZ && bz <= chunkMaxZ) {

              if (!fromCeiling && centerBy < startY + length) {
                int localSurface = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;

                if (centerBy > localSurface + 1 && world.getBlockState(new BlockPos(bx, centerBy - 1, bz)).isAir()) {
                  continue;
                }
              }
              BlockPos tendrilPos = new BlockPos(bx, centerBy, bz);
              if (shouldPlaceBlock(world, tendrilPos)) {
                world.setBlock(tendrilPos, tendrilBlock, 2);
              }
            }
          }
        }
      }

      if ((pillarHash & 0xF) == 0) {
        int dir = (int) ((pillarHash >>> 6) & 0x3);
        int ddx = (dir == 0) ? 1 : (dir == 1) ? -1 : 0;
        int ddz = (dir == 2) ? 1 : (dir == 3) ? -1 : 0;
        int spineLen = 1 + (int) ((pillarHash >>> 9) & 0x5);
        for (int s = 1; s <= spineLen; s++) {
          int bx = centerBx + ddx * (thickness + s);
          int bz = centerBz + ddz * (thickness + s);
          if (bx < chunkMinX || bx > chunkMaxX || bz < chunkMinZ || bz > chunkMaxZ) {
            break;
          }
          BlockPos spinePos = new BlockPos(bx, centerBy, bz);
          if (shouldPlaceBlock(world, spinePos)) {
            world.setBlock(spinePos, tendrilBlock, 2);
          }
        }
      }

      if (segment > 3) {
        long decorHash = positionHash(decorSeed, centerBx, centerBy, centerBz);
        if ((decorHash & 0x7) == 0) {
          int dir = (int) ((decorHash >> 3) & 0x3);
          int ddx = (dir == 0) ? 1 : (dir == 1) ? -1 : 0;
          int ddz = (dir == 2) ? 1 : (dir == 3) ? -1 : 0;
          int dbx = centerBx + ddx;
          int dbz = centerBz + ddz;
          if (dbx >= chunkMinX && dbx <= chunkMaxX && dbz >= chunkMinZ && dbz <= chunkMaxZ) {
            BlockPos decorPos = new BlockPos(dbx, centerBy, dbz);
            if (world.getBlockState(decorPos).isAir()) {
              world.setBlock(decorPos, decorationBlock, 2);
            }
          }
        }
      }

      if (fromCeiling && currentY < startY - length) {
        break;
      } else if (!fromCeiling && currentY > startY + length) {
        break;
      }
    }
  }

  private int findCeilingPosition(WorldGenLevel world, int x, int z, long pathSeed,
                                  int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {

    if (x < chunkMinX - 1 || x > chunkMaxX + 1 || z < chunkMinZ - 1 || z > chunkMaxZ + 1) {
      return -1;
    }
    Random ceilRand = new Random(pathSeed ^ 0xCE116L);
    for (int attempt = 0; attempt < 10; attempt++) {
      int y = 80 + ceilRand.nextInt(120);
      BlockPos checkPos = new BlockPos(x, y, z);
      if (world.getBlockState(checkPos).isSolid() &&
          world.getBlockState(checkPos.below()).isAir()) {
        return y;
      }
    }
    return -1;
  }

  private boolean shouldPlaceBlock(WorldGenLevel world, BlockPos pos) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir() ||
        existing.is(BlockTags.LEAVES) ||
        existing.is(Blocks.SNOW) ||
        existing.is(Blocks.VINE) ||
        existing.is(Blocks.WATER)) {
      return true;
    }

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
      case 0 -> Blocks.SCULK.defaultBlockState();
      case 1 -> Blocks.DEEPSLATE.defaultBlockState();
      case 2 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
      case 3 -> Blocks.BONE_BLOCK.defaultBlockState();
      case 4 -> Blocks.DRIPSTONE_BLOCK.defaultBlockState();
      case 5 -> Blocks.BASALT.defaultBlockState();
      case 6 -> Blocks.BLACKSTONE.defaultBlockState();
      default -> Blocks.SOUL_SOIL.defaultBlockState();
    };
  }

  private BlockState getDecorationBlock(BlockState baseBlock, Random random) {
    if (random.nextInt(3) == 0) {
      int choice = random.nextInt(6);
      return switch (choice) {
        case 0 -> Blocks.COBWEB.defaultBlockState();
        case 1 -> Blocks.CHAIN.defaultBlockState();
        case 2 -> Blocks.SCULK_VEIN.defaultBlockState();
        case 3 -> Blocks.SOUL_LANTERN.defaultBlockState();
        case 4 -> Blocks.GLOW_LICHEN.defaultBlockState();
        default -> Blocks.CAVE_VINES.defaultBlockState();
      };
    }
    return baseBlock;
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:vertical_tendrils";
  }
}
