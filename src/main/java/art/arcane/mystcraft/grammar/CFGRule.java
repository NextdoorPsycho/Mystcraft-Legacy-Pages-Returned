package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.util.WeightedItemSelector;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Context-Free Grammar rule for Age generation.
 * A rule defines how a parent token expands into a list of child tokens.
 */
public class CFGRule implements WeightedItemSelector.IWeightedItem {

    private final ResourceLocation parent;
    private final List<ResourceLocation> values;
    private final Integer rank;

    /**
     * Creates a new CFG rule.
     *
     * @param parent The token that this rule expands
     * @param values The tokens this rule produces (can be empty for epsilon rules)
     * @param rank   The rarity rank (null for extension rules, higher = rarer)
     */
    public CFGRule(ResourceLocation parent, List<ResourceLocation> values, Integer rank) {
        if (parent == null) {
            throw new IllegalArgumentException("CFG Rule requires a parent token");
        }
        this.parent = parent;
        this.values = values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
        this.rank = rank;
    }

    @Override
    public float getWeight() {
        if (rank == null) {
            return 0;
        }
        // Weight inversely proportional to rank - lower rank = more common = higher weight
        // Rank 0 = weight 1000, Rank 1 = weight 100, Rank 2 = weight 10, etc.
        return (float) Math.pow(10, 3 - rank);
    }

    /**
     * Gets the parent token that this rule expands.
     */
    public ResourceLocation getParent() {
        return parent;
    }

    /**
     * Gets the list of tokens this rule produces.
     */
    public List<ResourceLocation> getValues() {
        return values;
    }

    /**
     * Gets the number of tokens this rule produces.
     */
    public int size() {
        return values.size();
    }

    /**
     * Gets the rarity rank (null for extension rules).
     */
    public Integer getRank() {
        return rank;
    }

    /**
     * Checks if this rule has a rarity rank.
     */
    public boolean hasRank() {
        return rank != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(parent).append(" -> ");
        if (values.isEmpty()) {
            sb.append("(epsilon)");
        } else {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(values.get(i));
            }
        }
        if (rank != null) {
            sb.append(" [rank=").append(rank).append("]");
        }
        return sb.toString();
    }

    /**
     * Builder for creating CFG rules.
     */
    public static class Builder {
        private final ResourceLocation parent;
        private final List<ResourceLocation> values = new ArrayList<>();
        private Integer rank = null;

        public Builder(ResourceLocation parent) {
            this.parent = parent;
        }

        public Builder produces(ResourceLocation... tokens) {
            Collections.addAll(values, tokens);
            return this;
        }

        public Builder rank(int rank) {
            this.rank = rank;
            return this;
        }

        public CFGRule build() {
            return new CFGRule(parent, values, rank);
        }
    }

    /**
     * Creates a rule builder with the given parent token.
     */
    public static Builder builder(ResourceLocation parent) {
        return new Builder(parent);
    }
}
