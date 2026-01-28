package art.arcane.mystcraft.item;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.registry.ModMenuTypes;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Portfolio item - an UNORDERED, COLLECTION-BASED page archive.
 *
 * Key differences from Folder:
 * - UNORDERED: Pages are a collection, not fixed slots
 * - NOT WRITABLE: Cannot write symbols directly to pages
 * - SORTING: Has automatic sorting by category/name
 * - COLLECTION: Removes pages by content matching, not index
 * - CAPACITY: 64 pages (mass storage)
 * - ALWAYS STACK 1: Cannot stack even when empty
 *
 * The Portfolio is designed as a permanent storage archive for
 * collected pages, with automatic organization features.
 * It's crafted FROM a Folder (upgrade path).
 */
public class PortfolioItem extends Item {

    private static final String TAG_PAGES = "Pages";
    public static final int MAX_PAGES = 64;

    public PortfolioItem(Properties properties) {
        super(properties.stacksTo(1)); // Portfolio never stacks (unlike Folder)
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        List<ItemStack> pages = getPages(stack);
        if (!pages.isEmpty()) {
            tooltip.add(Component.translatable("item.mystcraft.portfolio.pages", pages.size(), MAX_PAGES));
        } else {
            tooltip.add(Component.translatable("item.mystcraft.portfolio.empty"));
        }
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            MenuProvider provider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.mystcraft.portfolio");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                    return new PortfolioMenu(containerId, playerInventory, slot);
                }
            };
            Services.PLATFORM.openMenu(serverPlayer, provider, buf -> buf.writeVarInt(slot));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Gets all pages in this portfolio.
     */
    public static List<ItemStack> getPages(ItemStack stack) {
        if (stack.getTag() == null) {
            return new ArrayList<>();
        }
        CompoundTag tag = stack.getTag();
        ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
        List<ItemStack> pages = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            ItemStack page = ItemStack.of(listTag.getCompound(i));
            if (!page.isEmpty()) {
                pages.add(page);
            }
        }
        return pages;
    }

    /**
     * Adds a page to this portfolio.
     *
     * @return true if the page was added successfully
     */
    public static boolean addPage(ItemStack portfolio, ItemStack page) {
        if (portfolio.getTag() == null) {
            portfolio.setTag(new CompoundTag());
        }
        List<ItemStack> pages = getPages(portfolio);
        if (pages.size() >= MAX_PAGES) {
            return false;
        }
        pages.add(page.copy());
        setPages(portfolio, pages);
        return true;
    }

    /**
     * Removes a page from this portfolio.
     *
     * @return the removed page, or ItemStack.EMPTY if not found
     */
    public static ItemStack removePage(ItemStack portfolio, int index) {
        List<ItemStack> pages = getPages(portfolio);
        if (index < 0 || index >= pages.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = pages.remove(index);
        setPages(portfolio, pages);
        return removed;
    }

    /**
     * Sets the pages in this portfolio.
     */
    public static void setPages(ItemStack portfolio, List<ItemStack> pages) {
        CompoundTag tag = portfolio.getOrCreateTag();
        ListTag listTag = new ListTag();
        for (ItemStack page : pages) {
            if (!page.isEmpty()) {
                listTag.add(page.save(new CompoundTag()));
            }
        }
        tag.put(TAG_PAGES, listTag);
    }

    /**
     * Clears all pages from this portfolio.
     */
    public static void clearPages(ItemStack portfolio) {
        setPages(portfolio, new ArrayList<>());
    }

    /**
     * Sorts pages by symbol category first, then by symbol name.
     * Link panels are sorted to the front.
     */
    public static void sortPages(ItemStack portfolio) {
        List<ItemStack> pages = getPages(portfolio);

        pages.sort(Comparator.comparing((ItemStack page) -> {
            // Link panels come first
            if (Page.isLinkPanel(page)) {
                return "000_linkpanel";
            }
            // Blank pages come second
            if (Page.isBlank(page)) {
                return "001_blank";
            }
            // Sort by category then name
            ResourceLocation symbolId = Page.getSymbol(page);
            if (symbolId != null) {
                IAgeSymbol symbol = SymbolRegistry.get(symbolId);
                if (symbol != null) {
                    String category = symbol.getCategory().getName().toLowerCase();
                    String name = symbol.getLocalizedName().toLowerCase();
                    return category + "_" + name;
                }
                return "zzz_" + symbolId.toString();
            }
            return "zzz_unknown";
        }));

        setPages(portfolio, pages);
    }

    /**
     * Gets the number of pages in this portfolio.
     */
    public static int getPageCount(ItemStack stack) {
        return getPages(stack).size();
    }

    /**
     * Checks if this portfolio is full.
     */
    public static boolean isFull(ItemStack stack) {
        return getPageCount(stack) >= MAX_PAGES;
    }

    /**
     * Checks if this portfolio is empty.
     */
    public static boolean isEmpty(ItemStack stack) {
        return getPages(stack).isEmpty();
    }

    // --- Portfolio: Collection Semantics ---

    /**
     * Removes a page by CONTENT matching (not by index).
     * This is the collection-based removal that distinguishes Portfolio from Folder.
     * Portfolio is a COLLECTION - you remove items by what they ARE, not where they are.
     *
     * @param portfolio The portfolio item stack
     * @param pageToRemove The page to find and remove (matched by NBT content)
     * @return The removed page, or ItemStack.EMPTY if not found
     */
    public static ItemStack removeByContent(ItemStack portfolio, ItemStack pageToRemove) {
        List<ItemStack> pages = getPages(portfolio);

        for (int i = 0; i < pages.size(); i++) {
            ItemStack page = pages.get(i);
            if (ItemStack.isSameItemSameTags(page, pageToRemove)) {
                ItemStack removed = pages.remove(i);
                setPages(portfolio, pages);
                return removed;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Checks if this portfolio contains a page matching the given content.
     */
    public static boolean containsPage(ItemStack portfolio, ItemStack pageToFind) {
        for (ItemStack page : getPages(portfolio)) {
            if (ItemStack.isSameItemSameTags(page, pageToFind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts how many copies of a specific page are in this portfolio.
     */
    public static int countMatchingPages(ItemStack portfolio, ItemStack pageToCount) {
        int count = 0;
        for (ItemStack page : getPages(portfolio)) {
            if (ItemStack.isSameItemSameTags(page, pageToCount)) {
                count++;
            }
        }
        return count;
    }

    // --- Portfolio: Category Statistics ---

    /**
     * Counts pages by type in this portfolio.
     */
    public static int countLinkPanels(ItemStack portfolio) {
        int count = 0;
        for (ItemStack page : getPages(portfolio)) {
            if (Page.isLinkPanel(page)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts blank pages in this portfolio.
     */
    public static int countBlankPages(ItemStack portfolio) {
        int count = 0;
        for (ItemStack page : getPages(portfolio)) {
            if (Page.isBlank(page)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts symbol pages in this portfolio.
     */
    public static int countSymbolPages(ItemStack portfolio) {
        int count = 0;
        for (ItemStack page : getPages(portfolio)) {
            if (Page.getSymbol(page) != null) {
                count++;
            }
        }
        return count;
    }

    // --- Portfolio: Bulk Operations ---

    /**
     * Imports all pages from another portfolio or folder into this one.
     * Returns any pages that couldn't fit.
     */
    public static List<ItemStack> importFrom(ItemStack portfolio, List<ItemStack> pagesToImport) {
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack page : pagesToImport) {
            if (!addPage(portfolio, page)) {
                overflow.add(page);
            }
        }
        return overflow;
    }

    /**
     * Gets unique symbol types in this portfolio (for display purposes).
     */
    public static List<ResourceLocation> getUniqueSymbols(ItemStack portfolio) {
        List<ResourceLocation> symbols = new ArrayList<>();
        for (ItemStack page : getPages(portfolio)) {
            ResourceLocation symbol = Page.getSymbol(page);
            if (symbol != null && !symbols.contains(symbol)) {
                symbols.add(symbol);
            }
        }
        return symbols;
    }
}
