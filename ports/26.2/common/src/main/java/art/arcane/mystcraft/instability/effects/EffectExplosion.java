package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Environmental effect that causes random explosions. Scales with instability:
 * nearly silent at low values, dangerous at high.
 */
public class EffectExplosion implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.0005f;
  private static final int RANGE = 32;
  private static final float MIN_POWER = 1.0f;
  private static final float MAX_POWER = 2.0f;

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {

    float intensity = Math.max(0.0f, Math.min((instability - 30.0f) / 70.0f, 1.0f));
    if (intensity <= 0.0f) return;

    if (level.getRandom().nextFloat() >= BASE_CHANCE * intensity) {
      return;
    }

    List<ServerPlayer> players = level.players();
    if (players.isEmpty()) {
      return;
    }

    ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));

    int x = target.getBlockX() + level.getRandom().nextIntBetweenInclusive(-RANGE, RANGE);
    int z = target.getBlockZ() + level.getRandom().nextIntBetweenInclusive(-RANGE, RANGE);
    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

    BlockPos pos = new BlockPos(x, y, z);
    if (target.blockPosition().distSqr(pos) < 64) {
      return;
    }

    float power = MIN_POWER + (MAX_POWER - MIN_POWER) * intensity;

    level.explode(null, x + 0.5, y + 0.5, z + 0.5,
        power, Level.ExplosionInteraction.BLOCK);
  }
}
