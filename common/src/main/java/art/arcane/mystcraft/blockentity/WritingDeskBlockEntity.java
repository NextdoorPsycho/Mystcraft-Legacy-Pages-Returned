package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.*;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Block entity for the Writing Desk.
 * Used for writing symbols onto pages using ink.
 * <p>
 * Main inventory slots:
 * 0 - Writing slot (book/page being written to)
 * 1 - Paper slot (paper supply)
 * 2 - Container in (ink bucket input)
 * 3 - Container out (empty bucket output)
 * <p>
 * Tab inventory:
 * 25 slots for notebooks, folders, portfolios
 */
public class WritingDeskBlockEntity extends MystcraftBlockEntity implements MenuProvider {

  public static final int SLOT_WRITING = 0;
  public static final int SLOT_PAPER = 1;
  public static final int SLOT_CONTAINER_IN = 2;
  public static final int SLOT_CONTAINER_OUT = 3;
  public static final int MAIN_SLOT_COUNT = 4;
  public static final int TAB_SLOT_COUNT = 25;
  /**
   * Ink cost per symbol written (in mB)
   */
  public static final int INK_COST = 100;
  /**
   * Ink tank capacity (1 bucket = 1000 mB)
   */
  public static final int INK_CAPACITY = 1000;
  private static final String TAG_INVENTORY = "inventory";
  private static final String TAG_TABS = "tabs";
  private static final String TAG_INK = "ink";
  private final SimpleContainer mainInventory = new SimpleContainer(MAIN_SLOT_COUNT) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      return switch (slot) {
        case SLOT_WRITING -> isWritableItem(stack);
        case SLOT_PAPER -> stack.is(Items.PAPER);
        case SLOT_CONTAINER_IN -> isInkContainer(stack);
        case SLOT_CONTAINER_OUT -> false; // Output only
        default -> false;
      };
    }

    @Override
    public int getMaxStackSize() {
      return 64;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      WritingDeskBlockEntity.this.setChanged();
      WritingDeskBlockEntity.this.markForUpdate();
    }
  };

  private final SimpleContainer tabInventory = new SimpleContainer(TAB_SLOT_COUNT) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      return isPageCollectionItem(stack);
    }

    @Override
    public int getMaxStackSize() {
      return 1;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      WritingDeskBlockEntity.this.setChanged();
      WritingDeskBlockEntity.this.markForUpdate();
    }
  };

  private final InkTank inkTank = new InkTank(INK_CAPACITY);

  public WritingDeskBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.WRITING_DESK.get(), pos, blockState);
  }

  /**
   * Checks if a fluid is valid ink.
   */
  public static boolean isValidInk(Fluid fluid) {
    return fluid == ModFluids.BLACK_INK_SOURCE.get() ||
        fluid == ModFluids.BLACK_INK_FLOWING.get();
  }

  /**
   * Checks if an item can be written to.
   */
  public static boolean isWritableItem(ItemStack stack) {
    if (stack.isEmpty()) return false;
    // Can write to blank pages
    if (stack.getItem() instanceof PageItem && Page.isBlank(stack)) {
      return true;
    }
    // Can write to agebooks (add pages)
    if (stack.getItem() instanceof AgebookItem) {
      return true;
    }
    // Can write to linkbooks (add pages)
    return stack.getItem() instanceof LinkbookItem;
  }

  /**
   * Checks if an item is a page collection (folder, portfolio).
   */
  public static boolean isPageCollectionItem(ItemStack stack) {
    if (stack.isEmpty()) return false;
    return stack.getItem() instanceof FolderItem ||
        stack.getItem() instanceof PortfolioItem ||
        stack.getItem() instanceof AgebookItem ||
        stack.getItem() instanceof LinkbookItem;
  }

  /**
   * Checks if an item is an ink container (bucket with ink or ink vial).
   */
  public static boolean isInkContainer(ItemStack stack) {
    if (stack.isEmpty()) return false;
    // Accept Mystcraft ink vials with ink
    if (stack.getItem() instanceof InkVialItem vial) {
      return vial.getInkAmount(stack) > 0;
    }
    // Accept ink buckets
    return stack.is(ModFluids.BLACK_INK_BUCKET.get());
  }

  /**
   * Gets the main inventory container for external access.
   */
  public Container getMainInventory() {
    return mainInventory;
  }

  /**
   * Gets the tab inventory container for external access.
   */
  public Container getTabInventory() {
    return tabInventory;
  }

  @Override
  protected void writeNbt(CompoundTag tag) {
    super.writeNbt(tag);
    tag.put(TAG_INVENTORY, saveContainerToTag(mainInventory));
    tag.put(TAG_TABS, saveContainerToTag(tabInventory));
    CompoundTag inkTag = new CompoundTag();
    inkTank.save(inkTag);
    tag.put(TAG_INK, inkTag);
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    super.readNbt(tag);
    loadContainerFromTag(mainInventory, tag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND));
    loadContainerFromTag(tabInventory, tag.getList(TAG_TABS, Tag.TAG_COMPOUND));
    inkTank.load(tag.getCompound(TAG_INK));
  }

  private ListTag saveContainerToTag(SimpleContainer container) {
    ListTag itemList = new ListTag();
    for (int i = 0; i < container.getContainerSize(); i++) {
      ItemStack stack = container.getItem(i);
      if (!stack.isEmpty()) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putInt("Slot", i);
        stack.save(itemTag);
        itemList.add(itemTag);
      }
    }
    return itemList;
  }

  private void loadContainerFromTag(SimpleContainer container, ListTag itemList) {
    container.clearContent();
    for (int i = 0; i < itemList.size(); i++) {
      CompoundTag itemTag = itemList.getCompound(i);
      int slot = itemTag.getInt("Slot");
      if (slot >= 0 && slot < container.getContainerSize()) {
        container.setItem(slot, ItemStack.of(itemTag));
      }
    }
  }

  /**
   * Gets the item in the writing slot.
   */
  @NotNull
  public ItemStack getWritingItem() {
    return mainInventory.getItem(SLOT_WRITING);
  }

  /**
   * Sets the item in the writing slot.
   */
  public void setWritingItem(@NotNull ItemStack stack) {
    mainInventory.setItem(SLOT_WRITING, stack);
  }

  /**
   * Gets the paper count available.
   */
  public int getPaperCount() {
    ItemStack paper = mainInventory.getItem(SLOT_PAPER);
    return paper.isEmpty() ? 0 : paper.getCount();
  }

  /**
   * Consumes one piece of paper.
   */
  public boolean consumePaper() {
    ItemStack paper = mainInventory.getItem(SLOT_PAPER);
    if (paper.isEmpty()) return false;
    paper.shrink(1);
    return true;
  }

  /**
   * Gets the tab item at the specified index.
   */
  @NotNull
  public ItemStack getTabItem(int index) {
    if (index < 0 || index >= TAB_SLOT_COUNT) {
      return ItemStack.EMPTY;
    }
    return tabInventory.getItem(index);
  }

  /**
   * Gets the current ink amount in mB.
   */
  public int getInkAmount() {
    return inkTank.getAmount();
  }

  /**
   * Gets the ink tank for rendering.
   */
  public InkTank getInkTank() {
    return inkTank;
  }

  /**
   * Checks if there's enough ink to write a symbol.
   */
  public boolean hasEnoughInk() {
    return getInkAmount() >= INK_COST;
  }

  /**
   * Uses ink for writing.
   */
  private void useInk() {
    inkTank.drain(INK_COST);
  }

  /**
   * Writes a symbol to the current writing item.
   * Returns true if successful.
   */
  public boolean writeSymbol(@Nullable Player player, ResourceLocation symbol) {
    if (level == null || level.isClientSide) return false;
    if (!hasEnoughInk()) return false;

    ItemStack writingItem = getWritingItem();

    // If writing slot is empty but we have paper, create a blank page
    if (writingItem.isEmpty() && getPaperCount() > 0) {
      ItemStack page = Page.createPage();
      setWritingItem(page);
      consumePaper();
      writingItem = page;
    }

    if (writingItem.isEmpty()) return false;

    // Write to a blank page
    if (writingItem.getItem() instanceof PageItem && Page.isBlank(writingItem)) {
      Page.setSymbol(writingItem, symbol);
      useInk();
      markForUpdate();
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
        art.arcane.mystcraft.advancements.ModAdvancements.WRITING_DESK_WRITE.trigger(serverPlayer);
      }
      return true;
    }

    // Write to an agebook (creates a new page and adds it)
    if (writingItem.getItem() instanceof AgebookItem agebookItem && getPaperCount() > 0) {
      ItemStack page = Page.createSymbolPage(symbol);
      agebookItem.addPages(writingItem, java.util.Collections.singletonList(page));
      useInk();
      consumePaper();
      markForUpdate();
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
        art.arcane.mystcraft.advancements.ModAdvancements.WRITING_DESK_WRITE.trigger(serverPlayer);
      }
      return true;
    }

    return false;
  }

  /**
   * Processes fluid containers - called from tick.
   */
  public void processFluidContainers() {
    if (level == null || level.isClientSide) return;

    ItemStack containerIn = mainInventory.getItem(SLOT_CONTAINER_IN);
    ItemStack containerOut = mainInventory.getItem(SLOT_CONTAINER_OUT);

    if (containerIn.isEmpty()) return;

    // Handle ink vials separately (they don't use fluid capabilities)
    if (containerIn.getItem() instanceof InkVialItem vial) {
      int vialInk = vial.getInkAmount(containerIn);
      int spaceInTank = INK_CAPACITY - getInkAmount();
      if (vialInk > 0 && spaceInTank > 0) {
        // Each vial unit = 10 mB of fluid (100 vial units = 1000 mB = 1 bucket)
        int inkToTransfer = Math.min(vialInk * 10, spaceInTank);
        int vialUnitsUsed = (inkToTransfer + 9) / 10; // Round up
        inkToTransfer = vialUnitsUsed * 10; // Actual amount transferred

        inkTank.fill(inkToTransfer);
        vial.setInkAmount(containerIn, vialInk - vialUnitsUsed);

        // If vial is empty, output empty glass bottle
        if (vial.getInkAmount(containerIn) <= 0) {
          ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
          if (containerOut.isEmpty()) {
            mainInventory.setItem(SLOT_CONTAINER_OUT, emptyBottle);
            containerIn.shrink(1);
          } else if (containerOut.is(Items.GLASS_BOTTLE) && containerOut.getCount() < containerOut.getMaxStackSize()) {
            containerOut.grow(1);
            containerIn.shrink(1);
          }
          // If output is full, don't consume the vial
        }
      }
      return;
    }

    // Handle ink buckets
    if (containerIn.is(ModFluids.BLACK_INK_BUCKET.get())) {
      int spaceInTank = INK_CAPACITY - getInkAmount();
      if (spaceInTank >= 1000) {
        // Get the empty container (crafting remainder)
        ItemStack emptyContainer;
        if (containerIn.getItem().hasCraftingRemainingItem()) {
          emptyContainer = new ItemStack(containerIn.getItem().getCraftingRemainingItem());
        } else {
          emptyContainer = new ItemStack(Items.BUCKET);
        }

        // Check if we can output the empty container
        if (containerOut.isEmpty()) {
          inkTank.fill(1000);
          mainInventory.setItem(SLOT_CONTAINER_OUT, emptyContainer);
          containerIn.shrink(1);
        } else if (ItemStack.isSameItemSameTags(containerOut, emptyContainer) &&
            containerOut.getCount() < containerOut.getMaxStackSize()) {
          inkTank.fill(1000);
          containerOut.grow(1);
          containerIn.shrink(1);
        }
      }
    }
  }

  /**
   * Server tick - process fluid containers.
   */
  public void tick() {
    if (level == null || level.isClientSide) return;
    processFluidContainers();
  }

  /**
   * Removes a page from a tab item at the specified index.
   */
  @NotNull
  public ItemStack removePageFromTab(@Nullable Player player, int tabIndex, int pageIndex) {
    ItemStack tabItem = getTabItem(tabIndex);
    if (tabItem.isEmpty()) return ItemStack.EMPTY;

    if (tabItem.getItem() instanceof FolderItem) {
      List<ItemStack> pages = FolderItem.getPages(tabItem);
      if (pageIndex >= 0 && pageIndex < pages.size()) {
        ItemStack removed = pages.remove(pageIndex);
        FolderItem.setPages(tabItem, pages);
        markForUpdate();
        return removed;
      }
    } else if (tabItem.getItem() instanceof PortfolioItem) {
      List<ItemStack> pages = PortfolioItem.getPages(tabItem);
      if (pageIndex >= 0 && pageIndex < pages.size()) {
        ItemStack removed = pages.remove(pageIndex);
        PortfolioItem.setPages(tabItem, pages);
        markForUpdate();
        return removed;
      }
    }

    return ItemStack.EMPTY;
  }

  /**
   * Adds a page to a tab item.
   */
  @NotNull
  public ItemStack addPageToTab(@Nullable Player player, int tabIndex, @NotNull ItemStack page) {
    if (page.isEmpty()) return page;

    ItemStack tabItem = getTabItem(tabIndex);
    if (tabItem.isEmpty()) return page;

    if (tabItem.getItem() instanceof FolderItem) {
      if (!FolderItem.isFull(tabItem)) {
        FolderItem.addPage(tabItem, page.copy());
        markForUpdate();
        return ItemStack.EMPTY;
      }
    } else if (tabItem.getItem() instanceof PortfolioItem) {
      if (!PortfolioItem.isFull(tabItem)) {
        PortfolioItem.addPage(tabItem, page.copy());
        markForUpdate();
        return ItemStack.EMPTY;
      }
    }

    return page;
  }

  /**
   * Gets all items for dropping when block is broken.
   */
  public List<ItemStack> getDrops() {
    List<ItemStack> drops = new ArrayList<>();

    // Main inventory
    for (int i = 0; i < mainInventory.getContainerSize(); i++) {
      ItemStack stack = mainInventory.getItem(i);
      if (!stack.isEmpty()) {
        drops.add(stack.copy());
      }
    }

    // Tab inventory
    for (int i = 0; i < tabInventory.getContainerSize(); i++) {
      ItemStack stack = tabInventory.getItem(i);
      if (!stack.isEmpty()) {
        drops.add(stack.copy());
      }
    }

    return drops;
  }

  // MenuProvider implementation

  @Override
  public Component getDisplayName() {
    return Component.translatable("container.mystcraft.writing_desk");
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
    return new WritingDeskMenu(containerId, playerInventory, this);
  }

  // --- InkTank ---

  /**
   * Simple ink tank that tracks fluid amount without Forge fluid capabilities.
   */
  public static class InkTank {
    private final int capacity;
    private int amount;

    public InkTank(int capacity) {
      this.capacity = capacity;
      this.amount = 0;
    }

    public int getAmount() {
      return amount;
    }

    public int getCapacity() {
      return capacity;
    }

    public boolean isEmpty() {
      return amount <= 0;
    }

    public int fill(int amount) {
      int filled = Math.min(amount, capacity - this.amount);
      this.amount += filled;
      return filled;
    }

    public int drain(int amount) {
      int drained = Math.min(amount, this.amount);
      this.amount -= drained;
      return drained;
    }

    public void load(CompoundTag tag) {
      this.amount = tag.getInt("InkAmount");
    }

    public CompoundTag save(CompoundTag tag) {
      tag.putInt("InkAmount", amount);
      return tag;
    }
  }
}
