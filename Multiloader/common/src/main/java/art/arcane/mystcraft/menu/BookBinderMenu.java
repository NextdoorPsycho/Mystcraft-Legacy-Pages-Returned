package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
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
 * Menu for the Book Binder block.
 * Provides access to cover slot and page list.
 */
public class BookBinderMenu extends AbstractContainerMenu {

  // Slot indices
  public static final int SLOT_COVER = 0;
  public static final int SLOT_OUTPUT = 1;
  public static final int BLOCK_ENTITY_SLOTS = 2;
  // Player inventory slot ranges
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

    // Cover/Input slot at (8, 27)
    addSlot(new Slot(container, 0, 8, 27));

    // Output/Craft result slot at (152, 27) - uses separate inventory
    addSlot(new Slot(craftResult, 0, 152, 27) {
      @Override
      public boolean mayPlace(@NotNull ItemStack stack) {
        return false; // Cannot place items in output
      }

      @Override
      public boolean mayPickup(@NotNull Player player) {
        return true; // Always allow picking up the output
      }

      @Override
      public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        // When taking the crafted item, build it
        blockEntity.buildItem(stack, player);
        super.onTake(player, stack);
      }
    });

    // Player inventory (3 rows of 9) - ySize=181, so inventory starts at y=99
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 99 + row * 18));
      }
    }

    // Player hotbar - at y=157 for ySize=181
    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 8 + col * 18, 157));
    }

    // Data slots
    pageCountData = addDataSlot(DataSlot.standalone());
    canBuildData = addDataSlot(DataSlot.standalone());

    if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
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

    // Update the craft result slot with preview of what would be crafted
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

      // Moving from block entity slots to player inventory
      if (index < BLOCK_ENTITY_SLOTS) {
        if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
          return ItemStack.EMPTY;
        }
      }
      // Moving from player inventory to block entity slots
      else {
        // Try cover slot
        if (!moveItemStackTo(stackInSlot, SLOT_COVER, SLOT_COVER + 1, false)) {
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
        slot.setByPlayer(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }

    return result;
  }
}
