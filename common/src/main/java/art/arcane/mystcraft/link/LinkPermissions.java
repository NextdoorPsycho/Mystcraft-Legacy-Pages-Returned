package art.arcane.mystcraft.link;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages linking permissions for Mystcraft dimensions.
 * Controls who can enter or depart from specific Ages.
 */
public class LinkPermissions extends SavedData {

  private static final String DATA_NAME = Mystcraft.MOD_ID + "_link_permissions";

  // Age UID -> Set of player UUIDs who are BLOCKED from entry
  private final Map<Integer, Set<UUID>> entryBlacklist = new HashMap<>();

  // Age UID -> Set of player UUIDs who are ALLOWED entry (whitelist mode)
  private final Map<Integer, Set<UUID>> entryWhitelist = new HashMap<>();

  // Age UID -> true if using whitelist mode (only listed players can enter)
  private final Map<Integer, Boolean> whitelistMode = new HashMap<>();

  // Age UID -> Set of player UUIDs who cannot DEPART from the age
  private final Map<Integer, Set<UUID>> departureBlocked = new HashMap<>();

  // Age UID -> Owner UUID (owner always has full access)
  private final Map<Integer, UUID> ageOwners = new HashMap<>();

  // Global admins who bypass all restrictions
  private final Set<UUID> globalAdmins = new HashSet<>();

  public LinkPermissions() {
  }

  public static LinkPermissions create() {
    return new LinkPermissions();
  }

  public static LinkPermissions load(CompoundTag tag) {
    LinkPermissions permissions = new LinkPermissions();

    // Load entry blacklist
    if (tag.contains("EntryBlacklist", Tag.TAG_COMPOUND)) {
      CompoundTag blacklist = tag.getCompound("EntryBlacklist");
      for (String key : blacklist.getAllKeys()) {
        int ageUID = Integer.parseInt(key);
        Set<UUID> players = loadUUIDSet(blacklist.getList(key, Tag.TAG_STRING));
        permissions.entryBlacklist.put(ageUID, players);
      }
    }

    // Load entry whitelist
    if (tag.contains("EntryWhitelist", Tag.TAG_COMPOUND)) {
      CompoundTag whitelist = tag.getCompound("EntryWhitelist");
      for (String key : whitelist.getAllKeys()) {
        int ageUID = Integer.parseInt(key);
        Set<UUID> players = loadUUIDSet(whitelist.getList(key, Tag.TAG_STRING));
        permissions.entryWhitelist.put(ageUID, players);
      }
    }

    // Load whitelist mode flags
    if (tag.contains("WhitelistMode", Tag.TAG_COMPOUND)) {
      CompoundTag modes = tag.getCompound("WhitelistMode");
      for (String key : modes.getAllKeys()) {
        int ageUID = Integer.parseInt(key);
        permissions.whitelistMode.put(ageUID, modes.getBoolean(key));
      }
    }

    // Load departure blocked
    if (tag.contains("DepartureBlocked", Tag.TAG_COMPOUND)) {
      CompoundTag blocked = tag.getCompound("DepartureBlocked");
      for (String key : blocked.getAllKeys()) {
        int ageUID = Integer.parseInt(key);
        Set<UUID> players = loadUUIDSet(blocked.getList(key, Tag.TAG_STRING));
        permissions.departureBlocked.put(ageUID, players);
      }
    }

    // Load age owners
    if (tag.contains("AgeOwners", Tag.TAG_COMPOUND)) {
      CompoundTag owners = tag.getCompound("AgeOwners");
      for (String key : owners.getAllKeys()) {
        int ageUID = Integer.parseInt(key);
        permissions.ageOwners.put(ageUID, UUID.fromString(owners.getString(key)));
      }
    }

    // Load global admins
    if (tag.contains("GlobalAdmins", Tag.TAG_LIST)) {
      permissions.globalAdmins.addAll(loadUUIDSet(tag.getList("GlobalAdmins", Tag.TAG_STRING)));
    }

    return permissions;
  }

