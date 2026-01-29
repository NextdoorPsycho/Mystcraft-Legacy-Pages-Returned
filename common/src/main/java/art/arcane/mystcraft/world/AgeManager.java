package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global manager for Mystcraft Ages.
 * Tracks all ages across the server and provides lookup functionality.
 * This data is saved in the overworld.
 */
public class AgeManager extends SavedData {

  private static final String DATA_NAME = Mystcraft.MOD_ID + "_ages";

  private static final String TAG_NEXT_UID = "NextUID";
  private static final String TAG_AGES = "Ages";
  private static final String TAG_UID = "UID";
  private static final String TAG_UUID = "UUID";
  private static final String TAG_DIMENSION = "Dimension";
  /**
   * Map of age UID to dimension ResourceLocation
   */
  private final Map<Integer, ResourceLocation> ageUIDtoDimension = new HashMap<>();
  /**
   * Map of dimension ResourceLocation to age UID
   */
  private final Map<ResourceLocation, Integer> dimensionToAgeUID = new HashMap<>();
  /**
   * Map of age UUID to age UID
   */
  private final Map<UUID, Integer> ageUUIDtoUID = new HashMap<>();
  /**
   * Next available age UID (starts at 1000 for 4-digit IDs, avoids collision with The End which is UID 1)
   */
  private int nextUID = 1000;

  public AgeManager() {
  }

  /**
   * Loads the age manager data from NBT (static factory method).
   */
  public static AgeManager load(CompoundTag tag) {
    AgeManager manager = new AgeManager();
    manager.loadFromTag(tag);
    return manager;
  }

  /**
   * Gets the AgeManager for the server.
   * Uses version-specific SavedData API through Services.VERSION.
   */
  public static AgeManager get(MinecraftServer server) {
    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
    if (overworld == null) {
      throw new IllegalStateException("Overworld not available");
    }
    return Services.VERSION.computeSavedData(
            overworld,
            AgeManager::new,
            AgeManager::load,
            DATA_NAME
    );
  }

  /**
   * Gets the AgeManager for a level.
   */
  public static AgeManager get(ServerLevel level) {
    return get(level.getServer());
  }

  /**
   * Loads the age manager data from NBT.
   */
  private void loadFromTag(CompoundTag tag) {
    this.nextUID = tag.getInt(TAG_NEXT_UID);
    // Don't bump existing saves - existing Ages with low UIDs must keep working.
    // Only fresh worlds (nextUID == 0 from missing tag) get the 1000 floor.
    if (this.nextUID <= 0) {
      this.nextUID = 1000;
    }

    this.ageUIDtoDimension.clear();
    this.dimensionToAgeUID.clear();
    this.ageUUIDtoUID.clear();

    ListTag agesList = tag.getList(TAG_AGES, Tag.TAG_COMPOUND);
    for (int i = 0; i < agesList.size(); i++) {
      CompoundTag ageTag = agesList.getCompound(i);
      int uid = ageTag.getInt(TAG_UID);
      String dimStr = ageTag.getString(TAG_DIMENSION);

      if (uid > 0 && !dimStr.isEmpty()) {
        ResourceLocation dimLoc = new ResourceLocation(dimStr);
        ageUIDtoDimension.put(uid, dimLoc);
        dimensionToAgeUID.put(dimLoc, uid);

        if (ageTag.contains(TAG_UUID)) {
          UUID uuid = UUID.fromString(ageTag.getString(TAG_UUID));
          ageUUIDtoUID.put(uuid, uid);
        }
      }
    }
  }

  @Override
  @NotNull
  public CompoundTag save(@NotNull CompoundTag tag) {
    tag.putInt(TAG_NEXT_UID, nextUID);

    ListTag agesList = new ListTag();
    for (Map.Entry<Integer, ResourceLocation> entry : ageUIDtoDimension.entrySet()) {
      CompoundTag ageTag = new CompoundTag();
      ageTag.putInt(TAG_UID, entry.getKey());
      ageTag.putString(TAG_DIMENSION, entry.getValue().toString());

      // Find UUID if available
      for (Map.Entry<UUID, Integer> uuidEntry : ageUUIDtoUID.entrySet()) {
        if (uuidEntry.getValue().equals(entry.getKey())) {
          ageTag.putString(TAG_UUID, uuidEntry.getKey().toString());
          break;
        }
      }

      agesList.add(ageTag);
    }
    tag.put(TAG_AGES, agesList);

    return tag;
  }

  /**
   * Allocates a new age UID.
   */
  public int allocateUID() {
    int uid = nextUID++;
    setDirty();
    return uid;
  }

  /**
   * Registers a new age.
   */
  public void registerAge(int uid, ResourceLocation dimension, @Nullable UUID uuid) {
    ageUIDtoDimension.put(uid, dimension);
    dimensionToAgeUID.put(dimension, uid);
    if (uuid != null) {
      ageUUIDtoUID.put(uuid, uid);
    }
    setDirty();
    Mystcraft.LOGGER.info("Registered age {} for dimension {}", uid, dimension);
  }

  /**
   * Gets the dimension for an age UID.
   */
  @Nullable
  public ResourceLocation getDimension(int uid) {
    return ageUIDtoDimension.get(uid);
  }

  /**
   * Gets the age UID for a dimension.
   */
  public int getAgeUID(ResourceLocation dimension) {
    return dimensionToAgeUID.getOrDefault(dimension, 0);
  }

  /**
   * Gets the age UID for a dimension key.
   */
  public int getAgeUID(ResourceKey<Level> dimensionKey) {
    return getAgeUID(dimensionKey.location());
  }

  /**
   * Gets the age UID for a UUID.
   */
  public int getAgeUIDByUUID(UUID uuid) {
    return ageUUIDtoUID.getOrDefault(uuid, 0);
  }

  /**
   * Checks if a dimension is a Mystcraft age.
   */
  public boolean isAge(ResourceLocation dimension) {
    return dimensionToAgeUID.containsKey(dimension);
  }

  /**
   * Checks if a dimension is a Mystcraft age.
   */
  public boolean isAge(ResourceKey<Level> dimensionKey) {
    return isAge(dimensionKey.location());
  }

  /**
   * Gets the ServerLevel for an age UID.
   */
  @Nullable
  public ServerLevel getAgeLevel(MinecraftServer server, int uid) {
    ResourceLocation dimLoc = getDimension(uid);
    if (dimLoc == null) {
      return null;
    }

    for (ServerLevel level : server.getAllLevels()) {
      if (level.dimension().location().equals(dimLoc)) {
        return level;
      }
    }
    return null;
  }

  /**
   * Gets the total number of registered ages.
   */
  public int getAgeCount() {
    return ageUIDtoDimension.size();
  }

  /**
   * Gets all registered age UIDs.
   */
  public Iterable<Integer> getAllAgeUIDs() {
    return ageUIDtoDimension.keySet();
  }

  /**
   * Clears all registered ages and resets the UID counter to 1000.
   */
  public void clearAllAges() {
    ageUIDtoDimension.clear();
    dimensionToAgeUID.clear();
    ageUUIDtoUID.clear();
    nextUID = 1000;
    setDirty();
  }
}
