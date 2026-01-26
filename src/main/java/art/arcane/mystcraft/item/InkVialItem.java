package art.arcane.mystcraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Ink Vial item.
 * Contains ink for use in writing desks.
 * Can hold different colors of ink.
 */
public class InkVialItem extends Item {

    private static final String TAG_INK_AMOUNT = "InkAmount";
    private static final String TAG_INK_COLOR = "InkColor";
    public static final int MAX_INK = 100;
    public static final int DEFAULT_COLOR = 0x000000; // Black

    public InkVialItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        int amount = getInkAmount(stack);
        int color = getInkColor(stack);

        if (amount > 0) {
            tooltip.add(Component.translatable("item.mystcraft.inkvial.amount", amount, MAX_INK));
            String colorHex = String.format("#%06X", color);
            tooltip.add(Component.translatable("item.mystcraft.inkvial.color", colorHex));
        } else {
            tooltip.add(Component.translatable("item.mystcraft.inkvial.empty"));
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getInkAmount(stack) > 0 && getInkAmount(stack) < MAX_INK;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13.0F * getInkAmount(stack) / MAX_INK);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return getInkColor(stack);
    }

    /**
     * Gets the amount of ink remaining in this vial.
     */
    public int getInkAmount(ItemStack stack) {
        if (stack.getTag() == null) {
            return MAX_INK; // Full by default when new
        }
        CompoundTag tag = stack.getTag();
        if (!tag.contains(TAG_INK_AMOUNT)) {
            return MAX_INK;
        }
        return tag.getInt(TAG_INK_AMOUNT);
    }

    /**
     * Sets the amount of ink in this vial.
     */
    public void setInkAmount(ItemStack stack, int amount) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_INK_AMOUNT, Math.max(0, Math.min(MAX_INK, amount)));
    }

    /**
     * Consumes ink from this vial.
     *
     * @return true if there was enough ink
     */
    public boolean consumeInk(ItemStack stack, int amount) {
        int current = getInkAmount(stack);
        if (current < amount) {
            return false;
        }
        setInkAmount(stack, current - amount);
        return true;
    }

    /**
     * Gets the color of this ink.
     */
    public int getInkColor(ItemStack stack) {
        if (stack.getTag() == null) {
            return DEFAULT_COLOR;
        }
        CompoundTag tag = stack.getTag();
        if (!tag.contains(TAG_INK_COLOR)) {
            return DEFAULT_COLOR;
        }
        return tag.getInt(TAG_INK_COLOR);
    }

    /**
     * Sets the color of this ink.
     */
    public void setInkColor(ItemStack stack, int color) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_INK_COLOR, color & 0xFFFFFF);
    }

    /**
     * Checks if this vial is empty.
     */
    public boolean isEmpty(ItemStack stack) {
        return getInkAmount(stack) <= 0;
    }

    /**
     * Checks if this vial is full.
     */
    public boolean isFull(ItemStack stack) {
        return getInkAmount(stack) >= MAX_INK;
    }

    /**
     * Creates an ink vial with the specified color and amount.
     */
    public static ItemStack createVial(Item item, int color, int amount) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_INK_COLOR, color & 0xFFFFFF);
        tag.putInt(TAG_INK_AMOUNT, Math.max(0, Math.min(MAX_INK, amount)));
        stack.setTag(tag);
        return stack;
    }
}
