package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.Page;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
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
 * The Folder item - an ORDERED, WRITABLE page container.
 * <p>
 * 1.18.2 version - uses TranslatableComponent instead of Component.translatable().
 */
public class FolderItem extends Item {

  public static final int MAX_PAGES = 16;
  private static final String TAG_PAGES = "Pages";
  private static final int STACK_SIZE_EMPTY = 32;
  private static final int STACK_SIZE_FILLED = 1;

  public FolderItem(Properties properties) {
    super(properties.stacksTo(STACK_SIZE_EMPTY));
  }

  /**
   * Gets all pages in this folder.
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
   * Adds a page to this folder.
   */
  public static boolean addPage(ItemStack folder, ItemStack page) {
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
   */
  public static ItemStack removePage(ItemStack folder, int index) {
    List<ItemStack> pages = getPages(folder);
    if (index < 0 || index >= pages.size()) {
      return ItemStack.EMPTY;
    }
    ItemStack removed = pages.remove(index);
    setPages(folder, pages);
    return removed;
  }

  /**
   * Sets all pages in this folder.
   */
  public static void setPages(ItemStack folder, List<ItemStack> pages) {
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
   * Checks if this folder is empty.
   */
  public static boolean isEmpty(ItemStack folder) {
    return getPages(folder).isEmpty();
  }

  /**
   * Checks if this folder is full.
   */
  public static boolean isFull(ItemStack folder) {
    return getPages(folder).size() >= MAX_PAGES;
  }

  /**
   * Gets the number of pages in this folder.
   */
  public static int getPageCount(ItemStack folder) {
    return getPages(folder).size();
  }

  /**
   * Clears all pages from this folder.
   */
  public static void clearPages(ItemStack folder) {
    setPages(folder, new ArrayList<>());
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    List<ItemStack> pages = getPages(stack);
    if (!pages.isEmpty()) {
      tooltip.add(new TranslatableComponent("item.mystcraft.folder.pages", pages.size(), MAX_PAGES));
    }
  }

  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (level.isClientSide) {
      // TODO: Open folder GUI when FolderMenu is ported to 1.18.2
      return InteractionResultHolder.success(stack);
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }

  // 1.18.2: getMaxStackSize(ItemStack) doesn't exist, using stacksTo in constructor
}
