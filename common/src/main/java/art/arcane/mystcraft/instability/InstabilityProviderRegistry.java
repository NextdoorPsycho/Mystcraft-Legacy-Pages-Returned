package art.arcane.mystcraft.instability;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Central registry for instability providers and deck management.
 */
public final class InstabilityProviderRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstabilityProviderRegistry.class);

  private static final Map<String, IInstabilityProvider> providers = new HashMap<>();
  private static final Map<String, Integer> cardCosts = new HashMap<>();
  private static final Map<String, Integer> cardCounts = new HashMap<>();

  private static final Map<String, List<String>> deckCards = new HashMap<>();
  private static final Map<String, Integer> deckCosts = new HashMap<>();

  private static final Set<String> erroredProviders = new HashSet<>();
  private static final Set<String> warnedProviders = new HashSet<>();
  private static int smallestCost = 500;

  private InstabilityProviderRegistry() {
  }

  /**
   * Registers an instability provider.
   *
   * @param identifier     Unique identifier for the provider
   * @param provider       The provider implementation
   * @param activationCost Instability cost to activate this provider
   * @return true if registration succeeded
   */
  public static boolean registerProvider(String identifier, IInstabilityProvider provider, int activationCost) {
    if (identifier == null || identifier.isEmpty()) {
      LOGGER.error("Attempting to register instability provider with null or empty identifier");
      return false;
    }

    if (providers.containsKey(identifier)) {
      LOGGER.warn("Instability provider '{}' already registered, overwriting", identifier);
    }

    providers.put(identifier, provider);
    cardCosts.put(identifier, activationCost);

    if (activationCost > 0 && activationCost < smallestCost) {
      smallestCost = activationCost;
    }

    LOGGER.debug("Registered instability provider '{}' with cost {}", identifier, activationCost);
    return true;
  }

  /**
   * Unregisters an instability provider.
   *
   * @param identifier The provider identifier
   * @param provider   The provider to unregister (must match currently
   *                   registered)
   */
  public static void unregisterProvider(String identifier, IInstabilityProvider provider) {
    if (providers.get(identifier) == provider) {
      providers.remove(identifier);
      LOGGER.info("Unregistered instability provider '{}'", identifier);
    }
  }

  /**
   * Gets a provider by identifier.
   *
   * @param identifier The provider identifier
   * @return The provider, or null if not found
   */
  public static IInstabilityProvider getProvider(String identifier) {
    IInstabilityProvider provider = providers.get(identifier);
    if (provider == null && warnedProviders.add(identifier)) {
      LOGGER.error("No instability provider found for identifier '{}'. Is the provider loaded?", identifier);
    }
    return provider;
  }

  /**
   * Gets all registered provider identifiers.
   */
  public static Collection<String> getAllProviderIds() {
    return Collections.unmodifiableCollection(providers.keySet());
  }

  /**
   * Gets the smallest activation cost among all providers.
   */
  public static int getSmallestCost() {
    return smallestCost;
  }

  /**
   * Gets the activation cost for a card.
   *
   * @param card The card/provider identifier
   * @return The activation cost, or 0 if not found
   */
  public static int getCardCost(String card) {
    Integer cost = cardCosts.get(card);
    return cost != null ? cost : 0;
  }

  /**
   * Sets the base cost for a deck.
   *
   * @param deckName The deck name
   * @param cost     The base cost required to access this deck
   */
  public static void setDeckCost(String deckName, int cost) {
    deckCosts.put(deckName, cost);
    LOGGER.debug("Set deck '{}' base cost to {}", deckName, cost);
  }

  /**
   * Gets the base cost for a deck.
   *
   * @param deckName The deck name
   * @return The base cost, or 0 if not found
   */
  public static int getDeckCost(String deckName) {
    Integer cost = deckCosts.get(deckName);
    return cost != null ? cost : 0;
  }

  /**
   * Gets all registered deck names.
   */
  public static Collection<String> getDeckNames() {
    return Collections.unmodifiableSet(deckCosts.keySet());
  }

  /**
   * Adds cards to a deck.
   *
   * @param deckName The deck name (must be registered with setDeckCost first)
   * @param cards    The cards to add
   * @throws IllegalStateException if deck is not registered
   */
  public static void addCards(String deckName, List<String> cards) {
    if (!deckCosts.containsKey(deckName)) {
      throw new IllegalStateException("Attempting to add cards to unregistered deck: " + deckName);
    }

    List<String> deck = deckCards.computeIfAbsent(deckName, k -> new ArrayList<>());

    Set<String> newCards = new HashSet<>();
    for (String card : cards) {
      if (getProvider(card) == null) {
        LOGGER.error("Cannot add card '{}' to deck '{}': provider not found", card, deckName);
        continue;
      }

      cardCounts.merge(card, 1, Integer::sum);
      deck.add(card);
      newCards.add(card);
    }

    LOGGER.debug("Added {} cards to deck '{}'", newCards.size(), deckName);
  }

  /**
   * Adds multiple copies of a card to a deck.
   *
   * @param deckName The deck name
   * @param card     The card identifier
   * @param count    Number of copies to add
   */
  public static void addCards(String deckName, String card, int count) {
    addCards(deckName, Collections.nCopies(count, card));
  }

  /**
   * Adds cards to a deck (varargs version).
   *
   * @param deckName The deck name
   * @param cards    The cards to add
   */
  public static void addCards(String deckName, String... cards) {
    addCards(deckName, Arrays.asList(cards));
  }

  /**
   * Creates Deck objects from all registered deck data.
   *
   * @return Collection of Deck objects
   */
  public static Collection<Deck> createDecks() {
    List<Deck> decks = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : deckCards.entrySet()) {
      decks.add(new Deck(entry.getKey(), entry.getValue()));
    }
    return decks;
  }

  /**
   * Gets the card count for a specific provider.
   *
   * @param card The card identifier
   * @return The number of times this card appears across all decks
   */
  public static int getCardCount(String card) {
    Integer count = cardCounts.get(card);
    return count != null ? count : 0;
  }

  /**
   * Checks if a provider has errored.
   *
   * @param identifier The provider identifier
   * @return true if the provider has errored during profiling
   */
  public static boolean hasErrored(String identifier) {
    return erroredProviders.contains(identifier);
  }

  /**
   * Marks a provider as errored.
   *
   * @param identifier The provider identifier
   */
  public static void markErrored(String identifier) {
    erroredProviders.add(identifier);
  }

  /**
   * Resets the registry (for testing only).
   */
  public static void reset() {
    providers.clear();
    cardCosts.clear();
    cardCounts.clear();
    deckCards.clear();
    deckCosts.clear();
    erroredProviders.clear();
    warnedProviders.clear();
    smallestCost = 500;
  }
}
