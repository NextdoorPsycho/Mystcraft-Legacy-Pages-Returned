package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

/**
 * Block registrations for Mystcraft (Fabric 1.20.1).
 */
public final class FabricModBlocks {

    // Workstation blocks
    public static final Supplier<Block> INK_MIXER = registerBlock("blockinkmixer",
            new InkMixerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Block> BOOK_BINDER = registerBlock("blockbookbinder",
            new BookBinderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Block> BOOK_RECEPTACLE = registerBlock("blockbookreceptacle",
            new BookReceptacleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Block> BOOKSTAND = registerBlock("blockbookstand",
            new BookstandBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Block> LINK_MODIFIER = registerBlock("blocklinkmodifier",
            new LinkModifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()));

    public static final Supplier<Block> WRITING_DESK = registerBlock("writingdesk",
            new WritingDeskBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .requiresCorrectToolForDrops()));

    // Special blocks
    public static final Supplier<Block> CRYSTAL = registerBlock("blockcrystal",
            new CrystalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.5F)
                    .lightLevel(CrystalBlock::getLightLevel)
                    .noOcclusion()));

    public static final Supplier<Block> DECAY = registerBlock("blockdecay",
            new DecayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .randomTicks()));

    public static final Supplier<Block> LINK_PORTAL = registerBlock("linkportal",
            new LinkPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .strength(-1.0F)
                    .noLootTable()
                    .lightLevel(state -> 11)
                    .pushReaction(PushReaction.BLOCK)));

    public static final Supplier<Block> STAR_FISSURE = registerBlock("blockstarfissure",
            new StarFissureBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .lightLevel(state -> 15)
                    .pushReaction(PushReaction.BLOCK)));

    // Fluid block - uses lazy init to avoid circular dependency with FabricModFluids
    public static final Supplier<Block> FLUID_INK;

    static {
        LiquidBlock fluidInkBlock = new LiquidBlock(FabricModFluids.BLACK_INK_SOURCE_FLUID, BlockBehaviour.Properties.copy(Blocks.WATER)
                .mapColor(MapColor.COLOR_BLACK)
                .noLootTable()) {};
        FLUID_INK = () -> fluidInkBlock;
    }

    private FabricModBlocks() {
    }

    private static <T extends Block> Supplier<T> registerBlock(String name, T block) {
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(Mystcraft.MOD_ID, name), block);
        return () -> block;
    }

    /**
     * Registers the fluid block. Called after FabricModFluids.register() to resolve fluid reference.
     */
    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(Mystcraft.MOD_ID, "fluidblockblackink"), FLUID_INK.get());
    }
}
