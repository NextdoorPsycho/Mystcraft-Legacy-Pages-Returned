package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.util.NbtCompat;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.menu.BookBinderMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Block entity for the Book Binder. Used to create descriptive books and
 * linkbooks from pages.
 * <p>
 * Has a cover slot for leather and stores a list of pages that will be bound
 * into the book.
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
   * Checks if a stack is valid as a book cover. Valid items are configured in
   * MystcraftConfig.bookBinderCoverItems. For folders, they must be empty.
   */
  public static boolean isValidCover(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }

    Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    List<String> validItems = MystcraftConfig.bookBinderCoverItems.get();

    boolean isConfiguredItem = validItems.stream()
        .anyMatch(id -> id.equals(itemId.toString()));

    if (!isConfiguredItem) {
      return false;
    }

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
        itemTag.merge(ItemStackNbt.save(stack));
        itemList.add(itemTag);
      }
    }
    tag.put(TAG_ITEMS, itemList);

    ListTag pageList = new ListTag();
    for (ItemStack page : pages) {
      pageList.add(ItemStackNbt.save(page));
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
    ListTag itemList = tag.getListOrEmpty(TAG_ITEMS);
    for (int i = 0; i < itemList.size(); i++) {
      CompoundTag itemTag = itemList.getCompoundOrEmpty(i);
      int slot = itemTag.getIntOr("Slot", 0);
      if (slot >= 0 && slot < inventory.getContainerSize()) {
        CompoundTag itemData = NbtCompat.contains(itemTag, "Item", Tag.TAG_COMPOUND) ? itemTag.getCompoundOrEmpty("Item") : itemTag;
        inventory.setItem(slot, ItemStackNbt.load(itemData));
      }
    }

    pages.clear();
    ListTag pageList = tag.getListOrEmpty(TAG_PAGES);
    for (int i = 0; i < pageList.size(); i++) {
      ItemStack page = ItemStackNbt.load(pageList.getCompoundOrEmpty(i));
      if (!page.isEmpty()) {
        pages.add(page);
      }
    }

    if (tag.contains(TAG_TITLE)) {
      pendingTitle = tag.getStringOr(TAG_TITLE, "");
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
    if (level != null && level.isClientSide()) {
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

    int maxSymbols = MystcraftConfig.maxSymbolsPerBook.get();
    if (maxSymbols >= 0 && pages.size() >= maxSymbols) {
      return stack;
    }

    if (!(stack.getItem() instanceof art.arcane.mystcraft.item.PageItem)) {
      return stack;
    }

    int insertIndex = Math.max(0, Math.min(index, pages.size()));
    boolean insertedAny = false;
    while (stack.getCount() > 0) {
      if (maxSymbols >= 0 && pages.size() >= maxSymbols) {
        break;
      }
      ItemStack clone = stack.copy();
      clone.setCount(1);
      pages.add(insertIndex, clone);
      stack.shrink(1);
      insertIndex++;
      insertedAny = true;
    }

    if (insertedAny) {
      setChanged();
      markForUpdate();
    }
    return stack.isEmpty() ? ItemStack.EMPTY : stack;
  }

  /**
   * Transfers as many pages as possible between this binder and a folder.
   *
   * @return the same folder stack, retaining every page that could not move
   */
  @NotNull
  public ItemStack insertFromFolder(@NotNull ItemStack folder, int index) {
    if (!(folder.getItem() instanceof FolderItem)) {
      return folder;
    }

    List<ItemStack> folderPages = FolderItem.getPages(folder);
    if (folderPages.isEmpty()) {
      List<ItemStack> binderRemainder = new ArrayList<>();
      for (ItemStack page : pages) {
        if (!FolderItem.addPage(folder, page)) {
          binderRemainder.add(page);
        }
      }
      pages.clear();
      pages.addAll(binderRemainder);
    } else {
      int insertIndex = Math.max(0, Math.min(index, pages.size()));
      List<ItemStack> folderRemainder = new ArrayList<>();
      for (ItemStack page : folderPages) {
        if (!page.isEmpty()) {
          int originalCount = page.getCount();
          ItemStack remainder = insertPage(page, insertIndex);
          int insertedCount = originalCount - remainder.getCount();
          insertIndex += insertedCount;
          if (!remainder.isEmpty()) {
            folderRemainder.add(remainder.copy());
          }
        }
      }
      FolderItem.setPages(folder, folderRemainder);
    }

    setChanged();
    markForUpdate();
    return folder;
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

    if (!isValidCover(getCoverStack())) {
      return false;
    }

    if (pages.isEmpty()) {
      return false;
    }

    if (!Page.isLinkPanel(pages.get(0))) {
      return false;
    }

    if (pendingTitle == null || pendingTitle.isEmpty()) {
      return false;
    }

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

      ItemStack cover = getCoverStack();
      Identifier coverId = cover.isEmpty() ? null
          : BuiltInRegistries.ITEM.getKey(cover.getItem());

      AgebookItem.create(output, player, new ArrayList<>(pages), pendingTitle);

      if (coverId != null) {
        LinkOptions opts = LinkOptions.fromItemStack(output);
        if (opts == null) {
          opts = new LinkOptions(null);
        }
        CompoundTag data = opts.getTagCompound();
        LinkOptions.setCoverItemId(data, coverId);
        opts.toItemStack(output);
      }

      pages.clear();
      pendingTitle = null;

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

    ItemStack cover = getCoverStack();
    if (!cover.isEmpty()) {
      drops.add(cover.copy());
    }

    drops.addAll(pages);

    return drops;
  }

  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if (level != null) {
      for (ItemStack drop : getDrops()) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
      }
    }
    super.preRemoveSideEffects(pos, state);
  }

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
