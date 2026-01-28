package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.instability.effects.EffectScorched;

/**
 * Provider for scorched earth effects (random fire spreading).
 */
public class ProviderScorched implements IInstabilityProvider {

    @Override
    public void addEffects(InstabilityDirector director, Integer level) {
        for (int i = 0; i < level; i++) {
            director.registerEffect(new EffectScorched());
        }
    }
}
