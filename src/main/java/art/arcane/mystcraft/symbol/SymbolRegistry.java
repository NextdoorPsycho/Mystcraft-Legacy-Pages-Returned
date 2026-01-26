package art.arcane.mystcraft.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.grammar.CFGGrammarGenerator;
import art.arcane.mystcraft.grammar.CFGRule;
import art.arcane.mystcraft.grammar.GrammarData;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.RandomSource;

import java.util.*;

/**
 * Registry for Age symbols.
 * Manages symbol registration, lookup, and categorization.
 * Also registers symbols with the CFG grammar system for age generation.
 */
public final class SymbolRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(SymbolRegistry.class);

    private static final Map<ResourceLocation, IAgeSymbol> SYMBOLS = new HashMap<>();
    private static final Map<SymbolCategory, List<IAgeSymbol>> BY_CATEGORY = new EnumMap<>(SymbolCategory.class);
    private static final Map<Integer, List<IAgeSymbol>> BY_CARD_RANK = new HashMap<>();
    private static final Set<ResourceLocation> BLACKLIST = new HashSet<>();

    private static boolean frozen = false;

    private SymbolRegistry() {}

    /**
     * Registers a symbol.
     * @param symbol The symbol to register
     * @return true if registration succeeded
     */
    public static boolean register(IAgeSymbol symbol) {
        if (frozen) {
            LOGGER.error("Cannot register symbol {} - registry is frozen", symbol.getRegistryName());
            return false;
        }

        ResourceLocation id = symbol.getRegistryName();

        if (id == null) {
            LOGGER.error("Cannot register symbol with null registry name");
            return false;
        }

        if (SYMBOLS.containsKey(id)) {
            LOGGER.error("Symbol {} is already registered", id);
            return false;
        }

        if (BLACKLIST.contains(id)) {
            LOGGER.info("Symbol {} is blacklisted, skipping registration", id);
            return false;
        }

        SYMBOLS.put(id, symbol);

        // Add to category index
        BY_CATEGORY.computeIfAbsent(symbol.getCategory(), k -> new ArrayList<>()).add(symbol);

        // Add to card rank index
        Integer rank = symbol.getCardRank();
        if (rank != null) {
            BY_CARD_RANK.computeIfAbsent(rank, k -> new ArrayList<>()).add(symbol);
        }

        // Register with CFG grammar so this symbol can be generated
        registerWithGrammar(symbol);

        LOGGER.debug("Registered symbol: {}", id);
        return true;
    }

    /**
     * Registers a symbol with the CFG grammar system.
     * Creates a rule that maps the category's grammar token to this symbol.
     */
    private static void registerWithGrammar(IAgeSymbol symbol) {
        ResourceLocation grammarToken = getCategoryGrammarToken(symbol.getCategory());
        if (grammarToken == null) {
            // Category doesn't have a grammar token (e.g., modifiers are handled differently)
            return;
        }

        // Create a rule: GRAMMAR_TOKEN -> symbol_id [rank]
        Integer rank = symbol.getCardRank();
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
            case SUN -> GrammarData.SUN;
            case MOON -> GrammarData.MOON;
            case STARS -> GrammarData.STARFIELD;
            case WEATHER -> GrammarData.WEATHER;
            case LIGHTING -> GrammarData.LIGHTING;
            case FEATURE -> GrammarData.FEATURE_LARGE;  // Large features by default
            case STRUCTURE -> GrammarData.FEATURE_MEDIUM;  // Structures are medium features
            case ENVIRONMENT -> GrammarData.EFFECT;
            case VISUAL_EFFECT -> GrammarData.VISUAL_EFFECT;  // Color targets (sky, fog, grass)
            // Modifiers don't generate via grammar - they're placed by players
            case COLOR, ANGLE, PHASE, LENGTH, MODIFIER, SPECIAL -> null;
            default -> null;
        };
    }

    /**
     * Gets a symbol by its registry name.
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
     * @param id The ID string (namespace:path)
     * @return The symbol, or null if not found
     */
    public static IAgeSymbol get(String id) {
        return get(new ResourceLocation(id));
    }

    /**
     * Checks if a symbol is registered.
     * @param id The registry name
     * @return true if registered
     */
    public static boolean contains(ResourceLocation id) {
        return SYMBOLS.containsKey(id) && !BLACKLIST.contains(id);
    }

    /**
     * Gets all registered symbols.
     * @return Unmodifiable collection of all symbols
     */
    public static Collection<IAgeSymbol> getAll() {
        return Collections.unmodifiableCollection(SYMBOLS.values());
    }

    /**
     * Gets all symbols in a category.
     * @param category The category
     * @return Unmodifiable list of symbols
     */
    public static List<IAgeSymbol> getByCategory(SymbolCategory category) {
        List<IAgeSymbol> list = BY_CATEGORY.get(category);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Gets all symbols with a specific card rank.
     * @param rank The card rank
     * @return Unmodifiable list of symbols
     */
    public static List<IAgeSymbol> getByCardRank(int rank) {
        List<IAgeSymbol> list = BY_CARD_RANK.get(rank);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Gets all symbols within a card rank range.
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
     * @param category The category
     * @param random The random source
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
     * @param random The random source
     * @return A random symbol
     */
    public static IAgeSymbol getRandomWeighted(RandomSource random) {
        // Simple weighted selection - lower ranks are more common
        List<IAgeSymbol> weighted = new ArrayList<>();
        for (Map.Entry<Integer, List<IAgeSymbol>> entry : BY_CARD_RANK.entrySet()) {
            int rank = entry.getKey();
            int weight = Math.max(1, 5 - rank); // Rank 0: weight 5, Rank 4: weight 1
            for (int i = 0; i < weight; i++) {
                weighted.addAll(entry.getValue());
            }
        }
        if (weighted.isEmpty()) return null;
        return weighted.get(random.nextInt(weighted.size()));
    }

    /**
     * Blacklists a symbol, preventing it from being used.
     * @param id The symbol ID
     */
    public static void blacklist(ResourceLocation id) {
        BLACKLIST.add(id);
        LOGGER.info("Blacklisted symbol: {}", id);
    }

    /**
     * Checks if a symbol is blacklisted.
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
     * @return true if frozen
     */
    public static boolean isFrozen() {
        return frozen;
    }

    /**
     * Gets the total number of registered symbols.
     * @return Symbol count
     */
    public static int size() {
        return SYMBOLS.size();
    }

    /**
     * Creates a Mystcraft-namespaced resource location.
     * @param path The path
     * @return The resource location
     */
    public static ResourceLocation mystcraftId(String path) {
        return new ResourceLocation(Mystcraft.MOD_ID, path);
    }
}
