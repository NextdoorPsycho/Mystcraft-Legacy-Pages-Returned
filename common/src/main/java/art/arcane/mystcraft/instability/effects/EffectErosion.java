package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;

/**
 * Environmental effect that erodes solid blocks adjacent to fluids. Scales with
 * instability: minimal erosion at low values, aggressive at high.
 */
public class EffectErosion implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.015f;
  private static final int MAX_EROSION_ATTEMPTS = 6;

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {

    float intensity = Math.max(0.1f, Math.min(instability / 80.0f, 1.0f));

    if (level.random.nextFloat() >= BASE_CHANCE * intensity) {
      return;
    }

    int attempts = Math.max(1, Math.round(MAX_EROSION_ATTEMPTS * intensity));

    for (int erosion = 0; erosion < attempts; erosion++) {
      int x = chunk.getPos().getMinBlockX() + level.random.nextInt(16);
      int z = chunk.getPos().getMinBlockZ() + level.random.nextInt(16);
      int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

      for (int attempt = 0; attempt < 12; attempt++) {
        BlockPos pos = new BlockPos(x, y - attempt, z);
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || !canErode(state)) {
          continue;
        }

        for (Direction direction : Direction.values()) {
          BlockPos neighborPos = pos.relative(direction);
          FluidState fluidState = level.getFluidState(neighborPos);

          if (!fluidState.isEmpty()) {
            level.setBlock(pos, fluidState.createLegacyBlock(), 3);
            break;
          }
        }
      }
    }
  }

  private boolean canErode(BlockState state) {
    if (state.is(Blocks.BEDROCK)) return false;
    if (state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME))
      return false;
    if (state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_GATEWAY))
      return false;
    if (state.getDestroySpeed(null, BlockPos.ZERO) < 0) return false;
    return !state.getFluidState().isEmpty() || state.isSolid();
  }
}
