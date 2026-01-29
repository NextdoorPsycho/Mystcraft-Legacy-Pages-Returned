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
public record CFGRule(ResourceLocation parent, List<ResourceLocation> values, Integer rank) implements WeightedItemSelector.IWeightedItem {

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

  /**
   * Creates a rule builder with the given parent token.
   */
  public static Builder builder(ResourceLocation parent) {
    return new Builder(parent);
  }

  @Override
  public float getWeight() {
    if (rank == null) {
      return 0;
    }
    // Look up the dynamically computed weight from the grammar generator's rank data.
    // Each rule's weight depends on the rank distribution of all rules
    // sharing the same parent token.
    CFGGrammarGenerator.RankData rankData = CFGGrammarGenerator.getRankData(parent);
    if (rankData != null && rankData.rankWeights != null) {
      Integer weight = rankData.rankWeights.get(rank);
      if (weight != null) {
        return weight;
      }
    }
    // Fallback if grammar hasn't been built yet
    return 1;
  }

  /**
   * Gets the parent token that this rule expands.
   */
  @Override
  public ResourceLocation parent() {
    return parent;
  }

  /**
   * Gets the list of tokens this rule produces.
   */
  @Override
  public List<ResourceLocation> values() {
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
  @Override
  public Integer rank() {
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
}
