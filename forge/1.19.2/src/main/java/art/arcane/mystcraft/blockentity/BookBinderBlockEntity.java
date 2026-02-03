package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.menu.BookBinderMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Block entity for the Book Binder.
 * Used to create descriptive books and linkbooks from pages.
 * <p>
 * Has a cover slot for leather and stores a list of pages
 * that will be bound into the book.
 * <p>
 * 1.19.2 version: Uses Registry.ITEM instead of BuiltInRegistries.ITEM
 */
public class BookBinderBlockEntity extends MystcraftBlockEntity implements MenuProvider {

  private static final String TAG_ITEMS = "items";
  private static final String TAG_PAGES = "pages";
  private static final String TAG_TITLE = "title";

  private static final int COVER_SLOT = 0;

  private final SimpleContainer inventory = new SimpleContainer(1) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      if (slot == COVER_SLOT) {
        return isValidCover(stack);
      }
      return true;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      BookBinderBlockEntity.this.setChanged();
    }
  };

  private List<ItemStack> pages = new LinkedList<>();
  private String pendingTitle = null;

  public BookBinderBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.BOOK_BINDER.get(), pos, blockState);
  }

  /**
   * Checks if a stack is valid as a book cover.
   * Valid items are configured in MystcraftConfig.bookBinderCoverItems.
   * For folders, they must be empty.
   */
  public static boolean isValidCover(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }

    // Check if item is in the configured list
    // 1.19.2: Use Registry.ITEM instead of BuiltInRegistries.ITEM
    ResourceLocation itemId = Registry.ITEM.getKey(stack.getItem());
    List<String> validItems = MystcraftConfig.bookBinderCoverItems.get();

    boolean isConfiguredItem = validItems.stream()
        .anyMatch(id -> id.equals(itemId.toString()));

    if (!isConfiguredItem) {
      return false;
    }

    // Special case: folders must be empty
    if (stack.getItem() instanceof FolderItem) {
      return FolderItem.isEmpty(stack);
    }

    return true;
  }

  /**
   * Gets the inventory container for external access.
   */
  public Container getInventory() {
    return inventory;
  }

  @Override
  protected void writeNbt(CompoundTag tag) {
    super.writeNbt(tag);
    ListTag itemList = new ListTag();
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putInt("Slot", i);
        stack.save(itemTag);
        itemList.add(itemTag);
      }
    }
    tag.put(TAG_ITEMS, itemList);

    ListTag pageList = new ListTag();
    for (ItemStack page : pages) {
      pageList.add(page.save(new CompoundTag()));
    }
    tag.put(TAG_PAGES, pageList);

    if (pendingTitle != null) {
      tag.putString(TAG_TITLE, pendingTitle);
    }
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    super.readNbt(tag);
    inventory.clearContent();
    ListTag itemList = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
    for (int i = 0; i < itemList.size(); i++) {
      CompoundTag itemTag = itemList.getCompound(i);
      int slot = itemTag.getInt("Slot");
      if (slot >= 0 && slot < inventory.getContainerSize()) {
        inventory.setItem(slot, ItemStack.of(itemTag));
      }
    }

    pages.clear();
    ListTag pageList = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
    for (int i = 0; i < pageList.size(); i++) {
      ItemStack page = ItemStack.of(pageList.getCompound(i));
      if (!page.isEmpty()) {
        pages.add(page);
      }
    }

    if (tag.contains(TAG_TITLE)) {
      pendingTitle = tag.getString(TAG_TITLE);
    } else {
      pendingTitle = null;
    }
  }

  /**
   * Gets the pending title for the book.
   */
  @NotNull
  public String getPendingTitle() {
    return pendingTitle == null ? "" : pendingTitle;
  }

  /**
   * Sets the book title.
   */
  public void setBookTitle(@Nullable String title) {
    this.pendingTitle = title;
    setChanged();
  }

  /**
   * Gets the list of pages in this binder.
   */
  public List<ItemStack> getPageList() {
    return pages;
  }

  /**
   * Sets the pages (for client sync).
   */
  public void setPages(List<ItemStack> pages) {
    if (level != null && level.isClientSide) {
      this.pages = pages;
    }
  }

  /**
   * Gets the cover slot item.
   */
  public ItemStack getCoverStack() {
    return inventory.getItem(COVER_SLOT);
  }

  /**
   * Sets the cover slot item.
   */
  public void setCoverStack(ItemStack stack) {
    inventory.setItem(COVER_SLOT, stack);
  }

  /**
   * Inserts a page at the given index.
   *
   * @return remaining stack that couldn't be inserted
   */
  @NotNull
  public ItemStack insertPage(@NotNull ItemStack stack, int index) {
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }

    // Check page limit
    int maxSymbols = MystcraftConfig.maxSymbolsPerBook.get();
    if (maxSymbols >= 0 && pages.size() >= maxSymbols) {
      return stack;
    }

    // Only accept pages
    if (!(stack.getItem() instanceof art.arcane.mystcraft.item.PageItem)) {
      return stack;
    }

    // Insert pages one at a time
    while (stack.getCount() > 0) {
      if (maxSymbols >= 0 && pages.size() >= maxSymbols) {
        break;
      }
      ItemStack clone = stack.copy();
      clone.setCount(1);
      pages.add(Math.min(index, pages.size()), clone);
      stack.shrink(1);
      index++;
    }

    setChanged();
    markForUpdate();
    return ItemStack.EMPTY;
  }

  /**
   * Inserts pages from a folder.
   */
  @NotNull
  public ItemStack insertFromFolder(@NotNull ItemStack folder, int index) {
    if (!(folder.getItem() instanceof FolderItem)) {
      return folder;
    }

    List<ItemStack> folderPages = FolderItem.getPages(folder);
    if (folderPages.isEmpty()) {
      // Empty folder - collect pages into it
      for (ItemStack page : pages) {
        FolderItem.addPage(folder, page);
      }
      pages.clear();
    } else {
      // Non-empty folder - insert pages from it
      for (ItemStack page : folderPages) {
        if (!page.isEmpty()) {
          ItemStack remainder = insertPage(page, index);
          if (remainder.isEmpty()) {
            index++;
          }
        }
      }
      // Clear the folder
      FolderItem.clearPages(folder);
    }

    setChanged();
    markForUpdate();
    return ItemStack.EMPTY;
  }

  /**
   * Removes a page at the given index.
   */
  @NotNull
  public ItemStack removePage(int index) {
    if (index < 0 || index >= pages.size()) {
      return ItemStack.EMPTY;
    }
    ItemStack removed = pages.remove(index);
    setChanged();
    markForUpdate();
    return removed;
  }

  /**
   * Checks if a book can be built with current contents.
   */
  public boolean canBuildItem() {
    // Need a valid cover
    if (!isValidCover(getCoverStack())) {
      return false;
    }
    // Need at least one page
    if (pages.isEmpty()) {
      return false;
    }
    // First page must be a link panel
    if (!Page.isLinkPanel(pages.get(0))) {
      return false;
    }
    // Need a title
    if (pendingTitle == null || pendingTitle.isEmpty()) {
      return false;
    }
    // No other pages can be link panels
    for (int i = 1; i < pages.size(); i++) {
      if (Page.isLinkPanel(pages.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets the item that would be crafted.
   */
  @NotNull
  public ItemStack getCraftedItem() {
    if (!canBuildItem()) {
      return ItemStack.EMPTY;
    }
    return new ItemStack(ModItems.AGEBOOK.get());
  }

  /**
   * Builds the book and gives it to the player.
   */
  public void buildItem(@NotNull ItemStack output, @NotNull Player player) {
    if (!canBuildItem()) {
      return;
    }

    if (output.getItem() instanceof AgebookItem) {
      // Create the agebook with pages
      AgebookItem.create(output, player, new ArrayList<>(pages), pendingTitle);

      // Clear the pages
      pages.clear();
      pendingTitle = null;

      // Consume one cover
      ItemStack cover = getCoverStack();
      cover.shrink(1);
      if (cover.isEmpty()) {
        inventory.setItem(COVER_SLOT, ItemStack.EMPTY);
      }

      setChanged();
      markForUpdate();
    } else {
      output.setCount(0);
    }
  }

  /**
   * Drops all contents when block is broken.
   */
  public List<ItemStack> getDrops() {
    List<ItemStack> drops = new ArrayList<>();

    // Add cover if present
    ItemStack cover = getCoverStack();
    if (!cover.isEmpty()) {
      drops.add(cover.copy());
    }

    // Add all pages
    drops.addAll(pages);

    return drops;
  }

  // MenuProvider implementation

  @Override
  @NotNull
  public Component getDisplayName() {
    return Component.translatable("container.mystcraft.book_binder");
  }

  @Override
  public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
    return new BookBinderMenu(containerId, playerInventory, this);
  }
}
