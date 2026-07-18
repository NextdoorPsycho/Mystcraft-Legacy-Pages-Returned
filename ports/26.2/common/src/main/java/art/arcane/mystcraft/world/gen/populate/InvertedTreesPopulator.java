package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Inverted trees populator that generates upside-down trees floating in the
 * sky. Trees hang from an invisible ceiling with roots reaching skyward and
 * leaf canopies at the bottom. Decorated with chains, lanterns, and weeping
 * vines trailing below the canopy.
 */
public class InvertedTreesPopulator implements IPopulate {

  private static final int DEFAULT_COUNT = 1;
  private static final float DEFAULT_SPAWN_CHANCE = 0.06f;
  private static final int DEFAULT_MIN_TRUNK = 8;
  private static final int DEFAULT_MAX_TRUNK = 18;
  private static final int DEFAULT_MIN_ROOTS = 3;
  private static final int DEFAULT_MAX_ROOTS = 5;
  private static final int DEFAULT_MIN_ROOT_LEN = 3;
  private static final int DEFAULT_MAX_ROOT_LEN = 6;
  private static final int DEFAULT_MIN_CANOPY_RADIUS = 3;
  private static final int DEFAULT_MAX_CANOPY_RADIUS = 5;
  private final long seed;
  private final int count;
  private final float spawnChance;
  private final int minTrunk;
  private final int maxTrunk;
  private final int minRoots;
  private final int maxRoots;
  private final int minRootLen;
  private final int maxRootLen;
  private final int minCanopyRadius;
  private final int maxCanopyRadius;
  private BlockPos currentChunkPos;

  public InvertedTreesPopulator(long seed) {
    this(seed, null);
  }

  public InvertedTreesPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.minTrunk = Math.max(4, PopulatorConfig.getInt(params, "min_trunk", DEFAULT_MIN_TRUNK));
    this.maxTrunk = Math.max(this.minTrunk, PopulatorConfig.getInt(params, "max_trunk", DEFAULT_MAX_TRUNK));
    this.minRoots = Math.max(1, PopulatorConfig.getInt(params, "min_roots", DEFAULT_MIN_ROOTS));
    this.maxRoots = Math.max(this.minRoots, PopulatorConfig.getInt(params, "max_roots", DEFAULT_MAX_ROOTS));
    this.minRootLen = Math.max(1, PopulatorConfig.getInt(params, "min_root_length", DEFAULT_MIN_ROOT_LEN));
    this.maxRootLen = Math.max(this.minRootLen, PopulatorConfig.getInt(params, "max_root_length", DEFAULT_MAX_ROOT_LEN));
    this.minCanopyRadius = Math.max(2, PopulatorConfig.getInt(params, "min_canopy_radius", DEFAULT_MIN_CANOPY_RADIUS));
    this.maxCanopyRadius = Math.max(this.minCanopyRadius, PopulatorConfig.getInt(params, "max_canopy_radius", DEFAULT_MAX_CANOPY_RADIUS));
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
    this.currentChunkPos = chunkPos;

