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
}
