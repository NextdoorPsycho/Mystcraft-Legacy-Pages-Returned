package art.arcane.mystcraft.registry;

import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * Common accessor for registered sound events.
 * Platform modules populate these suppliers during initialization.
 */
public final class ModSounds {

  public static Supplier<SoundEvent> LINKING_POP;
  public static Supplier<SoundEvent> LINKING_LINK;
  public static Supplier<SoundEvent> LINKING_DISARM;
  public static Supplier<SoundEvent> LINKING_FOLLOWING;
  public static Supplier<SoundEvent> LINKING_INTRA;
  public static Supplier<SoundEvent> LINKING_FISSURE;
  public static Supplier<SoundEvent> LINKING_PORTAL;
  public static Supplier<SoundEvent> METEOR_ROAR;
  public static Supplier<SoundEvent> METEOR_IMPACT;

  private ModSounds() {
  }
}
