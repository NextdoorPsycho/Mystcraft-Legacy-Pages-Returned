package art.arcane.mystcraft.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.CommonListenerCookie;

/**
 * Creates the 26.2 connection cookie used by the GameTest player harness.
 */
public final class CommonListenerCookieCompat {

  private CommonListenerCookieCompat() {
  }

  public static CommonListenerCookie createInitial(GameProfile profile) {
    return CommonListenerCookie.createInitial(profile, false);
  }
}
