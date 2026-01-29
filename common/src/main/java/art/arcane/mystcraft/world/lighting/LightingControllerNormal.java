package art.arcane.mystcraft.world.lighting;

import art.arcane.mystcraft.api.world.logic.ILightingController;

/**
 * Normal lighting controller - standard Minecraft brightness curve.
 * No modifications to lighting behavior.
 */
public class LightingControllerNormal implements ILightingController {

  public static final String TYPE = "normal";

  @Override
  public int getSkyLightLevel() {
    return 15;
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
    return blockLightValue;
  }

  @Override
  public void generateLightBrightnessTable(float[] lightBrightnessTable) {
    // Standard Minecraft brightness curve
    float f = 0.0F;
    for (int i = 0; i <= 15; ++i) {
      float f1 = 1.0F - (float) i / 15.0F;
      lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - f) + f;
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
