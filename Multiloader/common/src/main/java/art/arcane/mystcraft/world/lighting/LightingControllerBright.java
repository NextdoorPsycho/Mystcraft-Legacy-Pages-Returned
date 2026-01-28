package art.arcane.mystcraft.world.lighting;

import art.arcane.mystcraft.api.world.logic.ILightingController;

/**
 * Bright lighting controller - boosts all light levels.
 * Uses a higher base brightness floor (f=0.25) and scales up block light.
 * Dark areas become less dark, making exploration safer.
 */
public class LightingControllerBright implements ILightingController {

    public static final String TYPE = "bright";

    @Override
    public int getSkyLightLevel() {
        return 15;
    }

    @Override
    public int getMinLightLevel() {
        return 4; // Minimum ambient light level
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
        // Boost light by half the remaining distance to max
        // Light 0 -> 7, Light 8 -> 11, Light 15 -> 15
        return blockLightValue + (15 - blockLightValue) / 2;
    }

    @Override
    public void generateLightBrightnessTable(float[] lightBrightnessTable) {
        // Brighter curve with f=0.25 floor
        // This ensures even light level 0 has some brightness
        float f = 0.25F;
        for (int i = 0; i < lightBrightnessTable.length; ++i) {
            float f1 = 1.0F - i / 15F;
            lightBrightnessTable[i] = ((1.0F - f1) / (f1 * 3F + 1.0F)) * (1.0F - f) + f;
        }
    }

    @Override
    public float getAmbientLightMultiplier() {
        return 1.3f; // 30% brighter ambient
    }

    @Override
    public float getDarknessFactor() {
        return 0.0f;
    }
}
