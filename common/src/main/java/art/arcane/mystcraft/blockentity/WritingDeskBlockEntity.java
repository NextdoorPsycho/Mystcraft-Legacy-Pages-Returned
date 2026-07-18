package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.InkBlend;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.*;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModFluids;
import art.arcane.mystcraft.registry.ModTags;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ItemStackNbt;
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
 * Block entity for the Writing Desk. Used for writing symbols onto pages using
 * ink.
 * <p>
 * Main inventory slots: 0 - Writing slot (book/page being written to) 1 - Paper
 * slot (paper supply) 2 - Container in (ink bucket input) 3 - Container out
 * (empty bucket output)
 * <p>
 * Tab inventory: 25 slots for notebooks, folders, portfolios
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
        case SLOT_PAPER -> isBlankPage(stack);
        case SLOT_CONTAINER_IN -> isInkContainer(stack);
        case SLOT_CONTAINER_OUT -> false;
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

    if (stack.getItem() instanceof PageItem && Page.isBlank(stack)) {
      return true;
    }

    if (stack.getItem() instanceof AgebookItem) {
      return true;
    }

    return stack.getItem() instanceof LinkbookItem && !(stack.getItem() instanceof PersonalLinkBookItem);
  }

  /**
   * Checks if an item is a page collection (folder, portfolio).
   */
  public static boolean isPageCollectionItem(ItemStack stack) {
    if (stack.isEmpty()) return false;
    return stack.getItem() instanceof FolderItem ||
        stack.getItem() instanceof PortfolioItem ||
        stack.getItem() instanceof AgebookItem ||
        (stack.getItem() instanceof LinkbookItem && !(stack.getItem() instanceof PersonalLinkBookItem));
  }

  /**
   * Checks if an item is an ink container (bucket with ink or ink vial).
   */
  public static boolean isInkContainer(ItemStack stack) {
    if (stack.isEmpty()) return false;

    if (stack.getItem() instanceof InkVialItem vial) {
      return vial.getInkAmount(stack) > 0;
    }

    return stack.is(ModTags.Items.INK_BUCKETS);
  }

  public static boolean isBlankPage(ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() instanceof PageItem && Page.isBlank(stack);
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
        itemTag.merge(ItemStackNbt.save(stack));
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
        CompoundTag itemData = itemTag.contains("Item", Tag.TAG_COMPOUND) ? itemTag.getCompound("Item") : itemTag;
        container.setItem(slot, ItemStackNbt.load(itemData));
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
    return isBlankPage(paper) ? paper.getCount() : 0;
  }

  /**
   * Consumes one piece of paper.
   */
  public boolean consumePaper() {
    ItemStack paper = mainInventory.getItem(SLOT_PAPER);
    if (!isBlankPage(paper)) return false;
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

  private void useInk() {
    inkTank.drain(INK_COST);
  }

  /**
   * Writes a symbol to the current writing item. Returns true if successful.
   */
  public boolean writeSymbol(@Nullable Player player, ResourceLocation symbol) {
    if (level == null || level.isClientSide) return false;
    if (!hasEnoughInk()) return false;
    if (SymbolRegistry.get(symbol) == null) return false;

    ItemStack writingItem = getWritingItem();

    if (writingItem.isEmpty() && getPaperCount() > 0) {
      ItemStack page = Page.createPage();
      setWritingItem(page);
      consumePaper();
      writingItem = page;
    }

    if (writingItem.isEmpty()) return false;

    if (writingItem.getItem() instanceof PageItem && Page.isBlank(writingItem)) {
      // Page NBT applies to the whole ItemStack. Refuse malformed/legacy
      // stacked input rather than turning every page in the stack into a
      // symbol for the price of one page.
      if (writingItem.getCount() != 1) return false;
      Page.setSymbol(writingItem, symbol);
      useInk();
      markForUpdate();
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
        art.arcane.mystcraft.advancements.ModAdvancements.triggerWritingDeskWrite(serverPlayer);
      }
      return true;
    }

    if (writingItem.getItem() instanceof AgebookItem agebookItem && getPaperCount() > 0) {
      ItemStack page = Page.createSymbolPage(symbol);
      agebookItem.addPages(writingItem, java.util.Collections.singletonList(page));
      useInk();
      consumePaper();
      markForUpdate();
      if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
        art.arcane.mystcraft.advancements.ModAdvancements.triggerWritingDeskWrite(serverPlayer);
      }
      return true;
    }

    return false;
  }

  /**
   * Experimental writing — rolls a random symbol from {@link SymbolRegistry}
   * using the desk ink tank's accumulated {@link InkBlend} affinity. When the
   * tank carries no affinity this falls back to the plain weighted roll, so the
   * workflow stays useful on vanilla ink. Returns the symbol that was written,
   * or {@code null} if the desk could not write (no ink / no slot).
   */
  @Nullable
  public ResourceLocation writeSymbolExperimental(@Nullable Player player) {
    if (level == null || level.isClientSide) return null;
    if (!hasEnoughInk()) return null;

    InkBlend blend = inkTank.getBlend();
    IAgeSymbol symbol = blend != null && !blend.isEmpty()
        ? SymbolRegistry.getRandomWeightedWithAffinity(level.random, blend)
        : SymbolRegistry.getRandomWeighted(level.random);
    if (symbol == null) return null;

    ResourceLocation id = symbol.getRegistryName();
    return writeSymbol(player, id) ? id : null;
  }

  /**
   * Processes fluid containers - called from tick.
   */
  public void processFluidContainers() {
    if (level == null || level.isClientSide) return;

    ItemStack containerIn = mainInventory.getItem(SLOT_CONTAINER_IN);
    ItemStack containerOut = mainInventory.getItem(SLOT_CONTAINER_OUT);

    if (containerIn.isEmpty()) return;

    if (containerIn.getItem() instanceof InkVialItem vial) {
      int vialInk = vial.getInkAmount(containerIn);
      ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);

      // Empty vials still need to finish their container transaction after a
      // player frees the output slot.
      if (vialInk <= 0) {
        if (canAcceptContainerOutput(containerOut, emptyBottle)) {
          containerIn.shrink(1);
          addContainerOutput(containerOut, emptyBottle);
          mainInventory.setChanged();
        }
        return;
      }

      // One vial ink unit is exactly 10 mB. Round down to tank capacity so a
      // nearly-full tank never consumes ink it cannot store.
      int availableUnits = (INK_CAPACITY - getInkAmount()) / 10;
      int vialUnitsUsed = Math.min(vialInk, availableUnits);
      if (vialUnitsUsed <= 0) {
        return;
      }

      boolean drainsVial = vialUnitsUsed == vialInk;
      // A partially drained vial needs distinct NBT, so it cannot remain in a
      // stack with untouched vials. Wait for enough tank space instead.
      if (containerIn.getCount() > 1 && !drainsVial) {
        return;
      }
      if (drainsVial && !canAcceptContainerOutput(containerOut, emptyBottle)) {
        return;
      }

      inkTank.fill(vialUnitsUsed * 10);
      if (drainsVial) {
        containerIn.shrink(1);
        addContainerOutput(containerOut, emptyBottle);
      } else {
        vial.setInkAmount(containerIn, vialInk - vialUnitsUsed);
      }
      mainInventory.setChanged();
      return;
    }

    if (containerIn.is(ModTags.Items.INK_BUCKETS)) {
      int spaceInTank = INK_CAPACITY - getInkAmount();
      if (spaceInTank >= 1000) {

        ItemStack emptyContainer;
        if (containerIn.getItem().hasCraftingRemainingItem()) {
          emptyContainer = new ItemStack(containerIn.getItem().getCraftingRemainingItem());
        } else {
          emptyContainer = new ItemStack(Items.BUCKET);
        }

        if (containerOut.isEmpty()) {
          inkTank.fill(1000);
          mainInventory.setItem(SLOT_CONTAINER_OUT, emptyContainer);
          containerIn.shrink(1);
          mainInventory.setChanged();
        } else if (ItemStackNbt.isSameItemSameTags(containerOut, emptyContainer) &&
            containerOut.getCount() < containerOut.getMaxStackSize()) {
          inkTank.fill(1000);
          containerOut.grow(1);
          containerIn.shrink(1);
          mainInventory.setChanged();
        }
      }
    }
  }

  private static boolean canAcceptContainerOutput(ItemStack currentOutput, ItemStack output) {
    return currentOutput.isEmpty()
        || (ItemStackNbt.isSameItemSameTags(currentOutput, output)
        && currentOutput.getCount() < currentOutput.getMaxStackSize());
  }

  private void addContainerOutput(ItemStack currentOutput, ItemStack output) {
    if (currentOutput.isEmpty()) {
      mainInventory.setItem(SLOT_CONTAINER_OUT, output);
    } else {
      currentOutput.grow(output.getCount());
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
    if (!(page.getItem() instanceof PageItem)) return page;

    ItemStack tabItem = getTabItem(tabIndex);
    if (tabItem.isEmpty()) return page;

    ItemStack remaining = page.copy();
    if (tabItem.getItem() instanceof FolderItem) {
      while (!remaining.isEmpty() && !FolderItem.isFull(tabItem)) {
        ItemStack singlePage = remaining.copy();
        singlePage.setCount(1);
        if (!FolderItem.addPage(tabItem, singlePage)) break;
        remaining.shrink(1);
      }
    } else if (tabItem.getItem() instanceof PortfolioItem) {
      while (!remaining.isEmpty() && !PortfolioItem.isFull(tabItem)) {
        ItemStack singlePage = remaining.copy();
        singlePage.setCount(1);
        if (!PortfolioItem.addPage(tabItem, singlePage)) break;
        remaining.shrink(1);
      }
    }

    if (remaining.getCount() != page.getCount()) {
      markForUpdate();
    }
    return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
  }

  /**
   * Gets all items for dropping when block is broken.
   */
  public List<ItemStack> getDrops() {
    List<ItemStack> drops = new ArrayList<>();

    for (int i = 0; i < mainInventory.getContainerSize(); i++) {
      ItemStack stack = mainInventory.getItem(i);
      if (!stack.isEmpty()) {
        drops.add(stack.copy());
      }
    }

    for (int i = 0; i < tabInventory.getContainerSize(); i++) {
      ItemStack stack = tabInventory.getItem(i);
      if (!stack.isEmpty()) {
        drops.add(stack.copy());
      }
    }

    return drops;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("container.mystcraft.writing_desk");
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
    return new WritingDeskMenu(containerId, playerInventory, this);
  }

  /**
   * Simple ink tank that tracks fluid amount without Forge fluid capabilities.
   * Optionally carries an {@link InkBlend} accumulated from themed ink sources
   * (e.g. ink mixed at an Ink Mixer with diamond / nether star / etc. items) so
   * {@link #writeSymbolExperimental} can roll affinity-biased symbols.
   */
  public static class InkTank {
    private static final String TAG_BLEND = "AffinityBlend";

    private final int capacity;
    private int amount;
    @Nullable
    private InkBlend blend;

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
      int filled = Math.min(Math.max(0, amount), capacity - this.amount);
      this.amount += filled;
      return filled;
    }

    public int drain(int amount) {
      int drained = Math.min(Math.max(0, amount), this.amount);
      this.amount -= drained;

      if (this.amount <= 0) {
        this.blend = null;
      }
      return drained;
    }

    /**
     * Read-only handle on the tank's affinity blend, or {@code null} if the
     * tank holds plain ink. Mutating the returned blend is permitted and will
     * persist across saves; callers that don't intend to mutate should treat it
     * as read-only.
     */
    @Nullable
    public InkBlend getBlend() {
      return blend;
    }

    /**
     * Merges {@code other} into the tank's affinity blend (creating it on first
     * call). Use this when transferring themed ink from an Ink Mixer or pouring
     * a themed ink container in.
     */
    public void applyAffinity(@Nullable InkBlend other) {
      if (other == null || other.isEmpty()) return;
      if (blend == null) blend = new InkBlend();
      blend.merge(other);
    }

    /**
     * Wipes the tank's affinity (called when the tank is fully drained).
     */
    public void clearAffinity() {
      blend = null;
    }

    public void load(CompoundTag tag) {
      this.amount = Math.max(0, Math.min(capacity, tag.getInt("InkAmount")));
      if (tag.contains(TAG_BLEND, Tag.TAG_COMPOUND)) {
        InkBlend loaded = InkBlend.fromTag(tag.getCompound(TAG_BLEND));
        this.blend = amount <= 0 || loaded.isEmpty() ? null : loaded;
      } else {
        this.blend = null;
      }
    }

    public CompoundTag save(CompoundTag tag) {
      tag.putInt("InkAmount", amount);
      if (blend != null && !blend.isEmpty()) {
        tag.put(TAG_BLEND, blend.toNbt());
      }
      return tag;
    }
  }
}
