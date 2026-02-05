package art.arcane.mystcraft.util;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class MobEffectCompat {

  private static final Constructor<MobEffectInstance> CTOR_EFFECT =
      findCtor(MobEffectInstance.class, MobEffect.class, int.class, int.class);
  private static final Constructor<MobEffectInstance> CTOR_HOLDER =
      findCtor(MobEffectInstance.class, ReflectionCompat.getClassIfPresent("net.minecraft.core.Holder"), int.class, int.class);
  private static final Method EFFECT_TO_HOLDER =
      ReflectionCompat.findMethod(MobEffect.class, "builtInRegistryHolder",
          ReflectionCompat.getClassIfPresent("net.minecraft.core.Holder"));

  private MobEffectCompat() {
  }

  public static MobEffectInstance createInstance(MobEffect effect, int duration, int amplifier) {
    try {
      if (CTOR_EFFECT != null) {
        return CTOR_EFFECT.newInstance(effect, duration, amplifier);
      }
      if (CTOR_HOLDER != null && EFFECT_TO_HOLDER != null) {
        Object holder = EFFECT_TO_HOLDER.invoke(effect);
        if (holder != null) {
          return CTOR_HOLDER.newInstance(holder, duration, amplifier);
        }
      }
    } catch (ReflectiveOperationException ignored) {
    }
    return null;
  }

  private static <T> Constructor<T> findCtor(Class<T> owner, Class<?>... params) {
    if (params != null) {
      for (Class<?> param : params) {
        if (param == null) {
          return null;
        }
      }
    }
    try {
      return owner.getConstructor(params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private static Class<?> getClassIfPresent(String name) {
    return ReflectionCompat.getClassIfPresent(name);
  }
}
