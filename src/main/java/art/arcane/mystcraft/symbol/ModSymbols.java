package art.arcane.mystcraft.symbol;

import art.arcane.mystcraft.symbol.symbols.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration class for all built-in Mystcraft symbols.
 */
public final class ModSymbols {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModSymbols.class);

    private ModSymbols() {}

    /**
     * Registers all built-in symbols.
     * Called during mod common setup.
     */
    public static void registerAll() {
        LOGGER.info("Registering Mystcraft symbols...");

        // Terrain symbols
        TerrainSymbols.register();

        // Biome controller symbols
        BiomeControllerSymbols.register();

        // Individual biome symbols (dynamic from registry)
        BiomeSymbols.register();

        // Celestial symbols
        CelestialSymbols.register();

        // Weather symbols
        WeatherSymbols.register();

        // Lighting symbols
        LightingSymbols.register();

        // Feature symbols
        FeatureSymbols.register();

        // Structure symbols
        StructureSymbols.register();

        // Environment symbols
        EnvironmentSymbols.register();

        // Modifier symbols (colors, angles, phases, lengths)
        ModifierSymbols.register();

        // Block symbols (terrain and sea blocks)
        BlockSymbols.register();

        // Color target symbols (apply colors to world elements)
        ColorTargetSymbols.register();

        // Special symbols (star fissure, obelisks, etc.)
        SpecialSymbols.register();

        LOGGER.info("Registered {} symbols", SymbolRegistry.size());
    }
}
