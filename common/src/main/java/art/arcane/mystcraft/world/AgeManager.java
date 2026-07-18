package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.Services;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Global manager for Mystcraft Ages. Tracks all ages across the server and
 * provides lookup functionality. This data is saved in the overworld.
 */
public class AgeManager extends SavedData {

  private static final String DATA_NAME = Mystcraft.MOD_ID + "_ages";

  private static final String TAG_NEXT_UID = "NextUID";
  private static final String TAG_AGES = "Ages";
  private static final String TAG_UID = "UID";
  private static final String TAG_UUID = "UUID";
  private static final String TAG_DIMENSION = "Dimension";
  private static final String TAG_DEFINITION = "Definition";
  private final Map<Integer, ResourceLocation> ageUIDtoDimension = new HashMap<>();
  private final Map<ResourceLocation, Integer> dimensionToAgeUID = new HashMap<>();
  private final Map<UUID, Integer> ageUUIDtoUID = new HashMap<>();
  private final Map<Integer, UUID> ageUIDtoUUID = new HashMap<>();
  private final Map<Integer, AgeDefinition> ageDefinitions = new HashMap<>();
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
   * Gets the AgeManager for the server. Uses version-specific SavedData API
   * through Services.VERSION.
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

  private void loadFromTag(CompoundTag tag) {
    this.nextUID = tag.getInt(TAG_NEXT_UID);

    if (this.nextUID <= 0) {
      this.nextUID = 1000;
    }

    this.ageUIDtoDimension.clear();
    this.dimensionToAgeUID.clear();
    this.ageUUIDtoUID.clear();
    this.ageUIDtoUUID.clear();
    this.ageDefinitions.clear();

    ListTag agesList = tag.getList(TAG_AGES, Tag.TAG_COMPOUND);
    for (int i = 0; i < agesList.size(); i++) {
      CompoundTag ageTag = agesList.getCompound(i);
      int uid = ageTag.getInt(TAG_UID);
      String dimStr = ageTag.getString(TAG_DIMENSION);

      if (uid > 0 && !dimStr.isEmpty()) {
        ResourceLocation dimLoc = ResourceLocation.tryParse(dimStr);
        if (dimLoc == null) {
          Mystcraft.LOGGER.error("Ignoring Age {} with invalid dimension ID '{}'", uid, dimStr);
          continue;
        }
        if (dimensionToAgeUID.containsKey(dimLoc) || ageUIDtoDimension.containsKey(uid)) {
          Mystcraft.LOGGER.error("Ignoring duplicate AgeManager entry {} -> {}", uid, dimLoc);
          continue;
        }
        ageUIDtoDimension.put(uid, dimLoc);
        dimensionToAgeUID.put(dimLoc, uid);

        if (ageTag.contains(TAG_UUID, Tag.TAG_STRING)) {
          try {
            UUID uuid = UUID.fromString(ageTag.getString(TAG_UUID));
            if (ageUUIDtoUID.containsKey(uuid)) {
              Mystcraft.LOGGER.error("Ignoring duplicate UUID {} for Age {}", uuid, uid);
            } else {
              ageUUIDtoUID.put(uuid, uid);
              ageUIDtoUUID.put(uid, uuid);
            }
          } catch (IllegalArgumentException e) {
            Mystcraft.LOGGER.error("Failed to parse UUID for age {}: {}", uid, ageTag.getString(TAG_UUID));
          }
        }

        if (ageTag.contains(TAG_DEFINITION, Tag.TAG_COMPOUND)) {
          AgeDefinition definition = AgeDefinition.load(ageTag.getCompound(TAG_DEFINITION));
          if (definition != null) {
            ageDefinitions.put(uid, definition);
          } else {
            Mystcraft.LOGGER.error("Age {} has an invalid persisted generation definition", uid);
          }
        }
      }
    }
  }

