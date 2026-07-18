# JProfiler world-generation validation

Use this gate before publishing changes to Age generation, population, or
dimension lifecycle code. GameTests establish correctness; this capture checks
whether the same behavior remains practical under real chunk load.

## Start a representative client

Build and launch the target normally. For Minecraft 26.2, for example:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew runFabric26_2Client
```

In JProfiler, use **Quick Attach** and select the Minecraft client JVM (the
process whose main class is Fabric Knot Client or the Forge userdev client),
not the Gradle daemon.

## Record

1. Load a fresh test world and wait at spawn until initial generation settles.
2. Start CPU recording in sampling mode and allocation recording with object
   lifetimes disabled.
3. Create and enter representative normal, amplified, cave, skylands, void, and
   personal Ages through books.
4. In each generated Age, travel far enough to load new chunks, then revisit at
   least two earlier Ages to exercise cached and restored dimension state.
5. Stop recording after the final revisit and save the JProfiler snapshot.

Record the loader, Minecraft version, Java version, symbol/page list for each
book, travel distance, and any visible stalls alongside the snapshot. Compare
captures only when those inputs and JProfiler settings match.

## Review targets

Inspect wall time, allocation rate, retained objects, and monitor contention
under `AgeChunkGenerator`, terrain generators, biome decoration, and each
`IPopulate` implementation. Treat synchronization in population as a
correctness boundary: replace or narrow it only with equivalent deterministic
cross-chunk behavior and before/after profile evidence.
