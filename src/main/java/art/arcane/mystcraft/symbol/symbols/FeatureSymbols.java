package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.feature.MapGenCavesMyst;
import art.arcane.mystcraft.world.gen.feature.MapGenFloatingIslands;
import art.arcane.mystcraft.world.gen.feature.MapGenRavineMyst;
import art.arcane.mystcraft.world.gen.populate.DeepDarkPopulator;
import art.arcane.mystcraft.world.gen.populate.DeepLakesPopulator;
import art.arcane.mystcraft.world.gen.populate.DenseOresPopulator;
import art.arcane.mystcraft.world.gen.populate.DripstoneCavesPopulator;
import art.arcane.mystcraft.world.gen.populate.HugeTreePopulator;
import art.arcane.mystcraft.world.gen.populate.LushCavesPopulator;
import art.arcane.mystcraft.world.gen.populate.SpheresPopulator;
import art.arcane.mystcraft.world.gen.populate.SpikesPopulator;
import art.arcane.mystcraft.world.gen.populate.StarFissurePopulator;
import art.arcane.mystcraft.world.gen.populate.SurfaceLakesPopulator;
import art.arcane.mystcraft.world.gen.populate.TendrilsPopulator;
import art.arcane.mystcraft.world.gen.populate.PerlinWormsPopulator;
import art.arcane.mystcraft.world.gen.populate.VerticalTendrilsPopulator;

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
        SymbolRegistry.register(new VerticalTendrils());
        SymbolRegistry.register(new PerlinWorms());

        // Cave biome features (1.17+)
        SymbolRegistry.register(new DripstoneCaves());
        SymbolRegistry.register(new LushCaves());
        SymbolRegistry.register(new DeepDark());

        // Star Fissure feature
        SymbolRegistry.register(new StarFissureFeature());
    }

    public static class Caves extends SymbolBase {
        public Caves() {
            super(SymbolRegistry.mystcraftId("caves"), SymbolCategory.FEATURE_LARGE);
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
            super(SymbolRegistry.mystcraftId("ravines"), SymbolCategory.FEATURE_LARGE);
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
            super(SymbolRegistry.mystcraftId("floating_islands"), SymbolCategory.FEATURE_LARGE);
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
            super(SymbolRegistry.mystcraftId("skylands"), SymbolCategory.FEATURE_LARGE);
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
            super(SymbolRegistry.mystcraftId("dense_ores"), SymbolCategory.FEATURE_MEDIUM);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Resource", "Form", "Abundance", "Wealth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDenseOresEnabled(true);

            // Register the dense ores populator
            director.registerInterface(new DenseOresPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    public static class HugeTrees extends SymbolBase {
        public HugeTrees() {
            super(SymbolRegistry.mystcraftId("huge_trees"), SymbolCategory.FEATURE_MEDIUM);
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
            super(SymbolRegistry.mystcraftId("deep_lakes"), SymbolCategory.FEATURE_MEDIUM);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Water", "Form", "Depth", "Abyss");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDeepLakesEnabled(true);

            // Register the deep lakes populator for underground water/lava pools
            director.registerInterface(new DeepLakesPopulator(seed));
        }
    }

    public static class SurfaceLakes extends SymbolBase {
        public SurfaceLakes() {
            super(SymbolRegistry.mystcraftId("surface_lakes"), SymbolCategory.FEATURE_SMALL);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Water", "Form", "Surface", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSurfaceLakesEnabled(true);

            // Register the surface lakes populator for surface water/lava lakes
            director.registerInterface(new SurfaceLakesPopulator(seed));
        }
    }

    public static class Spikes extends SymbolBase {
        public Spikes() {
            super(SymbolRegistry.mystcraftId("spikes"), SymbolCategory.FEATURE_MEDIUM);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Transform", "Stone", "Pierce");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSpikesEnabled(true);

            // Register the spikes populator for ice-spike style formations
            director.registerInterface(new SpikesPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    public static class Spheres extends SymbolBase {
        public Spheres() {
            super(SymbolRegistry.mystcraftId("spheres"), SymbolCategory.FEATURE_MEDIUM);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Terrain", "Transform", "Form", "Circle");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSpheresEnabled(true);

            // Register the spheres populator for floating/embedded spherical formations
            director.registerInterface(new SpheresPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    public static class Tendrils extends SymbolBase {
        public Tendrils() {
            super(SymbolRegistry.mystcraftId("tendrils"), SymbolCategory.FEATURE_MEDIUM);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Transform", "Nature", "Weave");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTendrilsEnabled(true);

            // Register the tendrils populator for vine-like terrain formations
            director.registerInterface(new TendrilsPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    public static class VerticalTendrils extends SymbolBase {
        public VerticalTendrils() {
            super(SymbolRegistry.mystcraftId("vertical_tendrils"), SymbolCategory.FEATURE_MEDIUM);
            setCardRank(3);
            setInstabilityCost(4.0f);
            setPoem("Terrain", "Transform", "Stone", "Column");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setVerticalTendrilsEnabled(true);

            director.registerInterface(new VerticalTendrilsPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    public static class PerlinWorms extends SymbolBase {
        public PerlinWorms() {
            super(SymbolRegistry.mystcraftId("perlin_worms"), SymbolCategory.FEATURE_LARGE);
            setCardRank(3);
            setInstabilityCost(6.0f);
            setPoem("Terrain", "Transform", "Void", "Serpent");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setPerlinWormsEnabled(true);

            director.registerInterface(new PerlinWormsPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    // --- Cave Biome Features ---

    public static class DripstoneCaves extends SymbolBase {
        public DripstoneCaves() {
            super(SymbolRegistry.mystcraftId("dripstone_caves"), SymbolCategory.FEATURE_SMALL);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Stone", "Drip");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDripstoneCavesEnabled(true);

            // Register the dripstone caves populator for stalactites and stalagmites
            director.registerInterface(new DripstoneCavesPopulator(seed));
        }
    }

    public static class LushCaves extends SymbolBase {
        public LushCaves() {
            super(SymbolRegistry.mystcraftId("lush_caves"), SymbolCategory.FEATURE_SMALL);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Nature", "Growth", "Glow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setLushCavesEnabled(true);

            // Register the lush caves populator for moss, glow berries, and vegetation
            director.registerInterface(new LushCavesPopulator(seed));
        }
    }

    public static class DeepDark extends SymbolBase {
        public DeepDark() {
            super(SymbolRegistry.mystcraftId("deep_dark"), SymbolCategory.FEATURE_LARGE);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Void", "Sculk", "Terror");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDeepDarkEnabled(true);

            // Register the deep dark populator for sculk features
            director.registerInterface(new DeepDarkPopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }

    // --- Star Fissure Generation ---

    public static class StarFissureFeature extends SymbolBase {
        public StarFissureFeature() {
            super(SymbolRegistry.mystcraftId("star_fissure_feature"), SymbolCategory.FEATURE_LARGE);
            setCardRank(4);
            setInstabilityCost(-10.0f);
            setPoem("Link", "Form", "Star", "Escape");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarFissureEnabled(true);

            director.registerInterface(new StarFissurePopulator(seed));

            director.addInstability(getInstabilityCost());
        }
    }
}
