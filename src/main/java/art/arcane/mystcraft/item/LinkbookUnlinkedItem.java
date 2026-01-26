package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Unlinked Linkbook item.
 * A blank linkbook that can be linked to the current location.
 *
 * Legacy behavior (exact match):
 * - Stack size of 16
 * - When right-clicked with exactly 1 in hand, converts to a linked linkbook at current position
 * - Does NOT convert if stack count > 1 (legacy line 54)
 * - Transfers any link panel properties from the unlinked book to the new linked book
 */
public class LinkbookUnlinkedItem extends Item {

    public LinkbookUnlinkedItem(Properties properties) {
        super(properties.stacksTo(16)); // Legacy: setMaxStackSize(16)
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        // Legacy: Show link panel properties in tooltip
        if (stack.getTag() != null) {
            Page.getTooltip(stack, tooltip);
        }
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);

        // Legacy behavior: Only convert if on server AND stack count is exactly 1
        // See legacy ItemLinkbookUnlinked.java line 54: if (worldIn.isRemote || inHand.getCount() > 1)
        if (level.isClientSide || inHand.getCount() > 1) {
            return InteractionResultHolder.pass(inHand);
        }

        // Create a new linked linkbook
        ItemStack linkBook = new ItemStack(ModItems.LINKBOOK.get());

        // Initialize the linkbook with current position (legacy: ((ItemLinkbook) ModItems.linkbook).initialize(worldIn, linkBook, playerIn))
        initializeLinkbook(linkBook, level, player);

        // Apply link panel properties from unlinked book to linked book (legacy: Page.applyLinkPanel(inHand, linkBook))
        Page.applyLinkPanel(inHand, linkBook);

        // Replace the unlinked book with the linked one (legacy lines 60-61)
        player.setItemInHand(hand, linkBook);
        inHand.setCount(0);

        return InteractionResultHolder.pass(linkBook);
    }

    /**
     * Initializes a linkbook with the current position.
     * Matches legacy ItemLinkbook.initialize() behavior.
     */
    private void initializeLinkbook(ItemStack linkBook, Level level, Player player) {
        CompoundTag tag = new CompoundTag();

        // Set spawn position to player's current position
        LinkOptions.setSpawn(tag, player.blockPosition());
        LinkOptions.setSpawnYaw(tag, player.getYRot());

        // Set dimension UID
        int dimId = LinkingManager.getDimensionUID(level);
        LinkOptions.setDimensionUID(tag, dimId);

        // Set a default display name based on dimension
        String dimName = getDimensionDisplayName(level);
        LinkOptions.setDisplayName(tag, dimName);

        linkBook.setTag(tag);
    }

    /**
     * Gets a display name for the dimension.
     */
    private String getDimensionDisplayName(Level level) {
        ResourceKey<Level> dimension = level.dimension();
        if (dimension == Level.OVERWORLD) {
            return "Overworld";
        } else if (dimension == Level.NETHER) {
            return "The Nether";
        } else if (dimension == Level.END) {
            return "The End";
        }
        // For custom dimensions, use the path
        return dimension.location().getPath();
    }

    /**
     * Creates an unlinked book with a link panel's properties.
     * Legacy: ItemLinkbookUnlinked.createItem()
     */
    public static ItemStack createItem(@NotNull ItemStack linkpanel, @NotNull ItemStack covermat) {
        ItemStack linkbook = new ItemStack(ModItems.LINKBOOK_UNLINKED.get());
        CompoundTag prev = linkpanel.getTag();
        if (prev == null) {
            prev = new CompoundTag();
        }
        linkbook.setTag(prev.copy());
        return linkbook;
    }
}
