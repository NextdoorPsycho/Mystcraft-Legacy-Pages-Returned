package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Terrain generation symbols.
 */
public final class TerrainSymbols {

    private TerrainSymbols() {}

    public static void register() {
        SymbolRegistry.register(new TerrainNormal());
        SymbolRegistry.register(new TerrainAmplified());
        SymbolRegistry.register(new TerrainFlat());
        SymbolRegistry.register(new TerrainVoid());
        SymbolRegistry.register(new TerrainEnd());
        SymbolRegistry.register(new TerrainNether());
    }

    public static class TerrainNormal extends SymbolBase {
        public TerrainNormal() {
            super(SymbolRegistry.mystcraftId("terrain_normal"), SymbolCategory.TERRAIN);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Tradition", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainType("normal");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(63);
        }
    }

    public static class TerrainAmplified extends SymbolBase {
        public TerrainAmplified() {
            super(SymbolRegistry.mystcraftId("terrain_amplified"), SymbolCategory.TERRAIN);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Form", "Tradition", "Spur");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainType("amplified");
            director.setAverageGroundLevel(96);
            director.setSeaLevel(63);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainFlat extends SymbolBase {
        public TerrainFlat() {
            super(SymbolRegistry.mystcraftId("terrain_flat"), SymbolCategory.TERRAIN);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Form", "Inhibit", "Motion");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainType("flat");
            director.setAverageGroundLevel(4);
            director.setSeaLevel(-64); // Below world, effectively no sea
            director.setHasSea(false);
        }
    }

    public static class TerrainVoid extends SymbolBase {
        public TerrainVoid() {
            super(SymbolRegistry.mystcraftId("terrain_void"), SymbolCategory.TERRAIN);
            setCardRank(4);
            setInstabilityCost(10.0f);
            setPoem("Terrain", "Form", "Infinite", "Void");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainType("void");
            director.setAverageGroundLevel(0);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainEnd extends SymbolBase {
        public TerrainEnd() {
            super(SymbolRegistry.mystcraftId("terrain_end"), SymbolCategory.TERRAIN);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Form", "Ethereal", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainType("end");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x000000);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainNether extends SymbolBase {
        public TerrainNether() {
            super(SymbolRegistry.mystcraftId("terrain_nether"), SymbolCategory.TERRAIN);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Form", "Constraint", "Entropy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainType("nether");
            director.setAverageGroundLevel(64);
            director.setSeaLevel(-64);
            director.setHasSea(false);
            director.setSkyColor(0x330808);
            director.setFogColor(0x330808);
            director.addInstability(getInstabilityCost());
        }
    }
}
