package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.LinkFlags;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.link.LinkingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Linkbook item.
 * Links to a specific location in any dimension.
 *
 * Legacy behavior (exact match):
 * - Stack size of 1
 * - Max damage of 10 (health system)
 * - Right-click opens the book GUI (not direct linking!)
 * - Linking happens through the activate() method
 * - Book is dropped as an entity when linking (unless "following" flag)
 * - Auto-initializes with current position when first in inventory
 * - Returns a single link page when queried
 */
public class LinkbookItem extends Item {

    private static final float DEFAULT_MAX_HEALTH = 10.0f;

    public LinkbookItem(Properties properties) {
        super(properties.stacksTo(1).durability(10)); // Legacy: setMaxStackSize(1), setMaxDamage(10)
    }

    @Override
    @NotNull
    public Rarity getRarity(@NotNull ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    @NotNull
    public Component getName(@NotNull ItemStack stack) {
        if (stack.getTag() != null) {
            String displayName = LinkOptions.getDisplayName(stack.getTag());
            if (!"???".equals(displayName)) {
                return Component.literal(displayName);
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (stack.getTag() != null) {
            // Legacy shows display name in tooltip
            String name = LinkOptions.getDisplayName(stack.getTag());
            if (!name.isEmpty() && !"???".equals(name)) {
                tooltip.add(Component.literal(name));
            }
        }
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        // Legacy: validate() is called in onUpdate which calls initialize() if no tag
        if (!level.isClientSide) {
            validate(level, stack, entity);
        }
    }

    /**
     * Legacy: validate() - ensures the book is initialized.
     */
    public void validate(@Nullable Level level, @NotNull ItemStack stack, @Nullable Entity entity) {
        if (stack.getTag() == null) {
            initialize(level, stack, entity);
        }
    }

    /**
     * Legacy: initialize() - creates link info from current position.
     * Called when the book has no tag yet.
     */
    protected void initialize(@Nullable Level level, @NotNull ItemStack stack, @Nullable Entity entity) {
        if (level == null || entity == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        LinkOptions.setSpawn(tag, entity.blockPosition());
        LinkOptions.setSpawnYaw(tag, entity.getYRot());
        int dimId = LinkingManager.getDimensionUID(level);
        LinkOptions.setDimensionUID(tag, dimId);

        // Set max health
        tag.putFloat("MaxHealth", DEFAULT_MAX_HEALTH);

        stack.setTag(tag);
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Legacy: onItemRightClick opens the GUI on server, passes on client
        // See legacy ItemLinking.java line 148-154
        if (level.isClientSide) {
            // On client, open the book viewing screen
            art.arcane.mystcraft.client.screen.BookScreen.open(stack);
            return InteractionResultHolder.success(stack);
        }

        // On server, the GUI will handle the link via a packet/callback
        // For now, opening the GUI is the main action
        return InteractionResultHolder.success(stack);
    }

    /**
     * Legacy: activate() - performs the actual linking.
     * Called from the GUI or other activation sources.
     */
    public void activate(@NotNull ItemStack stack, Level level, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        if (stack.getTag() == null) {
            return;
        }

        CompoundTag linkData = stack.getTag();

        // Check link info validity
        BlockPos spawn = LinkOptions.getSpawn(linkData);
        Integer dimId = LinkOptions.getDimensionUID(linkData);
        if (spawn == null || dimId == null) {
            return;
        }

        // Perform pre-link actions
        onLink(stack, level, entity);

        // Perform the actual teleport
        LinkingManager.performLink(entity, linkData);
    }

    /**
     * Legacy: onLink() - called before linking, drops book if not "following".
     */
    protected void onLink(@NotNull ItemStack stack, Level level, Entity entity) {
        if (entity instanceof Player player) {
            // Find which slot has this book (main hand or off hand)
            // Legacy used reference comparison with a server container, but we use content comparison
            // since our BookScreen is client-only
            ItemStack mainHand = player.getInventory().getSelected();
            ItemStack offHand = player.getOffhandItem();

            int slotToEmpty = -1;
            if (ItemStack.isSameItemSameTags(mainHand, stack)) {
                slotToEmpty = player.getInventory().selected;
            } else if (ItemStack.isSameItemSameTags(offHand, stack)) {
                slotToEmpty = 40; // Offhand slot index
            } else {
                // Book not found in either hand
                return;
            }

            // Drop book if not "following" flag
            if (dropItemOnLink(stack)) {
                // Spawn the book entity in the original world
                LinkbookEntity bookEntity = new LinkbookEntity(level, player.getX(), player.getY(), player.getZ());
                bookEntity.setBookItem(stack.copy());
                level.addFreshEntity(bookEntity);

                // Remove from inventory (use tracked slot, not just selected)
                player.getInventory().setItem(slotToEmpty, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Legacy: dropItemOnLink() - checks if book should be dropped on link.
     * Returns true unless "following" flag is set.
     */
    public boolean dropItemOnLink(@NotNull ItemStack stack) {
        return !LinkOptions.getFlag(stack.getTag(), LinkFlags.FOLLOWING);
    }

    /**
     * Gets the list of pages - linkbooks always have a single link page.
     * Legacy: getPageList() returns Collections.singletonList(Page.createLinkPage())
     */
    public List<ItemStack> getPageList(Player player, @NotNull ItemStack stack) {
        return Collections.singletonList(Page.createLinkPage());
    }

    /**
     * Gets the authors of this book.
     * Legacy: getAuthors() checks for "Author" in NBT
     */
    public Collection<String> getAuthors(@NotNull ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("Author")) {
            return Collections.singleton(stack.getTag().getString("Author"));
        }
        return Collections.emptySet();
    }

    /**
     * Sets the display name of the linkbook.
     */
    public void setDisplayName(@NotNull ItemStack stack, String name) {
        LinkOptions.setDisplayName(stack.getOrCreateTag(), name);
    }

    /**
     * Gets the display name of the linkbook.
     */
    public String getDisplayName(@NotNull ItemStack stack) {
        return LinkOptions.getDisplayName(stack.getTag());
    }

    /**
     * Gets the destination position.
     */
    @Nullable
    public BlockPos getDestination(@NotNull ItemStack stack) {
        return LinkOptions.getSpawn(stack.getTag());
    }

    /**
     * Sets the destination position.
     */
    public void setDestination(@NotNull ItemStack stack, BlockPos pos) {
        LinkOptions.setSpawn(stack.getOrCreateTag(), pos);
    }

    // ========================= Health/Durability System =========================
    // Legacy: ItemLinking has health system with getHealth/setHealth/getMaxHealth

    /**
     * Linkbooks can take damage (health system from legacy).
     */
    public boolean isDamageableItem() {
        return true;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false; // Legacy: isBookEnchantable returns false
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return false;
    }

    /**
     * Legacy: setHealth()
     */
    public static void setHealth(@NotNull ItemStack book, float health) {
        if (book.isEmpty()) return;
        CompoundTag tag = book.getOrCreateTag();
        tag.putFloat("damage", getMaxHealth(book) - health);
    }

    /**
     * Legacy: getHealth()
     */
    public static float getHealth(@NotNull ItemStack book) {
        float health = getMaxHealth(book);
        if (book.isEmpty()) return health;
        CompoundTag tag = book.getTag();
        if (tag == null) return health;
        float damage = tag.getFloat("damage");
        return health - damage;
    }

    /**
     * Legacy: getMaxHealth()
     */
    public static float getMaxHealth(@NotNull ItemStack book) {
        float health = DEFAULT_MAX_HEALTH;
        if (book.isEmpty()) return health;
        CompoundTag tag = book.getTag();
        if (tag == null) return health;
        if (!tag.contains("MaxHealth")) {
            tag.putFloat("MaxHealth", health);
        }
        return tag.getFloat("MaxHealth");
    }

    @Override
    public boolean isDamaged(@NotNull ItemStack stack) {
        return getHealth(stack) != getMaxHealth(stack);
    }

    @Override
    public int getDamage(@NotNull ItemStack stack) {
        return (int) getMaxHealth(stack) - (int) getHealth(stack);
    }

    @Override
    public void setDamage(@NotNull ItemStack stack, int damage) {
        setHealth(stack, getMaxHealth(stack) - damage);
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        return (int) getMaxHealth(stack);
    }

    /**
     * Legacy: hasEffect() returns true if "following" flag is set.
     */
    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return LinkOptions.getFlag(stack.getTag(), LinkFlags.FOLLOWING);
    }

    // ========================= Custom Entity on Q-Drop =========================

    /**
     * Tell Forge that Q-dropped linkbooks should spawn as LinkbookEntity, not ItemEntity.
     */
    @Override
    public boolean hasCustomEntity(@NotNull ItemStack stack) {
        return true;
    }

    /**
     * Creates a LinkbookEntity when the item is Q-dropped instead of a regular ItemEntity.
     * This matches legacy behavior where dropped linkbooks appear as open books on the ground.
     */
    @Override
    @Nullable
    public Entity createEntity(Level level, Entity location, @NotNull ItemStack stack) {
        LinkbookEntity entity = new LinkbookEntity(level, location.getX(), location.getY(), location.getZ());
        entity.setBookItem(stack.copy());
        entity.setDeltaMovement(location.getDeltaMovement());
        return entity;
    }
}
