package art.arcane.mystcraft.platform.services;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Centralizes Minecraft API calls that are sensitive to mappings or loader
 * setup.
 */
public interface IVersionHelper {

  /**
   * Gets the Minecraft version this helper targets.
   *
   * @return Target Minecraft version string.
   */
  String getMinecraftVersion();

  /**
   * Computes or loads saved data using its 26.2 type definition.
   *
   * @param level The server level to get data storage from
   * @param type  The saved-data type, including its factory and codec
   * @param <T>   SavedData type
   * @return The SavedData instance
   */
  <T extends SavedData> T computeSavedData(ServerLevel level, SavedDataType<T> type);

}
