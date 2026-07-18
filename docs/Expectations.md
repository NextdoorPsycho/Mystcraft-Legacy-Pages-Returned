# Things To Expect (Current State)

## Supported Targets

- Minecraft 1.20.1 on Fabric and Forge uses Java 17.
- Minecraft 26.2 on Fabric and Forge uses the isolated Java 25 build under
  `ports/26.2/`.
- Player usability remains book-first on both targets: write or bind a book,
  create an Age, then link into it.

## Release Expectations

- Loader-native GameTests are the minimum gate for gameplay, persistence,
  networking, data loading, and dimension contracts.
- A real client pass is required for screens, book/portal presentation,
  resource reloads, and the complete book-to-Age flow.
- World-generation throughput and allocation claims require a representative
  JProfiler capture; automated tests establish correctness, not performance.
- Back up important worlds before testing development jars or data packs.

## Gameplay Notes

- Ages are deterministic: the same symbol set produces the same generation.
- Missing or conflicting symbols add instability.
- Random Ages use the Grammar Engine to fill gaps.
- Instability scales up effects over time.

## Mod Behavior

- Datapacks control symbol definitions and can override defaults.
- Symbol blacklisting can disable overpowered symbols.
- Features and structures may vary by terrain type or biome controller.
