package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.BoosterPackItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.GuidebookItem;
import art.arcane.mystcraft.item.InkVialItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.LinkbookUnlinkedItem;
import art.arcane.mystcraft.item.NeoForgePageItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.item.PortfolioItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

/**
 * Item registrations for Mystcraft.
 * Includes standalone items and auto-generated BlockItems.
 */
public final class NeoForgeModItems {

    // Mystcraft-specific items
    public static final DeferredHolder<Item, Item> PAGE =
            MystcraftRegistries.ITEMS.register("page",
                    () -> new NeoForgePageItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> AGEBOOK =
            MystcraftRegistries.ITEMS.register("agebook",
                    () -> new AgebookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> LINKBOOK =
            MystcraftRegistries.ITEMS.register("linkbook",
                    () -> new LinkbookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> LINKBOOK_UNLINKED =
            MystcraftRegistries.ITEMS.register("linkbook_unlinked",
                    () -> new LinkbookUnlinkedItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> PERSONAL_LINK_BOOK =
            MystcraftRegistries.ITEMS.register("personal_link_book",
                    () -> new PersonalLinkBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> BOOSTER_PACK =
            MystcraftRegistries.ITEMS.register("booster",
                    () -> new BoosterPackItem(new Item.Properties().stacksTo(16)));

    // Folder: stacksTo is controlled dynamically by FolderItem.getMaxStackSize()
    // Empty folders stack to 32, folders with pages stack to 1
    public static final DeferredHolder<Item, Item> FOLDER =
            MystcraftRegistries.ITEMS.register("folder",
                    () -> new FolderItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> PORTFOLIO =
            MystcraftRegistries.ITEMS.register("portfolio",
                    () -> new PortfolioItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> INK_VIAL =
            MystcraftRegistries.ITEMS.register("inkvial",
                    () -> new InkVialItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> GUIDEBOOK =
            MystcraftRegistries.ITEMS.register("guidebook",
                    () -> new GuidebookItem(new Item.Properties().stacksTo(1)));

    // Ink bucket
    public static final DeferredHolder<Item, Item> INK_BUCKET =
            MystcraftRegistries.ITEMS.register("ink_bucket",
                    () -> new BucketItem(NeoForgeModFluids.BLACK_INK_SOURCE,
                            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // Block items - auto-generated for each block
    public static final DeferredHolder<Item, Item> INK_MIXER_ITEM = blockItem("blockinkmixer", NeoForgeModBlocks.INK_MIXER);
    public static final DeferredHolder<Item, Item> BOOK_BINDER_ITEM = blockItem("blockbookbinder", NeoForgeModBlocks.BOOK_BINDER);
    public static final DeferredHolder<Item, Item> BOOK_RECEPTACLE_ITEM = blockItem("blockbookreceptacle", NeoForgeModBlocks.BOOK_RECEPTACLE);
    public static final DeferredHolder<Item, Item> BOOKSTAND_ITEM = blockItem("blockbookstand", NeoForgeModBlocks.BOOKSTAND);
    public static final DeferredHolder<Item, Item> LINK_MODIFIER_ITEM = blockItem("blocklinkmodifier", NeoForgeModBlocks.LINK_MODIFIER);
    public static final DeferredHolder<Item, Item> WRITING_DESK_ITEM = blockItem("writingdesk", NeoForgeModBlocks.WRITING_DESK);
    public static final DeferredHolder<Item, Item> CRYSTAL_ITEM = blockItem("blockcrystal", NeoForgeModBlocks.CRYSTAL);
    public static final DeferredHolder<Item, Item> DECAY_ITEM = blockItem("blockdecay", NeoForgeModBlocks.DECAY);

    private NeoForgeModItems() {
    }

    /**
     * Helper to create a BlockItem for a block.
     */
    private static DeferredHolder<Item, Item> blockItem(String name, Supplier<? extends Block> block) {
        return MystcraftRegistries.ITEMS.register(name,
                () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties()));
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
