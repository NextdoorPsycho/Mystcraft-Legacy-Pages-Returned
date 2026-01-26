package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Book Receptacle.
 * Holds a descriptive book or linkbook and creates a portal when attached to a crystal.
 * Only accepts items that can activate portals (books with link data).
 */
public class BookReceptacleBlockEntity extends MystcraftBlockEntity {

    private static final String TAG_INVENTORY = "inventory";

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isValidPortalActivator(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            handleBookChange();
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    public BookReceptacleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BOOK_RECEPTACLE.get(), pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        tag.put(TAG_INVENTORY, inventory.serializeNBT());
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        inventory.deserializeNBT(tag.getCompound(TAG_INVENTORY));
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
     * Checks if a stack can activate a portal.
     * Valid items are Linkbooks and Agebooks with link data.
     */
    public static boolean isValidPortalActivator(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // Accept linkbooks and agebooks
        if (stack.getItem() instanceof LinkbookItem || stack.getItem() instanceof AgebookItem) {
            return true;
        }
        return false;
    }

    /**
     * Gets the book in this receptacle.
     */
    @NotNull
    public ItemStack getBook() {
        return inventory.getStackInSlot(0);
    }

    /**
     * Sets the book in this receptacle.
     */
    public void setBook(@NotNull ItemStack book) {
        inventory.setStackInSlot(0, book);
    }

    /**
     * Checks if there's a book in this receptacle.
     */
    public boolean hasBook() {
        return !inventory.getStackInSlot(0).isEmpty();
    }

    /**
     * Called when the book changes.
     * Handles portal activation/deactivation.
     */
    private void handleBookChange() {
        if (level == null || level.isClientSide) {
            return;
        }

        markForUpdate();

        ItemStack book = getBook();
        if (!book.isEmpty() && isValidPortalActivator(book)) {
            // TODO: Activate portal via PortalUtils
            // PortalUtils.firePortal(level, worldPosition);
        } else {
            // TODO: Deactivate portal via PortalUtils
            // PortalUtils.shutdownPortal(level, worldPosition);
        }
    }

    /**
     * Gets the portal color from the book.
     */
    public int getPortalColor() {
        ItemStack book = getBook();
        if (book.isEmpty()) {
            return 0xFFFFFF; // White default
        }

        // TODO: Get color from book data
        // For now return a default mystcraft blue
        return 0x4444FF;
    }

    /**
     * Drops the book when the block is broken.
     */
    public void dropContents() {
        if (level != null && !level.isClientSide) {
            ItemStack book = getBook();
            if (!book.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), book);
                setBook(ItemStack.EMPTY);
            }
        }
    }

    /**
     * Calculates redstone signal based on book presence.
     */
    public int getAnalogOutputSignal() {
        return hasBook() ? 15 : 0;
    }
}
