package art.arcane.mystcraft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Random;

/**
 * Utility for weighted random selection from collections.
 * Supports both weighted and evenly distributed selection.
 */
public final class WeightedItemSelector {

  private static final Logger LOGGER = LoggerFactory.getLogger(WeightedItemSelector.class);

  private WeightedItemSelector() {
  }

  /**
   * Calculates the total weight of a collection using default provider.
   */
  public static <T> float getTotalWeight(Collection<T> collection) {
    return getTotalWeight(collection, WeightProviderDefault.INSTANCE);
  }

  /**
   * Calculates the total weight of a collection using specified provider.
   */
  public static <T> float getTotalWeight(Collection<T> collection, IWeightProvider provider) {
    float total = 0.0f;
    for (Object item : collection) {
      total += provider.getItemWeight(item);
    }
    return total;
  }

  /**
   * Gets a random item from a collection using weighted selection and default provider.
   */
  public static <T> T getRandomItem(Random rand, Collection<T> collection) {
    return getRandomItem(rand, collection, WeightProviderDefault.INSTANCE);
  }

  /**
   * Gets a random item from a collection using weighted selection.
   */
  public static <T> T getRandomItem(Random rand, Collection<T> collection, IWeightProvider provider) {
    if (collection == null || collection.isEmpty()) {
      return null;
    }

    float max = getTotalWeight(collection, provider);
    if (max <= 0) {
      return getRandomItemEvenly(rand, collection);
    }

    T last = null;
    float selection = rand.nextFloat() * max;
    for (T item : collection) {
      float weight = provider.getItemWeight(item);
      selection -= weight;
      if (weight > 0) {
        if (selection <= 0) {
          return item;
        }
        last = item;
      }
    }

    LOGGER.warn("Something odd happened when selecting a random item from a weighted collection.");
    return last;
  }

  /**
   * Gets a random item with even distribution (ignores weights).
   */
  public static <T> T getRandomItemEvenly(Random rand, Collection<T> collection) {
    if (collection == null || collection.isEmpty()) {
      return null;
    }

    T last = null;
    float selection = rand.nextFloat() * collection.size();
    for (T item : collection) {
      selection -= 1;
      if (selection <= 0) {
        return item;
      }
      last = item;
    }

    LOGGER.warn("Something odd happened when selecting a random item from an evenly weighted collection.");
    return last;
  }

  /**
   * Interface for objects that can provide their own weight.
   */
  public interface IWeightedItem {
    /**
     * The weight is used to determine how often the item is chosen.
     * Higher = more often; 0 = no chance.
     * Note: In a collection of items with no chance (all 0 weight),
     * the system will default to an even distribution.
     */
    float getWeight();
  }

  /**
   * Interface for external weight providers.
   */
  public interface IWeightProvider {
    float getItemWeight(Object item);
  }

  /**
   * Default weight provider that uses IWeightedItem interface.
   */
  public static class WeightProviderDefault implements IWeightProvider {
    public static final WeightProviderDefault INSTANCE = new WeightProviderDefault();

    private WeightProviderDefault() {
    }

    @Override
    public float getItemWeight(Object item) {
      if (item instanceof IWeightedItem weightedItem) {
        return weightedItem.getWeight();
      }
      return 1.0f;
    }
  }
}
