package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Environmental effect that accelerates random ticks in loaded chunks.
 * Causes crops to grow faster, fire to spread, and other tick-driven changes.
 */
public class EffectExtraTicks implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.08f;
  private static final int EXTRA_TICK_PASSES = 8;

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {
    if (instability < 70.0f) return;
    float intensity = Math.min((instability - 70.0f) / 30.0f, 1.0f);

    if (level.random.nextFloat() >= BASE_CHANCE * intensity) {
      return;
    }

    int minY = chunk.getMinBuildHeight();
    LevelChunkSection[] sections = chunk.getSections();
    int passes = Math.max(1, Math.round(EXTRA_TICK_PASSES * intensity));

    for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
      LevelChunkSection section = sections[sectionIndex];
      if (section == null || !section.isRandomlyTicking()) {
        continue;
      }

      int sectionY = minY + (sectionIndex * 16);

      for (int pass = 0; pass < passes; pass++) {
        int localX = level.random.nextInt(16);
        int localY = level.random.nextInt(16);
        int localZ = level.random.nextInt(16);

        BlockState state = section.getBlockState(localX, localY, localZ);
        if (state.isRandomlyTicking()) {
          BlockPos pos = new BlockPos(
              chunk.getPos().getMinBlockX() + localX,
              sectionY + localY,
              chunk.getPos().getMinBlockZ() + localZ
          );
          state.randomTick(level, pos, level.random);
        }
      }
    }
  }
}
