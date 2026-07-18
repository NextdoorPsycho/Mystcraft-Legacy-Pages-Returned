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
 * Menu for the Portfolio item. Shows pages stored in the portfolio (64 slots in
 * 8x8 grid).
 */
public class PortfolioMenu extends AbstractContainerMenu {

  public static final int PORTFOLIO_SLOTS = PortfolioItem.MAX_PAGES;

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

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        int slotIdx = col + row * 9 + 9;

        if (slotIdx == portfolioSlot) {
          addSlot(new LockedSlot(playerInventory, slotIdx, 8 + col * 18, 174 + row * 18));
        } else {
          addSlot(new Slot(playerInventory, slotIdx, 8 + col * 18, 174 + row * 18));
        }
      }
    }

    for (int col = 0; col < 9; col++) {
      int slotIdx = col;

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
    return current == portfolioStack && current.getItem() instanceof PortfolioItem;
  }

  @Override
  public void removed(@NotNull Player player) {
    super.removed(player);

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

      if (index < PORTFOLIO_SLOTS) {
        if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
          return ItemStack.EMPTY;
        }
      } else {

        if (stackInSlot.getItem() instanceof PageItem) {
          if (!moveItemStackTo(stackInSlot, 0, PORTFOLIO_SLOTS, false)) {

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
   * Reloads the inventory slots from the portfolio NBT. Called after sorting to
   * sync the new order to the GUI.
   */
  public void reloadFromPortfolio() {
    portfolioInventory.reloadFromPortfolio();
  }

  private static class PortfolioInventoryHandler extends SimpleContainer {
    private final ItemStack portfolio;
    private final List<ItemStack> overflow = new ArrayList<>();
    private boolean loading;

    public PortfolioInventoryHandler(ItemStack portfolio) {
      super(PortfolioItem.MAX_PAGES);
      this.portfolio = portfolio;
      reloadFromPortfolio();
    }

    /**
     * Reloads all slots from the portfolio NBT data. Used after external
     * modifications like sorting.
     */
    public void reloadFromPortfolio() {
      loading = true;
      try {
        // clearContent() invokes the overridable setChanged(); the loading
        // guard prevents a reload from saving an empty list over the NBT it is
        // about to read.
        super.clearContent();
        overflow.clear();
        List<ItemStack> pages = PortfolioItem.getPages(portfolio);
        for (int i = 0; i < pages.size(); i++) {
          ItemStack page = pages.get(i).copy();
          if (i < getContainerSize()) {
            setItem(i, page);
          } else {
            overflow.add(page);
          }
        }
      } finally {
        loading = false;
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
      for (ItemStack stack : overflow) {
        if (!stack.isEmpty()) {
          pages.add(stack.copy());
        }
      }
      PortfolioItem.setPages(portfolio, pages);
    }

    @Override
    public void setChanged() {
      super.setChanged();
      if (loading) {
        return;
      }
      promoteOverflow();
      saveToPortfolio();
    }

    private void promoteOverflow() {
      if (overflow.isEmpty()) {
        return;
      }
      loading = true;
      try {
        for (int slot = 0; slot < getContainerSize() && !overflow.isEmpty(); slot++) {
          if (getItem(slot).isEmpty()) {
            setItem(slot, overflow.remove(0));
          }
        }
      } finally {
        loading = false;
      }
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
