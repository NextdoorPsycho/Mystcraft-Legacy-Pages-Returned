package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.feature.MapGenCavesMyst;
import art.arcane.mystcraft.world.gen.feature.MapGenFloatingIslands;
import art.arcane.mystcraft.world.gen.feature.MapGenRavineMyst;
import art.arcane.mystcraft.world.gen.populate.DenseOresPopulator;
import art.arcane.mystcraft.world.gen.populate.HugeTreePopulator;

/**
 * Terrain feature symbols (caves, ravines, etc).
 * These symbols now register actual terrain alteration implementations
 * in addition to setting configuration flags.
 */
public final class FeatureSymbols {

    private FeatureSymbols() {}

    public static void register() {
        SymbolRegistry.register(new Caves());
        SymbolRegistry.register(new Ravines());
        SymbolRegistry.register(new FloatingIslands());
        SymbolRegistry.register(new Skylands());
        SymbolRegistry.register(new DenseOres());
        SymbolRegistry.register(new HugeTrees());
        SymbolRegistry.register(new DeepLakes());
        SymbolRegistry.register(new SurfaceLakes());
        SymbolRegistry.register(new Spikes());
        SymbolRegistry.register(new Spheres());
        SymbolRegistry.register(new Tendrils());

        // Cave biome features (1.17+)
        SymbolRegistry.register(new DripstoneCaves());
        SymbolRegistry.register(new LushCaves());
        SymbolRegistry.register(new DeepDark());

        // Star Fissure feature
        SymbolRegistry.register(new StarFissureFeature());
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

            // Register actual cave generation implementation
            MapGenCavesMyst caves = new MapGenCavesMyst(seed);
            director.registerInterface(caves);
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

            // Register actual ravine generation implementation
            MapGenRavineMyst ravines = new MapGenRavineMyst(seed);
            director.registerInterface(ravines);
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

            // Register actual floating island generation implementation
            MapGenFloatingIslands islands = new MapGenFloatingIslands(seed);
            director.registerInterface(islands);

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

            // Skylands use a denser floating island generation
            MapGenFloatingIslands skylands = new MapGenFloatingIslands(seed, 3, // Higher density
                    director.getTerrainBlock(), director.getTerrainBlock());
            director.registerInterface(skylands);

            director.addInstability(getInstabilityCost());
        }
    }

    public static class DenseOres extends SymbolBase {
        public DenseOres() {
            super(SymbolRegistry.mystcraftId("dense_ores"), SymbolCategory.FEATURE);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Resource", "Form", "Abundance", "Wealth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDenseOresEnabled(true);

            // Register the dense ores populator (matching legacy Mystcraft behavior)
            director.registerInterface(new DenseOresPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    public static class HugeTrees extends SymbolBase {
        public HugeTrees() {
            super(SymbolRegistry.mystcraftId("huge_trees"), SymbolCategory.FEATURE);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Nature", "Form", "Growth", "Giant");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setHugeTreesEnabled(true);

            // Register the huge tree populator for mega/giant tree generation
            director.registerInterface(new HugeTreePopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    public static class DeepLakes extends SymbolBase {
        public DeepLakes() {
            super(SymbolRegistry.mystcraftId("deep_lakes"), SymbolCategory.FEATURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Water", "Form", "Depth", "Abyss");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDeepLakesEnabled(true);
        }
    }

    public static class SurfaceLakes extends SymbolBase {
        public SurfaceLakes() {
            super(SymbolRegistry.mystcraftId("surface_lakes"), SymbolCategory.FEATURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Water", "Form", "Surface", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSurfaceLakesEnabled(true);
        }
    }

    public static class Spikes extends SymbolBase {
        public Spikes() {
            super(SymbolRegistry.mystcraftId("spikes"), SymbolCategory.FEATURE);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Transform", "Stone", "Pierce");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSpikesEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Spheres extends SymbolBase {
        public Spheres() {
            super(SymbolRegistry.mystcraftId("spheres"), SymbolCategory.FEATURE);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Terrain", "Transform", "Form", "Circle");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSpheresEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class Tendrils extends SymbolBase {
        public Tendrils() {
            super(SymbolRegistry.mystcraftId("tendrils"), SymbolCategory.FEATURE);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Transform", "Nature", "Weave");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTendrilsEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    // ========================= Cave Biome Features =========================

    public static class DripstoneCaves extends SymbolBase {
        public DripstoneCaves() {
            super(SymbolRegistry.mystcraftId("dripstone_caves"), SymbolCategory.FEATURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Stone", "Drip");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDripstoneCavesEnabled(true);
        }
    }

    public static class LushCaves extends SymbolBase {
        public LushCaves() {
            super(SymbolRegistry.mystcraftId("lush_caves"), SymbolCategory.FEATURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Nature", "Growth", "Glow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setLushCavesEnabled(true);
        }
    }

    public static class DeepDark extends SymbolBase {
        public DeepDark() {
            super(SymbolRegistry.mystcraftId("deep_dark"), SymbolCategory.FEATURE);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Void", "Sculk", "Terror");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDeepDarkEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    // ========================= Star Fissure Generation =========================

    public static class StarFissureFeature extends SymbolBase {
        public StarFissureFeature() {
            super(SymbolRegistry.mystcraftId("star_fissure_feature"), SymbolCategory.FEATURE);
            setCardRank(4);
            setInstabilityCost(-10.0f);
            setPoem("Link", "Form", "Star", "Escape");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarFissureEnabled(true);
            director.addInstability(getInstabilityCost());
        }
    }
}
