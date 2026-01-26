package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.celestial.CelestialSun;
import art.arcane.mystcraft.world.celestial.CelestialMoon;
import art.arcane.mystcraft.world.celestial.CelestialStars;

/**
 * Celestial object symbols (sun, moon, stars).
 * These symbols register celestial implementations that control how celestial
 * bodies appear and behave in the Age's sky.
 */
public final class CelestialSymbols {

    private CelestialSymbols() {}

    public static void register() {
        // Sun
        SymbolRegistry.register(new SunNormal());
        SymbolRegistry.register(new SunDark());

        // Moon
        SymbolRegistry.register(new MoonNormal());
        SymbolRegistry.register(new MoonDark());

        // Stars
        SymbolRegistry.register(new StarsNormal());
        SymbolRegistry.register(new StarsTwinkle());
        SymbolRegistry.register(new StarsEnd());
        SymbolRegistry.register(new StarsDark());
    }

    public static class SunNormal extends SymbolBase {
        public SunNormal() {
            super(SymbolRegistry.mystcraftId("sun_normal"), SymbolCategory.SUN);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Celestial", "Image", "Stimulate", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSunVisible(true);

            // Register actual celestial for custom rendering
            CelestialSun sun = new CelestialSun("sun_normal");
            sun.setVisible(true);

            // Apply any color from the modifier stack
            int color = director.popColor();
            if (color != -1) {
                sun.setColor(color);
            }

            // Apply angle modifier
            float angle = director.popAngle();
            sun.setAngle(angle);

            director.registerInterface(sun);
        }
    }

    public static class SunDark extends SymbolBase {
        public SunDark() {
            super(SymbolRegistry.mystcraftId("sun_dark"), SymbolCategory.SUN);
            setCardRank(1);
            setInstabilityCost(5.0f);
            setPoem("Celestial", "Void", "Inhibit", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSunVisible(false);

            // Register a dark sun (appears as black silhouette)
            CelestialSun sun = new CelestialSun("sun_dark");
            sun.setVisible(true);
            sun.setDark(true);

            // Apply angle modifier
            float angle = director.popAngle();
            sun.setAngle(angle);

            director.registerInterface(sun);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class MoonNormal extends SymbolBase {
        public MoonNormal() {
            super(SymbolRegistry.mystcraftId("moon_normal"), SymbolCategory.MOON);
            setCardRank(1);
            setInstabilityCost(0.0f);
            setPoem("Celestial", "Image", "Cycle", "Wisdom");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMoonVisible(true);

            // Register actual celestial
            CelestialMoon moon = new CelestialMoon("moon_normal");
            moon.setVisible(true);

            // Apply any color from the modifier stack
            int color = director.popColor();
            if (color != -1) {
                moon.setColor(color);
            }

            // Apply angle modifier
            float angle = director.popAngle();
            moon.setAngle(angle);

            director.registerInterface(moon);
        }
    }

    public static class MoonDark extends SymbolBase {
        public MoonDark() {
            super(SymbolRegistry.mystcraftId("moon_dark"), SymbolCategory.MOON);
            setCardRank(1);
            setInstabilityCost(3.0f);
            setPoem("Celestial", "Void", "Inhibit", "Wisdom");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMoonVisible(false);

            // Register a dark moon
            CelestialMoon moon = new CelestialMoon("moon_dark");
            moon.setVisible(true);
            moon.setDark(true);

            // Apply angle modifier
            float angle = director.popAngle();
            moon.setAngle(angle);

            director.registerInterface(moon);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class StarsNormal extends SymbolBase {
        public StarsNormal() {
            super(SymbolRegistry.mystcraftId("stars_normal"), SymbolCategory.STARS);
            setCardRank(1);
            setInstabilityCost(0.0f);
            setPoem("Celestial", "Harmony", "Ethereal", "Order");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarsVisible(true);
            director.setStarType("normal");

            CelestialStars stars = new CelestialStars("stars_normal");
            stars.setVisible(true);
            stars.setStarType("normal");

            // Apply any color from the modifier stack
            int color = director.popColor();
            if (color != -1) {
                stars.setColor(color);
            }

            director.registerInterface(stars);
        }
    }

    public static class StarsTwinkle extends SymbolBase {
        public StarsTwinkle() {
            super(SymbolRegistry.mystcraftId("stars_twinkle"), SymbolCategory.STARS);
            setCardRank(1);
            setInstabilityCost(0.0f);
            setPoem("Celestial", "Harmony", "Ethereal", "Entropy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarsVisible(true);
            director.setStarType("twinkle");

            CelestialStars stars = new CelestialStars("stars_twinkle");
            stars.setVisible(true);
            stars.setStarType("twinkle");

            // Apply any color from the modifier stack
            int color = director.popColor();
            if (color != -1) {
                stars.setColor(color);
            }

            director.registerInterface(stars);
        }
    }

    public static class StarsEnd extends SymbolBase {
        public StarsEnd() {
            super(SymbolRegistry.mystcraftId("stars_end"), SymbolCategory.STARS);
            setCardRank(1);
            setInstabilityCost(5.0f);
            setPoem("Celestial", "Image", "Chaos", "Weave");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarsVisible(true);
            director.setStarType("end");

            CelestialStars stars = new CelestialStars("stars_end");
            stars.setVisible(true);
            stars.setStarType("end");

            // Apply any color from the modifier stack
            int color = director.popColor();
            if (color != -1) {
                stars.setColor(color);
            }

            director.registerInterface(stars);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class StarsDark extends SymbolBase {
        public StarsDark() {
            super(SymbolRegistry.mystcraftId("stars_dark"), SymbolCategory.STARS);
            setCardRank(1);
            setInstabilityCost(2.0f);
            setPoem("Celestial", "Void", "Inhibit", "Order");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarsVisible(false);

            // Register stars with "dark" type (no stars rendered)
            CelestialStars stars = new CelestialStars("stars_dark");
            stars.setVisible(false);
            stars.setStarType("dark");

            director.registerInterface(stars);
            director.addInstability(getInstabilityCost());
        }
    }
}
