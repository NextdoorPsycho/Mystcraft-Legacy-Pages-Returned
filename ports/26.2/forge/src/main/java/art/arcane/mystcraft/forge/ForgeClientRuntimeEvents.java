package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.client.PocketHeadClientSync;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

/** Client play-session hooks kept separate so dedicated servers never resolve client classes. */
final class ForgeClientRuntimeEvents {

  private ForgeClientRuntimeEvents() {
  }

  static void register() {
    ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event ->
        PocketHeadClientSync.requestSend());
    ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event ->
        PocketHeadClientSync.reset());
    TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
        PocketHeadClientSync.tick());
  }
}
