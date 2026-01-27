package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

import java.util.Random;

/**
 * Color target symbols that apply pushed colors to specific world elements.
 * These symbols consume colors from the modifier stack and apply them.
 * When the stack is empty, a random vibrant color is generated.
 */
public final class ColorTargetSymbols {

    private ColorTargetSymbols() {}

    // Vibrant color palette for random generation
    private static final int[] VIBRANT_COLORS = {
        0x87CEEB, // Sky blue
        0x98FB98, // Pale green
        0xFFB6C1, // Light pink
        0xDDA0DD, // Plum
        0xF0E68C, // Khaki
        0xADD8E6, // Light blue
        0x90EE90, // Light green
        0xFFDAB9, // Peach
        0xE6E6FA, // Lavender
        0xFFFACD, // Lemon chiffon
        0xB0E0E6, // Powder blue
        0xE0FFFF, // Light cyan
        0x7FFFD4, // Aquamarine
        0xFFE4B5, // Moccasin
        0xFAFAD2, // Light goldenrod
        0xD8BFD8, // Thistle
    };

    private static final int[] SKY_COLORS = {
        0x87CEEB, // Light sky blue
        0x00BFFF, // Deep sky blue
        0x87CEFA, // Light sky blue
        0xB0C4DE, // Light steel blue
        0x6495ED, // Cornflower blue
        0x4169E1, // Royal blue
        0xADD8E6, // Light blue
        0xE0FFFF, // Light cyan
        0xFFB6C1, // Light pink (sunset)
        0xFFA07A, // Light salmon (sunset)
        0x98FB98, // Pale green (alien)
        0xDDA0DD, // Plum (alien)
    };

    private static final int[] FOG_COLORS = {
        0xC0C0C0, // Silver
        0xD3D3D3, // Light gray
        0xE8E8E8, // Very light gray
        0xB0E0E6, // Powder blue
        0xE6E6FA, // Lavender
        0xFFFAF0, // Floral white
        0xF5F5DC, // Beige
        0x98FB98, // Pale green
    };

    private static final int[] GRASS_COLORS = {
        0x7CFC00, // Lawn green
        0x90EE90, // Light green
        0x32CD32, // Lime green
        0x228B22, // Forest green
        0x006400, // Dark green
        0x9ACD32, // Yellow green
        0x6B8E23, // Olive drab
        0x556B2F, // Dark olive green
        0xADFF2F, // Green yellow
        0x98FB98, // Pale green
        0x8B4513, // Saddle brown (autumn)
        0xFF8C00, // Dark orange (autumn)
    };

    private static final int[] FOLIAGE_COLORS = {
        0x228B22, // Forest green
        0x006400, // Dark green
        0x2E8B57, // Sea green
        0x3CB371, // Medium sea green
        0x20B2AA, // Light sea green
        0x008B8B, // Dark cyan
        0x556B2F, // Dark olive green
        0x8B4513, // Saddle brown (autumn)
        0xFF4500, // Orange red (autumn)
        0xFFD700, // Gold (autumn)
    };

    private static final int[] WATER_COLORS = {
        0x1E90FF, // Dodger blue
        0x00CED1, // Dark turquoise
        0x40E0D0, // Turquoise
        0x48D1CC, // Medium turquoise
        0x00FFFF, // Cyan
        0x5F9EA0, // Cadet blue
        0x4682B4, // Steel blue
        0x6495ED, // Cornflower blue
        0x7B68EE, // Medium slate blue
        0x8A2BE2, // Blue violet
    };

    public static void register() {
        // Sky colors
        SymbolRegistry.register(new ColorSky());
        SymbolRegistry.register(new ColorSkyNatural());
        SymbolRegistry.register(new ColorSkyNight());

        // Cloud colors
        SymbolRegistry.register(new ColorCloud());
        SymbolRegistry.register(new ColorCloudNatural());

        // Fog colors
        SymbolRegistry.register(new ColorFog());
        SymbolRegistry.register(new ColorFogNatural());

        // Foliage colors
        SymbolRegistry.register(new ColorFoliage());
        SymbolRegistry.register(new ColorFoliageNatural());

        // Grass colors
        SymbolRegistry.register(new ColorGrass());
        SymbolRegistry.register(new ColorGrassNatural());

        // Water colors
        SymbolRegistry.register(new ColorWater());
        SymbolRegistry.register(new ColorWaterNatural());
    }

    /**
     * Gets a random color from an array using the seed.
     */
    private static int getRandomColor(int[] colors, long seed) {
        Random rand = new Random(seed);
        return colors[rand.nextInt(colors.length)];
    }

    // --- Sky Colors ---

    public static class ColorSky extends SymbolBase {
        public ColorSky() {
            super(SymbolRegistry.mystcraftId("color_sky"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Sky", "Image", "Color", "Form");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color == -1) {
                // Generate random sky color for vibrant ages
                color = getRandomColor(SKY_COLORS, seed);
                director.addInstability(3.0f); // Random colors add instability
            }
            director.setSkyColor(color);
        }
    }

