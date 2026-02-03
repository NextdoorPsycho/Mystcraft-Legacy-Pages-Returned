package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Environmental effect that spawns random lightning strikes.
 * At lower instability, strikes are distant and visual-only.
 * Only becomes damaging at high instability.
 * <p>
 * 1.18.2 version - uses random.nextInt instead of nextIntBetweenInclusive.
 */
public class EffectLightning implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.0003f;
  private static final int RANGE = 80;

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {
    // Ramps from zero at instability 70 to full at 100
    float intensity = Math.max(0.0f, Math.min((instability - 70.0f) / 30.0f, 1.0f));
    if (intensity <= 0.0f) return;

    if (level.random.nextFloat() >= BASE_CHANCE * intensity) {
      return;
    }

    List<ServerPlayer> players = level.players();
    if (players.isEmpty()) {
      return;
    }

    ServerPlayer target = players.get(level.random.nextInt(players.size()));

    // Strike distance shrinks with intensity: far away at low, closer at high
    int range = (int) (RANGE - (RANGE - 24) * intensity);
    int minDist = Math.max(16, (int) (40 * (1.0f - intensity)));

    // 1.18.2: Use nextInt instead of nextIntBetweenInclusive
    double x = target.getX() + level.random.nextInt(range * 2 + 1) - range;
    double z = target.getZ() + level.random.nextInt(range * 2 + 1) - range;
    double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, (int) x, (int) z);

    // Push away if too close at low intensity
    double distSq = (x - target.getX()) * (x - target.getX()) + (z - target.getZ()) * (z - target.getZ());
    if (distSq < minDist * minDist) {
      return;
    }

    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
    if (lightning != null) {
      lightning.moveTo(x, y, z);
      // Visual-only below 70 instability (intensity 0.4). Damaging above.
      lightning.setVisualOnly(intensity < 0.4f);
      level.addFreshEntity(lightning);
    }
  }
}
