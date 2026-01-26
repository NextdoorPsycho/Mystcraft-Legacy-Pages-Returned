package art.arcane.mystcraft.api.symbol;

/**
 * Categories for Age symbols.
 * Symbols in the same category are generally mutually exclusive.
 */
public enum SymbolCategory {

    /** Terrain generation (flat, normal, amplified, void, etc.) */
    TERRAIN("terrain"),

    /** Biome controllers (single, native, grid, etc.) */
    BIOME_CONTROLLER("biome_controller"),

    /** Individual biomes */
    BIOME("biome"),

    /** Sun configuration */
    SUN("sun"),

    /** Moon configuration */
    MOON("moon"),

    /** Stars configuration */
    STARS("stars"),

    /** Weather control (rain, snow, clear, etc.) */
    WEATHER("weather"),

    /** Lighting (bright, dark, normal) */
    LIGHTING("lighting"),

    /** Color modifiers for celestial/world objects */
    COLOR("color"),

    /** Environmental effects (accelerated, meteors, lightning, etc.) */
    ENVIRONMENT("environment"),

    /** Terrain features (caves, ravines, floating islands, etc.) */
    FEATURE("feature"),

    /** Structures (villages, dungeons, strongholds, etc.) */
    STRUCTURE("structure"),

    /** Angle modifiers (north, east, south, west) */
    ANGLE("angle"),

    /** Phase modifiers (rising, zenith, setting, nadir) */
    PHASE("phase"),

    /** Length modifiers (zero, half, full, double) */
    LENGTH("length"),

    /** Special symbols (star fissure, crystals, etc.) */
    SPECIAL("special");

    private final String name;

    SymbolCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
