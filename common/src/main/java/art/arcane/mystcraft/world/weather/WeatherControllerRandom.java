package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

import java.util.Random;

/**
 * Rapidly changing weather controller. Randomizes rain/thunder in short cycles
 * for chaotic environments.
 */
public class WeatherControllerRandom implements IWeatherController {

  public static final String TYPE = "random";

  private static final int CYCLE_BASE = 200;
  private static final int CYCLE_VARIANCE = 1000;
  private final Random random = new Random();
  private float rainLevel = 0.0f;
  private float thunderLevel = 0.0f;
  private boolean raining = false;
  private boolean thundering = false;
  private int cycleTime = 0;

  public WeatherControllerRandom() {
    resetCycle();
  }

  private void resetCycle() {
    cycleTime = CYCLE_BASE + random.nextInt(CYCLE_VARIANCE);
  }

  @Override
  public void updateWeather(ServerLevel level) {
    if (cycleTime > 0) {
      cycleTime--;
      if (cycleTime <= 0) {
        raining = random.nextBoolean();
        thundering = raining && random.nextFloat() < 0.45f;
        resetCycle();
      }
    }

    if (raining) {
      rainLevel = Math.min(1.0f, rainLevel + 0.04f);
    } else {
      rainLevel = Math.max(0.0f, rainLevel - 0.04f);
    }

    if (thundering && raining) {
      thunderLevel = Math.min(1.0f, thunderLevel + 0.05f);
    } else {
      thunderLevel = Math.max(0.0f, thunderLevel - 0.05f);
    }

    level.setWeatherParameters(
        raining ? 0 : cycleTime,
        raining ? cycleTime : 0,
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