    public static class ColorSkyNatural extends SymbolBase {
        public ColorSkyNatural() {
            super(SymbolRegistry.mystcraftId("color_sky_natural"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(3);  // Lower weight - custom colors are more vibrant
            setInstabilityCost(-2.0f);
            setPoem("Sky", "Image", "Nature", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSkyColorNatural(true);
        }
    }

    public static class ColorSkyNight extends SymbolBase {
        public ColorSkyNight() {
            super(SymbolRegistry.mystcraftId("color_sky_night"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Sky", "Image", "Color", "Void");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color == -1) {
                // Dark/subtle colors for night sky
                Random rand = new Random(seed);
                int r = rand.nextInt(40);
                int g = rand.nextInt(40);
                int b = 20 + rand.nextInt(60);
                color = (r << 16) | (g << 8) | b;
                director.addInstability(2.0f);
            }
            director.setNightSkyColor(color);
        }
    }

    // --- Cloud Colors ---

    public static class ColorCloud extends SymbolBase {
        public ColorCloud() {
            super(SymbolRegistry.mystcraftId("color_cloud"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Cloud", "Image", "Color", "Form");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color == -1) {
                // Generate soft, cloudy colors
                color = getRandomColor(VIBRANT_COLORS, seed);
                director.addInstability(2.0f);
            }
            director.setCloudColor(color);
        }
    }

    public static class ColorCloudNatural extends SymbolBase {
        public ColorCloudNatural() {
            super(SymbolRegistry.mystcraftId("color_cloud_natural"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(3);  // Lower weight - custom colors are more vibrant
            setInstabilityCost(-2.0f);
            setPoem("Cloud", "Image", "Nature", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setCloudColorNatural(true);
        }
    }

    // --- Fog Colors ---

    public static class ColorFog extends SymbolBase {
        public ColorFog() {
            super(SymbolRegistry.mystcraftId("color_fog"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Fog", "Image", "Color", "Form");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color == -1) {
                color = getRandomColor(FOG_COLORS, seed);
                director.addInstability(2.0f);
            }
            director.setFogColor(color);
        }
    }

    public static class ColorFogNatural extends SymbolBase {
        public ColorFogNatural() {
            super(SymbolRegistry.mystcraftId("color_fog_natural"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(3);  // Lower weight - custom colors are more vibrant
            setInstabilityCost(-2.0f);
            setPoem("Fog", "Image", "Nature", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setFogColorNatural(true);
        }
    }

    // --- Foliage Colors ---

    public static class ColorFoliage extends SymbolBase {
        public ColorFoliage() {
            super(SymbolRegistry.mystcraftId("color_foliage"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Foliage", "Image", "Color", "Form");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color == -1) {
                color = getRandomColor(FOLIAGE_COLORS, seed);
                director.addInstability(2.0f);
            }
            director.setFoliageColor(color);
        }
    }

    public static class ColorFoliageNatural extends SymbolBase {
        public ColorFoliageNatural() {
            super(SymbolRegistry.mystcraftId("color_foliage_natural"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(3);  // Lower weight - custom colors are more vibrant
            setInstabilityCost(-2.0f);
            setPoem("Foliage", "Image", "Nature", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setFoliageColorNatural(true);
        }
    }

    // --- Grass Colors ---

    public static class ColorGrass extends SymbolBase {
        public ColorGrass() {
            super(SymbolRegistry.mystcraftId("color_grass"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Grass", "Image", "Color", "Form");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color == -1) {
                color = getRandomColor(GRASS_COLORS, seed);
                director.addInstability(2.0f);
            }
            director.setGrassColor(color);
        }
    }

    public static class ColorGrassNatural extends SymbolBase {
        public ColorGrassNatural() {
            super(SymbolRegistry.mystcraftId("color_grass_natural"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(3);  // Lower weight - custom colors are more vibrant
            setInstabilityCost(-2.0f);
            setPoem("Grass", "Image", "Nature", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setGrassColorNatural(true);
        }
    }

    // --- Water Colors ---

    public static class ColorWater extends SymbolBase {
        public ColorWater() {
            super(SymbolRegistry.mystcraftId("color_water"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Water", "Image", "Color", "Form");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color == -1) {
                color = getRandomColor(WATER_COLORS, seed);
                director.addInstability(2.0f);
            }
            director.setWaterColor(color);
        }
    }

    public static class ColorWaterNatural extends SymbolBase {
        public ColorWaterNatural() {
            super(SymbolRegistry.mystcraftId("color_water_natural"), SymbolCategory.VISUAL_EFFECT);
            setCardRank(3);  // Lower weight - custom colors are more vibrant
            setInstabilityCost(-2.0f);
            setPoem("Water", "Image", "Nature", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWaterColorNatural(true);
        }
    }
}
