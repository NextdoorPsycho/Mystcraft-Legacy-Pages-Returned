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
   */
  public static void triggerWritingDeskWrite(ServerPlayer player) {
    Object trigger = Services.ADVANCEMENTS.getWritingDeskWriteTrigger();
    if (trigger instanceof WritingDeskWriteTrigger t) {
      t.trigger(player);
    } else {
      // Version-specific trigger, use reflection or cast appropriately
      try {
        trigger.getClass().getMethod("trigger", ServerPlayer.class).invoke(trigger, player);
      } catch (Exception e) {
        // Ignore - trigger failed
      }
    }
  }

  /**
   * Triggers the EnterMystDimensionSafe advancement for a player.
   */
  public static void triggerEnterMystDimensionSafe(ServerPlayer player) {
    Object trigger = Services.ADVANCEMENTS.getEnterMystDimensionSafeTrigger();
    if (trigger instanceof EnterMystDimensionSafeTrigger t) {
      t.trigger(player);
    } else {
      try {
        trigger.getClass().getMethod("trigger", ServerPlayer.class).invoke(trigger, player);
      } catch (Exception e) {
        // Ignore - trigger failed
      }
    }
  }

  /**
   * Triggers the EnterMystDimensionQuinn advancement for a player.
   */
  public static void triggerEnterMystDimensionQuinn(ServerPlayer player) {
    Object trigger = Services.ADVANCEMENTS.getEnterMystDimensionQuinnTrigger();
    if (trigger instanceof EnterMystDimensionQuinnTrigger t) {
      t.trigger(player);
    } else {
      try {
        trigger.getClass().getMethod("trigger", ServerPlayer.class).invoke(trigger, player);
      } catch (Exception e) {
        // Ignore - trigger failed
      }
    }
  }
}
