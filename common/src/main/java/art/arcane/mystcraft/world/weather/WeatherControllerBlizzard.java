package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

/**
 * Weather controller for intense blizzard conditions.
 * Constant heavy snow with strong winds (high precipitation level).
 * Biomes are treated as cold for snow particles.
 */
public class WeatherControllerBlizzard implements IWeatherController {

  public static final String TYPE = "blizzard";

  private float rainLevel = 0.0f;

  @Override
  public void updateWeather(ServerLevel level) {
    // Rapidly increase precipitation to max (blizzard intensity)
    rainLevel = Math.min(1.0f, rainLevel + 0.05f);

    // Force heavy precipitation
    if (!level.isRaining()) {
      level.setWeatherParameters(0, 12000, true, false);
    }
  }

  @Override
  public boolean isRaining() {
    return true; // Precipitation active (renders as snow in cold)
  }

  @Override
  public boolean isThundering() {
    return false; // Blizzards don't have thunder
  }

  @Override
  public float getRainLevel() {
    return rainLevel; // High intensity precipitation
  }

  @Override
  public float getThunderLevel() {
    return 0.0f;
  }

  @Override
  public boolean canWeatherChange() {
    return false;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  /**
   * Blizzard controller forces cold temperatures.
   *
   * @return True to indicate this controller forces cold weather
   */
  public boolean forceCold() {
    return true;
  }
}
