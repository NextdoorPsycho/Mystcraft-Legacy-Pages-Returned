package art.arcane.mystcraft.menu;

import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
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
 * Menu for the Link Modifier block.
 * Provides access to book slot and modifier page slots.
 */
public class LinkModifierMenu extends AbstractContainerMenu {

    private final LinkModifierBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final DataSlot canModifyData;

    // Slot indices
    public static final int SLOT_BOOK = 0;
    public static final int SLOT_MODIFIER_START = 1;
    public static final int SLOT_MODIFIER_END = 4;
    public static final int BLOCK_ENTITY_SLOTS = 5;

    // Player inventory slot ranges
    private static final int PLAYER_INVENTORY_START = BLOCK_ENTITY_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

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

        IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new IllegalStateException("LinkModifier has no item handler"));

        // Book slot (center)
        addSlot(new SlotItemHandler(handler, LinkModifierBlockEntity.SLOT_BOOK, 80, 17));

        // Modifier page slots (4 slots around the book)
        addSlot(new SlotItemHandler(handler, LinkModifierBlockEntity.SLOT_MODIFIER_START, 35, 35));
        addSlot(new SlotItemHandler(handler, LinkModifierBlockEntity.SLOT_MODIFIER_START + 1, 62, 35));
        addSlot(new SlotItemHandler(handler, LinkModifierBlockEntity.SLOT_MODIFIER_START + 2, 98, 35));
        addSlot(new SlotItemHandler(handler, LinkModifierBlockEntity.SLOT_MODIFIER_START + 3, 125, 35));

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

        // Data slot for canModify state
        canModifyData = addDataSlot(DataSlot.standalone());
        if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            canModifyData.set(blockEntity.canModify() ? 1 : 0);
        }
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
        canModifyData.set(blockEntity.canModify() ? 1 : 0);
    }

    /**
     * Gets whether modification can be performed.
     */
    public boolean canModify() {
        return canModifyData.get() != 0;
    }

    /**
     * Gets the block entity.
     */
    public LinkModifierBlockEntity getBlockEntity() {
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
                // Try book slot first
                if (!moveItemStackTo(stackInSlot, SLOT_BOOK, SLOT_BOOK + 1, false)) {
                    // Then try modifier slots
                    if (!moveItemStackTo(stackInSlot, SLOT_MODIFIER_START, SLOT_MODIFIER_END + 1, false)) {
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
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }
}
