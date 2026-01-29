package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.BoosterPackItem;
import art.arcane.mystcraft.item.FabricPageItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.GlassesItem;
import art.arcane.mystcraft.item.GuidebookItem;
import art.arcane.mystcraft.item.InkVialItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.LinkbookUnlinkedItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.item.PortfolioItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Item registrations for Mystcraft (Fabric).
 * Includes standalone items and auto-generated BlockItems.
 */
public final class FabricModItems {

    // Mystcraft-specific items
    public static final Supplier<Item> PAGE = registerItem("page",
            new FabricPageItem(new Item.Properties().stacksTo(64)));

    public static final Supplier<Item> AGEBOOK = registerItem("agebook",
            new AgebookItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> LINKBOOK = registerItem("linkbook",
            new LinkbookItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> LINKBOOK_UNLINKED = registerItem("linkbook_unlinked",
            new LinkbookUnlinkedItem(new Item.Properties().stacksTo(16)));

    public static final Supplier<Item> PERSONAL_LINK_BOOK = registerItem("personal_link_book",
            new PersonalLinkBookItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> BOOSTER_PACK = registerItem("booster",
            new BoosterPackItem(new Item.Properties().stacksTo(16)));

    public static final Supplier<Item> FOLDER = registerItem("folder",
            new FolderItem(new Item.Properties()));

    public static final Supplier<Item> PORTFOLIO = registerItem("portfolio",
            new PortfolioItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> INK_VIAL = registerItem("inkvial",
            new InkVialItem(new Item.Properties().stacksTo(16)));

    public static final Supplier<Item> GLASSES = registerItem("glasses",
            new GlassesItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> GUIDEBOOK = registerItem("guidebook",
            new GuidebookItem(new Item.Properties().stacksTo(1)));

    // Ink bucket
    public static final Supplier<Item> INK_BUCKET = registerItem("ink_bucket",
            new BucketItem(FabricModFluids.BLACK_INK_SOURCE_FLUID,
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // Block items
    public static final Supplier<Item> INK_MIXER_ITEM = registerBlockItem("blockinkmixer", FabricModBlocks.INK_MIXER);
    public static final Supplier<Item> BOOK_BINDER_ITEM = registerBlockItem("blockbookbinder", FabricModBlocks.BOOK_BINDER);
    public static final Supplier<Item> BOOK_RECEPTACLE_ITEM = registerBlockItem("blockbookreceptacle", FabricModBlocks.BOOK_RECEPTACLE);
    public static final Supplier<Item> BOOKSTAND_ITEM = registerBlockItem("blockbookstand", FabricModBlocks.BOOKSTAND);
    public static final Supplier<Item> LECTERN_ITEM = registerBlockItem("blocklectern", FabricModBlocks.LECTERN);
    public static final Supplier<Item> LINK_MODIFIER_ITEM = registerBlockItem("blocklinkmodifier", FabricModBlocks.LINK_MODIFIER);
    public static final Supplier<Item> WRITING_DESK_ITEM = registerBlockItem("writingdesk", FabricModBlocks.WRITING_DESK);
    public static final Supplier<Item> CRYSTAL_ITEM = registerBlockItem("blockcrystal", FabricModBlocks.CRYSTAL);
    public static final Supplier<Item> DECAY_ITEM = registerBlockItem("blockdecay", FabricModBlocks.DECAY);

    private FabricModItems() {
    }

    private static <T extends Item> Supplier<T> registerItem(String name, T item) {
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Mystcraft.MOD_ID, name), item);
        return () -> item;
    }

    private static Supplier<Item> registerBlockItem(String name, Supplier<? extends Block> block) {
        BlockItem blockItem = new BlockItem(block.get(), new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Mystcraft.MOD_ID, name), blockItem);
        return () -> blockItem;
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
