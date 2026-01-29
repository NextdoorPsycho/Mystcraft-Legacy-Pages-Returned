package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.item.PortfolioItem;
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
 * Menu for the Portfolio item.
 * Shows pages stored in the portfolio (64 slots in 8x8 grid).
 */
public class PortfolioMenu extends AbstractContainerMenu {

  // Slot indices
  public static final int PORTFOLIO_SLOTS = PortfolioItem.MAX_PAGES;
  // Player inventory slot ranges
  private static final int PLAYER_INVENTORY_START = PORTFOLIO_SLOTS;
  private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
  private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
  private final ItemStack portfolioStack;
  private final int portfolioSlot;
  private final PortfolioInventoryHandler portfolioInventory;

  /**
   * Client-side constructor.
   */
  public PortfolioMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
    this(containerId, playerInventory, extraData.readVarInt());
  }

  /**
   * Server-side constructor.
   */
  public PortfolioMenu(int containerId, Inventory playerInventory, int portfolioSlot) {
    super(ModMenuTypes.PORTFOLIO.get(), containerId);
    this.portfolioSlot = portfolioSlot;
    this.portfolioStack = playerInventory.getItem(portfolioSlot);
    this.portfolioInventory = new PortfolioInventoryHandler(portfolioStack);

    // Portfolio page slots (8 rows of 8)
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        int slotIndex = col + row * 8;
        addSlot(new Slot(portfolioInventory, slotIndex, 8 + col * 18, 18 + row * 18) {
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
        // Lock the portfolio slot to prevent duplication
        if (slotIdx == portfolioSlot) {
          addSlot(new LockedSlot(playerInventory, slotIdx, 8 + col * 18, 174 + row * 18));
        } else {
          addSlot(new Slot(playerInventory, slotIdx, 8 + col * 18, 174 + row * 18));
        }
      }
    }

    // Player hotbar
    for (int col = 0; col < 9; col++) {
      int slotIdx = col;
      // Lock the portfolio slot to prevent duplication
      if (slotIdx == portfolioSlot) {
        addSlot(new LockedSlot(playerInventory, slotIdx, 8 + col * 18, 232));
      } else {
        addSlot(new Slot(playerInventory, slotIdx, 8 + col * 18, 232));
      }
    }
  }

  @Override
  public boolean stillValid(@NotNull Player player) {
    ItemStack current = player.getInventory().getItem(portfolioSlot);
    return !current.isEmpty() && current.getItem() instanceof PortfolioItem;
  }

  @Override
  public void removed(@NotNull Player player) {
    super.removed(player);
    // Save pages back to portfolio
    portfolioInventory.saveToPortfolio();
  }

  @Override
  @NotNull
  public ItemStack quickMoveStack(@NotNull Player player, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = slots.get(index);

    if (slot.hasItem()) {
      ItemStack stackInSlot = slot.getItem();
      result = stackInSlot.copy();

      // Moving from portfolio slots to player inventory
      if (index < PORTFOLIO_SLOTS) {
        if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
          return ItemStack.EMPTY;
        }
      }
      // Moving from player inventory to portfolio slots
      else {
        // Only pages can go in portfolio
        if (stackInSlot.getItem() instanceof PageItem) {
          if (!moveItemStackTo(stackInSlot, 0, PORTFOLIO_SLOTS, false)) {
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
        slot.setByPlayer(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }

    return result;
  }

  /**
   * Gets the portfolio item stack.
   */
  public ItemStack getPortfolioStack() {
    return portfolioStack;
  }

  /**
   * Reloads the inventory slots from the portfolio NBT.
   * Called after sorting to sync the new order to the GUI.
   */
  public void reloadFromPortfolio() {
    portfolioInventory.reloadFromPortfolio();
  }

  /**
   * Custom inventory handler that wraps portfolio page storage.
   */
  private static class PortfolioInventoryHandler extends SimpleContainer {
    private final ItemStack portfolio;

    public PortfolioInventoryHandler(ItemStack portfolio) {
      super(PortfolioItem.MAX_PAGES);
      this.portfolio = portfolio;
      reloadFromPortfolio();
    }

    /**
     * Reloads all slots from the portfolio NBT data.
     * Used after external modifications like sorting.
     */
    public void reloadFromPortfolio() {
      // Clear all slots first
      clearContent();
      // Load from portfolio
      List<ItemStack> pages = PortfolioItem.getPages(portfolio);
      for (int i = 0; i < pages.size() && i < getContainerSize(); i++) {
        setItem(i, pages.get(i).copy());
      }
    }

    public void saveToPortfolio() {
      ArrayList<ItemStack> pages = new ArrayList<>();
      for (int i = 0; i < getContainerSize(); i++) {
        ItemStack stack = getItem(i);
        if (!stack.isEmpty()) {
          pages.add(stack.copy());
        }
      }
      PortfolioItem.setPages(portfolio, pages);
    }

    @Override
    public void setChanged() {
      super.setChanged();
      saveToPortfolio();
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
   * A slot that cannot be interacted with (used to lock the portfolio slot).
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
