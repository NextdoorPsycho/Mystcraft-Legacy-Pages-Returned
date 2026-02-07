package art.arcane.mystcraft.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 1.20.6-specific ItemStack NBT helpers using data components.
 * All tag-based APIs (getTag, setTag, etc.) were removed in 1.20.5+.
 * This version only uses the component system via reflection.
 */
public final class ItemStackNbt {

  private static final Object CUSTOM_DATA_TYPE = getStaticField("net.minecraft.core.component.DataComponents", "CUSTOM_DATA");
  private static final Object CUSTOM_NAME_TYPE = getStaticField("net.minecraft.core.component.DataComponents", "CUSTOM_NAME");
  private static final Method STACK_GET_COMPONENT = ReflectionCompat.findMethodBySignature(ItemStack.class, Object.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"));
  private static final Method STACK_SET_COMPONENT = ReflectionCompat.findMethodBySignature(ItemStack.class, Object.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"),
      Object.class
  );
  private static final Method CUSTOM_DATA_COPY_TAG = ReflectionCompat.findMethodBySignature(
      getClassIfPresent("net.minecraft.world.item.component.CustomData"),
      CompoundTag.class);
  private static final Method CUSTOM_DATA_SET = ReflectionCompat.findMethod(
      getClassIfPresent("net.minecraft.world.item.component.CustomData"),
      "set", void.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"),
      ItemStack.class,
      CompoundTag.class
  );

  private static final Method STACK_SAVE_PROVIDER_TAG = ReflectionCompat.findMethodBySignature(ItemStack.class, Tag.class, HolderLookup.Provider.class, Tag.class);
  private static final Method STACK_SAVE_PROVIDER = ReflectionCompat.findMethodBySignature(ItemStack.class, Tag.class, HolderLookup.Provider.class);
  private static final Method STACK_PARSE_OPTIONAL = ReflectionCompat.findMethod(ItemStack.class, "parseOptional", ItemStack.class, HolderLookup.Provider.class, CompoundTag.class);

  private ItemStackNbt() {
  }

  public static HolderLookup.Provider defaultProvider() {
    return RegistryAccess.EMPTY;
  }

  public static CompoundTag getTag(ItemStack stack) {
    Object customData = getCustomData(stack);
    if (customData == null) {
      return null;
    }
    CompoundTag tag = invoke(CUSTOM_DATA_COPY_TAG, customData);
    return tag;
  }

  public static CompoundTag getOrCreateTag(ItemStack stack) {
    CompoundTag tag = getTag(stack);
    if (tag == null) {
      tag = new CompoundTag();
    }
    return tag;
  }

  public static void setTag(ItemStack stack, CompoundTag tag) {
    if (CUSTOM_DATA_SET != null && CUSTOM_DATA_TYPE != null) {
      invokeVoid(CUSTOM_DATA_SET, null, CUSTOM_DATA_TYPE, stack, tag);
    }
  }

  public static void setHoverName(ItemStack stack, Component name) {
    if (STACK_SET_COMPONENT != null && CUSTOM_NAME_TYPE != null) {
      invoke(STACK_SET_COMPONENT, stack, CUSTOM_NAME_TYPE, name);
    }
  }

  public static boolean hasCustomHoverName(ItemStack stack) {
    if (STACK_GET_COMPONENT == null || CUSTOM_NAME_TYPE == null) {
      return false;
    }
    return invoke(STACK_GET_COMPONENT, stack, CUSTOM_NAME_TYPE) != null;
  }

  public static CompoundTag save(ItemStack stack) {
    return save(stack, defaultProvider());
  }

  public static CompoundTag save(ItemStack stack, HolderLookup.Provider provider) {
    if (STACK_SAVE_PROVIDER_TAG != null) {
      Tag tag = invoke(STACK_SAVE_PROVIDER_TAG, stack, provider, new CompoundTag());
      return tag instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    }
    if (STACK_SAVE_PROVIDER != null) {
      Tag tag = invoke(STACK_SAVE_PROVIDER, stack, provider);
      return tag instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    }
    return new CompoundTag();
  }

  public static ItemStack load(CompoundTag tag) {
    return load(tag, defaultProvider());
  }

  public static ItemStack load(CompoundTag tag, HolderLookup.Provider provider) {
    if (STACK_PARSE_OPTIONAL != null) {
      ItemStack parsed = invoke(STACK_PARSE_OPTIONAL, null, provider, tag);
      return parsed != null ? parsed : ItemStack.EMPTY;
    }
    return ItemStack.EMPTY;
  }

  public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
    Method sameComponents = ReflectionCompat.findMethodBySignature(ItemStack.class, boolean.class, ItemStack.class, ItemStack.class);
    if (sameComponents != null) {
      Boolean result = invoke(sameComponents, null, a, b);
      return result != null && result;
    }
    return ItemStack.isSameItem(a, b);
  }

  private static Object getCustomData(ItemStack stack) {
    if (STACK_GET_COMPONENT == null || CUSTOM_DATA_TYPE == null) {
      return null;
    }
    return invoke(STACK_GET_COMPONENT, stack, CUSTOM_DATA_TYPE);
  }

  private static Class<?> getClassIfPresent(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static Object getStaticField(String className, String fieldName) {
    Class<?> clazz = getClassIfPresent(className);
    if (clazz == null) {
      return null;
    }
    try {
      Field field = clazz.getField(fieldName);
      return field.get(null);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Method method, Object target, Object... args) {
    if (method == null) {
      return null;
    }
    try {
      return (T) method.invoke(target, args);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static void invokeVoid(Method method, Object target, Object... args) {
    if (method == null) {
      return;
    }
    try {
      method.invoke(target, args);
    } catch (ReflectiveOperationException ignored) {
    }
  }
}
