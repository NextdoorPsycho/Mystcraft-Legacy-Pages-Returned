package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Special effect symbols.
 */
public final class SpecialSymbols {

    private SpecialSymbols() {}

    public static void register() {
        SymbolRegistry.register(new StarFissure());
        SymbolRegistry.register(new Obelisks());
        SymbolRegistry.register(new AntiPvP());
        SymbolRegistry.register(new HideHorizon());
        SymbolRegistry.register(new CrystalFormation());
        SymbolRegistry.register(new Rainbow());
    }

    public static class StarFissure extends SymbolBase {
        public StarFissure() {
            super(SymbolRegistry.mystcraftId("star_fissure"), SymbolCategory.SPECIAL);
            setCardRank(4);
            setInstabilityCost(-25.0f);
            setPoem("Portal", "Form", "Home", "Return");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarFissureEnabled(true);
        }
    }

    public static class Obelisks extends SymbolBase {
        public Obelisks() {
            super(SymbolRegistry.mystcraftId("obelisks"), SymbolCategory.SPECIAL);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Structure", "Form", "Mystery", "Ancient");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setObelisksEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class AntiPvP extends SymbolBase {
        public AntiPvP() {
            super(SymbolRegistry.mystcraftId("anti_pvp"), SymbolCategory.SPECIAL);
            setCardRank(4);
            setInstabilityCost(0.0f);
            setPoem("Peace", "Inhibit", "Conflict", "Harmony");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setPvPEnabled(false);
        }
    }

    public static class HideHorizon extends SymbolBase {
        public HideHorizon() {
            super(SymbolRegistry.mystcraftId("hide_horizon"), SymbolCategory.SPECIAL);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Horizon", "Void", "Hide", "Mystery");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setHorizonHidden(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class CrystalFormation extends SymbolBase {
        public CrystalFormation() {
            super(SymbolRegistry.mystcraftId("crystal_formation"), SymbolCategory.SPECIAL);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Crystal", "Form", "Beauty", "Chaos");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setCrystalsEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Rainbow extends SymbolBase {
        public Rainbow() {
            super(SymbolRegistry.mystcraftId("rainbow"), SymbolCategory.SPECIAL);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Color", "Form", "Arc", "Beauty");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setRainbowEnabled(true);
        }
    }
}
