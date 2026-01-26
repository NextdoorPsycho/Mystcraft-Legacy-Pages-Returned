package art.arcane.mystcraft.guidebook;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all content for the Mystcraft Guidebook.
 * Written in the style of the Myst series - as an ancient tome
 * passed down from those who mastered the Art of Writing.
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
        return GuidebookChapter.create("The Art")
                .addPage("Foreword",
                        "If you are reading these words, then you have found " +
                        "what remains of our knowledge. Guard it well. The Art " +
                        "of Writing is not merely craft - it is the power to " +
                        "shape reality itself.")
                .addPage("The D'ni",
                        "We called ourselves the D'ni. For millennia, we lived " +
                        "in a great cavern beneath the earth, mastering the Art " +
                        "passed down from our ancestors. Through special books, " +
                        "we wrote links to countless Ages - worlds of our own " +
                        "description.")
                .addPage("The Linking",
                        "A properly written book does not create a world. " +
                        "It describes one that already exists, somewhere in the " +
                        "infinite possibilities. The book becomes a link, a " +
                        "window, a door. Touch the panel, and you are there.")
                .addPage("A Warning",
                        "I have seen what becomes of those who write carelessly. " +
                        "Ages that collapse. Worlds that decay. Writers trapped " +
                        "forever in their own flawed creations. Learn patience. " +
                        "Learn precision. The Art demands both.")
                .addPage("What Follows",
                        "In these pages, I have recorded what I know:\n\n" +
                        "- The making of ink and tools\n" +
                        "- The construction of Books\n" +
                        "- The grammar that binds all\n" +
                        "- The symbols and their effects\n\n" +
                        "Study well, and write wisely.");
    }

    private GuidebookChapter createInkAndMaterialsChapter() {
        return GuidebookChapter.create("Ink & Materials")
                .addPage("On Ink",
                        "The D'ni wrote with ink derived from a specific " +
                        "formula. In this world, a suitable substitute can be " +
                        "made from the sacs of squid, mixed with water. The " +
                        "resulting black ink carries the words between worlds.")
                .addPage("The Ink Mixer",
                        "I have constructed a device I call the Ink Mixer. " +
                        "Place ink sacs in the upper slot, ensure water is " +
                        "available, and the mixer will produce ink far more " +
                        "efficiently than crude methods allow.")
                .addPage("Ink Vials",
                        "Ink must be stored in glass vials for use at the " +
                        "Writing Desk. Craft empty vials from glass, then " +
                        "fill them at the Ink Mixer. A full vial provides " +
                        "enough ink for several pages.")
                .addPage("Colored Inks",
                        "The D'ni rarely used colored inks, but I have found " +
                        "they have their uses. Add dyes to the Ink Mixer to " +
                        "create colored variants. These affect certain aesthetic " +
                        "properties of an Age.")
                .addPage("Paper & Leather",
                        "Symbol pages require paper - the common variety made " +
                        "from reeds suffices. Book covers require leather. " +
                        "I recommend maintaining a healthy supply of both, " +
                        "as the Art consumes much material.");
    }

    private GuidebookChapter createWritingToolsChapter() {
        return GuidebookChapter.create("The Desk")
                .addPage("The Writing Desk",
                        "The Writing Desk is where symbols are inscribed onto " +
                        "pages. It is a precise instrument. Place your ink vial " +
                        "in the left receptacle, blank paper in the tray, and " +
                        "the symbol you wish to copy in the central slot.")
                .addPage("Copying Symbols",
                        "To copy a symbol:\n\n" +
                        "1. Insert a filled ink vial\n" +
                        "2. Place blank paper\n" +
                        "3. Insert the source symbol page\n" +
                        "4. Activate the desk\n\n" +
                        "The symbol transfers to the blank page.")
                .addPage("Ink Consumption",
                        "Each copy consumes ink. Complex symbols require more. " +
                        "Watch your vial's level - running dry mid-copy wastes " +
                        "the page. I keep several filled vials ready when " +
                        "preparing for extensive writing.")
                .addPage("The Book Binder",
                        "The Book Binder assembles pages into complete Books. " +
                        "Without it, your symbols are merely potential. The " +
                        "Binder weaves them into a coherent description, " +
                        "transforming scattered words into a doorway.")
                .addPage("Binding Process",
                        "To bind a Descriptive Book:\n\n" +
                        "1. Place a Link Panel page first\n" +
                        "2. Add symbol pages in order\n" +
                        "3. Insert a leather cover\n" +
                        "4. Activate the binding\n\n" +
                        "The order of symbols matters greatly.");
    }

    private GuidebookChapter createBooksChapter() {
        return GuidebookChapter.create("Books")
                .addPage("Descriptive Books",
                        "A Descriptive Book contains the full description of " +
                        "an Age. When you touch its Link Panel for the first " +
                        "time, the link is established. The Age described " +
                        "becomes reachable. The same book always links to " +
                        "the same Age.")
                .addPage("Writing a Book",
                        "A Descriptive Book requires at minimum a Link Panel. " +
                        "This alone will create an Age, but its properties " +
                        "will be random - often unstable. Add symbols to " +
                        "specify terrain, biomes, sky, and features.")
                .addPage("Linking Books",
                        "Linking Books are simpler. They do not describe Ages - " +
                        "they link to specific locations. Craft an unlinked " +
                        "book, then use it while standing where you wish to " +
                        "create the link. It binds to that exact spot.")
                .addPage("The Return Path",
                        "This is the most critical lesson I can teach:\n\n" +
                        "ALWAYS CARRY A LINKING BOOK HOME.\n\n" +
                        "Descriptive Books do not follow you. If you link to " +
                        "an Age without a return book, you are trapped there.")
                .addPage("I knew a writer once...",
                        "...who grew careless. He linked to test an Age, " +
                        "forgetting his return book on the desk. The Age was " +
                        "unstable. It collapsed within days. We never found " +
                        "a way to reach him. Do not share his fate.")
                .addPage("Link Modifiers",
                        "Linking Books can be enhanced at the Link Modifier:\n\n" +
                        "- Intra-Linking: Link within same world\n" +
                        "- Following: Bring nearby entities\n" +
                        "- Disarm: Strip held items\n" +
                        "- Momentum: Preserve velocity");
    }

    private GuidebookChapter createContainersChapter() {
        return GuidebookChapter.create("Storage")
                .addPage("The Folder",
                        "As your collection of symbols grows, organization " +
                        "becomes essential. The Collation Folder holds many " +
                        "pages in a portable form. Right-click to access its " +
                        "contents.")
                .addPage("The Portfolio",
                        "For serious practitioners, the Symbol Portfolio " +
                        "provides superior organization. It sorts pages " +
                        "automatically by type - terrain, biome, celestial, " +
                        "and so forth.")
                .addPage("Sealed Notebooks",
                        "In my explorations, I have found sealed notebooks " +
                        "in ancient ruins and libraries. They contain random " +
                        "symbol pages - remnants of D'ni knowledge. Open them " +
                        "carefully; some symbols are rare indeed.")
                .addPage("Acquiring Symbols",
                        "Symbols can be obtained by:\n\n" +
                        "- Finding sealed notebooks in dungeons\n" +
                        "- Trading with certain villagers\n" +
                        "- Copying from existing pages\n" +
                        "- Exploring abandoned Ages\n\n" +
                        "Build your collection over time.");
    }

    private GuidebookChapter createBlocksChapter() {
        return GuidebookChapter.create("Furnishings")
                .addPage("Bookstands",
                        "A Bookstand displays a book openly, allowing anyone " +
                        "to use its link without retrieving it from storage. " +
                        "Place a book, then simply touch the stand to link. " +
                        "Useful for frequently-visited Ages.")
                .addPage("The Lectern",
                        "The Lectern serves the same purpose as a Bookstand " +
                        "but with a different appearance. Both protect books " +
                        "from casual disturbance while keeping them accessible.")
                .addPage("Book Receptacles",
                        "For more secure storage, the Book Receptacle holds " +
                        "a book firmly in place. The book cannot be accidentally " +
                        "removed. Use these for critical links that must not " +
                        "be disturbed.")
                .addPage("The Link Modifier",
                        "The Link Modifier is a specialized station for " +
                        "enhancing Linking Books. Place a book within, add " +
                        "the appropriate reagents, and select the modification " +
                        "you wish to apply.")
                .addPage("Crystal Blocks",
                        "In some Ages, I have found crystalline formations " +
                        "that emit a soft glow. These crystals can be harvested " +
                        "and placed as light sources. They remind me of the " +
                        "great lamps of D'ni.");
    }

    private GuidebookChapter createGrammarChapter() {
        return GuidebookChapter.create("Grammar")
                .addPage("The Rules",
                        "The D'ni language follows strict rules. When writing " +
                        "an Age, symbols must be arranged in proper order. " +
                        "Violate the grammar, and the Age becomes unstable - " +
                        "or worse, contradictory.")
                .addPage("Modifiers First",
                        "The fundamental pattern is:\n\n" +
                        "MODIFIER then TARGET\n\n" +
                        "A modifier describes a property. A target receives it. " +
                        "'Red' modifies 'Sun' to create a red sun. The modifier " +
                        "must come before what it modifies.")
                .addPage("Symbol Types",
                        "Symbols fall into categories:\n\n" +
                        "Terrain - The shape of land\n" +
                        "Biome - Climate and life\n" +
                        "Celestial - Sun, moon, stars\n" +
                        "Feature - Caves, lakes, etc.\n" +
                        "Modifier - Colors, lengths\n" +
                        "Environment - Weather, light")
                .addPage("Modifier Order",
                        "When multiple modifiers apply:\n\n" +
                        "1. Color modifiers\n" +
                        "2. Direction modifiers\n" +
                        "3. Phase modifiers\n" +
                        "4. Length modifiers\n" +
                        "5. Target symbol\n\n" +
                        "Maintain this sequence.")
                .addPage("Clear Modifiers",
                        "The 'Clear Modifiers' symbol resets all pending " +
                        "modifiers. Use it when you have applied modifiers " +
                        "but wish to start fresh before the next target. " +
                        "It prevents accidental carry-over.")
                .addPage("Omissions",
                        "If you do not specify something, the link will fill " +
                        "it randomly. An Age with no terrain symbol receives " +
                        "random terrain. This randomness breeds instability. " +
                        "Specify everything you can.");
    }

    private GuidebookChapter createTerrainChapter() {
        return GuidebookChapter.create("Terrain")
                .addPage("Shaping Land",
                        "Terrain symbols define the fundamental shape of an " +
                        "Age. They determine whether the land is flat or " +
                        "mountainous, solid or void. Choose carefully - " +
                        "terrain affects everything built upon it.")
                .addPage("Standard Terrain",
                        "Standard terrain generates rolling hills, mountains, " +
                        "and valleys much like the surface world. It is stable " +
                        "and familiar. Most Ages should use this unless you " +
                        "have specific needs.")
                .addPage("Flat Terrain",
                        "Flat terrain produces a level surface extending to " +
                        "the horizon. No hills, no valleys. Useful for " +
                        "construction projects, but can feel monotonous. " +
                        "The emptiness is stable, at least.")
                .addPage("Void Terrain",
                        "Void terrain generates... nothing. An empty expanse. " +
                        "Only specific features like floating islands will " +
                        "appear. Stepping off solid ground means falling " +
                        "forever. Use with extreme caution.")
                .addPage("Amplified",
                        "Amplified terrain exaggerates everything. Mountains " +
                        "become towering spires. Valleys plunge to bedrock. " +
                        "Dramatic and dangerous. Navigation requires care.")
                .addPage("Nether Style",
                        "This terrain generates enclosed caverns filled with " +
                        "lava seas and harsh stone - the structure of the " +
                        "Nether realm. Hostile, but the resources can be " +
                        "valuable.")
                .addPage("End Style",
                        "End-style terrain creates floating islands over an " +
                        "infinite void, like the realm of the Ender Dragon. " +
                        "Falling is fatal. Building bridges becomes necessary.")
                .addPage("Terrain Blocks",
                        "You can specify what the terrain is made of:\n\n" +
                        "Stone, Deepslate, Sandstone, Netherrack, End Stone, " +
                        "and others. The block affects both appearance and " +
                        "what resources are available.");
    }

    private GuidebookChapter createBiomesChapter() {
        return GuidebookChapter.create("Biomes")
                .addPage("Life & Climate",
                        "Biomes determine the climate, vegetation, and " +
                        "creatures of an Age. A desert biome brings sand and " +
                        "heat. A forest brings trees and shade. Choose biomes " +
                        "that suit your purpose.")
                .addPage("Controllers",
                        "Biome Controllers determine how biomes are " +
                        "distributed across the land:\n\n" +
                        "Single - One biome everywhere\n" +
                        "Native - Natural distribution\n" +
                        "Tiled - Grid pattern\n" +
                        "Checkerboard - Alternating")
                .addPage("Single Biome",
                        "The Single Biome controller fills the entire Age " +
                        "with one biome type. Specify which with a biome " +
                        "symbol. The result is uniform but predictable. " +
                        "Good for resource gathering.")
                .addPage("Native Distribution",
                        "Native distribution mimics the natural world. " +
                        "Biomes blend into one another based on climate. " +
                        "Deserts transition to plains, plains to forest. " +
                        "The most stable approach.")
                .addPage("Biome Size",
                        "Size modifiers affect biome scale:\n\n" +
                        "Tiny - Small patches\n" +
                        "Small - Compact regions\n" +
                        "Medium - Standard (default)\n" +
                        "Large - Expansive areas\n" +
                        "Huge - Continent-scale")
                .addPage("Specific Biomes",
                        "You can specify exact biomes:\n\n" +
                        "Plains, Forest, Desert, Jungle, Taiga, Swamp, " +
                        "Savanna, Badlands, Ocean, Mountains, Tundra...\n\n" +
                        "Each brings its own character.");
    }

    private GuidebookChapter createCelestialsChapter() {
        return GuidebookChapter.create("Celestials")
                .addPage("The Heavens",
                        "The D'ni took great care in describing the skies " +
                        "of their Ages. A sun provides light and warmth. " +
                        "Moons illuminate the night. Stars fill the darkness. " +
                        "Without them, an Age feels incomplete.")
                .addPage("Suns",
                        "The Normal Sun behaves as expected - rising in the " +
                        "east, setting in the west, providing daylight. " +
                        "You can have multiple suns, or none at all. " +
                        "An Age without sun is eternally dark.")
                .addPage("Sun Properties",
                        "Suns can be modified:\n\n" +
                        "Color - Red, blue, any hue\n" +
                        "Position - Fixed or moving\n" +
                        "Dark Sun - Visible but no light\n\n" +
                        "A dark sun creates an eerie twilight.")
                .addPage("Moons",
                        "Moons provide softer light during night hours. " +
                        "Like suns, they can be colored or positioned. " +
                        "Multiple moons create complex lighting. " +
                        "Phases can be set to full, half, or crescent.")
                .addPage("Stars",
                        "Stars fill the night sky:\n\n" +
                        "Normal Stars - Familiar patterns\n" +
                        "Twinkling - Animated shimmer\n" +
                        "End Stars - Strange, alien\n" +
                        "None - Empty black void\n\n" +
                        "The absence of stars is unsettling.")
                .addPage("Colors",
                        "Apply color modifiers before celestial symbols:\n\n" +
                        "Red + Sun = crimson daylight\n" +
                        "Blue + Moon = cold blue nights\n" +
                        "Green + Stars = emerald sky\n\n" +
                        "The effect is striking.")
                .addPage("Day Length",
                        "Length modifiers affect the day cycle:\n\n" +
                        "Zero Length - Frozen in time\n" +
                        "Half Length - Rapid cycles\n" +
                        "Full Length - Normal (default)\n" +
                        "Double Length - Long days\n\n" +
                        "Eternal day or night is possible.");
    }

    private GuidebookChapter createFeaturesChapter() {
        return GuidebookChapter.create("Features")
                .addPage("World Details",
                        "Features add specific elements to the terrain - " +
                        "caves beneath the surface, lakes across the land, " +
                        "formations that break the monotony. They make an " +
                        "Age feel complete.")
                .addPage("Caves",
                        "The Caves symbol generates underground tunnel systems. " +
                        "They wind through the earth, sometimes opening into " +
                        "vast chambers. Resources hide within. Darkness waits.")
                .addPage("Ravines",
                        "Ravines are great gashes in the earth - deep gorges " +
                        "that expose stone layers. Useful for finding ores " +
                        "without mining. Dangerous for the unwary.")
                .addPage("Floating Islands",
                        "Floating Islands suspend landmasses in the sky. " +
                        "They drift above the ground, accessible only by " +
                        "building or flying. Combined with void terrain, " +
                        "they create sky realms.")
                .addPage("Skylands",
                        "Skylands are larger than floating islands - vast " +
                        "platforms at high altitude. An entire civilization " +
                        "could exist atop a skyland. Falling remains fatal.")
                .addPage("Stone Formations",
                        "Various stone formations can populate an Age:\n\n" +
                        "Stone Spikes - Towering pillars\n" +
                        "Spheres - Rounded masses\n" +
                        "Tendrils - Winding roots\n\n" +
                        "Each creates alien vistas.")
                .addPage("Water Features",
                        "Lakes and bodies of water:\n\n" +
                        "Surface Lakes - Scattered ponds\n" +
                        "Deep Lakes - Underground pools\n" +
                        "Ocean - Vast water expanses\n\n" +
                        "Water makes an Age feel alive.")
                .addPage("Resource Features",
                        "Some features affect resources:\n\n" +
                        "Dense Ores - More mineral deposits\n" +
                        "Huge Trees - Massive timber\n" +
                        "Crystal Formation - Glowing minerals\n\n" +
                        "Greed can compromise stability.");
    }

    private GuidebookChapter createStructuresChapter() {
        return GuidebookChapter.create("Structures")
                .addPage("Built Things",
                        "Structures are pre-built constructions that appear " +
                        "in an Age. Villages, dungeons, temples - remnants " +
                        "of civilizations that exist wherever these Ages " +
                        "connect. Or perhaps echoes of what could be.")
                .addPage("Villages",
                        "Villages spawn in appropriate biomes, populated by " +
                        "villagers who trade goods and services. They are " +
                        "a sign of life, of civilization. Peaceful, usually.")
                .addPage("Dungeons",
                        "Dungeons are small underground chambers containing " +
                        "a spawner and chests. The monsters within guard " +
                        "treasures. Useful for resources, dangerous for " +
                        "the unprepared.")
                .addPage("Mineshafts",
                        "Abandoned mineshafts wind through the underground - " +
                        "wooden supports, rails, forgotten chests. Someone " +
                        "was here before. The webs suggest they left quickly.")
                .addPage("Strongholds",
                        "Strongholds are ancient fortresses buried deep " +
                        "underground. In the normal world, they hold portals " +
                        "to the End. In written Ages, they hold mysteries " +
                        "and challenges.")
                .addPage("Other Structures",
                        "Many structures can appear:\n\n" +
                        "Desert Temples\n" +
                        "Jungle Temples\n" +
                        "Witch Huts\n" +
                        "Ocean Monuments\n" +
                        "Woodland Mansions\n" +
                        "Nether Fortresses");
    }

    private GuidebookChapter createEnvironmentChapter() {
        return GuidebookChapter.create("Environment")
                .addPage("Conditions",
                        "Environmental symbols affect the conditions within " +
                        "an Age - weather, lighting, growth rates. These " +
                        "determine whether an Age is pleasant to inhabit " +
                        "or actively hostile.")
                .addPage("Weather",
                        "Weather can be controlled:\n\n" +
                        "Normal Weather - Natural cycles\n" +
                        "Always Clear - Eternal sun\n" +
                        "Always Rain - Constant downpour\n" +
                        "Always Storm - Thunder never ends")
                .addPage("Lighting",
                        "Lighting affects visibility:\n\n" +
                        "Normal - Standard light levels\n" +
                        "Bright - Enhanced illumination\n" +
                        "Dark - Reduced light\n\n" +
                        "A dark Age is dangerous.")
                .addPage("Accelerated",
                        "The Accelerated symbol speeds plant growth. Crops " +
                        "mature faster, trees grow quickly. Useful for " +
                        "farming Ages. Carries some instability - growth " +
                        "without rest strains reality.")
                .addPage("Hostile Effects",
                        "Some effects are deliberately dangerous:\n\n" +
                        "Meteors - Fiery bombardment\n" +
                        "Lightning - Frequent strikes\n" +
                        "Scorched - Burned landscape\n" +
                        "Explosions - Random destruction")
                .addPage("A Confession",
                        "I once wrote an Age with meteors, thinking the " +
                        "spectacle would be beautiful. It was. It was also " +
                        "deadly. My home there burned. I barely escaped. " +
                        "Beauty and danger often coincide.");
    }

    private GuidebookChapter createModifiersChapter() {
        return GuidebookChapter.create("Modifiers")
                .addPage("Changing Properties",
                        "Modifiers alter the properties of other symbols. " +
                        "They must be placed BEFORE their target in the " +
                        "book. A modifier without a target does nothing. " +
                        "A target without a modifier uses defaults.")
                .addPage("Colors",
                        "Color modifiers are the most common:\n\n" +
                        "Red, Orange, Yellow, Green, Blue, Purple, " +
                        "White, Black, Cyan, Magenta, Lime, Pink, " +
                        "Gray, Brown\n\n" +
                        "Apply before sky, celestials, or foliage.")
                .addPage("Color Targets",
                        "Colors can modify:\n\n" +
                        "Sky Color - Daytime sky\n" +
                        "Night Sky - Nighttime sky\n" +
                        "Cloud Color\n" +
                        "Fog Color\n" +
                        "Grass Color\n" +
                        "Foliage Color\n" +
                        "Water Color")
                .addPage("Natural Colors",
                        "Each color target has a 'Natural' variant that " +
                        "uses biome-appropriate hues. Natural Sky Color " +
                        "varies by biome. Natural is usually more stable " +
                        "than forcing a specific color.")
                .addPage("Directions",
                        "Direction modifiers for celestials:\n\n" +
                        "North, East, South, West\n" +
                        "Rising, Zenith, Setting, Nadir\n\n" +
                        "These set where celestial bodies appear and " +
                        "how they move across the sky.")
                .addPage("Lengths",
                        "Length modifiers affect duration:\n\n" +
                        "Zero - Instant or frozen\n" +
                        "Half - 50% normal\n" +
                        "Full - Normal (default)\n" +
                        "Double - 200% normal\n\n" +
                        "Applies to day cycles primarily.")
                .addPage("Sea Level",
                        "The sea can be filled with:\n\n" +
                        "Water Sea (default)\n" +
                        "Lava Sea\n" +
                        "Packed Ice Sea\n" +
                        "Honey Sea\n\n" +
                        "A lava sea is spectacular and deadly.");
    }

    private GuidebookChapter createStabilityChapter() {
        return GuidebookChapter.create("Stability")
                .addPage("The Weight of Words",
                        "Every Age exists in tension. The description pulls " +
                        "at reality, shaping it into the form you wrote. " +
                        "If the description is clear and complete, the Age " +
                        "holds firm. If flawed, it strains. It cracks. It falls.")
                .addPage("What Causes Decay",
                        "Instability increases when:\n\n" +
                        "- Symbols are omitted (randomness fills gaps)\n" +
                        "- Grammar is violated\n" +
                        "- Dangerous features are added\n" +
                        "- Symbols contradict each other\n" +
                        "- Too much is demanded")
                .addStabilityPage("Symbol Effects",
                        "Some symbols stabilize, others destabilize:\n\n" +
                        "+Standard Terrain (stable)\n" +
                        "+Native Biomes (stable)\n" +
                        "+Normal Weather (stable)\n" +
                        "-Void Terrain (unstable)\n" +
                        "-Dense Ores (unstable)\n" +
                        "-Meteors (very unstable)\n" +
                        "-Explosions (catastrophic)")
                .addPage("Signs of Decay",
                        "An unstable Age shows symptoms:\n\n" +
                        "- Decay spreading across blocks\n" +
                        "- Spontaneous damage to entities\n" +
                        "- Environmental hazards appearing\n" +
                        "- In extreme cases, collapse\n\n" +
                        "Leave before it is too late.")
                .addPage("Writing Stable Ages",
                        "To maximize stability:\n\n" +
                        "1. Always begin with Link Panel\n" +
                        "2. Specify terrain explicitly\n" +
                        "3. Specify biome controller\n" +
                        "4. Include sun and sky\n" +
                        "5. Follow grammar rules\n" +
                        "6. Avoid greedy features")
                .addPage("The Star Fissure",
                        "Some Ages contain a Star Fissure - a rift in the " +
                        "ground that falls endlessly into starlight. If you " +
                        "fall in, you return to the surface world's spawn. " +
                        "It is an escape route of last resort.")
                .addPage("Final Words",
                        "The Art is vast. These pages hold only the beginning. " +
                        "Practice with simple Ages. Learn the symbols. Respect " +
                        "the grammar. Always carry a return book.\n\n" +
                        "The D'ni built wonders with this Art. And with hubris, " +
                        "they fell. Write with wisdom, not arrogance.\n\n" +
                        "May your Ages endure.");
    }
}
