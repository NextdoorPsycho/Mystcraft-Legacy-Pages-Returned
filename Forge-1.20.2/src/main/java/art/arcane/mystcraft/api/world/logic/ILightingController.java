package art.arcane.mystcraft.api.world.logic;

/**
 * Interface for lighting controllers that affect the Age's ambient light level.
 * Controls the brightness curve used for rendering and light propagation.
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

    /**
     * Scales a block light value according to this controller's rules.
     * Used for modifying how block light sources propagate.
     *
     * @param blockLightValue The original block light value (0-15)
     * @return The scaled light value
     */
    default int scaleLighting(int blockLightValue) {
        return blockLightValue;
    }

    /**
     * Generates the light brightness table used for rendering.
     * The table maps light levels (0-15) to brightness values (0.0-1.0).
     * Index 0 is darkest, index 15 is brightest.
     *
     * @param lightBrightnessTable Array of 16 floats to populate with brightness values
     */
    default void generateLightBrightnessTable(float[] lightBrightnessTable) {
        // Default implementation: standard Minecraft brightness curve
        float f = 0.0F;
        for (int i = 0; i <= 15; ++i) {
            float f1 = 1.0F - (float) i / 15.0F;
            lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - f) + f;
        }
    }

    /**
     * Gets the ambient light modifier applied on top of sky light.
     * Values > 1.0 brighten, values < 1.0 darken.
     *
     * @return The ambient light multiplier
     */
    default float getAmbientLightMultiplier() {
        return 1.0f;
    }

    /**
     * Gets the darkness factor for the Age.
     * 0.0 = full light, 1.0 = pitch black even in daylight.
     *
     * @return The darkness factor (0.0-1.0)
     */
    default float getDarknessFactor() {
        return 0.0f;
    }
}
