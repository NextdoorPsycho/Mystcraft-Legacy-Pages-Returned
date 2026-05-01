package art.arcane.mystcraft.client;

import art.arcane.mystcraft.client.screen.GuidebookScreen;
import net.minecraft.client.Minecraft;

/**
 * Client-side helper for opening the guidebook screen. This class is only
 * loaded via reflection from GuidebookItem when on the client side. It should
 * never be loaded on a dedicated server.
 */
public final class GuidebookClientHelper {

  private GuidebookClientHelper() {
  }

  public static void openGuidebook() {
    Minecraft.getInstance().setScreen(new GuidebookScreen());
  }
}
