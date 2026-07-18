# Project Structure & Docs Layout

## Project Directory

The project follows a multi-loader structure:

- `common/`
  Shared code (logic, mechanics, resources). Most development happens here.
- `fabric/`
  Fabric-specific implementation for 1.20.1.
- `forge/`
  Forge-specific implementation for 1.20.1.
- `ports/26.2/`
  Isolated Java 25 / Gradle 9.5.1 production build, with its own `common`,
  `fabric`, and `forge` modules for Minecraft 26.2.
- `docs/`
  Documentation (see below).

The two common source trees intentionally isolate incompatible Minecraft APIs.
Behavioral fixes that apply to both targets should be mirrored, while
loader-specific registration, networking, lifecycle, and client code stays in
the matching platform module.

## Docs Directory

This docs folder is organized for quick access:

- README.md
  Overview of available docs.
- Folder-Structure.md
  This file: quick layout and where to find things.
- Datapack-System-Detailed.md
  Full Symbols + Grammar walkthrough.
- How-To.md
  Player-facing usage steps.
- Expectations.md
  Current limitations and behavior notes.
- Configuration.md
  Config overview and tuning tips.
- Systems-Index.md
  Feature and system directory.
- ScriptedTerrain-Overview.md
  Intro to scripted terrain system.
- ScriptedTerrain-Reference.md
  Full DSL reference.
- ScriptedTerrain-Examples.md
  Example symbols.
- ScriptedTerrain-Tips.md
  Practical usage tips.
- templates/
  Copy-ready datapack templates.
