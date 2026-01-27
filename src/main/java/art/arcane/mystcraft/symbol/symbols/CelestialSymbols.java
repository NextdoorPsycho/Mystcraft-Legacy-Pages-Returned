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
        SymbolRegistry.register(new SunLarge());
        SymbolRegistry.register(new SunSmall());
        SymbolRegistry.register(new SunFast());
        SymbolRegistry.register(new SunSlow());

        // Moon
        SymbolRegistry.register(new MoonNormal());
        SymbolRegistry.register(new MoonDark());
        SymbolRegistry.register(new MoonLarge());
        SymbolRegistry.register(new MoonSmall());
        SymbolRegistry.register(new MoonFull());
        SymbolRegistry.register(new MoonFast());
        SymbolRegistry.register(new MoonSlow());

        // Stars
        SymbolRegistry.register(new StarsNormal());
        SymbolRegistry.register(new StarsTwinkle());
        SymbolRegistry.register(new StarsEnd());
        SymbolRegistry.register(new StarsDark());
        SymbolRegistry.register(new StarsDense());
        SymbolRegistry.register(new StarsSparse());
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

    // --- Sun Size & Speed Variants ---

    public static class SunLarge extends SymbolBase {
        public SunLarge() {
            super(SymbolRegistry.mystcraftId("sun_large"), SymbolCategory.SUN);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Celestial", "Image", "Growth", "Power");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSunVisible(true);
            CelestialSun sun = new CelestialSun("sun_large");
            sun.setVisible(true);
            sun.setSize(2.5f);
            int color = director.popColor();
            if (color != -1) { sun.setColor(color); }
            float angle = director.popAngle();
            sun.setAngle(angle);
            director.registerInterface(sun);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SunSmall extends SymbolBase {
        public SunSmall() {
            super(SymbolRegistry.mystcraftId("sun_small"), SymbolCategory.SUN);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Celestial", "Image", "Constraint", "Focus");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSunVisible(true);
            CelestialSun sun = new CelestialSun("sun_small");
            sun.setVisible(true);
            sun.setSize(0.4f);
            int color = director.popColor();
            if (color != -1) { sun.setColor(color); }
            float angle = director.popAngle();
            sun.setAngle(angle);
            director.registerInterface(sun);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SunFast extends SymbolBase {
        public SunFast() {
            super(SymbolRegistry.mystcraftId("sun_fast"), SymbolCategory.SUN);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Celestial", "Dynamic", "Spur", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSunVisible(true);
            CelestialSun sun = new CelestialSun("sun_fast");
            sun.setVisible(true);
            sun.setPeriod(0.5f);
            int color = director.popColor();
            if (color != -1) { sun.setColor(color); }
            float angle = director.popAngle();
            sun.setAngle(angle);
            director.registerInterface(sun);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class SunSlow extends SymbolBase {
        public SunSlow() {
            super(SymbolRegistry.mystcraftId("sun_slow"), SymbolCategory.SUN);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Celestial", "Dynamic", "Inhibit", "Patience");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSunVisible(true);
            CelestialSun sun = new CelestialSun("sun_slow");
            sun.setVisible(true);
            sun.setPeriod(2.0f);
            int color = director.popColor();
            if (color != -1) { sun.setColor(color); }
            float angle = director.popAngle();
            sun.setAngle(angle);
            director.registerInterface(sun);
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Moon Size & Speed Variants ---

    public static class MoonLarge extends SymbolBase {
        public MoonLarge() {
            super(SymbolRegistry.mystcraftId("moon_large"), SymbolCategory.MOON);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Celestial", "Image", "Growth", "Wisdom");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMoonVisible(true);
            CelestialMoon moon = new CelestialMoon("moon_large");
            moon.setVisible(true);
            moon.setSize(2.5f);
            int color = director.popColor();
            if (color != -1) { moon.setColor(color); }
            float angle = director.popAngle();
            moon.setAngle(angle);
            director.registerInterface(moon);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class MoonSmall extends SymbolBase {
        public MoonSmall() {
            super(SymbolRegistry.mystcraftId("moon_small"), SymbolCategory.MOON);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Celestial", "Image", "Constraint", "Wisdom");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMoonVisible(true);
            CelestialMoon moon = new CelestialMoon("moon_small");
            moon.setVisible(true);
            moon.setSize(0.4f);
            int color = director.popColor();
            if (color != -1) { moon.setColor(color); }
            float angle = director.popAngle();
            moon.setAngle(angle);
            director.registerInterface(moon);
            director.addInstability(getInstabilityCost());
        }
    }

    /** Full moon at all times (no phase cycling). */
    public static class MoonFull extends SymbolBase {
        public MoonFull() {
            super(SymbolRegistry.mystcraftId("moon_full"), SymbolCategory.MOON);
            setCardRank(2);
            setInstabilityCost(2.0f);
            setPoem("Celestial", "Image", "Static", "Wisdom");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMoonVisible(true);
            CelestialMoon moon = new CelestialMoon("moon_full");
            moon.setVisible(true);
            moon.setShowPhases(false);
            int color = director.popColor();
            if (color != -1) { moon.setColor(color); }
            float angle = director.popAngle();
            moon.setAngle(angle);
            director.registerInterface(moon);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class MoonFast extends SymbolBase {
        public MoonFast() {
            super(SymbolRegistry.mystcraftId("moon_fast"), SymbolCategory.MOON);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Celestial", "Dynamic", "Spur", "Wisdom");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMoonVisible(true);
            CelestialMoon moon = new CelestialMoon("moon_fast");
            moon.setVisible(true);
            moon.setPeriod(0.5f);
            int color = director.popColor();
            if (color != -1) { moon.setColor(color); }
            float angle = director.popAngle();
            moon.setAngle(angle);
            director.registerInterface(moon);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class MoonSlow extends SymbolBase {
        public MoonSlow() {
            super(SymbolRegistry.mystcraftId("moon_slow"), SymbolCategory.MOON);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Celestial", "Dynamic", "Inhibit", "Wisdom");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMoonVisible(true);
            CelestialMoon moon = new CelestialMoon("moon_slow");
            moon.setVisible(true);
            moon.setPeriod(2.0f);
            int color = director.popColor();
            if (color != -1) { moon.setColor(color); }
            float angle = director.popAngle();
            moon.setAngle(angle);
            director.registerInterface(moon);
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Star Density Variants ---

    public static class StarsDense extends SymbolBase {
        public StarsDense() {
            super(SymbolRegistry.mystcraftId("stars_dense"), SymbolCategory.STARS);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Celestial", "Harmony", "Abundance", "Order");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarsVisible(true);
            director.setStarType("normal");
            CelestialStars stars = new CelestialStars("stars_dense");
            stars.setVisible(true);
            stars.setStarType("normal");
            stars.setStarCount(4000);
            int color = director.popColor();
            if (color != -1) { stars.setColor(color); }
            director.registerInterface(stars);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class StarsSparse extends SymbolBase {
        public StarsSparse() {
            super(SymbolRegistry.mystcraftId("stars_sparse"), SymbolCategory.STARS);
            setCardRank(1);
            setInstabilityCost(0.0f);
            setPoem("Celestial", "Harmony", "Constraint", "Void");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStarsVisible(true);
            director.setStarType("normal");
            CelestialStars stars = new CelestialStars("stars_sparse");
            stars.setVisible(true);
            stars.setStarType("normal");
            stars.setStarCount(400);
            int color = director.popColor();
            if (color != -1) { stars.setColor(color); }
            director.registerInterface(stars);
        }
    }
}
