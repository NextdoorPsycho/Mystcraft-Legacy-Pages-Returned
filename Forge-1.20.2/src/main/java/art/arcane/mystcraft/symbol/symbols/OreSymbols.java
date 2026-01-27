package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.populate.SingleOrePopulator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ore control symbols for enabling/disabling and boosting ore generation.
 */
public final class OreSymbols {

    private OreSymbols() {}

    public static void register() {
        // Disable symbols
        SymbolRegistry.register(new NoOres());
        SymbolRegistry.register(new NoCoalOre());
        SymbolRegistry.register(new NoIronOre());
        SymbolRegistry.register(new NoCopperOre());
        SymbolRegistry.register(new NoGoldOre());
        SymbolRegistry.register(new NoRedstoneOre());
        SymbolRegistry.register(new NoDiamondOre());
        SymbolRegistry.register(new NoLapisOre());
        SymbolRegistry.register(new NoEmeraldOre());

        // Boost symbols
        SymbolRegistry.register(new ExtraCoalOre());
        SymbolRegistry.register(new ExtraIronOre());
        SymbolRegistry.register(new ExtraCopperOre());
        SymbolRegistry.register(new ExtraGoldOre());
        SymbolRegistry.register(new ExtraRedstoneOre());
        SymbolRegistry.register(new ExtraDiamondOre());
        SymbolRegistry.register(new ExtraLapisOre());
        SymbolRegistry.register(new ExtraEmeraldOre());

        Mystcraft.LOGGER.info("Registered ore symbols");
    }

    // --- Disable Symbols ---

    public static class NoOres extends SymbolBase {
        public NoOres() {
            super(SymbolRegistry.mystcraftId("no_ores"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(15.0f);
            setPoem("Modifier", "Constraint", "Earth", "Barren");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOresDisabled(true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoCoalOre extends SymbolBase {
        public NoCoalOre() {
            super(SymbolRegistry.mystcraftId("no_coal_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Modifier", "Constraint", "Earth", "Dark");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("coal", true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoIronOre extends SymbolBase {
        public NoIronOre() {
            super(SymbolRegistry.mystcraftId("no_iron_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Earth", "Rust");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("iron", true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoCopperOre extends SymbolBase {
        public NoCopperOre() {
            super(SymbolRegistry.mystcraftId("no_copper_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Modifier", "Constraint", "Earth", "Green");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("copper", true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoGoldOre extends SymbolBase {
        public NoGoldOre() {
            super(SymbolRegistry.mystcraftId("no_gold_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Earth", "Gleam");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("gold", true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoRedstoneOre extends SymbolBase {
        public NoRedstoneOre() {
            super(SymbolRegistry.mystcraftId("no_redstone_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Earth", "Spark");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("redstone", true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoDiamondOre extends SymbolBase {
        public NoDiamondOre() {
            super(SymbolRegistry.mystcraftId("no_diamond_ore"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Modifier", "Constraint", "Earth", "Crystal");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("diamond", true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoLapisOre extends SymbolBase {
        public NoLapisOre() {
            super(SymbolRegistry.mystcraftId("no_lapis_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Earth", "Blue");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("lapis", true);
            director.addInstability(getInstabilityCost());
        }
    }

    public static class NoEmeraldOre extends SymbolBase {
        public NoEmeraldOre() {
            super(SymbolRegistry.mystcraftId("no_emerald_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Constraint", "Earth", "Emerald");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOreDisabled("emerald", true);
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Boost Symbols ---

    public static class ExtraCoalOre extends SymbolBase {
        public ExtraCoalOre() {
            super(SymbolRegistry.mystcraftId("extra_coal_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Modifier", "Abundance", "Earth", "Dark");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.COAL_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(),
                    17, 20, -64, 192,
                    "mystcraft:extra_coal_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class ExtraIronOre extends SymbolBase {
        public ExtraIronOre() {
            super(SymbolRegistry.mystcraftId("extra_iron_ore"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Abundance", "Earth", "Rust");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.IRON_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
                    9, 20, -64, 72,
                    "mystcraft:extra_iron_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class ExtraCopperOre extends SymbolBase {
        public ExtraCopperOre() {
            super(SymbolRegistry.mystcraftId("extra_copper_ore"), SymbolCategory.MODIFIER);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Modifier", "Abundance", "Earth", "Green");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.COPPER_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState(),
                    10, 16, -16, 112,
                    "mystcraft:extra_copper_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class ExtraGoldOre extends SymbolBase {
        public ExtraGoldOre() {
            super(SymbolRegistry.mystcraftId("extra_gold_ore"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Modifier", "Abundance", "Earth", "Gleam");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.GOLD_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(),
                    9, 4, -64, 32,
                    "mystcraft:extra_gold_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class ExtraRedstoneOre extends SymbolBase {
        public ExtraRedstoneOre() {
            super(SymbolRegistry.mystcraftId("extra_redstone_ore"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Abundance", "Earth", "Spark");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.REDSTONE_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(),
                    8, 8, -64, 16,
                    "mystcraft:extra_redstone_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class ExtraDiamondOre extends SymbolBase {
        public ExtraDiamondOre() {
            super(SymbolRegistry.mystcraftId("extra_diamond_ore"), SymbolCategory.MODIFIER);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Modifier", "Abundance", "Earth", "Crystal");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.DIAMOND_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(),
                    8, 2, -64, 16,
                    "mystcraft:extra_diamond_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class ExtraLapisOre extends SymbolBase {
        public ExtraLapisOre() {
            super(SymbolRegistry.mystcraftId("extra_lapis_ore"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Modifier", "Abundance", "Earth", "Blue");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.LAPIS_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState(),
                    7, 2, -64, 64,
                    "mystcraft:extra_lapis_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class ExtraEmeraldOre extends SymbolBase {
        public ExtraEmeraldOre() {
            super(SymbolRegistry.mystcraftId("extra_emerald_ore"), SymbolCategory.MODIFIER);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Modifier", "Abundance", "Earth", "Emerald");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.registerInterface(new SingleOrePopulator(
                    Blocks.EMERALD_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState(),
                    3, 6, -16, 64,
                    "mystcraft:extra_emerald_ore"
            ));
            director.addInstability(getInstabilityCost());
        }
    }
}
