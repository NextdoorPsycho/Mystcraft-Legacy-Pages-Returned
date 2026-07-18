package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Generic instability provider backed by an explicit, type-safe effect factory.
 */
public final class InstabilityProvider implements IInstabilityProvider {
  private final IntFunction<? extends IEnvironmentalEffect> factory;
  private final boolean repeatForLevel;

  /**
   * Creates one effect whose configuration is derived from the instability
   * level.
   */
  public static InstabilityProvider leveled(
      IntFunction<? extends IEnvironmentalEffect> factory) {
    return new InstabilityProvider(factory, false);
  }

  /**
   * Creates one independent effect for each point of instability level.
   */
  public static InstabilityProvider repeated(
      Supplier<? extends IEnvironmentalEffect> factory) {
    Objects.requireNonNull(factory, "factory");
    return new InstabilityProvider(ignored -> factory.get(), true);
  }

  private InstabilityProvider(
      IntFunction<? extends IEnvironmentalEffect> factory,
      boolean repeatForLevel) {
    this.factory = Objects.requireNonNull(factory, "factory");
    this.repeatForLevel = repeatForLevel;
  }

  @Override
  public void addEffects(InstabilityDirector director, Integer level) {
    Objects.requireNonNull(director, "director");
    int resolvedLevel = Objects.requireNonNull(level, "level");
    int count = repeatForLevel ? Math.max(0, resolvedLevel) : 1;
    for (int i = 0; i < count; i++) {
      IEnvironmentalEffect effect = Objects.requireNonNull(
          factory.apply(resolvedLevel),
          "Instability effect factory returned null");
      director.registerEffect(effect);
    }
  }
}
