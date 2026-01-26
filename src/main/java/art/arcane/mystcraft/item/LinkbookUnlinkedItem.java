package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * The Unlinked Linkbook item.
 * A blank linkbook that can be linked to the current location.
 * When used, converts to a linked linkbook at the current position.
 */
public class LinkbookUnlinkedItem extends Item {

    public LinkbookUnlinkedItem(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Create a linked linkbook at the current position
            ItemStack linkedBook = createLinkedBook(level, player);

            // Replace the unlinked book with the linked one
            stack.shrink(1);

            if (stack.isEmpty()) {
                player.setItemInHand(hand, linkedBook);
            } else {
                // Add to inventory or drop
                if (!player.getInventory().add(linkedBook)) {
                    player.drop(linkedBook, false);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Creates a linked linkbook at the player's current position.
     */
    private ItemStack createLinkedBook(Level level, Player player) {
        ItemStack linkedBook = new ItemStack(ModItems.LINKBOOK.get());
        CompoundTag tag = new CompoundTag();

        // Set spawn position to player's current position
        LinkOptions.setSpawn(tag, player.blockPosition());
        LinkOptions.setSpawnYaw(tag, player.getYRot());

        // Set dimension
        int dimId = getDimensionId(level);
        LinkOptions.setDimensionUID(tag, dimId);

        // Set a default name based on position
        String defaultName = String.format("Link (%d, %d, %d)",
                player.getBlockX(), player.getBlockY(), player.getBlockZ());
        LinkOptions.setDisplayName(tag, defaultName);

        linkedBook.setTag(tag);
        return linkedBook;
    }

    /**
     * Gets a numeric dimension ID from a level.
     */
    private int getDimensionId(Level level) {
        ResourceKey<Level> dimension = level.dimension();
        if (dimension == Level.OVERWORLD) {
            return 0;
        } else if (dimension == Level.NETHER) {
            return -1;
        } else if (dimension == Level.END) {
            return 1;
        }
        return dimension.location().hashCode();
    }
}
