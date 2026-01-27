package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerGrid;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerNative;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerNoise;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerSingle;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerTiled;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * Biome controller symbols that determine how biomes are distributed.
 * These symbols now register actual biome controller implementations
 * rather than just setting configuration strings.
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

    /**
     * Collects biomes from the director's biome list and modifier stack.
     */
    private static List<Holder<Biome>> collectBiomes(AgeDirector director) {
        List<Holder<Biome>> biomes = new ArrayList<>();

        // First add any biomes pushed onto the modifier stack
        Holder<Biome> biome;
        while ((biome = director.popBiome()) != null) {
            biomes.add(biome);
        }

        // Then add biomes from the biome list
        biomes.addAll(director.getBiomes());

        return biomes;
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create single biome controller
            BiomeControllerSingle controller = new BiomeControllerSingle(biomes, seed);
            director.registerInterface(controller);
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
            // Native controller uses the overworld's biome generation
            BiomeControllerNative controller = new BiomeControllerNative(seed);
            director.registerInterface(controller);
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create noise-based controller with tiny scale
            BiomeControllerNoise controller = new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.TINY);
            director.registerInterface(controller);
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create noise-based controller with small scale
            BiomeControllerNoise controller = new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.SMALL);
            director.registerInterface(controller);
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create noise-based controller with medium scale
            BiomeControllerNoise controller = new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.MEDIUM);
            director.registerInterface(controller);
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create noise-based controller with large scale
            BiomeControllerNoise controller = new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.LARGE);
            director.registerInterface(controller);
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create noise-based controller with huge scale
            BiomeControllerNoise controller = new BiomeControllerNoise(biomes, seed, BiomeControllerNoise.Scale.HUGE);
            director.registerInterface(controller);
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create tiled biome controller
            BiomeControllerTiled controller = new BiomeControllerTiled(biomes, seed);
            director.registerInterface(controller);
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
            List<Holder<Biome>> biomes = collectBiomes(director);

            // Create grid biome controller
            BiomeControllerGrid controller = new BiomeControllerGrid(biomes, seed);
            director.registerInterface(controller);

            director.addInstability(getInstabilityCost());
        }
    }
}
