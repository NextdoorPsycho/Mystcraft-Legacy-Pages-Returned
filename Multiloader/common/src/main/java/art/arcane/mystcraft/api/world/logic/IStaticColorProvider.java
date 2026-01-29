package art.arcane.mystcraft.api.world.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Interface for providing static colors that don't change dynamically.
 * Multiple color providers can be registered and their colors will be averaged.
 * <p>
 * Used for grass color, foliage color, water color, and other block-based colors.
 */
public interface IStaticColorProvider {

  /**
   * Gets the color type this provider affects.
   *
   * @return The color type
   */
  ColorType getColorType();

  /**
   * Gets the color value for a position.
   *
   * @param pos   The block position
   * @param biome The biome at this position (can be null)
   * @return The RGB color value (0xRRGGBB format), or -1 to use default/skip this provider
   */
  int getColor(BlockPos pos, Holder<Biome> biome);

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
   * Returns whether this color should vary based on biome.
   * If true and biome is available, the color may be tinted based on biome climate.
   *
   * @return True to enable biome-based variation
   */
  default boolean varyByBiome() {
    return false;
  }

  /**
   * Gets an identifier for this color provider (for debugging/logging).
   *
   * @return The provider identifier
   */
  String getIdentifier();

  /**
   * Color type constants for identifying the target of this color provider.
   */
  enum ColorType {
    /**
     * Grass block color
     */
    GRASS,
    /**
     * Foliage (leaves) color
     */
    FOLIAGE,
    /**
     * Water color
     */
    WATER
  }
}
