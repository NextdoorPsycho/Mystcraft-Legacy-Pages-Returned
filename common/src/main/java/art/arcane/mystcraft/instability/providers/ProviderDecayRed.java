package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.block.DecayBlock;
import art.arcane.mystcraft.instability.effects.EffectDecay;

/**
 * Provider for red decay effects.
 * Red decay spreads and deals high damage.
 */
public class ProviderDecayRed implements IInstabilityProvider {

  @Override
  public void addEffects(InstabilityDirector director, Integer level) {
    for (int i = 0; i < level; i++) {
      director.registerEffect(new EffectDecay(DecayBlock.DecayType.RED));
    }
  }
}
