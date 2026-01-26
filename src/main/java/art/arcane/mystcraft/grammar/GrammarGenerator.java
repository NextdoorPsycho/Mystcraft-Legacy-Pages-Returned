package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generates Age configurations from symbol pages.
 * Handles incomplete page sets by filling in defaults and calculating instability.
 */
public class GrammarGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrammarGenerator.class);

    // Grammar rules define how Ages are built
    private static final List<GrammarRule> RULES = new ArrayList<>();

    static {
        // Required rules - every Age needs these
        RULES.add(new GrammarRule.Builder(SymbolCategory.TERRAIN)
                .priority(100)
                .count(1, 1)
                .required()
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.BIOME_CONTROLLER)
                .priority(90)
                .count(1, 1)
                .required()
                .build());

        // Celestials - random ages should have these for variety
        RULES.add(new GrammarRule.Builder(SymbolCategory.SUN)
                .priority(80)
                .count(1, 1)  // Always generate a sun
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.MOON)
                .priority(79)
                .count(1, 1)  // Always generate a moon
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.STARS)
                .priority(78)
                .count(1, 1)  // Always generate stars
                .build());

        // Weather - always have a weather type
        RULES.add(new GrammarRule.Builder(SymbolCategory.WEATHER)
                .priority(70)
                .count(1, 1)  // Always generate weather
                .build());

        // Features - add some variety
        RULES.add(new GrammarRule.Builder(SymbolCategory.FEATURE)
                .priority(60)
                .count(0, 4)
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.STRUCTURE)
                .priority(50)
                .count(0, 4)
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.ENVIRONMENT)
                .priority(40)
                .count(0, 2)
                .build());

        // Modifiers apply to other symbols
        RULES.add(new GrammarRule.Builder(SymbolCategory.ANGLE)
                .priority(30)
                .count(0, 8)
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.PHASE)
                .priority(29)
                .count(0, 8)
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.LENGTH)
                .priority(28)
                .count(0, 4)
                .build());

        RULES.add(new GrammarRule.Builder(SymbolCategory.COLOR)
                .priority(27)
                .count(0, 16)
                .build());

        // Sort by priority (highest first)
        RULES.sort(Comparator.comparingInt(GrammarRule::getPriority).reversed());
    }

    /**
     * Processes a list of symbols and fills in any missing required elements.
     * Returns the complete symbol list plus calculated instability.
     */
    public static GenerationResult generateAge(List<IAgeSymbol> inputSymbols, long seed) {
        RandomSource random = RandomSource.create(seed);
        List<IAgeSymbol> result = new ArrayList<>(inputSymbols);
        float instability = 0.0f;

        // Track what's been provided vs generated
        int providedCount = inputSymbols.size();
        int generatedCount = 0;

        LOGGER.debug("Generating Age with {} input symbols, seed {}", inputSymbols.size(), seed);

        // Process each rule
        for (GrammarRule rule : RULES) {
            SymbolCategory category = rule.getCategory();
            int currentCount = rule.countInList(result);

            // Check if we need to add symbols
            if (currentCount < rule.getMinCount()) {
                int toAdd = rule.getMinCount() - currentCount;
                List<IAgeSymbol> available = SymbolRegistry.getByCategory(category);

                if (available.isEmpty()) {
                    LOGGER.warn("No symbols available for required category: {}", category);
                    // Add instability for missing required symbols
                    if (rule.isRequired()) {
                        instability += 20.0f;
                    }
                    continue;
                }

                for (int i = 0; i < toAdd; i++) {
                    IAgeSymbol generated = selectWeightedSymbol(available, category, random);
                    result.add(generated);
                    generatedCount++;

                    // Generated symbols add instability
                    instability += 5.0f + Math.abs(generated.getInstabilityCost());

                    LOGGER.debug("Generated {} for category {}", generated.getRegistryName(), category);
                }
            }

            // Check if we have too many symbols of a type
            if (currentCount > rule.getMaxCount()) {
                // Extra symbols add instability
                int excess = currentCount - rule.getMaxCount();
                instability += excess * 10.0f;
                LOGGER.debug("Excess {} symbols in category {} adds {} instability",
                        excess, category, excess * 10.0f);
            }
        }

        // Randomly add colors to make ages more vibrant (like original Mystcraft)
        if (providedCount == 0 || !hasCategory(result, SymbolCategory.COLOR)) {
            int colorCount = random.nextInt(4) + 1; // 1-4 random colors
            List<IAgeSymbol> colorSymbols = SymbolRegistry.getByCategory(SymbolCategory.COLOR);
            if (!colorSymbols.isEmpty()) {
                for (int i = 0; i < colorCount; i++) {
                    IAgeSymbol color = colorSymbols.get(random.nextInt(colorSymbols.size()));
                    result.add(color);
                    generatedCount++;
                    instability += 2.0f; // Small instability for random colors
                    LOGGER.debug("Added random color: {}", color.getRegistryName());
                }
            }
        }

        // Randomly add 0-2 features for variety
        if (providedCount == 0) {
            int featureCount = random.nextInt(3); // 0-2 features
            List<IAgeSymbol> featureSymbols = SymbolRegistry.getByCategory(SymbolCategory.FEATURE);
            if (!featureSymbols.isEmpty() && featureCount > 0) {
                for (int i = 0; i < featureCount; i++) {
                    IAgeSymbol feature = featureSymbols.get(random.nextInt(featureSymbols.size()));
                    if (!containsSymbol(result, feature)) {
                        result.add(feature);
                        generatedCount++;
                        instability += 3.0f + feature.getInstabilityCost();
                        LOGGER.debug("Added random feature: {}", feature.getRegistryName());
                    }
                }
            }
        }

        // Calculate instability from provided symbols
        for (IAgeSymbol symbol : inputSymbols) {
            instability += symbol.getInstabilityCost();
        }

        // Bonus for well-written Ages (low generated count)
        if (generatedCount == 0 && providedCount >= 5) {
            instability *= 0.8f; // 20% reduction for complete Ages
        }

        // Clamp instability
        instability = Math.max(0.0f, instability);

        LOGGER.info("Generated Age: {} provided, {} generated, instability {}",
                providedCount, generatedCount, instability);

        return new GenerationResult(result, instability, providedCount, generatedCount);
    }

    /**
     * Selects a symbol with weighting based on category.
     * For terrain, favors normal terrain over exotic types.
     */
    private static IAgeSymbol selectWeightedSymbol(List<IAgeSymbol> available, SymbolCategory category, RandomSource random) {
        if (category == SymbolCategory.TERRAIN) {
            // Weight terrain selection to favor normal terrain
            // Normal terrain: 60%, others: shared 40%
            List<IAgeSymbol> normalTerrain = new ArrayList<>();
            List<IAgeSymbol> exoticTerrain = new ArrayList<>();

            for (IAgeSymbol symbol : available) {
                String path = symbol.getRegistryName().getPath();
                if (path.equals("terrain_normal") || path.equals("terrain_amplified")) {
                    normalTerrain.add(symbol);
                } else {
                    exoticTerrain.add(symbol);
                }
            }

            // 70% chance for normal/amplified terrain, 30% for exotic
            if (!normalTerrain.isEmpty() && (exoticTerrain.isEmpty() || random.nextFloat() < 0.7f)) {
                return normalTerrain.get(random.nextInt(normalTerrain.size()));
            } else if (!exoticTerrain.isEmpty()) {
                return exoticTerrain.get(random.nextInt(exoticTerrain.size()));
            }
        }

        // Default: random selection
        return available.get(random.nextInt(available.size()));
    }

    /**
     * Checks if the list contains any symbols of the given category.
     */
    private static boolean hasCategory(List<IAgeSymbol> symbols, SymbolCategory category) {
        return symbols.stream().anyMatch(s -> s.getCategory() == category);
    }

    /**
     * Checks if the list already contains a specific symbol.
     */
    private static boolean containsSymbol(List<IAgeSymbol> symbols, IAgeSymbol symbol) {
        return symbols.stream().anyMatch(s -> s.getRegistryName().equals(symbol.getRegistryName()));
    }

    /**
     * Generates a completely random Age.
     */
    public static GenerationResult generateRandomAge(long seed) {
        return generateAge(List.of(), seed);
    }

    /**
     * Validates a list of symbols for common issues.
     */
    public static List<String> validateSymbols(List<IAgeSymbol> symbols) {
        List<String> issues = new ArrayList<>();

        // Check for duplicate categories that should be unique
        for (GrammarRule rule : RULES) {
            if (rule.getMaxCount() == 1) {
                int count = rule.countInList(symbols);
                if (count > 1) {
                    issues.add("Multiple " + rule.getCategory().name() + " symbols (only 1 allowed)");
                }
            }
        }

        // Check for conflicting symbols
        boolean hasVoidTerrain = symbols.stream()
                .anyMatch(s -> s.getRegistryName().getPath().equals("terrain_void"));
        boolean hasCaves = symbols.stream()
                .anyMatch(s -> s.getRegistryName().getPath().equals("caves"));

        if (hasVoidTerrain && hasCaves) {
            issues.add("Caves symbol conflicts with Void Terrain");
        }

        // Check for orphaned modifiers (modifiers without something to modify)
        boolean hasModifier = symbols.stream()
                .anyMatch(s -> s.getCategory() == SymbolCategory.ANGLE ||
                        s.getCategory() == SymbolCategory.PHASE ||
                        s.getCategory() == SymbolCategory.LENGTH ||
                        s.getCategory() == SymbolCategory.COLOR);

        boolean hasCelestial = symbols.stream()
                .anyMatch(s -> s.getCategory() == SymbolCategory.SUN ||
                        s.getCategory() == SymbolCategory.MOON ||
                        s.getCategory() == SymbolCategory.STARS);

        if (hasModifier && !hasCelestial) {
            issues.add("Modifier symbols present without celestial objects to modify");
        }

        return issues;
    }

    /**
     * Result of Age generation.
     */
    public static class GenerationResult {
        private final List<IAgeSymbol> symbols;
        private final float instability;
        private final int providedCount;
        private final int generatedCount;

        public GenerationResult(List<IAgeSymbol> symbols, float instability,
                                int providedCount, int generatedCount) {
            this.symbols = symbols;
            this.instability = instability;
            this.providedCount = providedCount;
            this.generatedCount = generatedCount;
        }

        public List<IAgeSymbol> getSymbols() {
            return symbols;
        }

        public float getInstability() {
            return instability;
        }

        public int getProvidedCount() {
            return providedCount;
        }

        public int getGeneratedCount() {
            return generatedCount;
        }

        public boolean isComplete() {
            return generatedCount == 0;
        }
    }
}
