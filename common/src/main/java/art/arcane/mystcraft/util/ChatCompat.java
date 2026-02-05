package art.arcane.mystcraft.util;

import net.minecraft.SharedConstants;

import java.lang.reflect.Method;

public final class ChatCompat {

  private static final Method ALLOWED_CHAR_INT = findMethod("isAllowedChatCharacter", int.class);
  private static final Method ALLOWED_CHAR_CHAR = findMethod("isAllowedChatCharacter", char.class);

  private ChatCompat() {
  }

  public static boolean isAllowedChatCharacter(int codePoint) {
    if (ALLOWED_CHAR_INT != null) {
      Boolean result = invoke(ALLOWED_CHAR_INT, codePoint);
      return result != null && result;
    }
    if (ALLOWED_CHAR_CHAR != null) {
      char value = (char) codePoint;
      Boolean result = invoke(ALLOWED_CHAR_CHAR, value);
      return result != null && result;
    }
    return false;
  }

  private static Method findMethod(String name, Class<?>... params) {
    try {
      return SharedConstants.class.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Method method, Object... args) {
    try {
      return (T) method.invoke(null, args);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }
}
