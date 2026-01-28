package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Symbols controlling the day/night cycle speed in an Age.
 */
public final class TimescaleSymbols {

    private TimescaleSymbols() {}

    public static void register() {
        SymbolRegistry.register(new LongerDays());
        SymbolRegistry.register(new ShorterDays());
        SymbolRegistry.register(new SlowTime());
        SymbolRegistry.register(new StaticTime());
    }

    /** Day/night cycle at half speed (40-minute days instead of 20). */
    public static class LongerDays extends SymbolBase {
        public LongerDays() {
            super(SymbolRegistry.mystcraftId("env_longer_days"), SymbolCategory.ENVIRONMENT);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Environment", "Sustain", "Endure", "Patience");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTimescale(0.5f);
            director.addInstability(getInstabilityCost());
        }
    }

    /** Day/night cycle at double speed (10-minute days instead of 20). */
    public static class ShorterDays extends SymbolBase {
        public ShorterDays() {
            super(SymbolRegistry.mystcraftId("env_shorter_days"), SymbolCategory.ENVIRONMENT);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Environment", "Dynamic", "Haste", "Brevity");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTimescale(2.0f);
            director.addInstability(getInstabilityCost());
        }
    }

    /** Day/night cycle at quarter speed (80-minute days instead of 20). */
    public static class SlowTime extends SymbolBase {
        public SlowTime() {
            super(SymbolRegistry.mystcraftId("env_slow_time"), SymbolCategory.ENVIRONMENT);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Environment", "Sustain", "Stasis", "Crawl");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTimescale(0.25f);
            director.addInstability(getInstabilityCost());
        }
    }

    /** Day/night cycle is frozen. Time of day never changes. */
    public static class StaticTime extends SymbolBase {
        public StaticTime() {
            super(SymbolRegistry.mystcraftId("env_static_time"), SymbolCategory.ENVIRONMENT);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Environment", "Sustain", "Stasis", "Eternity");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTimescale(0.0f);
            director.addInstability(getInstabilityCost());
        }
    }
}
