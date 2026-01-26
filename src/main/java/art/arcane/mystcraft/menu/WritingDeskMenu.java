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
 *
 * Legacy layout constants:
 * - xShift = 228 + 5 = 233 (offset for right panel)
 * - yShift = 20 (top button bar height + gap)
 */
public class WritingDeskMenu extends AbstractContainerMenu {

    // Legacy positioning constants
    public static final int X_SHIFT = 228 + 5; // Left panel width + gap
    public static final int Y_SHIFT = 20; // Button bar + gap
    public static final int TAB_SLOT_COUNT = 4; // Visible tab slots

    private final WritingDeskBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final DataSlot inkAmountData;
    private final DataSlot inkCapacityData;

    // Slot indices in the menu (matching legacy order)
    // First 4 slots are tab slots (visible tabs)
    public static final int TAB_SLOTS_START = 0;
    public static final int TAB_SLOTS_END = TAB_SLOT_COUNT;
    // Then 4 main inventory slots
    public static final int SLOT_WRITING = TAB_SLOTS_END; // Target/writing slot
    public static final int SLOT_PAPER = TAB_SLOTS_END + 1;
    public static final int SLOT_CONTAINER_IN = TAB_SLOTS_END + 2;
    public static final int SLOT_CONTAINER_OUT = TAB_SLOTS_END + 3;
    public static final int BLOCK_ENTITY_SLOTS = TAB_SLOTS_END + 4;

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
     * Legacy slot positions:
     * - Tab slots (4 visible): x=37, y=14+i*37+yShift for i=0..3
     * - Main slot 0 (target): (8+xShift, 60+yShift) = (241, 80)
     * - Main slot 1 (paper): (8+xShift, 8+yShift) = (241, 28)
     * - Main slot 2 (container in): (152+xShift, 8+yShift) = (385, 28)
     * - Main slot 3 (container out): (152+xShift, 60+yShift) = (385, 80)
     * - Player inv: (8+xShift, 84+i*18+yShift) starts at (241, 104)
     * - Hotbar: (8+xShift, 142+yShift) = (241, 162)
     */
    public WritingDeskMenu(int containerId, Inventory playerInventory, WritingDeskBlockEntity blockEntity) {
        super(ModMenuTypes.WRITING_DESK.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        IItemHandler mainHandler = blockEntity.getMainInventory();
        IItemHandler tabHandler = blockEntity.getTabInventory();

        // Tab slots (4 visible slots on left side)
        // Legacy: (37, 14 + i * 37 + yShift) where yShift=20
        for (int i = 0; i < TAB_SLOT_COUNT; i++) {
            SlotItemHandler slot = new SlotItemHandler(tabHandler, i, 37, 14 + i * 37 + Y_SHIFT);
            addSlot(slot);
        }

        // Main inventory slots in the right panel
        // Slot 0: Target/writing slot at (8+xShift, 60+yShift) = (241, 80)
        SlotItemHandler writingSlot = new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_WRITING, 8 + X_SHIFT, 60 + Y_SHIFT);
        addSlot(writingSlot);

        // Slot 1: Paper slot at (8+xShift, 8+yShift) = (241, 28)
        addSlot(new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_PAPER, 8 + X_SHIFT, 8 + Y_SHIFT));

        // Slot 2: Container in (ink bucket) at (152+xShift, 8+yShift) = (385, 28)
        addSlot(new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_CONTAINER_IN, 152 + X_SHIFT, 8 + Y_SHIFT));

        // Slot 3: Container out (empty bucket) at (152+xShift, 60+yShift) = (385, 80) - output only
        addSlot(new SlotItemHandler(mainHandler, WritingDeskBlockEntity.SLOT_CONTAINER_OUT, 152 + X_SHIFT, 60 + Y_SHIFT) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        // Player inventory (3 rows of 9) at (8+xShift, 84+yShift) = (241, 104)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18 + X_SHIFT, 84 + row * 18 + Y_SHIFT));
            }
        }

        // Player hotbar at (8+xShift, 142+yShift) = (241, 162)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18 + X_SHIFT, 142 + Y_SHIFT));
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
                    if (!moveItemStackTo(stackInSlot, TAB_SLOTS_START, TAB_SLOTS_END, false)) {
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
