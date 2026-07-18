package art.arcane.mystcraft.advancements;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers and dispatches Mystcraft's custom advancement criteria triggers.
 */
public final class ModAdvancements {

  private static final EnterMystDimensionSafeTrigger ENTER_MYST_DIMENSION_SAFE =
      new EnterMystDimensionSafeTrigger();
  private static final EnterMystDimensionQuinnTrigger ENTER_MYST_DIMENSION_QUINN =
      new EnterMystDimensionQuinnTrigger();
  private static final WritingDeskWriteTrigger WRITING_DESK_WRITE =
      new WritingDeskWriteTrigger();
  private static boolean registered;

  private ModAdvancements() {
  }

  /**
   * Registers all custom criteria triggers with the vanilla registry. Must be
   * called once during common setup, before built-in registries are frozen.
   */
  public static synchronized void register() {
    if (registered) {
      return;
    }
    Registry.register(
        BuiltInRegistries.TRIGGER_TYPES,
        Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "enter_myst_dimension_safe"),
        ENTER_MYST_DIMENSION_SAFE
    );
    Registry.register(
        BuiltInRegistries.TRIGGER_TYPES,
        Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "enter_myst_dimension_quinn"),
        ENTER_MYST_DIMENSION_QUINN
    );
    Registry.register(
        BuiltInRegistries.TRIGGER_TYPES,
        Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "writing_desk_write"),
        WRITING_DESK_WRITE
    );
    registered = true;
  }

  /**
   * Returns the shared safe-entry trigger instance for loader-managed
   * registration. Forge must enqueue this instance in its deferred registry;
   * Fabric uses {@link #register()} before the vanilla registry is frozen.
   */
  public static EnterMystDimensionSafeTrigger enterMystDimensionSafe() {
    return ENTER_MYST_DIMENSION_SAFE;
  }

  /** Returns the shared unsafe-entry trigger instance for loader registration. */
  public static EnterMystDimensionQuinnTrigger enterMystDimensionQuinn() {
    return ENTER_MYST_DIMENSION_QUINN;
  }

  /** Returns the shared writing-desk trigger instance for loader registration. */
  public static WritingDeskWriteTrigger writingDeskWrite() {
    return WRITING_DESK_WRITE;
  }

  /**
   * Triggers the WritingDeskWrite advancement for a player.
   */
  public static void triggerWritingDeskWrite(ServerPlayer player) {
    WRITING_DESK_WRITE.trigger(player);
  }

  /**
   * Triggers the EnterMystDimensionSafe advancement for a player.
   */
  public static void triggerEnterMystDimensionSafe(ServerPlayer player) {
    ENTER_MYST_DIMENSION_SAFE.trigger(player);
  }

  /**
   * Triggers the EnterMystDimensionQuinn advancement for a player.
   */
  public static void triggerEnterMystDimensionQuinn(ServerPlayer player) {
    ENTER_MYST_DIMENSION_QUINN.trigger(player);
  }
}
