package art.arcane.mystcraft.item;

import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.registry.ModMenuTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
import java.util.List;

/**
 * The Portfolio item.
 * A larger container for holding pages, with collection sorting features.
 * Can store up to 64 pages.
 */
public class PortfolioItem extends Item {

    private static final String TAG_PAGES = "Pages";
    public static final int MAX_PAGES = 64;

    public PortfolioItem(Properties properties) {
        super(properties);
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
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.mystcraft.portfolio");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                    return new PortfolioMenu(containerId, playerInventory, slot);
                }
            }, buf -> buf.writeVarInt(slot));
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
     * Sorts pages alphabetically by symbol name.
     */
    public static void sortPages(ItemStack portfolio) {
        List<ItemStack> pages = getPages(portfolio);
        // TODO: Implement sorting by symbol type/name when SymbolManager is implemented
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
}
