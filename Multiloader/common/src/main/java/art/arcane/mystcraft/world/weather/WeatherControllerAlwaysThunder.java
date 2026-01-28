package art.arcane.mystcraft.world.weather;

import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.server.level.ServerLevel;

/**
 * Weather controller that maintains constant thunderstorms.
 * Perpetual rain and thunder with lightning strikes.
 */
public class WeatherControllerAlwaysThunder implements IWeatherController {

    public static final String TYPE = "always_thunder";

    private float rainLevel = 0.0f;
    private float thunderLevel = 0.0f;

    @Override
    public void updateWeather(ServerLevel level) {
        // Gradually increase both rain and thunder to max
        rainLevel = Math.min(1.0f, rainLevel + 0.02f);
        thunderLevel = Math.min(1.0f, thunderLevel + 0.02f);

        // Force thunderstorm in vanilla weather system
        if (!level.isThundering()) {
            level.setWeatherParameters(0, 6000, true, true);
        }
    }

    @Override
    public boolean isRaining() {
        return true;
    }

    @Override
    public boolean isThundering() {
        return true;
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
        return false;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
