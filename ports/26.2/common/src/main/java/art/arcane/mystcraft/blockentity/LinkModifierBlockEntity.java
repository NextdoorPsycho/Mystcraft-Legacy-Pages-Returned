package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.util.NbtCompat;

import art.arcane.mystcraft.data.LinkFlags;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.world.AgeSeed;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Block entity for the Link Modifier. Used to add modifier pages (link
 * properties) to existing linkbooks.
 * <p>
 * Slots: 0 - Book slot (linkbook to modify) 1-4 - Modifier page slots
 */
public class LinkModifierBlockEntity extends MystcraftBlockEntity implements MenuProvider {

  public static final int SLOT_BOOK = 0;
  public static final int SLOT_MODIFIER_START = 1;
  public static final int SLOT_MODIFIER_END = 4;
  public static final int SLOT_COUNT = 5;
  private static final String TAG_INVENTORY = "inventory";
  private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT) {
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      if (slot == SLOT_BOOK) {
        return isModifiableBook(stack);
      }
      if (slot >= SLOT_MODIFIER_START && slot <= SLOT_MODIFIER_END) {
        return stack.getItem() instanceof PageItem && hasKnownLinkProperty(stack);
      }
      return false;
    }

    @Override
    public int getMaxStackSize() {
      return 64;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      LinkModifierBlockEntity.this.setChanged();
      LinkModifierBlockEntity.this.markForUpdate();
    }
  };

  public LinkModifierBlockEntity(BlockPos pos, BlockState blockState) {
    super(ModBlockEntities.LINK_MODIFIER.get(), pos, blockState);
  }

  private static boolean isModifiableBook(ItemStack stack) {
    if (stack.isEmpty() || stack.getItem() instanceof PersonalLinkBookItem) {
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
        itemTag.merge(ItemStackNbt.save(stack));
        itemList.add(itemTag);
      }
    }
    tag.put(TAG_INVENTORY, itemList);
  }

  @Override
  protected void readNbt(CompoundTag tag) {
    super.readNbt(tag);
    inventory.clearContent();
    ListTag itemList = tag.getListOrEmpty(TAG_INVENTORY);
    for (int i = 0; i < itemList.size(); i++) {
      CompoundTag itemTag = itemList.getCompoundOrEmpty(i);
      int slot = itemTag.getIntOr("Slot", 0);
      if (slot >= 0 && slot < inventory.getContainerSize()) {
        CompoundTag itemData = NbtCompat.contains(itemTag, "Item", Tag.TAG_COMPOUND) ? itemTag.getCompoundOrEmpty("Item") : itemTag;
        inventory.setItem(slot, ItemStackNbt.load(itemData));
      }
    }
  }

  /**
   * Gets the book in the modifier.
   */
  @NotNull
  public ItemStack getBook() {
    return inventory.getItem(SLOT_BOOK);
  }

  /**
   * Sets the book in the modifier.
   */
  public void setBook(@NotNull ItemStack book) {
    inventory.setItem(SLOT_BOOK, book);
  }

  /**
   * Gets a modifier page.
   */
  @NotNull
  public ItemStack getModifierPage(int index) {
    int slot = SLOT_MODIFIER_START + index;
    if (index < 0 || slot > SLOT_MODIFIER_END) {
      return ItemStack.EMPTY;
    }
    return inventory.getItem(slot);
  }

  /**
   * Checks if modification can be performed. Requires a book and at least one
   * modifier page.
   */
  public boolean canModify() {
    ItemStack book = getBook();
    if (!isModifiableBook(book)) {
      return false;
    }

    for (int i = SLOT_MODIFIER_START; i <= SLOT_MODIFIER_END; i++) {
      ItemStack page = inventory.getItem(i);
      if (!page.isEmpty() && hasKnownLinkProperty(page)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Performs the modification, applying page properties to the book.
   */
  public void performModification() {
    if (!canModify()) {
      return;
    }

    ItemStack book = getBook();
    CompoundTag bookTag = ItemStackNbt.getOrCreateTag(book);

    for (int i = SLOT_MODIFIER_START; i <= SLOT_MODIFIER_END; i++) {
      ItemStack page = inventory.getItem(i);
      if (!page.isEmpty() && Page.hasLinkProperties(page)) {

        boolean appliedProperty = false;
        List<String> properties = Page.getLinkProperties(page);
        for (String property : properties) {
          if (!LinkFlags.isKnown(property)) {
            continue;
          }
          LinkOptions.setFlag(bookTag, property, true);
          appliedProperty = true;
        }

        if (appliedProperty) {
          page.shrink(1);
        }
      }
    }
    ItemStackNbt.setTag(book, bookTag);

    setChanged();
    markForUpdate();
  }

  private static boolean hasKnownLinkProperty(ItemStack page) {
    for (String property : Page.getLinkProperties(page)) {
      if (LinkFlags.isKnown(property)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets all items for dropping when block is broken.
   */
  public List<ItemStack> getDrops() {
    List<ItemStack> drops = new ArrayList<>();
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        drops.add(stack.copy());
      }
    }
    return drops;
  }

  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if (level != null) {
      for (ItemStack drop : getDrops()) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
      }
    }
    super.preRemoveSideEffects(pos, state);
  }

  /**
   * Gets the book title.
   */
  @NotNull
  public String getBookTitle() {
    ItemStack book = getBook();
    if (book.isEmpty()) return "";
    return book.getHoverName().getString();
  }

  /**
   * Sets the book title.
   */
  public void setBookTitle(@NotNull Player player, @NotNull String title) {
    ItemStack book = getBook();
    if (!isModifiableBook(book)) return;
    if (book.getItem() instanceof AgebookItem agebookItem) {
      agebookItem.setDisplayName(book, title);
    } else if (book.getItem() instanceof LinkbookItem linkbookItem) {
      linkbookItem.setDisplayName(book, title);
    } else {
      ItemStackNbt.setHoverName(book, Component.literal(title));
    }
    setChanged();
    markForUpdate();
  }

  /**
   * Gets a link flag value.
   */
  public boolean getLinkFlag(@NotNull String flagId) {
    ItemStack book = getBook();
    if (!isModifiableBook(book)) return false;
    return LinkOptions.getFlag(ItemStackNbt.getTag(book), flagId);
  }

  /**
   * Sets a link flag value.
   */
  public void setLinkFlag(@NotNull String flagId, boolean value) {
    ItemStack book = getBook();
    if (!isModifiableBook(book) || !LinkFlags.isKnown(flagId)) return;

    CompoundTag tag = ItemStackNbt.getOrCreateTag(book);
    LinkOptions.setFlag(tag, flagId, value);
    ItemStackNbt.setTag(book, tag);
    setChanged();
    markForUpdate();
  }

  /**
   * Gets the dimension UID for the book's link destination.
   */
  @NotNull
  public String getLinkDimensionUID() {
    ItemStack book = getBook();
    if (book.isEmpty()) return "";

    CompoundTag tag = ItemStackNbt.getTag(book);
    if (tag == null) return "";

    ResourceKey<Level> dimKey = LinkOptions.getDimension(tag);
    if (dimKey != null) return dimKey.identifier().toString();

    Integer dimensionUID = LinkOptions.getDimensionUID(tag);
    return dimensionUID != null ? String.valueOf(dimensionUID) : "";
  }

  /**
   * Gets the seed from an agebook.
   */
  @NotNull
  public String getItemSeed() {
    ItemStack book = getBook();
    if (book.isEmpty() || !(book.getItem() instanceof AgebookItem)) return "";

    OptionalLong seed = AgeSeed.read(book);
    return seed.isPresent() ? String.valueOf(seed.getAsLong()) : "";
  }

  /**
   * Sets the seed on an agebook.
   */
  public void setItemSeed(@NotNull Player player, @NotNull String seedStr) {
    ItemStack book = getBook();
    if (!isUnwrittenAgebook(book)) return;

    if (seedStr.isEmpty()) {
      AgeSeed.clear(book);
    } else {
      try {
        AgeSeed.write(book, Long.parseLong(seedStr));
      } catch (NumberFormatException ignored) {
        return;
      }
    }
    setChanged();
    markForUpdate();
  }

  /**
   * Checks if the book has a seed (only Agebooks).
   */
  public boolean hasItemSeed() {
    return isUnwrittenAgebook(getBook());
  }

  private static boolean isUnwrittenAgebook(ItemStack book) {
    return !book.isEmpty()
        && book.getItem() instanceof AgebookItem
        && LinkOptions.getDimensionUID(ItemStackNbt.getTag(book)) == null;
  }

  /**
   * Checks if the link is dead (dimension destroyed).
   */
  public boolean isLinkDead() {
    ItemStack book = getBook();
    if (book.isEmpty()) return false;

    return LinkOptions.isDead(ItemStackNbt.getTag(book));
  }

  /**
   * Marks the book's link as dead (recycles the dimension).
   */
  public void recycleDimension() {
    ItemStack book = getBook();
    if (!isModifiableBook(book)) return;

    CompoundTag tag = ItemStackNbt.getOrCreateTag(book);
    LinkOptions.setDead(tag, true);
    ItemStackNbt.setTag(book, tag);
    setChanged();
    markForUpdate();
  }

  @Override
  @NotNull
  public Component getDisplayName() {
    return Component.translatable("container.mystcraft.link_modifier");
  }

  @Override
  public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
    return new LinkModifierMenu(containerId, playerInventory, this);
  }
}
