package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import art.arcane.mystcraft.entity.MeteorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Environmental effect that spawns falling meteors. Scales with instability:
 * extremely rare at low values, frequent at high.
 */
public class EffectMeteor implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.0005f;
  private static final int RANGE = 48;

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {

    float intensity = Math.max(0.0f, Math.min((instability - 70.0f) / 30.0f, 1.0f));
    if (intensity <= 0.0f) return;

    if (level.getRandom().nextFloat() >= BASE_CHANCE * intensity) {
      return;
    }

    List<ServerPlayer> players = level.players();
    if (players.isEmpty()) {
      return;
    }

    ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));

    double x = target.getX() + level.getRandom().nextIntBetweenInclusive(-RANGE, RANGE);
    double z = target.getZ() + level.getRandom().nextIntBetweenInclusive(-RANGE, RANGE);
    double y = Math.min(target.getY() + 100 + level.getRandom().nextInt(50), level.getMaxY() - 1);

    int maxSize = Math.max(1, Math.round(3 * intensity));
    int size = 1 + level.getRandom().nextInt(maxSize);

    MeteorEntity meteor = new MeteorEntity(level, x, y, z, size);
    level.addFreshEntity(meteor);
  }
}
