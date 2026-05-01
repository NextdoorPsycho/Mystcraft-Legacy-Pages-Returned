package art.arcane.mystcraft.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection-based access to client-only classes to keep common code
 * server-safe.
 */
public final class ClientAccess {

  private static Method getInstanceMethod;
  private static Field levelField;

  private ClientAccess() {
  }

  @Nullable
  public static Object getMinecraftInstance() {
    try {
      Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
      if (getInstanceMethod == null) {
        getInstanceMethod = ReflectionCompat.findMethod(mcClass, "getInstance", mcClass);
      }
      return getInstanceMethod.invoke(null);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  @Nullable
  public static Object getClientLevel() {
    Object minecraft = getMinecraftInstance();
    if (minecraft == null) {
      return null;
    }
    try {
      if (levelField == null) {
        Class<?> levelType = ReflectionCompat.getClassIfPresent("net.minecraft.client.multiplayer.ClientLevel");
        levelField = ReflectionCompat.findField(minecraft.getClass(), "level", levelType);
        if (levelField == null && levelType != null) {
          levelField = ReflectionCompat.findFieldByType(minecraft.getClass(), levelType);
        }
        if (levelField == null) {
          return null;
        }
      }
      return levelField.get(minecraft);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }
}
