package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.Page;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Folder item.
 * A small container for holding pages.
 * Can store up to 16 pages.
 */
public class FolderItem extends Item {

    private static final String TAG_PAGES = "Pages";
    public static final int MAX_PAGES = 16;

    public FolderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        List<ItemStack> pages = getPages(stack);
        if (!pages.isEmpty()) {
            tooltip.add(Component.translatable("item.mystcraft.folder.pages", pages.size(), MAX_PAGES));
        } else {
            tooltip.add(Component.translatable("item.mystcraft.folder.empty"));
        }
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // TODO: Open folder GUI
            player.displayClientMessage(Component.translatable("item.mystcraft.folder.open"), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Gets all pages in this folder.
     */
    public List<ItemStack> getPages(ItemStack stack) {
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
     * Adds a page to this folder.
     *
     * @return true if the page was added successfully
     */
    public boolean addPage(ItemStack folder, ItemStack page) {
        if (folder.getTag() == null) {
            folder.setTag(new CompoundTag());
        }
        List<ItemStack> pages = getPages(folder);
        if (pages.size() >= MAX_PAGES) {
            return false;
        }
        pages.add(page.copy());
        setPages(folder, pages);
        return true;
    }

    /**
     * Removes a page from this folder.
     *
     * @return the removed page, or ItemStack.EMPTY if not found
     */
    public ItemStack removePage(ItemStack folder, int index) {
        List<ItemStack> pages = getPages(folder);
        if (index < 0 || index >= pages.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = pages.remove(index);
        setPages(folder, pages);
        return removed;
    }

    /**
     * Sets the pages in this folder.
     */
    public void setPages(ItemStack folder, List<ItemStack> pages) {
        CompoundTag tag = folder.getOrCreateTag();
        ListTag listTag = new ListTag();
        for (ItemStack page : pages) {
            if (!page.isEmpty()) {
                listTag.add(page.save(new CompoundTag()));
            }
        }
        tag.put(TAG_PAGES, listTag);
    }

    /**
     * Gets the number of pages in this folder.
     */
    public int getPageCount(ItemStack stack) {
        return getPages(stack).size();
    }

    /**
     * Checks if this folder is full.
     */
    public boolean isFull(ItemStack stack) {
        return getPageCount(stack) >= MAX_PAGES;
    }

    /**
     * Checks if this folder is empty.
     */
    public boolean isEmpty(ItemStack stack) {
        return getPages(stack).isEmpty();
    }
}
