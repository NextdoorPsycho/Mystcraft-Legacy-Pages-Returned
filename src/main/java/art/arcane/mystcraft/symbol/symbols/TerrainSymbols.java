package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terrain generation symbols.
 *
 * For normal/amplified terrain: These symbols set the terrain type and parameters.
 * The AgeChunkGenerator will delegate to vanilla's NoiseBasedChunkGenerator,
 * allowing symbols to MODULATE vanilla terrain rather than replace it.
 *
 * For special terrain (void, flat): These symbols set the terrain type which
 * triggers custom generation in AgeChunkGenerator.
 *
 * This matches how legacy Mystcraft worked - most terrain types were vanilla
 * terrain with modifications applied on top.
 */
public final class TerrainSymbols {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerrainSymbols.class);

    private TerrainSymbols() {}

    public static void register() {
        SymbolRegistry.register(new TerrainNormal());
        SymbolRegistry.register(new TerrainAmplified());
        SymbolRegistry.register(new TerrainFlat());
        SymbolRegistry.register(new TerrainVoid());
        SymbolRegistry.register(new TerrainEnd());
        SymbolRegistry.register(new TerrainNether());
    }

    /**
     * Normal terrain - delegates to vanilla's overworld NoiseBasedChunkGenerator.
     * This produces standard Minecraft terrain with all biome features.
     * Other symbols (Dense Ores, Big Trees, Tendrils, etc.) add on TOP of this.
     */
    public static class TerrainNormal extends SymbolBase {
        public TerrainNormal() {
            super(SymbolRegistry.mystcraftId("terrain_normal"), SymbolCategory.TERRAIN);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Tradition", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.info("[TerrainSymbol] Applying NORMAL terrain to age (seed: {})", seed);
            director.setTerrainType("normal");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(63);
            director.setHasSea(true);
            // Vanilla handles all terrain, surface, and biome decoration
            // Mystcraft populators (Dense Ores, Big Trees, etc.) add on top
        }
    }

    /**
     * Amplified terrain - delegates to vanilla's amplified NoiseGeneratorSettings.
     * Produces extreme terrain with very tall mountains and deep valleys.
     */
    public static class TerrainAmplified extends SymbolBase {
        public TerrainAmplified() {
            super(SymbolRegistry.mystcraftId("terrain_amplified"), SymbolCategory.TERRAIN);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Form", "Tradition", "Spur");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.info("[TerrainSymbol] Applying AMPLIFIED terrain to age (seed: {})", seed);
            director.setTerrainType("amplified");
            director.setAverageGroundLevel(96);
            director.setSeaLevel(63);
            director.setHasSea(true);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Flat terrain - custom generation, not vanilla-based.
     * Produces a superflat-like world with a single layer of terrain block.
     */
    public static class TerrainFlat extends SymbolBase {
        public TerrainFlat() {
            super(SymbolRegistry.mystcraftId("terrain_flat"), SymbolCategory.TERRAIN);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Inhibit", "Motion");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.info("[TerrainSymbol] Applying FLAT terrain to age (seed: {})", seed);
            director.setTerrainType("flat");
            director.setAverageGroundLevel(4);
            director.setSeaLevel(-64); // Below world, effectively no sea
            director.setHasSea(false);
        }
    }

    /**
     * Void terrain - custom generation.
     * Produces an empty world with only bedrock at the bottom.
     */
    public static class TerrainVoid extends SymbolBase {
        public TerrainVoid() {
            super(SymbolRegistry.mystcraftId("terrain_void"), SymbolCategory.TERRAIN);
            setCardRank(4);
            setInstabilityCost(10.0f);
            setPoem("Terrain", "Form", "Infinite", "Void");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.info("[TerrainSymbol] Applying VOID terrain to age (seed: {})", seed);
            director.setTerrainType("void");
            director.setAverageGroundLevel(65); // Platform at Y=64, spawn on top at Y=65
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * End terrain - TODO: Could delegate to vanilla's end noise settings.
     * For now, uses custom generation.
     */
    public static class TerrainEnd extends SymbolBase {
        public TerrainEnd() {
            super(SymbolRegistry.mystcraftId("terrain_end"), SymbolCategory.TERRAIN);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Form", "Ethereal", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.info("[TerrainSymbol] Applying END terrain to age (seed: {})", seed);
            director.setTerrainType("end");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x000000);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Nether terrain - TODO: Could delegate to vanilla's nether noise settings.
     * For now, uses custom generation.
     */
    public static class TerrainNether extends SymbolBase {
        public TerrainNether() {
            super(SymbolRegistry.mystcraftId("terrain_nether"), SymbolCategory.TERRAIN);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Form", "Constraint", "Entropy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.info("[TerrainSymbol] Applying NETHER terrain to age (seed: {})", seed);
            director.setTerrainType("nether");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x330808);
            director.setFogColor(0x330808);
            director.addInstability(getInstabilityCost());
        }
    }
}
