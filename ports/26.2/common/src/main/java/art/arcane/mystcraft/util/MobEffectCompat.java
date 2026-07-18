package art.arcane.mystcraft.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Creates 26.2 mob-effect instances from registered effect values.
 */
public final class MobEffectCompat {

  private MobEffectCompat() {
  }

  public static MobEffectInstance createInstance(MobEffect effect, int duration, int amplifier) {
    return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), duration, amplifier);
  }
}