  @NotNull
  public CompoundTag save(@NotNull CompoundTag tag) {
    Mystcraft.LOGGER.info("Saving AgeManager data...");
    tag.putInt(TAG_NEXT_UID, nextUID);

    Map<Integer, ResourceLocation> ageMap;
    Map<Integer, UUID> uuidMap;
    Map<Integer, AgeDefinition> definitionMap;
    synchronized (this) {
      ageMap = new TreeMap<>(ageUIDtoDimension);
      uuidMap = new HashMap<>(ageUIDtoUUID);
      definitionMap = new HashMap<>(ageDefinitions);
    }

    ListTag agesList = new ListTag();
    for (Map.Entry<Integer, ResourceLocation> entry : ageMap.entrySet()) {
      int uid = entry.getKey();
      CompoundTag ageTag = new CompoundTag();
      ageTag.putInt(TAG_UID, uid);
      ageTag.putString(TAG_DIMENSION, entry.getValue().toString());

      UUID uuid = uuidMap.get(uid);
      if (uuid != null) {
        ageTag.putString(TAG_UUID, uuid.toString());
      }

      AgeDefinition definition = definitionMap.get(uid);
      if (definition != null) {
        ageTag.put(TAG_DEFINITION, definition.save());
      }

      agesList.add(ageTag);
    }
    tag.put(TAG_AGES, agesList);

    Mystcraft.LOGGER.info("AgeManager save complete.");
    return tag;
  }

  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    return save(tag);
  }

  /**
   * Allocates a new age UID.
   */
  public synchronized int allocateUID() {
    while (ageUIDtoDimension.containsKey(nextUID)) {
      if (nextUID == Integer.MAX_VALUE) {
        throw new IllegalStateException("No free Mystcraft Age UIDs remain");
      }
      nextUID++;
    }
    int uid = nextUID++;
    setDirty();
    return uid;
  }

  /**
   * Registers a new age.
   */
  public synchronized void registerAge(int uid, ResourceLocation dimension, @Nullable UUID uuid) {
    registerAge(uid, dimension, uuid, null);
  }

  /**
   * Registers an Age together with the durable inputs needed to recreate its
   * chunk generator after a restart.
   */
  public synchronized void registerAge(
      int uid,
      @NotNull ResourceLocation dimension,
      @Nullable UUID uuid,
      @Nullable AgeDefinition definition
  ) {
    if (uid <= 0) {
      throw new IllegalArgumentException("Age UID must be positive: " + uid);
    }
    Integer dimensionOwner = dimensionToAgeUID.get(dimension);
    if (dimensionOwner != null && dimensionOwner != uid) {
      throw new IllegalStateException(
          "Dimension " + dimension + " is already registered to Age " + dimensionOwner
      );
    }
    if (uuid != null) {
      Integer uuidOwner = ageUUIDtoUID.get(uuid);
      if (uuidOwner != null && uuidOwner != uid) {
        throw new IllegalStateException("UUID " + uuid + " is already registered to Age " + uuidOwner);
      }
    }

    ResourceLocation previousDimension = ageUIDtoDimension.put(uid, dimension);
    if (previousDimension != null && !previousDimension.equals(dimension)) {
      dimensionToAgeUID.remove(previousDimension);
    }
    dimensionToAgeUID.put(dimension, uid);

    UUID previousUUID = ageUIDtoUUID.remove(uid);
    if (previousUUID != null) {
      ageUUIDtoUID.remove(previousUUID);
    }
    if (uuid != null) {
      ageUUIDtoUID.put(uuid, uid);
      ageUIDtoUUID.put(uid, uuid);
    }
    if (definition != null) {
      ageDefinitions.put(uid, definition);
    }
    setDirty();
    Mystcraft.LOGGER.info("Registered age {} for dimension {}", uid, dimension);
  }

  /**
   * Removes a partially-created Age registration without reusing its UID.
   */
  public synchronized void unregisterAge(int uid) {
    ResourceLocation dimension = ageUIDtoDimension.remove(uid);
    if (dimension != null) {
      dimensionToAgeUID.remove(dimension);
    }
    UUID uuid = ageUIDtoUUID.remove(uid);
    if (uuid != null) {
      ageUUIDtoUID.remove(uuid);
    }
    ageDefinitions.remove(uid);
    setDirty();
  }

  /**
   * Gets the dimension for an age UID.
   */
  @Nullable
  public synchronized ResourceLocation getDimension(int uid) {
    return ageUIDtoDimension.get(uid);
  }

  /**
   * Gets the age UID for a dimension.
   */
  public synchronized int getAgeUID(ResourceLocation dimension) {
    return dimensionToAgeUID.getOrDefault(dimension, 0);
  }

  /**
   * Gets the age UID for a dimension key.
   */
  public synchronized int getAgeUID(ResourceKey<Level> dimensionKey) {
    return getAgeUID(dimensionKey.location());
  }

  /**
   * Gets the age UID for a UUID.
   */
  public synchronized int getAgeUIDByUUID(UUID uuid) {
    return ageUUIDtoUID.getOrDefault(uuid, 0);
  }

  /**
   * Gets the persistent UUID for an Age.
   */
  @Nullable
  public synchronized UUID getAgeUUID(int uid) {
    return ageUIDtoUUID.get(uid);
  }

  /**
   * Gets the persisted generation definition for an Age.
   */
  @Nullable
  public synchronized AgeDefinition getDefinition(int uid) {
    return ageDefinitions.get(uid);
  }

  /**
   * Returns the persisted definition, importing the old per-Age SavedData
   * format once when necessary.
   */
  @Nullable
  public AgeDefinition getOrRecoverDefinition(@NotNull MinecraftServer server, int uid) {
    ResourceLocation dimension;
    synchronized (this) {
      AgeDefinition definition = ageDefinitions.get(uid);
      if (definition != null) {
        return definition;
      }
      dimension = ageUIDtoDimension.get(uid);
    }
    if (dimension == null) {
      return null;
    }

    AgeDefinition.LegacyMigration migration = AgeDefinition.loadLegacy(server, uid, dimension);
    if (migration == null) {
      return null;
    }

    synchronized (this) {
      AgeDefinition existing = ageDefinitions.get(uid);
      if (existing != null) {
        return existing;
      }
      ageDefinitions.put(uid, migration.definition());
      if (migration.ageUUID() != null && !ageUUIDtoUID.containsKey(migration.ageUUID())) {
        UUID previousUUID = ageUIDtoUUID.put(uid, migration.ageUUID());
        if (previousUUID != null) {
          ageUUIDtoUID.remove(previousUUID);
        }
        ageUUIDtoUID.put(migration.ageUUID(), uid);
      }
      setDirty();
      Mystcraft.LOGGER.info("Migrated persistent generation definition for Age {}", uid);
      return migration.definition();
    }
  }

  /**
   * Checks if a dimension is a Mystcraft age.
   */
  public synchronized boolean isAge(ResourceLocation dimension) {
    return dimensionToAgeUID.containsKey(dimension);
  }

  /**
   * Checks if a dimension is a Mystcraft age.
   */
  public synchronized boolean isAge(ResourceKey<Level> dimensionKey) {
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

    return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimLoc));
  }

  /**
   * Gets the total number of registered ages.
   */
  public synchronized int getAgeCount() {
    return ageUIDtoDimension.size();
  }

  /**
   * Gets all registered age UIDs.
   */
  public synchronized Iterable<Integer> getAllAgeUIDs() {
    return new ArrayList<>(ageUIDtoDimension.keySet());
  }

  /**
   * Clears all registered ages and resets the UID counter to 1000.
   */
  public synchronized void clearAllAges() {
    ageUIDtoDimension.clear();
    dimensionToAgeUID.clear();
    ageUUIDtoUID.clear();
    ageUIDtoUUID.clear();
    ageDefinitions.clear();
    nextUID = 1000;
    setDirty();
  }
}
