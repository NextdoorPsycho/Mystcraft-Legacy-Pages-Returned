package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Populates deep underground areas with sculk features. Generates sculk blocks,
 * sculk veins, sculk sensors, and sculk shriekers. Only generates at very low Y
 * levels to create an ominous deep dark atmosphere.
 */
public class DeepDarkPopulator implements IPopulate {

  private static final int DEFAULT_ATTEMPTS_PER_CHUNK = 10;
  private static final int DEFAULT_MAX_Y = 0;
  private final long seed;
  private final int attemptsPerChunk;
  private final int maxY;
  private final float spawnChance;

  public DeepDarkPopulator(long seed) {
    this(seed, null);
  }

  public DeepDarkPopulator(long seed, JsonObject params) {
    this.seed = seed;
    this.attemptsPerChunk = PopulatorConfig.getInt(params, "attempts", DEFAULT_ATTEMPTS_PER_CHUNK);
    this.maxY = PopulatorConfig.getInt(params, "max_y", DEFAULT_MAX_Y);
    this.spawnChance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
  }

  @Override
  public void populate(WorldGenLevel world, RandomSource random, BlockPos chunkPos) {
    int chunkX = chunkPos.getX();
    int chunkZ = chunkPos.getZ();

    if (random.nextFloat() > spawnChance) {
      return;
    }
    for (int attempt = 0; attempt < attemptsPerChunk; attempt++) {
      int x = chunkX + random.nextInt(16);
      int y = world.getMinY() + random.nextInt(Math.max(1, maxY - world.getMinY() + 1));
      int z = chunkZ + random.nextInt(16);

      BlockPos pos = new BlockPos(x, y, z);

      if (!world.getBlockState(pos).isAir()) {
        continue;
      }

      if (!isInCave(world, pos)) {
        continue;
      }

      int featureType = random.nextInt(100);

      if (featureType < 50) {
        tryPlaceSculkPatch(world, random, pos);
      } else if (featureType < 85) {
        tryPlaceSculkVeins(world, random, pos);
      } else if (featureType < 95) {
        tryPlaceSculkSensor(world, random, pos);
      } else {
        tryPlaceSculkShrieker(world, random, pos);
      }
    }
  }

  private void tryPlaceSculkPatch(WorldGenLevel world, RandomSource random, BlockPos center) {
    BlockPos below = center.below();
    BlockState belowState = world.getBlockState(below);

    if (!canReplaceSculk(belowState)) {
      return;
    }

    world.setBlock(below, Blocks.SCULK.defaultBlockState(), 2);

    int spread = 3 + random.nextInt(5);
    for (int i = 0; i < spread; i++) {
      int offsetX = random.nextInt(5) - 2;
      int offsetZ = random.nextInt(5) - 2;
      BlockPos sculkPos = below.offset(offsetX, 0, offsetZ);

      if (world.getBlockState(sculkPos.above()).isAir() && canReplaceSculk(world.getBlockState(sculkPos))) {
        world.setBlock(sculkPos, Blocks.SCULK.defaultBlockState(), 2);

        if (random.nextFloat() < 0.05f) {
          world.setBlock(sculkPos.above(), Blocks.SCULK_CATALYST.defaultBlockState(), 2);
        }
      }
    }
  }

  private void tryPlaceSculkVeins(WorldGenLevel world, RandomSource random, BlockPos center) {

    for (Direction direction : Direction.values()) {
      if (random.nextFloat() > 0.4f) {
        continue;
      }

      BlockPos adjacentPos = center.relative(direction);
      BlockState adjacentState = world.getBlockState(adjacentPos);

      if (!adjacentState.isSolid()) {
        continue;
      }

      BlockState veinState = Blocks.SCULK_VEIN.defaultBlockState();

      Direction opposite = direction.getOpposite();
      veinState = veinState.setValue(SculkVeinBlock.getFaceProperty(opposite), true);

      world.setBlock(center, veinState, 2);
      break;
    }
  }

  private void tryPlaceSculkSensor(WorldGenLevel world, RandomSource random, BlockPos pos) {
    BlockPos below = pos.below();
    BlockState belowState = world.getBlockState(below);

    if (!(belowState.is(Blocks.SCULK) || belowState.isSolid())) {
      return;
    }

    world.setBlock(pos, Blocks.SCULK_SENSOR.defaultBlockState(), 2);
  }

  private void tryPlaceSculkShrieker(WorldGenLevel world, RandomSource random, BlockPos pos) {
    BlockPos below = pos.below();
    BlockState belowState = world.getBlockState(below);

    if (!(belowState.is(Blocks.SCULK) || belowState.isSolid())) {
      return;
    }

    world.setBlock(pos, Blocks.SCULK_SHRIEKER.defaultBlockState(), 2);
  }

  private boolean isInCave(WorldGenLevel world, BlockPos pos) {
    int stoneCount = 0;
    int airCount = 0;

    for (Direction direction : Direction.values()) {
      BlockState state = world.getBlockState(pos.relative(direction));
      if (state.isAir()) {
        airCount++;
      } else if (state.isSolid()) {
        stoneCount++;
      }
    }

    return airCount >= 2 && stoneCount >= 2;
  }

  private boolean canReplaceSculk(BlockState state) {
    return state.is(Blocks.STONE) ||
        state.is(Blocks.DEEPSLATE) ||
        state.is(Blocks.GRANITE) ||
        state.is(Blocks.DIORITE) ||
        state.is(Blocks.ANDESITE) ||
        state.is(Blocks.DIRT) ||
        state.is(Blocks.GRAVEL) ||
        state.is(Blocks.TUFF);
  }

  @Override
  public String getIdentifier() {
    return "mystcraft:deep_dark";
  }
}
