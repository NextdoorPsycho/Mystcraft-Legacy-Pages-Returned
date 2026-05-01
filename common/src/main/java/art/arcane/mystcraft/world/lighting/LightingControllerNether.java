package art.arcane.mystcraft.world.lighting;

import art.arcane.mystcraft.api.world.logic.ILightingController;

/**
 * Nether-style lighting controller - constant dim ambient light. Similar to the
 * Nether dimension with a base ambient light level.
 */
public class LightingControllerNether implements ILightingController {

  public static final String TYPE = "nether";

  @Override
  public int getSkyLightLevel() {
    return 0;
  }

  @Override
  public int getMinLightLevel() {
    return 7;
  }

  @Override
  public boolean hasFixedLighting() {
    return true;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public int scaleLighting(int blockLightValue) {

    return Math.max(blockLightValue, 7);
  }

  @Override
  public void generateLightBrightnessTable(float[] lightBrightnessTable) {

    float ambientLight = 0.1F;
    for (int i = 0; i <= 15; ++i) {
      float f1 = 1.0F - (float) i / 15.0F;
      lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - ambientLight) + ambientLight;
    }
  }

  @Override
  public float getAmbientLightMultiplier() {
    return 1.0f;
  }

  @Override
  public float getDarknessFactor() {
    return 0.0f;
  }
}
