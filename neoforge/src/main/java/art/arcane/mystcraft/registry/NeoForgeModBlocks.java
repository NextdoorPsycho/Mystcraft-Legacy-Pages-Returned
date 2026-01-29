package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.block.BookBinderBlock;
import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.block.BookstandBlock;
import art.arcane.mystcraft.block.CrystalBlock;
import art.arcane.mystcraft.block.DecayBlock;
import art.arcane.mystcraft.block.InkMixerBlock;
import art.arcane.mystcraft.block.LinkModifierBlock;
import art.arcane.mystcraft.block.LinkPortalBlock;
import art.arcane.mystcraft.block.StarFissureBlock;
import art.arcane.mystcraft.block.WritingDeskBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Block registrations for Mystcraft.
 */
public final class NeoForgeModBlocks {

    // Workstation blocks
    public static final DeferredHolder<Block, Block> INK_MIXER =
            MystcraftRegistries.BLOCKS.register("blockinkmixer",
                    () -> new InkMixerBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> BOOK_BINDER =
            MystcraftRegistries.BLOCKS.register("blockbookbinder",
                    () -> new BookBinderBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> BOOK_RECEPTACLE =
            MystcraftRegistries.BLOCKS.register("blockbookreceptacle",
                    () -> new BookReceptacleBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5F)
                            .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> BOOKSTAND =
            MystcraftRegistries.BLOCKS.register("blockbookstand",
                    () -> new BookstandBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> LINK_MODIFIER =
            MystcraftRegistries.BLOCKS.register("blocklinkmodifier",
                    () -> new LinkModifierBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> WRITING_DESK =
            MystcraftRegistries.BLOCKS.register("writingdesk",
                    () -> new WritingDeskBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .requiresCorrectToolForDrops()));

    // Special blocks
    public static final DeferredHolder<Block, Block> CRYSTAL =
            MystcraftRegistries.BLOCKS.register("blockcrystal",
                    () -> new CrystalBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(1.5F)
                            .lightLevel(CrystalBlock::getLightLevel)
                            .noOcclusion()));

    public static final DeferredHolder<Block, Block> DECAY =
            MystcraftRegistries.BLOCKS.register("blockdecay",
                    () -> new DecayBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .randomTicks()));

    public static final DeferredHolder<Block, Block> LINK_PORTAL =
            MystcraftRegistries.BLOCKS.register("linkportal",
                    () -> new LinkPortalBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .noCollission()
                            .strength(-1.0F)
                            .noLootTable()
                            .lightLevel(state -> 11)
                            .pushReaction(PushReaction.BLOCK)));

    public static final DeferredHolder<Block, Block> STAR_FISSURE =
            MystcraftRegistries.BLOCKS.register("blockstarfissure",
                    () -> new StarFissureBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .noCollission()
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .lightLevel(state -> 15)
                            .pushReaction(PushReaction.BLOCK)));

    // Fluid block
    public static final DeferredHolder<Block, LiquidBlock> FLUID_INK =
            MystcraftRegistries.BLOCKS.register("fluidblockblackink",
                    () -> new LiquidBlock(NeoForgeModFluids.BLACK_INK_SOURCE, BlockBehaviour.Properties.copy(Blocks.WATER)
                            .mapColor(MapColor.COLOR_BLACK)
                            .noLootTable()));

    private NeoForgeModBlocks() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
