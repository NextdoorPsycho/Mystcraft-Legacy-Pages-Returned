package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
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
 * Menu for the Ink Mixer block.
 * Provides access to ink input, paper input, and output slots.
 */
public class InkMixerMenu extends AbstractContainerMenu {

  // Slot indices in the menu
  public static final int SLOT_INK_IN = 0;
  public static final int SLOT_PAPER = 1;
  public static final int SLOT_INK_OUT = 2;
  public static final int SLOT_CRAFT_RESULT = 3;
  public static final int BLOCK_ENTITY_SLOTS = 4;
  // Player inventory slot ranges
  private static final int PLAYER_INVENTORY_START = BLOCK_ENTITY_SLOTS;
  private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
  private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;
  private final InkMixerBlockEntity blockEntity;
  private final ContainerLevelAccess access;
  private final DataSlot hasInkData;
  private final Container craftResult;

  /**
   * Client-side constructor - called from ScreenConstructor.
   */
  public InkMixerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
    this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
  }

  /**
   * Server-side constructor.
   */
  public InkMixerMenu(int containerId, Inventory playerInventory, InkMixerBlockEntity blockEntity) {
    super(ModMenuTypes.INK_MIXER.get(), containerId);
    this.blockEntity = blockEntity;
    this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
    this.craftResult = new SimpleContainer(1);

    Container container = blockEntity.getInventory();

    // Block entity slots
    // Ink input slot (top left)
    addSlot(new Slot(container, InkMixerBlockEntity.SLOT_INK_IN, 8, 27));
    // Paper slot (bottom left)
    addSlot(new Slot(container, InkMixerBlockEntity.SLOT_PAPER, 8, 48));
    // Output slot (top right) - output only
    addSlot(new Slot(container, InkMixerBlockEntity.SLOT_INK_OUT, 152, 27) {
      @Override
      public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
      }
    });
    // Craft result slot (bottom right) - uses separate inventory for link panel output
    addSlot(new Slot(craftResult, 0, 152, 48) {
      @Override
      public boolean mayPlace(@NotNull ItemStack stack) {
        return false; // Cannot place items in output
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

    // Data slot for hasInk state
    hasInkData = addDataSlot(DataSlot.standalone());
    if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
      hasInkData.set(blockEntity.hasInk() ? 1 : 0);
    }
  }

  private static InkMixerBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
    BlockEntity be = playerInventory.player.getLevel().getBlockEntity(extraData.readBlockPos());
    if (be instanceof InkMixerBlockEntity mixer) {
      return mixer;
    }
    throw new IllegalStateException("Block entity is not an InkMixerBlockEntity");
  }

  @Override
  public boolean stillValid(@NotNull Player player) {
    return stillValid(access, player, ModBlocks.INK_MIXER.get());
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();
    hasInkData.set(blockEntity.hasInk() ? 1 : 0);

    // Update the craft result slot with preview of what would be crafted
    craftResult.setItem(0, blockEntity.getCraftedItem());
  }

  /**
   * Gets whether the mixer has ink.
   */
  public boolean hasInk() {
    return hasInkData.get() != 0;
  }

  /**
   * Gets the block entity.
   */
  public InkMixerBlockEntity getBlockEntity() {
    return blockEntity;
  }

  /**
   * Gets the current ink probabilities.
   * Used client-side for rendering the color gradient.
   */
  public java.util.Map<String, Float> getInkProbabilities() {
    return blockEntity.getInkProbabilities();
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
        // Try ink input slot first
        if (!moveItemStackTo(stackInSlot, SLOT_INK_IN, SLOT_INK_IN + 1, false)) {
          // Then try paper slot
          if (!moveItemStackTo(stackInSlot, SLOT_PAPER, SLOT_PAPER + 1, false)) {
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
