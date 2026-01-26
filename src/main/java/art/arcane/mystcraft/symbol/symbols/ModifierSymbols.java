package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

/**
 * Modifier symbols (colors, angles, phases, etc).
 */
public final class ModifierSymbols {

    private ModifierSymbols() {}

    public static void register() {
        // Angles
        SymbolRegistry.register(new AngleNorth());
        SymbolRegistry.register(new AngleEast());
        SymbolRegistry.register(new AngleSouth());
        SymbolRegistry.register(new AngleWest());

        // Phases
        SymbolRegistry.register(new PhaseNadir());
        SymbolRegistry.register(new PhaseRising());
        SymbolRegistry.register(new PhaseZenith());
        SymbolRegistry.register(new PhaseSetting());

        // Lengths
        SymbolRegistry.register(new LengthZero());
        SymbolRegistry.register(new LengthHalf());
        SymbolRegistry.register(new LengthFull());
        SymbolRegistry.register(new LengthDouble());

        // Colors
        SymbolRegistry.register(new ColorRed());
        SymbolRegistry.register(new ColorOrange());
        SymbolRegistry.register(new ColorYellow());
        SymbolRegistry.register(new ColorGreen());
        SymbolRegistry.register(new ColorBlue());
        SymbolRegistry.register(new ColorPurple());
        SymbolRegistry.register(new ColorWhite());
        SymbolRegistry.register(new ColorBlack());
        SymbolRegistry.register(new ColorCyan());
        SymbolRegistry.register(new ColorMagenta());
        SymbolRegistry.register(new ColorLime());
        SymbolRegistry.register(new ColorPink());
        SymbolRegistry.register(new ColorGray());
        SymbolRegistry.register(new ColorLightGray());
        SymbolRegistry.register(new ColorBrown());
        SymbolRegistry.register(new ColorLightBlue());

        // Special modifiers
        SymbolRegistry.register(new ModifierClear());
        SymbolRegistry.register(new NoSea());

        // Gradient modifiers (for sunset/celestial colors)
        SymbolRegistry.register(new GradientSunset());
        SymbolRegistry.register(new GradientDawn());
        SymbolRegistry.register(new GradientDusk());
    }

    // ========================= Angles =========================

    public static class AngleNorth extends SymbolBase {
        public AngleNorth() {
            super(SymbolRegistry.mystcraftId("mod_north"), SymbolCategory.ANGLE);
            setCardRank(0);
            setPoem("Modifier", "Flow", "Motion", "Control");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushAngle(0.0f);
        }
    }

    public static class AngleEast extends SymbolBase {
        public AngleEast() {
            super(SymbolRegistry.mystcraftId("mod_east"), SymbolCategory.ANGLE);
            setCardRank(0);
            setPoem("Modifier", "Flow", "Motion", "Tradition");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushAngle(90.0f);
        }
    }

    public static class AngleSouth extends SymbolBase {
        public AngleSouth() {
            super(SymbolRegistry.mystcraftId("mod_south"), SymbolCategory.ANGLE);
            setCardRank(0);
            setPoem("Modifier", "Flow", "Motion", "Chaos");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushAngle(180.0f);
        }
    }

    public static class AngleWest extends SymbolBase {
        public AngleWest() {
            super(SymbolRegistry.mystcraftId("mod_west"), SymbolCategory.ANGLE);
            setCardRank(0);
            setPoem("Modifier", "Flow", "Motion", "Change");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushAngle(270.0f);
        }
    }

    // ========================= Phases =========================

    public static class PhaseNadir extends SymbolBase {
        public PhaseNadir() {
            super(SymbolRegistry.mystcraftId("mod_nadir"), SymbolCategory.PHASE);
            setCardRank(0);
            setPoem("Modifier", "Cycle", "System", "Rebirth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushPhase(0.0f);
        }
    }

    public static class PhaseRising extends SymbolBase {
        public PhaseRising() {
            super(SymbolRegistry.mystcraftId("mod_rising"), SymbolCategory.PHASE);
            setCardRank(0);
            setPoem("Modifier", "Cycle", "System", "Growth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushPhase(90.0f);
        }
    }

    public static class PhaseZenith extends SymbolBase {
        public PhaseZenith() {
            super(SymbolRegistry.mystcraftId("mod_zenith"), SymbolCategory.PHASE);
            setCardRank(0);
            setPoem("Modifier", "Cycle", "System", "Harmony");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushPhase(180.0f);
        }
    }

    public static class PhaseSetting extends SymbolBase {
        public PhaseSetting() {
            super(SymbolRegistry.mystcraftId("mod_setting"), SymbolCategory.PHASE);
            setCardRank(0);
            setPoem("Modifier", "Cycle", "System", "Future");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushPhase(270.0f);
        }
    }

    // ========================= Lengths =========================

    public static class LengthZero extends SymbolBase {
        public LengthZero() {
            super(SymbolRegistry.mystcraftId("mod_zero"), SymbolCategory.LENGTH);
            setCardRank(0);
            setPoem("Modifier", "Time", "System", "Inhibit");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushLength(0.0f);
        }
    }

    public static class LengthHalf extends SymbolBase {
        public LengthHalf() {
            super(SymbolRegistry.mystcraftId("mod_half"), SymbolCategory.LENGTH);
            setCardRank(0);
            setPoem("Modifier", "Time", "System", "Stimulate");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushLength(0.5f);
        }
    }

    public static class LengthFull extends SymbolBase {
        public LengthFull() {
            super(SymbolRegistry.mystcraftId("mod_full"), SymbolCategory.LENGTH);
            setCardRank(0);
            setPoem("Modifier", "Time", "System", "Balance");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushLength(1.0f);
        }
    }

