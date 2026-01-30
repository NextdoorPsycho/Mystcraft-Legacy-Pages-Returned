package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players have received the Mystcraft guidebook.
 * Stored as server-level saved data.
 */
public class GuidebookData extends SavedData {

  private static final String DATA_NAME = Mystcraft.MOD_ID + "_guidebook";
  private static final String TAG_PLAYERS = "Players";

  private final Set<UUID> playersGiven = new HashSet<>();

  public GuidebookData() {
  }

  public static GuidebookData load(CompoundTag tag) {
    GuidebookData data = new GuidebookData();
    data.loadFromTag(tag);
    return data;
  }

  public static GuidebookData get(MinecraftServer server) {
    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
    if (overworld == null) {
      throw new IllegalStateException("Overworld not available");
    }
    return Services.VERSION.computeSavedData(
        overworld,
        GuidebookData::new,
        GuidebookData::load,
        DATA_NAME
    );
  }

  public static GuidebookData get(ServerLevel level) {
    return get(level.getServer());
  }

  private void loadFromTag(CompoundTag tag) {
    playersGiven.clear();
    ListTag list = tag.getList(TAG_PLAYERS, Tag.TAG_STRING);
    for (int i = 0; i < list.size(); i++) {
      try {
        playersGiven.add(UUID.fromString(list.getString(i)));
      } catch (IllegalArgumentException ignored) {
        // Skip malformed entries
      }
    }
  }

  @Override
  @NotNull
  public CompoundTag save(@NotNull CompoundTag tag) {
    ListTag list = new ListTag();
    for (UUID uuid : playersGiven) {
      list.add(net.minecraft.nbt.StringTag.valueOf(uuid.toString()));
    }
    tag.put(TAG_PLAYERS, list);
    return tag;
  }

  public boolean hasReceived(UUID playerId) {
    return playersGiven.contains(playerId);
  }

  public void markReceived(UUID playerId) {
    if (playersGiven.add(playerId)) {
      setDirty();
    }
  }
}
