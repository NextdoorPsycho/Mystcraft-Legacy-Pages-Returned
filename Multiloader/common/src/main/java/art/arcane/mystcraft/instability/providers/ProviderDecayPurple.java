package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.instability.effects.EffectDecay;
import art.arcane.mystcraft.block.DecayBlock;

/**
 * Provider for purple decay effects.
 * Purple decay spreads and deals moderate damage.
 */
public class ProviderDecayPurple implements IInstabilityProvider {

    @Override
    public void addEffects(InstabilityDirector director, Integer level) {
        for (int i = 0; i < level; i++) {
            director.registerEffect(new EffectDecay(DecayBlock.DecayType.PURPLE));
        }
    }
}
