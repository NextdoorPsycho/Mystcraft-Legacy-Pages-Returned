package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Builds an Age configuration from symbols using the grammar system.
 * This is the main entry point for creating Ages from page collections.
 */
public class AgeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgeBuilder.class);

    private final List<IAgeSymbol> symbols;
    private final long seed;
    private final GrammarGenerator.GenerationResult result;
    private AgeDirectorImpl director;

    /**
     * Creates an AgeBuilder with the given symbols and seed.
     */
    public AgeBuilder(List<IAgeSymbol> symbols, long seed) {
        this.symbols = symbols;
        this.seed = seed;
        this.result = GrammarGenerator.generateAge(symbols, seed);
    }

    /**
     * Creates a builder for a completely random Age.
     */
    public static AgeBuilder random(long seed) {
        return new AgeBuilder(List.of(), seed);
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
                result.getSymbols().size(), result.getInstability());

        // Apply each symbol's logic to the director
        for (IAgeSymbol symbol : result.getSymbols()) {
            try {
                symbol.registerLogic(director, seed);
                LOGGER.debug("Applied symbol: {}", symbol.getRegistryName());
            } catch (Exception e) {
                LOGGER.error("Failed to apply symbol {}: {}",
                        symbol.getRegistryName(), e.getMessage());
            }
        }

        // Set the instability
        director.setInstability(result.getInstability());

        return director;
    }

    /**
     * Gets the generation result containing symbols and instability info.
     */
    public GrammarGenerator.GenerationResult getResult() {
        return result;
    }

    /**
     * Gets the final instability value.
     */
    public float getInstability() {
        return result.getInstability();
    }

    /**
     * Gets the list of all symbols (provided + generated).
     */
    public List<IAgeSymbol> getAllSymbols() {
        return result.getSymbols();
    }

    /**
     * Gets the original input symbols.
     */
    public List<IAgeSymbol> getInputSymbols() {
        return symbols;
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
        return result.isComplete();
    }

    /**
     * Gets validation issues with the symbol set.
     */
    public List<String> getValidationIssues() {
        return GrammarGenerator.validateSymbols(result.getSymbols());
    }

    /**
     * Creates a summary of the Age for display.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Age Summary:\n");
        sb.append("  Symbols: ").append(result.getSymbols().size()).append("\n");
        sb.append("  Provided: ").append(result.getProvidedCount()).append("\n");
        sb.append("  Generated: ").append(result.getGeneratedCount()).append("\n");
        sb.append("  Instability: ").append(String.format("%.1f", result.getInstability())).append("\n");

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

        for (IAgeSymbol symbol : result.getSymbols()) {
            ResourceLocation name = symbol.getRegistryName();
            sb.append("  - ").append(name.getPath())
                    .append(" (").append(symbol.getCategory().name())
                    .append(", instability: ").append(symbol.getInstabilityCost())
                    .append(")\n");
        }

        return sb.toString();
    }
}
