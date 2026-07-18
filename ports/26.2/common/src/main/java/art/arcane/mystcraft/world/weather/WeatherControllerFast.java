package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

import java.util.Random;

/**
 * Weather controller with faster weather cycles. Weather changes more
 * frequently than normal.
 */
public class WeatherControllerFast implements IWeatherController {

  public static final String TYPE = "fast";

  private static final int RAIN_DURATION_BASE = 3000;
  private static final int RAIN_DURATION_VARIANCE = 3000;
  private static final int RAIN_COOLDOWN_BASE = 3000;
  private static final int RAIN_COOLDOWN_VARIANCE = 42000;
  private static final int THUNDER_DURATION_BASE = 900;
  private static final int THUNDER_DURATION_VARIANCE = 3000;
  private static final int THUNDER_COOLDOWN_BASE = 3000;
  private static final int THUNDER_COOLDOWN_VARIANCE = 42000;
  private final Random random = new Random();
  private float rainLevel = 0.0f;
  private float thunderLevel = 0.0f;
  private boolean raining = false;
  private boolean thundering = false;
  private int rainTime = 0;
  private int thunderTime = 0;

  public WeatherControllerFast() {
    rainTime = RAIN_COOLDOWN_BASE + random.nextInt(RAIN_COOLDOWN_VARIANCE);
    thunderTime = THUNDER_COOLDOWN_BASE + random.nextInt(THUNDER_COOLDOWN_VARIANCE);
  }

  @Override
  public void updateWeather(ServerLevel level) {

    if (rainTime > 0) {
      rainTime--;
      if (rainTime <= 0) {
        raining = !raining;
        if (raining) {
          rainTime = RAIN_DURATION_BASE + random.nextInt(RAIN_DURATION_VARIANCE);
        } else {
          rainTime = RAIN_COOLDOWN_BASE + random.nextInt(RAIN_COOLDOWN_VARIANCE);
        }
      }
    }

    if (raining && thunderTime > 0) {
      thunderTime--;
      if (thunderTime <= 0) {
        thundering = !thundering;
        if (thundering) {
          thunderTime = THUNDER_DURATION_BASE + random.nextInt(THUNDER_DURATION_VARIANCE);
        } else {
          thunderTime = THUNDER_COOLDOWN_BASE + random.nextInt(THUNDER_COOLDOWN_VARIANCE);
        }
      }
    } else if (!raining) {
      thundering = false;
    }

    if (raining) {
      rainLevel = Math.min(1.0f, rainLevel + 0.02f);
    } else {
      rainLevel = Math.max(0.0f, rainLevel - 0.02f);
    }

    if (thundering && raining) {
      thunderLevel = Math.min(1.0f, thunderLevel + 0.02f);
    } else {
      thunderLevel = Math.max(0.0f, thunderLevel - 0.02f);
    }

    level.getServer().setWeatherParameters(
        raining ? 0 : rainTime,
        raining ? rainTime : 0,
        raining,
        thundering
    );
  }

  @Override
  public boolean isRaining() {
    return rainLevel > 0.0f;
  }

  @Override
  public boolean isThundering() {
    return thunderLevel > 0.0f;
  }

  @Override
  public float getRainLevel() {
    return rainLevel;
  }

  @Override
  public float getThunderLevel() {
    return thunderLevel;
  }

  @Override
  public boolean canWeatherChange() {
    return true;
  }

  @Override
  public String getType() {
    return TYPE;
  }
}