    for (int i = 0; i < count; i++) {
      if (random.nextFloat() >= spawnChance) {
        continue;
      }

      int x = chunkPos.getX() + random.nextInt(16);
      int z = chunkPos.getZ() + random.nextInt(16);

      long topHash = positionHash(seed, x, 0, z);
      int topY = 100 + (int) (Math.abs(topHash) % 60);

      int trunkLength = minTrunk + random.nextInt(maxTrunk - minTrunk + 1);

      int rootCount = minRoots + random.nextInt(maxRoots - minRoots + 1);
      int canopyRadius = minCanopyRadius + random.nextInt(maxCanopyRadius - minCanopyRadius + 1);

      boolean useCherryLeaves = random.nextBoolean();

      generateInvertedTree(world, random, x, topY, z, trunkLength, rootCount,
          canopyRadius, useCherryLeaves);
    }
  }

  private void generateInvertedTree(WorldGenLevel world, RandomSource random,
                                    int x, int topY, int z, int trunkLength,
                                    int rootCount, int canopyRadius,
                                    boolean useCherryLeaves) {
    BlockState logBlock = Blocks.DARK_OAK_LOG.defaultBlockState();
    BlockState rootBlock = Blocks.SPRUCE_LOG.defaultBlockState();
    BlockState leafBlock = useCherryLeaves
        ? Blocks.CHERRY_LEAVES.defaultBlockState()
        : Blocks.AZALEA_LEAVES.defaultBlockState();

    int trunkBottomY = topY - trunkLength;
    for (int dy = 0; dy < trunkLength; dy++) {
      BlockPos trunkPos = new BlockPos(x, topY - dy, z);
      if (isInWritableArea(trunkPos, currentChunkPos)) {
        setBlockIfAir(world, trunkPos, logBlock);
      }
    }

    long rootSeed = positionHash(seed ^ 0xA00D5L, x, topY, z);
    for (int r = 0; r < rootCount; r++) {
      long branchHash = positionHash(rootSeed, r, topY, x + z);
      int rootLen = minRootLen + (int) (Math.abs(branchHash) % (maxRootLen - minRootLen + 1));

      double angle = ((branchHash >>> 16) & 0xFFFFL) / (double) 0xFFFFL * Math.PI * 2.0;
      double spread = 0.3 + ((branchHash >>> 32) & 0xFFL) / 255.0 * 0.5;

      double currentX = x;
      double currentY = topY;
      double currentZ = z;

      BlockPos lastRootPos = null;
      for (int seg = 0; seg < rootLen; seg++) {
        currentX += Math.cos(angle) * spread;
        currentY += 1;
        currentZ += Math.sin(angle) * spread;

        BlockPos rootPos = new BlockPos((int) Math.floor(currentX), (int) currentY, (int) Math.floor(currentZ));
        if (isInWritableArea(rootPos, currentChunkPos)) {
          setBlockIfAir(world, rootPos, rootBlock);
        }
        lastRootPos = rootPos;
      }

      if (lastRootPos != null && r < 2) {
        placeHangingDecoration(world, lastRootPos);
      }
    }

    int canopyCenterY = trunkBottomY;
    for (int dx = -canopyRadius; dx <= canopyRadius; dx++) {
      for (int dy = -canopyRadius; dy <= canopyRadius; dy++) {
        for (int dz = -canopyRadius; dz <= canopyRadius; dz++) {
          double distSq = (double) dx * dx + (double) dy * dy + (double) dz * dz;
          if (distSq <= (double) canopyRadius * canopyRadius) {
            BlockPos leafPos = new BlockPos(x + dx, canopyCenterY + dy, z + dz);
            if (isInWritableArea(leafPos, currentChunkPos)) {
              setLeafIfAir(world, leafPos, leafBlock);
            }
          }
        }
      }
    }

    for (int dx = -canopyRadius; dx <= canopyRadius; dx++) {
      for (int dz = -canopyRadius; dz <= canopyRadius; dz++) {
        double distSq = (double) dx * dx + (double) dz * dz;
        if (distSq > (double) (canopyRadius - 1) * (canopyRadius - 1)) {
          continue;
        }

        long vineHash = positionHash(seed ^ 0xB1AE5L, x + dx, canopyCenterY, z + dz);
        if ((vineHash & 0x7) > 2) {
          continue;
        }

        int vineLength = 2 + (int) (Math.abs(vineHash >>> 8) % 3);
        int vineStartY = canopyCenterY - canopyRadius;
        for (int vy = 1; vy <= vineLength; vy++) {
          BlockPos vinePos = new BlockPos(x + dx, vineStartY - vy, z + dz);
          if (isInWritableArea(vinePos, currentChunkPos) && world.getBlockState(vinePos).isAir()) {
            world.setBlock(vinePos, Blocks.WEEPING_VINES.defaultBlockState(), 2);
          }
        }
      }
    }
  }

  private void placeHangingDecoration(WorldGenLevel world, BlockPos branchTip) {

    BlockPos chainPos = branchTip.below();
    if (isInWritableArea(chainPos, currentChunkPos) && world.getBlockState(chainPos).isAir()) {
      world.setBlock(chainPos, Blocks.IRON_CHAIN.defaultBlockState(), 2);

      BlockPos lanternPos = chainPos.below();
      if (isInWritableArea(lanternPos, currentChunkPos) && world.getBlockState(lanternPos).isAir()) {
        world.setBlock(lanternPos, Blocks.LANTERN.defaultBlockState(), 2);
      }
    }
  }

  private void setBlockIfAir(WorldGenLevel world, BlockPos pos, BlockState state) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir()) {
      world.setBlock(pos, state, 2);
    }
  }

  private void setLeafIfAir(WorldGenLevel world, BlockPos pos, BlockState leaves) {
    BlockState existing = world.getBlockState(pos);
    if (existing.isAir()) {
      world.setBlock(pos, leaves, 2);
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:inverted_trees";
  }
}
