package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

import java.util.Random;

/**
 * Weather controller with slower weather cycles.
 * Weather changes less frequently than normal, with longer durations.
 */
public class WeatherControllerSlow implements IWeatherController {

  public static final String TYPE = "slow";
  // Slow timing: 4x normal duration
  private static final int RAIN_DURATION_BASE = 48000;
  private static final int RAIN_DURATION_VARIANCE = 48000;
  private static final int RAIN_COOLDOWN_BASE = 48000;
  private static final int RAIN_COOLDOWN_VARIANCE = 672000;
  private static final int THUNDER_DURATION_BASE = 14400;
  private static final int THUNDER_DURATION_VARIANCE = 48000;
  private static final int THUNDER_COOLDOWN_BASE = 48000;
  private static final int THUNDER_COOLDOWN_VARIANCE = 672000;
  private final Random random = new Random();
  private float rainLevel = 0.0f;
  private float thunderLevel = 0.0f;
  private boolean raining = false;
  private boolean thundering = false;
  private int rainTime = 0;
  private int thunderTime = 0;

  public WeatherControllerSlow() {
    rainTime = RAIN_COOLDOWN_BASE + random.nextInt(RAIN_COOLDOWN_VARIANCE);
    thunderTime = THUNDER_COOLDOWN_BASE + random.nextInt(THUNDER_COOLDOWN_VARIANCE);
  }

  @Override
  public void updateWeather(ServerLevel level) {
    // Update rain timer (slower cycles)
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

    // Update thunder timer
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

    // Slower transitions (0.5x speed)
    if (raining) {
      rainLevel = Math.min(1.0f, rainLevel + 0.005f);
    } else {
      rainLevel = Math.max(0.0f, rainLevel - 0.005f);
    }

    if (thundering && raining) {
      thunderLevel = Math.min(1.0f, thunderLevel + 0.005f);
    } else {
      thunderLevel = Math.max(0.0f, thunderLevel - 0.005f);
    }

    level.setWeatherParameters(
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
