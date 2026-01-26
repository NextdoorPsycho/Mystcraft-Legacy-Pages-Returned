package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Lighting control symbols.
 */
public final class LightingSymbols {

    private LightingSymbols() {}

    public static void register() {
        SymbolRegistry.register(new LightingNormal());
        SymbolRegistry.register(new LightingBright());
        SymbolRegistry.register(new LightingDark());
        SymbolRegistry.register(new LightingNether());
    }

    public static class LightingNormal extends SymbolBase {
        public LightingNormal() {
            super(SymbolRegistry.mystcraftId("lighting_normal"), SymbolCategory.LIGHTING);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Light", "Form", "Tradition", "Balance");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setLightingType("normal");
        }
    }

    public static class LightingBright extends SymbolBase {
        public LightingBright() {
            super(SymbolRegistry.mystcraftId("lighting_bright"), SymbolCategory.LIGHTING);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Light", "Form", "Stimulate", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setLightingType("bright");
            director.addInstability(getInstabilityCost());
        }
    }

    public static class LightingDark extends SymbolBase {
        public LightingDark() {
            super(SymbolRegistry.mystcraftId("lighting_dark"), SymbolCategory.LIGHTING);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Light", "Form", "Inhibit", "Void");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setLightingType("dark");
            director.addInstability(getInstabilityCost());
        }
    }

    /**
     * Nether-style dim ambient lighting.
     * Provides a constant dim light level similar to the Nether dimension.
     */
    public static class LightingNether extends SymbolBase {
        public LightingNether() {
            super(SymbolRegistry.mystcraftId("lighting_nether"), SymbolCategory.LIGHTING);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Light", "Form", "Inferno", "Glow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setLightingType("nether");
            director.addInstability(getInstabilityCost());
        }
    }
}
