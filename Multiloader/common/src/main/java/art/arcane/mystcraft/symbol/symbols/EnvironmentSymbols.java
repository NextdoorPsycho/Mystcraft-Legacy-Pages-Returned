package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Environmental effect symbols.
 */
public final class EnvironmentSymbols {

    private EnvironmentSymbols() {}

    public static void register() {
        SymbolRegistry.register(new Accelerated());
        SymbolRegistry.register(new Meteors());
        SymbolRegistry.register(new Lightning());
        SymbolRegistry.register(new Scorched());
        SymbolRegistry.register(new Explosions());
    }

    public static class Accelerated extends SymbolBase {
        public Accelerated() {
            super(SymbolRegistry.mystcraftId("env_accelerated"), SymbolCategory.ENVIRONMENT);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Environment", "Dynamic", "Change", "Spur");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setAcceleratedEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Meteors extends SymbolBase {
        public Meteors() {
            super(SymbolRegistry.mystcraftId("env_meteors"), SymbolCategory.ENVIRONMENT);
            setCardRank(3);
            setInstabilityCost(25.0f);
            setPoem("Environment", "Sacrifice", "Power", "Momentum");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMeteorsEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Lightning extends SymbolBase {
        public Lightning() {
            super(SymbolRegistry.mystcraftId("env_lightning"), SymbolCategory.ENVIRONMENT);
            setCardRank(3);
            setInstabilityCost(15.0f);
            setPoem("Environment", "Sacrifice", "Power", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setLightningEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Scorched extends SymbolBase {
        public Scorched() {
            super(SymbolRegistry.mystcraftId("env_scorched"), SymbolCategory.ENVIRONMENT);
            setCardRank(3);
            setInstabilityCost(20.0f);
            setPoem("Environment", "Sacrifice", "Power", "Chaos");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setScorchedEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Explosions extends SymbolBase {
        public Explosions() {
            super(SymbolRegistry.mystcraftId("env_explosions"), SymbolCategory.ENVIRONMENT);
            setCardRank(3);
            setInstabilityCost(30.0f);
            setPoem("Environment", "Sacrifice", "Power", "Entropy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setExplosionsEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }
}
