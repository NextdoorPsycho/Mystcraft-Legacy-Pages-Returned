package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Block shuffle populator that remaps blocks after generation.
 * Supports terrain-only and global shuffle modes.
 * 1.18.2 version - uses Registry.BLOCK instead of BuiltInRegistries.BLOCK
 * and state.getMaterial().isSolid() instead of state.isSolid().
 */
public class BlockShufflePopulator implements IPopulate {

  private final long seed;
  private final Mode mode;
  private final Map<Block, BlockState> mapping;
  private final Integer minYOverride;
  private final Integer maxYOverride;
  private final float chance;

  public BlockShufflePopulator(long seed, Mode mode) {
    this(seed, mode, null);
  }

  public BlockShufflePopulator(long seed, Mode mode, JsonObject params) {
    this.seed = seed;
    this.mode = mode == null ? Mode.TERRAIN : mode;
    this.mapping = buildMapping(this.mode, seed);
    this.minYOverride = PopulatorConfig.getOptionalInt(params, "min_y", PopulatorConfig.UNSET_INT);
    this.maxYOverride = PopulatorConfig.getOptionalInt(params, "max_y", PopulatorConfig.UNSET_INT);
    this.chance = PopulatorConfig.chanceFrom(params, 1.0f, 0);
  }

  private static Map<Block, BlockState> buildMapping(Mode mode, long seed) {
    List<Block> candidates = mode == Mode.GLOBAL
        ? collectGlobalCandidates()
        : collectTerrainCandidates();

    if (candidates.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Block> shuffled = new ArrayList<>(candidates);
    Random rand = new Random(seed ^ 0xB10C_5A71L ^ (mode == Mode.GLOBAL ? 0x55AA : 0x33CC));
    Collections.shuffle(shuffled, rand);

    Map<Block, BlockState> result = new HashMap<>();
    int size = candidates.size();
    for (int i = 0; i < size; i++) {
      Block from = candidates.get(i);
      Block to = shuffled.get(i);
      if (from == to) {
        to = shuffled.get((i + 1) % size);
      }
      result.put(from, to.defaultBlockState());
    }
    return result;
  }

  private static List<Block> collectTerrainCandidates() {
    List<Block> blocks = new ArrayList<>();
    // 1.18.2: Use Registry.BLOCK instead of BuiltInRegistries.BLOCK
    for (Block block : Registry.BLOCK) {
      BlockState state = block.defaultBlockState();
      if (state.isAir() || !state.getFluidState().isEmpty()) continue;
      if (!state.getMaterial().isSolid()) continue;
      if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL) || state.is(Blocks.NETHER_PORTAL)) continue;

      if (state.is(BlockTags.BASE_STONE_OVERWORLD)
          || state.is(BlockTags.BASE_STONE_NETHER)
          || state.is(BlockTags.DIRT)
          || state.is(BlockTags.TERRACOTTA)
          || state.is(BlockTags.SAND)
          || state.is(Blocks.GRAVEL)
          || state.is(Blocks.CLAY)
          || state.is(Blocks.RED_SAND)
          || state.is(Blocks.RED_SANDSTONE)
          || state.is(Blocks.SANDSTONE)
          || state.is(Blocks.TUFF)
          || state.is(Blocks.CALCITE)) {
        blocks.add(block);
      }
    }
    return blocks;
  }

  private static List<Block> collectGlobalCandidates() {
    List<Block> blocks = new ArrayList<>();
    // 1.18.2: Use Registry.BLOCK instead of BuiltInRegistries.BLOCK
    for (Block block : Registry.BLOCK) {
      BlockState state = block.defaultBlockState();
      if (state.isAir() || !state.getFluidState().isEmpty()) continue;
      if (!state.getMaterial().isSolid()) continue;
      if (state.hasBlockEntity()) continue;
      if (!state.canOcclude()) continue;
      if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME)
          || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_GATEWAY)) {
        continue;
      }
      blocks.add(block);
    }
    return blocks;
  }

  @Override
  public void populate(WorldGenLevel world, Random random, BlockPos chunkPos) {
    if (random.nextFloat() > chance) {
      return;
    }
    int minY = minYOverride != null ? minYOverride : world.getMinBuildHeight();
    int maxY = maxYOverride != null ? maxYOverride : world.getMaxBuildHeight();

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        int wx = chunkPos.getX() + x;
        int wz = chunkPos.getZ() + z;
        for (int y = minY; y < maxY; y++) {
          pos.set(wx, y, wz);
          BlockState state = world.getBlockState(pos);
          if (shouldSkip(state)) {
            continue;
          }
          BlockState replacement = mapping.get(state.getBlock());
          if (replacement != null && replacement != state) {
            world.setBlock(pos, replacement, 2);
          }
        }
      }
    }
  }

  private boolean shouldSkip(BlockState state) {
    if (state.isAir()) return true;
    if (!state.getFluidState().isEmpty()) return true;
    if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME)
        || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_GATEWAY)) {
      return true;
    }
    return state.hasBlockEntity();
  }

  @Override
  public String getIdentifier() {
    return mode == Mode.GLOBAL ? "mystcraft:block_shuffle_global" : "mystcraft:block_shuffle_terrain";
  }

  public enum Mode {
    TERRAIN,
    GLOBAL
  }
}
