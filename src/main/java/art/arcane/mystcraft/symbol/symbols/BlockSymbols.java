package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block modifier symbols that allow customizing terrain and sea blocks.
 * These can be used to create Ages with unusual terrain materials.
 */
public final class BlockSymbols {

    private BlockSymbols() {}

    public static void register() {
        // Terrain blocks (primary world material)
        SymbolRegistry.register(new TerrainBlockStone());
        SymbolRegistry.register(new TerrainBlockDeepslate());
        SymbolRegistry.register(new TerrainBlockGranite());
        SymbolRegistry.register(new TerrainBlockDiorite());
        SymbolRegistry.register(new TerrainBlockAndesite());
        SymbolRegistry.register(new TerrainBlockSandstone());
        SymbolRegistry.register(new TerrainBlockRedSandstone());
        SymbolRegistry.register(new TerrainBlockNetherrack());
        SymbolRegistry.register(new TerrainBlockEndStone());
        SymbolRegistry.register(new TerrainBlockBasalt());
        SymbolRegistry.register(new TerrainBlockBlackstone());
        SymbolRegistry.register(new TerrainBlockCalcite());
        SymbolRegistry.register(new TerrainBlockTuff());
        SymbolRegistry.register(new TerrainBlockDirt());
        SymbolRegistry.register(new TerrainBlockSand());
        SymbolRegistry.register(new TerrainBlockGravel());
        SymbolRegistry.register(new TerrainBlockClay());
        SymbolRegistry.register(new TerrainBlockObsidian());
        SymbolRegistry.register(new TerrainBlockPrismarine());
        SymbolRegistry.register(new TerrainBlockPackedIce());

        // Sea blocks (fluid/ocean material)
        SymbolRegistry.register(new SeaBlockWater());
        SymbolRegistry.register(new SeaBlockLava());
        SymbolRegistry.register(new SeaBlockPackedIce());
        SymbolRegistry.register(new SeaBlockBlueIce());
        SymbolRegistry.register(new SeaBlockHoney());
        SymbolRegistry.register(new SeaBlockSlime());
        SymbolRegistry.register(new SeaBlockPowderSnow());
        SymbolRegistry.register(new SeaBlockMud());

        Mystcraft.LOGGER.info("Registered block symbols");
    }

    // --- Terrain Blocks ---

