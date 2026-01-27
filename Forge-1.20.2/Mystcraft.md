# Mystcraft 2 - Pages Unturned: The Comprehensive Guide

Mystcraft is a Minecraft Forge mod for 1.20.2 that brings the Myst series to Minecraft, allowing players to write and travel to custom dimensions called Ages. Each Age is procedurally generated from a set of symbols written into Descriptive Books, with instability effects that punish poorly-written or overpowered Ages.

- **Minecraft**: 1.20.2
- **Forge**: 48.1.0
- **Java**: 17
- **License**: LGPLv3
- **Author**: NextdoorPsycho (originally created by XCompWiz)

---

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Items](#items)
3. [Blocks and Workstations](#blocks-and-workstations)
4. [Entities](#entities)
5. [The Symbol System](#the-symbol-system)
6. [Symbol Catalog](#symbol-catalog)
7. [The Grammar Engine](#the-grammar-engine)
8. [World Generation](#world-generation)
9. [Celestial Bodies](#celestial-bodies)
10. [Weather and Lighting](#weather-and-lighting)
11. [The Instability System](#the-instability-system)
12. [Linking and Portals](#linking-and-portals)
13. [The Archivist Villager](#the-archivist-villager)
14. [Structures and Loot](#structures-and-loot)
15. [Advancements](#advancements)
16. [Client Rendering](#client-rendering)
17. [Commands](#commands)
18. [Configuration](#configuration)
19. [Age Presets](#age-presets)
20. [Ink and Writing](#ink-and-writing)
21. [Networking](#networking)
22. [Event Handlers](#event-handlers)

---

## Core Concepts

**Ages** are custom dimensions. Each Age is defined by a set of **symbols** written onto **pages**, which are bound into a **Descriptive Book** (Agebook). When a player uses a Descriptive Book for the first time, the mod's grammar engine fills in any gaps, calculates instability, and creates a new dimension.

**Symbols** are the building blocks of Age design. They define terrain type, biome distribution, weather, lighting, celestial bodies, colors, features, structures, and environmental effects. Each symbol has an **instability cost** -- the total instability determines how dangerous the Age is.

**Instability** is the balancing mechanism. Ages with too many powerful features, missing required components, or conflicting symbols accumulate instability. High instability triggers environmental hazards: decay, explosions, meteors, lightning, potion effects, and ore degradation.

**Linking** is how players travel between dimensions. Descriptive Books create new Ages; Linkbooks provide return trips to specific coordinates. Both can be placed in Book Receptacles to create portal structures, or used directly from inventory or bookstands.

---

## Items

### Descriptive Book (Agebook)

The core item for creating Ages. Contains bound symbol pages that define the Age's properties.

- **Stack size**: 1
- **Right-click**: Opens the book GUI showing pages and Age info
- **First use**: Creates the Age dimension, runs the grammar engine, calculates instability
- **Display**: Shows Age number, author list, page count, instability rating
- **Enchantment glint**: Appears when linked to an existing Age
- **Drop behavior**: Renders as an open LinkbookEntity on the ground when Q-dropped

### Linkbook

A book linked to a specific location in a specific dimension. Allows return travel.

- **Stack size**: 1
- **Auto-initialization**: Records current position on first inventory tick
- **Health system**: 10.0 max health, degrades from fire (5/tick), drowning (20/tick), collision (10 per speed unit), starvation (1 per 10000 ticks), explosions (3x multiplier)
- **Following flag**: Optional -- keeps the book with the player after linking instead of dropping it
- **Enchantment glint**: Shows when "following" flag is active

### Unlinked Linkbook

A blank linkbook that converts to a linked Linkbook on right-click at the player's current position.

- **Stack size**: 16
- **Conversion**: Consumes one, creates a Linkbook recording current position

### Page

Symbol pages are the writing medium for Age creation.

- **Stack size**: 64
- **Types**: Blank page, Link Panel (special marker), Symbol Page (specific symbol written)
- **Enchantment glint**: Link Panels glow
- **Creation**: Written at the Writing Desk with ink

### Ink Vial

Holds colored ink for the Writing Desk.

- **Stack size**: 16
- **Properties**: Color (RGB hex) and amount (0-100)
- **Progress bar**: Visual indicator of ink level
- **Default**: Black ink (0x000000)

### Booster Pack

A one-time-use pack that generates random symbol pages.

- **Stack size**: 16
- **Right-click**: Opens and generates 3 random pages (20% link panel, 80% random symbol)
- **Pages**: Added to inventory or dropped if full

### Glasses

Equipment item that reveals hidden Age information.

- **Slot**: Head armor slot
- **Stack size**: 1
- **Function**: Provides overlay showing Age symbol data, instability rating, and symbol category info

### Guidebook ("The Art of Writing")

In-game manual and tutorial.

- **Stack size**: 1
- **Right-click**: Opens tutorial screen explaining ink creation, page writing, crafting, grammar
- **Auto-given**: New players receive one on first login (configurable)

### Folder (Collation Folder)

Ordered, writable page container.

- **Stack size**: 32 when empty, 1 when containing pages
- **Capacity**: 16 pages
- **Ordered**: Pages have fixed slot positions
- **Writable**: Can write symbols directly to blank pages inside
- **GUI**: Right-click opens FolderMenu for page management

### Portfolio (Symbol Portfolio)

Unordered page archive for collection and organization.

- **Stack size**: 1 (always)
- **Capacity**: 64 pages
- **Unordered**: Pages are a searchable, sortable collection
- **Not writable**: Cannot write symbols directly
- **Sorting**: Auto-sorts by category and name
- **Statistics**: Methods for counting link panels, blank pages, symbols

### Ink Bucket

Bucket containing black ink fluid.

- **Stack size**: 1
- **Craft remainder**: Returns empty vanilla bucket

---

## Blocks and Workstations

### Writing Desk

Multi-block (2x2x2) crafting station for writing symbols onto pages.

- **Material**: Wood (hardness 2.5)
- **Structure**: Main block + foot block + two top blocks
- **Function**: Accepts ink vial, blank page, and dyes to produce symbol pages
- **GUI**: WritingDeskMenu with ink, page, dye, and output slots
- **Redstone**: Signal based on ink amount

### Book Binder

Crafting station for binding pages into books.

- **Material**: Wood (hardness 2.5)
- **Function**: Accepts pages and leather/cover materials to create Agebooks or Linkbooks
- **Requirements**: At least 1 link panel as first page, cover material
- **GUI**: BookBinderMenu with 16 page slots + cover slot
- **Import**: Can import directly from Folder or Portfolio items
- **Redstone**: Signal based on page count (1-15)

### Ink Mixer

Crafting station for mixing ink colors.

- **Material**: Wood (hardness 2.5)
- **Function**: Mixes black ink with dyes to create colored inks, creates link panels with properties
- **GUI**: InkMixerMenu with ink bucket slot, dye slots, output slot
- **Redstone**: 15 when ink present, 0 otherwise

### Link Modifier

Modifies link properties on books.

- **Material**: Stone (hardness 3.0)
- **Function**: Applies flags to links (following, disarm, generate platform, etc.)
- **GUI**: LinkModifierMenu with flag toggle options

### Book Receptacle

Holds a book and creates a portal structure when powered by crystal blocks.

- **Material**: Stone (hardness 3.5)
- **Placement**: Must be placed on a Crystal Block
- **Function**: Insert a linked book to activate the portal, retrieve to deactivate
- **Redstone**: 15 when book present
- **Portal**: Fires portal via PortalUtils when book inserted

### Bookstand

Display pedestal for books.

- **Material**: Wood (hardness 2.0)
- **Function**: Right-click to open book GUI; shift+empty-hand to pick up book
- **Rendering**: Custom block entity renderer (open book model)
- **Redstone**: Signal based on book presence

### Lectern

Alternative display stand for books, doubles as the Archivist villager's workstation.

- **Material**: Wood (hardness 2.0)
- **POI**: Registered as Archivist point-of-interest block

### Crystal Block

Frame block for portal structures.

- **Properties**: ACTIVE (boolean), SOURCE_DIRECTION (facing toward receptacle)
- **Function**: Becomes part of portal when activated by a receptacle
- **Redstone**: 15 when active, 0 when inactive
- **Light**: Configurable (0-15), dynamic based on activation state

### Link Portal Block

The actual teleportation surface within a portal structure.

- **Properties**: ACTIVE (boolean), SOURCE_DIRECTION (toward receptacle)
- **Light level**: 11
- **No collision**: Entities walk through it
- **Cooldown**: 100 ticks (5 seconds) between teleports per entity
- **Mechanics**: Finds receptacle via PortalUtils, reads book data, performs linking with flags

### Decay Block

Instability corruption block that spreads and damages.

- **Unbreakable** by default
- **5 Decay Types**:
  - **Blue**: Non-spreading, non-damaging (aesthetic)
  - **Purple**: Spreading, moderate damage (3 HP)
  - **Red**: Spreading, high damage (4 HP)
  - **White**: Spreading, non-damaging (visual drama)
  - **Black**: Most destructive -- spreads, damages (2 HP), special falling behavior, removes fluids
- **Spread**: Random tick causes adjacent solid blocks to convert to decay
- **Contact damage**: Entities take damage from stepping on or touching decay

### Star Fissure Block

Dimensional rift that serves as an exit portal from Ages.

- **Light level**: 15
- **No collision**: Walk-through
- **Function**: Teleports entities to overworld spawn (or player respawn point)
- **Rendering**: Custom block entity renderer (sky portal effect)
- **Sound**: Plays fissure linking sound on departure/arrival

### Ink Fluid Block

Black ink as a placeable fluid.

- **Slope distance**: 2
- **Level decrease**: 2 per block
- **Color**: Black
- **Bucket support**: Fill/empty with Ink Bucket

---

## Entities

### Linkbook Entity

Dropped or placed books that can be interacted with in the world.

- **Size**: 0.5x0.5
- **Health system**: Max 100 HP
- **Damage sources**: Fire (5/tick), drowning (20/tick), collision (10 per speed), starvation (1 per 10000 ticks), explosions (3x)
- **Interactions**: Right-click to open GUI, shift+click to pick up
- **Hopper support**: Books can be extracted/inserted via hoppers
- **Rendering**: Custom open book model with hurt flash

### Meteor Entity

Falling projectile spawned by instability effects or commands.

- **Size**: 2x2
- **Fire immune**
- **Behavior**: Falls from sky, impacts terrain with explosion
- **Sounds**: Roar on approach, impact on landing
- **Scale**: Configurable (1-3x via instability or command)

### Colored Lightning Entity

Visual lightning bolt with customizable color.

- **No save/summon**: Transient visual effect
- **Purpose**: Instability visual effect, colored per-Age
- **Damage**: Optional (visual-only at low instability, damaging at high)

### Falling Block Entity

Custom falling block for decay physics.

- **Size**: 0.98x0.98
- **Purpose**: Black decay blocks that fall

### Dummy Entity

Utility/placeholder entity.

- **No tracking, no save, no summon**: Pure utility

---

## The Symbol System

Symbols are the fundamental building blocks of Age creation. Every aspect of an Age -- terrain, biomes, weather, lighting, colors, features, structures, environmental effects -- is controlled by symbols.

### Symbol Properties

Every symbol has:

- **ID**: Registry name (e.g., `mystcraft:terrain_normal`)
- **Display Name**: Human-readable name
- **Category**: Determines grammar role and stacking behavior (e.g., TERRAIN, BIOME, WEATHER)
- **Rank**: Rarity tier (0-4). Higher rank = rarer, more expensive from Archivists
- **Instability Cost**: Contribution to Age instability. Negative = stabilizing, positive = destabilizing
- **Poem Words**: Four D'ni-inspired words used in the grammar system
- **Allowed in Random**: Whether the symbol appears in random generation

### Symbol Categories

| Category | Singleton? | Description |
|---|---|---|
| TERRAIN | Yes | Base terrain generation algorithm |
| BIOME_CONTROLLER | Yes | How biomes are distributed |
| BIOME | No | Specific biome to include |
| WEATHER | Yes | Weather behavior |
| LIGHTING | Yes | Brightness/darkness model |
| SUN | No | Sun appearance and behavior |
| MOON | No | Moon appearance and behavior |
| STARS | No | Star field appearance |
| FEATURE_LARGE | No | Major terrain features (caves, ravines, floating islands) |
| FEATURE_MEDIUM | No | Medium features (dense ores, spikes, spheres) |
| FEATURE_SMALL | No | Minor features (surface lakes, dripstone) |
| STRUCTURE | No | Generated structures (villages, dungeons) |
| VISUAL_EFFECT | No | Color targets (sky, fog, grass, water colors) |
| ENVIRONMENT | No | Environmental effects (meteors, lightning, accelerated) |
| SEA | Yes | Sea/ocean fluid type |
| COLOR | No (stackable) | Color modifier values |
| ANGLE | No (stackable) | Directional modifiers |
| PHASE | No (stackable) | Orbital phase modifiers |
| LENGTH | No (stackable) | Length/scale modifiers |
| MODIFIER | No | General modifiers (clouds, horizon, gradients) |
| SPECIAL | No | Unique effects (star fissure, anti-PvP, rainbow) |

---

## Symbol Catalog

### Terrain Symbols (Category: TERRAIN)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `terrain_normal` | Normal Terrain | 0.0 | Standard overworld-like procedural terrain with noise-based heights |
| `terrain_amplified` | Amplified Terrain | 5.0 | Exaggerated heights (2x normal scale) |
| `terrain_flat` | Flat Terrain | 0.0 | Flat world with configurable ground level |
| `terrain_void` | Void Terrain | 10.0 | Empty void with single bedrock layer |
| `terrain_nether` | Nether Terrain | 8.0 | Ceiling/floor with open caves between, netherrack base |
| `terrain_end` | End Terrain | 10.0 | Distance-based floating islands, end stone |
| `terrain_skylands` | Skylands | 8.0 | Dense floating terrain masses, no ocean/ground |
| `terrain_cave` | Cave World | 8.0 | Underground terrain with cave networks |

### Biome Controller Symbols (Category: BIOME_CONTROLLER)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `biome_single` | Single Biome | -5.0 | One biome everywhere |
| `biome_native` | Native Biome | -5.0 | Standard overworld biome distribution |
| `biome_tiny` | Tiny Biomes | -2.0 | 64-block Voronoi regions |
| `biome_small` | Small Biomes | -3.0 | 128-block Voronoi regions |
| `biome_medium` | Medium Biomes | -4.0 | 256-block Voronoi regions |
| `biome_large` | Large Biomes | -3.0 | 512-block Voronoi regions |
| `biome_huge` | Huge Biomes | -2.0 | 1024-block Voronoi regions |
| `biome_tiled` | Tiled Biomes | 0.0 | Biomes in organized rectangular tiles |
| `biome_grid` | Grid Biomes | 2.0 | Biomes in strict grid pattern |

### Biome Symbols (Category: BIOME)

All vanilla biomes are registered as symbols. All have instability 0.0 and rank 1. Over 50 biomes including:

**Overworld**: Plains, Forest, Taiga, Desert, Swamp, Jungle, Snowy Plains, Savanna, Badlands, Dark Forest, Birch Forest, Flower Forest, Meadow, Beach, Ocean, Deep Ocean, River, Mushroom Fields, Windswept Hills, Cherry Grove, Old Growth Spruce/Pine Taiga, Old Growth Birch Forest, Sparse Jungle, Bamboo Jungle, Mangrove Swamp, Warm/Lukewarm/Cold/Frozen Ocean variants, Snowy Beach, Stony Shore, Frozen River, Ice Spikes, Sunflower Plains, Wooded/Eroded Badlands, Savanna Plateau, Dripstone Caves, Lush Caves, Deep Dark

**Nether**: Nether Wastes, Crimson Forest, Warped Forest, Soul Sand Valley, Basalt Deltas

**End**: The End, End Highlands, End Midlands, End Barrens, Small End Islands

### Sun Symbols (Category: SUN)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `sun_normal` | Normal Sun | 0.0 | Standard sun with 20-minute day cycle |
| `sun_dark` | Dark Sun | 5.0 | Eclipse silhouette (no sunlight) |
| `sun_large` | Large Sun | 5.0 | 2.5x normal size |
| `sun_small` | Small Sun | 3.0 | 0.4x normal size |
| `sun_fast` | Fast Sun | 8.0 | 10-minute day cycle (2x speed) |
| `sun_slow` | Slow Sun | 5.0 | 40-minute day cycle (0.5x speed) |

**Rendering**: Core disc + animated corona + 24 radial rays + outer glow halo. Pulsing animation (0.92-1.0 scale), rotating rays, wobbling corona. Dark mode renders black disc with bright corona ring.

### Moon Symbols (Category: MOON)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `moon_normal` | Normal Moon | 0.0 | Standard moon with phase cycling |
| `moon_dark` | Dark Moon | 3.0 | Lunar eclipse (no moonlight) |
| `moon_large` | Large Moon | 3.0 | 2.5x normal size |
| `moon_small` | Small Moon | 3.0 | 0.4x normal size |
| `moon_full` | Full Moon | 2.0 | Always full phase (no cycling) |
| `moon_fast` | Fast Moon | 5.0 | Phase cycle at 2x speed |
| `moon_slow` | Slow Moon | 3.0 | Phase cycle at 0.5x speed |

**Rendering**: Textured disc + procedural craters + atmospheric glow. Phase-aware shading (0-1 range). 32 disc segments, 48 glow segments.

### Stars Symbols (Category: STARS)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `stars_normal` | Normal Stars | 0.0 | Standard twinkling stars (1500 count) |
| `stars_twinkle` | Twinkling Stars | 0.0 | Enhanced twinkling animation |
| `stars_end` | End Stars | 5.0 | End-dimension floating star particles |
| `stars_dark` | Dark Stars | 2.0 | No stars rendered |
| `stars_dense` | Dense Stars | 3.0 | 4000 stars (very dense) |
| `stars_sparse` | Sparse Stars | 0.0 | 400 stars (very sparse) |

**Rendering**: Multi-layer parallax at different rotation speeds. Varied color temperatures (blue-white to orange-red). Nebula clouds, shooting stars, brightness fade at dawn/dusk.

### Weather Symbols (Category: WEATHER)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `weather_normal` | Normal Weather | 0.0 | Standard random cycling |
| `weather_off` | Weather Off | 0.0 | No weather events |
| `weather_always` | Weather Always | 5.0 | Constant rain |
| `weather_rain` | Rain | 3.0 | Always raining |
| `weather_snow` | Snow | 3.0 | Always snowing (temperature-dependent) |
| `weather_storm` | Storm | 8.0 | Constant thunderstorm with lightning |
| `weather_cloudy` | Cloudy | 0.0 | Always cloudy, no precipitation |
| `weather_fast` | Fast Weather | 3.0 | Weather cycles at 2x speed |
| `weather_slow` | Slow Weather | 2.0 | Weather cycles at 0.5x speed |
| `weather_thunder` | Thunder | 10.0 | Frequent lightning strikes |
| `weather_blizzard` | Blizzard | 12.0 | Heavy snow with blizzard effect |

### Lighting Symbols (Category: LIGHTING)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `lighting_normal` | Normal Lighting | 0.0 | Standard brightness curve |
| `lighting_bright` | Bright Lighting | 5.0 | 30% brighter, minimum light level 4/15 |
| `lighting_dark` | Dark Lighting | 10.0 | Very dark, reduced ambient light |
| `lighting_nether` | Nether Lighting | 8.0 | Dim ambient glow |

### Feature Symbols

#### Large Features (Category: FEATURE_LARGE)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `caves` | Caves | 0.0 | Vanilla cave systems |
| `ravines` | Ravines | 0.0 | Deep chasms |
| `floating_islands` | Floating Islands | 10.0 | 6 island morphologies: Classic, Spire, Mushroom, Eroded, Mesa, Archipelago |
| `skylands` | Skylands Feature | 8.0 | Dense floating terrain |
| `perlin_worms` | Perlin Worms | 6.0 | Large worm-like tunnels carved by noise |
| `deep_dark` | Deep Dark | 15.0 | Deep Dark biome with sculk features |
| `star_fissure_feature` | Star Fissure | -10.0 | Generates exit portal (stabilizing) |

#### Medium Features (Category: FEATURE_MEDIUM)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `dense_ores` | Dense Ores | 5.0 | Extremely abundant ore veins |
| `huge_trees` | Huge Trees | 3.0 | Mega trees scattered throughout |
| `deep_lakes` | Deep Lakes | 0.0 | Underground water/lava pools |
| `spikes` | Spikes | 5.0 | Ice-spike style vertical formations |
| `spheres` | Spheres | 8.0 | Floating/embedded spherical terrain (1 per ~33 chunks) |
| `tendrils` | Tendrils | 5.0 | Vine-like twisting terrain (20-50 blocks long, can span 7+ chunks) |
| `vertical_tendrils` | Vertical Tendrils | 4.0 | Upright column-like formations |

#### Small Features (Category: FEATURE_SMALL)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `surface_lakes` | Surface Lakes | 0.0 | Water/lava lakes on surface |
| `dripstone_caves` | Dripstone Caves | 0.0 | Stalactites and stalagmites |
| `lush_caves` | Lush Caves | 0.0 | Moss, glow berries, vegetation caves |

### Structure Symbols (Category: STRUCTURE)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `villages` | Villages | 0.0 | NPC villages |
| `dungeons` | Dungeons | 0.0 | Mob spawner rooms |
| `mineshafts` | Mineshafts | 0.0 | Underground mineshafts |
| `strongholds` | Strongholds | 0.0 | End fortress with portal |
| `nether_fortress` | Nether Fortress | 10.0 | Nether fortress |
| `pillager_outposts` | Pillager Outposts | 5.0 | Outpost towers |
| `ruined_portals` | Ruined Portals | 8.0 | Broken nether portals |
| `ocean_monuments` | Ocean Monuments | 5.0 | Guardian temples |
| `witch_huts` | Witch Huts | 3.0 | Swamp witch houses |
| `desert_temples` | Desert Temples | 3.0 | Pyramid temples |
| `jungle_temples` | Jungle Temples | 3.0 | Jungle puzzle temples |
| `woodland_mansions` | Woodland Mansions | 8.0 | Dark mansions |
| `trail_ruins` | Trail Ruins | 0.0 | Surface trail structures |
| `ancient_cities` | Ancient Cities | 15.0 | Deep underground city |
| `bastion_remnants` | Bastion Remnants | 12.0 | Nether bastions |
| `end_cities` | End Cities | 15.0 | End dimension cities |
| `igloos` | Igloos | 0.0 | Snowy igloos |
| `shipwrecks` | Shipwrecks | 0.0 | Sunken ships |
| `ocean_ruins` | Ocean Ruins | 0.0 | Underwater ruins |
| `buried_treasure` | Buried Treasure | 0.0 | Treasure map targets |
| `nether_fossils` | Nether Fossils | 3.0 | Nether bone structures |

### Color Target Symbols (Category: VISUAL_EFFECT)

Each color target has a custom variant (instability 0.0) and a "natural" variant (instability -2.0, stabilizing):

| Target | Custom Symbol | Natural Symbol |
|---|---|---|
| Sky | `color_sky` | `color_sky_natural` |
| Night Sky | `color_sky_night` | -- |
| Clouds | `color_cloud` | `color_cloud_natural` |
| Fog | `color_fog` | `color_fog_natural` |
| Foliage | `color_foliage` | `color_foliage_natural` |
| Grass | `color_grass` | `color_grass_natural` |
| Water | `color_water` | `color_water_natural` |
| Horizon | `color_horizon` | `color_horizon_natural` |
| Sunset | `color_sunset` | `color_sunset_natural` |

Custom color targets consume the next color from the modifier stack.

### Color Modifier Symbols (Category: COLOR)

**16 Basic Colors**:

| Symbol | Color | Hex |
|---|---|---|
| `color_red` | Red | 0xFF0000 |
| `color_orange` | Orange | 0xFF8000 |
| `color_yellow` | Yellow | 0xFFFF00 |
| `color_green` | Green | 0x00FF00 |
| `color_blue` | Blue | 0x0000FF |
| `color_purple` | Purple | 0x8000FF |
| `color_white` | White | 0xFFFFFF |
| `color_black` | Black | 0x000000 |
| `color_cyan` | Cyan | 0x00FFFF |
| `color_magenta` | Magenta | 0xFF00FF |
| `color_lime` | Lime | 0x80FF00 |
| `color_pink` | Pink | 0xFF80C0 |
| `color_gray` | Gray | 0x808080 |
| `color_light_gray` | Light Gray | 0xC0C0C0 |
| `color_brown` | Brown | 0x8B4513 |
| `color_light_blue` | Light Blue | 0x87CEEB |

**22 Extended Colors**: Maroon, Olive, Dark Green, Teal, Navy, Silver, Gold, Coral, Crimson, Turquoise, Indigo, Amber, Rose, Jade, Sapphire, Ruby, Violet, Copper, Midnight, Emerald, Peach, Ivory

All color modifiers have rank 0 and instability 0.0.

### Direction Modifiers (Category: ANGLE)

| Symbol | Direction | Degrees |
|---|---|---|
| `mod_north` | North | 0 |
| `mod_east` | East | 90 |
| `mod_south` | South | 180 |
| `mod_west` | West | 270 |

### Phase Modifiers (Category: PHASE)

| Symbol | Phase | Degrees |
|---|---|---|
| `mod_nadir` | Bottom | 0 |
| `mod_rising` | Rising | 90 |
| `mod_zenith` | Top | 180 |
| `mod_setting` | Setting | 270 |

### Length Modifiers (Category: LENGTH)

| Symbol | Value |
|---|---|
| `mod_zero` | 0.0 |
| `mod_half` | 0.5 |
| `mod_full` | 1.0 |
| `mod_double` | 2.0 |

### General Modifiers (Category: MODIFIER)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `mod_clear` | Clear Modifiers | 0.0 | Clears all modifier stacks |
| `no_sea` | No Sea | 5.0 | Removes all water/sea |
| `cloud_low` | Clouds Low | 3.0 | Cloud height at Y=96 |
| `cloud_high` | Clouds High | 3.0 | Cloud height at Y=300 |
| `clouds_none` | No Clouds | 5.0 | Clouds hidden (Y=-64) |
| `horizon_low` | Low Horizon | 2.0 | Horizon at Y=-32 |
| `horizon_high` | High Horizon | 2.0 | Horizon at Y=64 |

### Gradient Symbols

| Symbol | Name | Instability | Color | Description |
|---|---|---|---|---|
| `gradient_sunset` | Sunset Gradient | 0.0 | 0xFF6B35 | Orange-red sunset |
| `gradient_dawn` | Dawn Gradient | 0.0 | 0xFFB366 | Pink-gold dawn |
| `gradient_dusk` | Dusk Gradient | 0.0 | 0x8B5A8B | Purple dusk |
| `gradient_aurora` | Aurora Gradient | 3.0 | 0x00FF88 | Green aurora |
| `gradient_blood_sky` | Blood Sky | 5.0 | 0x8B0000 | Dark red blood sky |
| `gradient_builder` | Gradient Builder | 0.0 | -- | Pops color stack to gradient |
| `gradient_apply_sunset` | Apply Sunset | 0.0 | -- | Blends and applies gradients |

### Environment Symbols (Category: ENVIRONMENT)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `env_accelerated` | Accelerated | 10.0 | Crop growth and mob spawning at 2x |
| `env_meteors` | Meteors | 25.0 | Meteor strikes from sky |
| `env_lightning` | Lightning | 15.0 | Frequent lightning |
| `env_scorched` | Scorched | 20.0 | Blocks spontaneously catch fire |
| `env_explosions` | Explosions | 30.0 | Random explosions |

### Timescale Symbols (Category: ENVIRONMENT)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `env_longer_days` | Longer Days | 5.0 | 40-minute day cycle (0.5x) |
| `env_shorter_days` | Shorter Days | 5.0 | 10-minute day cycle (2x) |
| `env_slow_time` | Slow Time | 10.0 | 80-minute day cycle (0.25x) |
| `env_static_time` | Static Time | 15.0 | Time of day frozen |

### Special Symbols

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `star_fissure` | Star Fissure | -25.0 | Portal back to home world (highly stabilizing) |
| `obelisks` | Obelisks | 5.0 | Obelisk structures |
| `anti_pvp` | Anti-PvP | 0.0 | Disables PvP in the Age |
| `hide_horizon` | Hide Horizon | 3.0 | Hides horizon line |
| `crystal_formation` | Crystal Formation | 8.0 | Crystal structures generate |
| `rainbow` | Rainbow | 0.0 | Rainbow effect in sky |

### Ore Symbols (Dynamic)

Individual ore control symbols (e.g., `extra_iron_ore`, `extra_coal_ore`, `extra_copper_ore`, `extra_gold_ore`, `extra_diamond_ore`, `extra_emerald_ore`, `extra_redstone_ore`, `extra_lapis_ore`).

### Fluid/Sea Symbols (Category: SEA, Dynamic)

| Symbol | Name | Instability | Description |
|---|---|---|---|
| `sea_minecraft_water` | Water Sea | 0.0 | Standard water ocean |
| `sea_minecraft_lava` | Lava Sea | 25.0 | Lava ocean |

Additional fluids registered dynamically from Forge fluid registry (supports modded fluids).

### Block Terrain Symbols (~294, Dynamic)

Terrain block override symbols generated for all vanilla blocks. Sets the primary terrain material. Examples:

- `block_minecraft_stone` (instability 0.0)
- `block_minecraft_diamond_ore` (instability 20.0)
- `block_minecraft_obsidian` (instability 10.0)
- All wood types, stone variants, ores, terracotta colors, concrete colors, wool colors, glass colors, ice variants, etc.

---

## The Grammar Engine

The grammar engine is a Context-Free Grammar (CFG) system that expands user-provided symbols into a complete Age specification. It ensures every Age has all required components even if the player only provides a few symbols.

### How Age Building Works

1. **Input**: Player binds pages into an Agebook with a link panel and symbol pages
2. **Symbol Parsing**: Symbols are read from the book and matched to the grammar
3. **Tree Building**: CFGGrammarTree builds derivation trees connecting user symbols to the root
4. **Gap Filling**: Missing required components are generated using weighted random selection
5. **Post-Processing**: Removes duplicates, enforces singletons, filters incompatible combinations
6. **Instability Calculation**: Computed from symbol costs + generation penalties
7. **AgeDirector Configuration**: Symbols apply effects to the director (150+ properties)
8. **Dimension Creation**: AgeDimensionFactory creates the dimension from the director

### Grammar Structure

```
Root (age)
  |- terrain_gen -> (terrain type)
  |- biome_controller -> (distribution strategy)
  |- biomes -> (1-3 biomes)
  |- weather -> (weather type)
  |- lighting -> (lighting type)
  |- block_sea -> (ocean fluid)
  |- suns -> (1-3 suns with modifiers)
  |- moons -> (1-3 moons with modifiers)
  |- starfields -> (1-3 star types)
  |- visuals -> (color targets and effects)
  |- feature_larges -> (caves, ravines, etc.)
  |- feature_mediums -> (ores, spikes, etc.)
  |- feature_smalls -> (lakes, dripstone, etc.)
  |- effects -> (environmental effects)
```

### Post-Processing Rules

- **Singleton enforcement**: Only one terrain, biome controller, weather, lighting, sea symbol
- **Void terrain filtering**: Removes caves, ravines, villages, dungeons, mineshafts, strongholds
- **Nether terrain**: Removes incompatible surface structures
- **No-sea terrains**: Removes sea modifier symbols
- **Fallback biome**: Adds Plains if no biome specified

### Instability Calculation Formula

```
baseInstability = sum(symbol.getInstabilityCost() for each symbol)
generationPenalty = generatedSymbolCount * 2.5
missingPenalty = missingRequiredCount * 8.0
totalRaw = baseInstability + generationPenalty + missingPenalty

if (noGeneratedSymbols && providedCount >= 5 && noMissingSymbols):
    totalRaw *= 0.8  // Well-written bonus

finalInstability = totalRaw * 0.5  // Global reduction
```

### Fallback Systems (Applied After Grammar)

- **Terrain Block Bias**: 45% chance to select a random thematic terrain block
- **Ore Variation**: 30% chance for ore boost (1-3 extra ore symbols), 15% for ore disable
- **Timescale Variation**: 20% chance for time scaling (longer 40%, shorter 30%, slow 20%, static 10%)
- **Environment Effects**: 12% base chance, increases with instability > 20 (up to 50%)
- **Color Palettes**: Random colors for any unset color targets

---

## World Generation

### Chunk Generator

`AgeChunkGenerator` wraps vanilla `NoiseBasedChunkGenerator` for most terrain types and applies Mystcraft modifications on top.

**Vanilla delegation**: Normal, Amplified, Nether, End, Cave, Skylands terrain types use vanilla noise generation as the base.

**Custom generators**: Void and Flat terrain bypass vanilla noise entirely.

**Terrain mix modes**: Multiple terrain types can blend across chunks:
- `checkerboard`: Alternating 64-block regions
- `gradient`: Gradual transitions
- `gradient_x` / `gradient_z`: Per-axis gradients

### Terrain Types

**Normal Terrain**: Grid-based noise with 4x4x8 resolution cells, trilinear interpolation, 5x5x49 noise field. Uses 4 octave noise generators (16+ octaves). Blends noise sources via third "blend factor" noise. Parabolic falloff for biome height blending.

**Amplified Terrain**: Same as normal with doubled heights (1.0 + height * 2.0).

**Nether Terrain**: Same grid-based noise with cosine wave vertical envelope creating ceiling/floor layers. Higher noise frequency. Netherrack base, lava sea.

**End Terrain**: Distance-based density from world origin. Islands denser near center, sparse at distance. End stone base, no sea.

**Void Terrain**: Single bedrock layer at minimum build height. Pure air above.

**Flat Terrain**: Direct block filling. Bedrock bottom, solid terrain to ground level, optional sea.

**Cave World**: Underground-focused terrain with extensive cave networks.

**Skylands**: Dense floating terrain masses with no ground or ocean.

### Biome Controllers

**Single**: One biome throughout the entire Age.

**Native**: Standard overworld biome distribution (vanilla-like).

**Noise (Voronoi)**: Voronoi cell regions with jittered centers. Scales from Tiny (64 blocks) to Huge (1024 blocks). 4096-entry cache.

**Tiled**: Rectangular tile grid, each tile gets a biome from the list.

**Grid**: Strict grid pattern biome assignment.

### Populators (33+)

**Vanilla Structure Populators**: Villages, Dungeons, Mineshafts, Strongholds, Nether Fortress, End Cities, Desert Temples, Jungle Temples, Woodland Mansions, Ocean Monuments, Pillager Outposts, Witch Huts, Shipwrecks, Ocean Ruins, Buried Treasure, Nether Fossils, Igloos, Ruined Portals, Ancient Cities, Trail Ruins, Bastion Remnants.

**Mystcraft Feature Populators**:
- **Spheres**: Floating/embedded spherical formations, 1 per ~33 chunks, radius 5-15 blocks
- **Tendrils**: Vine-like twisting formations, 1 per ~12 chunks, 20-50 blocks long, can span 7+ chunks
- **Spikes**: Vertical rocky formations
- **Vertical Tendrils**: Upright column structures
- **Perlin Worms**: Large worm-carved tunnels using noise

**Resource Populators**: Standard Ores, Dense Ores, Single Ore (individual control).

**Decoration Populators**: Biome Decoration (vanilla flowers/trees), Deep Lakes, Surface Lakes, Huge Trees.

**Cross-chunk safety**: All multi-chunk populators use the neighbor-seed pattern -- each chunk scans neighboring chunks' deterministic RNG and only places blocks within its own 16x16 boundary.

### Floating Islands

6 procedural island morphologies:
- **Classic**: Broad plateau with noise-varied surface and tapered underside
- **Spire**: Tall narrow rocky peak
- **Mushroom**: Wide canopy on narrow stem
- **Eroded**: Irregular shape with carved-out holes
- **Mesa**: Flat-topped with layered sediment bands
- **Archipelago**: Cluster of small linked chunks

Distribution: 30% Classic, 15% Spire, 15% Mushroom, 15% Eroded, 15% Mesa, 10% Archipelago.

### Surface Building

Applies surface/subsurface block overrides per biome. Handles grass/sand/snow top layers, dirt/gravel/sandstone subsurface.

---

## Celestial Bodies

### Sun

**Rendering components**: Core disc, animated corona (48 segments), 24 radial rays, outer glow halo.

**Animation**: Pulsing scale (0.92-1.0), rotating rays, wobbling corona. All animated per-tick.

**Dark sun**: Eclipse rendering with black disc and bright corona ring.

**Custom color**: Default gold (255, 242, 204), overridable via color symbols.

### Moon

**Rendering components**: Textured disc (32 segments), procedural craters, atmospheric glow (48 segments).

**Phase system**: Phase-aware shading (0-1 range). Full moon symbol locks at max phase.

**Dark moon**: Lunar eclipse with corona ring.

### Stars

**Rendering**: Multi-layer parallax stars at different rotation speeds.

**Properties**: Varied color temperatures (blue-white to orange-red). Per-star twinkle effect. Configurable count (100-8000, default 1500).

**Features**: Nebula clouds, shooting stars, brightness fade at dawn/dusk.

**Star types**: Normal, Twinkle, End (vanilla end sky texture), Dark (no stars).

---

## Weather and Lighting

### Weather Controllers

9 controllers manage weather per-Age:

| Controller | Behavior |
|---|---|
| Normal | Vanilla random rain/thunder cycling |
| Off | No weather |
| Always/Rain | Constant rain |
| Snow | Always snowing |
| Storm/Thunder | Constant thunderstorm |
| Cloudy | Always cloudy, no precipitation |
| Blizzard | Heavy snow with blizzard |
| Fast | 2x weather cycle speed |
| Slow | 0.5x weather cycle speed |

**Rain duration**: 6000 ticks (5 min) base + variance. **Thunder duration**: 3600 ticks (3 min) base. **Transitions**: Smooth level changes at 0.01/tick.

### Lighting Controllers

| Controller | Behavior |
|---|---|
| Normal | Standard Minecraft brightness curve |
| Bright | 30% brighter, minimum light 4/15 |
| Dark | Reduced ambient, fog distance reduced to 70% |
| Nether | Nether-style dim glow |

---

## The Instability System

The instability system creates hazardous environments in poorly-written Ages. It uses a deck-drawing card system to determine which effects activate, ensuring each Age has a unique instability signature.

### Instability Rating Tiers

| Instability | Rating |
|---|---|
| <= 0 | Perfectly Stable |
| 1-10 | Stable |
| 11-30 | Slightly Unstable |
| 31-50 | Unstable |
| 51-80 | Very Unstable |
| 81-100 | Dangerous |
| 101-150 | Extremely Dangerous |
| 150+ | Catastrophic |

### The Deck System

Effects are organized into 5 card decks. Each deck has a base instability cost to unlock. Cards within decks have individual costs. As instability increases, more cards are "drawn" from each deck.

| Deck | Base Cost | Contents |
|---|---|---|
| basic | 0 | Scorched (x3), Extra Ticks (x5) |
| harsh | 10 | Environmental (Scorched, ExtraTicks) |
| destructive | 30 | Crumble, Erosion, Explosions, Lightning, Meteors |
| eating | 50 | All potion effects (debuffs + buffs), Decay (blue/purple/red) |
| death | 75 | Global potion variants, high-tier Decay (white/black) |

**Card drawing**: The controller iterates through shuffled deck cards, spending instability budget on each card's cost until the budget runs out. Shuffling ensures each Age draws different effects even at the same instability level.

### Effect Frequency Formula

```
chance = baseChance * (1.0 + min(excess / 30.0, 5.0)) * multiplier
```

Where `excess = instability - threshold`. Maximum 6x frequency at very high instability.

### Intensity Scaling

**Environmental effects**: `intensity = max(0.1, min(instability / 80.0, 1.0))`

**High-threshold effects**: `intensity = max(0.0, min((instability - threshold) / range, 1.0))`

### All Instability Effects

#### Environmental Effects

**Crumble** (Cost 8): Ore degradation chains. Diamond -> Emerald -> Gold -> Iron -> Coal -> Stone. Stone Bricks -> Cracked -> Cobblestone -> Gravel. Glass/Leaves -> Air. 1-8 passes per tick scaling with intensity.

**Decay** (Cost 8-20): Spreads colored decay blocks. 5 types (Blue, Purple, Red, White, Black) with varying damage and spread behavior. 1-6 attempts per tick.

**Erosion** (Cost 8): Fluids erode adjacent solid blocks. Checks all 6 neighbors for fluid, replaces solid with adjacent fluid. 1-6 attempts per tick.

**Explosions** (Cost 5, threshold 30): Random explosions near players. Power 1.0-2.0 scaling with intensity. Minimum 8 blocks from player. Base chance 0.0005/tick.

**Extra Ticks** (Cost 6): Accelerates random tick events (crop growth, fire spread, grass spread). 1-8 extra tick passes per chunk section.

**Scorched** (Cost 2, lowest threshold): Random fire blocks on solid surfaces. Base chance 0.008/tick.

**Lightning** (Cost 5, threshold 50): Lightning strikes. Visual-only at low instability (80 blocks away), damaging above instability 70 (24 blocks away). Minimum 16-40 block distance.

**Meteor** (Cost 5, threshold 30): Falling meteors from 100-150 blocks above player. Size 1-3 scaling with intensity. Base chance 0.0005/tick.

#### Potion Effects (Threshold 50+)

**Player Debuffs**: Blindness (3s), Mining Fatigue (4s), Hunger (4s), Nausea (3s), Poison (4s), Slowness (4s), Weakness (4s), Wither (1.5s, most dangerous).

**Player Buffs** (chaotic boons mixed with debuffs): Speed (30s), Haste (30s), Strength (20s), Jump Boost (30s), Regeneration (10s), Resistance (15s), Night Vision (60s).

**Enemy Buffs**: Regeneration and Resistance applied to all hostile mobs in loaded chunks.

### Death System (AgeDeathHandler)

Tracks per-player, per-Age death counts. Selects thematic death messages by instability tier:

- **Low (<50)**: "release", "ink_dries", "thread_unravels"
- **Medium (50-80)**: "shudders", "stricken", "no_mourn"
- **High (>80)**: "devours", "ink_runs_black", "reality_tears", "already_dying"
- **Cause-specific**: void_fall, fire, magic, explosion, mob, decay
- **Repeat deaths (3+)**: "remembers", "ink_knows", "how_many"

**Instability surge on death** (instability >= 50): 0.5 + severity * 7.5 added instability.

**Respawn debuffs** (scale with severity):
- Slowness: Always at 50+, 4-20s
- Mining Fatigue: At 65+, 5-30s, amplifier II at 85+
- Darkness: At 80+, 3-10s

---

## Linking and Portals

### Link Mechanics

The LinkingManager handles all inter-dimensional travel.

**Link flags** (stored in book NBT):
- **Following**: Bring nearby entities through portal
- **Disarm**: Drop held items on linking
- **Intra-Linking**: Allow same-dimension teleportation
- **Relative**: Apply position offset
- **Generate Platform**: Create 3x3 stone spawn platform
- **Maintain Momentum**: Preserve movement vector

### Teleportation Process

1. Pre-load destination chunk
2. Calculate safe spawn Y using heightmap
3. Detect ceiling terrain (nether/cave) and search inside
4. Generate spawn platform if needed (3x3 stone)
5. Build fluid surface platform for water/lava Ages
6. Teleport main entity and passengers
7. Teleport followers if following flag active
8. Play departure/arrival sounds

### Safe Spawn Detection

A position is safe if:
- Solid ground below (not lava/bedrock)
- Feet position is passable (not blocked)
- Head position is passable
- No fire, soul fire, or magma blocks

### Portal Structure

Portals are built from Crystal blocks with a Book Receptacle:

1. Place Crystal blocks as a frame
2. Place Book Receptacle on a Crystal block
3. Insert a linked book into the Receptacle
4. PortalUtils flood-fills Link Portal blocks into the enclosed air space
5. All portal blocks point back toward the Receptacle
6. Entities walking into portal blocks are teleported to the book's destination

Portal deactivation: Remove the book from the Receptacle.

### Link Permissions

Ages support access control:
- **Blacklist mode**: Block specific players from entering/departing
- **Whitelist mode**: Only allow specific players
- **Age owners**: Full access to own Age
- **Global admins**: Bypass all restrictions

---

## The Archivist Villager

A custom villager profession that trades Mystcraft items. Uses the Lectern block as its workstation.

### Trade Levels

**Level 1 (Novice)**:
- 2 Emeralds -> 1 Ink Vial
- 8 Pages -> 1 Emerald
- 1 Emerald -> 4 Pages
- Random rank-1 symbol page

**Level 2 (Apprentice)**:
- 5 Emeralds -> 1 Folder
- 3 Emeralds -> 2 Ink Vials
- Rank 1-2 symbol pages

**Level 3 (Journeyman)**:
- 12 Emeralds -> 1 Portfolio
- 8 Emeralds -> 1 Booster Pack
- Rank 2-3 symbol pages

**Level 4 (Expert)**:
- 20 Emeralds -> 1 Unlinked Linkbook
- 16 Emeralds -> 1 Glasses
- Rank 3-4 symbol pages

**Level 5 (Master)**:
- Random symbol pages (dynamic pricing)
- 1 Linkbook -> 24 Emeralds
- Category-specific high-rank symbols

**Symbol pricing formula**: 4 * (1 + symbolRank) emeralds. Rank 1 = 8, Rank 2 = 12, Rank 3 = 16, Rank 4 = 20.

### Village Integration

The VillageStructureHandler injects an Archivist house into vanilla village template pools for Plains, Desert, Savanna, Snowy, and Taiga villages with 2x weight for higher spawn chance.

---

## Structures and Loot

### Mystcraft Structures

**Abandoned Library**: Surface structure in plains/forest biomes. Contains symbol pages, descriptive books, and Mystcraft workstations. Jigsaw-based structure with template pool.

**Underground Archive**: Deep underground structure with rare symbol pages. Template pool construction.

**Scattered Library**: Small standalone library buildings scattered across the overworld. Uses structure set with random spread placement.

### Structure Data Packs

Each structure has:
- Structure definition JSON (biome placement, generation step)
- Structure set JSON (spacing, separation, salt)
- Template pool JSON (jigsaw pieces)
- Biome tags for placement filtering

### Loot Integration

**SymbolPageLootModifier**: Global loot modifier that adds 1-3 random symbol pages to dungeon, mineshaft, stronghold, and custom structure chests with 50% chance per chest.

**GuidebookLootModifier**: Adds guidebooks to loot tables.

**Custom loot tables**:
- `mystcraft:chests/abandoned_library`
- `mystcraft:chests/underground_archive`

---

## Advancements

3 custom criteria triggers:

| Trigger | ID | Condition |
|---|---|---|
| Writing Desk Write | `mystcraft:writing_desk_write` | Player writes symbol at Writing Desk |
| Enter Age (Safe) | `mystcraft:enter_myst_dimension_safe` | Player enters a Mystcraft Age |
| Enter Age (Quinn) | `mystcraft:enter_myst_dimension_quinn` | Thematic challenge variant |

Advancement data files at `/data/mystcraft/advancements/`:
- `linkbook.json` - Advancement for obtaining a linkbook
- `write.json` - Advancement for writing at a writing desk

---

## Client Rendering

### Sky Rendering (AgeDimensionSpecialEffects)

Custom sky renderer for each Age, handling:

1. **Fog color**: Custom fog color with time-of-day factor (0.2 at night, 1.0 at day). Lighting type modifications (bright: 1.2x, dark: 0.5x).
2. **Sky dome**: Custom color blending via tessellator
3. **Celestial rendering**: Calls ICelestial.render() for each registered sun/moon/stars
4. **Void rendering**: Black dome below horizon when HorizonHidden is true
5. **Stars**: Small quads with parallax layers

Falls back to vanilla rendering when no custom colors or effects are needed.

### Block Colors (AgeBlockColorHandler)

Custom block color handlers per-Age for grass, foliage, and water.

**Multi-color grass**: Uses 2D Perlin noise (48-block wavelength) to select from a palette of colors. Creates organic color blobs across the landscape.

**Portal colors**: Traces to nearest BookReceptacleBlockEntity to get portal tint color.

### Fog and Lighting (AgeRenderingHandler)

Modifies fog color and distance based on lighting type:
- **Bright**: 1.2x fog RGB (clamped)
- **Dark**: 0.5x fog RGB, near/far fog planes reduced to 50%/70%

### Instability Overlay (InstabilityEffects)

Code for screen vignette overlay (green -> yellow -> orange -> red) exists but is disabled. Instability is communicated through gameplay effects instead.

---

## Commands

All commands under `/mystcraft`, permission level 0 (view) or 2 (modify).

### Age Management

| Command | Permission | Description |
|---|---|---|
| `/mystcraft age list` | 0 | List all known Ages |
| `/mystcraft age tp <ageId>` | 2 | Teleport to Age |
| `/mystcraft age info <ageId>` | 0 | Show Age information |
| `/mystcraft age create` | 2 | Create new empty Age |

### Symbol Information

| Command | Permission | Description |
|---|---|---|
| `/mystcraft symbols` | 0 | List all registered symbols |
| `/mystcraft symbols info <symbolId>` | 0 | Show symbol details |

### Item Distribution

| Command | Permission | Description |
|---|---|---|
| `/mystcraft give randombook [count=5]` | 2 | Random Agebook with curated symbols |
| `/mystcraft give agebook <ageId>` | 2 | Book linked to existing Age |
| `/mystcraft give preset <name>` | 2 | Preset Agebook |

### Instability Control

| Command | Permission | Description |
|---|---|---|
| `/mystcraft instability toggle [ageId] [enabled]` | 2 | Toggle instability |
| `/mystcraft instability set <value> [ageId]` | 2 | Set instability value |
| `/mystcraft reprofile [ageId]` | 2 | Recalculate instability from pages |

### Age Environment

| Command | Permission | Description |
|---|---|---|
| `/mystcraft time set <ticks>` | 2 | Set Age time |
| `/mystcraft time add <ticks>` | 2 | Add time |
| `/mystcraft weather clear\|rain\|thunder` | 2 | Control weather |
| `/mystcraft regen [radius=1]` | 2 | Regenerate chunks |

### Effects and Debug

| Command | Permission | Description |
|---|---|---|
| `/mystcraft spawn meteor [scale=2]` | 2 | Spawn meteor at look position |
| `/mystcraft debug instability` | 2 | Current instability debug info |
| `/mystcraft where` | 0 | Show current Age info and instability |

---

## Configuration

Forge config at `mystcraft-common.toml`.

### General Settings

| Setting | Default | Description |
|---|---|---|
| `giveGuidebookOnFirstSpawn` | true | Give new players the guidebook |
| `maxSymbolsPerBook` | 50 | Max symbols per Agebook (-1 = unlimited) |
| `deleteAgesOnStartup` | false | Delete all Ages on server start (dev/testing) |

### Instability Settings

| Setting | Default | Description |
|---|---|---|
| `instabilityEnabled` | true | Master switch for all instability effects |
| `deathEffectsEnabled` | true | Thematic death effects in Ages |
| `allowUnstableAges` | true | Allow Ages above max threshold |
| `instabilityMultiplier` | 1.0 | Global chance multiplier (0.0-10.0) |
| `maxAllowedInstability` | 150.0 | Max instability threshold (0.0-1000.0) |

### Effect Thresholds

| Setting | Default | Effect |
|---|---|---|
| `thresholdDecay` | 20.0 | Decay block spreading |
| `thresholdTransmute` | 30.0 | Block transmutation |
| `thresholdLightning` | 40.0 | Lightning strikes |
| `thresholdMeteor` | 60.0 | Meteor falls |
| `thresholdPoison` | 80.0 | Poison/hunger effects |
| `thresholdWither` | 100.0 | Wither effects |

### Effect Base Chances (per tick)

| Setting | Default | Rate |
|---|---|---|
| `chanceDecay` | 0.001 | 0.1% per tick |
| `chanceTransmute` | 0.002 | 0.2% per tick |
| `chanceLightning` | 0.0005 | 0.05% per tick |
| `chanceMeteor` | 0.0002 | 0.02% per tick |
| `chancePlayerEffect` | 0.0001 | 0.01% per tick |

---

## Age Presets

Curated presets accessible via `/mystcraft give preset <name>`. Each preset defines fixed symbols plus random pools for variety.

Presets include themes like:

- **classic** - Balanced overworld-like Age with random biomes, celestials, structures, ores, and features
- **lush** - Lush paradise with vegetation-heavy biomes, bright lighting, huge trees, deep lakes, rainbow
- **nether** - Nether-style terrain with nether biomes
- **void** - Void terrain for creative building
- **skylands** - Floating terrain masses
- **cave** - Underground cave world
- **frozen** - Snow and ice themed
- **desert** - Arid badlands themed
- **ocean** - Ocean-dominated
- **dark** - Dark, dangerous Ages
- And 20+ more themed presets

Each preset specifies:
- Fixed symbols (terrain, biome controller, lighting, weather, etc.)
- Pool picks (e.g., "pick 6 biomes from these 13 options")
- Pool categories covering: biomes, celestials, structures, ores, features, modifiers, gradients

---

## Ink and Writing

### Ink Effect System (InkEffects)

Items can be combined with link panels in the Ink Mixer to add link properties:

| Item | Properties | Probability |
|---|---|---|
| Gunpowder | Disarm | 20% |
| Clay Ball | Generate Platform | 25% |
| Experience Bottle | Intra-Linking | 15% |
| Ender Pearl | Intra-Linking, Disarm | 15% each |
| Feather | Maintain Momentum | 15% |
| Fire Charge | Disarm | 25% |
| Ghast Tear | Relative | 15% |
| Slime Ball | Maintain Momentum | 10% |
| Magma Cream | Following | 20% |
| Blaze Powder | Disarm, Following | 10% each |
| Ender Eye | Intra-Linking, Following | 20%, 15% |
| Chorus Fruit | Relative | 20% |
| Prismarine Crystals | Generate Platform | 20% |
| Nether Star | All (rare) | 10% each |
| Gold Ingot | Intra-Linking, Platform | 10%, 5% |
| Diamond | Intra-Linking, Momentum, Platform | 10%, 5%, 5% |
| Emerald | Following | 15% |
| Amethyst Shard | Relative | 10% |
| Echo Shard | Relative, Intra-Linking | 15% each |

### Property Colors

| Property | Color |
|---|---|
| Intra-Linking | Green |
| Intra-Linking-Only | White |
| Generate-Platform | Gray |
| Maintain-Momentum | Blue |
| Disarm | Red |
| Relative | Purple |
| Following | Orange |

---

## Networking

SimpleChannel with 13 packet types. Protocol version 1.

| ID | Packet | Direction | Purpose |
|---|---|---|---|
| 0 | OpenBookPacket | Client -> Server | Player opens book GUI |
| 1 | SyncAgeDataPacket | Server -> Client | Sync Age colors/config |
| 2 | LinkEffectPacket | Server -> Client | Play link sound/particles |
| 3 | SymbolSyncPacket | Server -> Client | Sync symbol registry |
| 4 | ContainerActionPacket | Client -> Server | GUI button click |
| 5 | ConfigSyncPacket | Server -> Client | Sync mod config |
| 6 | DimensionSyncPacket | Server -> Client | Sync dimension info |
| 7 | ProfilingStatePacket | Server -> Client | Debug profiling toggle |
| 8 | ExplosionPacket | Server -> Client | Custom explosion particles |
| 9 | SpawnLightningPacket | Server -> Client | Colored lightning |
| 10 | LinkBookActivatePacket | Client -> Server | Hand-based book use |
| 11 | EntityBookActivatePacket | Client -> Server | Entity book use |
| 12 | BlockBookActivatePacket | Client -> Server | Bookstand/Lectern book use |

**SyncAgeDataPacket** transmits: Age name, instability, weather/lighting/biome controller types, all colors (sky, fog, grass array, foliage, water, cloud, nightSky, horizon, sunset), celestial visibility flags, star type, horizon hidden, cloud/horizon height.

**ClientAgeDataCache**: Client-side concurrent HashMap indexed by ageUID, accessed by rendering systems.

---

## Event Handlers

### AgeDataSyncHandler

Syncs Age configuration from server to client on:
- `PlayerChangedDimensionEvent`: Entering a Mystcraft Age
- `PlayerLoggedInEvent`: Logging in while in an Age
- `PlayerRespawnEvent`: Respawning in an Age

### AgeEffectsHandler

Server-side tick handler managing:
- **Weather control**: Per-dimension IWeatherController instances
- **Environmental effects**: Meteors, lightning, explosions, scorched (flag-based, once per 20 ticks)
- **Instability ticking**: Processes InstabilityController deck effects in 4-chunk radius around players
- **Timescale**: Adjusts day/night cycle speed (0.0 = frozen, 0.5 = half, 1.0 = normal, 2.0 = double)

### AgeDeathHandler

Handles thematic death messages and respawn penalties in Ages (see Instability System section).

### GuidebookHandler

Gives new players the Mystcraft guidebook on first login (checks persistent `mystcraft:guidebook_given` tag).

### VillageStructureHandler

Injects Archivist house into vanilla village template pools at server startup via reflection.

---

## Dimension Types

7 dimension type configurations:

| Type | File | Properties |
|---|---|---|
| Normal | `age_normal.json` | Overworld-like, Y -64 to 320, natural sky |
| Nether | `age_nether.json` | Ceiling at 128, no sky, always dark |
| End | `age_end.json` | No weather, fixed time, dragon fight |
| Dark | `age_dark.json` | Always dark, reduced ambient |
| Bright | `age_bright.json` | Always bright, increased ambient |
| Cave | `age_cave.json` | Underground, ceiling, no sky |
| Skylands | `age_skylands.json` | No bedrock floor, extended height |

---

## Sound Events

9 custom sounds:

| Sound | Usage |
|---|---|
| `linking.pop` | Pop/alert |
| `linking.link` | Successful link |
| `linking.link-disarm` | Link disarm |
| `linking.link-following` | Following link |
| `linking.link-intra` | Intra-dimensional link |
| `linking.link-fissure` | Star Fissure activation |
| `linking.link-portal` | Portal activation |
| `entity.meteor.roar` | Meteor approach |
| `entity.meteor.impact` | Meteor impact |

---

## Statistics

- **Total symbols**: 500+ (including ~294 dynamic block symbols, 50+ biomes, 37 colors)
- **Blocks**: 12
- **Items**: 10 (+9 block items, +1 bucket)
- **Block Entities**: 8
- **Entities**: 5
- **Menus/GUIs**: 6
- **Fluids**: 1 (2 states)
- **Sounds**: 9
- **Creative Tabs**: 2
- **Villager Professions**: 1
- **Structures**: 3 (Mystcraft-specific)
- **Dimension Types**: 7
- **Network Packets**: 13
- **Commands**: 40+ subcommands
- **Config Options**: 21
- **Instability Effects**: 25+ (8 environmental + 17 potion/enemy)
- **Age Presets**: 30+
