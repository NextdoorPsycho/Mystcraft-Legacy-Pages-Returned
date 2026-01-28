package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.BoosterPackItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.GlassesItem;
import art.arcane.mystcraft.item.GuidebookItem;
import art.arcane.mystcraft.item.InkVialItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.LinkbookUnlinkedItem;
import art.arcane.mystcraft.item.NeoForgePageItem;
import art.arcane.mystcraft.item.PortfolioItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Item registrations for Mystcraft.
 * Includes standalone items and auto-generated BlockItems.
 */
public final class NeoForgeModItems {

    // Mystcraft-specific items
    public static final RegistryObject<Item> PAGE =
            MystcraftRegistries.ITEMS.register("page",
                    () -> new NeoForgePageItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> AGEBOOK =
            MystcraftRegistries.ITEMS.register("agebook",
                    () -> new AgebookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LINKBOOK =
            MystcraftRegistries.ITEMS.register("linkbook",
                    () -> new LinkbookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LINKBOOK_UNLINKED =
            MystcraftRegistries.ITEMS.register("linkbook_unlinked",
                    () -> new LinkbookUnlinkedItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> BOOSTER_PACK =
            MystcraftRegistries.ITEMS.register("booster",
                    () -> new BoosterPackItem(new Item.Properties().stacksTo(16)));

    // Folder: stacksTo is controlled dynamically by FolderItem.getMaxStackSize()
    // Empty folders stack to 32, folders with pages stack to 1
    public static final RegistryObject<Item> FOLDER =
            MystcraftRegistries.ITEMS.register("folder",
                    () -> new FolderItem(new Item.Properties()));

    public static final RegistryObject<Item> PORTFOLIO =
            MystcraftRegistries.ITEMS.register("portfolio",
                    () -> new PortfolioItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INK_VIAL =
            MystcraftRegistries.ITEMS.register("inkvial",
                    () -> new InkVialItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> GLASSES =
            MystcraftRegistries.ITEMS.register("glasses",
                    () -> new GlassesItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GUIDEBOOK =
            MystcraftRegistries.ITEMS.register("guidebook",
                    () -> new GuidebookItem(new Item.Properties().stacksTo(1)));

    // Ink bucket
    public static final RegistryObject<Item> INK_BUCKET =
            MystcraftRegistries.ITEMS.register("ink_bucket",
                    () -> new BucketItem(NeoForgeModFluids.BLACK_INK_SOURCE,
                            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // Block items - auto-generated for each block
    public static final RegistryObject<Item> INK_MIXER_ITEM = blockItem("blockinkmixer", NeoForgeModBlocks.INK_MIXER);
    public static final RegistryObject<Item> BOOK_BINDER_ITEM = blockItem("blockbookbinder", NeoForgeModBlocks.BOOK_BINDER);
    public static final RegistryObject<Item> BOOK_RECEPTACLE_ITEM = blockItem("blockbookreceptacle", NeoForgeModBlocks.BOOK_RECEPTACLE);
    public static final RegistryObject<Item> BOOKSTAND_ITEM = blockItem("blockbookstand", NeoForgeModBlocks.BOOKSTAND);
    public static final RegistryObject<Item> LECTERN_ITEM = blockItem("blocklectern", NeoForgeModBlocks.LECTERN);
    public static final RegistryObject<Item> LINK_MODIFIER_ITEM = blockItem("blocklinkmodifier", NeoForgeModBlocks.LINK_MODIFIER);
    public static final RegistryObject<Item> WRITING_DESK_ITEM = blockItem("writingdesk", NeoForgeModBlocks.WRITING_DESK);
    public static final RegistryObject<Item> CRYSTAL_ITEM = blockItem("blockcrystal", NeoForgeModBlocks.CRYSTAL);
    public static final RegistryObject<Item> DECAY_ITEM = blockItem("blockdecay", NeoForgeModBlocks.DECAY);

    private NeoForgeModItems() {
    }

    /**
     * Helper to create a BlockItem for a block.
     */
    private static RegistryObject<Item> blockItem(String name, Supplier<? extends Block> block) {
        return MystcraftRegistries.ITEMS.register(name,
                () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties()));
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
