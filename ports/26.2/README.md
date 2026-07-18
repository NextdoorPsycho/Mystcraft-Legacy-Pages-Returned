# Minecraft 26.2 port

This is the production Minecraft 26.2 build of Mystcraft Legacy Returned. It
keeps the existing player workflow—write a descriptive book, create an Age,
and link into it—while isolating the Java 25 and modern-loader implementation
from the maintained Minecraft 1.20.1 build at the repository root.

The port is split into three modules:

- `common`: loader-neutral gameplay, world generation, networking, UI, mixins,
  resources, and GameTests.
- `fabric`: Fabric lifecycle, registration, networking, events, and client
  integration.
- `forge`: Forge lifecycle, registration, networking, events, and client
  integration.

Both loader jars embed the complete common output. There is no bootstrap-only
or compatibility-reflection layer in a production artifact.

## Toolchain

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Java | 25 |
| Gradle | 9.5.1 |
| Fabric Loom | 1.17.14 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.154.2+26.2 |
| ForgeGradle | 7.0.31 |
| Forge | 65.0.4 |
| Mixin | 0.8.7 |

These versions were selected from the current official 26.2 metadata and
loader templates:

- [Minecraft 26.2 release notes](https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2)
- [Fabric's 26.2 announcement](https://fabricmc.net/2026/06/15/262.html)
- [Fabric's dependency recommendations](https://fabricmc.net/develop/)
- [Forge Maven metadata](https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml)
- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)

## Build

Run from this directory with Java 25:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew verifyProduction
```

`verifyProduction` compiles all three modules, builds both loader jars, and
checks the packaged artifacts for representative book, Age, generator,
network, registry, screen, mixin, test, and data-pack content. It also rejects
the removed bootstrap classes, the removed reflection helper, incomplete
common output, stale placeholder metadata, and an invalid 26.2 pack range.

The production jars are written beneath:

```text
fabric/build/libs/
forge/build/libs/
```

## Runtime checks

Use the loader-native GameTest launches for data-pack, mixin, registry, and
server lifecycle validation:

```sh
./gradlew :fabric:runGametest
./gradlew :forge:runGameTestServer
```

Additional development launches are available when manual client or dedicated
server validation is needed:

```sh
./gradlew :fabric:runClient
./gradlew :fabric:runServer
./gradlew :forge:runClient
./gradlew :forge:runServer
```

Dedicated-server runs preserve Mojang's first-run EULA file. Accept it only
when you intend to operate that local run directory.

## Compatibility and world-generation gates

- Age creation validates the selected dimension type against the chunk
  generator's minimum Y and generation depth before a level is registered or
  any chunks are written.
- Normal and personal Ages have focused height-contract tests, including
  deliberate cross-mismatch failures.
- Item data, Age definitions, link permissions, packet payloads, and generated
  rules have deterministic round-trip coverage.
- Personal-Age rules no longer mutate server-global game rules.
- The same common generator and data resources are packaged for both loaders;
  loader-specific runtime tests remain the authority for parity.

Before publishing, also perform a graphical client pass on both loaders and a
representative JProfiler capture while generating and revisiting several Ages.
Compiler success and synthetic tests alone are not performance proof.
The repeatable capture procedure is documented in
[`../../docs/JProfiler-Worldgen-Validation.md`](../../docs/JProfiler-Worldgen-Validation.md).

The legacy Minecraft 1.20.1 build remains independent:

```sh
cd ../..
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew buildAllVersions
```
