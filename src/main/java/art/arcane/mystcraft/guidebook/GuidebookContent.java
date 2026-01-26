package art.arcane.mystcraft.guidebook;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all content for the Mystcraft Guidebook.
 * The guidebook is divided into chapters covering different aspects
 * of the Art of Writing Ages.
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
        chapters.add(createIntroductionChapter());
        chapters.add(createInkAndMaterialsChapter());
        chapters.add(createWritingToolsChapter());
        chapters.add(createBooksChapter());
        chapters.add(createContainersChapter());
        chapters.add(createBlocksChapter());
        chapters.add(createGrammarChapter());
        chapters.add(createTerrainChapter());
        chapters.add(createBiomesChapter());
        chapters.add(createCelestialsChapter());
        chapters.add(createFeaturesChapter());
        chapters.add(createStructuresChapter());
        chapters.add(createEnvironmentChapter());
        chapters.add(createModifiersChapter());
        chapters.add(createStabilityChapter());
    }

    private GuidebookChapter createIntroductionChapter() {
        return GuidebookChapter.create("Introduction")
                .addPage("Welcome",
                        "Welcome to the Art of Writing Ages. " +
                        "This guide will teach you everything you need " +
                        "to know about crafting your own dimensions.")
                .addPage("The D'ni Legacy",
                        "Long ago, a civilization called the D'ni mastered " +
                        "the art of creating passages to other worlds through " +
                        "specially crafted books. Their knowledge has survived, " +
                        "and now you can learn it too.")
                .addPage("What You'll Learn",
                        "In this guide, you will discover how to:\n\n" +
                        "- Craft ink and materials\n" +
                        "- Use the Writing Desk\n" +
                        "- Create Linking Books\n" +
                        "- Write Descriptive Books\n" +
                        "- Master the grammar of Ages")
                .addPage("Getting Started",
                        "To begin your journey, you'll need to gather some " +
                        "basic materials: paper, leather, ink sacs, and access " +
                        "to a crafting table. The first step is always creating ink.");
    }

    private GuidebookChapter createInkAndMaterialsChapter() {
        return GuidebookChapter.create("Ink & Materials")
                .addPage("Black Ink",
                        "The foundation of all Age writing is ink. Black ink " +
                        "is created by combining water with ink sacs. You can " +
                        "use a cauldron or craft an Ink Mixer for more efficient " +
                        "ink production.")
                .addPage("Ink Vials",
                        "Ink Vials are portable containers that hold ink for " +
                        "use at the Writing Desk. Craft an empty vial with glass, " +
                        "then fill it at an Ink Mixer or cauldron containing ink.")
                .addPage("The Ink Mixer",
                        "The Ink Mixer is a specialized workstation for creating " +
                        "and storing ink. Place ink sacs and a water source in its " +
                        "slots to produce black ink. The mixer stores ink internally " +
                        "and dispenses it into vials.")
                .addPage("Colored Ink",
                        "While black ink is used for writing, colored ink can " +
                        "be created by adding dyes to the Ink Mixer. Colored inks " +
                        "have special applications in modifying the appearance of Ages.")
                .addPage("Paper & Leather",
                        "Symbol pages require paper, crafted from sugar cane as " +
                        "usual. Book covers require leather from cows. You'll need " +
                        "plenty of both materials as you progress in your studies.");
    }

    private GuidebookChapter createWritingToolsChapter() {
        return GuidebookChapter.create("Writing Tools")
                .addPage("The Writing Desk",
                        "The Writing Desk is the primary workstation for creating " +
                        "symbol pages. It allows you to copy symbols from existing " +
                        "pages onto blank paper using ink.")
                .addPage("Using the Desk",
                        "To use the Writing Desk:\n\n" +
                        "1. Place an ink vial in the ink slot\n" +
                        "2. Place blank paper in the paper slot\n" +
                        "3. Insert a symbol page to copy\n" +
                        "4. Click write to create copies")
                .addPage("Ink Consumption",
                        "Each symbol copy consumes ink from your vial. More " +
                        "complex symbols may require more ink. Keep several " +
                        "filled vials ready when doing extensive writing work.")
                .addPage("The Book Binder",
                        "The Book Binder combines symbol pages into complete " +
                        "Descriptive Books. Place pages in order, add a leather " +
                        "cover, and bind them together to create your Age.")
                .addPage("Binding Process",
                        "When binding a book:\n\n" +
                        "- First page must be a Link Panel\n" +
                        "- Add symbol pages in order\n" +
                        "- Order affects grammar\n" +
                        "- Finish with leather cover");
    }

    private GuidebookChapter createBooksChapter() {
        return GuidebookChapter.create("Books & Linking")
                .addPage("Descriptive Books",
                        "Descriptive Books define entire Ages. When first used, " +
                        "they create a new dimension based on the symbols within. " +
                        "The same book always links to the same Age.")
                .addPage("Creating a Book",
                        "To create a Descriptive Book:\n\n" +
                        "1. Gather symbol pages\n" +
                        "2. Start with a Link Panel\n" +
                        "3. Add terrain, biome, and other symbols\n" +
                        "4. Bind in the Book Binder")
                .addPage("Linking Books",
                        "Linking Books create passages to specific locations. " +
                        "Unlike Descriptive Books, they link to existing places " +
                        "rather than creating new worlds.")
                .addPage("Creating Links",
                        "To create a Linking Book:\n\n" +
                        "1. Craft an Unlinked Link Book\n" +
                        "2. Right-click while holding it\n" +
                        "3. The book binds to your current location\n" +
                        "4. Use it to return from anywhere")
                .addPage("Link Safety",
                        "Always carry a Linking Book back to the Overworld! " +
                        "If you travel to an Age without one, you may become " +
                        "trapped. This is the most important rule of Age exploration.")
                .addPage("Book Properties",
                        "Books can have various properties:\n\n" +
                        "- Intra-Linking: Link within same dimension\n" +
                        "- Following: Bring nearby entities\n" +
                        "- Disarm: Remove held items\n" +
                        "- Maintain Momentum: Keep velocity");
    }

    private GuidebookChapter createContainersChapter() {
        return GuidebookChapter.create("Containers")
                .addPage("Collation Folder",
                        "The Collation Folder is a portable storage item for " +
                        "organizing symbol pages. It holds many pages and keeps " +
                        "them sorted for easy access during writing.")
                .addPage("Using Folders",
                        "Right-click a folder to open its inventory. Pages can " +
                        "be inserted and removed freely. The folder displays the " +
                        "number of pages it contains in its tooltip.")
                .addPage("Symbol Portfolio",
                        "The Symbol Portfolio is a specialized container for " +
                        "organizing pages by symbol type. It provides categorized " +
                        "storage for serious writers who collect many symbols.")
                .addPage("Portfolio Features",
                        "The Portfolio automatically sorts pages into categories:\n\n" +
                        "- Terrain pages\n" +
                        "- Biome pages\n" +
                        "- Celestial pages\n" +
                        "- Modifier pages\n" +
                        "- And more...")
                .addPage("Sealed Notebooks",
                        "Sealed Notebooks contain random symbol pages. They can " +
                        "be found in dungeon loot or traded with villagers. Open " +
                        "one by right-clicking to receive its contents.");
    }

    private GuidebookChapter createBlocksChapter() {
        return GuidebookChapter.create("Mystcraft Blocks")
                .addPage("Bookstand",
                        "The Bookstand displays books for decoration and easy " +
                        "access. Place a Descriptive Book or Linking Book on it " +
                        "and right-click to activate the link.")
                .addPage("Lectern",
                        "The Lectern functions similarly to the Bookstand but " +
                        "has a different appearance. Both allow hands-free book " +
                        "usage without holding the book in your inventory.")
                .addPage("Book Receptacle",
                        "The Book Receptacle is a decorative block that holds " +
                        "books securely. It protects books from accidental pickup " +
                        "and provides a permanent display location.")
                .addPage("Link Modifier",
                        "The Link Modifier adds special properties to Linking " +
                        "Books. Place a book and the appropriate modifiers to " +
                        "add effects like Following or Maintain Momentum.")
                .addPage("Crystal Block",
                        "Crystal blocks are decorative luminous blocks that " +
                        "emit light. They can be crafted or found naturally " +
                        "in certain Ages that include Crystal Formation.");
    }

    private GuidebookChapter createGrammarChapter() {
        return GuidebookChapter.create("Grammar of Ages")
                .addPage("Understanding Grammar",
                        "The Art of Writing follows specific rules called " +
                        "grammar. Symbols must be arranged properly for the " +
                        "Age to function correctly and remain stable.")
                .addPage("Basic Structure",
                        "A well-written Age follows this pattern:\n\n" +
                        "MODIFIER + TARGET\n\n" +
                        "Modifiers describe properties, targets receive them. " +
                        "For example: 'Red' (modifier) + 'Sky' (target) = red sky.")
                .addPage("Symbol Categories",
                        "Symbols fall into categories:\n\n" +
                        "- Terrain: World shape\n" +
                        "- Biome: Climate/vegetation\n" +
                        "- Celestials: Sun, moon, stars\n" +
                        "- Features: Caves, islands\n" +
                        "- Environment: Effects")
                .addPage("Modifier Order",
                        "When using modifiers, order matters:\n\n" +
                        "1. Color modifiers first\n" +
                        "2. Direction/angle modifiers\n" +
                        "3. Phase modifiers\n" +
                        "4. Length modifiers\n" +
                        "5. Target symbol last")
                .addPage("Clear Modifiers",
                        "The Clear Modifiers symbol resets any pending modifiers. " +
                        "Use it when you want to start fresh without affecting " +
                        "subsequent symbols. Very useful for complex Ages.")
                .addPage("Incomplete Ages",
                        "If you omit required symbols, the Age will fill in " +
                        "missing pieces randomly. This adds instability. For the " +
                        "most stable Ages, specify all major components explicitly.");
    }

    private GuidebookChapter createTerrainChapter() {
        return GuidebookChapter.create("Terrain Types")
                .addPage("Normal Terrain",
                        "Normal Terrain generates standard Overworld-style " +
                        "landscapes with hills, mountains, and valleys. This " +
                        "is the default and most familiar terrain type.")
                .addPage("Amplified Terrain",
                        "Amplified Terrain creates dramatically exaggerated " +
                        "landscapes with towering mountains and deep valleys. " +
                        "Expect extreme elevation changes throughout.")
                .addPage("Flat Terrain",
                        "Flat Terrain generates a completely level world. " +
                        "Useful for building projects or when you want " +
                        "predictable terrain without obstacles.")
                .addPage("Void Terrain",
                        "Void Terrain creates an empty dimension with no " +
                        "generated land. Only specific features like floating " +
                        "islands will appear. Dangerous without preparation.")
                .addPage("Nether Terrain",
                        "Nether Terrain generates the characteristic caves " +
                        "and lava seas of the Nether dimension, but in your " +
                        "custom Age with your chosen biomes and features.")
                .addPage("End Terrain",
                        "End Terrain creates floating islands over the void, " +
                        "similar to The End dimension. Combine with other " +
                        "symbols for unique floating world experiences.")
                .addPage("Terrain Blocks",
                        "You can modify the primary block used for terrain:\n\n" +
                        "- Stone (default)\n" +
                        "- Deepslate\n" +
                        "- Sandstone\n" +
                        "- Netherrack\n" +
                        "- And many more...");
    }

    private GuidebookChapter createBiomesChapter() {
        return GuidebookChapter.create("Biome Control")
                .addPage("Biome Controllers",
                        "Biome Controllers determine how biomes are distributed " +
                        "across your Age. Different controllers create different " +
                        "patterns and scales of biome placement.")
                .addPage("Single Biome",
                        "Single Biome fills the entire Age with one biome type. " +
                        "Specify which biome with a biome symbol. Creates very " +
                        "uniform worlds.")
                .addPage("Native Biomes",
                        "Native Biomes uses the natural biome distribution of " +
                        "the Overworld. Biomes blend naturally with appropriate " +
                        "climate transitions.")
                .addPage("Biome Sizes",
                        "Control biome scale with size modifiers:\n\n" +
                        "- Tiny: Very small patches\n" +
                        "- Small: Compact regions\n" +
                        "- Medium: Standard size\n" +
                        "- Large: Expansive areas\n" +
                        "- Huge: Continent-scale")
                .addPage("Tiled Biomes",
                        "Tiled Biomes arranges biomes in a repeating grid " +
                        "pattern. Each tile contains one biome. Creates very " +
                        "ordered, geometric worlds.")
                .addPage("Grid Biomes",
                        "Grid Biomes creates a checkerboard-like pattern of " +
                        "alternating biomes. Useful for organized farming " +
                        "or resource worlds.")
                .addPage("Specific Biomes",
                        "You can add specific biome symbols to your Age:\n\n" +
                        "Plains, Forest, Desert, Jungle, Taiga, Swamp, " +
                        "Ocean, Mountains, and many more. Each biome brings " +
                        "its own vegetation and creatures.");
    }

    private GuidebookChapter createCelestialsChapter() {
        return GuidebookChapter.create("Celestial Bodies")
                .addPage("The Sun",
                        "Every Age can have a sun. The Normal Sun provides " +
                        "standard daylight cycles. You can modify the sun's " +
                        "color, position, and behavior.")
                .addPage("Sun Variations",
                        "Sun options include:\n\n" +
                        "- Normal Sun: Standard behavior\n" +
                        "- Dark Sun: No light emission\n" +
                        "- Colored suns via modifiers")
                .addPage("The Moon",
                        "Moons provide nighttime illumination and affect " +
                        "various gameplay mechanics. Like suns, moons can " +
                        "be customized with colors and phases.")
                .addPage("Moon Phases",
                        "Moon phase modifiers:\n\n" +
                        "- Rising: Moon appears\n" +
                        "- Zenith: Full height\n" +
                        "- Setting: Moon descends\n" +
                        "- Nadir: Below horizon")
                .addPage("Stars",
                        "Stars fill the night sky. Options include:\n\n" +
                        "- Normal Stars: Standard sky\n" +
                        "- Twinkling Stars: Animated\n" +
                        "- End Stars: Strange patterns\n" +
                        "- No Stars: Empty night sky")
                .addPage("Celestial Colors",
                        "Apply color modifiers before celestial symbols:\n\n" +
                        "Red + Sun = Red sun\n" +
                        "Blue + Moon = Blue moon\n\n" +
                        "Creates dramatic atmospheric effects.")
                .addPage("Day/Night Cycle",
                        "Length modifiers affect cycle duration:\n\n" +
                        "- Zero Length: No movement\n" +
                        "- Half Length: Fast cycles\n" +
                        "- Full Length: Normal\n" +
                        "- Double Length: Slow cycles");
    }

    private GuidebookChapter createFeaturesChapter() {
        return GuidebookChapter.create("World Features")
                .addPage("Caves",
                        "The Caves symbol adds underground cave systems to " +
                        "your Age. Cave complexity varies but generally follows " +
                        "natural patterns with tunnels and chambers.")
                .addPage("Ravines",
                        "Ravines creates deep gorges that cut through the " +
                        "landscape. These dramatic features expose underground " +
                        "resources and create natural barriers.")
                .addPage("Floating Islands",
                        "Floating Islands generates landmasses suspended in " +
                        "the sky. Size and density vary. Works well with void " +
                        "terrain for skyworld Ages.")
                .addPage("Skylands",
                        "Skylands creates a world of massive floating land " +
                        "platforms at high altitude. More expansive than " +
                        "floating islands, suitable for large builds.")
                .addPage("Stone Spikes",
                        "Stone Spikes generates dramatic rocky pillars that " +
                        "rise from the ground. Creates otherworldly landscapes " +
                        "reminiscent of alien worlds.")
                .addPage("Spheres",
                        "Spheres creates spherical structures of terrain " +
                        "throughout the world. Can be hollow or solid, and " +
                        "creates very unusual geography.")
                .addPage("Tendrils",
                        "Tendrils generates winding, root-like formations that " +
                        "snake through the air and ground. Creates organic, " +
                        "flowing landscapes.")
                .addPage("Deep Lakes",
                        "Deep Lakes creates bodies of water that extend far " +
                        "below sea level. Good for underwater exploration " +
                        "and aquatic bases.")
                .addPage("Surface Lakes",
                        "Surface Lakes adds scattered lakes across the terrain " +
                        "at normal elevation. Creates a pleasant, varied " +
                        "landscape with water features.")
                .addPage("Dense Ores",
                        "Dense Ores increases the frequency of ore generation " +
                        "throughout the Age. Useful for resource gathering " +
                        "worlds, but may add instability.")
                .addPage("Huge Trees",
                        "Huge Trees generates massive tree structures that " +
                        "tower over the landscape. Creates ancient forest " +
                        "atmospheres and provides abundant wood.");
    }

    private GuidebookChapter createStructuresChapter() {
        return GuidebookChapter.create("Structures")
                .addPage("Villages",
                        "The Villages symbol causes villages to generate in " +
                        "appropriate biomes. Villagers will spawn and trading " +
                        "will be available as normal.")
                .addPage("Dungeons",
                        "Dungeons adds small underground rooms with monster " +
                        "spawners and loot chests. A good source of resources " +
                        "but increases danger.")
                .addPage("Mineshafts",
                        "Mineshafts creates abandoned mine networks underground. " +
                        "Contains rails, supports, and occasional loot. Can " +
                        "intersect with caves.")
                .addPage("Strongholds",
                        "Strongholds generates the large underground fortresses " +
                        "that normally house End Portals. In custom Ages, they " +
                        "provide exploration challenges.")
                .addPage("Nether Fortress",
                        "Nether Fortress creates the distinctive bridge and " +
                        "tower structures of the Nether. Blaze spawners and " +
                        "wither skeleton patrols included.")
                .addPage("Other Structures",
                        "Additional structures available:\n\n" +
                        "- Desert Temples\n" +
                        "- Jungle Temples\n" +
                        "- Witch Huts\n" +
                        "- Ocean Monuments\n" +
                        "- Woodland Mansions\n" +
                        "- And more...");
    }

    private GuidebookChapter createEnvironmentChapter() {
        return GuidebookChapter.create("Environment Effects")
                .addPage("Weather Control",
                        "Weather symbols control precipitation:\n\n" +
                        "- Normal Weather: Natural cycles\n" +
                        "- Clear Weather: Always sunny\n" +
                        "- Eternal Rain: Constant rain\n" +
                        "- Eternal Storm: Thunderstorms")
                .addPage("Lighting",
                        "Lighting symbols affect light levels:\n\n" +
                        "- Normal: Standard lighting\n" +
                        "- Bright: Enhanced light\n" +
                        "- Dark: Reduced light")
                .addPage("Accelerated Growth",
                        "Accelerated causes plants and crops to grow faster " +
                        "than normal. Useful for farming Ages but adds some " +
                        "instability to the dimension.")
                .addPage("Meteors",
                        "The Meteors symbol causes periodic meteor showers. " +
                        "Meteors can damage the landscape and are dangerous " +
                        "to players. Adds significant instability.")
                .addPage("Lightning Storms",
                        "Enhanced lightning activity beyond normal storms. " +
                        "Lightning strikes are more frequent and can be " +
                        "dangerous. Useful for charged creepers.")
                .addPage("Scorched Surface",
                        "Scorched creates a burned, devastated landscape. " +
                        "Fire spreads more easily and the terrain appears " +
                        "damaged. Very hostile environment.")
                .addPage("Explosions",
                        "Random explosions occur throughout the Age. Extremely " +
                        "dangerous and destructive. Only for those seeking " +
                        "challenge. Massive instability.");
    }

    private GuidebookChapter createModifiersChapter() {
        return GuidebookChapter.create("Modifiers")
                .addPage("Colors",
                        "Color modifiers change the appearance of targets:\n\n" +
                        "Red, Orange, Yellow, Green, Blue, Purple, White, " +
                        "Black, Cyan, Magenta, Lime, Pink, Gray, Brown...")
                .addPage("Color Targets",
                        "Colors can be applied to:\n\n" +
                        "- Sky Color: Daytime sky\n" +
                        "- Night Sky Color\n" +
                        "- Cloud Color\n" +
                        "- Fog Color\n" +
                        "- Grass Color\n" +
                        "- Foliage Color\n" +
                        "- Water Color")
                .addPage("Natural Colors",
                        "Each color target has a Natural variant that uses " +
                        "biome-appropriate colors. Natural Sky Color, for " +
                        "example, varies based on the biome.")
                .addPage("Directions",
                        "Direction modifiers for celestial bodies:\n\n" +
                        "- North, East, South, West\n" +
                        "- Rising, Zenith, Setting, Nadir\n\n" +
                        "Control where celestials appear in the sky.")
                .addPage("Lengths",
                        "Length modifiers affect duration:\n\n" +
                        "- Zero Length: Instant/none\n" +
                        "- Half Length: 50% duration\n" +
                        "- Full Length: Normal (100%)\n" +
                        "- Double Length: 200% duration")
                .addPage("Sea Types",
                        "Modify what fills the sea level:\n\n" +
                        "- Water Sea (default)\n" +
                        "- Lava Sea\n" +
                        "- Packed Ice Sea\n" +
                        "- Honey Sea\n" +
                        "- And other liquids...");
    }

    private GuidebookChapter createStabilityChapter() {
        return GuidebookChapter.create("Stability")
                .addPage("What is Stability?",
                        "Stability measures how well-written an Age is. " +
                        "Unstable Ages can have dangerous effects, while " +
                        "stable Ages are safe to inhabit long-term.")
                .addPage("Causes of Instability",
                        "Instability increases when:\n\n" +
                        "- Symbols are missing\n" +
                        "- Grammar is incorrect\n" +
                        "- Dangerous features added\n" +
                        "- Conflicting symbols used")
                .addStabilityPage("Stability Reference",
                        "Symbol stability effects:\n\n" +
                        "+Normal Terrain (stable)\n" +
                        "+Native Biomes (stable)\n" +
                        "-Void Terrain (unstable)\n" +
                        "-Dense Ores (unstable)\n" +
                        "-Meteors (very unstable)\n" +
                        "-Explosions (extreme)")
                .addPage("Instability Effects",
                        "Unstable Ages may experience:\n\n" +
                        "- Decay spreading\n" +
                        "- Spontaneous damage\n" +
                        "- Environmental hazards\n" +
                        "- Dimension collapse")
                .addPage("Writing Stable Ages",
                        "To maximize stability:\n\n" +
                        "1. Specify all major symbols\n" +
                        "2. Use correct grammar order\n" +
                        "3. Avoid dangerous features\n" +
                        "4. Include Link Panel first\n" +
                        "5. Test before building")
                .addPage("Star Fissure",
                        "The Star Fissure is a special feature that appears " +
                        "in some Ages. Falling into it returns you to the " +
                        "Overworld spawn point. It serves as an emergency exit.")
                .addPage("Final Advice",
                        "The Art of Writing takes practice. Start with simple " +
                        "Ages and gradually add complexity. Always carry a " +
                        "Linking Book home, and never delete your only return path!");
    }
}
