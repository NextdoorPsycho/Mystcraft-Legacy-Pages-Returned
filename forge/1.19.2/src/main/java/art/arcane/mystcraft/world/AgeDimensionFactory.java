package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.gen.biome.*;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Factory for dynamically creating Mystcraft Age dimensions at runtime.
 * Uses reflection to access MinecraftServer internals for dimension registration.
 * Access Transformers/Wideners ensure the reflection works at runtime.
 *
 * Ported to 1.19.2 API.
 */
public class AgeDimensionFactory {

  private static final String DIMENSION_PREFIX = "mystcraft_age_";

  // Dimension type resource keys for different age styles
  private static final ResourceKey<DimensionType> DIM_TYPE_NORMAL =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_normal"));
  private static final ResourceKey<DimensionType> DIM_TYPE_NETHER =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_nether"));
  private static final ResourceKey<DimensionType> DIM_TYPE_END =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_end"));
  private static final ResourceKey<DimensionType> DIM_TYPE_DARK =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_dark"));
  private static final ResourceKey<DimensionType> DIM_TYPE_BRIGHT =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_bright"));
  private static final ResourceKey<DimensionType> DIM_TYPE_CAVE =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_cave"));
  private static final ResourceKey<DimensionType> DIM_TYPE_SKYLANDS =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_skylands"));
  private static final ResourceKey<DimensionType> DIM_TYPE_PERSONAL =
      ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, "age_personal"));

  // Field names as they appear in the decompiled MC source (official mappings)
  private static final String FIELD_EXECUTOR = "executor";
  private static final String FIELD_LEVELS = "levels";
  private static final String FIELD_STORAGE_SOURCE = "storageSource";
  private static final String FIELD_FROZEN = "frozen";
  /**
   * Avoid spawning players in the upper/lower edges of the dimension.
   */
  private static final int SPAWN_EDGE_MARGIN = 50;

  /**
   * Gets a field value using reflection, trying the field name first.
   */
  @SuppressWarnings("unchecked")
  private static <T> T getFieldValue(Object obj, String fieldName, Class<T> type) {
    try {
      Field field = findField(obj.getClass(), fieldName, type);
      if (field != null) {
        field.setAccessible(true);
        return (T) field.get(obj);
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to get field {} from {}: {}", fieldName, obj.getClass().getName(), e.getMessage());
    }
    return null;
  }

  /**
   * Sets a field value using reflection.
   */
  private static void setFieldValue(Object obj, String fieldName, Object value) {
    try {
      Field field = findField(obj.getClass(), fieldName, value.getClass());
      if (field != null) {
        field.setAccessible(true);
        field.set(obj, value);
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to set field {} on {}: {}", fieldName, obj.getClass().getName(), e.getMessage());
    }
  }

  /**
   * Finds a field by name in a class hierarchy.
   */
  private static Field findField(Class<?> clazz, String name, Class<?> type) {
    Class<?> current = clazz;
    while (current != null) {
      for (Field field : current.getDeclaredFields()) {
        if (field.getName().equals(name)) {
          return field;
        }
        // Fallback: match by type if name doesn't match (for obfuscated environments)
        if (type != null && type.isAssignableFrom(field.getType())) {
          return field;
        }
      }
      current = current.getSuperclass();
    }
    return null;
  }

  /**
   * Creates a new Age dimension and returns the ServerLevel.
   *
   * @param server  The Minecraft server
   * @param ageUID  The unique age identifier
   * @param ageUUID The unique age UUID (for the dimension key)
   * @return The created ServerLevel, or null if creation failed
   */
  @Nullable
  public static ServerLevel createAgeDimension(@NotNull MinecraftServer server, int ageUID, @NotNull UUID ageUUID) {
    ResourceLocation dimensionId = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
    ResourceKey<Level> dimensionKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionId);

    // Check if dimension already exists
    ServerLevel existing = server.getLevel(dimensionKey);
    if (existing != null) {
      Mystcraft.LOGGER.info("Age {} already exists, returning existing level", ageUID);
      return existing;
    }

    try {
      return createAndRegisterWorld(server, dimensionKey, ageUID, ageUUID);
    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to create Age dimension {}", ageUID, e);
      return null;
    }
  }

  /**
   * Gets an existing Age dimension or creates a new one.
   */
  @Nullable
  public static ServerLevel getOrCreateAgeDimension(@NotNull MinecraftServer server, int ageUID) {
    AgeManager ageManager = AgeManager.get(server);
    ResourceLocation dimLoc = ageManager.getDimension(ageUID);

    if (dimLoc != null) {
      // Age exists in registry, try to get or load it
      ResourceKey<Level> dimensionKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimLoc);
      ServerLevel level = server.getLevel(dimensionKey);
      if (level != null) {
        return level;
      }
      // Level not loaded, need to create it
      // Get UUID from AgeData if available
      UUID ageUUID = UUID.randomUUID(); // Fallback
      return createAndRegisterWorld(server, dimensionKey, ageUID, ageUUID);
    }

    // Age doesn't exist, create new
    UUID newUUID = UUID.randomUUID();
    ServerLevel level = createAgeDimension(server, ageUID, newUUID);
    if (level != null) {
      ResourceLocation newDimLoc = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
      ageManager.registerAge(ageUID, newDimLoc, newUUID);
    }
    return level;
  }

  /**
   * Creates and registers a new world for the given dimension key.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  private static ServerLevel createAndRegisterWorld(
      @NotNull MinecraftServer server,
      @NotNull ResourceKey<Level> dimensionKey,
      int ageUID,
      @NotNull UUID ageUUID
  ) {
    return createAndRegisterWorld(server, dimensionKey, ageUID, ageUUID, null);
  }

  /**
   * Creates and registers a new world with optional AgeDirector configuration.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  private static ServerLevel createAndRegisterWorld(
      @NotNull MinecraftServer server,
      @NotNull ResourceKey<Level> dimensionKey,
      int ageUID,
      @NotNull UUID ageUUID,
      @Nullable AgeDirectorImpl director
  ) {
    try {
      // Access server internals via reflection (AT/AW ensures this works at runtime)
      // Use background executor directly to avoid potential deadlock with Render thread in Integrated Server
      Executor executor = net.minecraft.Util.backgroundExecutor();
      Map<ResourceKey<Level>, ServerLevel> levels = getFieldValue(server, FIELD_LEVELS, Map.class);
      LevelStorageSource.LevelStorageAccess storageSource = getFieldValue(server, FIELD_STORAGE_SOURCE,
          LevelStorageSource.LevelStorageAccess.class);

      if (executor == null || levels == null || storageSource == null) {
        Mystcraft.LOGGER.error("Failed to access MinecraftServer internals for dimension creation");
        return null;
      }

      // Get overworld for reference
      ServerLevel overworld = server.overworld();
      ServerLevelData overworldData = (ServerLevelData) overworld.getLevelData();

      // Create level stem for the new dimension with director configuration
      LevelStem levelStem = createLevelStem(server, ageUID, director);
      if (levelStem == null) {
        Mystcraft.LOGGER.error("Failed to create LevelStem for Age {}", ageUID);
        return null;
      }

      // Register the dimension in the registry
      ResourceKey<LevelStem> stemKey = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, dimensionKey.location());
      Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registry.LEVEL_STEM_REGISTRY);

      // Use reflection to register if it's a MappedRegistry
      if (stemRegistry instanceof MappedRegistry<LevelStem> mappedRegistry) {
        registerDimensionStem(mappedRegistry, stemKey, levelStem);
      }

      // Create derived level data
      DerivedLevelData derivedData = new DerivedLevelData(
          server.getWorldData(),
          overworldData
      );

      // Create the world seed
      long seed = BiomeManager.obfuscateSeed(server.getWorldData().worldGenSettings().seed()) + ageUID;

      // Create progress listener (use no-op for dynamic dimensions)
      ChunkProgressListener progressListener = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(net.minecraft.world.level.ChunkPos pos) {
        }

        @Override
        public void onStatusChange(net.minecraft.world.level.ChunkPos pos, @Nullable net.minecraft.world.level.chunk.ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
      };

      // Create the new ServerLevel (1.19.2 constructor differs from 1.20.x)
      ServerLevel newLevel = new ServerLevel(
          server,
          executor,
          storageSource,
          derivedData,
          dimensionKey,
          levelStem,
          progressListener,
          false, // isDebug
          seed,
          List.of(), // customSpawners
          false // tickTime
      );

      // Initialize world border
      // Personal pockets get their own fixed border, other Ages sync from overworld
      var overworldBorder = overworld.getWorldBorder();
      var newBorder = newLevel.getWorldBorder();

      boolean isPersonalPocket = director != null && director.isPersonalPocket();
      boolean isMicroDimension = director != null && director.isMicroDimensionsEnabled();
      if (isPersonalPocket) {
        // Personal pockets have a fixed small border centered at origin
        newBorder.setCenter(0.0, 0.0);
        newBorder.setSize(160);
        newBorder.setDamagePerBlock(0.0);
        newBorder.setDamageSafeZone(0.0);
        newBorder.setWarningBlocks(15);
        newBorder.setWarningTime(15);
        // Do NOT add border listener - personal pocket border is independent
      } else if (isMicroDimension) {
        int radiusChunks = Math.max(0, director.getMicroDimensionRadiusChunks());
        int borderSizeBlocks = (radiusChunks * 2 + 1) * 16;
        newBorder.setCenter(8.0, 8.0);
        newBorder.setSize(borderSizeBlocks);
        newBorder.setDamagePerBlock(0.0);
        newBorder.setDamageSafeZone(0.0);
        newBorder.setWarningBlocks(0);
        newBorder.setWarningTime(0);
      } else {
        // Regular Ages sync border from overworld
        newBorder.setCenter(overworldBorder.getCenterX(), overworldBorder.getCenterZ());
        newBorder.setSize(overworldBorder.getSize());
        newBorder.setDamagePerBlock(overworldBorder.getDamagePerBlock());
        newBorder.setDamageSafeZone(overworldBorder.getDamageSafeZone());
        newBorder.setWarningBlocks(overworldBorder.getWarningBlocks());
        newBorder.setWarningTime(overworldBorder.getWarningTime());
        // Add world border listener to sync future changes from overworld
        overworldBorder.addListener(new BorderChangeListener.DelegateBorderChangeListener(newBorder));
      }

      // Register the level with the server
      levels.put(dimensionKey, newLevel);

      // Mark worlds as dirty (method is public, call directly)
      try {
        java.lang.reflect.Method markDirty = MinecraftServer.class.getMethod("markWorldsDirty");
        markDirty.invoke(server);
      } catch (NoSuchMethodException e) {
        // Method may not exist in all versions, ignore
        Mystcraft.LOGGER.debug("markWorldsDirty() not available");
      } catch (Exception e) {
        Mystcraft.LOGGER.warn("Failed to mark worlds dirty: {}", e.getMessage());
      }

      // Disable vanilla death messages in Mystcraft Ages (custom messages only).
      art.arcane.mystcraft.event.AgeDeathHandler.configureAgeGameRules(newLevel);

      // Fire level load event through platform service
      Services.EVENTS.fireLevelLoadEvent(newLevel);

      Mystcraft.LOGGER.info("Created Age dimension {} at {}", ageUID, dimensionKey.location());

      // Initialize AgeData for this level
      AgeData ageData = AgeData.get(newLevel);
      ageData.setAgeUID(ageUID);
      ageData.setAgeUUID(ageUUID);
      applyMicroDimensionBorder(newLevel, ageData);

      ChunkGenerator generator = newLevel.getChunkSource().getGenerator();
      if (generator instanceof AgeChunkGenerator ageGen) {
        ageGen.ensurePocketHeadLoaded(newLevel);
      }

      return newLevel;

    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to create world for Age {}", ageUID, e);
      return null;
    }
  }

  /**
   * Creates a LevelStem for a new Age using the default overworld template.
   */
  @Nullable
  private static LevelStem createLevelStem(@NotNull MinecraftServer server, int ageUID) {
    return createLevelStem(server, ageUID, null);
  }

  /**
   * Creates a LevelStem for a new Age with optional AgeDirector configuration.
   */
  @Nullable
  private static LevelStem createLevelStem(@NotNull MinecraftServer server, int ageUID, @Nullable AgeDirectorImpl director) {
    try {
      // Get the overworld stem as a template
      Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registry.LEVEL_STEM_REGISTRY);
      LevelStem overworldStem = stemRegistry.get(LevelStem.OVERWORLD);

      if (overworldStem == null) {
        Mystcraft.LOGGER.error("Overworld stem not found");
        return null;
      }

      // Create biome source based on director configuration
      BiomeSource biomeSource;
      if (director != null) {
        // Get the biome controller registered by symbols
        IBiomeController biomeController = director.getBiomeControllerImpl();
        List<Holder<Biome>> directorBiomes = director.getBiomes();

        // Check if controller exists but has empty biomes while director has biomes
        // This can happen if the biome controller symbol was applied before biome symbols
        if (biomeController != null && biomeController.getBiomes().isEmpty() && !directorBiomes.isEmpty()) {
          Mystcraft.LOGGER.info("Age {} controller has 0 biomes but director has {}, recreating controller",
              ageUID, directorBiomes.size());
          // Recreate the controller with the actual biomes
          String controllerType = biomeController.getType();
          biomeController = switch (controllerType) {
            case "single" -> new BiomeControllerSingle(directorBiomes, director.getSeed());
            case "tiny" -> new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.TINY);
            case "small" -> new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.SMALL);
            case "medium" -> new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.MEDIUM);
            case "large" -> new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.LARGE);
            case "huge" -> new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.HUGE);
            case "tiled" -> new BiomeControllerTiled(directorBiomes, director.getSeed());
            case "grid" -> new BiomeControllerGrid(directorBiomes, director.getSeed());
            case "shuffle" -> new BiomeControllerShuffle(directorBiomes, director.getSeed());
            default -> new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.MEDIUM);
          };
          director.registerInterface(biomeController);
        }

        if (biomeController != null && !biomeController.getBiomes().isEmpty()) {
          // Use the director's biome controller wrapped in AgeBiomeSource
          biomeSource = new AgeBiomeSource(biomeController, director.getSeed());
          Mystcraft.LOGGER.info("Created Age {} with biome controller type: {} ({} biomes)",
              ageUID, biomeController.getType(), biomeController.getBiomes().size());
        } else if (!directorBiomes.isEmpty()) {
          // No controller but biomes were added - create a default medium noise controller
          Mystcraft.LOGGER.info("Age {} has {} biomes but no controller, creating default noise controller",
              ageUID, directorBiomes.size());
          IBiomeController defaultController = new BiomeControllerNoise(
              directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.MEDIUM);
          director.registerInterface(defaultController);
          biomeSource = new AgeBiomeSource(defaultController, director.getSeed());
        } else {
          // No biomes or controller - use overworld biome source as fallback
          Mystcraft.LOGGER.warn("Age {} has no biomes or controller, using overworld biomes", ageUID);
          biomeSource = overworldStem.generator().getBiomeSource();
        }
      } else {
        // No director - use overworld biome source
        biomeSource = overworldStem.generator().getBiomeSource();
      }

      // Create chunk generator based on director configuration
      ChunkGenerator generator;
      if (director != null) {
        // Use custom Age chunk generator with director settings
        generator = AgeChunkGenerator.fromDirector(director, biomeSource, ageUID);
        Mystcraft.LOGGER.info("Created Age {} with terrain type: {}", ageUID, director.getTerrainType());
      } else {
        // Use overworld generator as fallback
        generator = overworldStem.generator();
      }

      // Select the appropriate dimension type based on director settings
      // 1.19.2: LevelStem uses typeHolder() accessor instead of type()
      Holder<DimensionType> dimensionType = selectDimensionType(server, director, overworldStem.typeHolder());

      return new LevelStem(
          dimensionType,
          generator
      );
    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to create LevelStem", e);
      return null;
    }
  }

  /**
   * Selects the appropriate dimension type based on AgeDirector settings.
   * Falls back to the default (overworld) type if custom types aren't available.
   *
   * @param server      The Minecraft server
   * @param director    The AgeDirector with age configuration (can be null)
   * @param defaultType The default dimension type to use if no custom type matches
   * @return The selected dimension type holder
   */
  private static Holder<DimensionType> selectDimensionType(
      @NotNull MinecraftServer server,
      @Nullable AgeDirectorImpl director,
      @NotNull Holder<DimensionType> defaultType
  ) {
    if (director == null) {
      return defaultType;
    }

    // Determine which dimension type to use based on director properties
    ResourceKey<DimensionType> selectedKey = determineDimensionTypeKey(director);

    // Try to get the dimension type from the registry
    Registry<DimensionType> dimTypeRegistry = server.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
    Holder<DimensionType> holder = dimTypeRegistry.getHolder(selectedKey).orElse(null);

    if (holder != null) {
      Mystcraft.LOGGER.debug("Using dimension type {} for age", selectedKey.location());
      return holder;
    }

    // Fallback to default if custom type not found
    Mystcraft.LOGGER.warn("Dimension type {} not found, using default", selectedKey.location());
    return defaultType;
  }

  /**
   * Determines which dimension type resource key to use based on AgeDirector settings.
   * <p>
   * Priority order:
   * 1. Nether-style (nether terrain type or nether fort enabled)
   * 2. End-style (end terrain type)
   * 3. Cave-style (cave terrain or skylands disabled with no sea)
   * 4. Skylands (floating islands or skylands terrain)
   * 5. Bright (lighting type is "bright")
   * 6. Dark (lighting type is "dark" or no sun/moon)
   * 7. Normal (default)
   */
  private static ResourceKey<DimensionType> determineDimensionTypeKey(@NotNull AgeDirectorImpl director) {
    // Personal pockets use dedicated dimension type (eternal night, full brightness)
    if (director.isPersonalPocket()) {
      return DIM_TYPE_PERSONAL;
    }

    String terrainType = director.getTerrainType();
    String lightingType = director.getLightingType();

    // Check for nether-style age
    if ("nether".equalsIgnoreCase(terrainType) || director.isNetherFortEnabled()) {
      return DIM_TYPE_NETHER;
    }

    // Check for end-style age
    if ("end".equalsIgnoreCase(terrainType)) {
      return DIM_TYPE_END;
    }

    // Check for cave-style age (has ceiling, no skylight)
    if ("cave".equalsIgnoreCase(terrainType)) {
      return DIM_TYPE_CAVE;
    }

    // Check for skylands
    if (director.areSkylandsEnabled() || director.areFloatingIslandsEnabled() || "skylands".equalsIgnoreCase(terrainType)) {
      return DIM_TYPE_SKYLANDS;
    }

    // Check lighting type for bright/dark variants
    if ("bright".equalsIgnoreCase(lightingType)) {
      return DIM_TYPE_BRIGHT;
    }

    if ("dark".equalsIgnoreCase(lightingType)) {
      return DIM_TYPE_DARK;
    }

    // Check celestial visibility - no sun AND no moon suggests a dark world
    if (!director.isSunVisible() && !director.isMoonVisible()) {
      return DIM_TYPE_DARK;
    }

    // Default to normal
    return DIM_TYPE_NORMAL;
  }

  /**
   * Creates an Age dimension with a specific AgeDirector configuration.
   */
  @Nullable
  public static ServerLevel createAgeDimension(@NotNull MinecraftServer server, int ageUID,
                                               @NotNull UUID ageUUID, @Nullable AgeDirectorImpl director) {
    ResourceLocation dimensionId = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
    ResourceKey<Level> dimensionKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionId);

    // Check if dimension already exists
    ServerLevel existing = server.getLevel(dimensionKey);
    if (existing != null) {
      Mystcraft.LOGGER.info("Age {} already exists, returning existing level", ageUID);
      return existing;
    }

    try {
      return createAndRegisterWorld(server, dimensionKey, ageUID, ageUUID, director);
    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to create Age dimension {}", ageUID, e);
      return null;
    }
  }

  /**
   * Registers a dimension stem in the registry.
   * Uses reflection with AT/AW ensuring access at runtime.
   */
  private static void registerDimensionStem(
      MappedRegistry<LevelStem> registry,
      ResourceKey<LevelStem> key,
      LevelStem stem
  ) {
    try {
      // Find and unfreeze the registry
      Field frozenField = findField(MappedRegistry.class, FIELD_FROZEN, boolean.class);
      if (frozenField != null) {
        frozenField.setAccessible(true);
        frozenField.set(registry, false);
      }

      // Register the stem
      registry.register(key, stem, Lifecycle.stable());

      // Re-freeze the registry
      if (frozenField != null) {
        frozenField.set(registry, true);
      }

    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to register dimension stem", e);
    }
  }

  /**
   * Checks if a dimension key belongs to a Mystcraft Age.
   */
  public static boolean isMystcraftAge(@NotNull ResourceKey<Level> dimensionKey) {
    return dimensionKey.location().getNamespace().equals(Mystcraft.MOD_ID)
        && dimensionKey.location().getPath().startsWith(DIMENSION_PREFIX);
  }

  /**
   * Extracts the Age UID from a dimension key, or -1 if not a Mystcraft Age.
   */
  public static int getAgeUID(@NotNull ResourceKey<Level> dimensionKey) {
    return getAgeUID(dimensionKey.location());
  }

  /**
   * Extracts the Age UID from a dimension id, or -1 if not a Mystcraft Age.
   */
  public static int getAgeUID(@NotNull ResourceLocation dimensionId) {
    if (!dimensionId.getNamespace().equals(Mystcraft.MOD_ID)) {
      return -1;
    }
    String path = dimensionId.getPath();
    if (!path.startsWith(DIMENSION_PREFIX)) {
      return -1;
    }
    try {
      return Integer.parseInt(path.substring(DIMENSION_PREFIX.length()));
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Gets the spawn position for an Age.
   * Uses intelligent search to find safe ground if no spawn is set.
   * If no dry land exists, builds a small platform above the fluid surface.
   */
  @NotNull
  public static BlockPos getAgeSpawn(@NotNull ServerLevel level) {
    AgeData ageData = AgeData.getIfPresent(level);
    if (ageData != null && ageData.isSpawnSet()) {
      return new BlockPos(ageData.getSpawnX(), ageData.getSpawnY(), ageData.getSpawnZ());
    }

    // Try to find a safe spawn near world origin
    BlockPos safeSpawn = findSafeSpawnNear(level, BlockPos.ZERO, 64);
    if (safeSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(safeSpawn.getX(), safeSpawn.getY(), safeSpawn.getZ());
      }
      return safeSpawn;
    }

    // Fallback to world spawn
    BlockPos worldSpawn = level.getSharedSpawnPos();

    // Try to find safe spawn near world spawn
    safeSpawn = findSafeSpawnNear(level, worldSpawn, 32);
    if (safeSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(safeSpawn.getX(), safeSpawn.getY(), safeSpawn.getZ());
      }
      return safeSpawn;
    }

    // No dry land found - find a fluid surface and build a platform
    BlockPos platformSpawn = buildSpawnPlatform(level, BlockPos.ZERO);
    if (platformSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(platformSpawn.getX(), platformSpawn.getY(), platformSpawn.getZ());
      }
      return platformSpawn;
    }

    // Void dimension fallback - place a single stone block for the player to stand on
    BlockPos voidSpawn = buildVoidSpawnBlock(level, BlockPos.ZERO);
    if (voidSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(voidSpawn.getX(), voidSpawn.getY(), voidSpawn.getZ());
      }
      return voidSpawn;
    }

    // Last resort: return world spawn
    return worldSpawn;
  }

  /**
   * Places a single stone block for void dimensions with nothing to stand on.
   * Used as a last resort when no terrain, fluid, or solid ground exists.
   */
  @Nullable
  private static BlockPos buildVoidSpawnBlock(@NotNull ServerLevel level, @NotNull BlockPos center) {
    if (!level.hasChunk(center.getX() >> 4, center.getZ() >> 4)) {
      return null;
    }

    int minY = level.getMinBuildHeight();
    int maxY = level.getMaxBuildHeight();

    // Place the stone block in the middle of the safe Y range
    int safeMinY = minY + SPAWN_EDGE_MARGIN;
    int safeMaxY = maxY - SPAWN_EDGE_MARGIN;
    int spawnY = (safeMinY + safeMaxY) / 2;

    // Clamp to a reasonable height if the dimension is very tall
    spawnY = Math.max(safeMinY, Math.min(128, spawnY));

    BlockPos stonePos = new BlockPos(center.getX(), spawnY, center.getZ());
    BlockPos spawnPos = stonePos.above();

    // Place a single stone block
    level.setBlock(stonePos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);

    Mystcraft.LOGGER.info("Built void spawn block at {} (void dimension fallback)", stonePos);
    return spawnPos;
  }

  /**
   * Applies a fixed micro-dimension world border for new Ages (if enabled in AgeData).
   * Uses the spawn chunk center as the border center and aligns to chunk edges.
   */
  public static void applyMicroDimensionBorder(@NotNull ServerLevel level, @NotNull AgeData ageData) {
    if (ageData.isPersonalPocket() || !ageData.isMicroDimensionsEnabled()) {
      return;
    }

    int radiusChunks = Math.max(0, ageData.getMicroDimensionRadiusChunks());
    int borderSizeBlocks = (radiusChunks * 2 + 1) * 16;

    int spawnX = ageData.isSpawnSet() ? ageData.getSpawnX() : 8;
    int spawnZ = ageData.isSpawnSet() ? ageData.getSpawnZ() : 8;
    int centerChunkX = Math.floorDiv(spawnX, 16);
    int centerChunkZ = Math.floorDiv(spawnZ, 16);
    double centerX = centerChunkX * 16 + 8;
    double centerZ = centerChunkZ * 16 + 8;

    WorldBorder border = level.getWorldBorder();
    border.setCenter(centerX, centerZ);
    border.setSize(borderSizeBlocks);
    border.setDamagePerBlock(0.0);
    border.setDamageSafeZone(0.0);
    border.setWarningBlocks(0);
    border.setWarningTime(0);

    if (!level.players().isEmpty()) {
      PersonalPocketDimension.syncBorderToPlayers(level, border);
    }
  }

  /**
   * Finds a fluid surface near the given position and builds a 3x3 stone platform on top.
   * Returns the spawn position one block above the platform center, or null if no fluid found.
   * Respects the spawn edge margin.
   */
  @Nullable
  private static BlockPos buildSpawnPlatform(@NotNull ServerLevel level, @NotNull BlockPos center) {
    if (!level.hasChunk(center.getX() >> 4, center.getZ() >> 4)) {
      return null;
    }

    int minY = level.getMinBuildHeight();
    int maxY = level.getMaxBuildHeight();
    int safeMinY = minY + SPAWN_EDGE_MARGIN;
    int safeMaxY = maxY - SPAWN_EDGE_MARGIN;
    int startY = Math.min(safeMaxY - 1, 128);
    int cx = center.getX();
    int cz = center.getZ();

    // Search downward for the top of a fluid column
    for (int y = startY; y > safeMinY; y--) {
      BlockPos pos = new BlockPos(cx, y, cz);
      var state = level.getBlockState(pos);
      var aboveState = level.getBlockState(pos.above());

      boolean isFluid = !state.getFluidState().isEmpty();
      boolean aboveIsClear = aboveState.isAir() || aboveState.getFluidState().isEmpty();

      if (isFluid && aboveIsClear) {
        // Found the fluid surface - build a 3x3 raft one block above
        int platY = y + 1;
        net.minecraft.world.level.block.state.BlockState plank =
            net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
          for (int dz = -1; dz <= 1; dz++) {
            BlockPos platPos = new BlockPos(cx + dx, platY, cz + dz);
            level.setBlock(platPos, plank, 2);
          }
        }
        BlockPos spawnPos = new BlockPos(cx, platY + 1, cz);
        Mystcraft.LOGGER.info("Built spawn platform at {} over fluid surface", spawnPos);
        return spawnPos;
      }
    }

    return null;
  }

  /**
   * Checks if a position is safe for spawning.
   * Safe means: solid non-bedrock ground with at least 2 air blocks above.
   * Avoids upper/lower 50 blocks of the dimension to keep spawns in reasonable areas.
   */
  public static boolean isSafeSpawn(@NotNull ServerLevel level, @NotNull BlockPos pos) {
    int minY = level.getMinBuildHeight();
    int maxY = level.getMaxBuildHeight();

    // Check Y bounds - avoid upper/lower 50 blocks of the dimension
    int safeMinY = minY + SPAWN_EDGE_MARGIN;
    int safeMaxY = maxY - SPAWN_EDGE_MARGIN;
    if (pos.getY() < safeMinY || pos.getY() >= safeMaxY) {
      return false;
    }

    // Don't read blocks from unloaded chunks - would deadlock the server thread
    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
      return false;
    }

    // Check block below (must be solid and not bedrock)
    BlockPos below = pos.below();
    var groundState = level.getBlockState(below);

    // Must have solid ground
    if (!groundState.isSolidRender(level, below)) {
      return false;
    }

    // Must not be bedrock (player would be stuck at world bottom)
    if (groundState.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
      return false;
    }

    // Check position and above (must be air or passable)
    var feetState = level.getBlockState(pos);
    var headState = level.getBlockState(pos.above());

    // Both feet and head positions must be passable
    if (!feetState.isAir() && !feetState.getCollisionShape(level, pos).isEmpty()) {
      return false;
    }
    if (!headState.isAir() && !headState.getCollisionShape(level, pos.above()).isEmpty()) {
      return false;
    }

    // Check for dangerous blocks (lava, fire)
    return !groundState.is(net.minecraft.world.level.block.Blocks.LAVA) &&
        !feetState.is(net.minecraft.world.level.block.Blocks.LAVA) &&
        !feetState.is(net.minecraft.world.level.block.Blocks.FIRE);
  }

  /**
   * Finds a safe spawn position near the given center using a spiral search pattern.
   *
   * @param level     The server level
   * @param center    The center position to search from
   * @param maxRadius Maximum radius to search
   * @return A safe spawn position, or null if none found
   */
  @Nullable
  public static BlockPos findSafeSpawnNear(@NotNull ServerLevel level, @NotNull BlockPos center, int maxRadius) {
    int minY = level.getMinBuildHeight();
    int maxY = level.getMaxBuildHeight();

    // Search in spiral pattern from center
    for (int radius = 0; radius <= maxRadius; radius += 4) {
      for (int dx = -radius; dx <= radius; dx += 4) {
        for (int dz = -radius; dz <= radius; dz += 4) {
          // Only check perimeter at this radius (except for r=0)
          if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
            continue;
          }

          int x = center.getX() + dx;
          int z = center.getZ() + dz;

          // Find the surface Y at this X/Z
          BlockPos surfacePos = findSurfaceY(level, x, z, minY, maxY);
          if (surfacePos != null && isSafeSpawn(level, surfacePos)) {
            Mystcraft.LOGGER.debug("Found safe spawn at {} (radius {} from center)", surfacePos, radius);
            return surfacePos;
          }
        }
      }
    }

    Mystcraft.LOGGER.warn("Could not find safe spawn near {} within radius {}", center, maxRadius);
    return null;
  }

  /**
   * Finds the surface Y coordinate at a given X/Z position.
   * Returns the position of the first air block above solid ground.
   * Returns null if the chunk is not loaded (to avoid deadlocking the server thread).
   * Avoids upper/lower 50 blocks of the dimension.
   */
  @Nullable
  private static BlockPos findSurfaceY(@NotNull ServerLevel level, int x, int z, int minY, int maxY) {
    // Don't read blocks from unloaded chunks - would deadlock the server thread
    if (!level.hasChunk(x >> 4, z >> 4)) {
      return null;
    }

    // Apply edge margins to avoid spawning near dimension boundaries
    int safeMinY = minY + SPAWN_EDGE_MARGIN;
    int safeMaxY = maxY - SPAWN_EDGE_MARGIN;

    // Start from safe upper bound and search down
    int startY = Math.min(safeMaxY - 1, 128);

    // First pass: search downward from reasonable height
    for (int y = startY; y > safeMinY; y--) {
      BlockPos pos = new BlockPos(x, y, z);
      BlockPos below = pos.below();

      var state = level.getBlockState(pos);
      var belowState = level.getBlockState(below);

      // Found air/passable above solid ground
      if ((state.isAir() || state.getCollisionShape(level, pos).isEmpty()) &&
          belowState.isSolidRender(level, below)) {
        return pos;
      }
    }

    // Second pass: search upward from bottom (for underground ages or unusual terrain)
    for (int y = minY + 1; y < startY; y++) {
      BlockPos pos = new BlockPos(x, y, z);
      BlockPos below = pos.below();

      var state = level.getBlockState(pos);
      var belowState = level.getBlockState(below);

      if ((state.isAir() || state.getCollisionShape(level, pos).isEmpty()) &&
          belowState.isSolidRender(level, below)) {
        return pos;
      }
    }

    return null;
  }
}
