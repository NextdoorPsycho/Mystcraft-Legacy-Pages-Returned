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
 * Generates massive coral reef formations growing on dry land, towering
 * 15-40 blocks high. Made of living coral blocks (brain, tube, bubble,
 * fire, horn) with coral fans on surfaces and sea pickles for bioluminescence.
 * Upper portions have dead coral variants mixed in as the formations slowly
 * die in the air. A surreal ocean transplanted onto land.
 */
public class LivingCoralTowersPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.04f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_HEIGHT = 15;
  private static final int MAX_HEIGHT = 40;
  private static final int MIN_BASE_RADIUS = 4;
  private static final int MAX_BASE_RADIUS = 8;
  private static final int BRANCHES = 3;

  private static final BlockState[] LIVING_CORALS = {
      Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
      Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
      Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState(),
      Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
      Blocks.HORN_CORAL_BLOCK.defaultBlockState()
  };

  private static final BlockState[] DEAD_CORALS = {
      Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState(),
      Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState(),
      Blocks.DEAD_BUBBLE_CORAL_BLOCK.defaultBlockState(),
      Blocks.DEAD_FIRE_CORAL_BLOCK.defaultBlockState(),
      Blocks.DEAD_HORN_CORAL_BLOCK.defaultBlockState()
  };

  private static final BlockState SEA_PICKLE = Blocks.SEA_PICKLE.defaultBlockState();
  private static final BlockState PRISMARINE = Blocks.PRISMARINE.defaultBlockState();
  private static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public LivingCoralTowersPopulator(long seed) {
    this(seed, null);
  }

  public LivingCoralTowersPopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinBuildHeight() + 5 || surfaceY >= world.getMaxBuildHeight() - MAX_HEIGHT - 5) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int towerHeight = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
      int baseRadius = MIN_BASE_RADIUS + random.nextInt(MAX_BASE_RADIUS - MIN_BASE_RADIUS + 1);

      // Pick dominant coral type for this tower
      long coralSeed = positionHash(seed, x, surfaceY, z);
      int dominantCoral = (int) ((coralSeed >>> 8) & 0xFF) % LIVING_CORALS.length;

      generateTower(world, chunkPos, x, surfaceY, z, towerHeight, baseRadius, dominantCoral);
    }
  }

  private void generateTower(WorldGenLevel world, BlockPos chunkPos,
                              int cx, int surfaceY, int cz,
                              int height, int baseRadius, int dominantCoral) {
    // Main trunk: tapers from base to tip
    for (int dy = 0; dy < height; dy++) {
      double progress = (double) dy / height;
      // Radius tapers: wide at base, narrow at top, with some bulging
      double bulge = Math.sin(progress * Math.PI * 2.5) * 0.3;
      int radius = Math.max(1, (int) ((baseRadius * (1.0 - progress * 0.7)) + bulge * baseRadius));

      int by = surfaceY + dy;

      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          double distSq = dx * dx + dz * dz;
          if (distSq > (double) radius * radius) continue;

          int bx = cx + dx;
          int bz = cz + dz;
          BlockPos pos = new BlockPos(bx, by, bz);
          if (!isInWritableArea(pos, chunkPos)) continue;

          // Pick coral type
          long blockHash = positionHash(seed, bx, by, bz);
          float roll = hashFloat(blockHash);
          int coralIdx = roll < 0.6f ? dominantCoral : ((int) ((blockHash >>> 24) & 0xFF) % LIVING_CORALS.length);

          // Upper portions have dead coral mixed in
          boolean dead = false;
          if (progress > 0.5) {
            float deathChance = (float) ((progress - 0.5) * 2.0); // 0 at 50%, 1 at 100%
            long deathHash = positionHash(seed ^ 0xDEADL, bx, by, bz);
            dead = hashFloat(deathHash) < deathChance;
          }

          BlockState coralBlock;
          if (roll < 0.03f) {
            coralBlock = SEA_LANTERN; // Bioluminescent core
          } else if (roll < 0.06f) {
            coralBlock = PRISMARINE;
          } else {
            coralBlock = dead ? DEAD_CORALS[coralIdx] : LIVING_CORALS[coralIdx];
          }

          world.setBlock(pos, coralBlock, 2);

          // Sea pickles on top surface
          if (dy > 0 && dx * dx + dz * dz >= (radius - 1) * (radius - 1)) {
            BlockPos picklePos = pos.above();
            if (isInWritableArea(picklePos, chunkPos) && world.getBlockState(picklePos).isAir()) {
              long pickleHash = positionHash(seed ^ 0xB1C4L, bx, by + 1, bz);
              if (hashFloat(pickleHash) < 0.1f) {
                world.setBlock(picklePos, SEA_PICKLE, 2);
              }
            }
          }
        }
      }
    }

    // Branches: smaller coral growths branching off the main trunk
    int branchCount = BRANCHES + (int) (hashFloat(positionHash(seed ^ 0xB4A4L, cx, surfaceY, cz)) * 3);
    for (int b = 0; b < branchCount; b++) {
      long branchSeed = positionHash(seed ^ 0xB4A4L, cx + b, surfaceY, cz + b);
      double branchAngle = hashFloat(branchSeed) * Math.PI * 2.0;
      int branchStartY = surfaceY + (int) (height * (0.3 + hashFloat(positionHash(branchSeed, 0, 0, 0)) * 0.4));
      int branchLength = 5 + (int) (hashFloat(positionHash(branchSeed, 1, 0, 0)) * 10);
      int branchRadius = 1 + (int) (hashFloat(positionHash(branchSeed, 2, 0, 0)) * 2);

      double bDx = Math.cos(branchAngle);
      double bDz = Math.sin(branchAngle);

      for (int step = 0; step < branchLength; step++) {
        double t = (double) step / branchLength;
        int bx = cx + (int) Math.round(bDx * (baseRadius + step));
        int bz = cz + (int) Math.round(bDz * (baseRadius + step));
        int by = branchStartY + (int) (step * 0.5); // Slight upward angle

        int localRadius = Math.max(1, (int) (branchRadius * (1.0 - t)));

        for (int dx = -localRadius; dx <= localRadius; dx++) {
          for (int dz = -localRadius; dz <= localRadius; dz++) {
            if (dx * dx + dz * dz > localRadius * localRadius) continue;
            BlockPos branchPos = new BlockPos(bx + dx, by, bz + dz);
            if (!isInWritableArea(branchPos, chunkPos)) continue;

            long blockHash = positionHash(seed, bx + dx, by, bz + dz);
            int coralIdx = (int) ((blockHash >>> 24) & 0xFF) % LIVING_CORALS.length;
            boolean dead = t > 0.7 || (double) (by - surfaceY) / height > 0.6;
            world.setBlock(branchPos, dead ? DEAD_CORALS[coralIdx] : LIVING_CORALS[coralIdx], 2);
          }
        }
      }
    }

    // Prismarine base ring
    for (int dx = -(baseRadius + 1); dx <= baseRadius + 1; dx++) {
      for (int dz = -(baseRadius + 1); dz <= baseRadius + 1; dz++) {
        int distSq = dx * dx + dz * dz;
        if (distSq > (baseRadius + 1) * (baseRadius + 1) || distSq < baseRadius * baseRadius) continue;
        BlockPos ringPos = new BlockPos(cx + dx, surfaceY, cz + dz);
        if (isInWritableArea(ringPos, chunkPos)) {
          world.setBlock(ringPos, PRISMARINE, 2);
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:living_coral_towers";
  }
}
