package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ColorUtils;

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

        // Extended colors
        SymbolRegistry.register(new ColorMaroon());
        SymbolRegistry.register(new ColorOlive());
        SymbolRegistry.register(new ColorDarkGreen());
        SymbolRegistry.register(new ColorTeal());
        SymbolRegistry.register(new ColorNavy());
        SymbolRegistry.register(new ColorSilver());
        SymbolRegistry.register(new ColorGold());
        SymbolRegistry.register(new ColorCoral());
        SymbolRegistry.register(new ColorCrimson());
        SymbolRegistry.register(new ColorTurquoise());
        SymbolRegistry.register(new ColorIndigo());
        SymbolRegistry.register(new ColorAmber());
        SymbolRegistry.register(new ColorRose());
        SymbolRegistry.register(new ColorJade());
        SymbolRegistry.register(new ColorSapphire());
        SymbolRegistry.register(new ColorRuby());
        SymbolRegistry.register(new ColorViolet());
        SymbolRegistry.register(new ColorCopper());
        SymbolRegistry.register(new ColorMidnight());
        SymbolRegistry.register(new ColorEmerald());
        SymbolRegistry.register(new ColorPeach());
        SymbolRegistry.register(new ColorIvory());

        // Special modifiers
        SymbolRegistry.register(new ModifierClear());
        SymbolRegistry.register(new NoSea());

        // Cloud height modifiers
        SymbolRegistry.register(new CloudsLow());
        SymbolRegistry.register(new CloudsHigh());
        SymbolRegistry.register(new CloudsNone());

        // Horizon height modifiers
        SymbolRegistry.register(new HorizonLow());
        SymbolRegistry.register(new HorizonHigh());

        // Gradient modifiers (for sunset/celestial colors)
        SymbolRegistry.register(new GradientSunset());
        SymbolRegistry.register(new GradientDawn());
        SymbolRegistry.register(new GradientDusk());
        SymbolRegistry.register(new GradientAurora());
        SymbolRegistry.register(new GradientBloodSky());

        // Gradient builder symbols
        SymbolRegistry.register(new GradientBuilder());
        SymbolRegistry.register(new GradientApplySunset());
    }

    // --- Angles ---

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

    // --- Phases ---

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

    // --- Lengths ---

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

    // --- Colors ---

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

    // --- Legacy Colors ---

    public static class ColorMaroon extends SymbolBase {
        public ColorMaroon() {
            super(SymbolRegistry.mystcraftId("color_maroon"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Fire", "Darkness");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x800000);
        }
    }

    public static class ColorOlive extends SymbolBase {
        public ColorOlive() {
            super(SymbolRegistry.mystcraftId("color_olive"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Earth", "Growth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x808000);
        }
    }

    public static class ColorDarkGreen extends SymbolBase {
        public ColorDarkGreen() {
            super(SymbolRegistry.mystcraftId("color_dark_green"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Nature", "Darkness");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x008000);
        }
    }

    public static class ColorTeal extends SymbolBase {
        public ColorTeal() {
            super(SymbolRegistry.mystcraftId("color_teal"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Water", "Darkness");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x008080);
        }
    }

    public static class ColorNavy extends SymbolBase {
        public ColorNavy() {
            super(SymbolRegistry.mystcraftId("color_navy"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Void", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x000080);
        }
    }

    public static class ColorSilver extends SymbolBase {
        public ColorSilver() {
            super(SymbolRegistry.mystcraftId("color_silver"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Balance");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xC0C0C0);
        }
    }

    // --- Special Modifiers ---

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

    // --- Gradient Modifiers ---

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
            int color = director.popColor();
            if (color != -1) {
                director.setSunsetColor(color);
            } else {
                director.setSunsetColor(0x8B5A8B);
            }
        }
    }

    public static class GradientAurora extends SymbolBase {
        public GradientAurora() {
            super(SymbolRegistry.mystcraftId("gradient_aurora"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Image", "Gradient", "Sky", "Dance");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color != -1) {
                director.setSunsetColor(color);
            } else {
                director.setSunsetColor(0x00FF88);
            }
            director.addInstability(getInstabilityCost());
        }
    }

    public static class GradientBloodSky extends SymbolBase {
        public GradientBloodSky() {
            super(SymbolRegistry.mystcraftId("gradient_blood_sky"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Image", "Gradient", "Fire", "Omen");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color != -1) {
                director.setSunsetColor(color);
            } else {
                director.setSunsetColor(0x8B0000);
            }
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Extended Colors ---

    public static class ColorGold extends SymbolBase {
        public ColorGold() {
            super(SymbolRegistry.mystcraftId("color_gold"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Wealth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFFD700);
        }
    }

    public static class ColorCoral extends SymbolBase {
        public ColorCoral() {
            super(SymbolRegistry.mystcraftId("color_coral"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Water", "Warmth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFF7F50);
        }
    }

    public static class ColorCrimson extends SymbolBase {
        public ColorCrimson() {
            super(SymbolRegistry.mystcraftId("color_crimson"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Fire", "Sacrifice");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xDC143C);
        }
    }

    public static class ColorTurquoise extends SymbolBase {
        public ColorTurquoise() {
            super(SymbolRegistry.mystcraftId("color_turquoise"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Water", "Clarity");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x40E0D0);
        }
    }

    public static class ColorIndigo extends SymbolBase {
        public ColorIndigo() {
            super(SymbolRegistry.mystcraftId("color_indigo"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Void", "Mystery");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x4B0082);
        }
    }

    public static class ColorAmber extends SymbolBase {
        public ColorAmber() {
            super(SymbolRegistry.mystcraftId("color_amber"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Fire", "Ancient");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFFBF00);
        }
    }

    public static class ColorRose extends SymbolBase {
        public ColorRose() {
            super(SymbolRegistry.mystcraftId("color_rose"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Nature", "Nurture");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFF007F);
        }
    }

    public static class ColorJade extends SymbolBase {
        public ColorJade() {
            super(SymbolRegistry.mystcraftId("color_jade"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Earth", "Balance");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x00A86B);
        }
    }

    public static class ColorSapphire extends SymbolBase {
        public ColorSapphire() {
            super(SymbolRegistry.mystcraftId("color_sapphire"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Water", "Power");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x0F52BA);
        }
    }

    public static class ColorRuby extends SymbolBase {
        public ColorRuby() {
            super(SymbolRegistry.mystcraftId("color_ruby"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Fire", "Clarity");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xE0115F);
        }
    }

    public static class ColorViolet extends SymbolBase {
        public ColorViolet() {
            super(SymbolRegistry.mystcraftId("color_violet"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Magic", "Nature");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xEE82EE);
        }
    }

    public static class ColorCopper extends SymbolBase {
        public ColorCopper() {
            super(SymbolRegistry.mystcraftId("color_copper"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Earth", "Warmth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xB87333);
        }
    }

    public static class ColorMidnight extends SymbolBase {
        public ColorMidnight() {
            super(SymbolRegistry.mystcraftId("color_midnight"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Void", "Depth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x191970);
        }
    }

    public static class ColorEmerald extends SymbolBase {
        public ColorEmerald() {
            super(SymbolRegistry.mystcraftId("color_emerald"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Nature", "Clarity");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0x50C878);
        }
    }

    public static class ColorPeach extends SymbolBase {
        public ColorPeach() {
            super(SymbolRegistry.mystcraftId("color_peach"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Warmth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFFDAB9);
        }
    }

    public static class ColorIvory extends SymbolBase {
        public ColorIvory() {
            super(SymbolRegistry.mystcraftId("color_ivory"), SymbolCategory.COLOR);
            setCardRank(0);
            setPoem("Image", "Color", "Light", "Purity");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.pushColor(0xFFFFF0);
        }
    }

    // --- Cloud Height Modifiers ---

    public static class CloudsLow extends SymbolBase {
        public CloudsLow() {
            super(SymbolRegistry.mystcraftId("clouds_low"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Cloud", "Form", "Constraint", "Depth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setCloudHeight(96.0f);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class CloudsHigh extends SymbolBase {
        public CloudsHigh() {
            super(SymbolRegistry.mystcraftId("clouds_high"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Cloud", "Form", "Ascend", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setCloudHeight(300.0f);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class CloudsNone extends SymbolBase {
        public CloudsNone() {
            super(SymbolRegistry.mystcraftId("clouds_none"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Cloud", "Void", "Inhibit", "Clear");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            // Push clouds below the world so they never render
            director.setCloudHeight(-64.0f);
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Horizon Height Modifiers ---

    public static class HorizonLow extends SymbolBase {
        public HorizonLow() {
            super(SymbolRegistry.mystcraftId("horizon_low"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(2.0f);
            setPoem("Horizon", "Form", "Constraint", "Depth");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setHorizonHeight(-32.0f);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class HorizonHigh extends SymbolBase {
        public HorizonHigh() {
            super(SymbolRegistry.mystcraftId("horizon_high"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(2.0f);
            setPoem("Horizon", "Form", "Ascend", "Flow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setHorizonHeight(64.0f);
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Gradient Builder ---

    /**
     * Pops a color from the color stack and pushes it onto the gradient stack.
     */
    public static class GradientBuilder extends SymbolBase {
        public GradientBuilder() {
            super(SymbolRegistry.mystcraftId("gradient_builder"), SymbolCategory.MODIFIER);
            setCardRank(1);
            setInstabilityCost(0.0f);
            setPoem("Image", "Gradient", "Color", "Form");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int color = director.popColor();
            if (color != -1) {
                director.pushGradient(color);
            }
        }
    }

    /**
     * Pops all colors from the gradient stack, blends them, and applies to sunset color.
     */
    public static class GradientApplySunset extends SymbolBase {
        public GradientApplySunset() {
            super(SymbolRegistry.mystcraftId("gradient_apply_sunset"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Image", "Gradient", "Sun", "Merge");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            int[] colors = new int[16];
            int count = 0;

            int color = director.popGradient();
            while (color != -1 && count < colors.length) {
                colors[count++] = color;
                color = director.popGradient();
            }

            if (count > 0) {
                int blended = ColorUtils.blendGradientColors(colors, count);
                director.setSunsetColor(blended);
            }
        }
    }
}
