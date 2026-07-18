package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.platform.services.IEventHelper;
import net.minecraft.server.level.ServerLevel;

/**
 * Forge event bridge. Event registration is deliberately centralized here so
 * the entrypoint only owns lifecycle ordering.
 */
public final class ForgeEventHelper implements IEventHelper {

  @Override
  public void registerServerEvents() {
    ForgeEvents.registerServerEvents();
  }

  @Override
  public void registerClientEvents() {
    ForgeEvents.registerClientEvents();
  }

  @Override
  public void registerCommonEvents() {
    ForgeEvents.registerCommonEvents();
  }

  @Override
  public void fireLevelLoadEvent(ServerLevel level) {
    ForgeEvents.fireLevelLoad(level);
  }
}
