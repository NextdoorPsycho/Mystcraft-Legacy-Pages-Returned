package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Block entity for the Ink Mixer.
 * Handles mixing of inks and dyes to create link panels with properties.
 *
 * Slots:
 * 0 - Ink input (fluid containers)
 * 1 - Paper input
 * 2 - Empty container output
 */
public class InkMixerBlockEntity extends MystcraftBlockEntity implements MenuProvider {

    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_HAS_INK = "ink";
    private static final String TAG_PROBABILITIES = "probabilities";
    private static final String TAG_SEED = "seed";

    public static final int SLOT_INK_IN = 0;
    public static final int SLOT_PAPER = 1;
    public static final int SLOT_INK_OUT = 2;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_INK_IN) {
                // Accept fluid containers with valid ink
                return isValidInkContainer(stack);
            }
            if (slot == SLOT_PAPER) {
                return stack.is(Items.PAPER);
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markForUpdate();
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    private boolean hasInk = false;
    private Map<String, Float> inkProbabilities = new HashMap<>();
    private long nextSeed;

    public InkMixerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INK_MIXER.get(), pos, blockState);
        this.nextSeed = new Random().nextLong();
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        tag.put(TAG_INVENTORY, inventory.serializeNBT());
        tag.putBoolean(TAG_HAS_INK, hasInk);
        tag.putLong(TAG_SEED, nextSeed);

        // Save probabilities
        CompoundTag probs = new CompoundTag();
        for (Map.Entry<String, Float> entry : inkProbabilities.entrySet()) {
            probs.putFloat(entry.getKey(), entry.getValue());
        }
        tag.put(TAG_PROBABILITIES, probs);
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        inventory.deserializeNBT(tag.getCompound(TAG_INVENTORY));
        hasInk = tag.getBoolean(TAG_HAS_INK);
        nextSeed = tag.getLong(TAG_SEED);

        // Load probabilities
        inkProbabilities.clear();
        CompoundTag probs = tag.getCompound(TAG_PROBABILITIES);
        for (String key : probs.getAllKeys()) {
            inkProbabilities.put(key, probs.getFloat(key));
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    /**
     * Called every tick to process ink from containers.
     */
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!hasInk && !inventory.getStackInSlot(SLOT_INK_IN).isEmpty()) {
            tryFillFromContainer();
        }
    }

    /**
     * Tries to fill the basin from the ink container.
     */
    private void tryFillFromContainer() {
        ItemStack container = inventory.getStackInSlot(SLOT_INK_IN);
        if (container.isEmpty()) {
            return;
        }

        FluidStack fluid = FluidUtil.getFluidContained(container).orElse(FluidStack.EMPTY);
        if (fluid.isEmpty()) {
            return;
        }

        // Check if it's valid ink (black ink or any other valid ink)
        if (!isValidInk(fluid)) {
            return;
        }

        // Get the empty container
        ItemStack emptyContainer = container.getCraftingRemainingItem();
        if (emptyContainer.isEmpty()) {
            emptyContainer = new ItemStack(Items.BUCKET);
        }

        // Check if we can put the empty container in the output
        ItemStack currentOutput = inventory.getStackInSlot(SLOT_INK_OUT);
        if (!currentOutput.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(currentOutput, emptyContainer)) {
                return;
            }
            if (currentOutput.getCount() >= currentOutput.getMaxStackSize()) {
                return;
            }
        }

        // Fill the basin
        hasInk = true;
        container.shrink(1);
        if (container.isEmpty()) {
            inventory.setStackInSlot(SLOT_INK_IN, ItemStack.EMPTY);
        }

        // Add empty container to output
        if (currentOutput.isEmpty()) {
            inventory.setStackInSlot(SLOT_INK_OUT, emptyContainer);
        } else {
            currentOutput.grow(1);
        }

        setChanged();
        markForUpdate();
    }

    /**
     * Checks if a fluid is valid ink.
     */
    private boolean isValidInk(FluidStack fluid) {
        return fluid.getFluid() == ModFluids.BLACK_INK_SOURCE.get() ||
               fluid.getFluid() == ModFluids.BLACK_INK_FLOWING.get();
    }

    /**
     * Checks if a stack is a valid ink container.
     */
    private boolean isValidInkContainer(ItemStack stack) {
        FluidStack fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        return !fluid.isEmpty() && isValidInk(fluid);
    }

    /**
     * Checks if an item can be crafted.
     */
    public boolean canBuildItem() {
        ItemStack paper = inventory.getStackInSlot(SLOT_PAPER);
        return !paper.isEmpty() && paper.is(Items.PAPER) && hasInk;
    }

    /**
     * Gets the item that would be crafted.
     */
    @NotNull
    public ItemStack getCraftedItem() {
        if (!canBuildItem()) {
            return ItemStack.EMPTY;
        }
        return Page.createLinkPage();
    }

    /**
     * Builds the link panel and gives it to the player.
     */
    public void buildItem(@NotNull ItemStack output, @NotNull Player player) {
        if (!canBuildItem()) {
            return;
        }

        // Apply ink probabilities to the link panel
        Random rand = new Random(nextSeed);
        for (Map.Entry<String, Float> entry : inkProbabilities.entrySet()) {
            String property = entry.getKey();
            float probability = entry.getValue();
            if (rand.nextFloat() < probability) {
                Page.addLinkProperty(output, property);
            }
        }

        // Reset state
        nextSeed = rand.nextLong();
        hasInk = false;
        inkProbabilities.clear();

        // Consume paper
        inventory.getStackInSlot(SLOT_PAPER).shrink(1);

        setChanged();
        markForUpdate();
    }

    /**
     * Adds items to modify ink properties.
     */
    @NotNull
    public ItemStack addItems(@NotNull ItemStack stack, int amount) {
        // TODO: Implement ink effects registry for items (dyes, etc.)
        // For now, this is a stub that accepts items but doesn't modify probabilities
        return stack;
    }

    /**
     * Gets whether the mixer has ink.
     */
    public boolean hasInk() {
        return hasInk;
    }

    /**
     * Sets whether the mixer has ink.
     */
    public void setHasInk(boolean hasInk) {
        this.hasInk = hasInk;
        setChanged();
    }

    /**
     * Gets the current ink probabilities.
     */
    public Map<String, Float> getInkProbabilities() {
        return new HashMap<>(inkProbabilities);
    }

    /**
     * Gets drops for when the block is broken.
     */
    public List<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    // MenuProvider implementation

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.translatable("container.mystcraft.ink_mixer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new InkMixerMenu(containerId, playerInventory, this);
    }
}
