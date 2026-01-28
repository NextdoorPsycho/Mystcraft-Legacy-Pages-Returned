package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.util.RandomSource;
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
 * Most terrain types use vanilla terrain with modifications applied on top.
 */
public final class TerrainSymbols {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerrainSymbols.class);

    private TerrainSymbols() {}

    private static final String[] SECONDARY_TERRAIN_POOL = {"amplified", "cave", "skylands", "nether"};
    private static final int[] SECONDARY_TERRAIN_WEIGHTS = {3, 3, 3, 1};

    public static void register() {
        SymbolRegistry.register(new TerrainNormal());
        SymbolRegistry.register(new TerrainAmplified());
        SymbolRegistry.register(new TerrainCave());
        SymbolRegistry.register(new TerrainSkylands());
        SymbolRegistry.register(new TerrainFlat());
        SymbolRegistry.register(new TerrainVoid());
        SymbolRegistry.register(new TerrainEnd());
        SymbolRegistry.register(new TerrainNether());
        SymbolRegistry.register(new TerrainCheckerboard());
        SymbolRegistry.register(new TerrainBlend());
        SymbolRegistry.register(new TerrainStripes());
    }

    /**
     * Picks a secondary terrain type using weighted random selection.
     */
    private static String pickSecondary(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0xDEADBEEFL);
        int totalWeight = 0;
        for (int weight : SECONDARY_TERRAIN_WEIGHTS) {
            totalWeight += weight;
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < SECONDARY_TERRAIN_POOL.length; i++) {
            cumulative += SECONDARY_TERRAIN_WEIGHTS[i];
            if (roll < cumulative) {
                return SECONDARY_TERRAIN_POOL[i];
            }
        }
        return SECONDARY_TERRAIN_POOL[0];
    }

    /**
     * Normal terrain - delegates to vanilla's overworld NoiseBasedChunkGenerator.
     * This produces standard Minecraft terrain with all biome features.
     * Other symbols (Dense Ores, Big Trees, Tendrils, etc.) add on TOP of this.
     */
    public static class TerrainNormal extends SymbolBase {
        public TerrainNormal() {
            super(SymbolRegistry.mystcraftId("terrain_normal"), SymbolCategory.TERRAIN);
            setCardRank(0);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Tradition", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying NORMAL terrain to age (seed: {})", seed);
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
            setCardRank(1);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Form", "Tradition", "Spur");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying AMPLIFIED terrain to age (seed: {})", seed);
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
            setCardRank(6);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Inhibit", "Motion");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying FLAT terrain to age (seed: {})", seed);
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
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Terrain", "Form", "Infinite", "Void");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying VOID terrain to age (seed: {})", seed);
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
            setCardRank(3);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Form", "Ethereal", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying END terrain to age (seed: {})", seed);
            director.setTerrainType("end");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x000000);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Nether terrain - delegates to vanilla's nether NoiseGeneratorSettings.
     * Produces enclosed cave terrain with bedrock ceiling and floor.
     */
    public static class TerrainNether extends SymbolBase {
        public TerrainNether() {
            super(SymbolRegistry.mystcraftId("terrain_nether"), SymbolCategory.TERRAIN);
            setCardRank(3);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Form", "Constraint", "Entropy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying NETHER terrain to age (seed: {})", seed);
            director.setTerrainType("nether");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x330808);
            director.setFogColor(0x330808);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Cave terrain - delegates to vanilla's nether NoiseGeneratorSettings
     * but uses overworld-style blocks and biomes. Produces enclosed cave
     * systems with a bedrock ceiling and standard stone terrain.
     */
    public static class TerrainCave extends SymbolBase {
        public TerrainCave() {
            super(SymbolRegistry.mystcraftId("terrain_cave"), SymbolCategory.TERRAIN);
            setCardRank(2);
            setInstabilityCost(8.0f);
            setPoem("Terrain", "Form", "Constraint", "Depth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying CAVE terrain to age (seed: {})", seed);
            director.setTerrainType("cave");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Skylands terrain - delegates to vanilla's overworld NoiseGeneratorSettings
     * but with a dimension type that starts at Y=0 with no bedrock floor.
     * Produces floating island terrain.
     */
    public static class TerrainSkylands extends SymbolBase {
        public TerrainSkylands() {
            super(SymbolRegistry.mystcraftId("terrain_skylands"), SymbolCategory.TERRAIN);
            setCardRank(2);
            setInstabilityCost(8.0f);
            setPoem("Terrain", "Form", "Ascend", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            LOGGER.debug("[TerrainSymbol] Applying SKYLANDS terrain to age (seed: {})", seed);
            director.setTerrainType("skylands");
            director.setAverageGroundLevel(96);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Checkerboard mixed terrain - alternates two terrain types in 8-chunk regions.
     */
    public static class TerrainCheckerboard extends SymbolBase {
        public TerrainCheckerboard() {
            super(SymbolRegistry.mystcraftId("terrain_checkerboard"), SymbolCategory.TERRAIN);
            setCardRank(2);
            setInstabilityCost(12.0f);
            setPoem("Terrain", "Form", "Divide", "Pattern");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            String secondary = pickSecondary(seed);
            LOGGER.debug("[TerrainSymbol] Applying CHECKERBOARD terrain (normal + {}) to age (seed: {})", secondary, seed);
            director.setTerrainType("normal");
            director.setSecondaryTerrainType(secondary);
            director.setTerrainMixMode("checkerboard");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(63);
            director.setHasSea(true);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Noise blend terrain - uses noise to create organic regions of two terrain types.
     */
    public static class TerrainBlend extends SymbolBase {
        public TerrainBlend() {
            super(SymbolRegistry.mystcraftId("terrain_blend"), SymbolCategory.TERRAIN);
            setCardRank(2);
            setInstabilityCost(10.0f);
            setPoem("Terrain", "Form", "Merge", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            String secondary = pickSecondary(seed);
            LOGGER.debug("[TerrainSymbol] Applying BLEND terrain (normal + {}) to age (seed: {})", secondary, seed);
            director.setTerrainType("normal");
            director.setSecondaryTerrainType(secondary);
            director.setTerrainMixMode("noise");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(63);
            director.setHasSea(true);
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Striped terrain - alternating bands of two terrain types along the X axis.
     */
    public static class TerrainStripes extends SymbolBase {
        public TerrainStripes() {
            super(SymbolRegistry.mystcraftId("terrain_stripes"), SymbolCategory.TERRAIN);
            setCardRank(3);
            setInstabilityCost(12.0f);
            setPoem("Terrain", "Form", "Divide", "Line");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            String secondary = pickSecondary(seed);
            LOGGER.debug("[TerrainSymbol] Applying STRIPES terrain (normal + {}) to age (seed: {})", secondary, seed);
            director.setTerrainType("normal");
            director.setSecondaryTerrainType(secondary);
            director.setTerrainMixMode("stripes");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(63);
            director.setHasSea(true);
            director.addInstability(getInstabilityCost());
        }
    }
}