  private static Set<UUID> loadUUIDSet(ListTag list) {
    Set<UUID> set = new HashSet<>();
    for (int i = 0; i < list.size(); i++) {
      try {
        set.add(UUID.fromString(list.getString(i)));
      } catch (IllegalArgumentException ignored) {
      }
    }
    return set;
  }

  private static ListTag saveUUIDSet(Set<UUID> set) {
    ListTag list = new ListTag();
    for (UUID uuid : set) {
      list.add(StringTag.valueOf(uuid.toString()));
    }
    return list;
  }

  // --- Permission Checks ---

  /**
   * Gets the LinkPermissions for a server.
   * Uses version-specific SavedData API through Services.VERSION.
   */
  public static LinkPermissions get(MinecraftServer server) {
    return Services.VERSION.computeSavedData(
            server.overworld(),
            LinkPermissions::create,
            LinkPermissions::load,
            DATA_NAME
    );
  }

  @Override
  public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
    // Save entry blacklist
    CompoundTag blacklist = new CompoundTag();
    for (Map.Entry<Integer, Set<UUID>> entry : entryBlacklist.entrySet()) {
      blacklist.put(String.valueOf(entry.getKey()), saveUUIDSet(entry.getValue()));
    }
    tag.put("EntryBlacklist", blacklist);

    // Save entry whitelist
    CompoundTag whitelist = new CompoundTag();
    for (Map.Entry<Integer, Set<UUID>> entry : entryWhitelist.entrySet()) {
      whitelist.put(String.valueOf(entry.getKey()), saveUUIDSet(entry.getValue()));
    }
    tag.put("EntryWhitelist", whitelist);

    // Save whitelist mode flags
    CompoundTag modes = new CompoundTag();
    for (Map.Entry<Integer, Boolean> entry : whitelistMode.entrySet()) {
      modes.putBoolean(String.valueOf(entry.getKey()), entry.getValue());
    }
    tag.put("WhitelistMode", modes);

    // Save departure blocked
    CompoundTag blocked = new CompoundTag();
    for (Map.Entry<Integer, Set<UUID>> entry : departureBlocked.entrySet()) {
      blocked.put(String.valueOf(entry.getKey()), saveUUIDSet(entry.getValue()));
    }
    tag.put("DepartureBlocked", blocked);

    // Save age owners
    CompoundTag owners = new CompoundTag();
    for (Map.Entry<Integer, UUID> entry : ageOwners.entrySet()) {
      owners.putString(String.valueOf(entry.getKey()), entry.getValue().toString());
    }
    tag.put("AgeOwners", owners);

    // Save global admins
    tag.put("GlobalAdmins", saveUUIDSet(globalAdmins));