    public static class TerrainBlockStone extends SymbolBase {
        public TerrainBlockStone() {
            super(SymbolRegistry.mystcraftId("block_stone"), SymbolCategory.MODIFIER);
            setCardRank(1);
            setInstabilityCost(0.0f);
            setPoem("Modifier", "Constraint", "Earth", "Stone");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.STONE.defaultBlockState());
        }
    }

    public static class TerrainBlockDeepslate extends SymbolBase {
        public TerrainBlockDeepslate() {
            super(SymbolRegistry.mystcraftId("block_deepslate"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(2.0f);
            setPoem("Modifier", "Constraint", "Earth", "Depth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.DEEPSLATE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockGranite extends SymbolBase {
        public TerrainBlockGranite() {
            super(SymbolRegistry.mystcraftId("block_granite"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(1.0f);
            setPoem("Modifier", "Constraint", "Earth", "Fire");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.GRANITE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockDiorite extends SymbolBase {
        public TerrainBlockDiorite() {
            super(SymbolRegistry.mystcraftId("block_diorite"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(1.0f);
            setPoem("Modifier", "Constraint", "Earth", "Light");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.DIORITE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockAndesite extends SymbolBase {
        public TerrainBlockAndesite() {
            super(SymbolRegistry.mystcraftId("block_andesite"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(1.0f);
            setPoem("Modifier", "Constraint", "Earth", "Gray");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.ANDESITE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockSandstone extends SymbolBase {
        public TerrainBlockSandstone() {
            super(SymbolRegistry.mystcraftId("block_sandstone"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Modifier", "Constraint", "Earth", "Sand");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.SANDSTONE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockRedSandstone extends SymbolBase {
        public TerrainBlockRedSandstone() {
            super(SymbolRegistry.mystcraftId("block_red_sandstone"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Modifier", "Constraint", "Earth", "Rust");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.RED_SANDSTONE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockNetherrack extends SymbolBase {
        public TerrainBlockNetherrack() {
            super(SymbolRegistry.mystcraftId("block_netherrack"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Modifier", "Constraint", "Fire", "Chaos");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.NETHERRACK.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockEndStone extends SymbolBase {
        public TerrainBlockEndStone() {
            super(SymbolRegistry.mystcraftId("block_end_stone"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Modifier", "Constraint", "Void", "End");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.END_STONE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockBasalt extends SymbolBase {
        public TerrainBlockBasalt() {
            super(SymbolRegistry.mystcraftId("block_basalt"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Fire", "Stone");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.BASALT.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockBlackstone extends SymbolBase {
        public TerrainBlockBlackstone() {
            super(SymbolRegistry.mystcraftId("block_blackstone"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Modifier", "Constraint", "Void", "Stone");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.BLACKSTONE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockCalcite extends SymbolBase {
        public TerrainBlockCalcite() {
            super(SymbolRegistry.mystcraftId("block_calcite"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Modifier", "Constraint", "Earth", "White");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.CALCITE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockTuff extends SymbolBase {
        public TerrainBlockTuff() {
            super(SymbolRegistry.mystcraftId("block_tuff"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(2.0f);
            setPoem("Modifier", "Constraint", "Earth", "Rough");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.TUFF.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockDirt extends SymbolBase {
        public TerrainBlockDirt() {
            super(SymbolRegistry.mystcraftId("block_dirt"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Earth", "Soil");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.DIRT.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockSand extends SymbolBase {
        public TerrainBlockSand() {
            super(SymbolRegistry.mystcraftId("block_sand"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(8.0f);
            setPoem("Modifier", "Constraint", "Earth", "Desert");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.SAND.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockGravel extends SymbolBase {
        public TerrainBlockGravel() {
            super(SymbolRegistry.mystcraftId("block_gravel"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(8.0f);
            setPoem("Modifier", "Constraint", "Earth", "Loose");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.GRAVEL.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockClay extends SymbolBase {
        public TerrainBlockClay() {
            super(SymbolRegistry.mystcraftId("block_clay"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Earth", "Soft");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.CLAY.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockObsidian extends SymbolBase {
        public TerrainBlockObsidian() {
            super(SymbolRegistry.mystcraftId("block_obsidian"), SymbolCategory.MODIFIER);
            setCardRank(4);
            setInstabilityCost(20.0f);
            setPoem("Modifier", "Constraint", "Void", "Glass");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.OBSIDIAN.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockPrismarine extends SymbolBase {
        public TerrainBlockPrismarine() {
            super(SymbolRegistry.mystcraftId("block_prismarine"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Modifier", "Constraint", "Water", "Crystal");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.PRISMARINE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class TerrainBlockPackedIce extends SymbolBase {
        public TerrainBlockPackedIce() {
            super(SymbolRegistry.mystcraftId("block_packed_ice"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Modifier", "Constraint", "Ice", "Solid");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(Blocks.PACKED_ICE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Sea Blocks ---

    public static class SeaBlockWater extends SymbolBase {
        public SeaBlockWater() {
            super(SymbolRegistry.mystcraftId("sea_water"), SymbolCategory.MODIFIER);
            setCardRank(1);
            setInstabilityCost(0.0f);
            setPoem("Terrain", "Water", "Flow", "Sea");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.WATER.defaultBlockState());
        }
    }

    public static class SeaBlockLava extends SymbolBase {
        public SeaBlockLava() {
            super(SymbolRegistry.mystcraftId("sea_lava"), SymbolCategory.MODIFIER);
            setCardRank(4);
            setInstabilityCost(25.0f);
            setPoem("Terrain", "Fire", "Flow", "Chaos");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.LAVA.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SeaBlockPackedIce extends SymbolBase {
        public SeaBlockPackedIce() {
            super(SymbolRegistry.mystcraftId("sea_packed_ice"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Terrain", "Ice", "Solid", "Freeze");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.PACKED_ICE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SeaBlockBlueIce extends SymbolBase {
        public SeaBlockBlueIce() {
            super(SymbolRegistry.mystcraftId("sea_blue_ice"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(12.0f);
            setPoem("Terrain", "Ice", "Solid", "Deep");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.BLUE_ICE.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SeaBlockHoney extends SymbolBase {
        public SeaBlockHoney() {
            super(SymbolRegistry.mystcraftId("sea_honey"), SymbolCategory.MODIFIER);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Nature", "Flow", "Sweet");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.HONEY_BLOCK.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SeaBlockSlime extends SymbolBase {
        public SeaBlockSlime() {
            super(SymbolRegistry.mystcraftId("sea_slime"), SymbolCategory.MODIFIER);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Terrain", "Chaos", "Flow", "Bounce");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.SLIME_BLOCK.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SeaBlockPowderSnow extends SymbolBase {
        public SeaBlockPowderSnow() {
            super(SymbolRegistry.mystcraftId("sea_powder_snow"), SymbolCategory.MODIFIER);
            setCardRank(4);
            setInstabilityCost(20.0f);
            setPoem("Terrain", "Ice", "Trap", "Cold");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.POWDER_SNOW.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SeaBlockMud extends SymbolBase {
        public SeaBlockMud() {
            super(SymbolRegistry.mystcraftId("sea_mud"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(10.0f);
            setPoem("Terrain", "Earth", "Swamp", "Sink");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(Blocks.MUD.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }
    }
}
