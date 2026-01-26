package art.arcane.mystcraft.api.world.logic;

/**
 * Interface for celestial objects (sun, moon, stars) in an Age.
 * Controls visual appearance and day/night cycle effects.
 */
public interface ICelestial {

    /**
     * Gets the celestial type.
     * @return The type (SUN, MOON, STARS)
     */
    CelestialType getCelestialType();

    /**
     * Gets the color of this celestial object.
     * @return RGB color, or -1 for default
     */
    int getColor();

    /**
     * Gets the angle offset for this celestial's position.
     * @return Angle in degrees (0-360)
     */
    float getAngle();

    /**
     * Gets the phase offset for this celestial.
     * Affects when it rises/sets in the day cycle.
     * @return Phase offset in degrees
     */
    float getPhase();

    /**
     * Gets the size multiplier for this celestial.
     * @return Size multiplier (1.0 = normal)
     */
    float getSize();

    /**
     * Whether this celestial is visible.
     * @return true if visible
     */
    boolean isVisible();

    /**
     * Whether this celestial is "dark" (black body).
     * @return true if dark variant
     */
    boolean isDark();

    /**
     * Celestial object types.
     */
    enum CelestialType {
        SUN,
        MOON,
        STARS
    }
}
