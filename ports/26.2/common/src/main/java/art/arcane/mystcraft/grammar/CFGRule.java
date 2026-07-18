package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.util.WeightedItemSelector;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Context-Free Grammar rule for Age generation. A rule defines how
 * a parent token expands into a list of child tokens.
 */
public record CFGRule(Identifier parent, List<Identifier> values,
                      Integer rank) implements WeightedItemSelector.IWeightedItem {

  /**
   * Creates a new CFG rule.
   *
   * @param parent The token that this rule expands
   * @param values The tokens this rule produces (can be empty for epsilon
   *               rules)
   * @param rank   The rarity rank (null for extension rules, higher = rarer)
   */
  public CFGRule(Identifier parent, List<Identifier> values, Integer rank) {
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
  public static Builder builder(Identifier parent) {
    return new Builder(parent);
  }

  @Override
  public float getWeight() {
    if (rank == null) {
      return 0;
    }

    CFGGrammarGenerator.RankData rankData = CFGGrammarGenerator.getRankData(parent);
    if (rankData != null && rankData.rankWeights != null) {
      Integer weight = rankData.rankWeights.get(rank);
      if (weight != null) {
        return weight;
      }
    }

    return 1;
  }

  /**
   * Gets the parent token that this rule expands.
   */
  @Override
  public Identifier parent() {
    return parent;
  }

  /**
   * Gets the list of tokens this rule produces.
   */
  @Override
  public List<Identifier> values() {
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
    private final Identifier parent;
    private final List<Identifier> values = new ArrayList<>();
    private Integer rank = null;

    public Builder(Identifier parent) {
      this.parent = parent;
    }

    public Builder produces(Identifier... tokens) {
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
