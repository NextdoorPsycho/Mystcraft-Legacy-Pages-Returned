package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Biome controller symbols that determine how biomes are distributed.
 */
public final class BiomeControllerSymbols {

    private BiomeControllerSymbols() {}

    public static void register() {
        SymbolRegistry.register(new BiomeSingle());
        SymbolRegistry.register(new BiomeNative());
        SymbolRegistry.register(new BiomeTiny());
        SymbolRegistry.register(new BiomeSmall());
        SymbolRegistry.register(new BiomeMedium());
        SymbolRegistry.register(new BiomeLarge());
        SymbolRegistry.register(new BiomeHuge());
        SymbolRegistry.register(new BiomeTiled());
        SymbolRegistry.register(new BiomeGrid());
    }

    public static class BiomeSingle extends SymbolBase {
        public BiomeSingle() {
            super(SymbolRegistry.mystcraftId("biome_single"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(2);
            setInstabilityCost(-5.0f);
            setPoem("Biome", "Form", "Constraint", "Identity");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("single");
        }
    }

    public static class BiomeNative extends SymbolBase {
        public BiomeNative() {
            super(SymbolRegistry.mystcraftId("biome_native"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(1);
            setInstabilityCost(-5.0f);
            setPoem("Biome", "Form", "Tradition", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("native");
        }
    }

    public static class BiomeSmall extends SymbolBase {
        public BiomeSmall() {
            super(SymbolRegistry.mystcraftId("biome_small"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(2);
            setInstabilityCost(-3.0f);
            setPoem("Biome", "Form", "Cycle", "Entropy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("small");
        }
    }

    public static class BiomeMedium extends SymbolBase {
        public BiomeMedium() {
            super(SymbolRegistry.mystcraftId("biome_medium"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(1);
            setInstabilityCost(-4.0f);
            setPoem("Biome", "Form", "Cycle", "Balance");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("medium");
        }
    }

    public static class BiomeLarge extends SymbolBase {
        public BiomeLarge() {
            super(SymbolRegistry.mystcraftId("biome_large"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(2);
            setInstabilityCost(-3.0f);
            setPoem("Biome", "Form", "Cycle", "Order");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("large");
        }
    }

    public static class BiomeHuge extends SymbolBase {
        public BiomeHuge() {
            super(SymbolRegistry.mystcraftId("biome_huge"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(3);
            setInstabilityCost(-2.0f);
            setPoem("Biome", "Form", "Cycle", "Spur");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("huge");
        }
    }

    public static class BiomeTiled extends SymbolBase {
        public BiomeTiled() {
            super(SymbolRegistry.mystcraftId("biome_tiled"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Biome", "Form", "System", "Weave");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("tiled");
        }
    }

    public static class BiomeTiny extends SymbolBase {
        public BiomeTiny() {
            super(SymbolRegistry.mystcraftId("biome_tiny"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(2);
            setInstabilityCost(-2.0f);
            setPoem("Biome", "Form", "Cycle", "Chaos");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("tiny");
        }
    }

    public static class BiomeGrid extends SymbolBase {
        public BiomeGrid() {
            super(SymbolRegistry.mystcraftId("biome_grid"), SymbolCategory.BIOME_CONTROLLER);
            setCardRank(3);
            setInstabilityCost(2.0f);
            setPoem("Biome", "Form", "System", "Order");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBiomeController("grid");
            director.addInstability(getInstabilityCost());
        }
    }
}
