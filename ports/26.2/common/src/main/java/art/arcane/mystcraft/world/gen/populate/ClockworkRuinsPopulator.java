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
 * Generates ruins of ancient mechanical structures. Partially collapsed
 * frameworks of copper and iron blocks with piston "gears", redstone block
 * accents, and lightning rod antennas. Some sections are oxidized (weathered
 * copper), others still gleam. Suggests a civilization that built thinking
 * machines long before the current age.
 */
public class ClockworkRuinsPopulator implements IPopulate {

  private static final float DEFAULT_SPAWN_CHANCE = 0.03f;
  private static final int DEFAULT_COUNT = 1;
  private static final int MIN_SIZE = 8;
  private static final int MAX_SIZE = 18;

  private static final BlockState COPPER = Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState();
  private static final BlockState EXPOSED_COPPER = Blocks.COPPER_BLOCK.weathering().exposed().defaultBlockState();
  private static final BlockState WEATHERED_COPPER = Blocks.COPPER_BLOCK.weathering().weathered().defaultBlockState();
  private static final BlockState OXIDIZED_COPPER = Blocks.COPPER_BLOCK.weathering().oxidized().defaultBlockState();
  private static final BlockState CUT_COPPER = Blocks.CUT_COPPER.weathering().unaffected().defaultBlockState();
  private static final BlockState IRON = Blocks.IRON_BLOCK.defaultBlockState();
  private static final BlockState PISTON = Blocks.PISTON.defaultBlockState();
  private static final BlockState STICKY_PISTON = Blocks.STICKY_PISTON.defaultBlockState();
  private static final BlockState REDSTONE_BLOCK = Blocks.REDSTONE_BLOCK.defaultBlockState();
  private static final BlockState LIGHTNING_ROD = Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState();
  private static final BlockState CHAIN = Blocks.IRON_CHAIN.defaultBlockState();
  private static final BlockState IRON_BARS = Blocks.IRON_BARS.defaultBlockState();
  private static final BlockState HEAVY_WEIGHTED_PLATE = Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState();
  private static final BlockState ANVIL = Blocks.ANVIL.defaultBlockState();
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();

  private final long seed;
  private final float spawnChance;
  private final int count;

  public ClockworkRuinsPopulator(long seed) {
    this(seed, null);
  }

