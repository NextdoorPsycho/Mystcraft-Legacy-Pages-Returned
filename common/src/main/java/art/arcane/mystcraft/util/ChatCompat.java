package art.arcane.mystcraft.util;

import net.minecraft.SharedConstants;

import java.lang.reflect.Method;

/**
 * Compatibility wrapper for chat character validation methods that changed across 1.20.x mappings.
 */
public final class ChatCompat {

  private static final Method ALLOWED_CHAR_INT =
      ReflectionCompat.findMethod(SharedConstants.class, "isAllowedChatCharacter", boolean.class, int.class);
  private static final Method ALLOWED_CHAR_CHAR =
      ReflectionCompat.findMethod(SharedConstants.class, "isAllowedChatCharacter", boolean.class, char.class);

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

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Method method, Object... args) {
    try {
      return (T) method.invoke(null, args);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }
}