    public static class LengthDouble extends SymbolBase {
        public LengthDouble() {
            super(SymbolRegistry.mystcraftId("mod_double"), SymbolCategory.LENGTH);
            setCardRank(0);
            setPoem("Modifier", "Time", "System", "Sacrifice");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushLength(2.0f);
        }
    }

    // ========================= Colors =========================

    public static class ColorRed extends SymbolBase {
        public ColorRed() {
            super(SymbolRegistry.mystcraftId("color_red"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Fire", "Power");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFF0000);
        }
    }

    public static class ColorOrange extends SymbolBase {
        public ColorOrange() {
            super(SymbolRegistry.mystcraftId("color_orange"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Fire", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFF8000);
        }
    }

    public static class ColorYellow extends SymbolBase {
        public ColorYellow() {
            super(SymbolRegistry.mystcraftId("color_yellow"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFFFF00);
        }
    }

    public static class ColorGreen extends SymbolBase {
        public ColorGreen() {
            super(SymbolRegistry.mystcraftId("color_green"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Nature", "Growth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x00FF00);
        }
    }

    public static class ColorBlue extends SymbolBase {
        public ColorBlue() {
            super(SymbolRegistry.mystcraftId("color_blue"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Water", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x0000FF);
        }
    }

    public static class ColorPurple extends SymbolBase {
        public ColorPurple() {
            super(SymbolRegistry.mystcraftId("color_purple"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Magic", "Power");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x8000FF);
        }
    }

    public static class ColorWhite extends SymbolBase {
        public ColorWhite() {
            super(SymbolRegistry.mystcraftId("color_white"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Harmony");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFFFFFF);
        }
    }

    public static class ColorBlack extends SymbolBase {
        public ColorBlack() {
            super(SymbolRegistry.mystcraftId("color_black"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Void", "Darkness");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x000000);
        }
    }

    public static class ColorCyan extends SymbolBase {
        public ColorCyan() {
            super(SymbolRegistry.mystcraftId("color_cyan"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Water", "Harmony");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x00FFFF);
        }
    }

    public static class ColorMagenta extends SymbolBase {
        public ColorMagenta() {
            super(SymbolRegistry.mystcraftId("color_magenta"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Magic", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFF00FF);
        }
    }

    public static class ColorLime extends SymbolBase {
        public ColorLime() {
            super(SymbolRegistry.mystcraftId("color_lime"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Nature", "Energy");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x80FF00);
        }
    }

    public static class ColorPink extends SymbolBase {
        public ColorPink() {
            super(SymbolRegistry.mystcraftId("color_pink"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Nurture");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFF80C0);
        }
    }

    public static class ColorGray extends SymbolBase {
        public ColorGray() {
            super(SymbolRegistry.mystcraftId("color_gray"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Balance", "Constraint");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x808080);
        }
    }

    public static class ColorLightGray extends SymbolBase {
        public ColorLightGray() {
            super(SymbolRegistry.mystcraftId("color_light_gray"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Constraint");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xC0C0C0);
        }
    }

    public static class ColorBrown extends SymbolBase {
        public ColorBrown() {
            super(SymbolRegistry.mystcraftId("color_brown"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Earth", "Tradition");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x8B4513);
        }
    }

    public static class ColorLightBlue extends SymbolBase {
        public ColorLightBlue() {
            super(SymbolRegistry.mystcraftId("color_light_blue"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Sky", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x87CEEB);
        }
    }

    // ========================= Special Modifiers =========================

    public static class ModifierClear extends SymbolBase {
        public ModifierClear() {
            super(SymbolRegistry.mystcraftId("mod_clear"), SymbolCategory.MODIFIER);
            setCardRank(0);
            setInstabilityCost(0.0f);
            setPoem("Modifier", "Void", "System", "Clear");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.clearModifiers();
        }
    }

    public static class NoSea extends SymbolBase {
        public NoSea() {
            super(SymbolRegistry.mystcraftId("no_sea"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Terrain", "Water", "Inhibit", "Void");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setHasSea(false);
            director.addInstability(getInstabilityCost());
        }
    }

    // ========================= Gradient Modifiers =========================

    public static class GradientSunset extends SymbolBase {
        public GradientSunset() {
            super(SymbolRegistry.mystcraftId("gradient_sunset"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Image", "Gradient", "Sun", "Setting");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            // Orange-red sunset gradient
            int color = director.popColor();
            if (color != -1) {
                director.setSunsetColor(color);
            } else {
                // Default sunset color (orange-red)
                director.setSunsetColor(0xFF6B35);
            }
        }
    }

    public static class GradientDawn extends SymbolBase {
        public GradientDawn() {
            super(SymbolRegistry.mystcraftId("gradient_dawn"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Image", "Gradient", "Sun", "Rising");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            // Pink-yellow dawn gradient
            int color = director.popColor();
            if (color != -1) {
                director.setSunsetColor(color);
            } else {
                // Default dawn color (pink-gold)
                director.setSunsetColor(0xFFB366);
            }
        }
    }

    public static class GradientDusk extends SymbolBase {
        public GradientDusk() {
            super(SymbolRegistry.mystcraftId("gradient_dusk"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Image", "Gradient", "Sun", "Fading");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            // Purple-blue dusk gradient
            int color = director.popColor();
            if (color != -1) {
                director.setSunsetColor(color);
            } else {
                // Default dusk color (purple-blue)
                director.setSunsetColor(0x8B5A8B);
            }
        }
    }
}
