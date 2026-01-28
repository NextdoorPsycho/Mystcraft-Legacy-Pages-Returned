package art.arcane.mystcraft.instability;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.api.instability.InstabilityDirector;
import art.arcane.mystcraft.world.AgeData;
import com.google.common.collect.HashMultiset;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Runtime controller for instability effects in an Age.
 * Manages deck drawing and effect execution based on instability score.
 */
public class InstabilityController implements InstabilityDirector {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstabilityController.class);

    private final ServerLevel level;
    private final AgeData ageData;
    private final long seed;

    private boolean enabled;
    private int lastScore;

    private final Collection<Deck> decks;
    private final Map<String, Integer> providerLevels = new HashMap<>();
    private final List<IEnvironmentalEffect> effects = new ArrayList<>();

    /**
     * Creates an instability controller for the given level.
     *
     * @param level   The server level
     * @param ageData The age data containing instability info
     * @param seed    The age seed for randomization
     */
    public InstabilityController(ServerLevel level, AgeData ageData, long seed) {
        this.level = level;
        this.ageData = ageData;
        this.seed = seed;
        this.enabled = ageData.getInstability() > 0;
        this.lastScore = (int) ageData.getInstability();

        // Build decks from registry
        this.decks = InstabilityProviderRegistry.createDecks();
        initializeDecks();
        reconstruct();

        LOGGER.debug("InstabilityController initialized with {} decks, enabled={}", decks.size(), enabled);
    }

    /**
     * Initializes deck order, shuffling with saved state if available.
     */
    private void initializeDecks() {
        Random rand = new Random(seed);

        for (Deck deck : decks) {
            String deckName = deck.getName();

            // Get saved deck order if available
            List<String> savedOrder = ageData.getSavedDeckOrder(deckName);
            if (savedOrder != null && !savedOrder.isEmpty()) {
                // Reconstruct deck from saved order
                Collection<String> cards = HashMultiset.create(deck.getCards());
                deck.removeAll();

                boolean dirty = false;
                for (String card : savedOrder) {
                    if (cards.remove(card)) {
                        deck.putOnBottom(card);
                    } else {
                        dirty = true;
                    }
                }

                // Add any new cards that weren't in saved order
                if (!cards.isEmpty()) {
                    Deck newCards = new Deck("temp", cards);
                    newCards.shuffle(rand);
                    deck.putOnBottom(newCards);
                    dirty = true;
                }

                if (dirty) {
                    ageData.saveDeckOrder(deckName, deck.getCards());
                }
            } else {
                // New deck - shuffle and save
                deck.shuffle(rand);
                ageData.saveDeckOrder(deckName, deck.getCards());
            }
        }
    }

    /**
     * Validates the current state and rebuilds effects if needed.
     */
    private void validate() {
        boolean shouldBeEnabled = ageData.getInstability() > 0;
        if (enabled != shouldBeEnabled) {
            enabled = shouldBeEnabled;
            reconstruct();
            return;
        }

        // Check if instability score has changed significantly
        int currentScore = (int) ageData.getInstability();
        int smallestCost = InstabilityProviderRegistry.getSmallestCost();
        int normalizedNew = currentScore - (currentScore % smallestCost);

        if (normalizedNew != lastScore) {
            lastScore = normalizedNew;
            reconstruct();
        }
    }

    /**
     * Rebuilds all active effects based on current instability.
     */
    private void reconstruct() {
        providerLevels.clear();
        effects.clear();

        if (!enabled) {
            return;
        }

        // Determine which providers are active from each deck
        for (Deck deck : decks) {
            Collection<String> activeProviders = getActiveProviders(deck);
            if (activeProviders != null) {
                for (String provider : activeProviders) {
                    addProviderLevel(provider);
                }
            }
        }

        // Rebuild effects from active providers
        rebuildEffects();

        LOGGER.debug("Reconstructed instability effects: {} providers active, {} effects registered",
                providerLevels.size(), effects.size());
    }

    /**
     * Gets the active providers from a deck based on instability score.
     */
    private Collection<String> getActiveProviders(Deck deck) {
        int instabilityScore = getInstabilityScore();
        instabilityScore -= InstabilityProviderRegistry.getDeckCost(deck.getName());

        if (instabilityScore < 0) {
            return null;
        }

        List<String> activeProviders = new ArrayList<>();
        for (String card : deck.getCards()) {
            int cost = InstabilityProviderRegistry.getCardCost(card);
            instabilityScore -= cost;

            if (instabilityScore < 0) {
                break;
            }

            activeProviders.add(card);
        }

        return activeProviders;
    }

    /**
     * Increments the level for a provider.
     */
    private void addProviderLevel(String provider) {
        providerLevels.merge(provider, 1, Integer::sum);
    }

    /**
     * Rebuilds effect instances from active providers.
     */
    private void rebuildEffects() {
        effects.clear();

        for (Map.Entry<String, Integer> entry : providerLevels.entrySet()) {
            String name = entry.getKey();
            Integer level = entry.getValue();

            IInstabilityProvider provider = InstabilityProviderRegistry.getProvider(name);
            if (provider != null && level != null) {
                try {
                    provider.addEffects(this, level);
                } catch (Exception e) {
                    LOGGER.error("Error adding effects for provider '{}': {}", name, e.getMessage());
                    InstabilityProviderRegistry.markErrored(name);
                }
            }
        }
    }

    /**
     * Processes instability effects for a chunk.
     *
     * @param chunk The chunk to process
     */
    public void tick(LevelChunk chunk) {
        validate();

        if (!enabled || effects.isEmpty()) {
            return;
        }

        float instability = ageData.getInstability();
        for (IEnvironmentalEffect effect : effects) {
            try {
                effect.tick(level, chunk, instability);
            } catch (Exception e) {
                LOGGER.error("Error ticking instability effect: {}", e.getMessage());
            }
        }
    }

    @Override
    public int getInstabilityScore() {
        return lastScore;
    }

    @Override
    public void registerEffect(IEnvironmentalEffect effect) {
        effects.add(effect);
    }

    /**
     * Checks if instability is currently enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the number of active effects.
     */
    public int getActiveEffectCount() {
        return effects.size();
    }

    /**
     * Gets the number of active providers.
     */
    public int getActiveProviderCount() {
        return providerLevels.size();
    }

    /**
     * Gets the level of a specific provider.
     *
     * @param provider The provider identifier
     * @return The provider level, or 0 if not active
     */
    public int getProviderLevel(String provider) {
        return providerLevels.getOrDefault(provider, 0);
    }
}
