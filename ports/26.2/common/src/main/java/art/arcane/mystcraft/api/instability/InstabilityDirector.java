package art.arcane.mystcraft.api.instability;

/**
 * Interface for managing instability effects in an Age. Used by
 * IInstabilityProvider to register effects.
 */
public interface InstabilityDirector {

  /**
   * Gets the current instability score for this Age.
   *
   * @return The instability score (higher = more unstable)
   */
  int getInstabilityScore();

  /**
   * Registers an environmental effect to be active in this Age.
   *
   * @param effect The effect to register
   */
  void registerEffect(IEnvironmentalEffect effect);
}
