package art.arcane.mystcraft.item;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.link.LinkingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Linkbook item.
 * Links to a specific location in any dimension.
 * When used, teleports the player to the linked location.
 */
public class LinkbookItem extends Item {

    public LinkbookItem(Properties properties) {
        super(properties);
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
            BlockPos spawn = LinkOptions.getSpawn(stack.getTag());
            if (spawn != null) {
                tooltip.add(Component.translatable("item.mystcraft.linkbook.destination",
                        spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            Integer dimId = LinkOptions.getDimensionUID(stack.getTag());
            if (dimId != null) {
                tooltip.add(Component.translatable("item.mystcraft.linkbook.dimension", dimId));
            }
        }
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        // Initialize the linkbook if it has no data
        if (!level.isClientSide && stack.getTag() == null) {
            initializeLinkbook(stack, level, entity);
        }
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // Open book viewing screen on client
            if (level.isClientSide) {
                art.arcane.mystcraft.client.screen.BookScreen.open(stack);
            }
        } else {
            // Perform linking on server
            if (!level.isClientSide) {
                performLink(stack, level, player);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Initializes a linkbook with the current position.
     */
    private void initializeLinkbook(ItemStack stack, Level level, Entity entity) {
        CompoundTag tag = new CompoundTag();
        LinkOptions.setSpawn(tag, entity.blockPosition());
        LinkOptions.setSpawnYaw(tag, entity.getYRot());
        // Store dimension - in 1.20.2 dimensions are ResourceKey based
        // For now just store the dimension ID for overworld (0), nether (-1), end (1)
        int dimId = getDimensionId(level);
        LinkOptions.setDimensionUID(tag, dimId);
        stack.setTag(tag);
    }

    /**
     * Performs the linking teleportation.
     */
    private void performLink(ItemStack stack, Level level, Player player) {
        if (stack.getTag() == null) {
            player.displayClientMessage(Component.translatable("item.mystcraft.linkbook.invalid"), true);
            return;
        }

        BlockPos spawn = LinkOptions.getSpawn(stack.getTag());
        Integer dimId = LinkOptions.getDimensionUID(stack.getTag());

        if (spawn == null || dimId == null) {
            player.displayClientMessage(Component.translatable("item.mystcraft.linkbook.invalid"), true);
            return;
        }

        // Perform the actual link
        player.displayClientMessage(Component.translatable("item.mystcraft.linkbook.linking"), true);

        LinkingManager.LinkResult result = LinkingManager.performLink(player, stack.getTag());

        switch (result) {
            case SUCCESS -> {
                // Link successful - nothing more to do
            }
            case INVALID_DESTINATION -> player.displayClientMessage(
                    Component.translatable("item.mystcraft.linkbook.invalid"), true);
            case DIMENSION_NOT_FOUND -> player.displayClientMessage(
                    Component.literal("Dimension not found"), true);
            case BLOCKED -> player.displayClientMessage(
                    Component.literal("Link blocked"), true);
            default -> {
                // Handle other cases
            }
        }
    }

    /**
     * Gets a numeric dimension ID from a level.
     * Maps the vanilla dimensions to their classic IDs.
     */
    private int getDimensionId(Level level) {
        return LinkingManager.getDimensionUID(level);
    }

    /**
     * Sets the display name of the linkbook.
     */
    public void setDisplayName(ItemStack stack, String name) {
        LinkOptions.setDisplayName(stack.getOrCreateTag(), name);
    }

    /**
     * Gets the display name of the linkbook.
     */
    public String getDisplayName(ItemStack stack) {
        return LinkOptions.getDisplayName(stack.getTag());
    }

    /**
     * Gets the destination position.
     */
    @Nullable
    public BlockPos getDestination(ItemStack stack) {
        return LinkOptions.getSpawn(stack.getTag());
    }

    /**
     * Sets the destination position.
     */
    public void setDestination(ItemStack stack, BlockPos pos) {
        LinkOptions.setSpawn(stack.getOrCreateTag(), pos);
    }
}
