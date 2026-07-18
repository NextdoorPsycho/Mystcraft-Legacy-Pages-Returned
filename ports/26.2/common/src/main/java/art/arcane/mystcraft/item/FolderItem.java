package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Folder item - an ORDERED, WRITABLE page container.
 * <p>
 * Key differences from Portfolio: - ORDERED: Pages have fixed slot positions
 * (0, 1, 2, ...) - WRITABLE: Can write symbols directly to blank pages inside -
 * NON-STACKING: Stored page NBT always belongs to one physical folder -
 * BOOKBINDER: Can be used as a book cover (when empty) - CAPACITY: 16 pages (a working set for Age
 * creation)
 * <p>
 * The Folder is designed as a portable workspace for organizing pages at the
 * Writing Desk and binding them into Age books.
 */
public class FolderItem extends Item implements TooltipCompat {

  public static final int MAX_PAGES = 16;
  private static final String TAG_PAGES = "Pages";

  public FolderItem(Properties properties) {
    // Vanilla 1.20 has no cross-loader, per-stack max-size hook. Allowing
    // empty folders to stack means adding NBT to one mutates the entire stack
    // into multiple identical filled folders. Keep every NBT container unique.
    super(properties.stacksTo(1));
  }

  /**
   * Gets all pages in this folder.
   */
  public static List<ItemStack> getPages(ItemStack stack) {
    if (ItemStackNbt.getTag(stack) == null) {
      return new ArrayList<>();
    }
    CompoundTag tag = ItemStackNbt.getTag(stack);
    ListTag listTag = tag.getListOrEmpty(TAG_PAGES);
    List<ItemStack> pages = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      ItemStack page = ItemStackNbt.load(listTag.getCompoundOrEmpty(i));
      if (!page.isEmpty()) {
        int count = page.getCount();
        for (int pageIndex = 0; pageIndex < count; pageIndex++) {
          ItemStack singlePage = page.copy();
          singlePage.setCount(1);
          pages.add(singlePage);
        }
      }
    }
    return pages;
  }

  /**
   * Adds a page to this folder.
   *
   * @return true if the page was added successfully
   */
  public static boolean addPage(ItemStack folder, ItemStack page) {
    if (page.isEmpty() || !(page.getItem() instanceof PageItem)) {
      return false;
    }
    if (ItemStackNbt.getTag(folder) == null) {
      ItemStackNbt.setTag(folder, new CompoundTag());
    }
    List<ItemStack> pages = getPages(folder);
    if (pages.size() >= MAX_PAGES) {
      return false;
    }
    ItemStack singlePage = page.copy();
    singlePage.setCount(1);
    pages.add(singlePage);
    setPages(folder, pages);
    return true;
  }

  /**
   * Removes a page from this folder.
   *
   * @return the removed page, or ItemStack.EMPTY if not found
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
   * Sets the pages in this folder.
   */
  public static void setPages(ItemStack folder, List<ItemStack> pages) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(folder);
    ListTag listTag = new ListTag();
    for (ItemStack page : pages) {
      if (!page.isEmpty()) {
        int count = page.getCount();
        for (int pageIndex = 0; pageIndex < count; pageIndex++) {
          ItemStack singlePage = page.copy();
          singlePage.setCount(1);
          listTag.add(ItemStackNbt.save(singlePage));
        }
      }
    }
    tag.put(TAG_PAGES, listTag);
    ItemStackNbt.setTag(folder, tag);
  }

  /**
   * Clears all pages from this folder.
   */
  public static void clearPages(ItemStack folder) {
    setPages(folder, new ArrayList<>());
  }

  /**
   * Gets the number of pages in this folder.
   */
  public static int getPageCount(ItemStack stack) {
    return getPages(stack).size();
  }

  /**
   * Checks if this folder is full.
   */
  public static boolean isFull(ItemStack stack) {
    return getPageCount(stack) >= MAX_PAGES;
  }

  /**
   * Checks if this folder is empty.
   */
  public static boolean isEmpty(ItemStack stack) {
    return getPages(stack).isEmpty();
  }

  /**
   * Writes a symbol to the first blank page in this folder. This is a key
   * feature that distinguishes Folder from Portfolio. Folders are WRITABLE -
   * you can write symbols directly to pages inside.
   *
   * @param folder The folder item stack
   * @param symbol The symbol Identifier to write
   * @return true if a blank page was found and written to
   */
  public static boolean writeSymbol(ItemStack folder, Identifier symbol) {
    List<ItemStack> pages = getPages(folder);

    for (int i = 0; i < pages.size(); i++) {
      ItemStack page = pages.get(i);
      if (!page.isEmpty() && Page.isBlank(page)) {

        Page.setSymbol(page, symbol);
        setPages(folder, pages);
        return true;
      }
    }
    return false;
  }

  /**
   * Counts the number of blank pages in this folder that can be written to.
   */
  public static int countBlankPages(ItemStack folder) {
    int count = 0;
    for (ItemStack page : getPages(folder)) {
      if (!page.isEmpty() && Page.isBlank(page)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Checks if this folder has any blank pages that can be written to.
   */
  public static boolean hasBlankPages(ItemStack folder) {
    return countBlankPages(folder) > 0;
  }

  /**
   * Checks if this folder can be used as a BookBinder cover. Only empty folders
   * can serve as book covers.
   */
  public static boolean canBeBookCover(ItemStack folder) {
    return isEmpty(folder);
  }

  /**
   * Extracts all pages from this folder (for BookBinder batch import). Returns
   * the list and clears the folder.
   */
  public static List<ItemStack> extractAllPages(ItemStack folder) {
    List<ItemStack> pages = getPages(folder);
    clearPages(folder);
    return pages;
  }

  /**
   * Gets a page at a specific slot index. Folders support ORDERED access -
   * pages have fixed positions.
   */
  public static ItemStack getPageAt(ItemStack folder, int index) {
    List<ItemStack> pages = getPages(folder);
    if (index < 0 || index >= pages.size()) {
      return ItemStack.EMPTY;
    }
    return pages.get(index);
  }

  /**
   * Sets a page at a specific slot index, returning the displaced page. Folders
   * support ORDERED placement - you can put pages at specific positions.
   */
  public static ItemStack setPageAt(ItemStack folder, int index, ItemStack page) {
    if (index < 0 || index >= MAX_PAGES) {
      return page;
    }
    List<ItemStack> pages = getPages(folder);

    while (pages.size() <= index) {
      pages.add(ItemStack.EMPTY);
    }

    ItemStack displaced = pages.get(index);
    pages.set(index, page.copy());
    setPages(folder, pages);
    return displaced;
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                              @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip,
                              @NotNull TooltipFlag flag) {
    List<ItemStack> pages = getPages(stack);
    if (!pages.isEmpty()) {
      tooltip.accept(Component.translatable("item.mystcraft.folder.pages", pages.size(), MAX_PAGES));
    } else {
      tooltip.accept(Component.translatable("item.mystcraft.folder.empty"));
    }
  }

  @Override
  @NotNull
  public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
      // Repair legacy stacks before any page NBT can be written. Keep the
      // folder being opened in the selected hand slot and safely redistribute
      // or drop every additional folder.
      if (stack.getCount() > 1) {
        ItemStack extras = stack.copy();
        extras.setCount(stack.getCount() - 1);
        stack.setCount(1);
        if (!player.getInventory().add(extras) && !extras.isEmpty()) {
          player.drop(extras, false);
        }
      }
      int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 40;
      MenuProvider provider = new MenuProvider() {
        @Override
        public Component getDisplayName() {
          return Component.translatable("container.mystcraft.folder");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
          return new FolderMenu(containerId, playerInventory, slot);
        }
      };
      Services.PLATFORM.openMenu(serverPlayer, provider, buf -> buf.writeVarInt(slot));
    }

    return InteractionResult.SUCCESS;
  }
}
