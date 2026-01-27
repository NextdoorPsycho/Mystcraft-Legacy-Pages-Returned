package art.arcane.mystcraft.instability.providers;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.instability.effects.EffectLightning;

/**
 * Provider for random lightning strike effects.
 */
public class ProviderLightning implements IInstabilityProvider {

    @Override
    public void addEffects(InstabilityDirector director, Integer level) {
        for (int i = 0; i < level; i++) {
            director.registerEffect(new EffectLightning());
        }
    }
}
