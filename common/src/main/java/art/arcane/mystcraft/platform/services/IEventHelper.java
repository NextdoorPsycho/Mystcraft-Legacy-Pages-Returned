package art.arcane.mystcraft.platform.services;

import net.minecraft.server.level.ServerLevel;

/**
 * Abstracts platform-specific event registration.
 * Each platform implements this to hook into its own event system.
 */
public interface IEventHelper {

  /**
   * Registers all server-side event handlers (tick, level load, commands, etc.).
   */
  void registerServerEvents();

  /**
   * Registers all client-side event handlers (rendering, overlays, colors, etc.).
   */
  void registerClientEvents();

  /**
   * Registers all common event handlers (player events, entity events, etc.).
   */
  void registerCommonEvents();

  /**
   * Fires a platform-specific level load event for a dynamically created dimension.
   */
  void fireLevelLoadEvent(ServerLevel level);
}
