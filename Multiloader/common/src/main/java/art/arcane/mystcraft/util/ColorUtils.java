package art.arcane.mystcraft.util;

import art.arcane.mystcraft.api.world.logic.IDynamicColorProvider;
import art.arcane.mystcraft.api.world.logic.IStaticColorProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Utility class for color operations and averaging multiple color providers.
 */
public final class ColorUtils {

    private ColorUtils() {}

    /**
     * Extracts the red component from a packed RGB color.
     */
    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Extracts the green component from a packed RGB color.
     */
    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Extracts the blue component from a packed RGB color.
     */
    public static int getBlue(int color) {
        return color & 0xFF;
    }

    /**
     * Packs RGB components into a single integer.
     */
    public static int packRGB(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Packs float RGB components (0-1 range) into a single integer.
     */
    public static int packRGB(float r, float g, float b) {
        return packRGB(
                Mth.clamp((int) (r * 255), 0, 255),
                Mth.clamp((int) (g * 255), 0, 255),
                Mth.clamp((int) (b * 255), 0, 255)
        );
    }

    /**
     * Linearly interpolates between two colors.
     *
     * @param color1 First color
     * @param color2 Second color
     * @param factor Interpolation factor (0 = color1, 1 = color2)
     * @return The interpolated color
     */
    public static int lerpColor(int color1, int color2, float factor) {
        factor = Mth.clamp(factor, 0.0f, 1.0f);

        int r1 = getRed(color1);
        int g1 = getGreen(color1);
        int b1 = getBlue(color1);

        int r2 = getRed(color2);
        int g2 = getGreen(color2);
        int b2 = getBlue(color2);

        int r = (int) Mth.lerp(factor, r1, r2);
        int g = (int) Mth.lerp(factor, g1, g2);
        int b = (int) Mth.lerp(factor, b1, b2);

        return packRGB(r, g, b);
    }

    /**
     * Averages multiple dynamic color providers with weighted priorities.
     *
     * @param providers List of color providers
     * @param type The color type to query
     * @param level The level
     * @param entity The viewing entity
     * @param partialTick Partial tick
     * @return The averaged color, or -1 if no valid colors
     */
    public static int averageDynamicColors(
            List<IDynamicColorProvider> providers,
            IDynamicColorProvider.ColorType type,
            Level level,
            Entity entity,
            float partialTick
    ) {
        if (providers == null || providers.isEmpty()) {
            return -1;
        }

        float totalWeight = 0.0f;
        float totalR = 0.0f;
        float totalG = 0.0f;
        float totalB = 0.0f;
        int validCount = 0;

        for (IDynamicColorProvider provider : providers) {
            if (provider.getColorType() != type) {
                continue;
            }

            int color = provider.getColor(level, entity, partialTick);
            if (color == -1) {
                continue;
            }

            float weight = Math.max(0.01f, provider.getPriority());
            float r = getRed(color) / 255.0f;
            float g = getGreen(color) / 255.0f;
            float b = getBlue(color) / 255.0f;

            // Apply time of day blending if requested
            if (provider.blendWithTimeOfDay() && level != null) {
                float timeOfDay = level.getTimeOfDay(partialTick);
                float dayFactor = calculateDayFactor(timeOfDay);
                r *= dayFactor;
                g *= dayFactor;
                b *= dayFactor;
            }

            // Apply weather blending if requested
            if (provider.blendWithWeather() && level != null) {
                float rainLevel = level.getRainLevel(partialTick);
                float thunderLevel = level.getThunderLevel(partialTick);
                float weatherDarken = 1.0f - (rainLevel * 0.2f) - (thunderLevel * 0.3f);
                r *= weatherDarken;
                g *= weatherDarken;
                b *= weatherDarken;
            }

            totalR += r * weight;
            totalG += g * weight;
            totalB += b * weight;
            totalWeight += weight;
            validCount++;
        }

        if (validCount == 0 || totalWeight <= 0) {
            return -1;
        }

        // Calculate weighted average
        float avgR = totalR / totalWeight;
        float avgG = totalG / totalWeight;
        float avgB = totalB / totalWeight;

        return packRGB(avgR, avgG, avgB);
    }

    /**
     * Averages multiple static color providers with weighted priorities.
     *
     * @param providers List of color providers
     * @param type The color type to query
     * @param pos The block position
     * @param biome The biome at this position
     * @return The averaged color, or -1 if no valid colors
     */
    public static int averageStaticColors(
            List<IStaticColorProvider> providers,
            IStaticColorProvider.ColorType type,
            BlockPos pos,
            Holder<Biome> biome
    ) {
        if (providers == null || providers.isEmpty()) {
            return -1;
        }

        float totalWeight = 0.0f;
        float totalR = 0.0f;
        float totalG = 0.0f;
        float totalB = 0.0f;
        int validCount = 0;

        for (IStaticColorProvider provider : providers) {
            if (provider.getColorType() != type) {
                continue;
            }

            int color = provider.getColor(pos, biome);
            if (color == -1) {
                continue;
            }

            float weight = Math.max(0.01f, provider.getPriority());
            float r = getRed(color) / 255.0f;
            float g = getGreen(color) / 255.0f;
            float b = getBlue(color) / 255.0f;

            totalR += r * weight;
            totalG += g * weight;
            totalB += b * weight;
            totalWeight += weight;
            validCount++;
        }

        if (validCount == 0 || totalWeight <= 0) {
            return -1;
        }

        // Calculate weighted average
        float avgR = totalR / totalWeight;
        float avgG = totalG / totalWeight;
        float avgB = totalB / totalWeight;

        return packRGB(avgR, avgG, avgB);
    }

    /**
     * Blends an array of gradient colors by averaging their RGB components.
     *
     * @param colors Array of colors
     * @param count Number of valid colors in the array
     * @return The blended color
     */
    public static int blendGradientColors(int[] colors, int count) {
        if (count <= 0) return -1;
        if (count == 1) return colors[0];

        float totalR = 0, totalG = 0, totalB = 0;
        for (int i = 0; i < count; i++) {
            totalR += getRed(colors[i]);
            totalG += getGreen(colors[i]);
            totalB += getBlue(colors[i]);
        }

        return packRGB(
                (int) (totalR / count),
                (int) (totalG / count),
                (int) (totalB / count)
        );
    }

    /**
     * Calculates a day factor (0-1) based on time of day.
     * Returns higher values during day, lower during night.
     *
     * @param timeOfDay The time of day (0-1, where 0.25 is noon)
     * @return Day factor from 0.2 (night) to 1.0 (day)
     */
    public static float calculateDayFactor(float timeOfDay) {
        // Time 0 = sunrise, 0.25 = noon, 0.5 = sunset, 0.75 = midnight
        // Convert to a factor where noon = 1.0 and midnight = 0.2
        float angle = timeOfDay * 2.0f * (float) Math.PI;
        float factor = -Mth.cos(angle) * 0.5f + 0.5f; // 0 at midnight, 1 at noon
        return Mth.clamp(factor * 0.8f + 0.2f, 0.2f, 1.0f);
    }

}
