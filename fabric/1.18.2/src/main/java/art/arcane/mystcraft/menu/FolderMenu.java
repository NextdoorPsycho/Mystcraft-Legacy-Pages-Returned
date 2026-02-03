package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for the Folder item.
 * Shows pages stored in the folder and allows adding/removing pages.
 * <p>
 * 1.18.2 version - APIs are compatible with 1.19.2.
 */
public class FolderMenu extends AbstractContainerMenu {

  // Slot indices
  public static final int FOLDER_SLOTS = FolderItem.MAX_PAGES;
  // Player inventory slot ranges
  private static final int PLAYER_INVENTORY_START = FOLDER_SLOTS;
  private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
  private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
  private final ItemStack folderStack;
  private final int folderSlot;
  private final FolderInventoryHandler folderInventory;

  /**
   * Client-side constructor.
   */
  public FolderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
    this(containerId, playerInventory, extraData.readVarInt());
  }

  /**
   * Server-side constructor.
   */
  public FolderMenu(int containerId, Inventory playerInventory, int folderSlot) {
    super(ModMenuTypes.FOLDER.get(), containerId);
    this.folderSlot = folderSlot;
    this.folderStack = playerInventory.getItem(folderSlot);
    this.folderInventory = new FolderInventoryHandler(folderStack);

    // Folder page slots (2 rows of 8)
    for (int row = 0; row < 2; row++) {
      for (int col = 0; col < 8; col++) {
        int slotIndex = col + row * 8;
        addSlot(new Slot(folderInventory, slotIndex, 17 + col * 18, 18 + row * 18) {
          @Override
          public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof PageItem;
          }

          @Override
          public int getMaxStackSize() {
            return 1;
          }
        });
      }
    }

    // Player inventory (3 rows of 9)
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        int slotIdx = col + row * 9 + 9;
        // Lock the folder slot to prevent duplication
        if (slotIdx == folderSlot) {
          addSlot(new LockedSlot(playerInventory, slotIdx, 8 + col * 18, 72 + row * 18));
        } else {
          addSlot(new Slot(playerInventory, slotIdx, 8 + col * 18, 72 + row * 18));
        }
      }
    }

    // Player hotbar
    for (int col = 0; col < 9; col++) {
      int slotIdx = col;
      // Lock the folder slot to prevent duplication
      if (slotIdx == folderSlot) {
        addSlot(new LockedSlot(playerInventory, slotIdx, 8 + col * 18, 130));
      } else {
        addSlot(new Slot(playerInventory, slotIdx, 8 + col * 18, 130));
      }
    }
  }

  @Override
  public boolean stillValid(@NotNull Player player) {
    ItemStack current = player.getInventory().getItem(folderSlot);
    return !current.isEmpty() && current.getItem() instanceof FolderItem;
  }

  @Override
  public void removed(@NotNull Player player) {
    super.removed(player);
    // Save pages back to folder
    folderInventory.saveToFolder();
  }

  @Override
  @NotNull
  public ItemStack quickMoveStack(@NotNull Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = slots.get(index);

    if (slot.hasItem()) {
      ItemStack stackInSlot = slot.getItem();
      result = stackInSlot.copy();

      // Moving from folder slots to player inventory
      if (index < FOLDER_SLOTS) {
        if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
          return ItemStack.EMPTY;
        }
      }
      // Moving from player inventory to folder slots
      else {
        // Only pages can go in folder
        if (stackInSlot.getItem() instanceof PageItem) {
          if (!moveItemStackTo(stackInSlot, 0, FOLDER_SLOTS, false)) {
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
        } else {
          // Non-page items: move between inventory and hotbar
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

  /**
   * Gets the folder item stack.
   */
  public ItemStack getFolderStack() {
    return folderStack;
  }

  /**
   * Custom inventory handler that wraps folder page storage.
   */
  private static class FolderInventoryHandler extends SimpleContainer {
    private final ItemStack folder;

    public FolderInventoryHandler(ItemStack folder) {
      super(FolderItem.MAX_PAGES);
      this.folder = folder;
      loadFromFolder();
    }

    private void loadFromFolder() {
      List<ItemStack> pages = FolderItem.getPages(folder);
      for (int i = 0; i < pages.size() && i < getContainerSize(); i++) {
        setItem(i, pages.get(i).copy());
      }
    }

    public void saveToFolder() {
      ArrayList<ItemStack> pages = new ArrayList<>();
      for (int i = 0; i < getContainerSize(); i++) {
        ItemStack stack = getItem(i);
        if (!stack.isEmpty()) {
          pages.add(stack.copy());
        }
      }
      FolderItem.setPages(folder, pages);
    }

    @Override
    public void setChanged() {
      super.setChanged();
      saveToFolder();
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
      return stack.getItem() instanceof PageItem;
    }

    @Override
    public int getMaxStackSize() {
      return 1;
    }
  }

  /**
   * A slot that cannot be interacted with (used to lock the folder slot).
   */
  private static class LockedSlot extends Slot {
    public LockedSlot(Inventory inventory, int index, int x, int y) {
      super(inventory, index, x, y);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
      return false;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
      return false;
    }
  }
}
