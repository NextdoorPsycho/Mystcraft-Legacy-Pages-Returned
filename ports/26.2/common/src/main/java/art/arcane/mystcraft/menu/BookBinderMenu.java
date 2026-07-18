package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Menu for the Book Binder block. Provides access to cover slot and page list.
 */
public class BookBinderMenu extends AbstractContainerMenu {

  public static final int SLOT_COVER = 0;
  public static final int SLOT_OUTPUT = 1;
  public static final int BLOCK_ENTITY_SLOTS = 2;

  private static final int PLAYER_INVENTORY_START = BLOCK_ENTITY_SLOTS;
  private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
  private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
  private final BookBinderBlockEntity blockEntity;
  private final ContainerLevelAccess access;
  private final DataSlot pageCountData;
  private final DataSlot canBuildData;
  private final Container craftResult;

  /**
   * Client-side constructor - called from ScreenConstructor.
   */
  public BookBinderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
    this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
  }

  /**
   * Server-side constructor.
   */
  public BookBinderMenu(int containerId, Inventory playerInventory, BookBinderBlockEntity blockEntity) {
    super(ModMenuTypes.BOOK_BINDER.get(), containerId);
    this.blockEntity = blockEntity;
    this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
    this.craftResult = new SimpleContainer(1);

    Container container = blockEntity.getInventory();

    addSlot(new Slot(container, 0, 8, 27));

    addSlot(new Slot(craftResult, 0, 152, 27) {
      @Override
      public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
      }

      @Override
      public boolean mayPickup(@NotNull Player player) {
        return true;
      }

      @Override
      public void onTake(@NotNull Player player, @NotNull ItemStack stack) {

        blockEntity.buildItem(stack, player);
        super.onTake(player, stack);
      }
    });

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 99 + row * 18));
      }
    }

    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 8 + col * 18, 157));
    }

    pageCountData = addDataSlot(DataSlot.standalone());
    canBuildData = addDataSlot(DataSlot.standalone());

    if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
      pageCountData.set(blockEntity.getPageList().size());
      canBuildData.set(blockEntity.canBuildItem() ? 1 : 0);
    }
  }

  private static BookBinderBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
    BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
    if (be instanceof BookBinderBlockEntity binder) {
      return binder;
    }
    throw new IllegalStateException("Block entity is not a BookBinderBlockEntity");
  }

  @Override
  public boolean stillValid(@NotNull Player player) {
    return stillValid(access, player, ModBlocks.BOOK_BINDER.get());
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();
    pageCountData.set(blockEntity.getPageList().size());
    canBuildData.set(blockEntity.canBuildItem() ? 1 : 0);

    craftResult.setItem(0, blockEntity.getCraftedItem());
  }

  /**
   * Gets the number of pages in the binder.
   */
  public int getPageCount() {
    return pageCountData.get();
  }

  /**
   * Gets whether a book can be built.
   */
  public boolean canBuild() {
    return canBuildData.get() != 0;
  }

  /**
   * Gets the block entity.
   */
  public BookBinderBlockEntity getBlockEntity() {
    return blockEntity;
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

        if (stackInSlot.getItem() instanceof PageItem) {

          ItemStack singlePage = stackInSlot.copy();
          singlePage.setCount(1);

          ItemStack remainder = blockEntity.insertPage(singlePage, blockEntity.getPageList().size());
          if (remainder.isEmpty()) {

            stackInSlot.shrink(1);
            slot.setChanged();

            return result;
          }

          return ItemStack.EMPTY;
        }

        if (BookBinderBlockEntity.isValidCover(stackInSlot)) {
          if (!moveItemStackTo(stackInSlot, SLOT_COVER, SLOT_COVER + 1, false)) {

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
   * Shift-clicks a page out of the binder's page list to the player's
   * inventory. Called from the screen when clicking on a page in the list.
   *
   * @param pageIndex the index of the page in the binder's page list
   * @return true if the page was successfully moved to inventory
   */
  public boolean quickMovePageOut(int pageIndex) {
    if (pageIndex < 0 || pageIndex >= blockEntity.getPageList().size()) {
      return false;
    }

    ItemStack page = blockEntity.removePage(pageIndex);
    if (page.isEmpty()) {
      return false;
    }

    if (!moveItemStackTo(page, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {

      blockEntity.insertPage(page, pageIndex);
      return false;
    }

    return true;
  }
}
