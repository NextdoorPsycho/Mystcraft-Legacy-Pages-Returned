package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.TickEvent;

/**
 * Client-side events for NeoForge to sync pocket head palettes.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientNetworkEvents {

  private NeoForgeClientNetworkEvents() {
  }

  @SubscribeEvent
  public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
    PocketHeadClientSync.requestSend();
  }

  @SubscribeEvent
  public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
    PocketHeadClientSync.reset();
  }

  @SubscribeEvent
  public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    PocketHeadClientSync.tick();
  }
}
