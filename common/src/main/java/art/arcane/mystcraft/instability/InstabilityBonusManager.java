package art.arcane.mystcraft.instability;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages instability bonuses and penalties for an Age.
 * Bonuses can modify the effective instability level.
 */
public class InstabilityBonusManager {

  /**
   * A zero manager that always returns 0 bonus.
   */
  public static final InstabilityBonusManager ZERO = new InstabilityBonusManager();
  private static final Logger LOGGER = LoggerFactory.getLogger(InstabilityBonusManager.class);
  private static final Set<IInstabilityBonusProvider> bonusProviders = new HashSet<>();
  private final Set<IInstabilityBonus> bonuses = new HashSet<>();
  private int total;
  private boolean instabilityEnabled = true;

  /**
   * Creates a default (zero) bonus manager.
   */
  public InstabilityBonusManager() {
  }

  /**
   * Creates a bonus manager for a specific dimension.
   *
   * @param level The server level
   * @param dimId The dimension ID
   */
  public InstabilityBonusManager(ServerLevel level, int dimId) {
    for (IInstabilityBonusProvider provider : bonusProviders) {
      try {
        provider.register(this, dimId);
      } catch (Exception e) {
        LOGGER.error("Error registering bonus provider: {}", e.getMessage());
      }
    }
  }

  /**
   * Registers a global bonus provider.
   *
   * @param provider The provider to register
   */
  public static void registerBonusProvider(IInstabilityBonusProvider provider) {
    bonusProviders.add(provider);
  }

  /**
   * Unregisters a global bonus provider.
   *
   * @param provider The provider to unregister
   */
  public static void unregisterBonusProvider(IInstabilityBonusProvider provider) {
    bonusProviders.remove(provider);
  }

  /**
   * Registers a bonus with this manager.
   *
   * @param bonus The bonus to register
   */
  public void register(IInstabilityBonus bonus) {
    bonuses.add(bonus);
    LOGGER.debug("Registered instability bonus: {}", bonus.getName());
  }

  /**
   * Unregisters a bonus from this manager.
   *
   * @param bonus The bonus to unregister
   */
  public void unregister(IInstabilityBonus bonus) {
    bonuses.remove(bonus);
  }

  /**
   * Gets the total bonus/penalty value.
   * Positive = stability bonus, Negative = instability penalty.
   */
  public int getResult() {
    return total;
  }

  /**
   * Updates all bonuses and recalculates the total.
   *
   * @param level The server level
   */
  public void tick(ServerLevel level) {
    int newTotal = 0;
    for (IInstabilityBonus bonus : bonuses) {
      try {
        bonus.tick(level);
        newTotal += bonus.getValue();
      } catch (Exception e) {
        LOGGER.error("Error ticking instability bonus '{}': {}", bonus.getName(), e.getMessage());
      }
    }
    this.total = newTotal;
  }

  /**
   * Checks if instability is enabled for this manager.
   */
  public boolean isInstabilityEnabled() {
    return instabilityEnabled;
  }

  /**
   * Sets whether instability is enabled.
   */
  public void setInstabilityEnabled(boolean enabled) {
    this.instabilityEnabled = enabled;
  }

  /**
   * Gets the number of active bonuses.
   */
  public int getBonusCount() {
    return bonuses.size();
  }

  /**
   * Gets a debug summary of all bonuses.
   */
  public String getDebugSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Instability Bonuses (").append(bonuses.size()).append(" total, sum=").append(total).append("):\n");
    for (IInstabilityBonus bonus : bonuses) {
      sb.append("  ").append(bonus.getName()).append(": ").append(bonus.getValue()).append("\n");
    }
    return sb.toString();
  }

  /**
   * Interface for instability bonuses/penalties.
   */
  public interface IInstabilityBonus {
    /**
     * Gets the display name of this bonus.
     */
    String getName();

    /**
     * Gets the current value (positive = bonus/stability, negative = penalty/instability).
     */
    int getValue();

    /**
     * Called each tick to update the bonus.
     *
     * @param level The server level
     */
    void tick(ServerLevel level);
  }

  /**
   * Interface for registering instability bonuses.
   */
  public interface IInstabilityBonusProvider {
    /**
     * Registers bonuses with the manager.
     *
     * @param manager The bonus manager
     * @param dimId   The dimension ID
     */
    void register(InstabilityBonusManager manager, int dimId);
  }
}
