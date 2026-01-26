package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Celestial object symbols (sun, moon, stars).
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
            director.addInstability(getInstabilityCost());
        }
    }
}
