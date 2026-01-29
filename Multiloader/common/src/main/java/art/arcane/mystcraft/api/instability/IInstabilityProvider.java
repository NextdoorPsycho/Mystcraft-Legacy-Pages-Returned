package art.arcane.mystcraft.api.instability;

/**
 * Interface for providing instability effects.
 * Implementations create and register environmental effects based on instability level.
 */
public interface IInstabilityProvider {

  /**
   * Adds effects to the instability director based on the current level.
   *
   * @param director The instability director to register effects with
   * @param level    The activation level (number of times this provider's card was drawn)
   */
  void addEffects(InstabilityDirector director, Integer level);
}