    return tag;
  }

  // --- Permission Management ---

  /**
   * Checks if a player can enter the specified Age.
   */
  public boolean canEnter(ServerPlayer player, int ageUID) {
    UUID playerId = player.getUUID();

    // Global admins can always enter
    if (globalAdmins.contains(playerId)) {
      return true;
    }

    // Owners can always enter their age
    UUID owner = ageOwners.get(ageUID);
    if (owner != null && owner.equals(playerId)) {
      return true;
    }

    // Check if whitelist mode is enabled
    if (Boolean.TRUE.equals(whitelistMode.get(ageUID))) {
      // Whitelist mode: only whitelisted players can enter
      Set<UUID> allowed = entryWhitelist.get(ageUID);
      return allowed != null && allowed.contains(playerId);
    } else {
      // Blacklist mode: check if player is blocked
      Set<UUID> blocked = entryBlacklist.get(ageUID);
      return blocked == null || !blocked.contains(playerId);
    }
  }

  /**
   * Checks if a player can depart from the specified Age.
   */
  public boolean canDepart(ServerPlayer player, int ageUID) {
    UUID playerId = player.getUUID();

    // Global admins can always depart
    if (globalAdmins.contains(playerId)) {
      return true;
    }

    // Owners can always depart from their age
    UUID owner = ageOwners.get(ageUID);
    if (owner != null && owner.equals(playerId)) {
      return true;
    }

    // Check departure block list
    Set<UUID> blocked = departureBlocked.get(ageUID);
    return blocked == null || !blocked.contains(playerId);
  }

  /**
   * Sets the owner of an Age.
   */
  public void setOwner(int ageUID, UUID owner) {
    if (owner != null) {
      ageOwners.put(ageUID, owner);
    } else {
      ageOwners.remove(ageUID);
    }
    setDirty();
  }

  /**
   * Gets the owner of an Age.
   */
  public UUID getOwner(int ageUID) {
    return ageOwners.get(ageUID);
  }

  /**
   * Adds a player to the entry blacklist for an Age.
   */
  public void blockEntry(int ageUID, UUID player) {
    entryBlacklist.computeIfAbsent(ageUID, k -> new HashSet<>()).add(player);
    setDirty();
  }

  /**
   * Removes a player from the entry blacklist for an Age.
   */
  public void allowEntry(int ageUID, UUID player) {
    Set<UUID> blocked = entryBlacklist.get(ageUID);
    if (blocked != null) {
      blocked.remove(player);
      if (blocked.isEmpty()) {
        entryBlacklist.remove(ageUID);
      }
    }
    setDirty();
  }

  /**
   * Adds a player to the entry whitelist for an Age.
   */
  public void whitelistEntry(int ageUID, UUID player) {
    entryWhitelist.computeIfAbsent(ageUID, k -> new HashSet<>()).add(player);
    setDirty();
  }

  /**
   * Removes a player from the entry whitelist for an Age.
   */
  public void unwhitelistEntry(int ageUID, UUID player) {
    Set<UUID> allowed = entryWhitelist.get(ageUID);
    if (allowed != null) {
      allowed.remove(player);
      if (allowed.isEmpty()) {
        entryWhitelist.remove(ageUID);
      }
    }
    setDirty();
  }

  /**
   * Enables or disables whitelist mode for an Age.
   */
  public void setWhitelistMode(int ageUID, boolean enabled) {
    if (enabled) {
      whitelistMode.put(ageUID, true);
    } else {
      whitelistMode.remove(ageUID);
    }
    setDirty();
  }

  /**
   * Checks if whitelist mode is enabled for an Age.
   */
  public boolean isWhitelistMode(int ageUID) {
    return Boolean.TRUE.equals(whitelistMode.get(ageUID));
  }

  /**
   * Blocks a player from departing an Age.
   */
  public void blockDeparture(int ageUID, UUID player) {
    departureBlocked.computeIfAbsent(ageUID, k -> new HashSet<>()).add(player);
    setDirty();
  }

  /**
   * Allows a player to depart from an Age.
   */
  public void allowDeparture(int ageUID, UUID player) {
    Set<UUID> blocked = departureBlocked.get(ageUID);
    if (blocked != null) {
      blocked.remove(player);
      if (blocked.isEmpty()) {
        departureBlocked.remove(ageUID);
      }
    }
    setDirty();
  }

  /**
   * Adds a global admin who bypasses all restrictions.
   */
  public void addGlobalAdmin(UUID player) {
    globalAdmins.add(player);
    setDirty();
  }

  /**
   * Removes a global admin.
   */
  public void removeGlobalAdmin(UUID player) {
    globalAdmins.remove(player);
    setDirty();
  }

  // --- Data Access ---

  /**
   * Checks if a player is a global admin.
   */
  public boolean isGlobalAdmin(UUID player) {
    return globalAdmins.contains(player);
  }

  /**
   * Clears all permissions for an Age (used when deleting an Age).
   */
  public void clearAgePermissions(int ageUID) {
    entryBlacklist.remove(ageUID);
    entryWhitelist.remove(ageUID);
    whitelistMode.remove(ageUID);
    departureBlocked.remove(ageUID);
    ageOwners.remove(ageUID);
    setDirty();
  }
}
