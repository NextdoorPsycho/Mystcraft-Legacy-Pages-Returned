package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Stores per-player return locations for personal pocket dimensions.
 */
public class PersonalPocketData extends SavedData {

  private static final String DATA_NAME = Mystcraft.MOD_ID + "_personal_pocket";
  private static final String TAG_RETURNS = "Returns";
  private static final String TAG_PLAYER = "Player";
  private static final String TAG_LINK = "Link";
  private static final String TAG_HEADS = "HeadBlocks";
  private static final String TAG_FACES = "Faces";
  private static final String TAG_BLOCKS = "Blocks";
  private static final String TAG_PROXIES = "ActiveProxies";
  private static final String TAG_DIMENSION = "Dimension";
  private static final String TAG_X = "X";
  private static final String TAG_Y = "Y";
  private static final String TAG_Z = "Z";
  private static final String TAG_YAW = "Yaw";
  private static final String TAG_PITCH = "Pitch";
  private static final String TAG_PROXY = "Proxy";
  private static final String TAG_OWNER_NAME = "OwnerName";

  private final Map<UUID, CompoundTag> returnLinks = new HashMap<>();
  private final Map<UUID, Map<AgeData.PocketHeadFace, List<String>>> headBlocks = new HashMap<>();
  private final Map<UUID, ProxyState> activeProxies = new HashMap<>();

  public static PersonalPocketData load(CompoundTag tag) {
    PersonalPocketData data = new PersonalPocketData();
    data.loadFromTag(tag);
    return data;
  }

  public static PersonalPocketData get(MinecraftServer server) {
    ServerLevel overworld = server.overworld();
    return Services.VERSION.computeSavedData(
        overworld,
        PersonalPocketData::new,
        PersonalPocketData::load,
        DATA_NAME
    );
  }

  private void loadFromTag(CompoundTag tag) {
    returnLinks.clear();
    headBlocks.clear();
    activeProxies.clear();
    if (!tag.contains(TAG_RETURNS)) {
      // continue to head blocks
    } else {
      ListTag list = tag.getList(TAG_RETURNS, Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
        CompoundTag entry = list.getCompound(i);
        if (!entry.contains(TAG_PLAYER) || !entry.contains(TAG_LINK)) {
          continue;
        }
        UUID playerId = parseUuid(entry.getString(TAG_PLAYER));
        if (playerId == null) {
          continue;
        }
        CompoundTag link = entry.getCompound(TAG_LINK);
        returnLinks.put(playerId, link.copy());
      }
    }

    if (!tag.contains(TAG_HEADS)) {
      // continue to proxies
    } else {
      ListTag headList = tag.getList(TAG_HEADS, Tag.TAG_COMPOUND);
      for (int i = 0; i < headList.size(); i++) {
        CompoundTag entry = headList.getCompound(i);
        if (!entry.contains(TAG_PLAYER) || !entry.contains(TAG_FACES)) {
          continue;
        }
        UUID playerId = parseUuid(entry.getString(TAG_PLAYER));
        if (playerId == null) {
          continue;
        }
        CompoundTag facesTag = entry.getCompound(TAG_FACES);
        Map<AgeData.PocketHeadFace, List<String>> map = new EnumMap<>(AgeData.PocketHeadFace.class);
        for (AgeData.PocketHeadFace face : AgeData.PocketHeadFace.values()) {
          if (!facesTag.contains(face.name(), Tag.TAG_LIST)) {
            continue;
          }
          ListTag blocks = facesTag.getList(face.name(), Tag.TAG_STRING);
          if (blocks.size() != 64) {
            continue;
          }
          List<String> list = new ArrayList<>(64);
          for (int b = 0; b < blocks.size(); b++) {
            list.add(blocks.getString(b));
          }
          map.put(face, list);
        }
        if (map.size() == AgeData.PocketHeadFace.values().length) {
          headBlocks.put(playerId, map);
        }
      }
    }

    if (!tag.contains(TAG_PROXIES)) {
      return;
    }
    ListTag proxyList = tag.getList(TAG_PROXIES, Tag.TAG_COMPOUND);
    for (int i = 0; i < proxyList.size(); i++) {
      CompoundTag entry = proxyList.getCompound(i);
      if (!entry.contains(TAG_PLAYER) || !entry.contains(TAG_DIMENSION) || !entry.contains(TAG_LINK)) {
        continue;
      }
      UUID playerId = parseUuid(entry.getString(TAG_PLAYER));
      UUID proxyId = parseUuid(entry.getString(TAG_PROXY));
      if (playerId == null || proxyId == null) {
        continue;
      }
      BlockPos position = new BlockPos(entry.getInt(TAG_X), entry.getInt(TAG_Y), entry.getInt(TAG_Z));
      activeProxies.put(playerId, new ProxyState(
          entry.getString(TAG_OWNER_NAME),
          entry.getInt(TAG_DIMENSION),
          position,
          entry.getFloat(TAG_YAW),
          entry.getFloat(TAG_PITCH),
          proxyId,
          entry.getCompound(TAG_LINK)
      ));
    }
  }

