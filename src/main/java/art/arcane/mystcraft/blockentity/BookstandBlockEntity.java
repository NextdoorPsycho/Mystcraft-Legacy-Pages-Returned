package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Bookstand.
 * Holds a linkbook or agebook for display and use.
 * Supports rotation with 45-degree yaw snapping.
 */
public class BookstandBlockEntity extends MystcraftBlockEntity implements IRotateableBlockEntity {

    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_YAW = "Yaw";
    private static final String TAG_PITCH = "Pitch";

    /**
     * The yaw snap increment for bookstands (45 degrees).
     */
    protected static final int YAW_SNAP = 45;

    /**
     * The pitch snap increment for bookstands (15 degrees).
     */
    protected static final int PITCH_SNAP = 15;

    private short yaw = 0;
    private short pitch = 0;

    protected final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isValidBook(stack);
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

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    public BookstandBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BOOKSTAND.get(), pos, blockState);
    }

    protected BookstandBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        tag.put(TAG_INVENTORY, inventory.serializeNBT());
        tag.putShort(TAG_YAW, yaw);
        tag.putShort(TAG_PITCH, pitch);
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        inventory.deserializeNBT(tag.getCompound(TAG_INVENTORY));
        // Support legacy "Rotation" key from old versions
        if (tag.contains("Rotation")) {
            setYaw(tag.getInt("Rotation") + 270);
        } else if (tag.contains(TAG_YAW)) {
            this.yaw = tag.getShort(TAG_YAW);
        }
        if (tag.contains(TAG_PITCH)) {
            this.pitch = tag.getShort(TAG_PITCH);
        }
    }

    // ========== IRotateableBlockEntity Implementation ==========

    @Override
    public short getYaw() {
        return yaw;
    }

    @Override
    public void setYaw(int yaw) {
        // Snap to 45-degree increments for bookstand
        yaw = snapYaw(yaw, getYawSnap());
        this.yaw = (short) (yaw % 360);
        setChanged();
        markForUpdate();
    }

    @Override
    public short getPitch() {
        return pitch;
    }

    @Override
    public void setPitch(int pitch) {
        // Snap to 15-degree increments
        pitch = snapPitch(pitch, getPitchSnap());
        this.pitch = (short) (pitch % 360);
        setChanged();
        markForUpdate();
    }

    /**
     * Gets the yaw snap increment. Override in subclasses for different values.
     */
    protected int getYawSnap() {
        return YAW_SNAP;
    }

    /**
     * Gets the pitch snap increment. Override in subclasses for different values.
     */
    protected int getPitchSnap() {
        return PITCH_SNAP;
    }

    /**
     * Activates the link from the book on the stand.
     * Called when a player right-clicks the stand.
     */
    public void activateLink(Entity entity) {
        ItemStack book = getBook();
        if (book.isEmpty()) {
            return;
        }
        // TODO: Implement linking when LinkbookItem/AgebookItem have activateLink methods
        // For now, this is handled by the block's use() method opening the book GUI
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Extend render bounds slightly for the book model
        return new AABB(worldPosition).inflate(0.5);
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
     * Checks if a stack is a valid book for this display.
     */
    public static boolean isValidBook(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof LinkbookItem || stack.getItem() instanceof AgebookItem;
    }

    /**
     * Gets the book currently on the stand.
     */
    @NotNull
    public ItemStack getBook() {
        return inventory.getStackInSlot(0);
    }

    /**
     * Sets the book on the stand.
     */
    public void setBook(@NotNull ItemStack book) {
        // Eject current book if not empty
        ItemStack current = inventory.getStackInSlot(0);
        if (!current.isEmpty() && !book.isEmpty() && level != null && !level.isClientSide) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), current);
        }
        inventory.setStackInSlot(0, book);
    }

    /**
     * Checks if there's a book on the stand.
     */
    public boolean hasBook() {
        return !inventory.getStackInSlot(0).isEmpty();
    }

    /**
     * Gets the display title of the book.
     */
    @Nullable
    public String getBookTitle() {
        ItemStack book = getBook();
        if (book.isEmpty() || book.getTag() == null) {
            return null;
        }
        return LinkOptions.getDisplayName(book.getTag());
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
