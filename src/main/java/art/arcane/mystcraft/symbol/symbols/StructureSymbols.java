package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Structure generation symbols.
 */
public final class StructureSymbols {

    private StructureSymbols() {}

    public static void register() {
        SymbolRegistry.register(new Villages());
        SymbolRegistry.register(new Dungeons());
        SymbolRegistry.register(new Mineshafts());
        SymbolRegistry.register(new Strongholds());
    }

    public static class Villages extends SymbolBase {
        public Villages() {
            super(SymbolRegistry.mystcraftId("villages"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Society", "Harmony", "Nurture");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setVillagesEnabled(true);
        }
    }

    public static class Dungeons extends SymbolBase {
        public Dungeons() {
            super(SymbolRegistry.mystcraftId("dungeons"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Constraint", "Chain", "Resurrect");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDungeonsEnabled(true);
        }
    }

    public static class Mineshafts extends SymbolBase {
        public Mineshafts() {
            super(SymbolRegistry.mystcraftId("mineshafts"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Machine", "Motion", "Tradition");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMineshaftsEnabled(true);
        }
    }

    public static class Strongholds extends SymbolBase {
        public Strongholds() {
            super(SymbolRegistry.mystcraftId("strongholds"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Wisdom", "Future", "Honor");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStrongholdsEnabled(true);
        }
    }
}
