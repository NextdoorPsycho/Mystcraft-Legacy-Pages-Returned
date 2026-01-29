package art.arcane.mystcraft.guidebook;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all content for the Mystcraft Guidebook.
 * Comprehensive reference covering every system in the mod.
 */
public class GuidebookContent {

  private static GuidebookContent INSTANCE;

  private final List<GuidebookChapter> chapters;

  private GuidebookContent() {
    this.chapters = new ArrayList<>();
    initializeContent();
  }

  public static GuidebookContent getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new GuidebookContent();
    }
    return INSTANCE;
  }

  public List<GuidebookChapter> getChapters() {
    return chapters;
  }

  private void initializeContent() {
    chapters.add(createCoreConcepts());
    chapters.add(createItems());
    chapters.add(createBlocks());
    chapters.add(createEntities());
    chapters.add(createSymbolSystem());
    chapters.add(createTerrainSymbols());
    chapters.add(createBiomeSymbols());
    chapters.add(createSunSymbols());
    chapters.add(createMoonSymbols());
    chapters.add(createStarSymbols());
    chapters.add(createWeatherSymbols());
    chapters.add(createLightingSymbols());
    chapters.add(createFeatureSymbols());
    chapters.add(createStructureSymbols());
    chapters.add(createColorSystem());
    chapters.add(createModifierSymbols());
    chapters.add(createEnvironmentSymbols());
    chapters.add(createOreFluidSymbols());
    chapters.add(createGrammarEngine());
    chapters.add(createWorldGeneration());
    chapters.add(createInstabilitySystem());
    chapters.add(createInstabilityEffects());
    chapters.add(createDeathSystem());
    chapters.add(createLinkingPortals());
    chapters.add(createArchivistVillager());
    chapters.add(createStructuresLoot());
    chapters.add(createAdvancements());
    chapters.add(createCommands());
    chapters.add(createConfiguration());
    chapters.add(createAgePresets());
    chapters.add(createInkWriting());
    chapters.add(createDimensionTypes());
  }

  // --- Chapter 1: Core Concepts ---

  private GuidebookChapter createCoreConcepts() {
    return GuidebookChapter.create("Core Concepts")
        .addPage("Ages",
            "Ages are custom dimensions. Each Age is defined by symbols " +
                "written onto pages, bound into a Descriptive Book. When used " +
                "for the first time, the grammar engine fills gaps, calculates " +
                "instability, and creates a new dimension.")
        .addPage("Symbols",
            "Symbols are the building blocks of Age design. They define " +
                "terrain, biome distribution, weather, lighting, celestials, " +
                "colors, features, structures, and environment. Each symbol " +
                "has an instability cost.")
        .addPage("Instability",
            "Instability is the balancing mechanism. Ages with too many " +
                "powerful features, missing components, or conflicting symbols " +
                "accumulate instability. High instability triggers decay, " +
                "explosions, meteors, lightning, and potion effects.")
        .addPage("Linking",
            "Linking is how players travel between dimensions. Descriptive " +
                "Books create new Ages. Linkbooks provide return trips to " +
                "specific coordinates. Both can be placed in Book Receptacles " +
                "to create portal structures.");
  }

  // --- Chapter 2: Items ---

  private GuidebookChapter createItems() {
    return GuidebookChapter.create("Items")
        .addPage("Descriptive Book",
            "The core item for creating Ages. Contains bound symbol pages. " +
                "Stack size 1. Right-click opens book GUI. First use creates " +
                "the Age dimension. Shows Age number, author, page count, and " +
                "instability rating. Glows when linked.")
        .addPage("Linkbook",
            "A book linked to a specific location. Stack size 1. Auto-records " +
                "position on first inventory tick. Has a health system: 10 max HP. " +
                "Takes damage from fire (5/tick), drowning (20/tick), collision, " +
                "starvation, and explosions (3x).")
        .addPage("Unlinked Linkbook",
            "A blank linkbook. Stack size 16. Right-click converts one into " +
                "a linked Linkbook recording your current position and dimension. " +
                "Always carry a linked return book before entering any Age.")
        .addPage("Page",
            "Symbol pages are the writing medium. Stack size 64. Types: Blank " +
                "Page, Link Panel (special marker, glows), Symbol Page (has a " +
                "specific symbol written). Created at the Writing Desk with ink.")
        .addPage("Ink Vial",
            "Holds colored ink for the Writing Desk. Stack size 16. Has color " +
                "(RGB hex) and amount (0-100). Visual progress bar shows ink level. " +
                "Default color is black (0x000000).")
        .addPage("Booster Pack",
            "One-time-use pack generating random pages. Stack size 16. " +
                "Right-click opens and generates 3 random pages: 20% chance " +
                "for Link Panel, 80% for random symbol. Pages added to " +
                "inventory or dropped if full.")
        .addPage("Guidebook",
            "In-game manual (this book). Stack size 1. Right-click opens " +
                "tutorial screen. New players receive one on first login " +
                "(configurable via giveGuidebookOnFirstSpawn).")
        .addPage("Folder",
            "Collation Folder: ordered, writable page container. Stack " +
                "size 32 when empty, 1 with pages. Holds 16 pages in fixed " +
                "slots. Can write symbols directly to blank pages inside. " +
                "Right-click opens FolderMenu.")
        .addPage("Portfolio",
            "Symbol Portfolio: unordered page archive. Stack size always 1. " +
                "Holds 64 pages. Searchable and sortable collection. Cannot write " +
                "symbols directly. Auto-sorts by category and name. Tracks stats.")
        .addPage("Ink Bucket",
            "Bucket containing black ink fluid. Stack size 1. Returns an " +
                "empty vanilla bucket as craft remainder.");
  }

  // --- Chapter 3: Blocks ---

  private GuidebookChapter createBlocks() {
    return GuidebookChapter.create("Blocks")
        .addPage("Writing Desk",
            "Multi-block (2x2x2) crafting station for writing symbols. Wood, " +
                "hardness 2.5. Structure: main + foot + two top blocks. Accepts " +
                "ink vial, blank page, and dyes to produce symbol pages. " +
                "Redstone signal based on ink amount.")
        .addPage("Book Binder",
            "Binds pages into books. Wood, hardness 2.5. Accepts pages and " +
                "leather to create Agebooks or Linkbooks. Requires at least 1 " +
                "Link Panel as first page. 16 page slots + cover slot. Can " +
                "import from Folder/Portfolio. Redstone 1-15.")
        .addPage("Ink Mixer",
            "Mixes ink colors. Wood, hardness 2.5. Mixes black ink with " +
                "dyes for colored inks. Creates link panels with properties. " +
                "Has ink bucket slot, dye slots, output slot. Redstone 15 " +
                "when ink present.")
        .addPage("Link Modifier",
            "Modifies link properties on books. Stone, hardness 3.0. " +
                "Applies flags to links: following, disarm, generate platform, " +
                "intra-linking, relative, maintain momentum.")
        .addPage("Book Receptacle",
            "Holds a book and creates a portal. Stone, hardness 3.5. Must " +
                "be placed on a Crystal Block. Insert a linked book to activate " +
                "portal, retrieve to deactivate. Redstone 15 when book present. " +
                "Fires PortalUtils to create portal.")
        .addPage("Bookstand",
            "Display pedestal for books. Wood, hardness 2.0. Right-click to " +
                "open book GUI. Shift+empty-hand to pick up book. Custom block " +
                "entity renderer shows open book model. Redstone based on " +
                "book presence.")
        .addPage("Lectern",
            "Alternative book display stand. Wood, hardness 2.0. Same " +
                "function as Bookstand with different appearance. Registered " +
                "as Archivist villager's point-of-interest workstation block.")
        .addPage("Crystal Block",
            "Frame block for portal structures. Has ACTIVE and " +
                "SOURCE_DIRECTION properties. Becomes part of portal when " +
                "activated by a receptacle. Redstone 15 when active. " +
                "Configurable light level (0-15).")
        .addPage("Link Portal Block",
            "The teleportation surface within portals. Has ACTIVE and " +
                "SOURCE_DIRECTION properties. Light level 11. No collision. " +
                "100-tick (5 second) cooldown between teleports per entity. " +
                "Finds receptacle via PortalUtils.")
        .addPage("Decay Block",
            "Instability corruption block. Unbreakable by default. " +
                "5 types:\n" +
                "Blue: No spread, no damage\n" +
                "Purple: Spreads, 3 HP damage\n" +
                "Red: Spreads, 4 HP damage\n" +
                "White: Spreads, no damage\n" +
                "Black: Spreads, 2 HP, falls")
        .addPage("Star Fissure",
            "Dimensional rift serving as exit portal. Light level 15. " +
                "No collision. Teleports entities to overworld spawn or " +
                "player respawn point. Custom sky portal effect renderer. " +
                "Plays fissure linking sound.")
        .addPage("Ink Fluid",
            "Black ink as a placeable fluid block. Slope distance 2, " +
                "level decrease 2 per block. Black color. Supports fill/empty " +
                "with Ink Bucket.");
  }

  // --- Chapter 4: Entities ---

  private GuidebookChapter createEntities() {
    return GuidebookChapter.create("Entities")
        .addPage("Linkbook Entity",
            "Dropped/placed books in the world. Size 0.5x0.5. Max 100 HP. " +
                "Damage: fire 5/tick, drowning 20/tick, collision 10/speed, " +
                "starvation 1/10000 ticks, explosions 3x. Right-click opens GUI, " +
                "shift+click picks up. Hopper support.")
        .addPage("Meteor Entity",
            "Falling projectile from instability or commands. Size 2x2. " +
                "Fire immune. Falls from sky and impacts terrain with explosion. " +
                "Plays roar on approach, impact on landing. Scale 1-3x " +
                "configurable via instability or command.")
        .addPage("Other Entities",
            "Colored Lightning: Transient visual lightning bolt with " +
                "custom color. No save/summon. Visual-only at low instability, " +
                "damaging at high.\n\n" +
                "Falling Block: Custom falling block for decay physics (0.98x0.98).\n\n" +
                "Dummy: Utility/placeholder, no tracking/save/summon.");
  }

  // --- Chapter 5: Symbol System ---

  private GuidebookChapter createSymbolSystem() {
    return GuidebookChapter.create("Symbol System")
        .addPage("Symbol Properties",
            "Every symbol has: ID (registry name), Display Name, Category " +
                "(grammar role), Rank (0-4 rarity tier), Instability Cost " +
                "(positive = destabilizing, negative = stabilizing), Poem Words " +
                "(four D'ni words), and Allowed in Random flag.")
        .addPage("Categories",
            "TERRAIN: Singleton, base terrain\n" +
                "BIOME_CONTROLLER: Singleton\n" +
                "BIOME: Multiple allowed\n" +
                "WEATHER: Singleton\n" +
                "LIGHTING: Singleton\n" +
                "SUN: Multiple allowed\n" +
                "MOON: Multiple allowed\n" +
                "STARS: Multiple allowed")
        .addPage("More Categories",
            "FEATURE_LARGE: Caves, ravines\n" +
                "FEATURE_MEDIUM: Ores, spikes\n" +
                "FEATURE_SMALL: Lakes, etc.\n" +
                "STRUCTURE: Villages, etc.\n" +
                "VISUAL_EFFECT: Color targets\n" +
                "ENVIRONMENT: Effects\n" +
                "SEA: Singleton, ocean fluid")
        .addPage("Modifier Categories",
            "COLOR: Stackable color values\n" +
                "ANGLE: Direction modifiers\n" +
                "PHASE: Orbital phase mods\n" +
                "LENGTH: Scale modifiers\n" +
                "MODIFIER: General modifiers\n" +
                "SPECIAL: Unique effects\n\n" +
                "Singleton = only one allowed per Age.");
  }

  // --- Chapter 6: Terrain Symbols ---

  private GuidebookChapter createTerrainSymbols() {
    return GuidebookChapter.create("Terrain")
        .addPage("Normal Terrain",
            "terrain_normal - Instability 0.0\n" +
                "Standard overworld-like procedural terrain with noise-based " +
                "heights. Rolling hills, mountains, and valleys. The most stable " +
                "and familiar option.")
        .addPage("Amplified Terrain",
            "terrain_amplified - Instability 5.0\n" +
                "Exaggerated heights at 2x normal scale. Mountains become towering " +
                "spires, valleys plunge to bedrock. Dramatic and dangerous.")
        .addPage("Flat Terrain",
            "terrain_flat - Instability 0.0\n" +
                "Flat world with configurable ground level. No hills, no valleys. " +
                "Useful for construction. Stable but monotonous.")
        .addPage("Void Terrain",
            "terrain_void - Instability 10.0\n" +
                "Empty void with single bedrock layer. Only specific features " +
                "like floating islands appear. Falling means death. " +
                "Removes caves, villages, dungeons, mineshafts, strongholds.")
        .addPage("Nether Terrain",
            "terrain_nether - Instability 8.0\n" +
                "Ceiling and floor with open caves between. Netherrack base. " +
                "Enclosed caverns with lava seas. Removes incompatible surface " +
                "structures.")
        .addPage("End Terrain",
            "terrain_end - Instability 10.0\n" +
                "Distance-based floating islands. Denser near center, sparse " +
                "at distance. End stone base. No sea. Falling is fatal.")
        .addPage("Skylands",
            "terrain_skylands - Instability 8.0\n" +
                "Dense floating terrain masses. No ocean or ground below. " +
                "Extended height range. No bedrock floor.")
        .addPage("Cave World",
            "terrain_cave - Instability 8.0\n" +
                "Underground-focused terrain with extensive cave networks. " +
                "Ceiling above, no sky visible. Underground dimension type.");
  }

  // --- Chapter 7: Biome Symbols ---

  private GuidebookChapter createBiomeSymbols() {
    return GuidebookChapter.create("Biomes")
        .addPage("Biome Controllers",
            "biome_single: One biome everywhere (-5.0)\n" +
                "biome_native: Standard distribution (-5.0)\n" +
                "biome_tiny: 64-block Voronoi (-2.0)\n" +
                "biome_small: 128-block Voronoi (-3.0)\n" +
                "biome_medium: 256-block Voronoi (-4.0)\n" +
                "biome_large: 512-block Voronoi (-3.0)")
        .addPage("More Controllers",
            "biome_huge: 1024-block Voronoi (-2.0)\n" +
                "biome_tiled: Organized rectangular tiles (0.0)\n" +
                "biome_grid: Strict grid pattern (2.0)\n\n" +
                "Negative instability means these are stabilizing. " +
                "Controllers are singleton - only one per Age.")
        .addPage("Overworld Biomes",
            "All vanilla biomes available as symbols. All instability 0.0, " +
                "rank 1. Over 50 biomes including:\n\n" +
                "Plains, Forest, Taiga, Desert, Swamp, Jungle, Snowy Plains, " +
                "Savanna, Badlands, Dark Forest, Birch Forest, Flower Forest...")
        .addPage("More Overworld",
            "Meadow, Beach, Ocean, Deep Ocean, River, Mushroom Fields, " +
                "Windswept Hills, Cherry Grove, Old Growth Spruce/Pine Taiga, " +
                "Old Growth Birch Forest, Sparse Jungle, Bamboo Jungle, " +
                "Mangrove Swamp, Stony Shore, Ice Spikes, Dripstone/Lush Caves...")
        .addPage("Nether & End Biomes",
            "Nether: Nether Wastes, Crimson Forest, Warped Forest, " +
                "Soul Sand Valley, Basalt Deltas\n\n" +
                "End: The End, End Highlands, End Midlands, End Barrens, " +
                "Small End Islands\n\n" +
                "All instability 0.0, rank 1.");
  }

  // --- Chapter 8: Sun Symbols ---

  private GuidebookChapter createSunSymbols() {
    return GuidebookChapter.create("Suns")
        .addPage("Sun Symbols",
            "sun_normal: Standard sun, 20-min day (0.0)\n" +
                "sun_dark: Eclipse silhouette (5.0)\n" +
                "sun_large: 2.5x normal size (5.0)\n" +
                "sun_small: 0.4x normal size (3.0)\n" +
                "sun_fast: 10-min day, 2x speed (8.0)\n" +
                "sun_slow: 40-min day, 0.5x speed (5.0)")
        .addPage("Sun Rendering",
            "Sun renders with: core disc, animated corona (48 segments), " +
                "24 radial rays, and outer glow halo. Pulsing animation " +
                "(0.92-1.0 scale), rotating rays, wobbling corona. Dark sun " +
                "renders black disc with bright corona ring.")
        .addPage("Sun Colors",
            "Default sun color: gold (255, 242, 204). Overridable via " +
                "color modifier symbols placed before the sun symbol. " +
                "Multiple suns allowed per Age. An Age with no sun is " +
                "eternally dark.");
  }

  // --- Chapter 9: Moon Symbols ---

  private GuidebookChapter createMoonSymbols() {
    return GuidebookChapter.create("Moons")
        .addPage("Moon Symbols",
            "moon_normal: Standard moon with phases (0.0)\n" +
                "moon_dark: Lunar eclipse (3.0)\n" +
                "moon_large: 2.5x normal size (3.0)\n" +
                "moon_small: 0.4x normal size (3.0)\n" +
                "moon_full: Always full phase (2.0)\n" +
                "moon_fast: 2x phase cycle (5.0)\n" +
                "moon_slow: 0.5x phase cycle (3.0)")
        .addPage("Moon Rendering",
            "Renders with: textured disc (32 segments), procedural " +
                "craters, atmospheric glow (48 segments). Phase-aware " +
                "shading (0-1 range). Full moon symbol locks at max phase. " +
                "Dark moon shows corona ring.");
  }

  // --- Chapter 10: Star Symbols ---

  private GuidebookChapter createStarSymbols() {
    return GuidebookChapter.create("Stars")
        .addPage("Star Symbols",
            "stars_normal: Standard 1500 stars (0.0)\n" +
                "stars_twinkle: Enhanced twinkle (0.0)\n" +
                "stars_end: End-dimension particles (5.0)\n" +
                "stars_dark: No stars rendered (2.0)\n" +
                "stars_dense: 4000 stars (3.0)\n" +
                "stars_sparse: 400 stars (0.0)")
        .addPage("Star Rendering",
            "Multi-layer parallax at different rotation speeds. Varied " +
                "color temperatures from blue-white to orange-red. Per-star " +
                "twinkle effect. Configurable count (100-8000). Features nebula " +
                "clouds, shooting stars, brightness fade at dawn/dusk.");
  }

  // --- Chapter 11: Weather Symbols ---

  private GuidebookChapter createWeatherSymbols() {
    return GuidebookChapter.create("Weather")
        .addPage("Weather Symbols",
            "weather_normal: Standard cycling (0.0)\n" +
                "weather_off: No weather events (0.0)\n" +
                "weather_always: Constant rain (5.0)\n" +
                "weather_rain: Always raining (3.0)\n" +
                "weather_snow: Always snowing (3.0)\n" +
                "weather_storm: Constant thunder (8.0)")
        .addPage("More Weather",
            "weather_cloudy: Always cloudy, no rain (0.0)\n" +
                "weather_fast: 2x weather cycles (3.0)\n" +
                "weather_slow: 0.5x weather cycles (2.0)\n" +
                "weather_thunder: Frequent lightning (10.0)\n" +
                "weather_blizzard: Heavy snow/blizzard (12.0)")
        .addPage("Weather Details",
            "Weather is singleton (one per Age). Rain duration: " +
                "6000 ticks (5 min) base + variance. Thunder duration: " +
                "3600 ticks (3 min) base. Transitions are smooth level " +
                "changes at 0.01 per tick.");
  }

  // --- Chapter 12: Lighting Symbols ---

  private GuidebookChapter createLightingSymbols() {
    return GuidebookChapter.create("Lighting")
        .addPage("Lighting Symbols",
            "lighting_normal: Standard brightness (0.0)\n" +
                "lighting_bright: 30% brighter, min light 4/15 (5.0)\n" +
                "lighting_dark: Very dark, reduced ambient (10.0)\n" +
                "lighting_nether: Dim ambient glow (8.0)\n\n" +
                "Lighting is singleton - one per Age.")
        .addPage("Lighting Effects",
            "Bright: 1.2x fog RGB (clamped), enhanced illumination.\n" +
                "Dark: 0.5x fog RGB, near/far fog planes reduced to " +
                "50%/70%. Reduced ambient light makes navigation dangerous.\n" +
                "Nether: Dim, consistent glow similar to the Nether dimension.");
  }

  // --- Chapter 13: Feature Symbols ---

  private GuidebookChapter createFeatureSymbols() {
    return GuidebookChapter.create("Features")
        .addPage("Large Features",
            "caves: Vanilla cave systems (0.0)\n" +
                "ravines: Deep chasms (0.0)\n" +
                "floating_islands: 6 morphologies (10.0)\n" +
                "skylands: Dense floating terrain (8.0)\n" +
                "perlin_worms: Large noise tunnels (6.0)\n" +
                "deep_dark: Sculk features (15.0)\n" +
                "star_fissure_feature: Exit portal (-10.0)")
        .addPage("Floating Islands",
            "6 procedural morphologies:\n" +
                "Classic: Broad plateau (30%)\n" +
                "Spire: Tall narrow peak (15%)\n" +
                "Mushroom: Wide canopy/stem (15%)\n" +
                "Eroded: Irregular with holes (15%)\n" +
                "Mesa: Flat-top, layered (15%)\n" +
                "Archipelago: Linked cluster (10%)")
        .addPage("Medium Features",
            "dense_ores: Abundant ore veins (5.0)\n" +
                "huge_trees: Mega trees (3.0)\n" +
                "deep_lakes: Underground pools (0.0)\n" +
                "spikes: Ice-spike formations (5.0)\n" +
                "spheres: Floating/embedded orbs (8.0)\n" +
                "tendrils: Twisting 20-50 block vines (5.0)\n" +
                "vertical_tendrils: Column formations (4.0)")
        .addPage("Small Features",
            "surface_lakes: Water/lava on surface (0.0)\n" +
                "dripstone_caves: Stalactites (0.0)\n" +
                "lush_caves: Moss, glow berries (0.0)\n\n" +
                "Star Fissure Feature has -10.0 instability, meaning it " +
                "actively stabilizes the Age. It generates the exit portal.");
  }

  // --- Chapter 14: Structure Symbols ---

  private GuidebookChapter createStructureSymbols() {
    return GuidebookChapter.create("Structures")
        .addPage("Basic Structures",
            "villages: NPC villages (0.0)\n" +
                "dungeons: Mob spawner rooms (0.0)\n" +
                "mineshafts: Underground mines (0.0)\n" +
                "strongholds: End fortress (0.0)\n" +
                "igloos: Snowy igloos (0.0)\n" +
                "trail_ruins: Surface trails (0.0)\n" +
                "shipwrecks: Sunken ships (0.0)")
        .addPage("More Structures",
            "ocean_ruins: Underwater ruins (0.0)\n" +
                "buried_treasure: Treasure targets (0.0)\n" +
                "desert_temples: Pyramid temples (3.0)\n" +
                "jungle_temples: Puzzle temples (3.0)\n" +
                "witch_huts: Swamp houses (3.0)\n" +
                "nether_fossils: Bone structures (3.0)")
        .addPage("High-Value Structures",
            "pillager_outposts: Outpost towers (5.0)\n" +
                "ocean_monuments: Guardian temples (5.0)\n" +
                "ruined_portals: Broken portals (8.0)\n" +
                "nether_fortress: Nether fortress (10.0)\n" +
                "woodland_mansions: Dark mansions (8.0)\n" +
                "bastion_remnants: Nether bastions (12.0)")
        .addPage("Dangerous Structures",
            "ancient_cities: Deep underground (15.0)\n" +
                "end_cities: End dimension cities (15.0)\n\n" +
                "Higher instability structures contain better loot but " +
                "contribute more instability to the Age. Balance " +
                "carefully when writing.");
  }

  // --- Chapter 15: Color System ---

  private GuidebookChapter createColorSystem() {
    return GuidebookChapter.create("Colors")
        .addPage("Color Targets",
            "Each target has custom (0.0) and natural (-2.0) variants:\n\n" +
                "color_sky / color_sky_natural\n" +
                "color_sky_night (custom only)\n" +
                "color_cloud / color_cloud_natural\n" +
                "color_fog / color_fog_natural")
        .addPage("More Targets",
            "color_foliage / color_foliage_natural\n" +
                "color_grass / color_grass_natural\n" +
                "color_water / color_water_natural\n" +
                "color_horizon / color_horizon_natural\n" +
                "color_sunset / color_sunset_natural\n\n" +
                "Natural variants use biome-appropriate hues and are stabilizing.")
        .addPage("Basic Colors",
            "16 basic color modifiers (all 0.0):\n" +
                "Red (FF0000), Orange (FF8000)\n" +
                "Yellow (FFFF00), Green (00FF00)\n" +
                "Blue (0000FF), Purple (8000FF)\n" +
                "White (FFFFFF), Black (000000)\n" +
                "Cyan (00FFFF), Magenta (FF00FF)")
        .addPage("More Basic Colors",
            "Lime (80FF00), Pink (FF80C0)\n" +
                "Gray (808080), Light Gray (C0C0C0)\n" +
                "Brown (8B4513), Light Blue (87CEEB)\n\n" +
                "All color modifiers are rank 0, instability 0.0. " +
                "Place color before target: Red + Sky Color = red sky.")
        .addPage("Extended Colors",
            "22 additional colors:\n" +
                "Maroon, Olive, Dark Green, Teal, Navy, Silver, Gold, " +
                "Coral, Crimson, Turquoise, Indigo, Amber, Rose, Jade, " +
                "Sapphire, Ruby, Violet, Copper, Midnight, Emerald, " +
                "Peach, Ivory")
        .addPage("Gradient Symbols",
            "gradient_sunset: Orange-red (0.0)\n" +
                "gradient_dawn: Pink-gold (0.0)\n" +
                "gradient_dusk: Purple dusk (0.0)\n" +
                "gradient_aurora: Green aurora (3.0)\n" +
                "gradient_blood_sky: Dark red (5.0)\n" +
                "gradient_builder: Pops color stack\n" +
                "gradient_apply_sunset: Blends/applies");
  }

  // --- Chapter 16: Modifier Symbols ---

  private GuidebookChapter createModifierSymbols() {
    return GuidebookChapter.create("Modifiers")
        .addPage("Direction Modifiers",
            "Category: ANGLE\n\n" +
                "mod_north: 0 degrees\n" +
                "mod_east: 90 degrees\n" +
                "mod_south: 180 degrees\n" +
                "mod_west: 270 degrees\n\n" +
                "Applied to celestial bodies for positioning.")
        .addPage("Phase Modifiers",
            "Category: PHASE\n\n" +
                "mod_nadir: Bottom (0 degrees)\n" +
                "mod_rising: Rising (90 degrees)\n" +
                "mod_zenith: Top (180 degrees)\n" +
                "mod_setting: Setting (270 degrees)\n\n" +
                "Controls orbital phase of celestials.")
        .addPage("Length Modifiers",
            "Category: LENGTH\n\n" +
                "mod_zero: 0.0 (frozen)\n" +
                "mod_half: 0.5 (half speed)\n" +
                "mod_full: 1.0 (normal)\n" +
                "mod_double: 2.0 (double speed)\n\n" +
                "Primarily affects day cycle length.")
        .addPage("General Modifiers",
            "mod_clear: Reset all modifier stacks (0.0)\n" +
                "no_sea: Removes all water/sea (5.0)\n" +
                "cloud_low: Clouds at Y=96 (3.0)\n" +
                "cloud_high: Clouds at Y=300 (3.0)\n" +
                "clouds_none: Clouds hidden Y=-64 (5.0)\n" +
                "horizon_low: Horizon at Y=-32 (2.0)\n" +
                "horizon_high: Horizon at Y=64 (2.0)")
        .addPage("Special Symbols",
            "star_fissure: Home portal (-25.0)\n" +
                "obelisks: Obelisk structures (5.0)\n" +
                "anti_pvp: Disables PvP (0.0)\n" +
                "hide_horizon: Hides horizon (3.0)\n" +
                "crystal_formation: Crystal gen (8.0)\n" +
                "rainbow: Rainbow in sky (0.0)\n\n" +
                "Star Fissure is highly stabilizing at -25.");
  }

  // --- Chapter 17: Environment Symbols ---

  private GuidebookChapter createEnvironmentSymbols() {
    return GuidebookChapter.create("Environment")
        .addPage("Environment Effects",
            "env_accelerated: 2x crop/mob spawn (10.0)\n" +
                "env_meteors: Meteor strikes (25.0)\n" +
                "env_lightning: Frequent lightning (15.0)\n" +
                "env_scorched: Spontaneous fire (20.0)\n" +
                "env_explosions: Random explosions (30.0)\n\n" +
                "All are Category: ENVIRONMENT.")
        .addPage("Timescale Symbols",
            "env_longer_days: 40-min cycle, 0.5x (5.0)\n" +
                "env_shorter_days: 10-min cycle, 2x (5.0)\n" +
                "env_slow_time: 80-min cycle, 0.25x (10.0)\n" +
                "env_static_time: Time frozen (15.0)\n\n" +
                "Also Category: ENVIRONMENT. Affect the day/night " +
                "cycle speed of the Age.");
  }

  // --- Chapter 18: Ore/Fluid/Block Symbols ---

  private GuidebookChapter createOreFluidSymbols() {
    return GuidebookChapter.create("Ores & Fluids")
        .addPage("Ore Symbols",
            "Individual ore control symbols:\n" +
                "extra_iron_ore, extra_coal_ore,\n" +
                "extra_copper_ore, extra_gold_ore,\n" +
                "extra_diamond_ore, extra_emerald_ore,\n" +
                "extra_redstone_ore, extra_lapis_ore\n\n" +
                "Each adds extra veins of that specific ore.")
        .addPage("Sea/Fluid Symbols",
            "Category: SEA (singleton)\n\n" +
                "sea_minecraft_water: Water ocean (0.0)\n" +
                "sea_minecraft_lava: Lava ocean (25.0)\n\n" +
                "Additional fluids registered dynamically from the Forge " +
                "fluid registry, supporting modded fluids automatically.")
        .addPage("Terrain Block Symbols",
            "~294 dynamic symbols for terrain block overrides. Generated " +
                "for all vanilla blocks. Sets the primary terrain material.\n\n" +
                "block_minecraft_stone (0.0)\n" +
                "block_minecraft_diamond_ore (20.0)\n" +
                "block_minecraft_obsidian (10.0)")
        .addPage("More Block Symbols",
            "Includes all wood types, stone variants, ores, terracotta " +
                "colors, concrete colors, wool colors, glass colors, ice " +
                "variants, and more. Each has appropriate instability " +
                "based on the block's value.");
  }

  // --- Chapter 19: Grammar Engine ---

  private GuidebookChapter createGrammarEngine() {
    return GuidebookChapter.create("Grammar")
        .addPage("How Ages Are Built",
            "The grammar engine is a Context-Free Grammar (CFG) system. " +
                "It expands user symbols into a complete Age specification, " +
                "ensuring every Age has all required components even if the " +
                "player provides only a few symbols.")
        .addPage("Building Steps",
            "1. Player binds pages into Agebook with Link Panel\n" +
                "2. Symbols read and matched to grammar\n" +
                "3. CFGGrammarTree builds derivation trees\n" +
                "4. Missing components filled via weighted random\n" +
                "5. Post-processing removes duplicates")
        .addPage("More Steps",
            "6. Enforces singleton categories\n" +
                "7. Filters incompatible combinations\n" +
                "8. Instability calculated\n" +
                "9. Symbols apply effects to AgeDirector (150+ properties)\n" +
                "10. AgeDimensionFactory creates the dimension")
        .addPage("Grammar Tree",
            "Root (age) branches into:\n" +
                "terrain_gen, biome_controller,\n" +
                "biomes (1-3), weather, lighting,\n" +
                "block_sea, suns (1-3), moons (1-3),\n" +
                "starfields (1-3), visuals,\n" +
                "feature_larges, feature_mediums,\n" +
                "feature_smalls, effects")
        .addPage("Post-Processing",
            "Singleton enforcement: Only one terrain, biome controller, " +
                "weather, lighting, sea symbol.\n\n" +
                "Void terrain: Removes caves, ravines, villages, dungeons, " +
                "mineshafts, strongholds.\n\n" +
                "Nether terrain: Removes surface structures.\n" +
                "Fallback biome: Plains if none specified.")
        .addPage("Instability Formula",
            "base = sum of all symbol costs\n" +
                "genPenalty = generatedCount * 2.5\n" +
                "missingPenalty = missingCount * 8.0\n" +
                "totalRaw = base + genPenalty + missingPenalty\n\n" +
                "Well-written bonus: if no generated symbols, 5+ provided, " +
                "no missing: totalRaw *= 0.8\n\n" +
                "Final = totalRaw * 0.5")
        .addPage("Random Fallbacks",
            "After grammar, random variations applied:\n\n" +
                "Terrain Block Bias: 45% chance random block\n" +
                "Ore Variation: 30% boost (1-3 extra), 15% disable\n" +
                "Timescale: 20% chance (longer 40%, shorter 30%, " +
                "slow 20%, static 10%)")
        .addPage("More Fallbacks",
            "Environment Effects: 12% base chance, increases " +
                "with instability > 20 (up to 50%)\n\n" +
                "Color Palettes: Random colors assigned to any " +
                "unset color targets.\n\n" +
                "Specify everything you can to minimize randomness " +
                "and instability.");
  }

  // --- Chapter 20: World Generation ---

  private GuidebookChapter createWorldGeneration() {
    return GuidebookChapter.create("World Gen")
        .addPage("Chunk Generator",
            "AgeChunkGenerator wraps vanilla NoiseBasedChunkGenerator. " +
                "Normal, Amplified, Nether, End, Cave, Skylands use vanilla " +
                "noise as base. Void and Flat bypass vanilla noise entirely.")
        .addPage("Terrain Mix",
            "Multiple terrain types can blend across chunks:\n\n" +
                "checkerboard: Alternating 64-block regions\n" +
                "gradient: Gradual transitions\n" +
                "gradient_x / gradient_z: Per-axis gradients")
        .addPage("Normal Terrain",
            "Grid-based noise with 4x4x8 resolution cells, trilinear " +
                "interpolation, 5x5x49 noise field. Uses 4 octave noise " +
                "generators (16+ octaves). Blends via blend factor noise. " +
                "Parabolic falloff for biome height blending.")
        .addPage("Other Terrain",
            "Amplified: Normal with doubled heights (1.0 + h * 2.0)\n" +
                "Nether: Cosine wave vertical envelope, higher frequency\n" +
                "End: Distance-based density from origin\n" +
                "Void: Single bedrock layer, air above\n" +
                "Flat: Direct block filling to ground level")
        .addPage("Scripted Terrain",
            "Datapack symbols can register scripted terrain generators.\n\n" +
                "Density > 0 = solid, <= 0 = air.\n\n" +
                "Ops: add/sub/mul/div, min/max, clamp, lerp,\n" +
                "step/smoothstep, noise2/3, fbm2/3, ridged2/3,\n" +
                "cell, cell_distance. Variables: x,y,z,seed.")
        .addPage("Scripted Reference",
            "Primitive values: 12.5 or {\"value\":12.5}\n" +
                "Variables: {\"var\":\"x\"}, {\"var\":\"y\"}, {\"var\":\"z\"}\n\n" +
                "Binary ops: {\"op\":\"add\",\"a\":X,\"b\":Y}\n" +
                "Unary ops: {\"op\":\"abs\",\"input\":X}\n\n" +
                "Noise2: {\"op\":\"noise2\",\"scale\":0.01,\"octaves\":4,\"seed\":42}\n" +
                "Noise3: {\"op\":\"noise3\",\"scale\":0.02,\"octaves\":5}\n" +
                "Cells: {\"op\":\"cell\",\"mode\":\"hex\",\"size\":48,\"min\":0,\"max\":1}\n" +
                "CellDist: {\"op\":\"cell_distance\",\"mode\":\"square\",\"size\":64}\n\n" +
                "Common form: {\"op\":\"sub\",\"a\":HEIGHT,\"b\":{\"var\":\"y\"}}")
        .addPage("Biome Controllers",
            "Single: One biome everywhere.\n" +
                "Native: Standard overworld distribution.\n" +
                "Noise (Voronoi): Jittered cell regions, 64 to 1024 blocks. " +
                "4096-entry cache.\n" +
                "Tiled: Rectangular tile grid.\n" +
                "Grid: Strict grid pattern.")
        .addPage("Populators",
            "33+ populators including all vanilla structures plus " +
                "Mystcraft-specific features:\n\n" +
                "Spheres: 1 per ~33 chunks, radius 5-15\n" +
                "Tendrils: 1 per ~12 chunks, 20-50 blocks, 7+ chunk span\n" +
                "Spikes: Vertical rocky formations\n" +
                "Perlin Worms: Noise-carved tunnels")
        .addPage("Resource Pops",
            "Standard Ores: Normal ore distribution\n" +
                "Dense Ores: Extremely abundant veins\n" +
                "Single Ore: Individual ore type control\n\n" +
                "Decoration: Biome flowers/trees, deep lakes, surface lakes, " +
                "huge trees.\n\n" +
                "Cross-chunk safety: Neighbor-seed pattern ensures " +
                "deterministic generation.")
        .addPage("Surface Building",
            "Applies surface/subsurface block overrides per biome. " +
                "Handles grass/sand/snow top layers, dirt/gravel/sandstone " +
                "subsurface placement based on biome type.");
  }

  // --- Chapter 21: Instability System ---

  private GuidebookChapter createInstabilitySystem() {
    return GuidebookChapter.create("Instability")
        .addPage("Rating Tiers",
            "0 or less: Perfectly Stable\n" +
                "1-10: Stable\n" +
                "11-30: Slightly Unstable\n" +
                "31-50: Unstable\n" +
                "51-80: Very Unstable\n" +
                "81-100: Dangerous\n" +
                "101-150: Extremely Dangerous\n" +
                "150+: Catastrophic")
        .addPage("The Deck System",
            "Effects organized into 5 card decks. Each has a base " +
                "instability cost to unlock. Cards within have individual " +
                "costs. As instability increases, more cards are drawn.")
        .addPage("Deck Contents",
            "basic (cost 0): Scorched x3, Extra Ticks x5\n" +
                "harsh (cost 10): More environmental\n" +
                "destructive (cost 30): Crumble, Erosion, Explosions, " +
                "Lightning, Meteors\n" +
                "eating (cost 50): Potion debuffs + buffs, Decay (blue/purple/red)")
        .addPage("Death Deck",
            "death (cost 75): Global potion variants, " +
                "high-tier Decay (white/black)\n\n" +
                "Card drawing: Controller iterates shuffled deck cards, " +
                "spending instability budget on each cost. Shuffling ensures " +
                "different effects per Age at same instability.")
        .addPage("Frequency Formula",
            "chance = baseChance * (1.0 + min(excess / 30.0, 5.0)) " +
                "* multiplier\n\n" +
                "excess = instability - threshold\n" +
                "Maximum 6x frequency at very high instability.")
        .addPage("Intensity Scaling",
            "Environmental: intensity = max(0.1, min(instability / 80.0, 1.0))\n\n" +
                "High-threshold: intensity = max(0.0, min((instability - " +
                "threshold) / range, 1.0))\n\n" +
                "Effects scale from weak to devastating based on instability.");
  }

  // --- Chapter 23: Instability Effects ---

  private GuidebookChapter createInstabilityEffects() {
    return GuidebookChapter.create("Effects")
        .addPage("Crumble",
            "Cost 8. Ore degradation chains:\n" +
                "Diamond > Emerald > Gold > Iron > Coal > Stone\n" +
                "Stone Bricks > Cracked > Cobblestone > Gravel\n" +
                "Glass/Leaves > Air\n\n" +
                "1-8 passes per tick scaling with intensity.")
        .addPage("Decay",
            "Cost 8-20. Spreads colored decay blocks. 5 types with " +
                "varying damage and spread. 1-6 attempts per tick.\n\n" +
                "Blue: Non-spreading, no damage\n" +
                "Purple: Spreading, 3 HP\n" +
                "Red: Spreading, 4 HP")
        .addPage("More Decay",
            "White: Spreading, no damage (visual)\n" +
                "Black: Spreading, 2 HP, special falling behavior, " +
                "removes fluids. Most destructive type.\n\n" +
                "Spread: Random tick converts adjacent solid blocks to decay. " +
                "Contact damage on touch.")
        .addPage("Erosion",
            "Cost 8. Fluids erode adjacent solid blocks. Checks all 6 " +
                "neighbors for fluid. Replaces solid with adjacent fluid. " +
                "1-6 attempts per tick. Gradually dissolves terrain near " +
                "water and lava.")
        .addPage("Explosions",
            "Cost 5, threshold 30. Random explosions near players. " +
                "Power 1.0-2.0 scaling with intensity. Minimum 8 blocks " +
                "from player. Base chance 0.0005/tick.")
        .addPage("Extra Ticks",
            "Cost 6. Accelerates random tick events: crop growth, " +
                "fire spread, grass spread. 1-8 extra tick passes per " +
                "chunk section. Stacks with Accelerated environment symbol.")
        .addPage("Scorched",
            "Cost 2, lowest threshold. Random fire blocks on solid " +
                "surfaces. Base chance 0.008/tick. One of the first effects " +
                "to appear in unstable Ages.")
        .addPage("Lightning",
            "Cost 5, threshold 50. Lightning strikes near players. " +
                "Visual-only at low instability (80 blocks away). " +
                "Damaging above instability 70 (24 blocks away). " +
                "Minimum 16-40 block distance from player.")
        .addPage("Meteor",
            "Cost 5, threshold 30. Falling meteors from 100-150 " +
                "blocks above player. Size 1-3 scaling with intensity. " +
                "Base chance 0.0005/tick. Creates explosion on impact.")
        .addPage("Potion Debuffs",
            "Threshold 50+. Applied to players:\n" +
                "Blindness (3s), Mining Fatigue (4s),\n" +
                "Hunger (4s), Nausea (3s),\n" +
                "Poison (4s), Slowness (4s),\n" +
                "Weakness (4s), Wither (1.5s)\n\n" +
                "Wither is the most dangerous.")
        .addPage("Potion Buffs",
            "Chaotic boons mixed with debuffs:\n" +
                "Speed (30s), Haste (30s),\n" +
                "Strength (20s), Jump Boost (30s),\n" +
                "Regeneration (10s), Resistance (15s),\n" +
                "Night Vision (60s)\n\n" +
                "Unpredictable - both help and harm.")
        .addPage("Enemy Buffs",
            "Applied to all hostile mobs in loaded chunks:\n" +
                "Regeneration and Resistance.\n\n" +
                "Makes hostile mobs significantly harder to kill " +
                "in high-instability Ages.");
  }

  // --- Chapter 24: Death System ---

  private GuidebookChapter createDeathSystem() {
    return GuidebookChapter.create("Death System")
        .addPage("Death Tracking",
            "AgeDeathHandler tracks per-player, per-Age death counts. " +
                "Selects thematic death messages by instability tier.")
        .addPage("Death Messages",
            "Low (<50): release, ink_dries, thread_unravels\n" +
                "Medium (50-80): shudders, stricken, no_mourn\n" +
                "High (>80): devours, ink_runs_black, " +
                "reality_tears, already_dying\n\n" +
                "Cause-specific: void_fall, fire, magic, explosion, mob, decay")
        .addPage("Death Penalties",
            "Repeat deaths (3+): remembers, ink_knows, how_many\n\n" +
                "Instability surge on death (instability >= 50): " +
                "0.5 + severity * 7.5 added instability.\n\n" +
                "Respawn debuffs scale with severity:\n" +
                "Slowness: Always at 50+, 4-20s\n" +
                "Mining Fatigue: At 65+, 5-30s\n" +
                "Darkness: At 80+, 3-10s");
  }

  // --- Chapter 25: Linking and Portals ---

  private GuidebookChapter createLinkingPortals() {
    return GuidebookChapter.create("Linking")
        .addPage("Link Flags",
            "Stored in book NBT:\n" +
                "Following: Bring nearby entities\n" +
                "Disarm: Drop held items\n" +
                "Intra-Linking: Same-dimension TP\n" +
                "Relative: Apply position offset\n" +
                "Generate Platform: 3x3 stone\n" +
                "Maintain Momentum: Keep velocity")
        .addPage("Teleport Process",
            "1. Pre-load destination chunk\n" +
                "2. Calculate safe spawn Y (heightmap)\n" +
                "3. Detect ceiling terrain (nether/cave)\n" +
                "4. Generate spawn platform if flagged\n" +
                "5. Build fluid surface platform\n" +
                "6. Teleport entity and passengers\n" +
                "7. Teleport followers if Following\n" +
                "8. Play departure/arrival sounds")
        .addPage("Safe Spawn",
            "A position is safe if:\n" +
                "- Solid ground below (not lava/bedrock)\n" +
                "- Feet position is passable\n" +
                "- Head position is passable\n" +
                "- No fire, soul fire, or magma blocks")
        .addPage("Portal Structure",
            "Build portal from Crystal Blocks:\n" +
                "1. Place Crystal blocks as frame\n" +
                "2. Place Book Receptacle on Crystal\n" +
                "3. Insert linked book to activate\n" +
                "4. PortalUtils flood-fills portal blocks\n" +
                "5. Walk into portal to teleport\n" +
                "6. Remove book to deactivate")
        .addPage("Link Permissions",
            "Ages support access control:\n\n" +
                "Blacklist mode: Block specific players\n" +
                "Whitelist mode: Only allow specific players\n" +
                "Age owners: Full access to own Age\n" +
                "Global admins: Bypass all restrictions");
  }

  // --- Chapter 26: Archivist Villager ---

  private GuidebookChapter createArchivistVillager() {
    return GuidebookChapter.create("The Archivist")
        .addPage("Overview",
            "Custom villager profession trading Mystcraft items. Uses " +
                "the Lectern block as workstation. Has 5 trade levels " +
                "from Novice to Master. Spawns in Archivist houses " +
                "injected into village template pools.")
        .addPage("Level 1: Novice",
            "2 Emeralds -> 1 Ink Vial\n" +
                "8 Pages -> 1 Emerald\n" +
                "1 Emerald -> 4 Pages\n" +
                "Random rank-1 symbol page")
        .addPage("Level 2: Apprentice",
            "5 Emeralds -> 1 Folder\n" +
                "3 Emeralds -> 2 Ink Vials\n" +
                "Rank 1-2 symbol pages")
        .addPage("Level 3: Journeyman",
            "12 Emeralds -> 1 Portfolio\n" +
                "8 Emeralds -> 1 Booster Pack\n" +
                "Rank 2-3 symbol pages")
        .addPage("Level 4: Expert",
            "20 Emeralds -> 1 Unlinked Linkbook\n" +
                "Rank 3-4 symbol pages")
        .addPage("Level 5: Master",
            "Random symbol pages (dynamic pricing)\n" +
                "1 Linkbook -> 24 Emeralds\n" +
                "Category-specific high-rank symbols\n\n" +
                "Pricing formula: 4 * (1 + symbolRank) emeralds.\n" +
                "Rank 1=8, Rank 2=12, Rank 3=16, Rank 4=20")
        .addPage("Village Integration",
            "VillageStructureHandler injects an Archivist house into " +
                "vanilla village template pools for Plains, Desert, Savanna, " +
                "Snowy, and Taiga villages with 2x weight for higher " +
                "spawn chance.");
  }

  // --- Chapter 27: Structures and Loot ---

  private GuidebookChapter createStructuresLoot() {
    return GuidebookChapter.create("Loot")
        .addPage("Mystcraft Structures",
            "Abandoned Library: Surface structure in plains/forest. " +
                "Contains symbol pages, descriptive books, and workstations.\n\n" +
                "Underground Archive: Deep underground with rare symbols.\n\n" +
                "Scattered Library: Small standalone buildings across overworld.")
        .addPage("Structure Data",
            "Each structure has:\n" +
                "- Structure definition JSON (biome placement)\n" +
                "- Structure set JSON (spacing, separation)\n" +
                "- Template pool JSON (jigsaw pieces)\n" +
                "- Biome tags for placement filtering")
        .addPage("Loot Modifiers",
            "SymbolPageLootModifier: Adds 1-3 random symbol pages to " +
                "dungeon, mineshaft, stronghold, and custom structure chests " +
                "with 50% chance per chest.\n\n" +
                "GuidebookLootModifier: Adds guidebooks to stronghold " +
                "library chests with 25% chance.");
  }

  // --- Chapter 28: Advancements ---

  private GuidebookChapter createAdvancements() {
    return GuidebookChapter.create("Advancements")
        .addPage("Custom Triggers",
            "3 custom criteria triggers:\n\n" +
                "Writing Desk Write: Player writes a symbol at the Writing Desk " +
                "(ID: mystcraft:writing_desk_write)\n\n" +
                "Enter Age (Safe): Player enters a Mystcraft Age " +
                "(ID: mystcraft:enter_myst_dimension_safe)")
        .addPage("More Triggers",
            "Enter Age (Quinn): Thematic challenge variant " +
                "(ID: mystcraft:enter_myst_dimension_quinn)\n\n" +
                "Advancement data files at /data/mystcraft/advancements/:\n" +
                "linkbook.json - Obtaining a linkbook\n" +
                "write.json - Writing at a writing desk");
  }

  // --- Chapter 28: Commands ---

  private GuidebookChapter createCommands() {
    return GuidebookChapter.create("Commands")
        .addPage("Age Management",
            "All under /mystcraft:\n\n" +
                "age list: List all Ages (perm 0)\n" +
                "age tp <ageId>: Teleport (perm 2)\n" +
                "age info <ageId>: Show info (perm 0)\n" +
                "age create: Create empty Age (perm 2)")
        .addPage("Symbols & Items",
            "symbols: List all symbols (perm 0)\n" +
                "symbols info <id>: Details (perm 0)\n\n" +
                "give randombook [count=5]: Random Agebook (perm 2)\n" +
                "give agebook <ageId>: Book for Age (perm 2)\n" +
                "give preset <name>: Preset book (perm 2)")
        .addPage("Instability Cmds",
            "instability toggle [ageId] [on/off]: Toggle (perm 2)\n" +
                "instability set <value> [ageId]: Set value (perm 2)\n" +
                "reprofile [ageId]: Recalculate from pages (perm 2)\n\n" +
                "debug instability: Show debug info (perm 2)\n" +
                "where: Current Age info (perm 0)")
        .addPage("Environment Cmds",
            "time set <ticks>: Set Age time (perm 2)\n" +
                "time add <ticks>: Add time (perm 2)\n" +
                "weather clear|rain|thunder: Control weather (perm 2)\n" +
                "regen [radius=1]: Regenerate chunks (perm 2)\n" +
                "spawn meteor [scale=2]: Spawn meteor (perm 2)");
  }

  // --- Chapter 31: Configuration ---

  private GuidebookChapter createConfiguration() {
    return GuidebookChapter.create("Configuration")
        .addPage("General Settings",
            "Config file: mystcraft-common.toml\n\n" +
                "giveGuidebookOnFirstSpawn: true\n" +
                "maxSymbolsPerBook: 50 (-1 = unlimited)\n" +
                "deleteAgesOnStartup: false (dev only)\n" +
                "microDimensionsEnabled: false\n" +
                "microDimensionRadiusChunks: 0\n" +
                "microDimensionExtraChunks: 1\n" +
                "safeStories: true")
        .addPage("Instability Settings",
            "instabilityEnabled: true (master switch)\n" +
                "deathEffectsEnabled: true\n" +
                "allowUnstableAges: true\n" +
                "instabilityMultiplier: 1.0 (0.0-10.0)\n" +
                "maxAllowedInstability: 150.0 (0-1000)")
        .addPage("Effect Thresholds",
            "thresholdDecay: 20.0\n" +
                "thresholdTransmute: 30.0\n" +
                "thresholdLightning: 40.0\n" +
                "thresholdMeteor: 60.0\n" +
                "thresholdPoison: 80.0\n" +
                "thresholdWither: 100.0")
        .addPage("Effect Chances",
            "Base chances per tick:\n" +
                "chanceDecay: 0.001 (0.1%)\n" +
                "chanceTransmute: 0.002 (0.2%)\n" +
                "chanceLightning: 0.0005 (0.05%)\n" +
                "chanceMeteor: 0.0002 (0.02%)\n" +
                "chancePlayerEffect: 0.0001 (0.01%)");
  }

  // --- Chapter 32: Age Presets ---

  private GuidebookChapter createAgePresets() {
    return GuidebookChapter.create("Presets")
        .addPage("Using Presets",
            "Curated presets via /mystcraft give preset <name>. " +
                "Each defines fixed symbols plus random pools for variety. " +
                "30+ presets available.")
        .addPage("Basic Presets",
            "classic: Balanced overworld Age\n" +
                "lush: Vegetation paradise, bright, rainbow\n" +
                "nether: Nether terrain and biomes\n" +
                "void: Void terrain for building\n" +
                "skylands: Floating terrain\n" +
                "cave: Underground world")
        .addPage("Themed Presets",
            "frozen: Snow and ice themed\n" +
                "desert: Arid badlands\n" +
                "ocean: Ocean-dominated\n" +
                "dark: Dark, dangerous Ages\n\n" +
                "And 20+ more themed presets available.")
        .addPage("Preset Structure",
            "Each preset specifies:\n\n" +
                "Fixed symbols: terrain, biome controller, lighting, weather\n\n" +
                "Pool picks: e.g., 'pick 6 biomes from 13 options'\n\n" +
                "Pool categories: biomes, celestials, structures, ores, " +
                "features, modifiers, gradients");
  }

  // --- Chapter 33: Ink and Writing ---

  private GuidebookChapter createInkWriting() {
    return GuidebookChapter.create("Ink Effects")
        .addPage("Ink Effect System",
            "Items combined with Link Panels in the Ink Mixer add link " +
                "properties. Each item has specific properties and " +
                "probability chances.")
        .addPage("Common Reagents",
            "Gunpowder: Disarm (20%)\n" +
                "Clay Ball: Gen Platform (25%)\n" +
                "Experience Bottle: Intra-Link (15%)\n" +
                "Ender Pearl: Intra-Link + Disarm (15% each)\n" +
                "Feather: Momentum (15%)\n" +
                "Fire Charge: Disarm (25%)")
        .addPage("More Reagents",
            "Ghast Tear: Relative (15%)\n" +
                "Slime Ball: Momentum (10%)\n" +
                "Magma Cream: Following (20%)\n" +
                "Blaze Powder: Disarm + Following (10% each)\n" +
                "Ender Eye: Intra-Link 20% + Following 15%\n" +
                "Chorus Fruit: Relative (20%)")
        .addPage("Rare Reagents",
            "Prismarine Crystals: Platform (20%)\n" +
                "Nether Star: All properties (10% each)\n" +
                "Gold Ingot: Intra-Link 10%, Platform 5%\n" +
                "Diamond: Intra 10%, Momentum 5%, Platform 5%\n" +
                "Emerald: Following (15%)\n" +
                "Amethyst Shard: Relative (10%)\n" +
                "Echo Shard: Relative + Intra (15% each)")
        .addPage("Property Colors",
            "Visual indicator colors:\n\n" +
                "Intra-Linking: Green\n" +
                "Intra-Linking-Only: White\n" +
                "Generate-Platform: Gray\n" +
                "Maintain-Momentum: Blue\n" +
                "Disarm: Red\n" +
                "Relative: Purple\n" +
                "Following: Orange");
  }

  // --- Chapter 31: Dimension Types ---

  private GuidebookChapter createDimensionTypes() {
    return GuidebookChapter.create("Dimensions")
        .addPage("Dimension Types",
            "Ages use 7 dimension type configurations:\n\n" +
                "Normal: Overworld-like, Y -64 to 320, natural sky\n" +
                "Nether: Ceiling at 128, no sky, always dark\n" +
                "End: No weather, fixed time\n" +
                "Dark: Always dark, reduced ambient")
        .addPage("More Dim Types",
            "Bright: Always bright, increased ambient light\n" +
                "Cave: Underground, ceiling above, no sky\n" +
                "Skylands: No bedrock floor, extended height\n\n" +
                "The terrain symbol you choose determines which " +
                "dimension type is used for your Age.");
  }
}
