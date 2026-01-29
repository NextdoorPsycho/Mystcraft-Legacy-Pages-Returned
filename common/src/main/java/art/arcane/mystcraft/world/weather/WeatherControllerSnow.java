package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

/**
 * Weather controller that provides constant snow-producing precipitation.
 * Rain is always active but biomes are treated as cold (for snow).
 * This controller sets the weather, but temperature handling is done at biome level.
 */
public class WeatherControllerSnow implements IWeatherController {

  public static final String TYPE = "snow";

  private float rainLevel = 0.0f;

  @Override
  public void updateWeather(ServerLevel level) {
    // Gradually increase rain level (snow particles use same system)
    rainLevel = Math.min(1.0f, rainLevel + 0.02f);

    // Force precipitation
    if (!level.isRaining()) {
      level.setWeatherParameters(0, 6000, true, false);
    }
  }

  @Override
  public boolean isRaining() {
    return true; // Technically raining, but appears as snow due to cold biome temperature
  }

  @Override
  public boolean isThundering() {
    return false;
  }

  @Override
  public float getRainLevel() {
    return rainLevel;
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
   * Snow controller should force cold temperatures.
   * This is checked by biome-related code to determine if precipitation falls as snow.
   *
   * @return True to indicate this controller forces cold weather
   */
  public boolean forceCold() {
    return true;
  }
}
