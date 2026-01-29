package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.platform.Services;
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
import java.util.List;

/**
 * The Folder item - an ORDERED, WRITABLE page container.
 * <p>
 * Key differences from Portfolio:
 * - ORDERED: Pages have fixed slot positions (0, 1, 2, ...)
 * - WRITABLE: Can write symbols directly to blank pages inside
 * - STACKABLE: Stacks to 32 when empty (workspace items)
 * - BOOKBINDER: Can be used as a book cover (when empty)
 * - CAPACITY: 16 pages (a working set for Age creation)
 * <p>
 * The Folder is designed as a portable workspace for organizing
 * pages at the Writing Desk and binding them into Age books.
 */
public class FolderItem extends Item {

  public static final int MAX_PAGES = 16;
  private static final String TAG_PAGES = "Pages";
  // Folder stacks to 32 when empty (workspace item behavior)
  private static final int STACK_SIZE_EMPTY = 32;
  private static final int STACK_SIZE_FILLED = 1;

  public FolderItem(Properties properties) {
    super(properties.stacksTo(STACK_SIZE_EMPTY)); // Default max stack when empty
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
   *
   * @return true if the page was added successfully
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
   * Writes a symbol to the first blank page in this folder.
   * This is a key feature that distinguishes Folder from Portfolio.
   * Folders are WRITABLE - you can write symbols directly to pages inside.
   *
   * @param folder The folder item stack
   * @param symbol The symbol ResourceLocation to write
   * @return true if a blank page was found and written to
   */
  public static boolean writeSymbol(ItemStack folder, ResourceLocation symbol) {
    List<ItemStack> pages = getPages(folder);

    for (int i = 0; i < pages.size(); i++) {
      ItemStack page = pages.get(i);
      if (!page.isEmpty() && Page.isBlank(page)) {
        // Found a blank page - write the symbol to it
        Page.setSymbol(page, symbol);
        setPages(folder, pages);
        return true;
      }
    }
    return false; // No blank pages found
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

  // --- Folder: Writable ---

  /**
   * Checks if this folder can be used as a BookBinder cover.
   * Only empty folders can serve as book covers.
   */
  public static boolean canBeBookCover(ItemStack folder) {
    return isEmpty(folder);
  }

  /**
   * Extracts all pages from this folder (for BookBinder batch import).
   * Returns the list and clears the folder.
   */
  public static List<ItemStack> extractAllPages(ItemStack folder) {
    List<ItemStack> pages = getPages(folder);
    clearPages(folder);
    return pages;
  }

  /**
   * Gets a page at a specific slot index.
   * Folders support ORDERED access - pages have fixed positions.
   */
  public static ItemStack getPageAt(ItemStack folder, int index) {
    List<ItemStack> pages = getPages(folder);
    if (index < 0 || index >= pages.size()) {
      return ItemStack.EMPTY;
    }
    return pages.get(index);
  }

  // --- Folder: BookBinder Cover ---

  /**
   * Sets a page at a specific slot index, returning the displaced page.
   * Folders support ORDERED placement - you can put pages at specific positions.
   */
  public static ItemStack setPageAt(ItemStack folder, int index, ItemStack page) {
    List<ItemStack> pages = getPages(folder);

    // Extend the list if needed
    while (pages.size() <= index) {
      pages.add(ItemStack.EMPTY);
    }

    ItemStack displaced = pages.get(index);
    pages.set(index, page.copy());
    setPages(folder, pages);
    return displaced;
  }

  /**
   * Folders stack to 32 when empty, but only 1 when containing pages.
   */
  public int getMaxStackSize(ItemStack stack) {
    return isEmpty(stack) ? STACK_SIZE_EMPTY : STACK_SIZE_FILLED;
  }

  // --- Folder: Ordered Access ---

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

    if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
      int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
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

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }
}
