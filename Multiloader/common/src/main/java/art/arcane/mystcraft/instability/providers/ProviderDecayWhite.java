package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.block.DecayBlock;
import art.arcane.mystcraft.instability.effects.EffectDecay;

/**
 * Provider for white decay effects.
 * White decay spreads but doesn't deal damage - visually dramatic but safe.
 */
public class ProviderDecayWhite implements IInstabilityProvider {

  @Override
  public void addEffects(InstabilityDirector director, Integer level) {
    for (int i = 0; i < level; i++) {
      director.registerEffect(new EffectDecay(DecayBlock.DecayType.WHITE));
    }
  }
}
