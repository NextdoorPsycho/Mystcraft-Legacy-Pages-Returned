package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.block.DecayBlock;
import art.arcane.mystcraft.instability.effects.EffectDecay;

/**
 * Provider for blue decay effects.
 * Blue decay is non-spreading and non-damaging - relatively benign.
 */
public class ProviderDecayBlue implements IInstabilityProvider {

  @Override
  public void addEffects(InstabilityDirector director, Integer level) {
    for (int i = 0; i < level; i++) {
      director.registerEffect(new EffectDecay(DecayBlock.DecayType.BLUE));
    }
  }
}
