package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Terrain feature symbols (caves, ravines, etc).
 */
public final class FeatureSymbols {

    private FeatureSymbols() {}

    public static void register() {
        SymbolRegistry.register(new Caves());
        SymbolRegistry.register(new Ravines());
        SymbolRegistry.register(new FloatingIslands());
        SymbolRegistry.register(new Skylands());
    }

    public static class Caves extends SymbolBase {
        public Caves() {
            super(SymbolRegistry.mystcraftId("caves"), SymbolCategory.FEATURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Transform", "Void", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setCavesEnabled(true);
        }
    }

    public static class Ravines extends SymbolBase {
        public Ravines() {
            super(SymbolRegistry.mystcraftId("ravines"), SymbolCategory.FEATURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Transform", "Void", "Weave");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setRavinesEnabled(true);
        }
    }

    public static class FloatingIslands extends SymbolBase {
        public FloatingIslands() {
            super(SymbolRegistry.mystcraftId("floating_islands"), SymbolCategory.FEATURE);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Terrain", "Transform", "Form", "Celestial");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setFloatingIslandsEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Skylands extends SymbolBase {
        public Skylands() {
            super(SymbolRegistry.mystcraftId("skylands"), SymbolCategory.FEATURE);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Terrain", "Transform", "Void", "Elevate");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSkylandsEnabled(true);
            director.setHasSea(false);
            director.addInstability(getInstabilityCost());
        }
    }
}