  public ClockworkRuinsPopulator(long seed, JsonObject params) {
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

      if (surfaceY <= world.getMinY() + 5 || surfaceY >= world.getMaxY() - MAX_SIZE - 10) {
        continue;
      }

      BlockState ground = world.getBlockState(new BlockPos(x, surfaceY, z));
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES)) {
        continue;
      }

      int size = MIN_SIZE + random.nextInt(MAX_SIZE - MIN_SIZE + 1);
      generateRuin(world, chunkPos, x, surfaceY, z, size, random);
    }
  }

  private void generateRuin(WorldGenLevel world, BlockPos chunkPos,
                            int cx, int surfaceY, int cz, int size, RandomSource random) {
    int halfSize = size / 2;
    int height = size + random.nextInt(6);

    long oxidHash = positionHash(seed, cx, surfaceY, cz);
    float oxidLevel = hashFloat(oxidHash);

    for (int dx = -halfSize; dx <= halfSize; dx++) {
      for (int dz = -halfSize; dz <= halfSize; dz++) {
        BlockPos foundPos = new BlockPos(cx + dx, surfaceY, cz + dz);
        if (!isInWritableArea(foundPos, chunkPos)) continue;
        long foundHash = positionHash(seed, cx + dx, surfaceY, cz + dz);
        float foundRoll = hashFloat(foundHash);
        if (foundRoll < 0.7f) {
          world.setBlock(foundPos, foundRoll < 0.3f ? IRON : pickCopper(oxidLevel, foundHash), 2);
        }
      }
    }

    int[][] corners = {
        {cx - halfSize, cz - halfSize},
        {cx + halfSize, cz - halfSize},
        {cx - halfSize, cz + halfSize},
        {cx + halfSize, cz + halfSize}
    };

    for (int[] corner : corners) {
      long cornerHash = positionHash(seed ^ 0xC047L, corner[0], surfaceY, corner[1]);
      float survivalChance = hashFloat(cornerHash);
      int pillarHeight = (int) (height * (0.3f + survivalChance * 0.7f));

      for (int dy = 1; dy <= pillarHeight; dy++) {
        BlockPos pillarPos = new BlockPos(corner[0], surfaceY + dy, corner[1]);
        if (!isInWritableArea(pillarPos, chunkPos)) continue;

        long damageHash = positionHash(seed ^ 0xDA5EL, corner[0], surfaceY + dy, corner[1]);
        if (hashFloat(damageHash) < 0.15f) continue;

        world.setBlock(pillarPos, pickCopper(oxidLevel, damageHash), 2);
      }

      if (survivalChance > 0.5f) {
        BlockPos rodPos = new BlockPos(corner[0], surfaceY + pillarHeight + 1, corner[1]);
        if (isInWritableArea(rodPos, chunkPos)) {
          world.setBlock(rodPos, LIGHTNING_ROD, 2);
        }
      }
    }

    int beamInterval = 4 + random.nextInt(3);
    for (int beamY = beamInterval; beamY < height; beamY += beamInterval) {

      for (int dx = -halfSize; dx <= halfSize; dx++) {
        for (int side = -1; side <= 1; side += 2) {
          int bz = cz + side * halfSize;
          BlockPos beamPos = new BlockPos(cx + dx, surfaceY + beamY, bz);
          if (!isInWritableArea(beamPos, chunkPos)) continue;
          long beamHash = positionHash(seed ^ 0xBEAFL, cx + dx, surfaceY + beamY, bz);
          if (hashFloat(beamHash) < 0.3f) continue;
          world.setBlock(beamPos, IRON, 2);
        }
      }

      for (int dz = -halfSize; dz <= halfSize; dz++) {
        for (int side = -1; side <= 1; side += 2) {
          int bx = cx + side * halfSize;
          BlockPos beamPos = new BlockPos(bx, surfaceY + beamY, cz + dz);
          if (!isInWritableArea(beamPos, chunkPos)) continue;
          long beamHash = positionHash(seed ^ 0xBEAFL, bx, surfaceY + beamY, cz + dz);
          if (hashFloat(beamHash) < 0.3f) continue;
          world.setBlock(beamPos, IRON, 2);
        }
      }
    }

    int machineCount = 3 + random.nextInt(5);
    for (int m = 0; m < machineCount; m++) {
      int mx = cx + random.nextInt(size) - halfSize;
      int mz = cz + random.nextInt(size) - halfSize;
      int my = surfaceY + 1 + random.nextInt(Math.max(1, height / 2));

      BlockPos machinePos = new BlockPos(mx, my, mz);
      if (!isInWritableArea(machinePos, chunkPos)) continue;

      long machineHash = positionHash(seed ^ 0xAC31L, mx, my, mz);
      float machineRoll = hashFloat(machineHash);

      if (machineRoll < 0.2f) {
        world.setBlock(machinePos, PISTON, 2);
      } else if (machineRoll < 0.35f) {
        world.setBlock(machinePos, STICKY_PISTON, 2);
      } else if (machineRoll < 0.5f) {
        world.setBlock(machinePos, REDSTONE_BLOCK, 2);
      } else if (machineRoll < 0.7f) {
        world.setBlock(machinePos, IRON_BARS, 2);
      } else if (machineRoll < 0.85f) {
        world.setBlock(machinePos, CHAIN, 2);
      } else {
        world.setBlock(machinePos, HEAVY_WEIGHTED_PLATE, 2);
      }
    }

    int gearY = surfaceY + height / 2;
    BlockPos corePos = new BlockPos(cx, gearY, cz);
    if (isInWritableArea(corePos, chunkPos)) {
      world.setBlock(corePos, REDSTONE_BLOCK, 2);
    }
    for (int gx = -1; gx <= 1; gx++) {
      for (int gz = -1; gz <= 1; gz++) {
        if (gx == 0 && gz == 0) continue;
        BlockPos gearPos = new BlockPos(cx + gx, gearY, cz + gz);
        if (isInWritableArea(gearPos, chunkPos)) {
          long gearHash = positionHash(seed, cx + gx, gearY, cz + gz);
          if (hashFloat(gearHash) > 0.2f) {
            world.setBlock(gearPos, hashFloat(gearHash) < 0.6f ? PISTON : STICKY_PISTON, 2);
          }
        }
      }
    }

    for (int r = 0; r < size; r++) {
      int rx = cx + random.nextInt(size + 4) - halfSize - 2;
      int rz = cz + random.nextInt(size + 4) - halfSize - 2;
      int ry = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rx, rz);
      BlockPos rubblePos = new BlockPos(rx, ry, rz);
      if (!isInWritableArea(rubblePos, chunkPos)) continue;
      if (!world.getBlockState(rubblePos).isAir()) continue;

      long rubbleHash = positionHash(seed ^ 0x3ABBL, rx, ry, rz);
      float rubbleRoll = hashFloat(rubbleHash);
      if (rubbleRoll < 0.3f) {
        world.setBlock(rubblePos, pickCopper(oxidLevel, rubbleHash), 2);
      } else if (rubbleRoll < 0.5f) {
        world.setBlock(rubblePos, IRON, 2);
      }
    }
  }

  private BlockState pickCopper(float oxidLevel, long hash) {
    float roll = hashFloat(hash ^ 0xC0BBL);
    float adjusted = roll * oxidLevel;
    if (adjusted < 0.15f) {
      return COPPER;
    } else if (adjusted < 0.35f) {
      return CUT_COPPER;
    } else if (adjusted < 0.55f) {
      return EXPOSED_COPPER;
    } else if (adjusted < 0.75f) {
      return WEATHERED_COPPER;
    } else {
      return OXIDIZED_COPPER;
    }
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:clockwork_ruins";
  }
}
