# Mystcraft Feature Comparison: Original (1.12.2) vs Port (1.20.2)

## Core Feature Table

| Feature Area | Sub-Feature | Original | Port | Status |
|---|---|---|---|---|
| **Linking Books** | Age Book / Link Book / Unlinked Link Book | Yes | Yes | DONE |
| | 7 Link Flags (intra-link, relative, disarm, following, etc.) | Yes | Yes | DONE |
| | Link Events (Allow, Alter, Start, End, Failed) | Basic | Full hierarchy | BETTER |
| | Link Property Visuals (color gradients on panels) | Yes | No | MISSING |
| | Book Durability / Health | Yes | No | MISSING |
| **Writing System** | Writing Desk | Yes | Yes | DONE |
| | Ink Mixer | Yes | Yes | DONE |
| | Book Binder | Yes | Yes | DONE |
| | Pages / Ink Vials / Ink Fluid | Yes | Yes | DONE |
| | Folder / Portfolio | Yes | Yes | DONE |
| **Ages/Dimensions** | Dynamic dimension creation | Yes | Yes | DONE |
| | Age Data (NBT storage) | Yes | Yes | DONE |
| | AgeDirector (150+ properties) | Yes | Yes | DONE |
| | 7 Dimension Types | Yes | Yes (JSON) | DONE |
| **Symbols & Grammar** | CFG Grammar Engine | Yes | Yes | DONE |
| | Terrain Symbols (6 types) | Yes | Yes | DONE |
| | Biome Controllers (10 types) | Yes | Yes | DONE |
| | Biome Symbols (all vanilla) | Yes | Yes (dynamic) | DONE |
| | Celestial Symbols (sun/moon/stars variants) | Yes | Yes | DONE |
| | Weather Symbols (9 types) | Yes | Yes | DONE |
| | Lighting Symbols (3 types) | Yes | Yes | DONE |
| | Feature Symbols (caves, ravines, islands, etc.) | Yes | Yes | DONE |
| | Structure Symbols (~5 types) | Yes | Yes (20+) | BETTER |
| | Color Symbols (sky, fog, clouds, grass, water, foliage) | Yes | Yes | DONE |
| | Modifier Symbols | Yes | Yes (100+) | DONE |
| | Block / Fluid / Ore Symbols | Partial | Yes | BETTER |
| | Timescale Symbols | No | Yes | NEW |
| **Instability** | Potion / Potion Enemy effects | Yes | Yes | DONE |
| | Lightning / Explosion / Meteor / Scorched | Yes | Yes | DONE |
| | Erosion | Yes | Yes | DONE |
| | Accelerated Ticks | Yes | Yes | DONE |
| | Crumble (falling blocks) | No | Yes | NEW |
| | White / Red / Blue / Purple Decay | Yes | Yes | DONE |
| | Black Decay | Yes | Yes | DONE |
| | Instability Bonus Manager | Yes | Yes | DONE |
| | InstabilityBlockManager / Profiler | Yes | No | MISSING |
| **Blocks** | Star Fissure | Yes | Yes | DONE |
| | Link Portal | Yes | Yes | DONE |
| | Book Receptacle | Yes | Yes | DONE |
| | Writing Desk / Ink Mixer / Book Binder | Yes | Yes | DONE |
| | Lectern / Bookstand | Yes | Yes | DONE |
| | Link Modifier | Yes | Yes | DONE |
| | Crystal | Yes | Yes | DONE |
| | Decay Block | Yes | Yes | DONE |
| | Book Display (3rd display variant) | Yes | No | MINOR |
| **Items** | All core items (books, pages, ink, folder, portfolio) | Yes | Yes | DONE |
| | Glasses (vision modifier) | Yes | Yes | DONE |
| | Booster Pack | Yes | Yes | DONE |
| | Guidebook (in-game docs) | No | Yes | NEW |
| **Villager/Archivist** | Profession registered | Yes | Yes | DONE |
| | Trades (symbol pages, link panels, items) | Yes | Needs verification | PARTIAL |
| | Archivist House in villages | Yes | Structure exists, integration unclear | PARTIAL |
| | Custom Shop GUI | Yes | No (uses vanilla) | MISSING |
| **Ink System** | Ink Vials with color | Yes | Yes | DONE |
| | Ink Mixing / Color blending | Yes | Yes | DONE |
| | Ink Fluid (placeable) | Yes | Yes | DONE |
| | Ink Effects | Basic | Yes | BETTER |
| **Star Fissure** | Block + BlockEntity + Symbol | Yes | Yes | DONE |
| | One-way portal home | Yes | Yes | DONE |
| **Crystal** | Block + Symbol + Growth | Yes | Yes | DONE |
| **Link Panels** | Basic link panel in book GUI | Yes | Yes | DONE |
| | Dynamic dimension preview | Yes | No | MISSING |
| | Link property color/gradient effects | Yes | No | MISSING |
| **Portals** | Portal creation via Book Receptacle | Yes | Yes | DONE |
| | Portal color from dimension | Yes | No (hardcoded white) | MISSING |
| | Momentum preservation | Yes | Yes | DONE |
| | IItemPortalActivator interface | Yes | No | MISSING |
| **Client Rendering** | Custom sky rendering | Yes | Yes | DONE |
| | Custom cloud rendering | Yes | Yes | DONE |
| | Celestial rendering (multi-sun/moon/stars) | Yes | Yes | DONE |
| | Dynamic color providers (sky/fog/clouds) | Yes | Yes | DONE |
| | Void plane rendering | Yes | Yes | DONE |
| | Hide horizon | Yes | Yes | DONE |
| | Custom brightness/lighting | Yes | Yes | DONE |
| | Weather rendering | Yes | Yes | DONE |
| **API/Events** | Symbol registration event | Yes | Yes | DONE |
| | Link events | Basic | Full (5 events) | BETTER |
| | Item interfaces (IItemPageProvider, etc.) | Yes (6+) | No | MISSING |
| | IMC System (blacklisting, etc.) | Yes | No | MISSING |
| | DimensionAPI / SymbolAPI / GrammarAPI hooks | Yes | No | MISSING |
| **Structures** | Abandoned Library | Yes | Yes | DONE |
| | Underground Archive | No | Yes | NEW |
| | Scattered Library | No | Yes | NEW |
| | Archivist House | Yes | Yes | DONE |
| | Loot modifiers (pages in dungeon chests) | Yes | Yes | DONE |
| **Advancements** | Advancement triggers | Limited | Yes | BETTER |
| **Commands** | /mystcraft commands | Yes | Yes | DONE |
| | Age Presets | No | Yes | NEW |

