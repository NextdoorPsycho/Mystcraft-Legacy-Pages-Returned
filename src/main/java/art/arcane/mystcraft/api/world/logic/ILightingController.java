package art.arcane.mystcraft.api.world.logic;

/**
 * Interface for lighting controllers that affect the Age's ambient light level.
 */
public interface ILightingController {

    /**
     * Gets the base sky light level for the Age.
     * Normal = 15, Dark = 0, Bright = 15 with overrides
     *
     * @return The sky light level (0-15)
     */
    int getSkyLightLevel();

    /**
     * Gets the minimum light level override.
     * Used by "bright" lighting to ensure a minimum ambient light.
     *
     * @return The minimum light level, or -1 for no override
     */
    int getMinLightLevel();

    /**
     * Whether this Age has a fixed time of day for lighting.
     * @return true if lighting ignores day/night cycle
     */
    boolean hasFixedLighting();

    /**
     * Gets the lighting type identifier.
     * @return The type name (e.g., "normal", "bright", "dark")
     */
    String getType();
}
