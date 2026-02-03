package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Bookstand.
 * Holds a linkbook or agebook for display and use.
 * Supports rotation with 45-degree yaw snapping.
 * <p>
 * 1.18.2 version - identical to common, uses local imports.
 */
public class BookstandBlockEntity extends MystcraftBlockEntity implements IRotateableBlockEntity {

  /**
   * The yaw snap increment for bookstands (45 degrees).
   */
  protected static final int YAW_SNAP = 45;
  /**
   * The pitch snap increment for bookstands (15 degrees).
   */
  protected static final int PITCH_SNAP = 15;
  private static final String TAG_INVENTORY = "inventory";
  private static final String TAG_YAW = "Yaw";
  private static final String TAG_PITCH = "Pitch";
  protected final SimpleContainer inventory = new SimpleContainer(1) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      return isValidBook(stack);
    }

    @Override
    public int getMaxStackSize() {
      return 1;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      BookstandBlockEntity.this.setChanged();
      BookstandBlockEntity.this.markForUpdate();
    }
  };
  private short yaw = 0;
  private short pitch = 0;

  public BookstandBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.BOOKSTAND.get(), pos, blockState);
  }

  protected BookstandBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
    super(type, pos, blockState);
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
    tag.put(TAG_INVENTORY, itemList);
    tag.putShort(TAG_YAW, yaw);
    tag.putShort(TAG_PITCH, pitch);
  }

  // --- IRotateableBlockEntity Implementation ---

  @Override
  protected void readNbt(CompoundTag tag) {
    super.readNbt(tag);
    inventory.clearContent();
    ListTag itemList = tag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
    for (int i = 0; i < itemList.size(); i++) {
      CompoundTag itemTag = itemList.getCompound(i);
      int slot = itemTag.getInt("Slot");
      if (slot >= 0 && slot < inventory.getContainerSize()) {
        inventory.setItem(slot, ItemStack.of(itemTag));
      }
    }
    // Support alternate "Rotation" key format
    if (tag.contains("Rotation")) {
      setYaw(tag.getInt("Rotation") + 270);
    } else if (tag.contains(TAG_YAW)) {
      this.yaw = tag.getShort(TAG_YAW);
    }
    if (tag.contains(TAG_PITCH)) {
      this.pitch = tag.getShort(TAG_PITCH);
    }
  }

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

  // Render bounds are extended by platform-specific block entity renderers

  /**
   * Activates the link from the book on the stand.
   * Called when a player right-clicks the stand.
   */
  public void activateLink(Entity entity) {
    ItemStack book = getBook();
    if (book.isEmpty()) {
    }
    // TODO: Implement linking when LinkbookItem/AgebookItem have activateLink methods
    // For now, this is handled by the block's use() method opening the book GUI
  }

  /**
   * Gets the book currently on the stand.
   */
  @NotNull
  public ItemStack getBook() {
    return inventory.getItem(0);
  }

  /**
   * Sets the book on the stand.
   */
  public void setBook(@NotNull ItemStack book) {
    // Eject current book if not empty
    ItemStack current = inventory.getItem(0);
    if (!current.isEmpty() && !book.isEmpty() && level != null && !level.isClientSide) {
      Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), current);
    }
    inventory.setItem(0, book);
  }

  /**
   * Checks if there's a book on the stand.
   */
  public boolean hasBook() {
    return !inventory.getItem(0).isEmpty();
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
