package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.datapack.symbol.PopulatorRegistry;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Builds an Age configuration from symbols using the CFG grammar system.
 * This is the main entry point for creating Ages from page collections.
 * <p>
 * Uses the full Context-Free Grammar tree expansion,
 * ensuring diverse and vibrant ages with proper symbol expansion.
 * Post-build fallbacks apply ore variation, timescale shifts,
 * environment effects, and color palettes to fill gaps.
 */
public class AgeBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgeBuilder.class);
  private static final float MISSING_SYMBOL_INSTABILITY = 8.0f;
  private static final Set<String> NETHER_BIOMES = Set.of(
      "biome_nether_wastes", "biome_soul_sand_valley", "biome_crimson_forest",
      "biome_warped_forest", "biome_basalt_deltas"
  );
  private static final Set<String> END_BIOMES = Set.of(
      "biome_the_end", "biome_end_midlands", "biome_end_highlands",
      "biome_end_barrens", "biome_small_end_islands"
  );
  private static final Set<String> NETHER_TERRAINS = Set.of("nether");
  private static final Set<String> END_TERRAINS = Set.of("end");
  private static final Set<String> VOID_TERRAINS = Set.of("void");
  private static final Set<String> NO_SEA_TERRAINS = Set.of(
      "void", "nether", "cave", "skylands", "flat", "end"
  );
  private static final Set<String> OCEAN_BIOME_PATHS = Set.of(
      "biome_ocean", "biome_deep_ocean", "biome_warm_ocean", "biome_lukewarm_ocean",
      "biome_cold_ocean", "biome_frozen_ocean", "biome_deep_lukewarm_ocean",
      "biome_deep_cold_ocean", "biome_deep_frozen_ocean",
      "biome_river", "biome_frozen_river",
      "biome_beach", "biome_snowy_beach", "biome_stony_shore"
  );
  private static final Set<String> SINGLETON_CATEGORIES = Set.of(
      "terrain", "biome_controller", "weather", "lighting", "sea"
  );
  private static final Set<String> VOID_INCOMPATIBLE_FEATURES = Set.of(
      "caves", "ravines", "dripstone_caves", "lush_caves", "deep_dark",
      "surface_lakes", "deep_lakes", "perlin_worms",
      "bonefields", "corrupted_columns", "shattered_grid"
  );
  private static final Set<String> VOID_INCOMPATIBLE_STRUCTURES = Set.of(
      "villages", "dungeons", "mineshafts", "strongholds",
      "pillager_outposts", "ruined_portals", "ocean_monuments",
      "witch_huts", "desert_temples", "jungle_temples",
      "woodland_mansions", "trail_ruins", "ancient_cities",
      "nether_fortress", "bastion_remnants", "end_cities",
      "igloos", "shipwrecks", "ocean_ruins", "buried_treasure",
      "nether_fossils"
  );
  private static final Set<String> FLAT_INCOMPATIBLE_FEATURES = Set.of(
      "caves", "ravines", "dripstone_caves", "lush_caves",
      "deep_dark", "deep_lakes", "perlin_worms"
  );
  // Stack-based modifier categories are inherently repeatable (push operations)
  private static final Set<SymbolCategory> STACKABLE_CATEGORIES = Set.of(
      SymbolCategory.COLOR, SymbolCategory.ANGLE, SymbolCategory.PHASE, SymbolCategory.LENGTH
  );

  // --- Biome/Terrain classification for coherence filtering ---
  private static final float TERRAIN_BLOCK_CHANGE_CHANCE = 0.50f;
  private static final Block[] OVERWORLD_BLOCKS = {
      Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
      Blocks.DEEPSLATE, Blocks.TUFF, Blocks.CALCITE,
      Blocks.SANDSTONE, Blocks.RED_SANDSTONE,
      Blocks.TERRACOTTA, Blocks.PACKED_MUD,
      Blocks.SMOOTH_BASALT, Blocks.DRIPSTONE_BLOCK,
      Blocks.PRISMARINE, Blocks.DARK_PRISMARINE,
      Blocks.MOSS_BLOCK, Blocks.MUD_BRICKS,
      Blocks.RED_TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
      Blocks.YELLOW_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
      Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE,
      Blocks.CYAN_CONCRETE, Blocks.BLUE_CONCRETE,
      Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ,
      Blocks.BONE_BLOCK, Blocks.PACKED_ICE,
      Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE,
      Blocks.POLISHED_GRANITE, Blocks.POLISHED_DIORITE, Blocks.POLISHED_ANDESITE,
      Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
      Blocks.CLAY, Blocks.COPPER_BLOCK, Blocks.OXIDIZED_COPPER,
      Blocks.CUT_COPPER, Blocks.AMETHYST_BLOCK,
      Blocks.SMOOTH_STONE, Blocks.POLISHED_BLACKSTONE_BRICKS,
      Blocks.END_STONE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS,
      Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS, Blocks.BAMBOO_PLANKS,
      Blocks.BLUE_ICE, Blocks.SCULK
  };
  private static final Block[] NETHER_BLOCKS = {
      Blocks.BASALT, Blocks.BLACKSTONE,
      Blocks.SOUL_SAND, Blocks.SOUL_SOIL,
      Blocks.MAGMA_BLOCK, Blocks.CRIMSON_NYLIUM,
      Blocks.WARPED_NYLIUM, Blocks.POLISHED_BLACKSTONE,
      Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS,
      Blocks.GILDED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE,
      Blocks.CRACKED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS,
      Blocks.SMOOTH_BASALT, Blocks.POLISHED_BASALT
  };
  private static final Block[] END_BLOCKS = {
      Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN,
      Blocks.CRYING_OBSIDIAN, Blocks.PURPUR_PILLAR,
      Blocks.END_STONE_BRICKS, Blocks.END_STONE,
      Blocks.AMETHYST_BLOCK, Blocks.QUARTZ_BLOCK
  };
  private static final Block[] CAVE_BLOCKS = {
      Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE,
      Blocks.TUFF, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK,
      Blocks.MOSS_BLOCK, Blocks.MUD_BRICKS, Blocks.SMOOTH_BASALT,
      Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
      Blocks.SCULK, Blocks.PACKED_MUD
  };
  private static final Block[] SKYLANDS_BLOCKS = {
      Blocks.CALCITE, Blocks.DIORITE, Blocks.POLISHED_DIORITE,
      Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ,
      Blocks.PRISMARINE, Blocks.DARK_PRISMARINE,
      Blocks.AMETHYST_BLOCK, Blocks.MOSS_BLOCK,
      Blocks.BONE_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
      Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE,
      Blocks.SANDSTONE
  };
  // Chance that a random/incomplete age gets extra ore generation
  private static final float ORE_BOOST_CHANCE = 0.30f;
  // Chance that a random/incomplete age gets an ore disabled
  private static final float ORE_DISABLE_CHANCE = 0.15f;
  private static final String[] EXTRA_ORE_SYMBOLS = {
      "extra_coal_ore", "extra_iron_ore", "extra_copper_ore",
      "extra_gold_ore", "extra_redstone_ore", "extra_diamond_ore",
      "extra_lapis_ore", "extra_emerald_ore"
  };
  private static final String[] DISABLE_ORE_SYMBOLS = {
      "no_coal_ore", "no_iron_ore", "no_copper_ore",
      "no_gold_ore", "no_redstone_ore", "no_diamond_ore",
      "no_lapis_ore", "no_emerald_ore"
  };
  // Chance that a random age gets a non-default timescale
  private static final float TIMESCALE_CHANGE_CHANCE = 0.30f;
  private static final String[] TIMESCALE_SYMBOLS = {
      "env_longer_days", "env_shorter_days", "env_slow_time", "env_static_time"
  };
  // Weights: longer days is most common, static time is rarest
  private static final int[] TIMESCALE_WEIGHTS = {40, 30, 20, 10};
  // Chance that a random age with high instability gets an environment effect
  private static final float ENV_EFFECT_CHANCE = 0.20f;
  // Higher instability increases the chance of getting an environment effect
  private static final float ENV_EFFECT_INSTABILITY_THRESHOLD = 10.0f;
  private static final String[] ENV_EFFECT_SYMBOLS = {
      "env_accelerated", "env_meteors", "env_lightning",
      "env_scorched", "env_explosions"
  };
  // Weights: accelerated is most common, explosions rarest
  private static final int[] ENV_EFFECT_WEIGHTS = {35, 20, 25, 15, 5};
  // Feature injection: 25% chance to add 1-2 random features
  private static final float FEATURE_INJECTION_CHANCE = 0.25f;
  private static final String[] INJECTABLE_FEATURES = {
      "tendrils", "vertical_tendrils", "spheres", "spikes",
      "obelisks", "crystal_formation", "perlin_worms",
      "bonefields", "meat_pillars", "shattered_grid",
      "eyeblight", "inverted_trees", "corrupted_columns"
  };
  // Biome chaos: 15% chance to inject nether/end biomes into overworld
  private static final float BIOME_CHAOS_CHANCE = 0.15f;
  private static final String[] CHAOS_BIOMES = {
      "biome_nether_wastes", "biome_soul_sand_valley", "biome_crimson_forest",
      "biome_warped_forest", "biome_basalt_deltas",
      "biome_the_end", "biome_end_midlands", "biome_end_highlands"
  };
  // Color palette variety
  private static final float MONOCHROME_CHANCE = 0.20f;
  private static final float INVERTED_PALETTE_CHANCE = 0.10f;
  private static final int[] DEFAULT_SKY_COLORS = {
      0x87CEEB, 0x00BFFF, 0xB0C4DE, 0x6495ED, 0x4169E1,
      0xE0FFFF, 0xFFB6C1, 0xFFA07A, 0x98FB98, 0xDDA0DD,
      0xFFF8DC, 0xF0E68C, 0x191970, 0x483D8B, 0xFFD700,
      0xFF7F50, 0x40E0D0, 0x4B0082, 0xFFBF00, 0x0F52BA,
      0xEE82EE, 0xFFC0CB, 0xFFA500, 0xFFFFF0,
      0xB87333, 0x00A86B, 0x50C878, 0xACE1AF, 0xE34234,
      0x40826D, 0xC8A2C8, 0xFADADD, 0xD2B48C, 0x708090,
  };
  private static final int[] DEFAULT_FOG_COLORS = {
      0xC0C0C0, 0xD3D3D3, 0xB0E0E6, 0xE6E6FA, 0xFFFAF0,
      0xF5F5DC, 0x98FB98, 0xFFE4E1, 0xD2B48C, 0xE0D8C8,
      0xFFE4B5, 0xE8D0A9, 0xC8A2C8, 0xADD8E6, 0xFADADD,
      0xACE1AF, 0xFFE0E0, 0xE0FFE0, 0xD0E8FF, 0xFFF0C0,
      0xD0D0FF, 0xB0FFE0, 0xE0B0FF,
  };
  private static final int[] DEFAULT_CLOUD_COLORS = {
      0xFFFFFF, 0xF0F0F0, 0xE8E0D0, 0xD0E8FF, 0xFFE0E0,
      0xE0FFE0, 0xFFE4B5, 0xC0C0C0, 0x708090, 0xFFDAB9,
      0xE0B0FF, 0xB0FFE0, 0xFFF0C0, 0xD0D0FF,
  };
  private static final int[] DEFAULT_GRASS_COLORS = {
      0x7CFC00, 0x90EE90, 0x32CD32, 0x228B22, 0x006400,
      0x9ACD32, 0x6B8E23, 0x556B2F, 0xADFF2F, 0x98FB98,
      0x8B4513, 0xFF8C00, 0xDAA520, 0xBDB76B, 0x2E8B57,
      0x00FF7F, 0x00A86B, 0x50C878, 0x40826D, 0xACE1AF,
      0xB5651D, 0xCC5500, 0x8FBC8F, 0x3CB371,
  };
  private static final int[] DEFAULT_FOLIAGE_COLORS = {
      0x228B22, 0x006400, 0x2E8B57, 0x3CB371, 0x20B2AA,
      0x008B8B, 0x556B2F, 0x8B4513, 0xFF4500, 0xFFD700,
      0x9370DB, 0xDC143C, 0xFF69B4, 0x00CED1, 0xE34234,
      0x0F52BA, 0xB87333, 0x00A86B, 0x50C878, 0xFFBF00,
      0xFF7F50, 0x191970,
  };
  private static final int[] DEFAULT_WATER_COLORS = {
      0x1E90FF, 0x00CED1, 0x40E0D0, 0x48D1CC, 0x00FFFF,
      0x5F9EA0, 0x4682B4, 0x6495ED, 0x7B68EE, 0x8A2BE2,
      0x20B2AA, 0x008080, 0x2F4F4F, 0x7FFFD4, 0x0F52BA,
      0x4B0082, 0x00A86B, 0xE34234, 0x191970, 0xFFD700,
      0xB87333, 0x50C878,
  };
  private static final int[] DEFAULT_SUNSET_COLORS = {
      0xFF4500, 0xFF6347, 0xFFA500, 0xFFD700, 0xFF69B4,
      0xDC143C, 0xFF7F50, 0xFFBF00, 0xE34234, 0xFF8C00,
      0xDA70D6, 0xEE82EE, 0xB87333, 0xCC5500, 0x40E0D0,
  };
  private static final int[] DEFAULT_NIGHT_SKY_COLORS = {
      0x191970, 0x0B0B45, 0x2C2C54, 0x1B1464, 0x0A0A2E,
      0x2E1A47, 0x1A1A3E, 0x0D0D30, 0x0F1035, 0x1A0A2E,
  };
  private static final int[] DEFAULT_HORIZON_COLORS = {
      0xFFA07A, 0xFFB6C1, 0xE6E6FA, 0xB0C4DE, 0xFFE4B5,
      0xD2B48C, 0xF0E68C, 0xDDA0DD, 0xFFDAB9, 0xACE1AF,
      0x87CEEB, 0xFFD700, 0x40E0D0, 0xFFC0CB,
  };
  private final List<IAgeSymbol> inputSymbols;
  private final long seed;
  private final List<ResourceLocation> missingSymbols = new ArrayList<>();
  private final int providedCount;
  private List<IAgeSymbol> expandedSymbols;
  private AgeDirectorImpl director;
  private float instability;
  private int generatedCount;

  // ===================================================================
  // --- Terrain Block Bias ---
  // ===================================================================
  private int missingSymbolCount;

  /**
   * Creates an AgeBuilder with the given symbols and seed.
   */
  public AgeBuilder(List<IAgeSymbol> symbols, long seed) {
    this.inputSymbols = new ArrayList<>(symbols);
    this.seed = seed;
    this.providedCount = symbols.size();

    // Perform CFG expansion, then enforce coherence
    expandSymbols();
    postProcessSymbols();
  }

  /**
   * Creates a builder for a completely random Age.
   */
  public static AgeBuilder random(long seed) {
    return new AgeBuilder(List.of(), seed);
  }

  private static boolean isFeatureCategory(SymbolCategory category) {
    return category == SymbolCategory.FEATURE_LARGE
        || category == SymbolCategory.FEATURE_MEDIUM
        || category == SymbolCategory.FEATURE_SMALL;
  }

  private static Block[] getTerrainBlockPool(String terrainType) {
    if (terrainType == null) {
      return OVERWORLD_BLOCKS;
    }

    switch (terrainType) {
      case "nether":
        return NETHER_BLOCKS;
      case "end":
        return END_BLOCKS;
      case "cave":
        return CAVE_BLOCKS;
      case "skylands":
        return SKYLANDS_BLOCKS;
      case "void":
        return new Block[0];
      default:
        return OVERWORLD_BLOCKS;
    }
  }

  private static String colorHex(int color) {
    return color == -1 ? "default" : String.format("#%06X", color & 0xFFFFFF);
  }

  /**
   * Performs CFG-based symbol expansion.
   * Uses the grammar tree to expand provided symbols into a complete age specification.
   */
  private void expandSymbols() {
    Random rand = new Random(seed);

    List<ResourceLocation> terminals = new ArrayList<>();
    for (IAgeSymbol symbol : inputSymbols) {
      terminals.add(symbol.getRegistryName());
    }

    LOGGER.debug("Expanding Age with {} input symbols using CFG grammar", terminals.size());

    CFGGrammarTree tree = new CFGGrammarTree(GrammarRules.ROOT);
    tree.parseTerminals(terminals, rand);

    List<ResourceLocation> expandedNames = tree.getExpanded(rand);

    LOGGER.debug("CFG expansion produced {} symbols from {} inputs",
        expandedNames.size(), terminals.size());

    expandedSymbols = new ArrayList<>();
    missingSymbols.clear();
    missingSymbolCount = 0;

    for (ResourceLocation name : expandedNames) {
      IAgeSymbol symbol = SymbolRegistry.get(name);
      if (symbol != null) {
        expandedSymbols.add(symbol);
      } else {
        missingSymbols.add(name);
        missingSymbolCount++;
        LOGGER.warn("Missing symbol in Age generation: {} - this will add instability", name);
      }
    }

    if (missingSymbolCount > 0) {
      LOGGER.warn("Age generation has {} missing symbols - instability penalty: {}",
          missingSymbolCount, missingSymbolCount * MISSING_SYMBOL_INSTABILITY);
    }

    generatedCount = expandedSymbols.size() - providedCount;
    instability = calculateInstability();

    LOGGER.debug("Age generation complete: {} provided, {} generated, {} total, instability: {}",
        providedCount, generatedCount, expandedSymbols.size(), instability);

    StringBuilder symbolList = new StringBuilder();
    for (int i = 0; i < expandedSymbols.size(); i++) {
      IAgeSymbol sym = expandedSymbols.get(i);
      if (i > 0) symbolList.append(", ");
      symbolList.append(sym.getRegistryName().getPath());
      symbolList.append(" [").append(sym.getCategory().name()).append("]");
    }
    LOGGER.debug("[AgeBuilder] Expanded symbols: {}", symbolList);
  }

  /**
   * Enforces coherence on the expanded symbol list.
   */
  private void postProcessSymbols() {
    int beforeSize = expandedSymbols.size();

    removeDuplicates();
    enforceSingletons();

    filterUnrequestedVoidTerrain();

    String terrainType = findTerrainType();

    filterRandomOceanBiomes();
    filterBiomesByTerrain(terrainType);
    filterFeaturesByTerrain(terrainType);

    if (VOID_TERRAINS.contains(terrainType)) {
      filterStructuresFromVoid();
    }

    if (NO_SEA_TERRAINS.contains(terrainType)) {
      removeSeaModifiers();
    }

    int removed = beforeSize - expandedSymbols.size();
    if (removed > 0) {
      LOGGER.debug("[AgeBuilder] Post-processing removed {} incoherent symbols ({} -> {})",
          removed, beforeSize, expandedSymbols.size());

      generatedCount = Math.max(0, expandedSymbols.size() - providedCount);
      instability = calculateInstability();
    }
  }

  // ===================================================================
  // --- Ore Variation ---
  // ===================================================================

  /**
   * Removes symbols with duplicate registry IDs, keeping the first occurrence.
   * Symbols that return canDuplicate() == true are exempt from deduplication.
   * Stack-based modifier categories (COLOR, ANGLE, PHASE, LENGTH) are always
   * exempt since they push to a stack and repeats are intentional.
   */
  private void removeDuplicates() {
    Set<ResourceLocation> seen = new HashSet<>();
    Iterator<IAgeSymbol> iterator = expandedSymbols.iterator();
    while (iterator.hasNext()) {
      IAgeSymbol symbol = iterator.next();
      if (symbol.canDuplicate() || STACKABLE_CATEGORIES.contains(symbol.getCategory())) {
        continue;
      }
      if (!seen.add(symbol.getRegistryName())) {
        LOGGER.debug("[AgeBuilder] Removing duplicate symbol: {}", symbol.getRegistryName());
        iterator.remove();
      }
    }
  }

  /**
   * For singleton categories, keeps only the first symbol and removes subsequent duplicates.
   */
  private void enforceSingletons() {
    Set<String> seenCategories = new HashSet<>();
    Iterator<IAgeSymbol> iterator = expandedSymbols.iterator();
    while (iterator.hasNext()) {
      IAgeSymbol symbol = iterator.next();
      String categoryName = symbol.getCategory().getName();
      if (SINGLETON_CATEGORIES.contains(categoryName)) {
        if (!seenCategories.add(categoryName)) {
          LOGGER.debug("[AgeBuilder] Removing extra {} symbol: {}",
              categoryName, symbol.getRegistryName());
          iterator.remove();
        }
      }
    }
  }

  /**
   * Removes ocean/river/beach biomes that the user did not explicitly include.
   * Preserves at least one biome to avoid leaving the age with no biomes.
   */
  private void filterRandomOceanBiomes() {
    // 50% chance to skip ocean filtering entirely - allows wilder ages
    Random filterRand = new Random(seed ^ 0x0CEA4L);
    if (filterRand.nextFloat() < 0.5f) {
      LOGGER.debug("[AgeBuilder] Skipping ocean biome filter (chaos roll)");
      return;
    }

    // Build set of user-provided biome paths
    Set<String> userBiomePaths = new HashSet<>();
    for (IAgeSymbol symbol : inputSymbols) {
      if (symbol.getCategory() == SymbolCategory.BIOME) {
        userBiomePaths.add(symbol.getRegistryName().getPath());
      }
    }

    // Count non-ocean biomes to make sure we don't strip everything
    long nonOceanBiomeCount = expandedSymbols.stream()
        .filter(s -> s.getCategory() == SymbolCategory.BIOME)
        .filter(s -> !OCEAN_BIOME_PATHS.contains(s.getRegistryName().getPath()))
        .count();

    if (nonOceanBiomeCount == 0) {
      return;
    }

    Iterator<IAgeSymbol> iterator = expandedSymbols.iterator();
    while (iterator.hasNext()) {
      IAgeSymbol symbol = iterator.next();
      if (symbol.getCategory() != SymbolCategory.BIOME) {
        continue;
      }
      String path = symbol.getRegistryName().getPath();
      if (OCEAN_BIOME_PATHS.contains(path) && !userBiomePaths.contains(path)) {
        LOGGER.debug("[AgeBuilder] Filtering randomly-generated ocean biome: {}", path);
        iterator.remove();
      }
    }
  }

  /**
   * Finds the terrain type string from the first TERRAIN symbol.
   */
  private String findTerrainType() {
    for (IAgeSymbol symbol : expandedSymbols) {
      if (symbol.getCategory() == SymbolCategory.TERRAIN) {
        String path = symbol.getRegistryName().getPath();
        if (path.startsWith("terrain_")) {
          return path.substring("terrain_".length());
        }
      }
    }
    return "normal";
  }

  /**
   * Ensures at least one biome is present for the terrain type.
   */
  private void filterBiomesByTerrain(String terrainType) {
    if (hasBiome()) {
      return;
    }

    boolean isNether = NETHER_TERRAINS.contains(terrainType);
    boolean isEnd = END_TERRAINS.contains(terrainType);

    ResourceLocation defaultBiome;
    if (isNether) {
      defaultBiome = new ResourceLocation("mystcraft", "biome_nether_wastes");
    } else if (isEnd) {
      defaultBiome = new ResourceLocation("mystcraft", "biome_the_end");
    } else {
      defaultBiome = new ResourceLocation("mystcraft", "biome_plains");
    }

    IAgeSymbol fallback = SymbolRegistry.get(defaultBiome);
    if (fallback != null) {
      expandedSymbols.add(fallback);
      LOGGER.debug("[AgeBuilder] Added fallback biome for {} terrain: {}", terrainType, defaultBiome);
    }
  }

  private void filterUnrequestedVoidTerrain() {
    boolean hasExplicitVoid = false;
    for (IAgeSymbol symbol : inputSymbols) {
      if (symbol.getCategory() == SymbolCategory.TERRAIN
          && "terrain_void".equals(symbol.getRegistryName().getPath())) {
        hasExplicitVoid = true;
        break;
      }
    }
    if (hasExplicitVoid) {
      return;
    }
    boolean removed = false;
    Iterator<IAgeSymbol> iterator = expandedSymbols.iterator();
    while (iterator.hasNext()) {
      IAgeSymbol symbol = iterator.next();
      if (symbol.getCategory() == SymbolCategory.TERRAIN
          && "terrain_void".equals(symbol.getRegistryName().getPath())) {
        iterator.remove();
        removed = true;
        LOGGER.debug("[AgeBuilder] Removed randomly-generated void terrain");
      }
    }
    if (removed && !hasTerrainSymbol()) {
      List<IAgeSymbol> terrains = SymbolRegistry.getByCategory(SymbolCategory.TERRAIN);
      List<IAgeSymbol> candidates = new ArrayList<>();
      for (IAgeSymbol t : terrains) {
        if (!"terrain_void".equals(t.getRegistryName().getPath())) {
          candidates.add(t);
        }
      }
      IAgeSymbol fallback;
      if (!candidates.isEmpty()) {
        fallback = candidates.get(new Random(seed ^ 0xF17C01DL).nextInt(candidates.size()));
      } else {
        fallback = SymbolRegistry.get(new ResourceLocation("mystcraft", "terrain_normal"));
      }
      if (fallback != null) {
        expandedSymbols.add(fallback);
        LOGGER.debug("[AgeBuilder] Added random fallback terrain {} after removing void terrain", fallback.getRegistryName());
      }
    }
  }

  private boolean hasTerrainSymbol() {
    for (IAgeSymbol symbol : expandedSymbols) {
      if (symbol.getCategory() == SymbolCategory.TERRAIN) {
        return true;
      }
    }
    return false;
  }

  // ===================================================================
  // --- Timescale Variation ---
  // ===================================================================

  private boolean hasBiome() {
    for (IAgeSymbol symbol : expandedSymbols) {
      if (symbol.getCategory() == SymbolCategory.BIOME) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes features incompatible with the terrain type.
   */
  private void filterFeaturesByTerrain(String terrainType) {
    Set<String> incompatible;
    if (VOID_TERRAINS.contains(terrainType)) {
      incompatible = VOID_INCOMPATIBLE_FEATURES;
    } else if ("flat".equals(terrainType)) {
      incompatible = FLAT_INCOMPATIBLE_FEATURES;
    } else {
      return;
    }

    Iterator<IAgeSymbol> iterator = expandedSymbols.iterator();
    while (iterator.hasNext()) {
      IAgeSymbol symbol = iterator.next();
      if (isFeatureCategory(symbol.getCategory())) {
        String path = symbol.getRegistryName().getPath();
        if (incompatible.contains(path)) {
          LOGGER.debug("[AgeBuilder] Removing incompatible feature '{}' for {} terrain", path, terrainType);
          iterator.remove();
        }
      }
    }
  }

  /**
   * Removes all structure symbols from void terrain.
   */
  private void filterStructuresFromVoid() {
    Iterator<IAgeSymbol> iterator = expandedSymbols.iterator();
    while (iterator.hasNext()) {
      IAgeSymbol symbol = iterator.next();
      if (symbol.getCategory() == SymbolCategory.STRUCTURE) {
        String path = symbol.getRegistryName().getPath();
        if (VOID_INCOMPATIBLE_STRUCTURES.contains(path)) {
          LOGGER.debug("[AgeBuilder] Removing structure '{}' from void terrain", path);
          iterator.remove();
        }
      }
    }
  }

  /**
   * Removes sea modifier symbols when the terrain has no sea.
   */
  private void removeSeaModifiers() {
    Iterator<IAgeSymbol> iterator = expandedSymbols.iterator();
    while (iterator.hasNext()) {
      IAgeSymbol symbol = iterator.next();
      if (symbol.getCategory() == SymbolCategory.SEA) {
        LOGGER.debug("[AgeBuilder] Removing sea symbol '{}' from no-sea terrain",
            symbol.getRegistryName());
        iterator.remove();
      } else if (symbol.getRegistryName().getPath().equals("deep_lakes")
          && isFeatureCategory(symbol.getCategory())) {
        LOGGER.debug("[AgeBuilder] Removing deep_lakes from no-sea terrain");
        iterator.remove();
      }
    }
  }

  // ===================================================================
  // --- Environment Effect Fallback ---
  // ===================================================================

  /**
   * Calculates instability based on symbols present.
   */
  private float calculateInstability() {
    float total = 0.0f;

    // Generated symbols add base instability
    total += generatedCount * 2.5f;

    // Missing symbols add instability penalty
    total += missingSymbolCount * MISSING_SYMBOL_INSTABILITY;

    // Each symbol contributes its own instability cost
    for (IAgeSymbol symbol : expandedSymbols) {
      total += symbol.getInstabilityCost();
    }

    // Bonus for well-written Ages (low generated count)
    if (generatedCount == 0 && providedCount >= 5 && missingSymbolCount == 0) {
      total *= 0.8f;
    }

    // Global instability reduction
    total *= 0.5f;

    return Math.max(0.0f, total);
  }

  /**
   * Builds the Age director with all symbol logic applied.
   * After symbol application, fallback systems fill in remaining gaps:
   * terrain block bias, ore variation, timescale variation,
   * environment effects, and default colors.
   */
  public AgeDirectorImpl build() {
    if (director != null) {
      return director;
    }

    director = new AgeDirectorImpl(seed);

    LOGGER.debug("Building Age with {} symbols (instability: {})",
        expandedSymbols.size(), instability);

    // Apply each symbol's logic to the director
    Random symbolRand = new Random(seed);
    for (int i = 0; i < expandedSymbols.size(); i++) {
      IAgeSymbol symbol = expandedSymbols.get(i);
      try {
        LOGGER.debug("[AgeBuilder] [{}/{}] Applying symbol: {} (category: {}, instability: {})",
            i + 1, expandedSymbols.size(), symbol.getRegistryName(),
            symbol.getCategory().name(), symbol.getInstabilityCost());
        symbol.registerLogic(director, symbolRand.nextLong());
      } catch (Exception e) {
        LOGGER.error("[AgeBuilder] [{}/{}] FAILED to apply symbol {}: {}",
            i + 1, expandedSymbols.size(), symbol.getRegistryName(), e.getMessage(), e);
      }
    }

    applyStarFissureDefault(director, symbolRand);

    // --- Fallback pipeline (gated by completeness) ---
    float completenessRatio = expandedSymbols.isEmpty() ? 0.0f
        : 1.0f - (generatedCount / (float) expandedSymbols.size());
    LOGGER.debug("[AgeBuilder] Completeness ratio: {} ({} provided / {} total)",
        String.format("%.2f", completenessRatio), providedCount, expandedSymbols.size());

    if (completenessRatio < 0.8f) {
      // Halve chances when completeness is 0.5-0.8, full rates below 0.5
      float fallbackScale = completenessRatio >= 0.5f ? 0.5f : 1.0f;
      applyTerrainBlockBias(director, symbolRand, fallbackScale);
      applyOreVariation(director, symbolRand, fallbackScale);
      applyTimescaleVariation(director, symbolRand, fallbackScale);
      applyEnvironmentEffects(director, symbolRand, fallbackScale);
      applyFeatureInjection(director, symbolRand, fallbackScale);
      applyBiomeChaos(director, symbolRand, fallbackScale);
      applyDefaultColors(director, symbolRand);
    } else {
      LOGGER.debug("[AgeBuilder] Skipping chaos fallbacks (completeness >= 0.8)");
      applyDefaultColors(director, symbolRand);
    }

    // Log final director state
    LOGGER.debug("[AgeBuilder] Director state after all symbols applied:");
    LOGGER.debug("[AgeBuilder]   Terrain: type={}, groundLevel={}, seaLevel={}, hasSea={}",
        director.getTerrainType(), director.getAverageGroundLevel(),
        director.getSeaLevel(), director.hasSea());
    LOGGER.debug("[AgeBuilder]   Blocks: terrain={}, sea={}",
        director.getTerrainBlock(), director.getSeaBlock());
    LOGGER.debug("[AgeBuilder]   Weather: {}, Lighting: {}",
        director.getWeatherType(), director.getLightingType());
    LOGGER.debug("[AgeBuilder]   Timescale: {}", director.getTimescale());
    LOGGER.debug("[AgeBuilder]   Colors: sky={}, fog={}, grass={}, foliage={}, water={}, cloud={}",
        colorHex(director.getSkyColor()), colorHex(director.getFogColor()),
        director.getGrassColors().isEmpty() ? "default" : director.getGrassColors().toString(), colorHex(director.getFoliageColor()),
        colorHex(director.getWaterColor()), colorHex(director.getCloudColor()));
    LOGGER.debug("[AgeBuilder]   Biomes: {} registered, controller={}",
        director.getBiomes().size(), director.getBiomeController());
    LOGGER.debug("[AgeBuilder]   Registered interfaces: {} alterations, {} populators",
        director.getTerrainAlterations().size(),
        director.getPopulateFunctions().size());
    if (!director.getTerrainAlterations().isEmpty()) {
      for (var alt : director.getTerrainAlterations()) {
        LOGGER.debug("[AgeBuilder]     Alteration: {}", alt.getClass().getSimpleName());
      }
    }
    if (!director.getPopulateFunctions().isEmpty()) {
      for (var pop : director.getPopulateFunctions()) {
        LOGGER.debug("[AgeBuilder]     Populator: {}", pop.getIdentifier());
      }
    }

    // Set the instability
    director.setInstability(instability);

    return director;
  }

  private void applyStarFissureDefault(AgeDirectorImpl director, Random rand) {
    if (director.isPersonalPocket()) {
      return;
    }
    boolean explicit = director.isStarFissureExplicit();
    boolean enabled = director.isStarFissureEnabled();

    boolean hasPopulator = false;
    for (IPopulate pop : director.getPopulateFunctions()) {
      if ("mystcraft:star_fissure".equals(pop.getIdentifier())) {
        hasPopulator = true;
        break;
      }
    }
    if (hasPopulator && !explicit) {
      director.setStarFissureEnabled(true);
      director.setStarFissureExplicit(true);
      return;
    }

    if (!explicit) {
      enabled = rand.nextBoolean();
      director.setStarFissureEnabled(enabled);
      director.setStarFissureExplicit(false);
    }

    if (!enabled) {
      return;
    }

    if (!hasPopulator) {
      JsonObject params = director.getStarFissureParams();
      if (params == null) {
        params = new JsonObject();
      }
      IPopulate populator = PopulatorRegistry.create(new ResourceLocation("mystcraft", "star_fissure"), params, director.getSeed());
      if (populator != null) {
        director.registerInterface(populator);
      }
    }
  }

  public float getInstability() {
    return instability;
  }

  public List<IAgeSymbol> getAllSymbols() {
    return expandedSymbols;
  }

  // ===================================================================
  // --- Default Color Palettes ---
  // ===================================================================

  public List<IAgeSymbol> getInputSymbols() {
    return inputSymbols;
  }

  public long getSeed() {
    return seed;
  }

  public boolean isComplete() {
    return generatedCount == 0;
  }

  public int getProvidedCount() {
    return providedCount;
  }

  public int getGeneratedCount() {
    return generatedCount;
  }

  public int getMissingSymbolCount() {
    return missingSymbolCount;
  }

  /**
   * Applies a terrain block bias during random generation.
   */
  private void applyTerrainBlockBias(AgeDirectorImpl director, Random rand, float fallbackScale) {
    boolean hasExplicitBlock = false;
    for (IAgeSymbol symbol : expandedSymbols) {
      ResourceLocation name = symbol.getRegistryName();
      if (name.getPath().startsWith("block_") && symbol.getCategory() == SymbolCategory.MODIFIER) {
        hasExplicitBlock = true;
        break;
      }
    }

    if (hasExplicitBlock) {
      return;
    }

    if (rand.nextFloat() >= TERRAIN_BLOCK_CHANGE_CHANCE * fallbackScale) {
      return;
    }

    String terrainType = director.getTerrainType();
    Block[] pool = getTerrainBlockPool(terrainType);
    if (pool.length == 0) {
      return;
    }

    Block chosen = pool[rand.nextInt(pool.length)];
    BlockState chosenState = chosen.defaultBlockState();
    director.setTerrainBlock(chosenState);

    LOGGER.info("[AgeBuilder] Terrain block bias applied: {} for terrain type '{}'",
        chosen.getName().getString(), terrainType);
  }

  /**
   * Randomly adds ore boost or ore disable symbols when the grammar
   * did not produce any explicit ore modifiers. Gives random ages
   * distinct resource profiles.
   */
  private void applyOreVariation(AgeDirectorImpl director, Random rand, float fallbackScale) {
    // Check if any ore modifier was already provided
    boolean hasOreModifier = false;
    for (IAgeSymbol symbol : expandedSymbols) {
      String path = symbol.getRegistryName().getPath();
      if (path.startsWith("extra_") && path.endsWith("_ore")) {
        hasOreModifier = true;
        break;
      }
      if (path.startsWith("no_") && path.endsWith("_ore")) {
        hasOreModifier = true;
        break;
      }
      if (path.equals("no_ores") || path.equals("dense_ores")) {
        hasOreModifier = true;
        break;
      }
    }

    if (hasOreModifier) {
      return;
    }

    // Roll for ore boost (1-3 random extra ore symbols)
    if (rand.nextFloat() < ORE_BOOST_CHANCE * fallbackScale) {
      int boostCount = 1 + rand.nextInt(3);
      Set<Integer> pickedIndices = new HashSet<>();
      for (int i = 0; i < boostCount && pickedIndices.size() < EXTRA_ORE_SYMBOLS.length; i++) {
        int idx = rand.nextInt(EXTRA_ORE_SYMBOLS.length);
        if (pickedIndices.add(idx)) {
          IAgeSymbol oreSymbol = SymbolRegistry.get(
              new ResourceLocation("mystcraft", EXTRA_ORE_SYMBOLS[idx]));
          if (oreSymbol != null) {
            oreSymbol.registerLogic(director, rand.nextLong());
            director.addInstability(oreSymbol.getInstabilityCost() * 0.5f);
            LOGGER.debug("[AgeBuilder] Applied ore boost: {}", EXTRA_ORE_SYMBOLS[idx]);
          }
        }
      }
    }

    // Roll for ore disable (1 random ore disabled)
    if (rand.nextFloat() < ORE_DISABLE_CHANCE * fallbackScale) {
      int idx = rand.nextInt(DISABLE_ORE_SYMBOLS.length);
      IAgeSymbol disableSymbol = SymbolRegistry.get(
          new ResourceLocation("mystcraft", DISABLE_ORE_SYMBOLS[idx]));
      if (disableSymbol != null) {
        disableSymbol.registerLogic(director, rand.nextLong());
        director.addInstability(disableSymbol.getInstabilityCost() * 0.5f);
        LOGGER.debug("[AgeBuilder] Applied ore disable: {}", DISABLE_ORE_SYMBOLS[idx]);
      }
    }
  }

  /**
   * Randomly applies a timescale variation when the grammar didn't produce one.
   * Gives random ages distinct day/night cycle speeds.
   */
  private void applyTimescaleVariation(AgeDirectorImpl director, Random rand, float fallbackScale) {
    // Check if any timescale was already provided
    boolean hasTimescale = false;
    for (IAgeSymbol symbol : expandedSymbols) {
      String path = symbol.getRegistryName().getPath();
      if (path.startsWith("env_longer") || path.startsWith("env_shorter")
          || path.startsWith("env_slow_time") || path.startsWith("env_static_time")) {
        hasTimescale = true;
        break;
      }
    }

    if (hasTimescale) {
      return;
    }

    // Also skip if timescale was already set by some other mechanism
    if (director.getTimescale() != 1.0f) {
      return;
    }

    if (rand.nextFloat() >= TIMESCALE_CHANGE_CHANCE * fallbackScale) {
      return;
    }

    // Weighted random selection
    int totalWeight = 0;
    for (int weight : TIMESCALE_WEIGHTS) {
      totalWeight += weight;
    }
    int roll = rand.nextInt(totalWeight);
    int cumulative = 0;
    int selectedIdx = 0;
    for (int i = 0; i < TIMESCALE_WEIGHTS.length; i++) {
      cumulative += TIMESCALE_WEIGHTS[i];
      if (roll < cumulative) {
        selectedIdx = i;
        break;
      }
    }

    IAgeSymbol timescaleSymbol = SymbolRegistry.get(
        new ResourceLocation("mystcraft", TIMESCALE_SYMBOLS[selectedIdx]));
    if (timescaleSymbol != null) {
      timescaleSymbol.registerLogic(director, rand.nextLong());
      // Reduced instability for fallback-applied timescale
      director.addInstability(timescaleSymbol.getInstabilityCost() * 0.3f);
      LOGGER.debug("[AgeBuilder] Applied timescale variation: {}", TIMESCALE_SYMBOLS[selectedIdx]);
    }
  }

  /**
   * For ages with high instability or random rolls, adds an environmental
   * effect to create more dangerous dimensions. Unstable ages are more
   * hostile by nature.
   */
  private void applyEnvironmentEffects(AgeDirectorImpl director, Random rand, float fallbackScale) {
    // Check if any env effect was already provided
    boolean hasEnvEffect = false;
    for (IAgeSymbol symbol : expandedSymbols) {
      String path = symbol.getRegistryName().getPath();
      if (path.startsWith("env_") && symbol.getCategory() == SymbolCategory.ENVIRONMENT) {
        hasEnvEffect = true;
        break;
      }
    }

    if (hasEnvEffect) {
      return;
    }

    // Higher instability makes env effects more likely
    float effectChance = ENV_EFFECT_CHANCE * fallbackScale;
    if (instability > ENV_EFFECT_INSTABILITY_THRESHOLD) {
      float instabilityBonus = (instability - ENV_EFFECT_INSTABILITY_THRESHOLD) * 0.005f;
      effectChance = Math.min(0.5f, effectChance + instabilityBonus);
    }

    if (rand.nextFloat() >= effectChance) {
      return;
    }

    // Weighted random selection
    int totalWeight = 0;
    for (int weight : ENV_EFFECT_WEIGHTS) {
      totalWeight += weight;
    }
    int roll = rand.nextInt(totalWeight);
    int cumulative = 0;
    int selectedIdx = 0;
    for (int i = 0; i < ENV_EFFECT_WEIGHTS.length; i++) {
      cumulative += ENV_EFFECT_WEIGHTS[i];
      if (roll < cumulative) {
        selectedIdx = i;
        break;
      }
    }

    IAgeSymbol envSymbol = SymbolRegistry.get(
        new ResourceLocation("mystcraft", ENV_EFFECT_SYMBOLS[selectedIdx]));
    if (envSymbol != null) {
      envSymbol.registerLogic(director, rand.nextLong());
      LOGGER.debug("[AgeBuilder] Applied environment effect: {} (instability was {})",
          ENV_EFFECT_SYMBOLS[selectedIdx], instability);
    }
  }

  /**
   * Injects 1-2 random features when the grammar didn't already generate them.
   */
  private void applyFeatureInjection(AgeDirectorImpl director, Random rand, float fallbackScale) {
    if (rand.nextFloat() >= FEATURE_INJECTION_CHANCE * fallbackScale) {
      return;
    }

    Set<String> existingFeatures = new HashSet<>();
    for (IPopulate pop : director.getPopulateFunctions()) {
      String id = pop.getIdentifier();
      if (id != null && id.startsWith("mystcraft:")) {
        existingFeatures.add(id.substring("mystcraft:".length()));
      }
    }

    int injectCount = 1 + rand.nextInt(2);
    List<String> candidates = new ArrayList<>();
    for (String feature : INJECTABLE_FEATURES) {
      if (!existingFeatures.contains(feature)) {
        candidates.add(feature);
      }
    }

    for (int i = 0; i < injectCount && !candidates.isEmpty(); i++) {
      int idx = rand.nextInt(candidates.size());
      String feature = candidates.remove(idx);
      IAgeSymbol featureSymbol = SymbolRegistry.get(
          new ResourceLocation("mystcraft", feature));
      if (featureSymbol != null) {
        featureSymbol.registerLogic(director, rand.nextLong());
        director.addInstability(featureSymbol.getInstabilityCost() * 0.5f);
        LOGGER.debug("[AgeBuilder] Injected feature: {}", feature);
      }
    }
  }

  /**
   * Injects nether/end biomes into overworld terrain ages for truly alien dimensions.
   */
  private void applyBiomeChaos(AgeDirectorImpl director, Random rand, float fallbackScale) {
    String terrainType = findTerrainType();
    if (NETHER_TERRAINS.contains(terrainType) || END_TERRAINS.contains(terrainType)
        || VOID_TERRAINS.contains(terrainType)) {
      return;
    }

    if (rand.nextFloat() >= BIOME_CHAOS_CHANCE * fallbackScale) {
      return;
    }

    int injectCount = 1 + rand.nextInt(2);
    for (int i = 0; i < injectCount; i++) {
      String biomePath = CHAOS_BIOMES[rand.nextInt(CHAOS_BIOMES.length)];
      IAgeSymbol biomeSymbol = SymbolRegistry.get(
          new ResourceLocation("mystcraft", biomePath));
      if (biomeSymbol != null) {
        expandedSymbols.add(biomeSymbol);
        director.addInstability(biomeSymbol.getInstabilityCost());
        LOGGER.debug("[AgeBuilder] Injected chaos biome: {}", biomePath);
      }
    }
  }

  /**
   * Generates random colors for sky, fog, cloud, grass, foliage, water,
   * sunset, night sky, and horizon when the grammar didn't produce color
   * symbols. Each Age gets a unique visual signature.
   */
  private void applyDefaultColors(AgeDirectorImpl director, Random rand) {
    // Monochrome palette: pick a hue family and use it for everything
    if (rand.nextFloat() < MONOCHROME_CHANCE) {
      int[][] hueFamily = {
          {0x4B0082, 0x6A0DAD, 0x9370DB, 0xB19CD9, 0xE6E6FA}, // Purple
          {0x8B0000, 0xDC143C, 0xFF6347, 0xFFB6C1, 0xFFF0F5}, // Red/Rose
          {0x006400, 0x228B22, 0x32CD32, 0x98FB98, 0xF0FFF0}, // Green
          {0x00008B, 0x4169E1, 0x6495ED, 0xADD8E6, 0xF0F8FF}, // Blue
          {0x8B4513, 0xD2691E, 0xDEB887, 0xFFDEAD, 0xFFF8DC}, // Brown/Amber
      };
      int[] family = hueFamily[rand.nextInt(hueFamily.length)];
      if (director.getSkyColor() == -1 && !director.isSkyColorNatural()) {
        director.setSkyColor(family[3]);
      }
      if (director.getFogColor() == -1 && !director.isFogColorNatural()) {
        director.setFogColor(family[4]);
      }
      if (director.getCloudColor() == -1 && !director.isCloudColorNatural()) {
        director.setCloudColor(family[4]);
      }
      if (director.getGrassColors().isEmpty() && !director.isGrassColorNatural()) {
        director.setGrassColor(family[1]);
      }
      if (director.getFoliageColor() == -1 && !director.isFoliageColorNatural()) {
        director.setFoliageColor(family[2]);
      }
      if (director.getWaterColor() == -1 && !director.isWaterColorNatural()) {
        director.setWaterColor(family[0]);
      }
      director.addInstability(8.0f);
      LOGGER.debug("[AgeBuilder] Applied monochrome color palette");
      return;
    }

    // Inverted palette: dark sky, bright fog, unusual grass/foliage
    if (rand.nextFloat() < INVERTED_PALETTE_CHANCE) {
      if (director.getSkyColor() == -1 && !director.isSkyColorNatural()) {
        director.setSkyColor(0x0A0A2E);
      }
      if (director.getFogColor() == -1 && !director.isFogColorNatural()) {
        director.setFogColor(0xFFE4B5);
      }
      if (director.getCloudColor() == -1 && !director.isCloudColorNatural()) {
        director.setCloudColor(0x2C2C54);
      }
      if (director.getGrassColors().isEmpty() && !director.isGrassColorNatural()) {
        director.setGrassColor(0xFF4500);
      }
      if (director.getFoliageColor() == -1 && !director.isFoliageColorNatural()) {
        director.setFoliageColor(0x9370DB);
      }
      if (director.getWaterColor() == -1 && !director.isWaterColorNatural()) {
        director.setWaterColor(0x4B0082);
      }
      director.addInstability(10.0f);
      LOGGER.debug("[AgeBuilder] Applied inverted color palette");
      return;
    }

    if (director.getSkyColor() == -1 && !director.isSkyColorNatural()) {
      int color = DEFAULT_SKY_COLORS[rand.nextInt(DEFAULT_SKY_COLORS.length)];
      director.setSkyColor(color);
      director.addInstability(2.0f);
      LOGGER.debug("[AgeBuilder] Applied default sky color: #{}", String.format("%06X", color));
    }

    if (director.getFogColor() == -1 && !director.isFogColorNatural()) {
      int color = DEFAULT_FOG_COLORS[rand.nextInt(DEFAULT_FOG_COLORS.length)];
      director.setFogColor(color);
      director.addInstability(2.0f);
      LOGGER.debug("[AgeBuilder] Applied default fog color: #{}", String.format("%06X", color));
    }

    if (director.getCloudColor() == -1 && !director.isCloudColorNatural()) {
      int color = DEFAULT_CLOUD_COLORS[rand.nextInt(DEFAULT_CLOUD_COLORS.length)];
      director.setCloudColor(color);
      director.addInstability(2.0f);
      LOGGER.debug("[AgeBuilder] Applied default cloud color: #{}", String.format("%06X", color));
    }

    if (director.getGrassColors().isEmpty() && !director.isGrassColorNatural()) {
      int color = DEFAULT_GRASS_COLORS[rand.nextInt(DEFAULT_GRASS_COLORS.length)];
      director.setGrassColor(color);
      director.addInstability(3.0f);
      LOGGER.debug("[AgeBuilder] Applied default grass color: #{}", String.format("%06X", color));
    }

    if (director.getFoliageColor() == -1 && !director.isFoliageColorNatural()) {
      int color = DEFAULT_FOLIAGE_COLORS[rand.nextInt(DEFAULT_FOLIAGE_COLORS.length)];
      director.setFoliageColor(color);
      director.addInstability(3.0f);
      LOGGER.debug("[AgeBuilder] Applied default foliage color: #{}", String.format("%06X", color));
    }

    if (director.getWaterColor() == -1 && !director.isWaterColorNatural()) {
      int color = DEFAULT_WATER_COLORS[rand.nextInt(DEFAULT_WATER_COLORS.length)];
      director.setWaterColor(color);
      director.addInstability(3.0f);
      LOGGER.debug("[AgeBuilder] Applied default water color: #{}", String.format("%06X", color));
    }

    // Sunset color: 30% chance
    if (director.getSunsetColor() == -1 && rand.nextFloat() < 0.30f) {
      int color = DEFAULT_SUNSET_COLORS[rand.nextInt(DEFAULT_SUNSET_COLORS.length)];
      director.setSunsetColor(color);
      director.addInstability(2.0f);
      LOGGER.debug("[AgeBuilder] Applied default sunset color: #{}", String.format("%06X", color));
    }

    // Night sky color: 25% chance
    if (director.getNightSkyColor() == -1 && rand.nextFloat() < 0.25f) {
      int color = DEFAULT_NIGHT_SKY_COLORS[rand.nextInt(DEFAULT_NIGHT_SKY_COLORS.length)];
      director.setNightSkyColor(color);
      director.addInstability(2.0f);
      LOGGER.debug("[AgeBuilder] Applied default night sky color: #{}", String.format("%06X", color));
    }

    // Horizon color: 20% chance
    if (director.getHorizonColor() == -1 && !director.isHorizonColorNatural() && rand.nextFloat() < 0.20f) {
      int color = DEFAULT_HORIZON_COLORS[rand.nextInt(DEFAULT_HORIZON_COLORS.length)];
      director.setHorizonColor(color);
      director.addInstability(2.0f);
      LOGGER.debug("[AgeBuilder] Applied default horizon color: #{}", String.format("%06X", color));
    }
  }

}
