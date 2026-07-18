package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import art.arcane.mystcraft.util.MobEffectCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Environmental effect that applies a potion effect to hostile mobs (buffing
 * enemies).
 */
public class EffectPotionEnemy implements IEnvironmentalEffect {

  private static final float BASE_CHANCE = 0.002f;
  private final int level;
  private final boolean global;
  private final MobEffect effect;
  private final int duration;

  /**
   * Creates an enemy buff effect.
   *
   * @param level    The effect level (affects frequency)
   * @param global   If true, affects all mobs; if false, only nearby mobs
   * @param effect   The mob effect to apply
   * @param duration The duration in ticks
   */
  public EffectPotionEnemy(int level, boolean global, MobEffect effect, int duration) {
    this.level = level;
    this.global = global;
    this.effect = effect;
    this.duration = duration;
  }

  @Override
  public void tick(ServerLevel level, LevelChunk chunk, float instability) {

    float intensity = Math.max(0.0f, Math.min((instability - 50.0f) / 50.0f, 1.0f));
    if (intensity <= 0.0f) return;
    float chance = BASE_CHANCE * this.level * intensity;
    if (level.getRandom().nextFloat() >= chance) {
      return;
    }

    AABB area;
    if (global) {

      if (level.players().isEmpty()) return;
      ServerPlayer player = level.players().get(0);
      area = new AABB(player.blockPosition()).inflate(128, 64, 128);
    } else {

      int chunkX = chunk.getPos().getMinBlockX();
      int chunkZ = chunk.getPos().getMinBlockZ();
      area = new AABB(chunkX, level.getMinY(), chunkZ,
          chunkX + 16, level.getMaxY(), chunkZ + 16);
    }

    List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area);
    if (mobs.isEmpty()) {
      return;
    }

    Mob target = mobs.get(level.getRandom().nextInt(mobs.size()));
    MobEffectInstance instance = MobEffectCompat.createInstance(effect, duration, 0);
    if (instance != null) {
      target.addEffect(instance);
    }
  }
}
