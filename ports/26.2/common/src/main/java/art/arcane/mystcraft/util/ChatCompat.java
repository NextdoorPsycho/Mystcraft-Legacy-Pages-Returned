package art.arcane.mystcraft.util;

import net.minecraft.util.StringUtil;

/**
 * Shared access to the 26.2 chat character validation rule.
 */
public final class ChatCompat {

  private ChatCompat() {
  }

  public static boolean isAllowedChatCharacter(int codePoint) {
    return StringUtil.isAllowedChatCharacter(codePoint);
  }
}
