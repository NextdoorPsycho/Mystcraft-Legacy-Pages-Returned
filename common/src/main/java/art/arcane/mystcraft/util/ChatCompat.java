package art.arcane.mystcraft.util;

import net.minecraft.SharedConstants;

/**
 * Shared access to the Minecraft 1.20.1 chat character validation rule.
 */
public final class ChatCompat {

  private ChatCompat() {
  }

  public static boolean isAllowedChatCharacter(int codePoint) {
    return codePoint >= Character.MIN_VALUE
        && codePoint <= Character.MAX_VALUE
        && SharedConstants.isAllowedChatCharacter((char) codePoint);
  }
}
