package art.arcane.mystcraft.advancements;

import art.arcane.mystcraft.platform.Services;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers Mystcraft custom advancement criteria triggers.
 * Uses version-specific factory via Services for cross-version compatibility.
 */
public final class ModAdvancements {

  private ModAdvancements() {
  }

  /**
   * Registers all custom criteria triggers with the vanilla registry.
   * Delegates to version-specific factory loaded via ServiceLoader.
   * Must be called during common setup.
   */
  public static void register() {
    Services.ADVANCEMENTS.registerTriggers();
  }

  /**
   * Triggers the WritingDeskWrite advancement for a player.
   * Uses reflection to avoid direct class references that may not exist in all versions.
   */
  public static void triggerWritingDeskWrite(ServerPlayer player) {
    Object trigger = Services.ADVANCEMENTS.getWritingDeskWriteTrigger();
    if (trigger == null) return;
    triggerViaReflection(trigger, player);
  }

  /**
   * Triggers the EnterMystDimensionSafe advancement for a player.
   * Uses reflection to avoid direct class references that may not exist in all versions.
   */
  public static void triggerEnterMystDimensionSafe(ServerPlayer player) {
    Object trigger = Services.ADVANCEMENTS.getEnterMystDimensionSafeTrigger();
    if (trigger == null) return;
    triggerViaReflection(trigger, player);
  }

  /**
   * Triggers the EnterMystDimensionQuinn advancement for a player.
   * Uses reflection to avoid direct class references that may not exist in all versions.
   */
  public static void triggerEnterMystDimensionQuinn(ServerPlayer player) {
    Object trigger = Services.ADVANCEMENTS.getEnterMystDimensionQuinnTrigger();
    if (trigger == null) return;
    triggerViaReflection(trigger, player);
  }

  /**
   * Invokes the trigger method via reflection to avoid compile-time class dependencies.
   */
  private static void triggerViaReflection(Object trigger, ServerPlayer player) {
    try {
      trigger.getClass().getMethod("trigger", ServerPlayer.class).invoke(trigger, player);
    } catch (Exception e) {
      // Silently ignore - trigger not available in this version
    }
  }
}
