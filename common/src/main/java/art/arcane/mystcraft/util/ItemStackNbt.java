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

  private static final Method STACK_GET_TAG = findMethod(ItemStack.class, "getTag", CompoundTag.class);
  private static final Method STACK_SET_TAG = findMethod(ItemStack.class, "setTag", void.class, CompoundTag.class);
  private static final Method STACK_SET_HOVER_NAME = findMethod(ItemStack.class, "setHoverName", void.class, Component.class);
  private static final Method STACK_HAS_CUSTOM_HOVER_NAME = findMethod(ItemStack.class, "hasCustomHoverName", boolean.class);
  private static final Method STACK_SAVE_OLD = findMethod(ItemStack.class, "save", CompoundTag.class, CompoundTag.class);
  private static final Method STACK_SAVE_PROVIDER_TAG = findMethod(ItemStack.class, "save", Tag.class, HolderLookup.Provider.class, Tag.class);
  private static final Method STACK_SAVE_PROVIDER = findMethod(ItemStack.class, "save", Tag.class, HolderLookup.Provider.class);
  private static final Method STACK_PARSE_OPTIONAL = findMethod(ItemStack.class, "parseOptional", ItemStack.class, HolderLookup.Provider.class, CompoundTag.class);
  private static final Method STACK_OF_OLD = findMethod(ItemStack.class, "of", ItemStack.class, CompoundTag.class);
  private static final Method STACK_SAME_TAGS = findMethod(ItemStack.class, "isSameItemSameTags", boolean.class, ItemStack.class, ItemStack.class);
  private static final Method STACK_SAME_COMPONENTS = findMethod(ItemStack.class, "isSameItemSameComponents", boolean.class, ItemStack.class, ItemStack.class);

  private static final Object CUSTOM_DATA_TYPE = getStaticField("net.minecraft.core.component.DataComponents", "CUSTOM_DATA");
  private static final Object CUSTOM_NAME_TYPE = getStaticField("net.minecraft.core.component.DataComponents", "CUSTOM_NAME");
  private static final Method STACK_GET_COMPONENT = findMethod(ItemStack.class, "get", Object.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"));
  private static final Method STACK_SET_COMPONENT = findMethod(ItemStack.class, "set", void.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"),
      Object.class
  );
  private static final Method CUSTOM_DATA_COPY_TAG = findMethod(getClassIfPresent("net.minecraft.world.item.component.CustomData"),
      "copyTag", CompoundTag.class);
  private static final Method CUSTOM_DATA_SET = findMethod(getClassIfPresent("net.minecraft.world.item.component.CustomData"),
      "set", void.class,
      getClassIfPresent("net.minecraft.core.component.DataComponentType"),
      ItemStack.class,
      CompoundTag.class
  );

  private ItemStackNbt() {
  }

  public static HolderLookup.Provider defaultProvider() {
    return RegistryAccess.EMPTY;
  }

  public static CompoundTag getTag(ItemStack stack) {
    if (STACK_GET_TAG != null) {
      return invoke(STACK_GET_TAG, stack);
    }
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
      // Don't call setTag here - callers are responsible for persisting after modifications.
      // Calling setTag here causes infinite recursion when Minecraft calls setDamage during
      // ItemStack deserialization, as setTag triggers NBT processing which calls setDamage again.
    }
    return tag;
  }

  public static void setTag(ItemStack stack, CompoundTag tag) {
    if (STACK_SET_TAG != null) {
      invokeVoid(STACK_SET_TAG, stack, tag);
      return;
    }
    if (CUSTOM_DATA_SET != null && CUSTOM_DATA_TYPE != null) {
      invokeVoid(CUSTOM_DATA_SET, null, CUSTOM_DATA_TYPE, stack, tag);
    }
  }

  public static void setHoverName(ItemStack stack, Component name) {
    if (STACK_SET_HOVER_NAME != null) {
      invokeVoid(STACK_SET_HOVER_NAME, stack, name);
      return;
    }
    if (STACK_SET_COMPONENT != null && CUSTOM_NAME_TYPE != null) {
      invokeVoid(STACK_SET_COMPONENT, stack, CUSTOM_NAME_TYPE, name);
    }
  }

  public static boolean hasCustomHoverName(ItemStack stack) {
    if (STACK_HAS_CUSTOM_HOVER_NAME != null) {
      Boolean result = invoke(STACK_HAS_CUSTOM_HOVER_NAME, stack);
      return result != null && result;
    }
    if (STACK_GET_COMPONENT != null && CUSTOM_NAME_TYPE != null) {
      Object customName = invoke(STACK_GET_COMPONENT, stack, CUSTOM_NAME_TYPE);
      return customName != null;
    }
    return false;
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
    if (STACK_SAVE_OLD != null) {
      return invoke(STACK_SAVE_OLD, stack, new CompoundTag());
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
    if (STACK_OF_OLD != null) {
      ItemStack parsed = invoke(STACK_OF_OLD, null, tag);
      return parsed != null ? parsed : ItemStack.EMPTY;
    }
    return ItemStack.EMPTY;
  }

  public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
    if (STACK_SAME_TAGS != null) {
      Boolean result = invoke(STACK_SAME_TAGS, null, a, b);
      return result != null && result;
    }
    if (STACK_SAME_COMPONENTS != null) {
      Boolean result = invoke(STACK_SAME_COMPONENTS, null, a, b);
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

  private static Method findMethod(Class<?> owner, String name, Class<?> returnType, Class<?>... params) {
    if (owner == null) {
      return null;
    }
    if (name != null) {
      try {
        Method method = owner.getMethod(name, params);
        if (returnType == null || returnType.isAssignableFrom(method.getReturnType())) {
          return method;
        }
      } catch (NoSuchMethodException ignored) {
        // Fall through to signature-based lookup.
      }
    }
    for (Method method : owner.getMethods()) {
      if (params.length != method.getParameterCount()) {
        continue;
      }
      if (returnType != null && !returnType.isAssignableFrom(method.getReturnType())) {
        continue;
      }
      Class<?>[] methodParams = method.getParameterTypes();
      boolean matches = true;
      for (int i = 0; i < params.length; i++) {
        if (params[i] == null || !methodParams[i].isAssignableFrom(params[i])) {
          matches = false;
          break;
        }
      }
      if (matches) {
        return method;
      }
    }
    return null;
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
