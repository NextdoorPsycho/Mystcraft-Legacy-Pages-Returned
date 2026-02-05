package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Block entity for the Book Receptacle.
 * Holds a descriptive book or linkbook and creates a portal when attached to a crystal.
 * Only accepts items that can activate portals (books with link data).
 */
public class BookReceptacleBlockEntity extends MystcraftBlockEntity {

  private static final String TAG_INVENTORY = "inventory";
  private boolean loading = false;

  public BookReceptacleBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.BOOK_RECEPTACLE.get(), pos, blockState);
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
    return stack.getItem() instanceof LinkbookItem || stack.getItem() instanceof AgebookItem;
  }  private final SimpleContainer inventory = new SimpleContainer(1) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      return isValidPortalActivator(stack);
    }

    @Override
    public int getMaxStackSize() {
      return 1;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      BookReceptacleBlockEntity.this.setChanged();
      handleBookChange();
    }
  };

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
    tag.put(TAG_INVENTORY, itemList);
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    loading = true;
    super.readNbt(tag);
    inventory.clearContent();
    ListTag itemList = tag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
    for (int i = 0; i < itemList.size(); i++) {
      CompoundTag itemTag = itemList.getCompound(i);
      int slot = itemTag.getInt("Slot");
      if (slot >= 0 && slot < inventory.getContainerSize()) {
        CompoundTag itemData = itemTag.contains("Item", Tag.TAG_COMPOUND) ? itemTag.getCompound("Item") : itemTag;
        inventory.setItem(slot, ItemStackNbt.load(itemData));
      }
    }
    loading = false;
  }

  /**
   * Gets the book in this receptacle.
   */
  @NotNull
  public ItemStack getBook() {
    return inventory.getItem(0);
  }

  /**
   * Sets the book in this receptacle.
   */
  public void setBook(@NotNull ItemStack book) {
    inventory.setItem(0, book);
  }

  /**
   * Checks if there's a book in this receptacle.
   */
  public boolean hasBook() {
    return !inventory.getItem(0).isEmpty();
  }

  /**
   * Called when the book changes.
   * Handles portal activation/deactivation.
   */
  private void handleBookChange() {
    if (level == null || level.isClientSide || loading) {
      return;
    }

    markForUpdate();

    ItemStack book = getBook();
    if (!book.isEmpty() && isValidPortalActivator(book)) {
      // Activate portal
      PortalUtils.firePortal(level, worldPosition);
    } else {
      // Deactivate portal
      PortalUtils.shutdownPortal(level, worldPosition);
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

    // Get color from book's link data
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag != null) {
      Integer color = LinkOptions.getLinkColor(tag);
      if (color != null) {
        return color;
      }
    }

    // Default Mystcraft blue if no custom color
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
