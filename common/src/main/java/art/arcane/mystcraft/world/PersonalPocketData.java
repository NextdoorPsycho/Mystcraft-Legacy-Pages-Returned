package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
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

  private final Map<UUID, CompoundTag> returnLinks = new HashMap<>();

  public static SavedData.Factory<PersonalPocketData> factory() {
    return new SavedData.Factory<>(PersonalPocketData::new, PersonalPocketData::load, DataFixTypes.LEVEL);
  }

  public static PersonalPocketData load(CompoundTag tag) {
    PersonalPocketData data = new PersonalPocketData();
    data.loadFromTag(tag);
    return data;
  }

  public static PersonalPocketData get(MinecraftServer server) {
    ServerLevel overworld = server.overworld();
    return overworld.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
  }

  private void loadFromTag(CompoundTag tag) {
    returnLinks.clear();
    if (!tag.contains(TAG_RETURNS)) {
      return;
    }
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
}
