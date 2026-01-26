package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds an Age configuration from symbols using the CFG grammar system.
 * This is the main entry point for creating Ages from page collections.
 *
 * Uses the full Context-Free Grammar tree expansion like the original Mystcraft,
 * ensuring diverse and vibrant ages with proper symbol expansion.
 */
public class AgeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgeBuilder.class);

    private final List<IAgeSymbol> inputSymbols;
    private final long seed;
    private List<IAgeSymbol> expandedSymbols;
    private AgeDirectorImpl director;
    private float instability;
    private int providedCount;
    private int generatedCount;

    /**
     * Creates an AgeBuilder with the given symbols and seed.
     */
    public AgeBuilder(List<IAgeSymbol> symbols, long seed) {
        this.inputSymbols = new ArrayList<>(symbols);
        this.seed = seed;
        this.providedCount = symbols.size();

        // Perform CFG expansion
        expandSymbols();
    }

    /**
     * Creates a builder for a completely random Age.
     */
    public static AgeBuilder random(long seed) {
        return new AgeBuilder(List.of(), seed);
    }

    /**
     * Performs CFG-based symbol expansion like the original Mystcraft.
     * Uses the grammar tree to expand provided symbols into a complete age specification.
     */
    private void expandSymbols() {
        Random rand = new Random(seed);

        // Convert input symbols to their registry names (terminals)
        List<ResourceLocation> terminals = new ArrayList<>();
        for (IAgeSymbol symbol : inputSymbols) {
            terminals.add(symbol.getRegistryName());
        }

        LOGGER.info("Expanding Age with {} input symbols using CFG grammar", terminals.size());

        // Build grammar tree and parse terminals
        CFGGrammarTree tree = new CFGGrammarTree(GrammarRules.ROOT);
        tree.parseTerminals(terminals, rand);

        // Get the fully expanded symbol list
        List<ResourceLocation> expandedNames = tree.getExpanded(rand);

        LOGGER.info("CFG expansion produced {} symbols from {} inputs",
                expandedNames.size(), terminals.size());

        // Convert expanded names back to IAgeSymbol objects
        expandedSymbols = new ArrayList<>();
        for (ResourceLocation name : expandedNames) {
            IAgeSymbol symbol = SymbolRegistry.get(name);
            if (symbol != null) {
                expandedSymbols.add(symbol);
            } else {
                LOGGER.debug("No symbol registered for grammar token: {}", name);
            }
        }

        // Calculate counts and instability
        generatedCount = expandedSymbols.size() - providedCount;
        instability = calculateInstability();

        LOGGER.info("Age generation complete: {} provided, {} generated, {} total, instability: {}",
                providedCount, generatedCount, expandedSymbols.size(), instability);
    }

    /**
     * Calculates instability based on symbols present.
     */
    private float calculateInstability() {
        float total = 0.0f;

        // Generated symbols add base instability
        total += generatedCount * 5.0f;

        // Each symbol contributes its own instability cost
        for (IAgeSymbol symbol : expandedSymbols) {
            total += symbol.getInstabilityCost();
        }

        // Bonus for well-written Ages (low generated count)
        if (generatedCount == 0 && providedCount >= 5) {
            total *= 0.8f; // 20% reduction for complete Ages
        }

        return Math.max(0.0f, total);
    }

    /**
     * Builds the Age director with all symbol logic applied.
     */
    public AgeDirectorImpl build() {
        if (director != null) {
            return director;
        }

        director = new AgeDirectorImpl();

        LOGGER.info("Building Age with {} symbols (instability: {})",
                expandedSymbols.size(), instability);

        // Apply each symbol's logic to the director
        Random symbolRand = new Random(seed);
        for (IAgeSymbol symbol : expandedSymbols) {
            try {
                symbol.registerLogic(director, symbolRand.nextLong());
                LOGGER.debug("Applied symbol: {}", symbol.getRegistryName());
            } catch (Exception e) {
                LOGGER.error("Failed to apply symbol {}: {}",
                        symbol.getRegistryName(), e.getMessage());
            }
        }

        // Set the instability
        director.setInstability(instability);

        return director;
    }

    /**
     * Gets the final instability value.
     */
    public float getInstability() {
        return instability;
    }

    /**
     * Gets the list of all symbols (provided + generated).
     */
    public List<IAgeSymbol> getAllSymbols() {
        return expandedSymbols;
    }

    /**
     * Gets the original input symbols.
     */
    public List<IAgeSymbol> getInputSymbols() {
        return inputSymbols;
    }

    /**
     * Gets the seed used for generation.
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Checks if the Age was completely specified (no symbols were generated).
     */
    public boolean isComplete() {
        return generatedCount == 0;
    }

    /**
     * Gets the number of provided symbols.
     */
    public int getProvidedCount() {
        return providedCount;
    }

    /**
     * Gets the number of generated symbols.
     */
    public int getGeneratedCount() {
        return generatedCount;
    }

    /**
     * Gets validation issues with the symbol set.
     */
    public List<String> getValidationIssues() {
        List<String> issues = new ArrayList<>();

        // Check for conflicting symbols
        boolean hasVoidTerrain = expandedSymbols.stream()
                .anyMatch(s -> s.getRegistryName().getPath().equals("terrain_void"));
        boolean hasCaves = expandedSymbols.stream()
                .anyMatch(s -> s.getRegistryName().getPath().equals("caves"));

        if (hasVoidTerrain && hasCaves) {
            issues.add("Caves symbol conflicts with Void Terrain");
        }

        return issues;
    }

    /**
     * Creates a summary of the Age for display.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Age Summary:\n");
        sb.append("  Symbols: ").append(expandedSymbols.size()).append("\n");
        sb.append("  Provided: ").append(providedCount).append("\n");
        sb.append("  Generated: ").append(generatedCount).append("\n");
        sb.append("  Instability: ").append(String.format("%.1f", instability)).append("\n");

        if (!isComplete()) {
            sb.append("  Status: INCOMPLETE (random elements added)\n");
        } else {
            sb.append("  Status: COMPLETE\n");
        }

        List<String> issues = getValidationIssues();
        if (!issues.isEmpty()) {
            sb.append("  Issues:\n");
            for (String issue : issues) {
                sb.append("    - ").append(issue).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Creates a detailed symbol list for display.
     */
    public String getSymbolList() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbols in Age:\n");

        for (IAgeSymbol symbol : expandedSymbols) {
            ResourceLocation name = symbol.getRegistryName();
            sb.append("  - ").append(name.getPath())
                    .append(" (").append(symbol.getCategory().name())
                    .append(", instability: ").append(symbol.getInstabilityCost())
                    .append(")\n");
        }

        return sb.toString();
    }
}
