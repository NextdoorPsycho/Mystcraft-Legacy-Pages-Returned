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
 * Cross-version ItemStack NBT helpers.
 * Handles classic ItemStack tag APIs (1.20.1/1.20.2) and data components (1.20.5+).
 */
public final class ItemStackNbt {

  // Check if we're on the component system (1.20.5+) by looking for DataComponents class
  private static final boolean USE_COMPONENTS = getClassIfPresent("net.minecraft.core.component.DataComponents") != null;

  // Reflection fallbacks for component system (1.20.5+)
  private static final Object CUSTOM_DATA_TYPE = USE_COMPONENTS ? getStaticField("net.minecraft.core.component.DataComponents", "CUSTOM_DATA") : null;
  private static final Object CUSTOM_NAME_TYPE = USE_COMPONENTS ? getStaticField("net.minecraft.core.component.DataComponents", "CUSTOM_NAME") : null;
  private static final Method STACK_GET_COMPONENT = USE_COMPONENTS ? ReflectionCompat.findMethodBySignature(ItemStack.class, Object.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType")) : null;
  private static final Method STACK_SET_COMPONENT = USE_COMPONENTS ? ReflectionCompat.findMethodBySignature(ItemStack.class, Object.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"),
      Object.class
  ) : null;
  private static final Method CUSTOM_DATA_COPY_TAG = USE_COMPONENTS ? ReflectionCompat.findMethodBySignature(
      getClassIfPresent("net.minecraft.world.item.component.CustomData"),
      CompoundTag.class) : null;
  private static final Method CUSTOM_DATA_SET = USE_COMPONENTS ? ReflectionCompat.findMethod(
      getClassIfPresent("net.minecraft.world.item.component.CustomData"),
      "set", void.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"),
      ItemStack.class,
      CompoundTag.class
  ) : null;

  // Reflection fallbacks for save/load (signature-based to handle obfuscation)
  private static final Method STACK_SAVE_PROVIDER_TAG = ReflectionCompat.findMethodBySignature(ItemStack.class, Tag.class, HolderLookup.Provider.class, Tag.class);
  private static final Method STACK_SAVE_PROVIDER = ReflectionCompat.findMethodBySignature(ItemStack.class, Tag.class, HolderLookup.Provider.class);
  private static final Method STACK_SAVE_OLD = ReflectionCompat.findMethodBySignature(ItemStack.class, CompoundTag.class, CompoundTag.class);
  private static final Method STACK_PARSE_OPTIONAL = ReflectionCompat.findMethod(ItemStack.class, "parseOptional", ItemStack.class, HolderLookup.Provider.class, CompoundTag.class);
  private static final Method STACK_OF_OLD = ReflectionCompat.findMethod(ItemStack.class, "of", ItemStack.class, CompoundTag.class);

  private ItemStackNbt() {
  }

  public static HolderLookup.Provider defaultProvider() {
    return RegistryAccess.EMPTY;
  }

  public static CompoundTag getTag(ItemStack stack) {
    // For 1.20.1-1.20.4, use direct method call (works with obfuscation via reobfJar)
    if (!USE_COMPONENTS) {
      return stack.getTag();
    }
    // For 1.20.5+, use component system via reflection
    Object customData = getCustomData(stack);
    if (customData == null) {
      return null;
    }
    CompoundTag tag = invoke(CUSTOM_DATA_COPY_TAG, customData);
    return tag;
  }

  public static CompoundTag getOrCreateTag(ItemStack stack) {
    // For 1.20.1-1.20.4, use direct method call
    if (!USE_COMPONENTS) {
      return stack.getOrCreateTag();
    }
    // For 1.20.5+, manual implementation
    CompoundTag tag = getTag(stack);
    if (tag == null) {
      tag = new CompoundTag();
      // Don't call setTag here - callers are responsible for persisting after modifications.
    }
    return tag;
  }

  public static void setTag(ItemStack stack, CompoundTag tag) {
    // For 1.20.1-1.20.4, use direct method call
    if (!USE_COMPONENTS) {
      stack.setTag(tag);
      return;
    }
    // For 1.20.5+, use component system via reflection
    if (CUSTOM_DATA_SET != null && CUSTOM_DATA_TYPE != null) {
      invokeVoid(CUSTOM_DATA_SET, null, CUSTOM_DATA_TYPE, stack, tag);
    }
  }

  public static void setHoverName(ItemStack stack, Component name) {
    // Direct call works for all versions - method exists in both old and new APIs
    stack.setHoverName(name);
  }

  public static boolean hasCustomHoverName(ItemStack stack) {
    // Direct call works for all versions
    return stack.hasCustomHoverName();
  }

  public static CompoundTag save(ItemStack stack) {
    return save(stack, defaultProvider());
  }

  public static CompoundTag save(ItemStack stack, HolderLookup.Provider provider) {
    // For 1.20.1-1.20.4, use the old save(CompoundTag) method
    if (!USE_COMPONENTS) {
      return stack.save(new CompoundTag());
    }
    // For 1.20.5+, try newer save methods via reflection
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
    // For 1.20.1-1.20.4, use ItemStack.of(CompoundTag)
    if (!USE_COMPONENTS) {
      return ItemStack.of(tag);
    }
    // For 1.20.5+, try newer parse methods via reflection
    if (STACK_PARSE_OPTIONAL != null) {
      ItemStack parsed = invoke(STACK_PARSE_OPTIONAL, null, provider, tag);
      return parsed != null ? parsed : ItemStack.EMPTY;
    }
    return ItemStack.EMPTY;
  }

  public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
    // For 1.20.1-1.20.4, use isSameItemSameTags
    if (!USE_COMPONENTS) {
      return ItemStack.isSameItemSameTags(a, b);
    }
    // For 1.20.5+, use isSameItemSameComponents via reflection (method renamed)
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
