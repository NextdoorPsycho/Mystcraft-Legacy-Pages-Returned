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
 * Eyeblight populator that generates clusters of "watching" eye formations
 * embedded in the ground, staring upward. Eyes are flat circular structures
 * with concentric rings of obsidian/blackstone, black concrete, a colored
 * iris, and a glowing pupil center. Surrounding decorations include sculk
 * veins and occasional sculk sensors.
 * Simple single-chunk populator.
 */
public class EyeblightPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.04f;
  private static final int DEFAULT_COUNT = 1;
  private final long seed;
  private final float spawnChance;
  private final int count;

  public EyeblightPopulator(long seed) {
    this(seed, null);
  }

  public EyeblightPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.spawnChance = PopulatorConfig.chanceFrom(params, DEFAULT_SPAWN_CHANCE, 0);
    this.count = PopulatorConfig.getInt(params, "count", DEFAULT_COUNT);
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
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    if (random.nextFloat() >= spawnChance) {
      return;
    }

    int chunkMinX = chunkPos.getX();
    int chunkMinZ = chunkPos.getZ();

    for (int i = 0; i < count; i++) {
      int x = chunkMinX + random.nextInt(16);
      int z = chunkMinZ + random.nextInt(16);
      int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

      if (surfaceY <= world.getMinBuildHeight() + 2 || surfaceY >= world.getMaxBuildHeight() - 2) {
        continue;
      }

      BlockPos surfacePos = new BlockPos(x, surfaceY, z);
      BlockState groundBlock = world.getBlockState(surfacePos);
      if (!groundBlock.isSolid()) {
        continue;
      }

      // Size variant: 40% small (3x3), 40% medium (5x5), 20% large (7x7)
      int sizeRoll = random.nextInt(100);
      int radius;
      if (sizeRoll < 40) {
        radius = 1; // 3x3
      } else if (sizeRoll < 80) {
        radius = 2; // 5x5
      } else {
        radius = 3; // 7x7
      }

      // Material choices for this eye
      boolean useWarpedIris = random.nextBoolean();
      boolean useShroomlightPupil = random.nextBoolean();

      BlockState irisBlock = useWarpedIris
          ? Blocks.WARPED_WART_BLOCK.defaultBlockState()
          : Blocks.LAPIS_BLOCK.defaultBlockState();
      BlockState pupilBlock = useShroomlightPupil
          ? Blocks.SHROOMLIGHT.defaultBlockState()
          : Blocks.SEA_LANTERN.defaultBlockState();
      BlockState middleBlock = Blocks.BLACK_CONCRETE.defaultBlockState();

      // Ring radius thresholds (distance from center)
      double outerRadius = radius;
      double middleRadius = radius * 0.75;
      double irisRadius = radius * 0.5;
      double innerRadius = radius * 0.2;

      // Build the eye structure
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          double dist = Math.sqrt((double) dx * dx + (double) dz * dz);

          if (dist > outerRadius) {
            continue;
          }

          int bx = x + dx;
          int bz = z + dz;
          BlockPos eyePos = new BlockPos(bx, surfaceY, bz);

          if (!isInWritableArea(eyePos, chunkPos)) {
            continue;
          }

          // Determine which ring this block falls in
          BlockState blockToPlace;
          if (dist <= innerRadius) {
            // Pupil center
            blockToPlace = pupilBlock;
          } else if (dist <= irisRadius) {
            // Iris
            blockToPlace = irisBlock;
          } else if (dist <= middleRadius) {
            // Middle ring: black concrete
            blockToPlace = middleBlock;
          } else {
            // Outer ring: obsidian or blackstone alternating by position hash
            long ringHash = positionHash(seed, bx, surfaceY, bz);
            boolean useObsidian = (ringHash & 1L) == 0;
            blockToPlace = useObsidian
                ? Blocks.OBSIDIAN.defaultBlockState()
                : Blocks.BLACKSTONE.defaultBlockState();
          }

          // Replace terrain block with eye material
          BlockState existing = world.getBlockState(eyePos);
          if (existing.isSolid() || existing.is(BlockTags.DIRT) || existing.is(Blocks.GRASS_BLOCK)
              || existing.is(Blocks.SAND) || existing.is(Blocks.GRAVEL)) {
            world.setBlock(eyePos, blockToPlace, 2);
          }

          // Shallow bowl deformation: dig 1-2 blocks below near the center
          if (dist <= middleRadius) {
            int bowlDepth = dist <= innerRadius ? 2 : 1;
            for (int depth = 1; depth <= bowlDepth; depth++) {
              BlockPos belowPos = new BlockPos(bx, surfaceY - depth, bz);
              if (isInWritableArea(belowPos, chunkPos)) {
                BlockState belowExisting = world.getBlockState(belowPos);
                if (belowExisting.isSolid()) {
                  world.setBlock(belowPos, blockToPlace, 2);
                }
              }
            }
          }
        }
      }

      // Decorations: sculk_vein radiating outward from eye edge (3-5 positions)
      int vineCount = 3 + random.nextInt(3);
      for (int v = 0; v < vineCount; v++) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        int decorDist = radius + 1 + random.nextInt(3);
        int vx = x + (int) Math.round(Math.cos(angle) * decorDist);
        int vz = z + (int) Math.round(Math.sin(angle) * decorDist);
        int vy = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, vx, vz);

        BlockPos vinePos = new BlockPos(vx, vy, vz);
        if (isInWritableArea(vinePos, chunkPos) && world.getBlockState(vinePos).isAir()) {
          world.setBlock(vinePos, Blocks.SCULK_VEIN.defaultBlockState(), 2);
        }
      }

      // Decorations: occasional sculk_sensor (1-2 per eye)
      int sensorCount = 1 + random.nextInt(2);
      for (int s = 0; s < sensorCount; s++) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        int sensorDist = radius + random.nextInt(2);
        int sx = x + (int) Math.round(Math.cos(angle) * sensorDist);
        int sz = z + (int) Math.round(Math.sin(angle) * sensorDist);
        int sy = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);

        BlockPos sensorPos = new BlockPos(sx, sy, sz);
        if (isInWritableArea(sensorPos, chunkPos) && world.getBlockState(sensorPos).isAir()) {
          BlockState below = world.getBlockState(sensorPos.below());
          if (below.isSolid()) {
            world.setBlock(sensorPos, Blocks.SCULK_SENSOR.defaultBlockState(), 2);
          }
        }
      }
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:eyeblight";
  }
}
