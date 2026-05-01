# Mystcraft Legacy

Mystcraft is a recreation of the lore of the Myst series in Minecraft. It allows
for the writing of and travel to new dimensions, called Ages.

Check out Direwolf20's Spotlights for more info, or come hang out on the
Discord (`https://discord.gg/pRGH45W`).

---

## Project Overview

Mystcraft Legacy is a Fabric and Forge continuation of Mystcraft targeting
Minecraft 1.20.1. The mod preserves the core experience while moving symbol
content to datapacks for maximum flexibility.

TLDR: You can do just about everything you could before, it supports newer
1.20.1, and it is designed to keep evolving.

---

## Supported Versions

| Version    | Fabric | Forge |
|:-----------|:------:|:-----:|
| **1.20.1** |   ✅    |   ✅   |

---

## Current Status (Important Notes)

- Block renderers for portals, writing desks, vanilla lecterns, and similar
  display blocks are not working as intended yet.
  Visuals may appear incorrect or missing while the backend logic still
  functions.

---

## What the Mod Lets Players Do

- Create custom Ages (dimensions) using Descriptive Books built from symbol
  pages.
- Personal Dimension Books.
- Travel between Ages using Linkbooks and portal structures.
- Customize how links behave (follow, relative, disarm, maintain momentum,
  generate platform).
- Discover or trade for symbols, then craft tailored worlds with specific
  terrain, biomes, weather, lighting, structures, and visual effects.
- Experience instability in unsafe Ages (decay, lightning, meteors, explosions,
  potion effects).

---

## Core Systems

- Ages: Each Age is a unique dimension built from a symbol list. Missing or
  conflicting symbols add instability.
- Symbols: Data-driven “world rules” that define terrain, biomes, weather,
  lighting, features, structures, colors, and effects.
- Grammar Engine: A context-free grammar system fills gaps and generates
  coherent Ages when symbol sets are incomplete.
- Instability: A balancing system that escalates effects as instability rises.
- Linking: Teleportation system that handles permissions, spawn safety, and link
  flags.

---

## Main Gameplay Blocks

- Writing Desk: Writes symbols onto blank pages.
- Book Binder: Binds pages into Agebooks or Linkbooks.
- Ink Mixer: Creates colored inks and imbues link properties.
- Link Modifier: Applies link flags to books. (Borked)
- Book Receptacle: Holds a book and activates a portal. Right-click with a book to insert; shift+empty-hand to retrieve. Each portal cell tints to the held book's colour (Linkbook: stored link colour, default 0x4488FF; Agebook: 0x66AAFF linked / 0x808890 unwritten; PersonalLinkBook: 0xAA44FF). Breaking the receptacle, breaking any frame crystal, or `/setblock`-ing it to air takes the lit portal down deterministically.
- Crystal Block + Link Portal: Portal frame and portal surface. Each lit portal block carries a `LinkPortalBlockEntity` that stores a stamped colour, the source receptacle's BlockPos, and a snapshot of the book NBT — so the colour is uniform across every cell of a portal, two adjacent portals never bleed colours, and teleportation works even if the receptacle is removed mid-traversal.
- Lectern (Vanilla): Displays and opens Mystcraft books. (Borked)

---

## Key Items

- Descriptive Book: Creates Ages from symbol pages.
- Linkbook / Unlinked Linkbook: Teleport to recorded locations.
- Pages: Blank, symbol pages, and link panels.
- Ink Vial / Ink Bucket: Required for writing symbols.
- Folder / Portfolio: Stores pages (ordered vs searchable).
- Booster Pack: Generates random pages.
- Guidebook: In-game manual.

---

## World Generation Controls (Symbols)

- Terrain: Normal, nether, end, void, cave, skylands, amplified, flat, etc.
- Biomes: Single biome, lists, themed or randomized mixes.
- Weather: Off, normal, rain, thunder, blizzard, etc.
- Lighting: Normal, dark, bright, nether-like.
- Features & Structures: Villages, dungeons, temples, strongholds, mansions,
  oceans, etc.
- Colors: Sky, fog, water, grass, foliage, clouds, sunset, horizon.
- Environment Effects: Accelerated time, meteors, lightning, scorched,
  explosions.

---

## Instability Effects

- Decay (multiple decay types)
- Erosion and crumbling
- Meteors and lightning
- Explosions
- Player potion effects (negative and positive)
- Scorched terrain

---

## Link Properties (Ink Effects)

- Intra-linking and intra-only links
- Maintain momentum
- Disarm
- Relative positioning
- Following links
- Generate platforms

---

## Commands

- Age list/info/teleport/create
- Give random/preset books
- Instability controls and recalculation
- Age weather/time controls
- Meteor spawning and regen tools

---

## Configuration (What You Can Tune)

- Starter guidebook on first spawn
- Max symbols per book
- Allow/disallow unstable Ages
- Instability multiplier and max allowed instability
- Effect thresholds and per-tick chances
- Symbol blacklisting (disable overpowered symbols)

---

## Development Setup

### Prerequisites

- **Java:** JDK 17
- **Minecraft Version:** 1.20.1

### Build Commands

The project uses Gradle.

**Build All:**

```bash
./gradlew buildAll
```

**Run Client (Fabric):**

```bash
./gradlew :fabric:1.20.1:runClient
```

**Run Client (Forge):**

```bash
./gradlew :forge:1.20.1:runClient
```

### VS Code / VS Code Insiders

Open `Mystcraft-Legacy-Returned.code-workspace` from either VS Code or VS Code
Insiders. The workspace applies a blue UI background so it is obvious the
workspace settings loaded, recommends the Java/Gradle extensions, and exposes
the supported build/run/test entries through **Terminal > Run Task**.

The workspace tasks mirror the supported 1.20.1 targets:

- Build all game platform jars
- Build Fabric / Forge jars separately
- Build Psycho / XComp jars
- Run Fabric / Forge client and server
- Run Fabric / Forge GameTest servers
- Run the fast all-platform GameTest script

---

## Access Transformers / Access Wideners

This mod requires access to private Minecraft fields (e.g.,
`LecternBlockEntity.book`). Each loader has different requirements:

| Loader     | File Location                                                    | Field Naming           |
|------------|------------------------------------------------------------------|------------------------|
| **Forge**  | `forge/1.20.1/src/main/resources/META-INF/accesstransformer.cfg` | SRG names (`f_59527_`) |
| **Fabric** | `fabric/1.20.1/src/main/resources/mystcraft.accesswidener`       | Mojang names (`book`)  |

**Why different names?** Forge's AT processor runs on the SRG-mapped JAR before
remapping to Mojang names. Fabric process access wideners after Mojang mapping
is applied.

When adding new AT entries for Forge, look up the SRG name (format: `f_NNNNN_`
for fields, `m_NNNNN_` for methods) from the MCP mappings or Forge decompiled
sources.

---

## Docs

See `docs/README.md` for the full documentation hub, including:

- Datapack system details
- Scripted terrain DSL
- Templates (basic, advanced, grammar-heavy)
- How-to guides, expectations, and configuration
