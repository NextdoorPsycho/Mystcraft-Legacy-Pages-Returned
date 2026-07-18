package art.arcane.mystcraft.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Bridges Mystcraft's legacy compound payloads onto the Minecraft 26.2 item
 * component API.
 *
 * <p>The returned compounds are detached copies. A caller that mutates a value
 * returned by {@link #getOrCreateTag(ItemStack)} must finish by calling
 * {@link #setTag(ItemStack, CompoundTag)}.</p>
 */
public final class ItemStackNbt {

  private ItemStackNbt() {
  }

  public static CompoundTag getTag(ItemStack stack) {
    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
    return customData == null ? null : customData.copyTag();
  }

  public static CompoundTag getOrCreateTag(ItemStack stack) {
    CompoundTag tag = getTag(stack);
    return tag == null ? new CompoundTag() : tag;
  }

  public static void setTag(ItemStack stack, CompoundTag tag) {
    if (tag == null) {
      stack.remove(DataComponents.CUSTOM_DATA);
      return;
    }
    CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
  }

  public static void setHoverName(ItemStack stack, Component name) {
    stack.set(DataComponents.CUSTOM_NAME, name);
  }

  public static boolean hasCustomHoverName(ItemStack stack) {
    return stack.has(DataComponents.CUSTOM_NAME);
  }

  public static CompoundTag save(ItemStack stack) {
    Tag encoded = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow();
    if (encoded instanceof CompoundTag compoundTag) {
      return compoundTag;
    }
    throw new IllegalStateException("Minecraft encoded an item stack as non-compound NBT");
  }

  public static ItemStack load(CompoundTag tag) {
    return ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
  }

  public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
    return ItemStack.isSameItemSameComponents(a, b);
  }
}
