package art.arcane.mystcraft.api.world.logic;

import net.minecraft.server.level.ServerLevel;

/**
 * Interface for weather controllers that manage precipitation and weather
 * events.
 */
public interface IWeatherController {

  /**
   * Updates the weather for this Age. Called each tick to potentially change
   * weather state.
   *
   * @param level The server level
   */
  void updateWeather(ServerLevel level);

  /**
   * Whether it is currently raining in this Age.
   *
   * @return true if raining
   */
  boolean isRaining();

  /**
   * Whether it is currently thundering in this Age.
   *
   * @return true if thundering
   */
  boolean isThundering();

  /**
   * Gets the current rain strength.
   *
   * @return Rain strength from 0.0 to 1.0
   */
  float getRainLevel();

  /**
   * Gets the current thunder strength.
   *
   * @return Thunder strength from 0.0 to 1.0
   */
  float getThunderLevel();

  /**
   * Whether weather can change in this Age.
   *
   * @return true if weather is dynamic, false if fixed
   */
  boolean canWeatherChange();

  /**
   * Gets the weather type identifier.
   *
   * @return The type name (e.g., "normal", "always_rain", "never")
   */
  String getType();
}
