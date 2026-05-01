package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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

  private boolean loading = false;

  public BookReceptacleBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.BOOK_RECEPTACLE.get(), pos, blockState);
  }

  /**
   * @return {@code true} when the stack is a book that can light the portal.
   *
   * <p>Accepted:
   * <ul>
   *   <li>Any {@link LinkbookItem} (including {@link art.arcane.mystcraft.item.PersonalLinkBookItem},
   *       which is a subclass).</li>
   *   <li>A linked {@link AgebookItem} (one with a {@code DimensionUID}).</li>
   *   <li>An unlinked {@link AgebookItem} whose page&nbsp;0 is a link panel — this is the
   *       "auto-create-on-traverse" path; the Age is generated when the player walks
   *       through the portal. See {@link AgebookItem#isNewAgebook(ItemStack)}.</li>
   * </ul>
   *
   * <p>Rejected: Agebooks that lack both a {@code DimensionUID} and a page-0 link panel
   * (typically a freshly-crafted but never-written stack, which would silently no-op
   * on portal traversal).
   */
  public static boolean isValidPortalActivator(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    if (stack.getItem() instanceof LinkbookItem) {

      return true;
    }
    if (stack.getItem() instanceof AgebookItem) {
      CompoundTag tag = ItemStackNbt.getTag(stack);
      if (tag != null && LinkOptions.getDimensionUID(tag) != null) {

        return true;
      }

      return AgebookItem.isNewAgebook(stack);
    }
    return false;
  }

  /**
   * Container view used by hopper integration etc. — never mutate this directly
   * unless you also want the portal to fire/shutdown automatically.
   */
  public Container getInventory() {
    return inventory;
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

  /**
   * @return the stored book, or {@link ItemStack#EMPTY}. Never {@code null}.
   */
  @NotNull
  public ItemStack getBook() {
    return inventory.getItem(0);
  }

  /**
   * Replace the stored book. Pass {@link ItemStack#EMPTY} to clear it. Triggers
   * portal fire/shutdown side-effects automatically.
   * <p>
   * Initialises a fresh Linkbook's link data on insertion when none exists —
   * so a book that was crafted and dropped straight into a receptacle (never
   * sitting in player inventory, never receiving an {@code inventoryTick})
   * still has a usable Spawn/DimensionUID by the time {@code firePortal} runs.
   * Without this, the portal would light but {@code entityInside} would
   * silently drop the teleport because {@code performLink} aborts on missing
   * link data.
   */
  public void setBook(@NotNull ItemStack book) {
    ItemStack prepared = book;
    if (level != null && !level.isClientSide && book.getItem() instanceof LinkbookItem linkbook) {
      prepared = book.copy();
      linkbook.validate(level, prepared, null);
    }
    inventory.setItem(0, prepared);
  }

  /**
   * @return {@code true} when a book is present.
   */
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

    handleBookChange();
    setChanged();
    return book;
  }

  private void handleBookChange() {
    if (level == null || level.isClientSide || loading) {
      return;
    }
    markForUpdate();

    ItemStack book = getBook();
    if (isValidPortalActivator(book)) {
      art.arcane.mystcraft.Mystcraft.LOGGER.info(
          "[Receptacle] Book inserted at {}: {} — calling firePortal",
          worldPosition,
          BuiltInRegistries.ITEM.getKey(book.getItem()));
      PortalUtils.firePortal(level, worldPosition);
    } else {
      art.arcane.mystcraft.Mystcraft.LOGGER.info(
          "[Receptacle] Book removed at {} — calling shutdownPortal",
          worldPosition);
      PortalUtils.shutdownPortal(level, worldPosition);
    }
  }

  /**
   * Tint colour for the connected portal blocks. Branches on the stored book's
   * type so each book reads at a glance:
   *
   * <ul>
   *   <li>Personal Link Book → fixed violet ({@code 0xAA44FF}).</li>
   *   <li>Linked Agebook → the Age's sky colour from {@link ClientAgeDataCache}
   *       (or a soft blue if the data hasn't synced yet).</li>
   *   <li>Unwritten Agebook (page-0 link panel, no UID) → muted grey
   *       ({@code 0x808890}) — telegraphs the auto-create-on-traverse path.</li>
   *   <li>Other Linkbook → the book's stored {@code LinkColor}, or default
   *       Mystcraft blue ({@code 0x4488FF}).</li>
   *   <li>Empty receptacle → white default; rarely reached because the portal
   *       is shut down whenever the book is removed.</li>
   * </ul>
   *
   * <p>This method is safe to call from the server (the {@link ClientAgeDataCache}
   * lookup just returns -1 and we fall back to the default blue), but its
   * primary caller is the client-side {@code BlockColor} provider on
   * {@code LINK_PORTAL}.
   */
  public int getPortalColor() {
    ItemStack book = getBook();
    if (book.isEmpty()) {
      return 0xFFFFFF;
    }

    if (book.getItem() instanceof PersonalLinkBookItem) {
      return 0xAA44FF;
    }

    if (book.getItem() instanceof AgebookItem) {
      CompoundTag tag = ItemStackNbt.getTag(book);
      Integer ageUID = tag != null ? LinkOptions.getDimensionUID(tag) : null;
      if (ageUID == null) {
        return 0x808890;
      }
      int sky = ClientAgeDataCache.getSkyColor(ageUID);
      return sky != -1 ? sky : 0x66AAFF;
    }

    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag != null) {
      Integer color = LinkOptions.getLinkColor(tag);
      if (color != null) {
        return color;
      }
    }
    return 0x4488FF;
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

  /**
   * Comparator output: full when a book is present, empty otherwise.
   */
  public int getAnalogOutputSignal() {
    return hasBook() ? 15 : 0;
  }

}
