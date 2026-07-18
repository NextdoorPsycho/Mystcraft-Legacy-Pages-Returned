package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModMenuTypes;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.world.AgeSeed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

/**
 * Menu for the Link Modifier block. Provides access to book slot and modifier
 * page slots.
 */
public class LinkModifierMenu extends AbstractContainerMenu {

  public static final int SLOT_BOOK = 0;
  public static final int BLOCK_ENTITY_SLOTS = LinkModifierBlockEntity.SLOT_COUNT;

  private static final int PLAYER_INVENTORY_START = BLOCK_ENTITY_SLOTS;
  private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
  private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
  private final LinkModifierBlockEntity blockEntity;
  private final ContainerLevelAccess access;
  private final DataSlot canModifyData;
  private final DataSlot hasItemSeedData;
  private final DataSlot isLinkDeadData;

  /**
   * Client-side constructor - called from ScreenConstructor.
   */
  public LinkModifierMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
    this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
  }

  /**
   * Server-side constructor.
   */
  public LinkModifierMenu(int containerId, Inventory playerInventory, LinkModifierBlockEntity blockEntity) {
    super(ModMenuTypes.LINK_MODIFIER.get(), containerId);
    this.blockEntity = blockEntity;
    this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

    Container container = blockEntity.getInventory();

    addSlot(new Slot(container, LinkModifierBlockEntity.SLOT_BOOK, 17, 27));
    for (int slot = LinkModifierBlockEntity.SLOT_MODIFIER_START;
         slot <= LinkModifierBlockEntity.SLOT_MODIFIER_END; slot++) {
      addSlot(new Slot(container, slot, 17 + (slot - LinkModifierBlockEntity.SLOT_MODIFIER_START) * 18, 110));
    }

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 43 + col * 18, 146 + row * 18));
      }
    }

    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 43 + col * 18, 204));
    }

    canModifyData = addDataSlot(DataSlot.standalone());
    hasItemSeedData = addDataSlot(DataSlot.standalone());
    isLinkDeadData = addDataSlot(DataSlot.standalone());

    updateCachedData();
  }

  private static LinkModifierBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
    BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
    if (be instanceof LinkModifierBlockEntity modifier) {
      return modifier;
    }
    throw new IllegalStateException("Block entity is not a LinkModifierBlockEntity");
  }

  @Override
  public boolean stillValid(@NotNull Player player) {
    return stillValid(access, player, ModBlocks.LINK_MODIFIER.get());
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();
    updateCachedData();
  }

  private void updateCachedData() {
    if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
      canModifyData.set(blockEntity.canModify() ? 1 : 0);
      hasItemSeedData.set(blockEntity.hasItemSeed() ? 1 : 0);
      isLinkDeadData.set(blockEntity.isLinkDead() ? 1 : 0);

    }
  }

  /**
   * Gets whether modification can be performed.
   */
  public boolean canModify() {
    return canModifyData.get() != 0;
  }

  /**
   * Gets whether the item has a seed field (Agebooks only).
   */
  public boolean hasItemSeed() {
    return hasItemSeedData.get() != 0;
  }

  /**
   * Gets whether the link is dead.
   */
  public boolean isLinkDead() {
    return isLinkDeadData.get() != 0;
  }

  /**
   * Gets the book title.
   */
  @NotNull
  public String getBookTitle() {
    ItemStack book = getBookItem();
    return book.isEmpty() ? "" : book.getHoverName().getString();
  }

  /**
   * Sets the book title (client prediction).
   */
  public void setBookTitleClient(@NotNull String title) {
    ItemStack book = getBookItem();
    if (book.getItem() instanceof AgebookItem agebookItem) {
      agebookItem.setDisplayName(book, title);
    } else if (book.getItem() instanceof LinkbookItem linkbookItem) {
      linkbookItem.setDisplayName(book, title);
    }
  }

  /**
   * Gets the item seed.
   */
  @NotNull
  public String getItemSeed() {
    OptionalLong seed = AgeSeed.read(getBookItem());
    return seed.isPresent() ? String.valueOf(seed.getAsLong()) : "";
  }

  /**
   * Sets the item seed (client prediction).
   */
  public void setItemSeedClient(@NotNull String seed) {
    ItemStack book = getBookItem();
    if (!(book.getItem() instanceof AgebookItem)
        || LinkOptions.getDimensionUID(ItemStackNbt.getTag(book)) != null) {
      return;
    }
    if (seed.isEmpty()) {
      AgeSeed.clear(book);
      return;
    }
    try {
      AgeSeed.write(book, Long.parseLong(seed));
    } catch (NumberFormatException ignored) {
    }
  }

  /**
   * Gets the dimension UID.
   */
  @NotNull
  public String getLinkDimensionUID() {
    CompoundTag tag = ItemStackNbt.getTag(getBookItem());
    Integer dimensionUID = LinkOptions.getDimensionUID(tag);
    return dimensionUID != null ? String.valueOf(dimensionUID) : "";
  }

  /**
   * Gets a link flag value.
   */
  public boolean getLinkFlag(@NotNull String flagId) {
    return LinkOptions.getFlag(ItemStackNbt.getTag(getBookItem()), flagId);
  }

  /**
   * Sets a link flag (client prediction).
   */
  public void setLinkFlagClient(@NotNull String flagId, boolean value) {
    ItemStack book = getBookItem();
    if (book.isEmpty()) {
      return;
    }
    CompoundTag tag = ItemStackNbt.getOrCreateTag(book);
    LinkOptions.setFlag(tag, flagId, value);
    ItemStackNbt.setTag(book, tag);
  }

  /**
   * Gets the block entity.
   */
  public LinkModifierBlockEntity getBlockEntity() {
    return blockEntity;
  }

  /**
   * Gets the book item in the book slot.
   */
  @NotNull
  public ItemStack getBookItem() {
    return slots.get(SLOT_BOOK).getItem();
  }

  /**
   * Checks if the linked Age is dead (no longer exists).
   */
  public boolean isLinkedAgeDead() {
    return isLinkDeadData.get() != 0;
  }

  @Override
  @NotNull
  public ItemStack quickMoveStack(@NotNull Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = slots.get(index);

    if (slot.hasItem()) {
      ItemStack stackInSlot = slot.getItem();
      result = stackInSlot.copy();

      if (index < BLOCK_ENTITY_SLOTS) {
        if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
          return ItemStack.EMPTY;
        }
      } else {
        boolean movedToBook = moveItemStackTo(stackInSlot, SLOT_BOOK, SLOT_BOOK + 1, false);
        boolean movedToModifier = !movedToBook && moveItemStackTo(
            stackInSlot,
            LinkModifierBlockEntity.SLOT_MODIFIER_START,
            LinkModifierBlockEntity.SLOT_MODIFIER_END + 1,
            false
        );
        if (!movedToBook && !movedToModifier) {
          if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_END, PLAYER_HOTBAR_END, false)) {
              return ItemStack.EMPTY;
            }
          } else {
            if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
              return ItemStack.EMPTY;
            }
          }
        }
      }

      if (stackInSlot.isEmpty()) {
        slot.setByPlayer(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }

    return result;
  }
}
