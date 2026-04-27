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
 * <p>
 * Holds a single Linkbook or Agebook. Mutating the stored book fires
 * {@link PortalUtils#firePortal(net.minecraft.world.level.Level, BlockPos)} or
 * {@link PortalUtils#shutdownPortal(net.minecraft.world.level.Level, BlockPos)}
 * automatically; callers should never need to touch the portal API directly.
 */
public class BookReceptacleBlockEntity extends MystcraftBlockEntity {

  private static final String TAG_INVENTORY = "inventory";

  /** True while loading from NBT — suppresses the portal-state side-effect. */
  private boolean loading = false;

  public BookReceptacleBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.BOOK_RECEPTACLE.get(), pos, blockState);
  }

  /**
   * @return {@code true} when the stack is a Linkbook or Agebook (the only
   *         items that can fire a portal).
   */
  public static boolean isValidPortalActivator(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    return stack.getItem() instanceof LinkbookItem || stack.getItem() instanceof AgebookItem;
  }

  private final SimpleContainer inventory = new SimpleContainer(1) {
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
   * Container view used by hopper integration etc. — never mutate this directly
   * unless you also want the portal to fire/shutdown automatically.
   */
  public Container getInventory() {
    return inventory;
  }

  @Override
  protected void writeNbt(CompoundTag tag) {
    super.writeNbt(tag);
    ListTag itemList = new ListTag();
    ItemStack book = inventory.getItem(0);
    if (!book.isEmpty()) {
      CompoundTag itemTag = new CompoundTag();
      itemTag.putInt("Slot", 0);
      itemTag.merge(ItemStackNbt.save(book));
      itemList.add(itemTag);
    }
    tag.put(TAG_INVENTORY, itemList);
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    loading = true;
    try {
      super.readNbt(tag);
      inventory.clearContent();
      ListTag itemList = tag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
      for (int i = 0; i < itemList.size(); i++) {
        CompoundTag itemTag = itemList.getCompound(i);
        int slot = itemTag.getInt("Slot");
        if (slot == 0) {
          CompoundTag itemData = itemTag.contains("Item", Tag.TAG_COMPOUND) ? itemTag.getCompound("Item") : itemTag;
          inventory.setItem(0, ItemStackNbt.load(itemData));
        }
      }
    } finally {
      loading = false;
    }
  }

  /** @return the stored book, or {@link ItemStack#EMPTY}. Never {@code null}. */
  @NotNull
  public ItemStack getBook() {
    return inventory.getItem(0);
  }

  /**
   * Replace the stored book. Pass {@link ItemStack#EMPTY} to clear it. Triggers
   * portal fire/shutdown side-effects automatically.
   */
  public void setBook(@NotNull ItemStack book) {
    inventory.setItem(0, book);
  }

  /** @return {@code true} when a book is present. */
  public boolean hasBook() {
    return !inventory.getItem(0).isEmpty();
  }

  /**
   * Atomically remove the book and return it. Returns {@link ItemStack#EMPTY}
   * if there was nothing to take. Triggers portal shutdown.
   */
  @NotNull
  public ItemStack takeBook() {
    ItemStack book = inventory.removeItemNoUpdate(0);
    // Force the side-effect since removeItemNoUpdate skips setChanged().
    handleBookChange();
    setChanged();
    return book;
  }

  /**
   * Server-side hook fired whenever the inventory mutates. Drives portal
   * activation: presence of a valid book → fire; otherwise → shutdown.
   */
  private void handleBookChange() {
    if (level == null || level.isClientSide || loading) {
      return;
    }
    markForUpdate();

    ItemStack book = getBook();
    if (isValidPortalActivator(book)) {
      PortalUtils.firePortal(level, worldPosition);
    } else {
      PortalUtils.shutdownPortal(level, worldPosition);
    }
  }

  /**
   * Returns the tint colour for the connected portal blocks based on the link
   * data of the stored book. Falls back to Mystcraft blue when no colour is set.
   */
  public int getPortalColor() {
    ItemStack book = getBook();
    if (book.isEmpty()) {
      return 0xFFFFFF; // White default — should rarely render with no book
    }
    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag != null) {
      Integer color = LinkOptions.getLinkColor(tag);
      if (color != null) {
        return color;
      }
    }
    return 0x4444FF; // Mystcraft blue
  }

  /**
   * Drops the contained book at the receptacle position. Called from the
   * block's {@code onRemove} so the player gets their book back when breaking.
   */
  public void dropContents() {
    if (level == null || level.isClientSide) {
      return;
    }
    ItemStack book = getBook();
    if (!book.isEmpty()) {
      Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), book);
      setBook(ItemStack.EMPTY);
    }
  }

  /** Comparator output: full when a book is present, empty otherwise. */
  public int getAnalogOutputSignal() {
    return hasBook() ? 15 : 0;
  }
}
