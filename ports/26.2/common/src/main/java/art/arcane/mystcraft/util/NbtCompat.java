package art.arcane.mystcraft.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Typed NBT checks used by the 26.2 data migration paths.
 */
public final class NbtCompat {

  /** Legacy wildcard used by the old two-argument CompoundTag.contains API. */
  public static final int TAG_ANY_NUMERIC = 99;

  private NbtCompat() {
  }

  /**
   * Preserves the typed containment check removed from CompoundTag in 26.2.
   */
  public static boolean contains(CompoundTag tag, String key, int expectedType) {
    Tag value = tag.get(key);
    if (value == null) {
      return false;
    }
    if (expectedType == TAG_ANY_NUMERIC) {
      return value.asNumber().isPresent();
    }
    return value.getId() == expectedType;
  }

}
