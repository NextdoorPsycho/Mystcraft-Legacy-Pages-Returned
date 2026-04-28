package art.arcane.mystcraft.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import art.arcane.mystcraft.util.ItemStackNbt;

import java.util.UUID;

/**
 * Utility class for reading and writing link data to/from NBT.
 * Handles dimension IDs, spawn positions, display names, and link flags.
 */
public class LinkOptions {

  private static final String TAG_LINK_OPTIONS = "LinkOptions";
  private CompoundTag data;

  public LinkOptions(@Nullable CompoundTag data) {
    if (data != null) {
      this.data = data.copy();
    } else {
      this.data = new CompoundTag();
    }
  }

  public static CompoundTag setDisplayName(@Nullable CompoundTag nbttagcompound, @NotNull String name) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    nbttagcompound.putString("DisplayName", name);
    return nbttagcompound;
  }

  @NotNull
  public static String getDisplayName(@Nullable CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("DisplayName")) {
      return nbttagcompound.getString("DisplayName");
    }
    if (nbttagcompound != null && nbttagcompound.contains("agename")) {
      return nbttagcompound.getString("agename");
    }
    return "???";
  }

  public static CompoundTag setFlag(CompoundTag nbttagcompound, String flag, boolean val) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    getFlagCompound(nbttagcompound).putBoolean(flag, val);
    return nbttagcompound;
  }

  public static boolean getFlag(CompoundTag nbttagcompound, String flag) {
    if (nbttagcompound != null && getFlagCompound(nbttagcompound).contains(flag)) {
      return getFlagCompound(nbttagcompound).getBoolean(flag);
    }
    return false;
  }

  public static CompoundTag setProperty(CompoundTag nbttagcompound, String flag, String value) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    CompoundTag props = getPropertyCompound(nbttagcompound);
    if (value == null) {
      props.remove(flag);
    } else {
      props.putString(flag, value);
    }
    return nbttagcompound;
  }

  @Nullable
  public static String getProperty(CompoundTag nbttagcompound, String flag) {
    if (nbttagcompound != null && getPropertyCompound(nbttagcompound).contains(flag)) {
      return getPropertyCompound(nbttagcompound).getString(flag);
    }
    return null;
  }

  public static CompoundTag setDimensionUID(CompoundTag nbttagcompound, int uid) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    nbttagcompound.putInt("Dimension", uid);
    return nbttagcompound;
  }

  @Nullable
  public static Integer getDimensionUID(CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("Dimension")) {
      return nbttagcompound.getInt("Dimension");
    }
    if (nbttagcompound != null && nbttagcompound.contains("AgeUID")) {
      return nbttagcompound.getInt("AgeUID");
    }
    return null;
  }

  public static CompoundTag setUUID(CompoundTag nbttagcompound, @Nullable UUID uuid) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    if (uuid != null) {
      nbttagcompound.putString("TargetUUID", uuid.toString());
    } else {
      nbttagcompound.remove("TargetUUID");
    }
    return nbttagcompound;
  }

  @Nullable
  public static UUID getUUID(CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("TargetUUID")) {
      return UUID.fromString(nbttagcompound.getString("TargetUUID"));
    }
    return null;
  }

  public static CompoundTag setSpawn(CompoundTag nbttagcompound, @Nullable BlockPos coords) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    if (coords != null) {
      nbttagcompound.putInt("SpawnX", coords.getX());
      nbttagcompound.putInt("SpawnY", coords.getY());
      nbttagcompound.putInt("SpawnZ", coords.getZ());
    }
    return nbttagcompound;
  }

  @Nullable
  public static BlockPos getSpawn(CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("SpawnX") && nbttagcompound.contains("SpawnY") && nbttagcompound.contains("SpawnZ")) {
      return new BlockPos(nbttagcompound.getInt("SpawnX"), nbttagcompound.getInt("SpawnY"), nbttagcompound.getInt("SpawnZ"));
    }
    return null;
  }

  public static CompoundTag setSpawnYaw(CompoundTag nbttagcompound, float yaw) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    nbttagcompound.putFloat("SpawnYaw", yaw);
    return nbttagcompound;
  }

  public static float getSpawnYaw(CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("SpawnYaw")) {
      return nbttagcompound.getFloat("SpawnYaw");
    }
    return 180;
  }

  /**
   * Sets the portal/link color.
   */
  public static CompoundTag setLinkColor(CompoundTag nbttagcompound, int color) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    nbttagcompound.putInt("LinkColor", color);
    return nbttagcompound;
  }

  // Static utility methods

  /**
   * Gets the portal/link color.
   *
   * @return The color as RGB, or null if not set
   */
  @Nullable
  public static Integer getLinkColor(CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("LinkColor")) {
      return nbttagcompound.getInt("LinkColor");
    }
    return null;
  }

  /**
   * Gets the portal/link color with a default fallback.
   *
   * @param defaultColor The color to return if not set
   * @return The color as RGB
   */
  public static int getLinkColor(CompoundTag nbttagcompound, int defaultColor) {
    Integer color = getLinkColor(nbttagcompound);
    return color != null ? color : defaultColor;
  }

  @NotNull
  private static CompoundTag getFlagCompound(CompoundTag nbttagcompound) {
    if (!nbttagcompound.contains("Flags")) {
      nbttagcompound.put("Flags", new CompoundTag());
    }
    return nbttagcompound.getCompound("Flags");
  }

  @NotNull
  private static CompoundTag getPropertyCompound(CompoundTag nbttagcompound) {
    if (!nbttagcompound.contains("Props")) {
      nbttagcompound.put("Props", new CompoundTag());
    }
    return nbttagcompound.getCompound("Props");
  }

  public static CompoundTag setDimension(CompoundTag nbttagcompound, @Nullable ResourceKey<Level> dimension) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    if (dimension != null) {
      nbttagcompound.putString("DimensionKey", dimension.location().toString());
    } else {
      nbttagcompound.remove("DimensionKey");
    }
    return nbttagcompound;
  }

  @Nullable
  public static ResourceKey<Level> getDimension(CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("DimensionKey")) {
      String key = nbttagcompound.getString("DimensionKey");
      return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(key));
    }
    return null;
  }

  public static CompoundTag setDead(CompoundTag nbttagcompound, boolean dead) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    if (dead) {
      nbttagcompound.putBoolean("LinkDead", true);
    } else {
      nbttagcompound.remove("LinkDead");
    }
    return nbttagcompound;
  }

  public static boolean isDead(CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("LinkDead")) {
      return nbttagcompound.getBoolean("LinkDead");
    }
    return false;
  }

  // ---------------------------------------------------------------------------
  // Cover material (added 2026-04 for content-aware book UI)
  // ---------------------------------------------------------------------------

  /**
   * Records the resource id of the item used as the book cover at binding time.
   * Used by the procedural book texture factory to pick a cover palette.
   */
  public static CompoundTag setCoverItemId(CompoundTag nbttagcompound, @Nullable ResourceLocation coverId) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    if (coverId != null) {
      nbttagcompound.putString("Cover", coverId.toString());
    } else {
      nbttagcompound.remove("Cover");
    }
    return nbttagcompound;
  }

  /**
   * Returns the cover item id, or null if the book pre-dates the cover field.
   * Callers should fall back to a sensible default (typically leather).
   */
  @Nullable
  public static ResourceLocation getCoverItemId(@Nullable CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("Cover")) {
      String raw = nbttagcompound.getString("Cover");
      if (!raw.isEmpty()) {
        return ResourceLocation.tryParse(raw);
      }
    }
    return null;
  }

  /**
   * Records the blended ARGB ink tint produced by the Ink Mixer when this link
   * panel was written. Used to colour page edges and ribbon bookmark.
   */
  public static CompoundTag setInkTint(CompoundTag nbttagcompound, int argb) {
    if (nbttagcompound == null) {
      nbttagcompound = new CompoundTag();
    }
    nbttagcompound.putInt("InkTint", argb);
    return nbttagcompound;
  }

  /**
   * Returns the recorded ARGB ink tint. Returns {@code -1} (i.e. opaque white)
   * when the tag is missing so callers can detect "unset".
   */
  public static int getInkTint(@Nullable CompoundTag nbttagcompound) {
    if (nbttagcompound != null && nbttagcompound.contains("InkTint")) {
      return nbttagcompound.getInt("InkTint");
    }
    return -1;
  }

  /**
   * Creates LinkOptions from an ItemStack.
   *
   * @param stack The item stack
   * @return LinkOptions, or null if the stack has no link data
   */
  @Nullable
  public static LinkOptions fromItemStack(ItemStack stack) {
    if (stack.isEmpty()) return null;
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag == null || !tag.contains(TAG_LINK_OPTIONS)) return null;
    return new LinkOptions(tag.getCompound(TAG_LINK_OPTIONS));
  }

  /**
   * Removes LinkOptions from an ItemStack.
   *
   * @param stack The item stack
   */
  public static void removeFromItemStack(ItemStack stack) {
    if (stack.isEmpty()) return;
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag != null) {
      tag.remove(TAG_LINK_OPTIONS);
      ItemStackNbt.setTag(stack, tag);
    }
  }

  @NotNull
  public CompoundTag getTagCompound() {
    return data;
  }

  public LinkOptions copy() {
    return new LinkOptions(this.getTagCompound());
  }

  @NotNull
  public String getDisplayName() {
    return getDisplayName(data);
  }

  public void setDisplayName(@NotNull String displayname) {
    data = setDisplayName(data, displayname);
  }

  @Nullable
  public Integer getDimensionUID() {
    return getDimensionUID(data);
  }

  public void setDimensionUID(int uid) {
    data = setDimensionUID(data, uid);
  }

  @Nullable
  public UUID getTargetUUID() {
    return getUUID(data);
  }

  public void setTargetUUID(@Nullable UUID uuid) {
    data = setUUID(data, uuid);
  }

  @Nullable
  public BlockPos getSpawn() {
    return getSpawn(data);
  }

  // Dimension key methods

  public void setSpawn(@Nullable BlockPos spawn) {
    data = setSpawn(data, spawn);
  }

  public float getSpawnYaw() {
    return getSpawnYaw(data);
  }

  public void setSpawnYaw(float spawnyaw) {
    data = setSpawnYaw(data, spawnyaw);
  }

  public boolean getFlag(String flag) {
    return getFlag(data, flag);
  }

  // Dead link methods

  public void setFlag(String flag, boolean value) {
    data = setFlag(data, flag, value);
  }

  public String getProperty(String prop) {
    return getProperty(data, prop);
  }

  public void setProperty(String prop, String value) {
    data = setProperty(data, prop, value);
  }

  /**
   * Gets the dimension ResourceKey from the data.
   */
  @Nullable
  public ResourceKey<Level> getDimension() {
    return getDimension(data);
  }

  // ItemStack methods

  /**
   * Sets the dimension ResourceKey.
   */
  public void setDimension(@Nullable ResourceKey<Level> dimension) {
    data = setDimension(data, dimension);
  }

  /**
   * Checks if the link is marked as dead.
   */
  public boolean isDead() {
    return isDead(data);
  }

  /**
   * Sets the link dead status.
   */
  public void setDead(boolean dead) {
    data = setDead(data, dead);
  }

  /**
   * Saves LinkOptions to an ItemStack.
   *
   * @param stack The item stack to save to
   */
  public void toItemStack(ItemStack stack) {
    if (stack.isEmpty()) return;
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    tag.put(TAG_LINK_OPTIONS, this.data.copy());
    ItemStackNbt.setTag(stack, tag);
  }
}
