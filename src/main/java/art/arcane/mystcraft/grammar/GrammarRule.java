package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;

import java.util.List;
import java.util.Set;

/**
 * Represents a grammar rule for symbol generation.
 * Rules define what symbols can be generated and their prerequisites.
 */
public class GrammarRule {

    private final SymbolCategory category;
    private final int priority;
    private final int minCount;
    private final int maxCount;
    private final Set<SymbolCategory> prerequisites;
    private final boolean required;

    public GrammarRule(SymbolCategory category, int priority, int minCount, int maxCount,
                       Set<SymbolCategory> prerequisites, boolean required) {
        this.category = category;
        this.priority = priority;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.prerequisites = prerequisites;
        this.required = required;
    }

    public SymbolCategory getCategory() {
        return category;
    }

    public int getPriority() {
        return priority;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public Set<SymbolCategory> getPrerequisites() {
        return prerequisites;
    }

    public boolean isRequired() {
        return required;
    }

    /**
     * Checks if the prerequisites are satisfied by the given symbols.
     */
    public boolean prerequisitesSatisfied(List<IAgeSymbol> existingSymbols) {
        if (prerequisites.isEmpty()) {
            return true;
        }

        for (SymbolCategory prereq : prerequisites) {
            boolean found = false;
            for (IAgeSymbol symbol : existingSymbols) {
                if (symbol.getCategory() == prereq) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Counts how many symbols of this rule's category exist.
     */
    public int countInList(List<IAgeSymbol> symbols) {
        int count = 0;
        for (IAgeSymbol symbol : symbols) {
            if (symbol.getCategory() == category) {
                count++;
            }
        }
        return count;
    }

    /**
     * Builder for grammar rules.
     */
    public static class Builder {
        private final SymbolCategory category;
        private int priority = 0;
        private int minCount = 0;
        private int maxCount = 1;
        private Set<SymbolCategory> prerequisites = Set.of();
        private boolean required = false;

        public Builder(SymbolCategory category) {
            this.category = category;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder count(int min, int max) {
            this.minCount = min;
            this.maxCount = max;
            return this;
        }

        public Builder prerequisites(SymbolCategory... prereqs) {
            this.prerequisites = Set.of(prereqs);
            return this;
        }

        public Builder required() {
            this.required = true;
            return this;
        }

        public GrammarRule build() {
            return new GrammarRule(category, priority, minCount, maxCount, prerequisites, required);
        }
    }
}
