package art.arcane.mystcraft.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Small reflection helpers that favor stable signatures over names.
 */
public final class ReflectionCompat {

  private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<>();

  static {
    PRIMITIVE_WRAPPERS.put(boolean.class, Boolean.class);
    PRIMITIVE_WRAPPERS.put(byte.class, Byte.class);
    PRIMITIVE_WRAPPERS.put(short.class, Short.class);
    PRIMITIVE_WRAPPERS.put(int.class, Integer.class);
    PRIMITIVE_WRAPPERS.put(long.class, Long.class);
    PRIMITIVE_WRAPPERS.put(float.class, Float.class);
    PRIMITIVE_WRAPPERS.put(double.class, Double.class);
    PRIMITIVE_WRAPPERS.put(char.class, Character.class);
    PRIMITIVE_WRAPPERS.put(void.class, Void.class);
  }

  private ReflectionCompat() {
  }

  public static Class<?> getClassIfPresent(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static Method findMethod(Class<?> owner, String name, Class<?> returnType, Class<?>... params) {
    if (owner == null) {
      return null;
    }
    if (name != null && params != null && !hasNull(params)) {
      try {
        Method method = owner.getMethod(name, params);
        if (returnType == null || isReturnCompatible(returnType, method.getReturnType())) {
          return method;
        }
      } catch (NoSuchMethodException ignored) {
        // Fall through to signature-based lookup.
      }
    }
    return findMethodBySignature(owner, returnType, params);
  }

  public static Method findMethodBySignature(Class<?> owner, Class<?> returnType, Class<?>... params) {
    if (owner == null) {
      return null;
    }
    Method[] methods = owner.getMethods();
    for (Method method : methods) {
      if (!parametersMatch(method.getParameterTypes(), params)) {
        continue;
      }
      if (returnType != null && !isReturnCompatible(returnType, method.getReturnType())) {
        continue;
      }
      return method;
    }
    return null;
  }

  public static Field findField(Class<?> owner, String name, Class<?> type) {
    if (owner == null) {
      return null;
    }
    if (name != null) {
      try {
        Field field = owner.getField(name);
        if (type == null || isReturnCompatible(type, field.getType())) {
          return field;
        }
      } catch (NoSuchFieldException ignored) {
        // Fall through.
      }
      try {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        if (type == null || isReturnCompatible(type, field.getType())) {
          return field;
        }
      } catch (NoSuchFieldException ignored) {
      }
    }
    return findFieldByType(owner, type);
  }

  public static Field findFieldByType(Class<?> owner, Class<?> type) {
    if (owner == null || type == null) {
      return null;
    }
    for (Field field : owner.getDeclaredFields()) {
      if (isReturnCompatible(type, field.getType())) {
        field.setAccessible(true);
        return field;
      }
    }
    for (Field field : owner.getFields()) {
      if (isReturnCompatible(type, field.getType())) {
        return field;
      }
    }
    return null;
  }

  private static boolean parametersMatch(Class<?>[] methodParams, Class<?>[] desiredParams) {
    if (desiredParams == null) {
      return methodParams.length == 0;
    }
    if (methodParams.length != desiredParams.length) {
      return false;
    }
    for (int i = 0; i < desiredParams.length; i++) {
      Class<?> desired = desiredParams[i];
      if (desired == null) {
        return false;
      }
      if (!isParamCompatible(methodParams[i], desired)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isParamCompatible(Class<?> methodParam, Class<?> desiredParam) {
    if (methodParam.isPrimitive()) {
      Class<?> wrapped = PRIMITIVE_WRAPPERS.get(methodParam);
      return wrapped != null && wrapped.isAssignableFrom(desiredParam);
    }
    return methodParam.isAssignableFrom(desiredParam);
  }

  private static boolean isReturnCompatible(Class<?> expected, Class<?> actual) {
    if (actual.isPrimitive()) {
      Class<?> wrapped = PRIMITIVE_WRAPPERS.get(actual);
      return wrapped != null && expected.isAssignableFrom(wrapped);
    }
    return expected.isAssignableFrom(actual);
  }

  private static boolean hasNull(Class<?>[] params) {
    for (Class<?> param : params) {
      if (param == null) {
        return true;
      }
    }
    return false;
  }
}
