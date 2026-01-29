package art.arcane.mystcraft.world.lighting;

import art.arcane.mystcraft.api.world.logic.ILightingController;

/**
 * Dark lighting controller - halves all light levels.
 * Creates a perpetually dim atmosphere even during the day.
 * Makes torches less effective and exploration more dangerous.
 */
public class LightingControllerDark implements ILightingController {

  public static final String TYPE = "dark";

  @Override
  public int getSkyLightLevel() {
    return 8; // Reduced sky light
  }

  @Override
  public int getMinLightLevel() {
    return -1; // No minimum override
  }

  @Override
  public boolean hasFixedLighting() {
    return false;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public int scaleLighting(int blockLightValue) {
    // Halve all light values
    return blockLightValue / 2;
  }

  @Override
  public void generateLightBrightnessTable(float[] lightBrightnessTable) {
    // Darker curve - halves all brightness values
    float f = 0.0F;
    for (int i = 0; i < lightBrightnessTable.length; ++i) {
      float f1 = 1.0F - i / 15F;
      lightBrightnessTable[i] = (((1.0F - f1) / (f1 * 3F + 1.0F)) * (1.0F - f) + f) / 2;
    }
  }

  @Override
  public float getAmbientLightMultiplier() {
    return 0.5f; // 50% darker ambient
  }

  @Override
  public float getDarknessFactor() {
    return 0.3f; // 30% perpetual darkness
  }
}
