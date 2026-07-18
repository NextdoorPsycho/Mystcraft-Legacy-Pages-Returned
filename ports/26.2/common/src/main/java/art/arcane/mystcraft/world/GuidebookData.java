package art.arcane.mystcraft.world;

import com.mojang.serialization.Codec;
import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.Services;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players have received the Mystcraft guidebook. Stored as
 * server-level saved data.
 */
public class GuidebookData extends SavedData {

  public static final Codec<GuidebookData> CODEC = CompoundTag.CODEC.xmap(
      GuidebookData::load,
      data -> data.save(new CompoundTag())
  );
  public static final SavedDataType<GuidebookData> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "guidebook"),
      GuidebookData::new,
      CODEC,
      DataFixTypes.SAVED_DATA_COMMAND_STORAGE
  );
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
    return Services.VERSION.computeSavedData(overworld, TYPE);
  }

  public static GuidebookData get(ServerLevel level) {
    return get(level.getServer());
  }

  private void loadFromTag(CompoundTag tag) {
    playersGiven.clear();
    ListTag list = tag.getListOrEmpty(TAG_PLAYERS);
    for (int i = 0; i < list.size(); i++) {
      try {
        playersGiven.add(UUID.fromString(list.getStringOr(i, "")));
      } catch (IllegalArgumentException ignored) {

      }
    }
  }

  @NotNull
  public CompoundTag save(@NotNull CompoundTag tag) {
    ListTag list = new ListTag();
    for (UUID uuid : playersGiven) {
      list.add(net.minecraft.nbt.StringTag.valueOf(uuid.toString()));
    }
    tag.put(TAG_PLAYERS, list);
    return tag;
  }

  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    return save(tag);
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
