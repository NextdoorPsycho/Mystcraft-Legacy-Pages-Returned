package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

  private final Map<UUID, CompoundTag> returnLinks = new HashMap<>();
  private final Map<UUID, Map<AgeData.PocketHeadFace, List<String>>> headBlocks = new HashMap<>();

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
    if (!tag.contains(TAG_RETURNS)) {
      // continue to head blocks
    } else {
      ListTag list = tag.getList(TAG_RETURNS, Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
        CompoundTag entry = list.getCompound(i);
        if (!entry.contains(TAG_PLAYER) || !entry.contains(TAG_LINK)) {
          continue;
        }
        UUID playerId = UUID.fromString(entry.getString(TAG_PLAYER));
        CompoundTag link = entry.getCompound(TAG_LINK);
        returnLinks.put(playerId, link.copy());
      }
    }

    if (!tag.contains(TAG_HEADS)) {
      return;
    }
    ListTag headList = tag.getList(TAG_HEADS, Tag.TAG_COMPOUND);
    for (int i = 0; i < headList.size(); i++) {
      CompoundTag entry = headList.getCompound(i);
      if (!entry.contains(TAG_PLAYER) || !entry.contains(TAG_FACES)) {
        continue;
      }
      UUID playerId = UUID.fromString(entry.getString(TAG_PLAYER));
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

  @Override
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
    return tag;
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
}
