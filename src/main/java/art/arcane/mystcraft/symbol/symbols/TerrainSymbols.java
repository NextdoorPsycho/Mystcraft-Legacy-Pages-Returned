package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.populate.BiomeDecorationPopulator;
import art.arcane.mystcraft.world.gen.populate.StandardOresPopulator;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorEnd;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorFlat;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorNether;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorNormal;
import art.arcane.mystcraft.world.gen.terrain.TerrainGeneratorVoid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terrain generation symbols.
 * These symbols now register actual terrain generator implementations
 * rather than just setting configuration strings.
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
            director.setAverageGroundLevel(64);
            director.setSeaLevel(63);

            // Create and register actual terrain generator
            TerrainGeneratorNormal generator = new TerrainGeneratorNormal(director, seed, false);

            // Apply terrain block modifiers if any were pushed
            // (from BlockSymbols like terrain_stone, terrain_deepslate, etc.)
            // The generator will use its defaults if none specified

            director.registerInterface(generator);

            // Register default populators for normal terrain
            // Standard ores provide baseline ore generation (unless Dense Ores is used)
            director.registerInterface(new StandardOresPopulator(seed));

            // Biome decoration provides trees, grass, flowers
            director.registerInterface(new BiomeDecorationPopulator(seed));
        }
    }

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
            director.setAverageGroundLevel(96);
            director.setSeaLevel(63);

            // Create amplified terrain generator (same as normal but with amplified flag)
            TerrainGeneratorNormal generator = new TerrainGeneratorNormal(director, seed, true);
            director.registerInterface(generator);

            // Register default populators for amplified terrain
            director.registerInterface(new StandardOresPopulator(seed));
            director.registerInterface(new BiomeDecorationPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

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
            director.setAverageGroundLevel(4);
            director.setSeaLevel(-64); // Below world, effectively no sea
            director.setHasSea(false);

            // Create flat terrain generator
            TerrainGeneratorFlat generator = new TerrainGeneratorFlat(director, seed);
            director.registerInterface(generator);

            // Flat terrain still has ores underground, but no vegetation
            director.registerInterface(new StandardOresPopulator(seed));
        }
    }

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
            director.setAverageGroundLevel(0);
            director.setSeaLevel(-64);
            director.setHasSea(false);

            // Create void terrain generator
            TerrainGeneratorVoid generator = new TerrainGeneratorVoid(director, seed);
            director.registerInterface(generator);

            director.addInstability(getInstabilityCost());
        }
    }

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
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x000000);

            // Create End terrain generator
            TerrainGeneratorEnd generator = new TerrainGeneratorEnd(director, seed);
            director.registerInterface(generator);

            // End terrain has ores hidden in the end stone
            director.registerInterface(new StandardOresPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

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
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x330808);
            director.setFogColor(0x330808);

            // Create Nether terrain generator
            TerrainGeneratorNether generator = new TerrainGeneratorNether(director, seed);
            director.registerInterface(generator);

            // Nether terrain has ores in the netherrack
            director.registerInterface(new StandardOresPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }
}
