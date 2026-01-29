package art.arcane.mystcraft.api.world.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Interface for celestial objects (sun, moon, stars) in an Age.
 * Controls visual appearance, position, and day/night cycle effects.
 * <p>
 * Multiple celestials can be registered with an Age, allowing for
 * multiple suns, moons, or custom celestial bodies.
 */
public interface ICelestial {

  /**
   * Gets the celestial type.
   *
   * @return The type (SUN, MOON, STARS)
   */
  CelestialType getCelestialType();

  /**
   * Gets the color of this celestial object.
   *
   * @return RGB color, or -1 for default
   */
  int getColor();

  /**
   * Gets the angle offset for this celestial's orbital axis.
   * This tilts the celestial's path across the sky.
   *
   * @return Angle in degrees (0-360)
   */
  float getAngle();

  /**
   * Gets the phase offset for this celestial.
   * Affects when it rises/sets in the day cycle.
   *
   * @return Phase offset (0-1, where 0.5 = 180 degrees)
   */
  float getPhase();

  /**
   * Gets the period multiplier for this celestial.
   * Affects how fast it moves across the sky.
   *
   * @return Period multiplier (1.0 = normal day length, 2.0 = twice as long)
   */
  default float getPeriod() {
    return 1.0f;
  }

  /**
   * Gets the size multiplier for this celestial.
   *
   * @return Size multiplier (1.0 = normal)
   */
  float getSize();

  /**
   * Whether this celestial is visible.
   *
   * @return true if visible
   */
  boolean isVisible();

  /**
   * Whether this celestial is "dark" (black body/eclipse variant).
   *
   * @return true if dark variant
   */
  boolean isDark();

  /**
   * Whether this celestial provides light (affects day/night cycle).
   * Only suns typically provide light.
   *
   * @return true if this celestial contributes to lighting
   */
  default boolean providesLight() {
    return getCelestialType() == CelestialType.SUN && !isDark();
  }

  /**
   * Gets the altitude angle of this celestial at the given time.
   * Used for determining when the celestial is "up" vs "down".
   *
   * @param dayTime     The current day time (0-1, where 0.25 = noon)
   * @param partialTick Partial tick for smooth interpolation
   * @return Altitude angle where 0 = horizon, positive = above, negative = below
   */
  default float getAltitudeAngle(float dayTime, float partialTick) {
    // Apply phase offset and period
    float adjustedTime = (dayTime + getPhase()) / getPeriod();
    adjustedTime = adjustedTime - (float) Math.floor(adjustedTime); // Wrap to 0-1

    // Convert to angle (0 = sunrise, 0.25 = noon/zenith, 0.5 = sunset, 0.75 = nadir)
    return (float) Math.sin(adjustedTime * Math.PI * 2) * 90.0f;
  }

  /**
   * Gets the time until this celestial next rises (crosses above horizon).
   *
   * @param dayTime The current day time (0-1)
   * @return Time until dawn (0-1 scale)
   */
  default float getTimeToDawn(float dayTime) {
    float adjustedTime = (dayTime + getPhase()) / getPeriod();
    adjustedTime = adjustedTime - (float) Math.floor(adjustedTime);

    // Dawn occurs at adjustedTime = 0 (or 1)
    if (adjustedTime <= 0.5f) {
      return 0.5f - adjustedTime; // Time until sunset, then 0.5 more to next dawn
    } else {
      return 1.0f - adjustedTime;
    }
  }

  /**
   * Renders this celestial object.
   * Called during sky rendering for each registered celestial.
   *
   * @param poseStack   The pose stack for transformations
   * @param level       The client level
   * @param partialTick Partial tick for smooth animation
   * @param dayTime     The current day time (0-1)
   * @param rainLevel   Current rain level (0-1)
   */
  void render(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime, float rainLevel);

  /**
   * Gets the sunset/sunrise color contribution from this celestial.
   *
   * @param dayTime The current day time (0-1)
   * @return RGB color for sunset gradient, or -1 to use default
   */
  default int getSunsetColor(float dayTime) {
    return -1;
  }

  /**
   * Gets an identifier for this celestial (for debugging/logging).
   *
   * @return The identifier string
   */
  String getIdentifier();

  /**
   * Celestial object types.
   */
  enum CelestialType {
    /**
     * Sun - typically provides light and affects day/night
     */
    SUN,
    /**
     * Moon - may have phases, typically visible at night
     */
    MOON,
    /**
     * Stars - background celestial objects
     */
    STARS
  }
}
