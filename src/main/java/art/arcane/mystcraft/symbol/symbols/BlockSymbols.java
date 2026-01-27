package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Block modifier symbols: curated terrain block groups.
 * Sea/fluid symbols are handled by FluidSymbols.
 */
public final class BlockSymbols {

    private BlockSymbols() {}

    public static void register() {
        registerTerrainBlocks();
    }

    // --- Terrain Blocks ---

    private static void registerTerrainBlocks() {
        int count = 0;

        // Logs
        count += terrain("oak_log", Blocks.OAK_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("spruce_log", Blocks.SPRUCE_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("birch_log", Blocks.BIRCH_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("jungle_log", Blocks.JUNGLE_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("acacia_log", Blocks.ACACIA_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("dark_oak_log", Blocks.DARK_OAK_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("mangrove_log", Blocks.MANGROVE_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("cherry_log", Blocks.CHERRY_LOG, 2, 3.0f, "Earth", "Growth");
        count += terrain("crimson_stem", Blocks.CRIMSON_STEM, 3, 5.0f, "Fire", "Growth");
        count += terrain("warped_stem", Blocks.WARPED_STEM, 3, 5.0f, "Fire", "Growth");
        count += terrain("bamboo_block", Blocks.BAMBOO_BLOCK, 2, 3.0f, "Earth", "Growth");

        // Planks
        count += terrain("oak_planks", Blocks.OAK_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("spruce_planks", Blocks.SPRUCE_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("birch_planks", Blocks.BIRCH_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("jungle_planks", Blocks.JUNGLE_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("acacia_planks", Blocks.ACACIA_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("dark_oak_planks", Blocks.DARK_OAK_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("mangrove_planks", Blocks.MANGROVE_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("cherry_planks", Blocks.CHERRY_PLANKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("crimson_planks", Blocks.CRIMSON_PLANKS, 3, 5.0f, "Fire", "Crafted");
        count += terrain("warped_planks", Blocks.WARPED_PLANKS, 3, 5.0f, "Fire", "Crafted");
        count += terrain("bamboo_planks", Blocks.BAMBOO_PLANKS, 2, 3.0f, "Earth", "Crafted");

        // Stones
        count += terrain("stone", Blocks.STONE, 1, 0.0f, "Earth", "Solid");
        count += terrain("granite", Blocks.GRANITE, 2, 2.0f, "Earth", "Solid");
        count += terrain("diorite", Blocks.DIORITE, 2, 2.0f, "Earth", "Solid");
        count += terrain("andesite", Blocks.ANDESITE, 2, 2.0f, "Earth", "Solid");
        count += terrain("deepslate", Blocks.DEEPSLATE, 2, 2.0f, "Earth", "Solid");
        count += terrain("tuff", Blocks.TUFF, 2, 2.0f, "Earth", "Solid");
        count += terrain("calcite", Blocks.CALCITE, 2, 2.0f, "Earth", "Solid");
        count += terrain("dripstone_block", Blocks.DRIPSTONE_BLOCK, 2, 3.0f, "Earth", "Solid");
        count += terrain("cobblestone", Blocks.COBBLESTONE, 1, 1.0f, "Earth", "Solid");
        count += terrain("mossy_cobblestone", Blocks.MOSSY_COBBLESTONE, 2, 2.0f, "Earth", "Solid");
        count += terrain("smooth_stone", Blocks.SMOOTH_STONE, 2, 2.0f, "Earth", "Crafted");

        // Stone Bricks
        count += terrain("stone_bricks", Blocks.STONE_BRICKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("mossy_stone_bricks", Blocks.MOSSY_STONE_BRICKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("cracked_stone_bricks", Blocks.CRACKED_STONE_BRICKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("chiseled_stone_bricks", Blocks.CHISELED_STONE_BRICKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("cobbled_deepslate", Blocks.COBBLED_DEEPSLATE, 2, 2.0f, "Earth", "Solid");
        count += terrain("polished_deepslate", Blocks.POLISHED_DEEPSLATE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("deepslate_bricks", Blocks.DEEPSLATE_BRICKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("deepslate_tiles", Blocks.DEEPSLATE_TILES, 2, 3.0f, "Earth", "Crafted");
        count += terrain("polished_granite", Blocks.POLISHED_GRANITE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("polished_diorite", Blocks.POLISHED_DIORITE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("polished_andesite", Blocks.POLISHED_ANDESITE, 2, 3.0f, "Earth", "Crafted");

        // Sandstones
        count += terrain("sandstone", Blocks.SANDSTONE, 2, 3.0f, "Earth", "Solid");
        count += terrain("red_sandstone", Blocks.RED_SANDSTONE, 2, 3.0f, "Earth", "Solid");
        count += terrain("smooth_sandstone", Blocks.SMOOTH_SANDSTONE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("smooth_red_sandstone", Blocks.SMOOTH_RED_SANDSTONE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("cut_sandstone", Blocks.CUT_SANDSTONE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("cut_red_sandstone", Blocks.CUT_RED_SANDSTONE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("chiseled_sandstone", Blocks.CHISELED_SANDSTONE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("chiseled_red_sandstone", Blocks.CHISELED_RED_SANDSTONE, 2, 3.0f, "Earth", "Crafted");

        // Ore Blocks (Raw)
        count += terrain("coal_ore", Blocks.COAL_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("iron_ore", Blocks.IRON_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("copper_ore", Blocks.COPPER_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("gold_ore", Blocks.GOLD_ORE, 3, 12.0f, "Earth", "Vein");
        count += terrain("redstone_ore", Blocks.REDSTONE_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("diamond_ore", Blocks.DIAMOND_ORE, 4, 20.0f, "Earth", "Vein");
        count += terrain("lapis_ore", Blocks.LAPIS_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("emerald_ore", Blocks.EMERALD_ORE, 4, 15.0f, "Earth", "Vein");
        count += terrain("deepslate_coal_ore", Blocks.DEEPSLATE_COAL_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("deepslate_iron_ore", Blocks.DEEPSLATE_IRON_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("deepslate_copper_ore", Blocks.DEEPSLATE_COPPER_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("deepslate_gold_ore", Blocks.DEEPSLATE_GOLD_ORE, 3, 12.0f, "Earth", "Vein");
        count += terrain("deepslate_redstone_ore", Blocks.DEEPSLATE_REDSTONE_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("deepslate_diamond_ore", Blocks.DEEPSLATE_DIAMOND_ORE, 4, 20.0f, "Earth", "Vein");
        count += terrain("deepslate_lapis_ore", Blocks.DEEPSLATE_LAPIS_ORE, 3, 10.0f, "Earth", "Vein");
        count += terrain("deepslate_emerald_ore", Blocks.DEEPSLATE_EMERALD_ORE, 4, 15.0f, "Earth", "Vein");
        count += terrain("nether_gold_ore", Blocks.NETHER_GOLD_ORE, 3, 12.0f, "Fire", "Vein");
        count += terrain("nether_quartz_ore", Blocks.NETHER_QUARTZ_ORE, 3, 8.0f, "Fire", "Vein");

        // Ore Blocks (Smelted / Storage)
        count += terrain("coal_block", Blocks.COAL_BLOCK, 3, 12.0f, "Earth", "Refined");
        count += terrain("iron_block", Blocks.IRON_BLOCK, 3, 15.0f, "Earth", "Refined");
        count += terrain("copper_block", Blocks.COPPER_BLOCK, 3, 12.0f, "Earth", "Refined");
        count += terrain("gold_block", Blocks.GOLD_BLOCK, 4, 20.0f, "Earth", "Refined");
        count += terrain("redstone_block", Blocks.REDSTONE_BLOCK, 3, 15.0f, "Earth", "Refined");
        count += terrain("diamond_block", Blocks.DIAMOND_BLOCK, 4, 25.0f, "Earth", "Refined");
        count += terrain("lapis_block", Blocks.LAPIS_BLOCK, 3, 15.0f, "Earth", "Refined");
        count += terrain("emerald_block", Blocks.EMERALD_BLOCK, 4, 25.0f, "Earth", "Refined");
        count += terrain("netherite_block", Blocks.NETHERITE_BLOCK, 4, 30.0f, "Fire", "Refined");
        count += terrain("amethyst_block", Blocks.AMETHYST_BLOCK, 3, 12.0f, "Earth", "Refined");
        count += terrain("raw_iron_block", Blocks.RAW_IRON_BLOCK, 3, 15.0f, "Earth", "Vein");
        count += terrain("raw_copper_block", Blocks.RAW_COPPER_BLOCK, 3, 12.0f, "Earth", "Vein");
        count += terrain("raw_gold_block", Blocks.RAW_GOLD_BLOCK, 4, 20.0f, "Earth", "Vein");

        // Nether Blocks
        count += terrain("netherrack", Blocks.NETHERRACK, 2, 5.0f, "Fire", "Solid");
        count += terrain("nether_bricks", Blocks.NETHER_BRICKS, 3, 8.0f, "Fire", "Crafted");
        count += terrain("red_nether_bricks", Blocks.RED_NETHER_BRICKS, 3, 8.0f, "Fire", "Crafted");
        count += terrain("soul_sand", Blocks.SOUL_SAND, 3, 8.0f, "Fire", "Loose");
        count += terrain("soul_soil", Blocks.SOUL_SOIL, 3, 8.0f, "Fire", "Loose");
        count += terrain("basalt", Blocks.BASALT, 3, 8.0f, "Fire", "Solid");
        count += terrain("smooth_basalt", Blocks.SMOOTH_BASALT, 3, 8.0f, "Fire", "Crafted");
        count += terrain("polished_basalt", Blocks.POLISHED_BASALT, 3, 8.0f, "Fire", "Crafted");
        count += terrain("blackstone", Blocks.BLACKSTONE, 3, 8.0f, "Fire", "Solid");
        count += terrain("polished_blackstone", Blocks.POLISHED_BLACKSTONE, 3, 8.0f, "Fire", "Crafted");
        count += terrain("polished_blackstone_bricks", Blocks.POLISHED_BLACKSTONE_BRICKS, 3, 8.0f, "Fire", "Crafted");
        count += terrain("chiseled_polished_blackstone", Blocks.CHISELED_POLISHED_BLACKSTONE, 3, 8.0f, "Fire", "Crafted");
        count += terrain("magma_block", Blocks.MAGMA_BLOCK, 3, 10.0f, "Fire", "Solid");
        count += terrain("glowstone", Blocks.GLOWSTONE, 3, 10.0f, "Fire", "Solid");
        count += terrain("crying_obsidian", Blocks.CRYING_OBSIDIAN, 4, 15.0f, "Void", "Solid");
        count += terrain("ancient_debris", Blocks.ANCIENT_DEBRIS, 4, 25.0f, "Fire", "Vein");
        count += terrain("crimson_nylium", Blocks.CRIMSON_NYLIUM, 3, 8.0f, "Fire", "Growth");
        count += terrain("warped_nylium", Blocks.WARPED_NYLIUM, 3, 8.0f, "Fire", "Growth");
        count += terrain("shroomlight", Blocks.SHROOMLIGHT, 3, 8.0f, "Fire", "Growth");

        // End Blocks
        count += terrain("end_stone", Blocks.END_STONE, 3, 8.0f, "Void", "Solid");
        count += terrain("end_stone_bricks", Blocks.END_STONE_BRICKS, 3, 8.0f, "Void", "Crafted");
        count += terrain("purpur_block", Blocks.PURPUR_BLOCK, 3, 8.0f, "Void", "Crafted");
        count += terrain("purpur_pillar", Blocks.PURPUR_PILLAR, 3, 8.0f, "Void", "Crafted");

        // Earth & Soil
        count += terrain("dirt", Blocks.DIRT, 1, 3.0f, "Earth", "Loose");
        count += terrain("coarse_dirt", Blocks.COARSE_DIRT, 1, 3.0f, "Earth", "Loose");
        count += terrain("rooted_dirt", Blocks.ROOTED_DIRT, 2, 3.0f, "Earth", "Growth");
        count += terrain("mud", Blocks.MUD, 2, 3.0f, "Earth", "Loose");
        count += terrain("packed_mud", Blocks.PACKED_MUD, 2, 3.0f, "Earth", "Crafted");
        count += terrain("mud_bricks", Blocks.MUD_BRICKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("clay", Blocks.CLAY, 2, 3.0f, "Earth", "Loose");
        count += terrain("gravel", Blocks.GRAVEL, 1, 5.0f, "Earth", "Loose");
        count += terrain("sand", Blocks.SAND, 1, 5.0f, "Earth", "Loose");
        count += terrain("red_sand", Blocks.RED_SAND, 2, 5.0f, "Earth", "Loose");
        count += terrain("mycelium", Blocks.MYCELIUM, 2, 5.0f, "Earth", "Growth");
        count += terrain("podzol", Blocks.PODZOL, 2, 3.0f, "Earth", "Growth");
        count += terrain("grass_block", Blocks.GRASS_BLOCK, 1, 2.0f, "Earth", "Growth");
        count += terrain("moss_block", Blocks.MOSS_BLOCK, 2, 3.0f, "Earth", "Growth");

        // Terracotta
        count += terrain("terracotta", Blocks.TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("white_terracotta", Blocks.WHITE_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("orange_terracotta", Blocks.ORANGE_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("magenta_terracotta", Blocks.MAGENTA_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("light_blue_terracotta", Blocks.LIGHT_BLUE_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("yellow_terracotta", Blocks.YELLOW_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("lime_terracotta", Blocks.LIME_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("pink_terracotta", Blocks.PINK_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("gray_terracotta", Blocks.GRAY_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("light_gray_terracotta", Blocks.LIGHT_GRAY_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("cyan_terracotta", Blocks.CYAN_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("purple_terracotta", Blocks.PURPLE_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("blue_terracotta", Blocks.BLUE_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("brown_terracotta", Blocks.BROWN_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("green_terracotta", Blocks.GREEN_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("red_terracotta", Blocks.RED_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");
        count += terrain("black_terracotta", Blocks.BLACK_TERRACOTTA, 2, 3.0f, "Earth", "Crafted");

        // Glazed Terracotta
        count += terrain("white_glazed_terracotta", Blocks.WHITE_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("orange_glazed_terracotta", Blocks.ORANGE_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("magenta_glazed_terracotta", Blocks.MAGENTA_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("light_blue_glazed_terracotta", Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("yellow_glazed_terracotta", Blocks.YELLOW_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("lime_glazed_terracotta", Blocks.LIME_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("pink_glazed_terracotta", Blocks.PINK_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("gray_glazed_terracotta", Blocks.GRAY_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("light_gray_glazed_terracotta", Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("cyan_glazed_terracotta", Blocks.CYAN_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("purple_glazed_terracotta", Blocks.PURPLE_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("blue_glazed_terracotta", Blocks.BLUE_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("brown_glazed_terracotta", Blocks.BROWN_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("green_glazed_terracotta", Blocks.GREEN_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("red_glazed_terracotta", Blocks.RED_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");
        count += terrain("black_glazed_terracotta", Blocks.BLACK_GLAZED_TERRACOTTA, 2, 4.0f, "Earth", "Crafted");

        // Concrete
        count += terrain("white_concrete", Blocks.WHITE_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("orange_concrete", Blocks.ORANGE_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("magenta_concrete", Blocks.MAGENTA_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("light_blue_concrete", Blocks.LIGHT_BLUE_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("yellow_concrete", Blocks.YELLOW_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("lime_concrete", Blocks.LIME_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("pink_concrete", Blocks.PINK_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("gray_concrete", Blocks.GRAY_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("light_gray_concrete", Blocks.LIGHT_GRAY_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("cyan_concrete", Blocks.CYAN_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("purple_concrete", Blocks.PURPLE_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("blue_concrete", Blocks.BLUE_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("brown_concrete", Blocks.BROWN_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("green_concrete", Blocks.GREEN_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("red_concrete", Blocks.RED_CONCRETE, 2, 3.0f, "Earth", "Crafted");
        count += terrain("black_concrete", Blocks.BLACK_CONCRETE, 2, 3.0f, "Earth", "Crafted");

        // Wool
        count += terrain("white_wool", Blocks.WHITE_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("orange_wool", Blocks.ORANGE_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("magenta_wool", Blocks.MAGENTA_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("light_blue_wool", Blocks.LIGHT_BLUE_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("yellow_wool", Blocks.YELLOW_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("lime_wool", Blocks.LIME_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("pink_wool", Blocks.PINK_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("gray_wool", Blocks.GRAY_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("light_gray_wool", Blocks.LIGHT_GRAY_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("cyan_wool", Blocks.CYAN_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("purple_wool", Blocks.PURPLE_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("blue_wool", Blocks.BLUE_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("brown_wool", Blocks.BROWN_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("green_wool", Blocks.GREEN_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("red_wool", Blocks.RED_WOOL, 2, 3.0f, "Earth", "Soft");
        count += terrain("black_wool", Blocks.BLACK_WOOL, 2, 3.0f, "Earth", "Soft");

        // Ice & Snow
        count += terrain("ice", Blocks.ICE, 3, 8.0f, "Ice", "Solid");
        count += terrain("packed_ice", Blocks.PACKED_ICE, 3, 8.0f, "Ice", "Solid");
        count += terrain("blue_ice", Blocks.BLUE_ICE, 3, 10.0f, "Ice", "Solid");
        count += terrain("snow_block", Blocks.SNOW_BLOCK, 2, 5.0f, "Ice", "Loose");

        // Prismarine & Ocean
        count += terrain("prismarine", Blocks.PRISMARINE, 3, 5.0f, "Water", "Solid");
        count += terrain("prismarine_bricks", Blocks.PRISMARINE_BRICKS, 3, 5.0f, "Water", "Crafted");
        count += terrain("dark_prismarine", Blocks.DARK_PRISMARINE, 3, 5.0f, "Water", "Crafted");
        count += terrain("sea_lantern", Blocks.SEA_LANTERN, 3, 8.0f, "Water", "Solid");

        // Quartz
        count += terrain("quartz_block", Blocks.QUARTZ_BLOCK, 3, 5.0f, "Earth", "Crafted");
        count += terrain("smooth_quartz", Blocks.SMOOTH_QUARTZ, 3, 5.0f, "Earth", "Crafted");
        count += terrain("quartz_bricks", Blocks.QUARTZ_BRICKS, 3, 5.0f, "Earth", "Crafted");
        count += terrain("chiseled_quartz_block", Blocks.CHISELED_QUARTZ_BLOCK, 3, 5.0f, "Earth", "Crafted");
        count += terrain("quartz_pillar", Blocks.QUARTZ_PILLAR, 3, 5.0f, "Earth", "Crafted");

        // Glass
        count += terrain("glass", Blocks.GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("white_stained_glass", Blocks.WHITE_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("orange_stained_glass", Blocks.ORANGE_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("magenta_stained_glass", Blocks.MAGENTA_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("light_blue_stained_glass", Blocks.LIGHT_BLUE_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("yellow_stained_glass", Blocks.YELLOW_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("lime_stained_glass", Blocks.LIME_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("pink_stained_glass", Blocks.PINK_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("gray_stained_glass", Blocks.GRAY_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("light_gray_stained_glass", Blocks.LIGHT_GRAY_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("cyan_stained_glass", Blocks.CYAN_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("purple_stained_glass", Blocks.PURPLE_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("blue_stained_glass", Blocks.BLUE_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("brown_stained_glass", Blocks.BROWN_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("green_stained_glass", Blocks.GREEN_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("red_stained_glass", Blocks.RED_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("black_stained_glass", Blocks.BLACK_STAINED_GLASS, 2, 4.0f, "Earth", "Crafted");
        count += terrain("tinted_glass", Blocks.TINTED_GLASS, 3, 5.0f, "Earth", "Crafted");

        // Bricks & Miscellaneous
        count += terrain("bricks", Blocks.BRICKS, 2, 3.0f, "Earth", "Crafted");
        count += terrain("obsidian", Blocks.OBSIDIAN, 3, 10.0f, "Void", "Solid");
        count += terrain("hay_block", Blocks.HAY_BLOCK, 2, 3.0f, "Earth", "Growth");
        count += terrain("bone_block", Blocks.BONE_BLOCK, 2, 5.0f, "Earth", "Solid");
        count += terrain("sponge", Blocks.SPONGE, 3, 10.0f, "Water", "Solid");
        count += terrain("dried_kelp_block", Blocks.DRIED_KELP_BLOCK, 2, 3.0f, "Water", "Growth");
        count += terrain("honeycomb_block", Blocks.HONEYCOMB_BLOCK, 2, 5.0f, "Earth", "Crafted");
        count += terrain("oxidized_copper", Blocks.OXIDIZED_COPPER, 3, 12.0f, "Earth", "Refined");
        count += terrain("weathered_copper", Blocks.WEATHERED_COPPER, 3, 12.0f, "Earth", "Refined");
        count += terrain("exposed_copper", Blocks.EXPOSED_COPPER, 3, 12.0f, "Earth", "Refined");
        count += terrain("waxed_copper_block", Blocks.WAXED_COPPER_BLOCK, 3, 12.0f, "Earth", "Refined");
        count += terrain("cut_copper", Blocks.CUT_COPPER, 3, 12.0f, "Earth", "Crafted");

        Mystcraft.LOGGER.info("Registered {} terrain block symbols", count);
    }

    /**
     * Registers a single terrain block symbol and returns 1 for counting.
     */
    private static int terrain(String name, Block block, int rank, float instability, String element, String quality) {
        SymbolRegistry.register(new TerrainBlockSymbol(
                SymbolRegistry.mystcraftId("block_minecraft_" + name),
                block, rank, instability,
                "Modifier", "Constraint", element, quality
        ));
        return 1;
    }

    // --- Terrain Block Symbol ---

    /**
     * A terrain block symbol that sets the Age's primary terrain material.
     * Display name is derived from the block's own name.
     */
    public static class TerrainBlockSymbol extends SymbolBase {

        private final Block block;

        public TerrainBlockSymbol(ResourceLocation symbolId, Block block, int cardRank, float instability, String... poem) {
            super(symbolId, SymbolCategory.MODIFIER);
            this.block = block;
            setCardRank(cardRank);
            setInstabilityCost(instability);
            setPoem(poem);
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTerrainBlock(block.defaultBlockState());
            director.addInstability(getInstabilityCost());
        }

        @Override
        public boolean allowInRandomGeneration() {
            return false;
        }

        @Override
        public String getLocalizedName() {
            return block.getName().getString() + " Terrain";
        }
    }

}
