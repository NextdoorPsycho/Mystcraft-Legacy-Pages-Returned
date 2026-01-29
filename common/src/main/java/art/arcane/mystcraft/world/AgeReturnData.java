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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores per-player return locations for Mystcraft Ages.
 */
public class AgeReturnData extends SavedData {

  private static final String DATA_NAME = Mystcraft.MOD_ID + "_age_returns";
  private static final String TAG_RETURNS = "Returns";
  private static final String TAG_PLAYER = "Player";
  private static final String TAG_AGE = "AgeUID";
  private static final String TAG_LINK = "Link";

  private final Map<UUID, Map<Integer, CompoundTag>> returnLinks = new HashMap<>();

  public static AgeReturnData load(CompoundTag tag) {
    AgeReturnData data = new AgeReturnData();
    data.loadFromTag(tag);
    return data;
  }

  public static AgeReturnData get(MinecraftServer server) {
    ServerLevel overworld = server.overworld();
    return Services.VERSION.computeSavedData(
            overworld,
            AgeReturnData::new,
            AgeReturnData::load,
            DATA_NAME
    );
  }

  private void loadFromTag(CompoundTag tag) {
    returnLinks.clear();
    if (!tag.contains(TAG_RETURNS)) {
      return;
    }
    ListTag list = tag.getList(TAG_RETURNS, Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag entry = list.getCompound(i);
      if (!entry.contains(TAG_PLAYER) || !entry.contains(TAG_AGE) || !entry.contains(TAG_LINK)) {
        continue;
      }
      UUID playerId = UUID.fromString(entry.getString(TAG_PLAYER));
      int ageUID = entry.getInt(TAG_AGE);
      CompoundTag link = entry.getCompound(TAG_LINK);
      returnLinks.computeIfAbsent(playerId, key -> new HashMap<>())
          .put(ageUID, link.copy());
    }
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag list = new ListTag();
    for (Map.Entry<UUID, Map<Integer, CompoundTag>> playerEntry : returnLinks.entrySet()) {
      UUID playerId = playerEntry.getKey();
      for (Map.Entry<Integer, CompoundTag> ageEntry : playerEntry.getValue().entrySet()) {
        CompoundTag item = new CompoundTag();
        item.putString(TAG_PLAYER, playerId.toString());
        item.putInt(TAG_AGE, ageEntry.getKey());
        item.put(TAG_LINK, ageEntry.getValue().copy());
        list.add(item);
      }
    }
    tag.put(TAG_RETURNS, list);
    return tag;
  }

  public void setReturnLink(UUID playerId, int ageUID, CompoundTag linkData) {
    returnLinks.computeIfAbsent(playerId, key -> new HashMap<>())
        .put(ageUID, linkData.copy());
    setDirty();
  }

  @Nullable
  public CompoundTag getReturnLink(UUID playerId, int ageUID) {
    Map<Integer, CompoundTag> perAge = returnLinks.get(playerId);
    if (perAge == null) {
      return null;
    }
    CompoundTag data = perAge.get(ageUID);
    return data != null ? data.copy() : null;
  }
}
