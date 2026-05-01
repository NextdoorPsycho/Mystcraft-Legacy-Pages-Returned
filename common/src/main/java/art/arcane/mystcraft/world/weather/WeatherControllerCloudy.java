package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

/**
 * Weather controller for cloudy skies without precipitation. Overcast
 * appearance but no actual rain or snow.
 */
public class WeatherControllerCloudy implements IWeatherController {

  public static final String TYPE = "cloudy";

  private float cloudLevel = 0.0f;

  @Override
  public void updateWeather(ServerLevel level) {

    cloudLevel = Math.min(0.5f, cloudLevel + 0.01f);

    if (level.isRaining()) {
      level.setWeatherParameters(6000, 0, false, false);
    }
  }

  @Override
  public boolean isRaining() {
    return false;
  }

  @Override
  public boolean isThundering() {
    return false;
  }

  @Override
  public float getRainLevel() {
    return cloudLevel;
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
}
