package art.arcane.mystcraft.api.symbol;

/**
 * Categories for Age symbols.
 * Symbols in the same category are generally mutually exclusive.
 */
public enum SymbolCategory {

  /**
   * Terrain generation (flat, normal, amplified, void, etc.)
   */
  TERRAIN("terrain"),

  /**
   * Biome controllers (single, native, grid, etc.)
   */
  BIOME_CONTROLLER("biome_controller"),

  /**
   * Individual biomes
   */
  BIOME("biome"),

  /**
   * Weather control (rain, snow, clear, etc.)
   */
  WEATHER("weather"),

  /**
   * Lighting (bright, dark, normal)
   */
  LIGHTING("lighting"),

  /**
   * Color modifiers for world objects (pushed onto stack)
   */
  COLOR("color"),

  /**
   * Visual effects (sky color, fog color, grass color targets)
   */
  VISUAL_EFFECT("visual_effect"),

  /**
   * Environmental effects (accelerated, meteors, lightning, etc.)
   */
  ENVIRONMENT("environment"),

  /**
   * Large terrain features (caves, ravines, floating islands, perlin worms, deep dark)
   */
  FEATURE_LARGE("feature_large"),

  /**
   * Medium terrain features (lakes, huge trees, dense ores, spikes, spheres, tendrils)
   */
  FEATURE_MEDIUM("feature_medium"),

  /**
   * Small terrain features (dripstone caves, lush caves, surface lakes)
   */
  FEATURE_SMALL("feature_small"),

  /**
   * Structures (villages, dungeons, strongholds, etc.)
   */
  STRUCTURE("structure"),

  /**
   * Angle modifiers (north, east, south, west)
   */
  ANGLE("angle"),

  /**
   * Phase modifiers (rising, zenith, setting, nadir)
   */
  PHASE("phase"),

  /**
   * Length modifiers (zero, half, full, double)
   */
  LENGTH("length"),

  /**
   * Sea/fluid symbols (water, lava, modded fluids)
   */
  SEA("sea"),

  /**
   * General modifiers (clear, no sea, etc.)
   */
  MODIFIER("modifier"),

  /**
   * Special symbols (star fissure, crystals, etc.)
   */
  SPECIAL("special");

  private final String name;

  SymbolCategory(String name) {
    this.name = name;
  }

  /**
   * Resolves a category from its string name (case-insensitive).
   * Returns null if no category matches.
   */
  public static SymbolCategory fromName(String name) {
    if (name == null) return null;
    String needle = name.trim().toLowerCase();
    for (SymbolCategory category : values()) {
      if (category.name.equals(needle)) {
        return category;
      }
    }
    return null;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
