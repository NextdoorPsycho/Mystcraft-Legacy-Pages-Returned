package art.arcane.mystcraft.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.GrammarBindingMode;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.IGrammarBinding;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.InkBlend;
import art.arcane.mystcraft.grammar.CFGGrammarGenerator;
import art.arcane.mystcraft.grammar.CFGRule;
import art.arcane.mystcraft.grammar.GrammarData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for Age symbols.
 * Manages symbol registration, lookup, and categorization.
 * Also registers symbols with the CFG grammar system for age generation.
 *
 * Thread-safety: Uses concurrent collections to allow safe access from
 * multiple threads (render thread, server thread during datapack reload).
 */
public final class SymbolRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(SymbolRegistry.class);

  // Use concurrent collections to prevent ConcurrentModificationException during datapack reload
  private static final Map<ResourceLocation, IAgeSymbol> SYMBOLS = new ConcurrentHashMap<>();
  private static final Map<SymbolCategory, List<IAgeSymbol>> BY_CATEGORY = new ConcurrentHashMap<>();
  private static final Map<Integer, List<IAgeSymbol>> BY_CARD_RANK = new ConcurrentHashMap<>();
  private static final Set<ResourceLocation> BLACKLIST = ConcurrentHashMap.newKeySet();
  private static final Map<ResourceLocation, IAgeSymbol> STATIC_SYMBOLS = new ConcurrentHashMap<>();

  private static volatile boolean frozen = false;
  private static volatile boolean staticRegistrationOpen = true;

  private SymbolRegistry() {
  }

  /**
   * Registers a symbol.
   *
   * @param symbol The symbol to register
   * @return true if registration succeeded
   */
  public static boolean register(IAgeSymbol symbol) {
    return register(symbol, false);
  }

  /**
   * Registers a symbol, optionally replacing an existing entry.
   */
  public static boolean register(IAgeSymbol symbol, boolean replace) {
    if (frozen) {
      LOGGER.error("Cannot register symbol {} - registry is frozen", symbol.getRegistryName());
      return false;
    }

    ResourceLocation id = symbol.getRegistryName();

    if (id == null) {
      LOGGER.error("Cannot register symbol with null registry name");
      return false;
    }

    if (BLACKLIST.contains(id)) {
      LOGGER.info("Symbol {} is blacklisted, skipping registration", id);
      return false;
    }

    if (SYMBOLS.containsKey(id)) {
      if (!replace) {
        LOGGER.error("Symbol {} is already registered", id);
        return false;
      }
      removeInternal(id);
    }

    registerInternal(symbol, true);

    if (staticRegistrationOpen) {
      STATIC_SYMBOLS.put(id, symbol);
    }

    LOGGER.debug("Registered symbol: {}", id);
    return true;
  }

  /**
   * Registers a symbol with the CFG grammar system.
   * Creates a rule that maps the category's grammar token to this symbol.
   */
  private static void registerWithGrammar(IAgeSymbol symbol) {
    ResourceLocation grammarToken;
    Integer rank = symbol.getCardRank();
    if (symbol instanceof IGrammarBinding binding) {
      GrammarBindingMode mode = binding.getGrammarBindingMode();
      if (mode == GrammarBindingMode.DISABLED) {
        return;
      }
      if (mode == GrammarBindingMode.CUSTOM) {
        grammarToken = binding.getGrammarToken();
        if (grammarToken == null) {
          LOGGER.warn("Symbol {} requested CUSTOM grammar binding with null token", symbol.getRegistryName());
          return;
        }
        if (binding.getGrammarRank() != null) {
          rank = binding.getGrammarRank();
        }
      } else {
        grammarToken = getCategoryGrammarToken(symbol.getCategory());
      }
    } else {
      grammarToken = getCategoryGrammarToken(symbol.getCategory());
    }

    if (grammarToken == null) {
      return;
    }

    // Create a rule: GRAMMAR_TOKEN -> symbol_id [rank]
    CFGRule rule = new CFGRule(grammarToken, Collections.singletonList(symbol.getRegistryName()), rank);

    try {
      CFGGrammarGenerator.registerRule(rule);
      LOGGER.debug("Registered grammar rule: {} -> {} [rank {}]",
          grammarToken, symbol.getRegistryName(), rank);
    } catch (IllegalStateException e) {
      // Grammar already finalized - this is fine, rule registration happens during init
      LOGGER.debug("Grammar already finalized, skipping rule for {}", symbol.getRegistryName());
    }
  }

  /**
   * Maps a SymbolCategory to its corresponding grammar token.
   * Returns null for categories that don't have grammar tokens.
   */
  private static ResourceLocation getCategoryGrammarToken(SymbolCategory category) {
    return switch (category) {
      case TERRAIN -> GrammarData.TERRAIN;
      case BIOME_CONTROLLER -> GrammarData.BIOMECONTROLLER;
      case BIOME -> GrammarData.BIOME;
      case WEATHER -> GrammarData.WEATHER;
      case LIGHTING -> GrammarData.LIGHTING;
      case FEATURE_LARGE -> GrammarData.FEATURE_LARGE;
      case FEATURE_MEDIUM -> GrammarData.FEATURE_MEDIUM;
      case FEATURE_SMALL -> GrammarData.FEATURE_SMALL;
      case STRUCTURE -> GrammarData.FEATURE_MEDIUM;
      case ENVIRONMENT -> GrammarData.EFFECT;
      case VISUAL_EFFECT -> GrammarData.VISUAL_EFFECT;  // Color targets (sky, fog, grass)
      case SEA -> GrammarData.BLOCK_SEA;
      // Modifiers don't generate via grammar - they're placed by players
      case COLOR, ANGLE, PHASE, LENGTH, MODIFIER, SPECIAL -> null;
      default -> null;
    };
  }

  private static void registerInternal(IAgeSymbol symbol, boolean includeGrammar) {
    ResourceLocation id = symbol.getRegistryName();
    SYMBOLS.put(id, symbol);

    // Use CopyOnWriteArrayList for thread-safe iteration during render
    BY_CATEGORY.computeIfAbsent(symbol.getCategory(), k -> new CopyOnWriteArrayList<>()).add(symbol);

    Integer rank = symbol.getCardRank();
    if (rank != null) {
      BY_CARD_RANK.computeIfAbsent(rank, k -> new CopyOnWriteArrayList<>()).add(symbol);
    }

    if (includeGrammar) {
      registerWithGrammar(symbol);
    }
  }

  private static void removeInternal(ResourceLocation id) {
    IAgeSymbol existing = SYMBOLS.remove(id);
    if (existing == null) return;
    List<IAgeSymbol> categoryList = BY_CATEGORY.get(existing.getCategory());
    if (categoryList != null) {
      categoryList.remove(existing);
    }
    Integer rank = existing.getCardRank();
    if (rank != null) {
      List<IAgeSymbol> rankList = BY_CARD_RANK.get(rank);
      if (rankList != null) {
        rankList.remove(existing);
      }
    }
  }

  /**
   * Gets a symbol by its registry name.
   *
   * @param id The registry name
   * @return The symbol, or null if not found
   */
  public static IAgeSymbol get(ResourceLocation id) {
    if (id == null) return null;
    if (BLACKLIST.contains(id)) return null;
    return SYMBOLS.get(id);
  }

  /**
   * Gets a symbol by its string ID.
   *
   * @param id The ID string (namespace:path)
   * @return The symbol, or null if not found
   */
  public static IAgeSymbol get(String id) {
    return get(new ResourceLocation(id));
  }

  /**
   * Checks if a symbol is registered.
   *
   * @param id The registry name
   * @return true if registered
   */
  public static boolean contains(ResourceLocation id) {
    return SYMBOLS.containsKey(id) && !BLACKLIST.contains(id);
  }

  /**
   * Gets all registered symbols.
   *
   * @return Unmodifiable collection of all symbols
   */
  public static Collection<IAgeSymbol> getAll() {
    return Collections.unmodifiableCollection(SYMBOLS.values());
  }

  /**
   * Gets all symbols in a category.
   *
   * @param category The category
   * @return Unmodifiable list of symbols
   */
  public static List<IAgeSymbol> getByCategory(SymbolCategory category) {
    List<IAgeSymbol> list = BY_CATEGORY.get(category);
    return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
  }

  /**
   * Gets all symbols with a specific card rank.
   *
   * @param rank The card rank
   * @return Unmodifiable list of symbols
   */
  public static List<IAgeSymbol> getByCardRank(int rank) {
    List<IAgeSymbol> list = BY_CARD_RANK.get(rank);
    return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
  }

  /**
   * Gets all symbols within a card rank range.
   *
   * @param minRank Minimum rank (inclusive)
   * @param maxRank Maximum rank (inclusive)
   * @return List of symbols
   */
  public static List<IAgeSymbol> getByCardRankRange(int minRank, int maxRank) {
    List<IAgeSymbol> result = new ArrayList<>();
    for (int rank = minRank; rank <= maxRank; rank++) {
      List<IAgeSymbol> list = BY_CARD_RANK.get(rank);
      if (list != null) {
        result.addAll(list);
      }
    }
    return result;
  }

  /**
   * Gets a random symbol from a category.
   *
   * @param category The category
   * @param random   The random source
   * @return A random symbol, or null if category is empty
   */
  public static IAgeSymbol getRandomFromCategory(SymbolCategory category, RandomSource random) {
    List<IAgeSymbol> list = BY_CATEGORY.get(category);
    if (list == null || list.isEmpty()) return null;
    return list.get(random.nextInt(list.size()));
  }

  /**
   * Gets a random symbol based on weighted card ranks.
   * Higher ranks are rarer.
   *
   * @param random The random source
   * @return A random symbol
   */
  public static IAgeSymbol getRandomWeighted(RandomSource random) {
    return getRandomWeightedWithAffinity(random, null);
  }

  /**
   * Default ceiling on {@code card_rank} when rolling with affinity.
   * Without an affinity {@code tierBonus} (e.g. nether star = +2), only
   * symbols with rank ≤ this value can roll. {@link #getRandomWeighted}
   * remains uncapped for backwards compatibility.
   */
  public static final int DEFAULT_AFFINITY_RANK_CAP = 3;

  /**
   * Affinity-aware variant of {@link #getRandomWeighted}.
   * <p>
   * Each candidate symbol gets a base weight derived from its card rank
   * (rank 1 → 4, rank 4 → 1) and is then multiplied by
   * {@code (1 + symbolBoost + categoryBoost + poemBoost)} where each boost
   * comes from the supplied {@link InkBlend}. Symbols whose rank exceeds
   * {@link #DEFAULT_AFFINITY_RANK_CAP} {@code + blend.tierBonus()} are skipped.
   * <p>
   * If {@code blend} is {@code null} or {@link InkBlend#isEmpty() empty} the
   * algorithm reduces to the legacy uncapped weighted selection so callers
   * that don't track affinity get identical behaviour to before.
   *
   * @param random the source of randomness
   * @param blend  the affinity blend, or {@code null} for plain weighted
   * @return a random symbol respecting both rarity and affinity
   */
  public static IAgeSymbol getRandomWeightedWithAffinity(RandomSource random, InkBlend blend) {
    boolean hasBlend = blend != null && !blend.isEmpty();
    int rankCap = hasBlend ? DEFAULT_AFFINITY_RANK_CAP + blend.tierBonus() : Integer.MAX_VALUE;

    List<IAgeSymbol> weighted = new ArrayList<>();
    for (Map.Entry<Integer, List<IAgeSymbol>> entry : BY_CARD_RANK.entrySet()) {
      int rank = entry.getKey();
      if (rank > rankCap) continue;
      int baseWeight = Math.max(1, 5 - rank);
      for (IAgeSymbol symbol : entry.getValue()) {
        if (!symbol.allowInRandomGeneration()) continue;
        if (isBlacklisted(symbol.getRegistryName())) continue;

        int weight = baseWeight;
        if (hasBlend) {
          float boost = 0f;
          boost += blend.symbolWeight(symbol.getRegistryName());
          boost += blend.categoryWeight(symbol.getCategory());
          boost += blend.poemTokenSum(symbol.getPoem());
          if (boost < 0f) boost = 0f;
          // Multiplicative bias on the base weight; ceil to keep boosts visible
          // even when the base weight is already 1 (rarer symbols benefit most).
          weight = (int) Math.ceil(baseWeight * (1f + boost));
          // Sanity clamp to keep the weighted pool from exploding under stacked
          // contributions from many high-WEIGHT_CAP entries.
          if (weight > baseWeight * 32) weight = baseWeight * 32;
        }
        for (int i = 0; i < weight; i++) {
          weighted.add(symbol);
        }
      }
    }
    if (weighted.isEmpty()) return null;
    return weighted.get(random.nextInt(weighted.size()));
  }

  /**
   * Blacklists a symbol, preventing it from being used.
   *
   * @param id The symbol ID
   */
  public static void blacklist(ResourceLocation id) {
    BLACKLIST.add(id);
    LOGGER.info("Blacklisted symbol: {}", id);
  }

  public static void clearBlacklist() {
    BLACKLIST.clear();
  }

  /**
   * Checks if a symbol is blacklisted.
   *
   * @param id The symbol ID
   * @return true if blacklisted
   */
  public static boolean isBlacklisted(ResourceLocation id) {
    return BLACKLIST.contains(id);
  }

  /**
   * Freezes the registry, preventing further registrations.
   * Called after mod loading is complete.
   */
  public static void freeze() {
    frozen = true;
    LOGGER.info("Symbol registry frozen with {} symbols", SYMBOLS.size());
  }

  /**
   * Checks if the registry is frozen.
   *
   * @return true if frozen
   */
  public static boolean isFrozen() {
    return frozen;
  }

  /**
   * Checks whether the current symbol entry originates from static (code) registration.
   */
  public static boolean isStaticSymbol(ResourceLocation id) {
    return STATIC_SYMBOLS.containsKey(id);
  }

  /**
   * Checks whether a static symbol has been overridden by a non-static symbol.
   */
  public static boolean isOverridden(ResourceLocation id) {
    if (!STATIC_SYMBOLS.containsKey(id)) return false;
    IAgeSymbol current = SYMBOLS.get(id);
    return current != null && current != STATIC_SYMBOLS.get(id);
  }

  /**
   * Prevents additional symbols from being marked as static.
   * Call after all code-based symbol registration completes.
   */
  public static void sealStaticRegistration() {
    staticRegistrationOpen = false;
  }

  /**
   * Resets the registry back to code-registered static symbols.
   * Used before applying datapack symbols.
   */
  public static void resetToStatic() {
    SYMBOLS.clear();
    BY_CATEGORY.clear();
    BY_CARD_RANK.clear();
    frozen = false;

    for (IAgeSymbol symbol : STATIC_SYMBOLS.values()) {
      if (!BLACKLIST.contains(symbol.getRegistryName())) {
        registerInternal(symbol, true);
      }
    }
  }

  /**
   * Registers a symbol without enforcing the frozen guard and without grammar binding.
   * Intended for client sync only.
   */
  public static boolean registerSynced(IAgeSymbol symbol, boolean replace) {
    ResourceLocation id = symbol.getRegistryName();
    if (id == null) {
      LOGGER.error("Cannot register symbol with null registry name");
      return false;
    }
    if (BLACKLIST.contains(id)) {
      LOGGER.info("Symbol {} is blacklisted, skipping registration", id);
      return false;
    }
    if (SYMBOLS.containsKey(id)) {
      if (!replace) {
        return false;
      }
      removeInternal(id);
    }
    registerInternal(symbol, false);
    return true;
  }

  /**
   * Gets the total number of registered symbols.
   *
   * @return Symbol count
   */
  public static int size() {
    return SYMBOLS.size();
  }

  /**
   * Creates a Mystcraft-namespaced resource location.
   *
   * @param path The path
   * @return The resource location
   */
  public static ResourceLocation mystcraftId(String path) {
    return new ResourceLocation(Mystcraft.MOD_ID, path);
  }
}
