package art.arcane.mystcraft.api.world.logic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Interface for providing dynamic colors that can change based on time, weather, and position.
 * Multiple color providers can be registered and their colors will be averaged.
 *
 * Used for sky color, fog color, cloud color, and other atmosphere effects.
 */
public interface IDynamicColorProvider {

    /**
     * Color type constants for identifying the target of this color provider.
     */
    enum ColorType {
        /** Sky color - affects the overall sky appearance */
        SKY,
        /** Fog color - affects distance fog rendering */
        FOG,
        /** Cloud color - affects cloud rendering */
        CLOUD,
        /** Sunset/sunrise gradient color */
        SUNSET,
        /** Night sky color - affects sky during night */
        NIGHT_SKY
    }

    /**
     * Gets the color type this provider affects.
     *
     * @return The color type
     */
    ColorType getColorType();

    /**
     * Gets the current color value.
     *
     * @param level The level/world
     * @param entity The viewing entity (player), can be null
     * @param partialTick Partial tick for interpolation
     * @return The RGB color value (0xRRGGBB format), or -1 to use default/skip this provider
     */
    int getColor(Level level, Entity entity, float partialTick);

    /**
     * Gets the priority of this color provider.
     * Higher priority providers have more weight in the averaging calculation.
     * Default is 1.0.
     *
     * @return The priority weight (positive value)
     */
    default float getPriority() {
        return 1.0f;
    }

    /**
     * Returns whether this color should be blended based on time of day.
     * If true, the color will be dimmed at night and brighter during day.
     *
     * @return True to enable time-based blending
     */
    default boolean blendWithTimeOfDay() {
        return true;
    }

    /**
     * Returns whether this color should be affected by weather.
     * If true, the color will be darkened during rain/thunder.
     *
     * @return True to enable weather-based blending
     */
    default boolean blendWithWeather() {
        return true;
    }

    /**
     * Gets an identifier for this color provider (for debugging/logging).
     *
     * @return The provider identifier
     */
    String getIdentifier();
}
