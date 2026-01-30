# Mystcraft Legacy (Multiloader)

Mystcraft is a recreation of the lore of the Myst series in Minecraft.  It allows for the writing of and travel to new dimensions, called Ages.

Check out Direwolf20's Spotlights for more info, or come hang out on the Discord (`https://discord.gg/pRGH45W`).

---

## Project Overview

Mystcraft Legacy is a multi-loader continuation of Mystcraft with modern versions in mind. The mod preserves the core experience while moving symbol content to datapacks for maximum flexibility.

TLDR: You can do just about everything you could before, it supports newer versions, and it is designed to keep evolving.

---

## Supported Versions

| Version    | Fabric | Forge | NeoForge |
|:-----------|:------:|:-----:|:--------:|
| **1.20.1** |   ✅    |   ✅   |    ❌     |
| **1.20.2** |   ✅    |   ✅   |    ✅     |

---

## Current Status (Important Notes)

- Block renderers for portals, writing desks, bookstands/lecterns, and similar display blocks are not working as intended yet.
  Visuals may appear incorrect or missing while the backend logic still functions.

---

## What the Mod Lets Players Do

- Create custom Ages (dimensions) using Descriptive Books built from symbol pages.
- Personal Dimension Books.
- Travel between Ages using Linkbooks and portal structures.
- Customize how links behave (follow, relative, disarm, maintain momentum, generate platform).
- Discover or trade for symbols, then craft tailored worlds with specific terrain, biomes, weather, lighting, structures, and visual effects.
- Experience instability in unsafe Ages (decay, lightning, meteors, explosions, potion effects).

---

## Core Systems

- Ages: Each Age is a unique dimension built from a symbol list. Missing or conflicting symbols add instability.
- Symbols: Data-driven “world rules” that define terrain, biomes, weather, lighting, features, structures, colors, and effects.
- Grammar Engine: A context-free grammar system fills gaps and generates coherent Ages when symbol sets are incomplete.
- Instability: A balancing system that escalates effects as instability rises.
- Linking: Teleportation system that handles permissions, spawn safety, and link flags.

---

## Main Gameplay Blocks

- Writing Desk: Writes symbols onto blank pages.
- Book Binder: Binds pages into Agebooks or Linkbooks.
- Ink Mixer: Creates colored inks and imbues link properties.
- Link Modifier: Applies link flags to books. (Borked)
- Book Receptacle: Holds a book and activates a portal. (Borked)
- Crystal Block + Link Portal: Portal frame and portal surface. (Borked)
- Bookstand/Lectern: Displays and opens books. (Borked)

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
- Celestials: Sun, moon, stars, gradients, colors, angles.
- Features & Structures: Villages, dungeons, temples, strongholds, mansions, oceans, etc.
- Colors: Sky, fog, water, grass, foliage, clouds, sunset, horizon.
- Environment Effects: Accelerated time, meteors, lightning, scorched, explosions.

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
- **Minecraft Versions:** 1.20.1, 1.20.2

### Build Commands
The project uses Gradle.

**Build All:**
```bash
./gradlew buildAll
```

**Run Client (Fabric):**
```bash
./gradlew :fabric:1.20.1:runClient
./gradlew :fabric:1.20.2:runClient
```

**Run Client (Forge):**
```bash
./gradlew :forge:1.20.1:runClient
./gradlew :forge:1.20.2:runClient
```

**Run Client (NeoForge):**
```bash
./gradlew :neoforge:1.20.2:runClient
```

---

## Access Transformers / Access Wideners

This mod requires access to private Minecraft fields (e.g., `LecternBlockEntity.book`). Each loader has different requirements:

| Loader | File Location | Field Naming |
|--------|---------------|--------------|
| **Forge** | `forge/1.20.x/src/main/resources/META-INF/accesstransformer.cfg` | SRG names (`f_59527_`) |
| **NeoForge** | `neoforge/1.20.2/src/main/resources/META-INF/accesstransformer.cfg` | Mojang names (`book`) |
| **Fabric** | `fabric/1.20.x/src/main/resources/mystcraft.accesswidener` | Mojang names (`book`) |

**Why different names?** Forge's AT processor runs on the SRG-mapped JAR before remapping to Mojang names. NeoForge and Fabric process ATs after Mojang mapping is applied.

**Important:** The `common/` module contains an AT file with Mojang names. Forge build.gradle files exclude this and use their own SRG-named AT files instead.

When adding new AT entries for Forge, look up the SRG name (format: `f_NNNNN_` for fields, `m_NNNNN_` for methods) from the MCP mappings or Forge decompiled sources.

---

## Docs

See `docs/README.md` for the full documentation hub, including:
- Datapack system details
- Scripted terrain DSL
- Templates (basic, advanced, grammar-heavy)
- How-to guides, expectations, and configuration
