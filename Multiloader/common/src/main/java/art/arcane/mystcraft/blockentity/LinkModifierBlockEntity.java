package art.arcane.mystcraft.blockentity;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Block entity for the Link Modifier.
 * Used to add modifier pages (link properties) to existing linkbooks.
 *
 * Slots:
 * 0 - Book slot (linkbook to modify)
 * 1-4 - Modifier page slots
 */
public class LinkModifierBlockEntity extends MystcraftBlockEntity implements MenuProvider {

    private static final String TAG_INVENTORY = "inventory";
    public static final int SLOT_BOOK = 0;
    public static final int SLOT_MODIFIER_START = 1;
    public static final int SLOT_MODIFIER_END = 4;
    public static final int SLOT_COUNT = 5;

    private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT) {
        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_BOOK) {
                return stack.getItem() instanceof LinkbookItem;
            }
            if (slot >= SLOT_MODIFIER_START && slot <= SLOT_MODIFIER_END) {
                return stack.getItem() instanceof PageItem && Page.hasLinkProperties(stack);
            }
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            LinkModifierBlockEntity.this.setChanged();
            LinkModifierBlockEntity.this.markForUpdate();
        }
    };

    public LinkModifierBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LINK_MODIFIER.get(), pos, blockState);
    }

    /**
     * Gets the inventory container for external access.
     */
    public Container getInventory() {
        return inventory;
    }

    @Override
    protected void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        ListTag itemList = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                itemList.add(itemTag);
            }
        }
        tag.put(TAG_INVENTORY, itemList);
    }

    @Override
    protected void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        inventory.clearContent();
        ListTag itemList = tag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            CompoundTag itemTag = itemList.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < inventory.getContainerSize()) {
                inventory.setItem(slot, ItemStack.of(itemTag));
            }
        }
    }

    /**
     * Gets the book in the modifier.
     */
    @NotNull
    public ItemStack getBook() {
        return inventory.getItem(SLOT_BOOK);
    }

    /**
     * Sets the book in the modifier.
     */
    public void setBook(@NotNull ItemStack book) {
        inventory.setItem(SLOT_BOOK, book);
    }

    /**
     * Gets a modifier page.
     */
    @NotNull
    public ItemStack getModifierPage(int index) {
        int slot = SLOT_MODIFIER_START + index;
        if (slot > SLOT_MODIFIER_END) {
            return ItemStack.EMPTY;
        }
        return inventory.getItem(slot);
    }

    /**
     * Checks if modification can be performed.
     * Requires a book and at least one modifier page.
     */
    public boolean canModify() {
        ItemStack book = getBook();
        if (book.isEmpty() || !(book.getItem() instanceof LinkbookItem)) {
            return false;
        }

        // Check if there's at least one modifier page
        for (int i = SLOT_MODIFIER_START; i <= SLOT_MODIFIER_END; i++) {
            ItemStack page = inventory.getItem(i);
            if (!page.isEmpty() && Page.hasLinkProperties(page)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Performs the modification, applying page properties to the book.
     */
    public void performModification() {
        if (!canModify()) {
            return;
        }

        ItemStack book = getBook();

        // Apply each modifier page's properties to the book
        for (int i = SLOT_MODIFIER_START; i <= SLOT_MODIFIER_END; i++) {
            ItemStack page = inventory.getItem(i);
            if (!page.isEmpty() && Page.hasLinkProperties(page)) {
                // Get properties from page and add to book
                List<String> properties = Page.getLinkProperties(page);
                for (String property : properties) {
                    Page.addLinkProperty(book, property);
                }
                // Consume the page
                page.shrink(1);
            }
        }

        setChanged();
        markForUpdate();
    }

    /**
     * Gets all items for dropping when block is broken.
     */
    public List<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    /**
     * Gets the book title.
     */
    @NotNull
    public String getBookTitle() {
        ItemStack book = getBook();
        if (book.isEmpty()) return "";
        return book.getHoverName().getString();
    }

    /**
     * Sets the book title.
     */
    public void setBookTitle(@NotNull Player player, @NotNull String title) {
        ItemStack book = getBook();
        if (book.isEmpty()) return;
        book.setHoverName(Component.literal(title));
        setChanged();
        markForUpdate();
    }

    /**
     * Gets a link flag value.
     */
    public boolean getLinkFlag(@NotNull String flagId) {
        ItemStack book = getBook();
        if (book.isEmpty()) return false;
        return Page.hasLinkProperty(book, flagId);
    }

    /**
     * Sets a link flag value.
     */
    public void setLinkFlag(@NotNull String flagId, boolean value) {
        ItemStack book = getBook();
        if (book.isEmpty()) return;

        if (value) {
            Page.addLinkProperty(book, flagId);
        } else {
            Page.removeLinkProperty(book, flagId);
        }
        setChanged();
        markForUpdate();
    }

    /**
     * Gets the dimension UID for the book's link destination.
     */
    @NotNull
    public String getLinkDimensionUID() {
        ItemStack book = getBook();
        if (book.isEmpty()) return "";

        LinkOptions options = LinkOptions.fromItemStack(book);
        if (options == null) return "";

        ResourceKey<Level> dimKey = options.getDimension();
        if (dimKey == null) return "";

        return dimKey.location().toString();
    }

    /**
     * Gets the seed from an agebook.
     */
    @NotNull
    public String getItemSeed() {
        ItemStack book = getBook();
        if (book.isEmpty() || !(book.getItem() instanceof AgebookItem)) return "";

        CompoundTag tag = book.getTag();
        if (tag == null || !tag.contains("Seed")) return "";

        return String.valueOf(tag.getLong("Seed"));
    }

    /**
     * Sets the seed on an agebook.
     */
    public void setItemSeed(@NotNull Player player, @NotNull String seedStr) {
        ItemStack book = getBook();
        if (book.isEmpty() || !(book.getItem() instanceof AgebookItem)) return;

        CompoundTag tag = book.getOrCreateTag();
        if (seedStr.isEmpty()) {
            tag.remove("Seed");
        } else {
            try {
                long seed = Long.parseLong(seedStr);
                tag.putLong("Seed", seed);
            } catch (NumberFormatException ignored) {
                // Invalid seed format
            }
        }
        setChanged();
        markForUpdate();
    }

    /**
     * Checks if the book has a seed (only Agebooks).
     */
    public boolean hasItemSeed() {
        ItemStack book = getBook();
        return !book.isEmpty() && book.getItem() instanceof AgebookItem;
    }

    /**
     * Checks if the link is dead (dimension destroyed).
     */
    public boolean isLinkDead() {
        ItemStack book = getBook();
        if (book.isEmpty()) return false;

        LinkOptions options = LinkOptions.fromItemStack(book);
        if (options == null) return false;

        return options.isDead();
    }

    /**
     * Marks the book's link as dead (recycles the dimension).
     */
    public void recycleDimension() {
        ItemStack book = getBook();
        if (book.isEmpty()) return;

        LinkOptions options = LinkOptions.fromItemStack(book);
        if (options == null) return;

        options.setDead(true);
        options.toItemStack(book);
        setChanged();
        markForUpdate();
    }

    // MenuProvider implementation

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.translatable("container.mystcraft.link_modifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new LinkModifierMenu(containerId, playerInventory, this);
    }
}
