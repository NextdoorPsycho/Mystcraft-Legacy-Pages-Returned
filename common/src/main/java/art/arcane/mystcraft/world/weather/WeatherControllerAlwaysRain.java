package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

/**
 * Weather controller that maintains constant rain. No thunder, just perpetual
 * precipitation.
 */
public class WeatherControllerAlwaysRain implements IWeatherController {

  public static final String TYPE = "always_rain";

  private float rainLevel = 0.0f;

  @Override
  public void updateWeather(ServerLevel level) {

    rainLevel = Math.min(1.0f, rainLevel + 0.02f);

    if (!level.isRaining()) {
      level.setWeatherParameters(0, 6000, true, false);
    }
  }

  @Override
  public boolean isRaining() {
    return true;
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
}