---

## What Players Would Actually Notice Is Missing

| Priority | Missing Feature | Impact |
|---|---|---|
| Medium | Portal color per-dimension | All portals are white instead of colored by age |
| Medium | Link panel visual effects | Book panels don't show property-based colors/gradients |
| Medium | Archivist village spawning | Archivists may not appear naturally in villages |
| Low | Dynamic link panel preview | Can't preview destination dimension in book |
| Low | Book display block (3rd variant) | Lectern and bookstand cover this |
| Low | Custom archivist shop GUI | Vanilla trading screen works fine |
| Low | Item API interfaces | Only matters for cross-mod integration |
| Low | IMC system | Cross-mod compat, less relevant in 1.20+ |
| Low | InstabilityBlockManager/Profiler | Performance optimization, not player-facing |
| Low | Book durability/health | Books don't degrade with use |

## Features That Exceed The Original

| Feature | What's Better |
|---|---|
| Link Events | 5-event hierarchy (Allow/Alter/Start/End/Failed) vs basic |
| Structure Symbols | 20+ types vs ~5 in original |
| Symbol Architecture | Category-based classes vs one-class-per-symbol |
| Dimension Types | JSON-based, easier to extend |
| Guidebook | In-game documentation (new) |
| Timescale Symbols | Day/night speed control (new) |
| Crumble Effect | New instability effect |
| Underground Archive | New structure |
| Scattered Library | New structure |
| Age Presets | Pre-built age configurations (new) |
| Ink Effects | Richer fluid interaction system |
| Advancements | Progression tracking |

## Verdict

The core gameplay loop -- writing ages, linking, traveling, instability, decay, symbols, grammar, star fissure, crystal, ink, all blocks/items, sky rendering, celestials -- is complete. The gaps are visual polish (portal colors, link panel effects), archivist village integration, and mod-integration APIs. Nothing that breaks the core Mystcraft experience is missing.
