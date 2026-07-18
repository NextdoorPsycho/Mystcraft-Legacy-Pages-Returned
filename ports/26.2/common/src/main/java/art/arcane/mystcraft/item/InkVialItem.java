package art.arcane.mystcraft.item;

import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Ink Vial item. Contains ink for use in writing desks. Can hold different
 * colors of ink.
 */
public class InkVialItem extends Item implements TooltipCompat {

  public static final int MAX_INK = 100;
  public static final int DEFAULT_COLOR = 0x000000;
  private static final String TAG_INK_AMOUNT = "InkAmount";
  private static final String TAG_INK_COLOR = "InkColor";

  public InkVialItem(Properties properties) {
    super(properties);
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                              @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip,
                              @NotNull TooltipFlag flag) {
    int amount = getInkAmount(stack);
    int color = getInkColor(stack);

    if (amount > 0) {
      tooltip.accept(Component.translatable("item.mystcraft.inkvial.amount", amount, MAX_INK));
      String colorHex = String.format("#%06X", color);
      tooltip.accept(Component.translatable("item.mystcraft.inkvial.color", colorHex));
    } else {
      tooltip.accept(Component.translatable("item.mystcraft.inkvial.empty"));
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
    if (ItemStackNbt.getTag(stack) == null) {
      return MAX_INK;
    }
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (!tag.contains(TAG_INK_AMOUNT)) {
      return MAX_INK;
    }
    return tag.getIntOr(TAG_INK_AMOUNT, 0);
  }

  /**
   * Sets the amount of ink in this vial.
   */
  public void setInkAmount(ItemStack stack, int amount) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    tag.putInt(TAG_INK_AMOUNT, Math.max(0, Math.min(MAX_INK, amount)));
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Gets the color of this ink.
   */
  public int getInkColor(ItemStack stack) {
    if (ItemStackNbt.getTag(stack) == null) {
      return DEFAULT_COLOR;
    }
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (!tag.contains(TAG_INK_COLOR)) {
      return DEFAULT_COLOR;
    }
    return tag.getIntOr(TAG_INK_COLOR, 0);
  }

  /**
   * Sets the color of this ink.
   */
  public void setInkColor(ItemStack stack, int color) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    tag.putInt(TAG_INK_COLOR, color & 0xFFFFFF);
    ItemStackNbt.setTag(stack, tag);
  }

}
