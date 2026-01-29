package art.arcane.mystcraft.world.gen;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import art.arcane.mystcraft.world.gen.biome.BiomeControllerNoise;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chunk generator for Mystcraft Ages that WRAPS vanilla's NoiseBasedChunkGenerator.
 * <p>
 * For normal/amplified terrain: Delegates to vanilla's terrain generation, then applies
 * Mystcraft alterations and populators on top. This allows symbols to MODULATE vanilla
 * terrain rather than replace it entirely.
 * <p>
 * For special terrain (void, flat): Uses custom lightweight generators.
 * <p>
 * Most symbols modify vanilla generation rather than replacing it.
 */
public class AgeChunkGenerator extends ChunkGenerator {

  public static final Codec<AgeChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource),
          Codec.STRING.fieldOf("terrain_type").forGetter(gen -> gen.terrainType),
          Codec.INT.fieldOf("ground_level").forGetter(gen -> gen.groundLevel),
          Codec.INT.fieldOf("sea_level").forGetter(gen -> gen.seaLevelValue),
          Codec.BOOL.fieldOf("has_sea").forGetter(gen -> gen.hasSea),
          Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed),
          Codec.INT.fieldOf("age_uid").forGetter(gen -> gen.ageUID),
          Codec.STRING.optionalFieldOf("secondary_terrain_type", "none").forGetter(gen -> gen.secondaryTerrainType),
          Codec.STRING.optionalFieldOf("terrain_mix_mode", "none").forGetter(gen -> gen.terrainMixMode),
          Codec.BOOL.optionalFieldOf("micro_enabled", false).forGetter(gen -> gen.microDimensionsEnabled),
          Codec.INT.optionalFieldOf("micro_radius_chunks", 0).forGetter(gen -> gen.microDimensionRadiusChunks),
          Codec.INT.optionalFieldOf("micro_extra_chunks", 1).forGetter(gen -> gen.microDimensionExtraChunks)
      ).apply(instance, AgeChunkGenerator::new)
  );
  private static final int DEBUG_CHUNK_LIMIT = 50;
  // Configuration stored for serialization
  private final String terrainType;
  private final String secondaryTerrainType;
  private final String terrainMixMode;
  private final int groundLevel;
  private final int seaLevelValue;
  private final boolean hasSea;
  private final long seed;
  private final int ageUID;
  private final boolean microDimensionsEnabled;
  private final int microDimensionRadiusChunks;
  private final int microDimensionExtraChunks;
  // Debug counters for first N chunks
  private final AtomicInteger fillFromNoiseCount = new AtomicInteger(0);
  private final AtomicInteger buildSurfaceCount = new AtomicInteger(0);
  private final AtomicInteger biomeDecorationCount = new AtomicInteger(0);
  // Default block states
  private final BlockState bedrockBlock = Blocks.BEDROCK.defaultBlockState();
  // Director with registered interfaces (symbols register here)
  private AgeDirectorImpl director;
  // Vanilla generator delegates - used for normal/amplified terrain
  // volatile for safe double-check locking (no full synchronized needed)
  private volatile NoiseBasedChunkGenerator vanillaDelegate;
  private volatile RandomState vanillaRandomState;
  private volatile NoiseBasedChunkGenerator vanillaDelegate2;
  private volatile RandomState vanillaRandomState2;
  private volatile boolean delegateInitialized = false;

  /**
   * Creates a chunk generator from codec deserialization.
   */
  public AgeChunkGenerator(BiomeSource biomeSource, String terrainType, int groundLevel,
                           int seaLevel, boolean hasSea, long seed, int ageUID,
                           String secondaryTerrainType, String terrainMixMode,
                           boolean microEnabled, int microRadiusChunks, int microExtraChunks) {
    super(biomeSource);
    this.terrainType = terrainType;
    this.secondaryTerrainType = secondaryTerrainType != null ? secondaryTerrainType : "none";
    this.terrainMixMode = terrainMixMode != null ? terrainMixMode : "none";
    this.groundLevel = groundLevel;
    this.seaLevelValue = seaLevel;
    this.hasSea = hasSea;
    this.seed = seed;
    this.ageUID = ageUID;
    this.microDimensionsEnabled = microEnabled;
    this.microDimensionRadiusChunks = Math.max(0, microRadiusChunks);
    this.microDimensionExtraChunks = Math.max(0, microExtraChunks);
  }

  /**
   * Creates a chunk generator from an AgeDirector configuration.
   */
  public AgeChunkGenerator(AgeDirectorImpl director, BiomeSource biomeSource, long seed, int ageUID) {
    super(biomeSource);
    this.director = director;
    this.terrainType = director.getTerrainType();
    this.secondaryTerrainType = director.getSecondaryTerrainType();
    this.terrainMixMode = director.getTerrainMixMode();
    this.groundLevel = director.getAverageGroundLevel();
    this.seaLevelValue = director.getSeaLevel();
    this.hasSea = director.hasSea();
    this.seed = seed;
    this.ageUID = ageUID;
    this.microDimensionsEnabled = director.isMicroDimensionsEnabled();
    this.microDimensionRadiusChunks = Math.max(0, director.getMicroDimensionRadiusChunks());
    this.microDimensionExtraChunks = Math.max(0, director.getMicroDimensionExtraChunks());
  }

  /**
   * Creates a chunk generator from an AgeDirector.
   */
  public static AgeChunkGenerator fromDirector(AgeDirectorImpl director, BiomeSource biomeSource, int ageUID) {
    return new AgeChunkGenerator(director, biomeSource, director.getSeed(), ageUID);
  }

  private static boolean isNetherOrCaveTerrain(String type) {
    return "nether".equals(type) || "cave".equals(type);
  }

  private static boolean isVanillaDelegateType(String type) {
    return "normal".equals(type) || "amplified".equals(type)
        || "nether".equals(type) || "end".equals(type)
        || "cave".equals(type) || "skylands".equals(type)
        || type == null;
  }

  private boolean isChunkWithinMicroGenerationBounds(int chunkX, int chunkZ) {
    if (!microDimensionsEnabled) {
      return true;
    }
    int maxRadius = microDimensionRadiusChunks + microDimensionExtraChunks;
    return Math.abs(chunkX) <= maxRadius && Math.abs(chunkZ) <= maxRadius;
  }

  /**
   * Returns the Age seed used for deterministic population.
   */
  public long getAgeSeed() {
    return seed;
  }

  /**
   * Returns the current director, if available.
   */
  public AgeDirectorImpl getDirector() {
    return director;
  }

  /**
   * Sets the director for this generator.
   */
  public void setDirector(AgeDirectorImpl director) {
    this.director = director;
  }

  /**
   * Gets the terrain type string (e.g. "normal", "nether", "end", "void", "flat", "cave", "skylands").
   */
  public String getTerrainType() {
    return terrainType;
  }

  private boolean usesVanillaDelegate() {
    return isVanillaDelegateType(terrainType);
  }

  private art.arcane.mystcraft.api.world.logic.ITerrainGenerator getCustomTerrainGenerator() {
    if (director == null) {
      return null;
    }
    var generator = director.getTerrainGenerator();
    if (generator == null) {
      return null;
    }
    String type = generator.getType();
    if (type == null || type.isBlank()) {
      return generator;
    }
    return isVanillaDelegateType(type) ? null : generator;
  }

  private boolean hasMixMode() {
    return !"none".equals(terrainMixMode) && !"none".equals(secondaryTerrainType);
  }

  /**
   * Determines which terrain type to use for a given chunk based on the mix mode.
   * Returns true if the primary terrain should be used, false for secondary.
   */
  private boolean usePrimaryTerrain(int chunkX, int chunkZ) {
    if (!hasMixMode()) {
      return true;
    }
    switch (terrainMixMode) {
      case "checkerboard" -> {
        return ((chunkX / 64) + (chunkZ / 64)) % 2 == 0;
      }
      case "noise" -> {
        // Simple noise sampling using seed-based hash
        double nx = chunkX * 0.02;
        double nz = chunkZ * 0.02;
        double noiseVal = Math.sin(nx * 12.9898 + nz * 78.233 + seed * 0.0001) * 43758.5453;
        noiseVal = noiseVal - Math.floor(noiseVal); // Fractional part, 0-1
        return noiseVal < 0.5;
      }
      case "stripes" -> {
        return (Math.floorDiv(chunkX, 16) % 2) == 0;
      }
      default -> {
        return true;
      }
    }
  }

  /**
   * Initializes the vanilla delegate generator lazily.
   * This gets the overworld's NoiseGeneratorSettings and creates a NoiseBasedChunkGenerator.
   */
  private void ensureVanillaDelegate() {
    // Fast path: volatile read, no locking needed once initialized
    if (delegateInitialized) {
      return;
    }

    // Slow path: double-check locking to avoid full synchronization
    // on every chunk gen call (which can cause monitor deadlocks with
    // the server thread's managedBlock task pumping)
    synchronized (this) {
      if (delegateInitialized) {
        return;
      }

      String threadName = Thread.currentThread().getName();
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} ensureVanillaDelegate called on thread: {}", ageUID, threadName);

      if (!usesVanillaDelegate()) {
        delegateInitialized = true;
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} does not use vanilla delegate (type: {})", ageUID, terrainType);
        return;
      }

      MinecraftServer server = Mystcraft.getCurrentServer();
      if (server == null) {
        // Don't set delegateInitialized - retry next call when server may be available
        Mystcraft.LOGGER.warn("[ChunkGen] Age {} cannot initialize vanilla delegate: server not available (will retry) [thread: {}]",
            ageUID, threadName);
        return;
      }
      // Note: delegateInitialized set to true at the END of this block,
      // after vanillaDelegate and vanillaRandomState are fully assigned.
      // This ensures other threads see the delegate before the flag.

      try {
        // Get the appropriate NoiseGeneratorSettings for the terrain type
        Holder<NoiseGeneratorSettings> noiseSettings;
        var noiseSettingsRegistry = server.registryAccess()
            .registryOrThrow(Registries.NOISE_SETTINGS);
        if ("amplified".equals(terrainType)) {
          noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED);
        } else if ("nether".equals(terrainType) || "cave".equals(terrainType)) {
          noiseSettings = createStretchedNetherSettings(noiseSettingsRegistry);
        } else if ("end".equals(terrainType)) {
          noiseSettings = createStretchedSettings(
              noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.END).value());
        } else {
          // "normal", "skylands", and fallback all use overworld noise settings
          noiseSettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        }

        // Create vanilla generator with our biome source
        vanillaDelegate = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);

        // Create a proper RandomState with real noise settings instead of dummy
        // ServerChunkCache creates RandomState with NoiseGeneratorSettings.dummy() for
        // non-NoiseBasedChunkGenerator generators, which produces flat terrain.
        // We need a RandomState built from the actual overworld/amplified noise settings.
        vanillaRandomState = RandomState.create(
            noiseSettings.value(),
            server.registryAccess().lookupOrThrow(Registries.NOISE),
            seed
        );

        Mystcraft.LOGGER.debug("[ChunkGen] Age {} initialized vanilla terrain delegate (type: {}, noiseSettings: {})",
            ageUID, terrainType,
            "amplified".equals(terrainType) ? "AMPLIFIED" :
                ("nether".equals(terrainType) || "cave".equals(terrainType)) ? "NETHER" :
                    "end".equals(terrainType) ? "END" : "OVERWORLD");

        // Initialize secondary delegate for mixed terrain modes (independent try-catch
        // so primary delegate failure doesn't block generation entirely)
        if (hasMixMode() && isVanillaDelegateType(secondaryTerrainType)) {
          try {
            Holder<NoiseGeneratorSettings> secondarySettings;
            if ("amplified".equals(secondaryTerrainType)) {
              secondarySettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED);
            } else if ("nether".equals(secondaryTerrainType) || "cave".equals(secondaryTerrainType)) {
              secondarySettings = createStretchedNetherSettings(noiseSettingsRegistry);
            } else if ("end".equals(secondaryTerrainType)) {
              secondarySettings = createStretchedSettings(
                  noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.END).value());
            } else {
              secondarySettings = noiseSettingsRegistry.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
            }
            vanillaDelegate2 = new NoiseBasedChunkGenerator(biomeSource, secondarySettings);
            vanillaRandomState2 = RandomState.create(
                secondarySettings.value(),
                server.registryAccess().lookupOrThrow(Registries.NOISE),
                seed
            );
            Mystcraft.LOGGER.debug("[ChunkGen] Age {} initialized SECONDARY vanilla terrain delegate (type: {})",
                ageUID, secondaryTerrainType);
          } catch (Exception e) {
            Mystcraft.LOGGER.error("[ChunkGen] Age {} failed to initialize secondary delegate (type: {}), falling back to primary only",
                ageUID, secondaryTerrainType, e);
          }
        }
      } catch (Exception e) {
        Mystcraft.LOGGER.error("Failed to initialize vanilla delegate for age {}", ageUID, e);
      }

      // Always mark as initialized so we don't block all chunk gen threads
      // retrying forever if something went wrong
      delegateInitialized = true;
    }
  }

  /**
   * Gets the appropriate vanilla delegate for the given chunk position,
   * considering the terrain mix mode.
   */
  private NoiseBasedChunkGenerator getDelegateForChunk(int chunkX, int chunkZ) {
    if (hasMixMode() && !usePrimaryTerrain(chunkX, chunkZ) && vanillaDelegate2 != null) {
      return vanillaDelegate2;
    }
    return vanillaDelegate;
  }

  /**
   * Gets the appropriate RandomState for the given chunk position,
   * considering the terrain mix mode.
   */
  private RandomState getRandomStateForChunk(int chunkX, int chunkZ) {
    if (hasMixMode() && !usePrimaryTerrain(chunkX, chunkZ) && vanillaRandomState2 != null) {
      return vanillaRandomState2;
    }
    return vanillaRandomState;
  }

  /**
   * Reconstructs the director from saved AgeData.
   */
  public void reconstructDirectorFromAgeData(ServerLevel level) {
    Mystcraft.LOGGER.debug("[ChunkGen] Age {} reconstructDirectorFromAgeData called on thread: {} | director={}",
        ageUID, Thread.currentThread().getName(), director != null ? "exists" : "null");

    if (director != null) {
      return;
    }

    if (ageUID <= 0) {
      Mystcraft.LOGGER.warn("Cannot reconstruct director: no valid ageUID");
      return;
    }

    AgeData ageData = AgeData.getIfPresent(level);
    if (ageData == null) {
      Mystcraft.LOGGER.warn("Cannot reconstruct director for age {}: no AgeData found", ageUID);
      return;
    }

    List<ItemStack> pages = ageData.getPages();
    List<IAgeSymbol> symbols = new ArrayList<>();

    for (ItemStack page : pages) {
      net.minecraft.resources.ResourceLocation symbolId = Page.getSymbol(page);
      if (symbolId != null) {
        IAgeSymbol symbol = SymbolRegistry.get(symbolId);
        if (symbol != null) {
          symbols.add(symbol);
        }
      }
    }

    Mystcraft.LOGGER.debug("Reconstructing director for age {} from {} pages ({} symbols)",
        ageUID, pages.size(), symbols.size());

    AgeBuilder builder = new AgeBuilder(symbols, seed);
    this.director = builder.build();
    this.director.setInstability(ageData.getInstability());

    // Update the biome source if we have an AgeBiomeSource
    if (biomeSource instanceof AgeBiomeSource ageBiomeSource) {
      IBiomeController biomeController = director.getBiomeControllerImpl();
      if (biomeController != null) {
        ageBiomeSource.setBiomeController(biomeController);
      } else if (!director.getBiomes().isEmpty()) {
        IBiomeController defaultController = new BiomeControllerNoise(
            director.getBiomes(), seed, BiomeControllerNoise.Scale.MEDIUM);
        director.registerInterface(defaultController);
        ageBiomeSource.setBiomeController(defaultController);
      }
    }

    Mystcraft.LOGGER.info("Director reconstructed successfully for age {}", ageUID);
  }

  @Override
  protected Codec<? extends ChunkGenerator> codec() {
    return CODEC;
  }

  @Override
  public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
                           BiomeManager biomeManager, StructureManager structureManager,
                           ChunkAccess chunk, GenerationStep.Carving step) {
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    if (!isChunkWithinMicroGenerationBounds(chunkX, chunkZ)) {
      return;
    }
    ensureVanillaDelegate();

    // Use vanillaRandomState (built from real noise settings) when available,
    // otherwise fall back to the parameter's randomState (which may be dummy for
    // non-NoiseBasedChunkGenerator generators).
    RandomState effectiveState = vanillaRandomState != null ? vanillaRandomState : randomState;

    // Run vanilla carvers via delegate. This ensures caves and ravines generate.
    if (vanillaDelegate != null) {
      vanillaDelegate.applyCarvers(level, seed, effectiveState, biomeManager,
          structureManager, chunk, step);
    }
  }

  @Override
  public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                           RandomState randomState, ChunkAccess chunk) {
    if ("personal".equals(terrainType)) {
      return;
    }
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    if (!isChunkWithinMicroGenerationBounds(chunkX, chunkZ)) {
      return;
    }
    int count = buildSurfaceCount.incrementAndGet();

    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} buildSurface #{}: chunk [{}, {}] on thread: {} | delegate={}",
          ageUID, count, chunkX, chunkZ, Thread.currentThread().getName(),
          vanillaDelegate != null ? "ready" : (delegateInitialized ? "null" : "not-init"));
    }

    ensureVanillaDelegate();

    // Delegate surface building to vanilla - this applies proper biome surfaces
    // (grass, dirt, sand, etc.) on top of stone
    NoiseBasedChunkGenerator surfaceDelegate = getDelegateForChunk(chunkX, chunkZ);
    RandomState surfaceRandomState = getRandomStateForChunk(chunkX, chunkZ);
    if (surfaceDelegate != null) {
      if (count <= DEBUG_CHUNK_LIMIT) {
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} buildSurface #{}: delegating to vanilla [thread: {}]",
            ageUID, count, Thread.currentThread().getName());
      }
      surfaceDelegate.buildSurface(level, structureManager, surfaceRandomState, chunk);
      if (count <= DEBUG_CHUNK_LIMIT) {
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} buildSurface #{}: vanilla COMPLETE [thread: {}]",
            ageUID, count, Thread.currentThread().getName());
      }
    }

    // Strip bedrock from all Mystcraft Age terrain. Ages should not have
    // vanilla bedrock barriers. Must happen after vanilla surface rules.
    stripBedrockBlocks(chunk);

    // AFTER vanilla applies biome surfaces, replace remaining stone/water with
    // symbol-specified blocks. This order is critical: vanilla's surface builder
    // needs to find stone to know where to place grass/dirt. If we replaced stone
    // before surface building, surfaces would never be applied.
    if (director != null) {
      applyBlockReplacements(chunk);
      applySurfaceModifications(chunk);
    }

    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} buildSurface #{}: DONE [{}, {}]", ageUID, count, chunkX, chunkZ);
    }
  }

  /**
   * Applies Mystcraft surface modifications after vanilla surface building.
   * This is where symbols like "Sand Surface" or "Snow Surface" take effect.
   */
  private void applySurfaceModifications(ChunkAccess chunk) {
    BlockState surfaceOverride = director.getSurfaceBlock();
    BlockState subsurfaceOverride = director.getSubsurfaceBlock();

    if (surfaceOverride == null && subsurfaceOverride == null) {
      return; // No modifications needed
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = chunk.getMaxBuildHeight();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        // Find surface
        for (int y = maxY - 1; y >= minY; y--) {
          pos.set(x, y, z);
          BlockState state = chunk.getBlockState(pos);

          if (state.isAir() || state.getBlock() == Blocks.WATER) {
            continue;
          }

          // Found surface - check if it's a replaceable surface block
          if (isSurfaceBlock(state)) {
            if (surfaceOverride != null) {
              chunk.setBlockState(pos, surfaceOverride, false);
            }

            // Replace subsurface (next 3 blocks down)
            if (subsurfaceOverride != null) {
              for (int d = 1; d <= 3 && y - d >= minY; d++) {
                pos.set(x, y - d, z);
                BlockState sub = chunk.getBlockState(pos);
                if (isSubsurfaceBlock(sub)) {
                  chunk.setBlockState(pos, subsurfaceOverride, false);
                }
              }
            }
          }
          break; // Move to next column
        }
      }
    }
  }

  private boolean isSurfaceBlock(BlockState state) {
    return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) ||
        state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) ||
        state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MYCELIUM) ||
        state.is(Blocks.PODZOL) || state.is(Blocks.MUD) ||
        state.is(Blocks.END_STONE) ||
        state.is(Blocks.NETHERRACK) || state.is(Blocks.SOUL_SAND) ||
        state.is(Blocks.SOUL_SOIL) || state.is(Blocks.CRIMSON_NYLIUM) ||
        state.is(Blocks.WARPED_NYLIUM);
  }

  private boolean isSubsurfaceBlock(BlockState state) {
    return state.is(Blocks.DIRT) || state.is(Blocks.SAND) ||
        state.is(Blocks.GRAVEL) || state.is(Blocks.SANDSTONE) ||
        state.is(Blocks.MUD) ||
        state.is(Blocks.END_STONE) || state.is(Blocks.NETHERRACK);
  }

  @Override
  public void spawnOriginalMobs(WorldGenRegion level) {
    // Spawn vanilla mobs via delegate for all terrain types
    ensureVanillaDelegate();
    if (vanillaDelegate != null) {
      vanillaDelegate.spawnOriginalMobs(level);
    }
  }

  /**
   * Applies biome decoration including vanilla features AND Mystcraft populators.
   * This is the correct place for trees, ores, dungeons, etc.
   */
  @Override
  public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
    // Personal terrain is pure void - no decoration whatsoever
    if ("personal".equals(terrainType)) {
      return;
    }

    int count = biomeDecorationCount.incrementAndGet();
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    if (!isChunkWithinMicroGenerationBounds(chunkX, chunkZ)) {
      return;
    }

    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} applyBiomeDecoration #{}: chunk [{}, {}] on thread: {}",
          ageUID, count, chunkX, chunkZ, Thread.currentThread().getName());
    }

    // Run vanilla biome features (trees, flowers, ores, dungeons, etc.) via super.
    // The base ChunkGenerator.applyBiomeDecoration() uses our BiomeSource's feature
    // lists directly, so this works for ALL terrain types -- not just delegate types.
    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} applyBiomeDecoration #{}: running vanilla features [thread: {}]",
          ageUID, count, Thread.currentThread().getName());
    }
    try {
      super.applyBiomeDecoration(level, chunk, structureManager);
    } catch (Exception e) {
      Mystcraft.LOGGER.error("[ChunkGen] Age {} applyBiomeDecoration #{}: vanilla features FAILED [{}, {}]: {}",
          ageUID, count, chunkX, chunkZ, e.getMessage(), e);
    }
    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} applyBiomeDecoration #{}: vanilla features COMPLETE [thread: {}]",
          ageUID, count, Thread.currentThread().getName());
    }

    // Then apply Mystcraft populators ON TOP of vanilla
    if (director == null) {
      return;
    }

    List<IPopulate> populators = director.getPopulateFunctions();
    if (populators.isEmpty()) {
      return;
    }

    BlockPos chunkPos = new BlockPos(chunkX * 16, 0, chunkZ * 16);

    long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
    RandomSource random = RandomSource.create(chunkSeed);

    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} applyBiomeDecoration #{}: applying {} Mystcraft populators [{}, {}] [thread: {}]",
          ageUID, count, populators.size(), chunkX, chunkZ, Thread.currentThread().getName());
    }

    for (IPopulate populator : populators) {
      String popId = populator.getIdentifier();
      if (count <= DEBUG_CHUNK_LIMIT) {
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} chunk [{}, {}] >> START populator '{}' [thread: {}]",
            ageUID, chunkX, chunkZ, popId, Thread.currentThread().getName());
      }
      long startTime = System.nanoTime();
      try {
        populator.populate(level, random, chunkPos);
        if (count <= DEBUG_CHUNK_LIMIT) {
          long elapsed = (System.nanoTime() - startTime) / 1_000_000;
          Mystcraft.LOGGER.debug("[ChunkGen] Age {} chunk [{}, {}] << DONE  populator '{}' ({}ms)",
              ageUID, chunkX, chunkZ, popId, elapsed);
        }
      } catch (Exception e) {
        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        Mystcraft.LOGGER.error("[ChunkGen] Age {} chunk [{}, {}] !! ERROR populator '{}' after {}ms: {}",
            ageUID, chunkX, chunkZ, popId, elapsed, e.getMessage(), e);
      }
    }

    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} chunk [{}, {}] == ALL POPULATORS DONE ({} total) [thread: {}]",
          ageUID, chunkX, chunkZ, populators.size(), Thread.currentThread().getName());
    }
  }

  @Override
  public int getGenDepth() {
    ensureVanillaDelegate();
    if (vanillaDelegate != null) {
      return vanillaDelegate.getGenDepth();
    }
    return 384;
  }


  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                      RandomState randomState, StructureManager structureManager,
                                                      ChunkAccess chunk) {
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    if (!isChunkWithinMicroGenerationBounds(chunkX, chunkZ)) {
      return CompletableFuture.completedFuture(chunk);
    }
    int count = fillFromNoiseCount.incrementAndGet();
    String threadName = Thread.currentThread().getName();

    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: chunk [{}, {}] on thread: {} | delegate={}, director={}",
          ageUID, count, chunkX, chunkZ, threadName,
          vanillaDelegate != null ? "ready" : (delegateInitialized ? "null(failed)" : "not-init"),
          director != null ? "yes" : "no");
    }

    ensureVanillaDelegate();

    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: post-init delegate={} [thread: {}]",
          ageUID, count,
          vanillaDelegate != null ? "ready" : "null",
          threadName);
    }

    // Custom scripted terrain generator (datapack-driven)
    var customGenerator = getCustomTerrainGenerator();
    if (customGenerator != null) {
      if (hasMixMode()) {
        Mystcraft.LOGGER.warn("[ChunkGen] Age {} has custom terrain generator but mix mode is set; using custom generator only", ageUID);
      }
      if (count <= DEBUG_CHUNK_LIMIT) {
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: CUSTOM generator '{}' for [{}, {}]",
            ageUID, count, customGenerator.getType(), chunkX, chunkZ);
      }
      return CompletableFuture.supplyAsync(() -> {
        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
        RandomSource random = RandomSource.create(chunkSeed);
        customGenerator.generateTerrain(chunkX, chunkZ, chunk, random);
        applyTerrainAlterations(chunk, randomState);
        if (count <= DEBUG_CHUNK_LIMIT) {
          Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: custom generator COMPLETE for [{}, {}] [thread: {}]",
              ageUID, count, chunkX, chunkZ, Thread.currentThread().getName());
        }
        return chunk;
      }, executor);
    }

    // For normal/amplified terrain, delegate to vanilla with proper RandomState
    // Use per-chunk delegate selection for mixed terrain modes
    NoiseBasedChunkGenerator chunkDelegate = getDelegateForChunk(chunkX, chunkZ);
    RandomState chunkRandomState = getRandomStateForChunk(chunkX, chunkZ);
    if (chunkDelegate != null) {
      if (count <= DEBUG_CHUNK_LIMIT) {
        boolean isPrimary = usePrimaryTerrain(chunkX, chunkZ);
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: DELEGATING to vanilla (type: {}, primary: {}) [thread: {}]",
            ageUID, count, isPrimary ? terrainType : secondaryTerrainType, isPrimary, threadName);
      }
      return chunkDelegate.fillFromNoise(executor, blender, chunkRandomState, structureManager, chunk)
          .thenApply(filledChunk -> {
            if (count <= DEBUG_CHUNK_LIMIT) {
              Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: vanilla COMPLETE for [{}, {}] [thread: {}]",
                  ageUID, count, chunkX, chunkZ, Thread.currentThread().getName());
            }
            // Apply Mystcraft terrain alterations AFTER vanilla fills the chunk
            // Note: block replacements (terrain/sea block symbols) happen in buildSurface
            // AFTER vanilla applies biome surfaces, so surface builder can find stone
            applyTerrainAlterations(filledChunk, randomState);
            if (count <= DEBUG_CHUNK_LIMIT) {
              Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: alterations COMPLETE for [{}, {}]",
                  ageUID, count, chunkX, chunkZ);
            }
            return filledChunk;
          });
    }

    // For special terrain types (void, flat), use custom generation
    if (count <= DEBUG_CHUNK_LIMIT) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: CUSTOM generation (type: {}) for [{}, {}] [thread: {}]",
          ageUID, count, terrainType, chunkX, chunkZ, threadName);
    }
    return CompletableFuture.supplyAsync(() -> {
      generateSpecialTerrain(chunk, randomState);
      if (count <= DEBUG_CHUNK_LIMIT) {
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} fillFromNoise #{}: custom COMPLETE for [{}, {}] [thread: {}]",
            ageUID, count, chunkX, chunkZ, Thread.currentThread().getName());
      }
      return chunk;
    }, executor);
  }

  /**
   * Applies Mystcraft terrain alterations after vanilla terrain generation.
   * This includes caves, ravines, floating islands, tendrils, etc.
   */
  private void applyTerrainAlterations(ChunkAccess chunk, RandomState randomState) {
    if (director == null) {
      return;
    }

    List<ITerrainAlteration> alterations = director.getTerrainAlterations();
    if (alterations.isEmpty()) {
      return;
    }

    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
    RandomSource random = RandomSource.create(chunkSeed);

    int altCount = fillFromNoiseCount.get(); // reuse fillFromNoise counter for gating
    for (ITerrainAlteration alteration : alterations) {
      String altName = alteration.getClass().getSimpleName();
      if (altCount <= DEBUG_CHUNK_LIMIT) {
        Mystcraft.LOGGER.debug("[ChunkGen] Age {} chunk [{}, {}] >> START terrain alteration '{}' [thread: {}]",
            ageUID, chunkX, chunkZ, altName, Thread.currentThread().getName());
      }
      long startTime = System.nanoTime();
      try {
        alteration.alterTerrain(null, chunkX, chunkZ, chunk, random);
        if (altCount <= DEBUG_CHUNK_LIMIT) {
          long elapsed = (System.nanoTime() - startTime) / 1_000_000;
          Mystcraft.LOGGER.debug("[ChunkGen] Age {} chunk [{}, {}] << DONE  terrain alteration '{}' ({}ms)",
              ageUID, chunkX, chunkZ, altName, elapsed);
        }
      } catch (Exception e) {
        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        Mystcraft.LOGGER.error("[ChunkGen] Age {} chunk [{}, {}] !! ERROR terrain alteration '{}' after {}ms: {}",
            ageUID, chunkX, chunkZ, altName, elapsed, e.getMessage(), e);
      }
    }
  }

  /**
   * Replaces vanilla stone and water with symbol-specified blocks.
   * Skips replacement directly under surface blocks (grass, dirt, sand, etc.)
   * to prevent non-solid terrain blocks from collapsing surfaces.
   */
  private void applyBlockReplacements(ChunkAccess chunk) {
    if (director == null) {
      return;
    }

    BlockState terrainBlock = director.getTerrainBlock();
    BlockState seaBlock = director.getSeaBlock();

    boolean replaceStone = terrainBlock != null && !terrainBlock.is(Blocks.STONE);
    boolean replaceWater = seaBlock != null && !seaBlock.is(Blocks.WATER);

    if (!replaceStone && !replaceWater) {
      return;
    }

    // If the terrain block is not a full solid cube, we need to protect
    // the subsurface layers so that surface blocks (grass etc.) don't collapse.
    boolean terrainNeedsSurfaceProtection = replaceStone && !terrainBlock.canOcclude();

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = chunk.getMaxBuildHeight();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = minY; y < maxY; y++) {
          pos.set(x, y, z);
          BlockState state = chunk.getBlockState(pos);

          if (replaceStone && isTerrainReplaceable(state)) {
            if (terrainNeedsSurfaceProtection && isUnderSurface(chunk, x, y, z, maxY, checkPos)) {
              continue;
            }
            chunk.setBlockState(pos, terrainBlock, false);
          } else if (replaceWater && (state.is(Blocks.WATER) || state.is(Blocks.LAVA))) {
            chunk.setBlockState(pos, seaBlock, false);
          }
        }
      }
    }
  }

  /**
   * Checks if a block position is within the subsurface zone (top 5 blocks
   * below a surface block like grass, dirt, sand, etc.).
   */
  private boolean isUnderSurface(ChunkAccess chunk, int x, int y, int z, int maxY, BlockPos.MutableBlockPos checkPos) {
    for (int dy = 1; dy <= 5 && y + dy < maxY; dy++) {
      checkPos.set(x, y + dy, z);
      BlockState above = chunk.getBlockState(checkPos);
      if (above.isAir() || above.is(Blocks.WATER)) {
        return false;
      }
      if (isSurfaceBlock(above) || isSubsurfaceBlock(above)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a block is a dimension-native terrain block that should be replaced
   * when a terrain block symbol is specified. Covers overworld stone, end stone,
   * and nether terrain blocks.
   */
  private boolean isTerrainReplaceable(BlockState state) {
    return state.is(Blocks.STONE) ||
        state.is(Blocks.END_STONE) ||
        state.is(Blocks.NETHERRACK) ||
        state.is(Blocks.SOUL_SAND) ||
        state.is(Blocks.SOUL_SOIL) ||
        state.is(Blocks.BASALT);
  }

  /**
   * Creates noise settings based on vanilla NETHER but stretched to fill the full
   * dimension height (-64 to 320). Mystcraft nether Ages have no bedrock barriers
   * and terrain extends from min to max build height.
   */
  private Holder<NoiseGeneratorSettings> createStretchedNetherSettings(
      net.minecraft.core.Registry<NoiseGeneratorSettings> registry) {
    return createStretchedSettings(registry.getHolderOrThrow(NoiseGeneratorSettings.NETHER).value());
  }

  /**
   * Creates a copy of the given noise settings with the height stretched to
   * the full dimension range (-64 to 320) and noise size parameters varied
   * based on the Age seed. This gives each Age structurally different terrain
   * shapes while keeping the overall style (e.g. End islands vary in density
   * and size, Nether caves vary in proportion).
   */
  private Holder<NoiseGeneratorSettings> createStretchedSettings(NoiseGeneratorSettings vanilla) {
    int baseH = vanilla.noiseSettings().noiseSizeHorizontal();
    int baseV = vanilla.noiseSettings().noiseSizeVertical();

    // Vary noise size by +/- 25% based on the Age seed
    java.util.Random noiseRand = new java.util.Random(seed ^ 0x4E6F69736553697AL);
    int variedH = Math.max(1, baseH + noiseRand.nextInt(baseH / 2 + 1) - baseH / 4);
    int variedV = Math.max(1, baseV + noiseRand.nextInt(baseV / 2 + 1) - baseV / 4);

    // Ensure vertical noise size aligns with the dimension minY/height.
    // If cellHeight doesn't divide 64/384, vanilla noise fill can request y < minY.
    int minY = -64;
    int height = 384;
    int cellHeight = variedV * 4;
    while ((Math.floorMod(minY, cellHeight) != 0 || height % cellHeight != 0) && variedV > 1) {
      variedV--;
      cellHeight = variedV * 4;
    }

    Mystcraft.LOGGER.debug("[ChunkGen] Age {} noise variation: horizontal {} -> {}, vertical {} -> {}",
        ageUID, baseH, variedH, baseV, variedV);

    NoiseSettings stretched = NoiseSettings.create(-64, 384, variedH, variedV);
    NoiseGeneratorSettings mystcraftSettings = new NoiseGeneratorSettings(
        stretched,
        vanilla.defaultBlock(),
        vanilla.defaultFluid(),
        vanilla.noiseRouter(),
        vanilla.surfaceRule(),
        vanilla.spawnTarget(),
        vanilla.seaLevel(),
        vanilla.disableMobGeneration(),
        vanilla.aquifersEnabled(),
        vanilla.oreVeinsEnabled(),
        vanilla.useLegacyRandomSource()
    );
    return Holder.direct(mystcraftSettings);
  }

  /**
   * Strips all bedrock blocks from the chunk and replaces them with the terrain block.
   * Mystcraft Ages should not have bedrock barriers.
   */
  private void stripBedrockBlocks(ChunkAccess chunk) {
    BlockState replacement = Blocks.NETHERRACK.defaultBlockState();
    if ("personal".equals(terrainType)) {
      replacement = Blocks.AIR.defaultBlockState();
    } else if (director != null && director.getTerrainBlock() != null) {
      replacement = director.getTerrainBlock();
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = chunk.getMaxBuildHeight();

    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = minY; y < maxY; y++) {
          pos.set(x, y, z);
          if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
            chunk.setBlockState(pos, replacement, false);
          }
        }
      }
    }
  }

  /**
   * Generates special terrain types (void, flat) that don't use vanilla generation.
   */
  private void generateSpecialTerrain(ChunkAccess chunk, RandomState randomState) {
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L + seed;
    RandomSource random = RandomSource.create(chunkSeed);

    switch (terrainType) {
      case "void" -> generateVoidTerrain(chunk, random);
      case "personal" -> {
        generatePersonalTerrain(chunk, random);
        return; // Personal terrain is pure void - no alterations
      }
      case "flat" -> generateFlatTerrain(chunk, random);
      case "skygrid" -> generateSkygridTerrain(chunk, random);
      case "sponge" -> generateSpongeTerrain(chunk, random);
      case "bridges" -> generateBridgesTerrain(chunk, random);
      case "rooms" -> generateRoomsTerrain(chunk, random);
      case "tunnels" -> generateTunnelsTerrain(chunk, random);
      case "pillars" -> generatePillarsTerrain(chunk, random);
      case "checker" -> generateCheckerTerrain(chunk, random);
      case "colors" -> generateColorsTerrain(chunk, random);
      case "slime" -> generateSlimeTerrain(chunk, random);
      case "decay" -> generateDecayTerrain(chunk, random);
      case "library" -> generateLibraryTerrain(chunk, random);
      default -> {
        // Fallback to flat if unknown special type
        Mystcraft.LOGGER.warn("Unknown terrain type '{}', using flat", terrainType);
        generateFlatTerrain(chunk, random);
      }
    }

    // Apply terrain alterations for special terrain (except personal)
    applyTerrainAlterations(chunk, randomState);
  }

  private void generateVoidTerrain(ChunkAccess chunk, RandomSource random) {
    // Void terrain - completely empty, no bedrock.
    // Only generate a stone spawn platform at (0,0) in the spawn chunk
    // so the player has somewhere to stand.
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;

    if (chunkX == 0 && chunkZ == 0) {
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      BlockState platformBlock = Blocks.STONE.defaultBlockState();
      int platformY = 64;

      // 5x5 platform centered at (8, 64, 8) within the chunk
      for (int x = 6; x <= 10; x++) {
        for (int z = 6; z <= 10; z++) {
          pos.set(x, platformY, z);
          chunk.setBlockState(pos, platformBlock, false);
        }
      }
    }
    // All other chunks are pure void (air)
  }

  private void generatePersonalTerrain(ChunkAccess chunk, RandomSource random) {
    // Personal pocket: hollow rectangular structure
    // Inner void: configurable separately for XZ (horizontal) and Y (vertical)
    // Inner shell: configurable thickness with simplex-like pattern from configured palette
    // Outer shell: configurable thickness with configured block

    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    int chunkMinX = chunkX << 4;
    int chunkMinZ = chunkZ << 4;

    // Rectangular parameters from PersonalPocketDimension config
    int innerHalfXZ = art.arcane.mystcraft.world.PersonalPocketDimension.getInnerHalfSizeXZ();
    int innerHalfY = art.arcane.mystcraft.world.PersonalPocketDimension.getInnerHalfSizeY();
    int innerThick = art.arcane.mystcraft.world.PersonalPocketDimension.getInnerThickness();
    int totalHalfXZ = art.arcane.mystcraft.world.PersonalPocketDimension.getTotalHalfSizeXZ();
    int totalHalfY = art.arcane.mystcraft.world.PersonalPocketDimension.getTotalHalfSizeY();
    int centerY = art.arcane.mystcraft.world.PersonalPocketDimension.getCenterY();

    // Rectangular boundaries in world coordinates
    int boxMinX = -totalHalfXZ;
    int boxMaxX = totalHalfXZ - 1;
    int boxMinZ = -totalHalfXZ;
    int boxMaxZ = totalHalfXZ - 1;
    int boxMinY = centerY - totalHalfY;
    int boxMaxY = centerY + totalHalfY - 1;

    // Quick bounds check - skip chunks entirely outside the box
    if (chunkMinX > boxMaxX || chunkMinX + 15 < boxMinX ||
        chunkMinZ > boxMaxZ || chunkMinZ + 15 < boxMinZ) {
      return; // Chunk is outside box, leave as void
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    BlockState outerBlock = art.arcane.mystcraft.world.PersonalPocketDimension.getOuterBlock();

    // Inner block palette for simplex-like pattern
    java.util.List<BlockState> innerPalette = art.arcane.mystcraft.world.PersonalPocketDimension.getInnerBlockPalette();
    BlockState[] innerBlocks = innerPalette.toArray(new BlockState[0]);

    for (int localX = 0; localX < 16; localX++) {
      int worldX = chunkMinX + localX;
      if (worldX < boxMinX || worldX > boxMaxX) continue;

      for (int localZ = 0; localZ < 16; localZ++) {
        int worldZ = chunkMinZ + localZ;
        if (worldZ < boxMinZ || worldZ > boxMaxZ) continue;

        for (int worldY = boxMinY; worldY <= boxMaxY; worldY++) {
          // Calculate distance from center for each axis
          int distX = Math.abs(worldX);
          int distY = Math.abs(worldY - centerY);
          int distZ = Math.abs(worldZ);

          // Determine which layer this block is in (rectangular shell logic)
          // For each axis, calculate how far into the shells we are
          int layerX = distX < innerHalfXZ ? 0 : distX - innerHalfXZ;
          int layerY = distY < innerHalfY ? 0 : distY - innerHalfY;
          int layerZ = distZ < innerHalfXZ ? 0 : distZ - innerHalfXZ;
          int shellLayer = Math.max(Math.max(layerX, layerY), layerZ);

          if (shellLayer == 0) {
            // Inside inner void - leave as air
            continue;
          } else if (shellLayer <= innerThick) {
            // Inner shell layer - use simplex-like pattern for variety
            BlockState innerBlock = getSimplexInnerBlock(worldX, worldY, worldZ, innerBlocks);
            pos.set(localX, worldY, localZ);
            chunk.setBlockState(pos, innerBlock, false);
          } else {
            // Outer shell layer
            pos.set(localX, worldY, localZ);
            chunk.setBlockState(pos, outerBlock, false);
          }
        }
      }
    }
  }

  /**
   * Returns an inner block type based on simplex-like noise pattern.
   * Creates organic-looking variation in the inner shell.
   */
  private BlockState getSimplexInnerBlock(int x, int y, int z, BlockState[] innerBlocks) {
    if (innerBlocks.length == 1) {
      return innerBlocks[0];
    }
    // Simple hash-based noise for block type variation
    // Creates patches of similar block types
    double scale = 0.15;
    long hash = (long) (x * scale) * 73856093L ^ (long) (y * scale) * 19349663L ^ (long) (z * scale) * 83492791L ^ seed;
    hash = hash * 6364136223846793005L + 1442695040888963407L;
    int index = (int) ((hash & 0x7FFFFFFFL) % innerBlocks.length);
    return innerBlocks[index];
  }

  private void generateFlatTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();

    BlockState terrainBlock = director != null && director.getTerrainBlock() != null
        ? director.getTerrainBlock()
        : Blocks.STONE.defaultBlockState();

    // Flat terrain: bedrock at bottom, then terrain block up to ground level
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = minY; y < groundLevel; y++) {
          pos.set(x, y, z);
          if (y == minY) {
            chunk.setBlockState(pos, bedrockBlock, false);
          } else {
            chunk.setBlockState(pos, terrainBlock, false);
          }
        }
      }
    }
  }

  private void generateSkygridTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = chunk.getMaxBuildHeight();
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    int grid = 4;

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        if (Math.floorMod(wx, grid) != 0 || Math.floorMod(wz, grid) != 0) {
          continue;
        }
        for (int y = minY; y < maxY; y++) {
          if (Math.floorMod(y, grid) != 0) continue;
          pos.set(x, y, z);
          BlockState block = pickSkygridBlock(wx, y, wz);
          chunk.setBlockState(pos, block, false);
        }
      }
    }
  }

  private BlockState pickSkygridBlock(int x, int y, int z) {
    BlockState[] palette = new BlockState[]{
        Blocks.STONE.defaultBlockState(),
        Blocks.COBBLESTONE.defaultBlockState(),
        Blocks.DIRT.defaultBlockState(),
        Blocks.GRASS_BLOCK.defaultBlockState(),
        Blocks.OAK_PLANKS.defaultBlockState(),
        Blocks.SAND.defaultBlockState(),
        Blocks.GRAVEL.defaultBlockState(),
        Blocks.GLASS.defaultBlockState(),
        Blocks.OAK_LEAVES.defaultBlockState(),
        Blocks.OBSIDIAN.defaultBlockState(),
        Blocks.NETHERRACK.defaultBlockState(),
        Blocks.END_STONE.defaultBlockState()
    };
    long h = (long) x * 73428767L ^ (long) y * 91236781L ^ (long) z * 4236067L ^ seed;
    int idx = Math.floorMod(h, palette.length);
    return palette[idx];
  }

  private void generateSpongeTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = Math.min(chunk.getMaxBuildHeight(), groundLevel + 16);
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = minY; y < maxY; y++) {
          pos.set(x, y, z);
          if (y == minY) {
            chunk.setBlockState(pos, bedrockBlock, false);
          } else {
            chunk.setBlockState(pos, Blocks.SPONGE.defaultBlockState(), false);
          }
        }
      }
    }
  }

  private void generateBridgesTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    int bridgeY = groundLevel;
    int spacing = 32;
    int width = 2;
    BlockState block = Blocks.STONE_BRICKS.defaultBlockState();

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        boolean onX = Math.floorMod(wx, spacing) == 0;
        boolean onZ = Math.floorMod(wz, spacing) == 0;
        if (onX || onZ) {
          for (int dx = -width; dx <= width; dx++) {
            for (int dz = -width; dz <= width; dz++) {
              if (!onX && dx != 0) continue;
              if (!onZ && dz != 0) continue;
              int bx = x + dx;
              int bz = z + dz;
              if (bx < 0 || bx > 15 || bz < 0 || bz > 15) continue;
              pos.set(bx, bridgeY, bz);
              chunk.setBlockState(pos, block, false);
            }
          }
        }
      }
    }
  }

  private void generateRoomsTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = Math.min(chunk.getMaxBuildHeight(), groundLevel + 96);
    int roomSize = 16;
    int roomHeight = 8;
    BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
    BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();

    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        int localX = Math.floorMod(wx, roomSize);
        int localZ = Math.floorMod(wz, roomSize);
        boolean wallX = localX == 0 || localX == roomSize - 1;
        boolean wallZ = localZ == 0 || localZ == roomSize - 1;
        for (int y = minY; y < maxY; y++) {
          int localY = Math.floorMod(y - minY, roomHeight);
          boolean wallY = localY == 0 || localY == roomHeight - 1;
          if (wallX || wallZ || wallY) {
            pos.set(x, y, z);
            chunk.setBlockState(pos, wallY ? floor : wall, false);
          }
        }
      }
    }
  }

  private void generateTunnelsTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = Math.min(chunk.getMaxBuildHeight(), groundLevel + 96);
    int spacing = 12;
    int radius = 2;
    BlockState fill = Blocks.STONE.defaultBlockState();

    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        for (int y = minY; y < maxY; y++) {
          if (y == minY) {
            pos.set(x, y, z);
            chunk.setBlockState(pos, bedrockBlock, false);
            continue;
          }
          int dx = Math.abs(Math.floorMod(wx, spacing) - spacing / 2);
          int dz = Math.abs(Math.floorMod(wz, spacing) - spacing / 2);
          int dy = Math.abs(Math.floorMod(y - minY, spacing) - spacing / 2);
          boolean inTunnel = dx <= radius || dz <= radius || dy <= radius;
          if (!inTunnel) {
            pos.set(x, y, z);
            chunk.setBlockState(pos, fill, false);
          }
        }
      }
    }
  }

  private void generatePillarsTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = chunk.getMaxBuildHeight();
    int spacing = 8;
    int radius = 1;
    BlockState block = Blocks.POLISHED_ANDESITE.defaultBlockState();

    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        if (Math.floorMod(wx, spacing) != 0 || Math.floorMod(wz, spacing) != 0) continue;
        for (int y = minY; y < maxY; y++) {
          for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
              int bx = x + dx;
              int bz = z + dz;
              if (bx < 0 || bx > 15 || bz < 0 || bz > 15) continue;
              pos.set(bx, y, bz);
              chunk.setBlockState(pos, block, false);
            }
          }
        }
      }
    }
  }

  private void generateCheckerTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int topY = groundLevel;
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        boolean dark = ((wx + wz) & 1) == 0;
        BlockState top = dark ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState();
        for (int y = minY; y <= topY; y++) {
          pos.set(x, y, z);
          if (y == minY) {
            chunk.setBlockState(pos, bedrockBlock, false);
          } else if (y == topY) {
            chunk.setBlockState(pos, top, false);
          } else {
            chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
          }
        }
      }
    }
  }

  private void generateColorsTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int topY = groundLevel;
    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;
    BlockState[] palette = new BlockState[]{
        Blocks.RED_CONCRETE.defaultBlockState(),
        Blocks.ORANGE_CONCRETE.defaultBlockState(),
        Blocks.YELLOW_CONCRETE.defaultBlockState(),
        Blocks.LIME_CONCRETE.defaultBlockState(),
        Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
        Blocks.BLUE_CONCRETE.defaultBlockState(),
        Blocks.PURPLE_CONCRETE.defaultBlockState(),
        Blocks.MAGENTA_CONCRETE.defaultBlockState()
    };

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        long h = seed ^ (long) wx * 73428767L ^ (long) wz * 91236781L;
        BlockState top = palette[Math.floorMod(h, palette.length)];
        for (int y = minY; y <= topY; y++) {
          pos.set(x, y, z);
          if (y == minY) {
            chunk.setBlockState(pos, bedrockBlock, false);
          } else if (y == topY) {
            chunk.setBlockState(pos, top, false);
          } else {
            chunk.setBlockState(pos, Blocks.WHITE_TERRACOTTA.defaultBlockState(), false);
          }
        }
      }
    }
  }

  private void generateSlimeTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int topY = groundLevel;
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = minY; y <= topY; y++) {
          pos.set(x, y, z);
          if (y == minY) {
            chunk.setBlockState(pos, bedrockBlock, false);
          } else if (y == topY) {
            chunk.setBlockState(pos, Blocks.SLIME_BLOCK.defaultBlockState(), false);
          } else {
            chunk.setBlockState(pos, Blocks.SLIME_BLOCK.defaultBlockState(), false);
          }
        }
      }
    }
  }

  private void generateDecayTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int topY = groundLevel;
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        for (int y = minY; y <= topY; y++) {
          pos.set(x, y, z);
          if (y == minY) {
            chunk.setBlockState(pos, bedrockBlock, false);
          } else {
            chunk.setBlockState(pos, art.arcane.mystcraft.registry.ModBlocks.DECAY.get().defaultBlockState(), false);
          }
        }
      }
    }
  }

  private void generateLibraryTerrain(ChunkAccess chunk, RandomSource random) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int minY = chunk.getMinBuildHeight();
    int maxY = Math.min(chunk.getMaxBuildHeight(), groundLevel + 80);
    int roomSize = 12;
    int roomHeight = 7;

    int chunkX = chunk.getPos().x;
    int chunkZ = chunk.getPos().z;

    for (int x = 0; x < 16; x++) {
      int wx = chunkX * 16 + x;
      for (int z = 0; z < 16; z++) {
        int wz = chunkZ * 16 + z;
        int localX = Math.floorMod(wx, roomSize);
        int localZ = Math.floorMod(wz, roomSize);
        boolean wallX = localX == 0 || localX == roomSize - 1;
        boolean wallZ = localZ == 0 || localZ == roomSize - 1;
        for (int y = minY; y < maxY; y++) {
          int localY = Math.floorMod(y - minY, roomHeight);
          boolean wallY = localY == 0 || localY == roomHeight - 1;
          if (wallX || wallZ || wallY) {
            pos.set(x, y, z);
            if (wallY) {
              chunk.setBlockState(pos, Blocks.DARK_OAK_PLANKS.defaultBlockState(), false);
            } else {
              chunk.setBlockState(pos, Blocks.BOOKSHELF.defaultBlockState(), false);
            }
          }
        }
      }
    }
  }

  @Override
  public int getSeaLevel() {
    ensureVanillaDelegate();
    if (vanillaDelegate != null) {
      return vanillaDelegate.getSeaLevel();
    }
    return seaLevelValue;
  }

  @Override
  public int getMinY() {
    ensureVanillaDelegate();
    if (vanillaDelegate != null) {
      return vanillaDelegate.getMinY();
    }
    return -64;
  }

  @Override
  public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
    if (!delegateInitialized) {
      Mystcraft.LOGGER.debug("[ChunkGen] Age {} getBaseHeight called BEFORE delegate init at ({},{}) on thread: {}",
          ageUID, x, z, Thread.currentThread().getName());
    }
    ensureVanillaDelegate();
    int chunkX = x >> 4;
    int chunkZ = z >> 4;
    if (!isChunkWithinMicroGenerationBounds(chunkX, chunkZ)) {
      return level.getMinBuildHeight();
    }
    NoiseBasedChunkGenerator heightDelegate = getDelegateForChunk(chunkX, chunkZ);
    RandomState heightRandomState = getRandomStateForChunk(chunkX, chunkZ);
    if (heightDelegate != null) {
      return heightDelegate.getBaseHeight(x, z, type, level, heightRandomState);
    }

    // Fallback for special terrain
    if ("void".equals(terrainType)) {
      return level.getMinBuildHeight();
    }
    return groundLevel;
  }

  @Override
  public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
    ensureVanillaDelegate();
    int chunkX = x >> 4;
    int chunkZ = z >> 4;
    if (!isChunkWithinMicroGenerationBounds(chunkX, chunkZ)) {
      BlockState[] states = new BlockState[level.getHeight()];
      BlockState air = Blocks.AIR.defaultBlockState();
      for (int i = 0; i < states.length; i++) {
        states[i] = air;
      }
      return new NoiseColumn(level.getMinBuildHeight(), states);
    }
    if (vanillaDelegate != null) {
      return vanillaDelegate.getBaseColumn(x, z, level, vanillaRandomState);
    }

    // Fallback for special terrain
    BlockState[] states = new BlockState[level.getHeight()];
    int minY = level.getMinBuildHeight();

    for (int i = 0; i < states.length; i++) {
      int y = minY + i;
      if (y == minY) {
        states[i] = bedrockBlock;
      } else if ("void".equals(terrainType)) {
        states[i] = Blocks.AIR.defaultBlockState();
      } else if (y < groundLevel) {
        states[i] = Blocks.STONE.defaultBlockState();
      } else {
        states[i] = Blocks.AIR.defaultBlockState();
      }
    }

    return new NoiseColumn(minY, states);
  }

  @Override
  public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    info.add("Mystcraft Age - Terrain: " + terrainType + (vanillaDelegate != null ? " (vanilla delegate)" : ""));
    if (hasMixMode()) {
      info.add("Mix Mode: " + terrainMixMode + ", Secondary: " + secondaryTerrainType);
    }
    info.add("Ground Level: " + groundLevel + ", Sea Level: " + seaLevelValue);
    if (director != null) {
      info.add("Alterations: " + director.getTerrainAlterations().size());
      info.add("Populators: " + director.getPopulateFunctions().size());
    }
  }

  public int getAgeUID() {
    return ageUID;
  }

  public boolean needsDirectorReconstruction() {
    return director == null && ageUID > 0;
  }
}