  public CompoundTag save(CompoundTag tag) {
    ListTag list = new ListTag();
    for (Map.Entry<UUID, CompoundTag> entry : returnLinks.entrySet()) {
      CompoundTag item = new CompoundTag();
      item.putString(TAG_PLAYER, entry.getKey().toString());
      item.put(TAG_LINK, entry.getValue().copy());
      list.add(item);
    }
    tag.put(TAG_RETURNS, list);

    ListTag headList = new ListTag();
    for (Map.Entry<UUID, Map<AgeData.PocketHeadFace, List<String>>> entry : headBlocks.entrySet()) {
      CompoundTag item = new CompoundTag();
      item.putString(TAG_PLAYER, entry.getKey().toString());
      CompoundTag faces = new CompoundTag();
      for (Map.Entry<AgeData.PocketHeadFace, List<String>> faceEntry : entry.getValue().entrySet()) {
        ListTag blocks = new ListTag();
        for (String id : faceEntry.getValue()) {
          blocks.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        faces.put(faceEntry.getKey().name(), blocks);
      }
      item.put(TAG_FACES, faces);
      headList.add(item);
    }
    tag.put(TAG_HEADS, headList);

    ListTag proxyList = new ListTag();
    for (Map.Entry<UUID, ProxyState> entry : activeProxies.entrySet()) {
      ProxyState state = entry.getValue();
      CompoundTag item = new CompoundTag();
      item.putString(TAG_PLAYER, entry.getKey().toString());
      item.putString(TAG_OWNER_NAME, state.ownerName());
      item.putInt(TAG_DIMENSION, state.dimensionUid());
      item.putInt(TAG_X, state.position().getX());
      item.putInt(TAG_Y, state.position().getY());
      item.putInt(TAG_Z, state.position().getZ());
      item.putFloat(TAG_YAW, state.yaw());
      item.putFloat(TAG_PITCH, state.pitch());
      item.putString(TAG_PROXY, state.proxyId().toString());
      item.put(TAG_LINK, state.returnLink());
      proxyList.add(item);
    }
    tag.put(TAG_PROXIES, proxyList);
    return tag;
  }

  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    return save(tag);
  }

  public void setReturnLink(UUID playerId, CompoundTag linkData) {
    returnLinks.put(playerId, linkData.copy());
    setDirty();
  }

  @Nullable
  public CompoundTag getReturnLink(UUID playerId) {
    CompoundTag data = returnLinks.get(playerId);
    return data != null ? data.copy() : null;
  }

  public void setHeadBlocks(UUID playerId, Map<AgeData.PocketHeadFace, List<String>> blocks) {
    if (blocks == null || blocks.isEmpty()) {
      headBlocks.remove(playerId);
      setDirty();
      return;
    }
    Map<AgeData.PocketHeadFace, List<String>> copy = new EnumMap<>(AgeData.PocketHeadFace.class);
    for (Map.Entry<AgeData.PocketHeadFace, List<String>> entry : blocks.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    headBlocks.put(playerId, copy);
    setDirty();
  }

  @Nullable
  public Map<AgeData.PocketHeadFace, List<String>> getHeadBlocks(UUID playerId) {
    Map<AgeData.PocketHeadFace, List<String>> data = headBlocks.get(playerId);
    if (data == null) {
      return null;
    }
    Map<AgeData.PocketHeadFace, List<String>> copy = new EnumMap<>(AgeData.PocketHeadFace.class);
    for (Map.Entry<AgeData.PocketHeadFace, List<String>> entry : data.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return copy;
  }

  public void setActiveProxy(UUID playerId, ProxyState state) {
    activeProxies.put(playerId, state);
    setDirty();
  }

  @Nullable
  public ProxyState getActiveProxy(UUID playerId) {
    return activeProxies.get(playerId);
  }

  public Set<UUID> getActiveProxyOwners() {
    return new HashSet<>(activeProxies.keySet());
  }

  public void clearActiveProxy(UUID playerId) {
    if (activeProxies.remove(playerId) != null) {
      setDirty();
    }
  }

  @Nullable
  private static UUID parseUuid(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  public record ProxyState(String ownerName, int dimensionUid, BlockPos position, float yaw, float pitch,
                           UUID proxyId, CompoundTag returnLink) {
    public ProxyState {
      ownerName = ownerName == null ? "" : ownerName;
      returnLink = returnLink == null ? new CompoundTag() : returnLink.copy();
    }

    @Override
    public CompoundTag returnLink() {
      return returnLink.copy();
    }
  }
}
