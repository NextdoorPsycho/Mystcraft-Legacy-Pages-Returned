package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.instability.effects.EffectErosion;

/**
 * Provider for erosion effects (fluid spreading into solid blocks).
 */
public class ProviderErosion implements IInstabilityProvider {

  @Override
  public void addEffects(InstabilityDirector director, Integer level) {
    for (int i = 0; i < level; i++) {
      director.registerEffect(new EffectErosion());
    }
  }
}
