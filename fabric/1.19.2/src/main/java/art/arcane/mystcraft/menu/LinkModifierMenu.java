package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModMenuTypes;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Menu for the Link Modifier block.
 * Provides access to book slot and modifier page slots.
 */
public class LinkModifierMenu extends AbstractContainerMenu {

  // Slot indices - only book slot is exposed in GUI
  public static final int SLOT_BOOK = 0;
  public static final int BLOCK_ENTITY_SLOTS = 1;
  // Player inventory slot ranges
  private static final int PLAYER_INVENTORY_START = BLOCK_ENTITY_SLOTS;
  private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
  private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
  private final LinkModifierBlockEntity blockEntity;
  private final ContainerLevelAccess access;
  private final DataSlot canModifyData;
  private final DataSlot hasItemSeedData;
  private final DataSlot isLinkDeadData;
  private final Map<String, Boolean> cachedLinkFlags = new HashMap<>();
  // Cached data for client
  private String cachedTitle = "";
  private String cachedSeed = "";
  private String cachedDimensionUID = "";

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

    // Book slot (center)
    addSlot(new Slot(container, LinkModifierBlockEntity.SLOT_BOOK, 80, 35));

    // Player inventory (3 rows of 9)
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }

    // Player hotbar
    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    // Data slots
    canModifyData = addDataSlot(DataSlot.standalone());
    hasItemSeedData = addDataSlot(DataSlot.standalone());
    isLinkDeadData = addDataSlot(DataSlot.standalone());

    // Initialize link flags cache
    for (String prop : InkEffects.getProperties()) {
      cachedLinkFlags.put(prop, false);
    }

    updateCachedData();
  }

  private static LinkModifierBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
    BlockEntity be = playerInventory.player.getLevel().getBlockEntity(extraData.readBlockPos());
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

      cachedTitle = blockEntity.getBookTitle();
      cachedSeed = blockEntity.getItemSeed();
      cachedDimensionUID = blockEntity.getLinkDimensionUID();

      for (String prop : InkEffects.getProperties()) {
        cachedLinkFlags.put(prop, blockEntity.getLinkFlag(prop));
      }
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
    return cachedTitle;
  }

  /**
   * Sets the book title (client prediction).
   */
  public void setBookTitleClient(@NotNull String title) {
    cachedTitle = title;
  }

  /**
   * Gets the item seed.
   */
  @NotNull
  public String getItemSeed() {
    return cachedSeed;
  }

  /**
   * Sets the item seed (client prediction).
   */
  public void setItemSeedClient(@NotNull String seed) {
    cachedSeed = seed;
  }

  /**
   * Gets the dimension UID.
   */
  @NotNull
  public String getLinkDimensionUID() {
    return cachedDimensionUID;
  }

  /**
   * Gets a link flag value.
   */
  public boolean getLinkFlag(@NotNull String flagId) {
    return cachedLinkFlags.getOrDefault(flagId, false);
  }

  /**
   * Sets a link flag (client prediction).
   */
  public void setLinkFlagClient(@NotNull String flagId, boolean value) {
    cachedLinkFlags.put(flagId, value);
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

      // Moving from block entity slots to player inventory
      if (index < BLOCK_ENTITY_SLOTS) {
        if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
          return ItemStack.EMPTY;
        }
      }
      // Moving from player inventory to block entity slots
      else {
        // Try book slot first
        if (!moveItemStackTo(stackInSlot, SLOT_BOOK, SLOT_BOOK + 1, false)) {
          // Move between inventory and hotbar
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
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }

    return result;
  }
}
