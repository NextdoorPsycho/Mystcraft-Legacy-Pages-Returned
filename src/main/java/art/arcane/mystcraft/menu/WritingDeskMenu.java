package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Menu for the Writing Desk block.
 * Provides access to writing slot, paper slot, ink container slots,
 * tab slots, and displays ink level.
 */
public class WritingDeskMenu extends AbstractContainerMenu {

    private final WritingDeskBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final DataSlot inkAmountData;
    private final DataSlot inkCapacityData;

    // Slot indices in the menu
    public static final int SLOT_WRITING = 0;
    public static final int SLOT_PAPER = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int MAIN_SLOTS = 4;
    public static final int TAB_SLOTS = 25;
    public static final int BLOCK_ENTITY_SLOTS = MAIN_SLOTS + TAB_SLOTS;

    // Player inventory slot ranges
    private static final int PLAYER_INVENTORY_START = BLOCK_ENTITY_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    /**
     * Client-side constructor - called from ScreenConstructor.
     */
    public WritingDeskMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    /**
     * Server-side constructor.
     */
    public WritingDeskMenu(int containerId, Inventory playerInventory, WritingDeskBlockEntity blockEntity) {
        super(ModMenuTypes.WRITING_DESK.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        IItemHandler mainHandler = blockEntity.getMainInventory();
        IItemHandler tabHandler = blockEntity.getTabInventory();

        // Main inventory slots (left side)
        // Writing slot (top)
        addSlot(new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_WRITING, 26, 17));
        // Paper slot
        addSlot(new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_PAPER, 26, 53));
        // Container in (ink bucket)
        addSlot(new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_CONTAINER_IN, 8, 35));
        // Container out (empty bucket) - output only
        addSlot(new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_CONTAINER_OUT, 44, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        // Tab slots (5 rows of 5, on the right side)
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = col + row * 5;
                addSlot(new SlotItemHandler(tabHandler, slotIndex, 80 + col * 18, 8 + row * 18));
            }
        }

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }

        // Data slots for ink display
        inkAmountData = addDataSlot(DataSlot.standalone());
        inkCapacityData = addDataSlot(DataSlot.standalone());
        if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            inkAmountData.set(blockEntity.getInkAmount());
            inkCapacityData.set(WritingDeskBlockEntity.INK_CAPACITY);
        }
    }

    private static WritingDeskBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof WritingDeskBlockEntity desk) {
            return desk;
        }
        throw new IllegalStateException("Block entity is not a WritingDeskBlockEntity");
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ModBlocks.WRITING_DESK.get());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        inkAmountData.set(blockEntity.getInkAmount());
    }

    /**
     * Gets the current ink amount (client-safe).
     */
    public int getInkAmount() {
        return inkAmountData.get();
    }

    /**
     * Gets the ink capacity (client-safe).
     */
    public int getInkCapacity() {
        return inkCapacityData.get();
    }

    /**
     * Gets the ink percentage (0-100).
     */
    public int getInkPercentage() {
        int capacity = getInkCapacity();
        if (capacity <= 0) return 0;
        return (getInkAmount() * 100) / capacity;
    }

    /**
     * Checks if there's enough ink to write.
     */
    public boolean hasEnoughInk() {
        return getInkAmount() >= WritingDeskBlockEntity.INK_COST;
    }

    /**
     * Gets the block entity.
     */
    public WritingDeskBlockEntity getBlockEntity() {
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
                // Try writing slot first
                if (WritingDeskBlockEntity.isWritableItem(stackInSlot)) {
                    if (!moveItemStackTo(stackInSlot, SLOT_WRITING, SLOT_WRITING + 1, false)) {
                        // Fall through to other slots
                    } else {
                        if (stackInSlot.isEmpty()) {
                            slot.setByPlayer(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        return result;
                    }
                }

                // Try container in slot (ink buckets)
                if (WritingDeskBlockEntity.isInkContainer(stackInSlot)) {
                    if (!moveItemStackTo(stackInSlot, SLOT_CONTAINER_IN, SLOT_CONTAINER_IN + 1, false)) {
                        // Fall through
                    } else {
                        if (stackInSlot.isEmpty()) {
                            slot.setByPlayer(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        return result;
                    }
                }

                // Try paper slot
                if (stackInSlot.is(net.minecraft.world.item.Items.PAPER)) {
                    if (!moveItemStackTo(stackInSlot, SLOT_PAPER, SLOT_PAPER + 1, false)) {
                        // Fall through
                    } else {
                        if (stackInSlot.isEmpty()) {
                            slot.setByPlayer(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        return result;
                    }
                }

                // Try tab slots (page collections)
                if (WritingDeskBlockEntity.isPageCollectionItem(stackInSlot)) {
                    if (!moveItemStackTo(stackInSlot, MAIN_SLOTS, BLOCK_ENTITY_SLOTS, false)) {
                        // Fall through
                    } else {
                        if (stackInSlot.isEmpty()) {
                            slot.setByPlayer(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        return result;
                    }
                }

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

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }
}
