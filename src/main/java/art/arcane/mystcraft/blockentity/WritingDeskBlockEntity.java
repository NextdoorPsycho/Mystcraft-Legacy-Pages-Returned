package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.InkVialItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Block entity for the Writing Desk.
 * Used for writing symbols onto pages using ink.
 *
 * Main inventory slots:
 * 0 - Writing slot (book/page being written to)
 * 1 - Paper slot (paper supply)
 * 2 - Container in (ink bucket input)
 * 3 - Container out (empty bucket output)
 *
 * Tab inventory:
 * 25 slots for notebooks, folders, portfolios
 */
public class WritingDeskBlockEntity extends MystcraftBlockEntity implements MenuProvider {

    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_TABS = "tabs";
    private static final String TAG_INK = "ink";

    public static final int SLOT_WRITING = 0;
    public static final int SLOT_PAPER = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int MAIN_SLOT_COUNT = 4;

    public static final int TAB_SLOT_COUNT = 25;

    /** Ink cost per symbol written (in mB) */
    public static final int INK_COST = 100;

    /** Ink tank capacity (1 bucket = 1000 mB) */
    public static final int INK_CAPACITY = 1000;

    private final ItemStackHandler mainInventory = new ItemStackHandler(MAIN_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_WRITING -> isWritableItem(stack);
                case SLOT_PAPER -> stack.is(Items.PAPER);
                case SLOT_CONTAINER_IN -> isInkContainer(stack);
                case SLOT_CONTAINER_OUT -> false; // Output only
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_WRITING ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markForUpdate();
        }
    };

    private final ItemStackHandler tabInventory = new ItemStackHandler(TAB_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isPageCollectionItem(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markForUpdate();
        }
    };

    private final FluidTank inkTank = new FluidTank(INK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isValidInk(stack.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            markForUpdate();
        }
    };

    private final LazyOptional<IItemHandler> mainItemHandler = LazyOptional.of(() -> mainInventory);
    private final LazyOptional<IItemHandler> tabItemHandler = LazyOptional.of(() -> tabInventory);
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> inkTank);

    public WritingDeskBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WRITING_DESK.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        tag.put(TAG_INVENTORY, mainInventory.serializeNBT());
        tag.put(TAG_TABS, tabInventory.serializeNBT());
        tag.put(TAG_INK, inkTank.writeToNBT(new CompoundTag()));
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        mainInventory.deserializeNBT(tag.getCompound(TAG_INVENTORY));
        tabInventory.deserializeNBT(tag.getCompound(TAG_TABS));
        inkTank.readFromNBT(tag.getCompound(TAG_INK));
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mainItemHandler.invalidate();
        tabItemHandler.invalidate();
        fluidHandler.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            // Top side accesses tabs, others access main inventory
            if (side == Direction.UP) {
                return tabItemHandler.cast();
            }
            return mainItemHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
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
        if (stack.getItem() instanceof LinkbookItem) {
            return true;
        }
        return false;
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
        // Accept fluid containers with ink
        FluidStack fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        return !fluid.isEmpty() && isValidInk(fluid.getFluid());
    }

    /**
     * Gets the item in the writing slot.
     */
    @NotNull
    public ItemStack getWritingItem() {
        return mainInventory.getStackInSlot(SLOT_WRITING);
    }

    /**
     * Sets the item in the writing slot.
     */
    public void setWritingItem(@NotNull ItemStack stack) {
        mainInventory.setStackInSlot(SLOT_WRITING, stack);
    }

    /**
     * Gets the paper count available.
     */
    public int getPaperCount() {
        ItemStack paper = mainInventory.getStackInSlot(SLOT_PAPER);
        return paper.isEmpty() ? 0 : paper.getCount();
    }

    /**
     * Consumes one piece of paper.
     */
    public boolean consumePaper() {
        ItemStack paper = mainInventory.getStackInSlot(SLOT_PAPER);
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
        return tabInventory.getStackInSlot(index);
    }

    /**
     * Gets the current ink amount in mB.
     */
    public int getInkAmount() {
        FluidStack fluid = inkTank.getFluid();
        return fluid.isEmpty() ? 0 : fluid.getAmount();
    }

    /**
     * Gets the ink tank for rendering.
     */
    public FluidTank getInkTank() {
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
        inkTank.drain(INK_COST, IFluidHandler.FluidAction.EXECUTE);
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
            return true;
        }

        // Write to an agebook (creates a new page and adds it)
        if (writingItem.getItem() instanceof AgebookItem agebookItem && getPaperCount() > 0) {
            ItemStack page = Page.createSymbolPage(symbol);
            agebookItem.addPages(writingItem, java.util.Collections.singletonList(page));
            useInk();
            consumePaper();
            markForUpdate();
            return true;
        }

        return false;
    }

    /**
     * Processes fluid containers - called from tick.
     */
    public void processFluidContainers() {
        if (level == null || level.isClientSide) return;

        ItemStack containerIn = mainInventory.getStackInSlot(SLOT_CONTAINER_IN);
        ItemStack containerOut = mainInventory.getStackInSlot(SLOT_CONTAINER_OUT);

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

                inkTank.fill(new FluidStack(ModFluids.BLACK_INK_SOURCE.get(), inkToTransfer), IFluidHandler.FluidAction.EXECUTE);
                vial.setInkAmount(containerIn, vialInk - vialUnitsUsed);

                // If vial is empty, output empty glass bottle
                if (vial.getInkAmount(containerIn) <= 0) {
                    ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (containerOut.isEmpty()) {
                        mainInventory.setStackInSlot(SLOT_CONTAINER_OUT, emptyBottle);
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

        // Try to drain fluid from container into tank
        FluidUtil.getFluidHandler(containerIn).ifPresent(handler -> {
            FluidStack contained = handler.getFluidInTank(0);
            if (!contained.isEmpty() && isValidInk(contained.getFluid())) {
                int fillAmount = inkTank.fill(contained, IFluidHandler.FluidAction.SIMULATE);
                if (fillAmount > 0) {
                    FluidStack drained = handler.drain(fillAmount, IFluidHandler.FluidAction.SIMULATE);
                    if (!drained.isEmpty()) {
                        // Check if we can output the empty container
                        ItemStack emptied = containerIn.copy();
                        emptied.setCount(1);

                        // Perform the actual drain
                        FluidStack actualDrained = handler.drain(fillAmount, IFluidHandler.FluidAction.EXECUTE);
                        inkTank.fill(actualDrained, IFluidHandler.FluidAction.EXECUTE);

                        // Handle container result
                        ItemStack result = containerIn.getItem().getCraftingRemainingItem(containerIn);
                        if (!result.isEmpty()) {
                            if (containerOut.isEmpty()) {
                                mainInventory.setStackInSlot(SLOT_CONTAINER_OUT, result);
                            } else if (ItemStack.isSameItemSameTags(containerOut, result) &&
                                       containerOut.getCount() < containerOut.getMaxStackSize()) {
                                containerOut.grow(1);
                            }
                        }
                        containerIn.shrink(1);
                    }
                }
            }
        });
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
        for (int i = 0; i < mainInventory.getSlots(); i++) {
            ItemStack stack = mainInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }

        // Tab inventory
        for (int i = 0; i < tabInventory.getSlots(); i++) {
            ItemStack stack = tabInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }

        return drops;
    }

    /**
     * Gets the main item handler for GUI access.
     */
    public ItemStackHandler getMainInventory() {
        return mainInventory;
    }

    /**
     * Gets the tab item handler for GUI access.
     */
    public ItemStackHandler getTabInventory() {
        return tabInventory;
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
}
