package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

/**
 * Weather controller that prevents all weather.
 * Clear skies always.
 */
public class WeatherControllerNever implements IWeatherController {

  public static final String TYPE = "never";

  @Override
  public void updateWeather(ServerLevel level) {
    // Force clear weather
    if (level.isRaining() || level.isThundering()) {
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
    return 0.0f;
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
