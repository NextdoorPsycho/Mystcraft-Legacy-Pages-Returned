package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Environmental effect that causes blocks to crumble (turn to air or gravel).
 * Scales with instability: a single pass at low values, aggressive multi-pass
 * at high.
 */
public class EffectCrumble implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.012f;
  private static final int MAX_CRUMBLE_PASSES = 8;

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {

    float intensity = Math.max(0.1f, Math.min(instability / 80.0f, 1.0f));

    if (level.random.nextFloat() >= BASE_CHANCE * intensity) {
      return;
    }

    int passes = Math.max(1, Math.round(MAX_CRUMBLE_PASSES * intensity));

    for (int pass = 0; pass < passes; pass++) {

      int x = chunk.getPos().getMinBlockX() + level.random.nextInt(16);
      int z = chunk.getPos().getMinBlockZ() + level.random.nextInt(16);
      int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

      for (int attempt = 0; attempt < 8; attempt++) {
        BlockPos pos = new BlockPos(x, y - attempt, z);
        BlockState state = level.getBlockState(pos);

        if (state.isAir() ||
            state.is(Blocks.BEDROCK) ||
            state.is(Blocks.WATER) ||
            state.is(Blocks.LAVA)) {
          continue;
        }

        BlockState result = getCrumbleResult(state, level);
        if (result != null) {
          level.setBlock(pos, result, 3);
          break;
        }
      }
    }
  }

  private BlockState getCrumbleResult(BlockState original, ServerLevel level) {

    if (original.is(Blocks.DIAMOND_ORE))
      return Blocks.EMERALD_ORE.defaultBlockState();
    if (original.is(Blocks.EMERALD_ORE))
      return Blocks.GOLD_ORE.defaultBlockState();
    if (original.is(Blocks.GOLD_ORE))
      return Blocks.IRON_ORE.defaultBlockState();
    if (original.is(Blocks.IRON_ORE))
      return Blocks.COAL_ORE.defaultBlockState();
    if (original.is(Blocks.COAL_ORE)) return Blocks.STONE.defaultBlockState();

    if (original.is(Blocks.DEEPSLATE_DIAMOND_ORE))
      return Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_EMERALD_ORE))
      return Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_GOLD_ORE))
      return Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_IRON_ORE))
      return Blocks.DEEPSLATE_COAL_ORE.defaultBlockState();
    if (original.is(Blocks.DEEPSLATE_COAL_ORE))
      return Blocks.DEEPSLATE.defaultBlockState();

    if (original.is(Blocks.STONE_BRICKS))
      return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    if (original.is(Blocks.CRACKED_STONE_BRICKS))
      return Blocks.COBBLESTONE.defaultBlockState();

    if (original.is(Blocks.STONE) ||
        original.is(Blocks.COBBLESTONE) ||
        original.is(Blocks.ANDESITE) ||
        original.is(Blocks.DIORITE) ||
        original.is(Blocks.GRANITE)) {
      return level.random.nextBoolean() ?
          Blocks.GRAVEL.defaultBlockState() :
          Blocks.AIR.defaultBlockState();
    }

    if (original.is(Blocks.DEEPSLATE))
      return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    if (original.is(Blocks.COBBLED_DEEPSLATE))
      return Blocks.GRAVEL.defaultBlockState();

    if (original.is(Blocks.TERRACOTTA)) return Blocks.CLAY.defaultBlockState();

    if (original.is(Blocks.GLASS) || original.is(Blocks.TINTED_GLASS)) {
      return Blocks.AIR.defaultBlockState();
    }
    if (original.getBlock() instanceof net.minecraft.world.level.block.StainedGlassBlock) {
      return Blocks.AIR.defaultBlockState();
    }

    if (original.is(net.minecraft.tags.BlockTags.LEAVES)) {
      return Blocks.AIR.defaultBlockState();
    }

    if (original.is(Blocks.DIRT) ||
        original.is(Blocks.GRASS_BLOCK) ||
        original.is(Blocks.COARSE_DIRT)) {
      return Blocks.AIR.defaultBlockState();
    }

    if (original.is(Blocks.GRAVEL) || original.is(Blocks.SAND)) {
      return Blocks.AIR.defaultBlockState();
    }

    if (original.is(net.minecraft.tags.BlockTags.LOGS) ||
        original.is(net.minecraft.tags.BlockTags.PLANKS)) {
      return Blocks.AIR.defaultBlockState();
    }

    return null;
  }
}
