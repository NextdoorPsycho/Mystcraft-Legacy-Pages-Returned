package art.arcane.mystcraft.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * ItemStack NBT helpers for the supported Minecraft 1.20.1 API.
 */
public final class ItemStackNbt {

  private ItemStackNbt() {
  }

  public static CompoundTag getTag(ItemStack stack) {
    return stack.getTag();
  }

  public static CompoundTag getOrCreateTag(ItemStack stack) {
    return stack.getOrCreateTag();
  }

  public static void setTag(ItemStack stack, CompoundTag tag) {
    stack.setTag(tag);
  }

  public static void setHoverName(ItemStack stack, Component name) {
    stack.setHoverName(name);
  }

  public static boolean hasCustomHoverName(ItemStack stack) {
    return stack.hasCustomHoverName();
  }

  public static CompoundTag save(ItemStack stack) {
    return stack.save(new CompoundTag());
  }

  public static ItemStack load(CompoundTag tag) {
    return ItemStack.of(tag);
  }

  public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
    return ItemStack.isSameItemSameTags(a, b);
  }
}
