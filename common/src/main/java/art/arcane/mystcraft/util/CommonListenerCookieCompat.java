package art.arcane.mystcraft.util;

import com.mojang.authlib.GameProfile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class CommonListenerCookieCompat {

  private CommonListenerCookieCompat() {
  }

  public static Object createInitial(GameProfile profile) {
    Class<?> cookieClass = getClassIfPresent("net.minecraft.server.network.CommonListenerCookie");
    if (cookieClass == null) {
      return null;
    }
    Method createInitialBoolean = findMethod(cookieClass, "createInitial", GameProfile.class, boolean.class);
    Method createInitial = findMethod(cookieClass, "createInitial", GameProfile.class);
    if (createInitialBoolean != null) {
      Object cookie = invoke(createInitialBoolean, profile, false);
      if (cookie != null) {
        return cookie;
      }
    }
    if (createInitial != null) {
      Object cookie = invoke(createInitial, profile);
      if (cookie != null) {
        return cookie;
      }
    }
    return createViaConstructor(profile, cookieClass);
  }

  private static Object createViaConstructor(GameProfile profile, Class<?> cookieClass) {
    Object clientInfo = createDefaultClientInfo();
    Object connectionType = defaultConnectionType();
    for (Constructor<?> ctor : cookieClass.getConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      try {
        if (params.length == 4
            && params[0] == GameProfile.class
            && params[1] == int.class
            && clientInfo != null
            && params[2].isInstance(clientInfo)
            && params[3] == boolean.class) {
          return ctor.newInstance(profile, 0, clientInfo, false);
        }
        if (params.length == 5
            && params[0] == GameProfile.class
            && params[1] == int.class
            && clientInfo != null
            && params[2].isInstance(clientInfo)
            && params[3] == boolean.class
            && connectionType != null
            && params[4].isInstance(connectionType)) {
          return ctor.newInstance(profile, 0, clientInfo, false, connectionType);
        }
      } catch (ReflectiveOperationException ignored) {
      }
    }
    return null;
  }

  private static Object createDefaultClientInfo() {
    Class<?> infoClass = getClassIfPresent("net.minecraft.network.protocol.game.ClientInformation");
    if (infoClass == null) {
      return null;
    }
    try {
      Method createDefault = infoClass.getMethod("createDefault");
      return createDefault.invoke(null);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static Object defaultConnectionType() {
    Class<?> typeClass = getClassIfPresent("net.minecraft.network.ConnectionType");
    if (typeClass == null) {
      return null;
    }
    Object[] constants = typeClass.getEnumConstants();
    return constants != null && constants.length > 0 ? constants[0] : null;
  }

  private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
    try {
      return owner.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Method method, Object... args) {
    if (method == null) {
      return null;
    }
    try {
      return (T) method.invoke(null, args);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static Class<?> getClassIfPresent(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
