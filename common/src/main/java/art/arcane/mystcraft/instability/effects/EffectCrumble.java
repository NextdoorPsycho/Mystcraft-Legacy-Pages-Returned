package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Environmental effect that causes blocks to crumble (turn to air or gravel).
 * Scales with instability: a single pass at low values, aggressive multi-pass at high.
 */
public class EffectCrumble implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.012f;
  private static final int MAX_CRUMBLE_PASSES = 8;

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {
    // Environmental: ramps 0.1 at instability 8, full at 80
    float intensity = Math.max(0.1f, Math.min(instability / 80.0f, 1.0f));

    if (level.random.nextFloat() >= BASE_CHANCE * intensity) {
      return;
    }

    int passes = Math.max(1, Math.round(MAX_CRUMBLE_PASSES * intensity));

    for (int pass = 0; pass < passes; pass++) {
      // Pick a random position in the chunk
      int x = chunk.getPos().getMinBlockX() + level.random.nextInt(16);
      int z = chunk.getPos().getMinBlockZ() + level.random.nextInt(16);
      int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

      // Try a few times to find a crumble-able block
      for (int attempt = 0; attempt < 8; attempt++) {
        BlockPos pos = new BlockPos(x, y - attempt, z);
        BlockState state = level.getBlockState(pos);

        // Skip air, bedrock, and other protected blocks
        if (state.isAir() ||
            state.is(Blocks.BEDROCK) ||
            state.is(Blocks.WATER) ||
            state.is(Blocks.LAVA)) {
          continue;
        }

        // Determine crumble result based on block type
        BlockState result = getCrumbleResult(state, level);
        if (result != null) {
          level.setBlock(pos, result, 3);
          break;
        }
      }
    }
  }

  /**
   * Determines what a block crumbles into.
   */
  private BlockState getCrumbleResult(BlockState original, ServerLevel level) {
    // Ore degradation chain: Diamond -> Emerald -> Gold -> Iron -> Coal -> Stone
    if (original.is(Blocks.DIAMOND_ORE)) return Blocks.EMERALD_ORE.defaultBlockState();
    if (original.is(Blocks.EMERALD_ORE)) return Blocks.GOLD_ORE.defaultBlockState();
    if (original.is(Blocks.GOLD_ORE)) return Blocks.IRON_ORE.defaultBlockState();
    if (original.is(Blocks.IRON_ORE)) return Blocks.COAL_ORE.defaultBlockState();
    if (original.is(Blocks.COAL_ORE)) return Blocks.STONE.defaultBlockState();

    // Deepslate ore degradation
    if (original.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_EMERALD_ORE)) return Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_GOLD_ORE)) return Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_IRON_ORE)) return Blocks.DEEPSLATE_COAL_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_COAL_ORE)) return Blocks.DEEPSLATE.defaultBlockState();

    // Brick degradation: Stone Bricks -> Cracked -> Cobblestone -> Gravel
    if (original.is(Blocks.STONE_BRICKS)) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    if (original.is(Blocks.CRACKED_STONE_BRICKS)) return Blocks.COBBLESTONE.defaultBlockState();

    // Stone-like blocks crumble to gravel or air
    if (original.is(Blocks.STONE) ||
        original.is(Blocks.COBBLESTONE) ||
        original.is(Blocks.ANDESITE) ||
        original.is(Blocks.DIORITE) ||
        original.is(Blocks.GRANITE)) {
      return level.random.nextBoolean() ?
          Blocks.GRAVEL.defaultBlockState() :
          Blocks.AIR.defaultBlockState();
    }

    // Deepslate crumbles to cobbled deepslate then gravel
    if (original.is(Blocks.DEEPSLATE)) return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    if (original.is(Blocks.COBBLED_DEEPSLATE)) return Blocks.GRAVEL.defaultBlockState();

    // Terracotta -> Clay
    if (original.is(Blocks.TERRACOTTA)) return Blocks.CLAY.defaultBlockState();

    // Glass -> Air
    if (original.is(Blocks.GLASS) || original.is(Blocks.TINTED_GLASS)) {
      return Blocks.AIR.defaultBlockState();
    }
    if (original.getBlock() instanceof net.minecraft.world.level.block.StainedGlassBlock) {
      return Blocks.AIR.defaultBlockState();
    }

    // Leaves -> Air
    if (original.is(net.minecraft.tags.BlockTags.LEAVES)) {
      return Blocks.AIR.defaultBlockState();
    }

    // Dirt/grass crumbles to air
    if (original.is(Blocks.DIRT) ||
        original.is(Blocks.GRASS_BLOCK) ||
        original.is(Blocks.COARSE_DIRT)) {
      return Blocks.AIR.defaultBlockState();
    }

    // Gravel/sand falls through (becomes air)
    if (original.is(Blocks.GRAVEL) || original.is(Blocks.SAND)) {
      return Blocks.AIR.defaultBlockState();
    }

    // Wood crumbles to air
    if (original.is(net.minecraft.tags.BlockTags.LOGS) ||
        original.is(net.minecraft.tags.BlockTags.PLANKS)) {
      return Blocks.AIR.defaultBlockState();
    }

    // Default: no crumble
    return null;
  }
}
