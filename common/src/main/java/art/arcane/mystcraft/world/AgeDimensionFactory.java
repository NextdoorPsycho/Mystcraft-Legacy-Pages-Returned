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
import net.minecraft.core.registries.Registries;
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
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Factory for dynamically creating Mystcraft Age dimensions at runtime. Uses
 * reflection to access MinecraftServer internals for dimension registration.
 * Access Transformers/Wideners ensure the reflection works at runtime.
 */
public class AgeDimensionFactory {

  private static final String DIMENSION_PREFIX = "mystcraft_age_";

  private static final ResourceKey<DimensionType> DIM_TYPE_NORMAL =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_normal"));
  private static final ResourceKey<DimensionType> DIM_TYPE_NETHER =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_nether"));
  private static final ResourceKey<DimensionType> DIM_TYPE_END =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_end"));
  private static final ResourceKey<DimensionType> DIM_TYPE_DARK =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_dark"));
  private static final ResourceKey<DimensionType> DIM_TYPE_BRIGHT =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_bright"));
  private static final ResourceKey<DimensionType> DIM_TYPE_CAVE =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_cave"));
  private static final ResourceKey<DimensionType> DIM_TYPE_SKYLANDS =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_skylands"));
  private static final ResourceKey<DimensionType> DIM_TYPE_PERSONAL =
      ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Mystcraft.MOD_ID, "age_personal"));

  private static final String FIELD_EXECUTOR = "executor";
  private static final String FIELD_LEVELS = "levels";
  private static final String FIELD_STORAGE_SOURCE = "storageSource";
  private static final String FIELD_FROZEN = "frozen";
  private static final int SPAWN_EDGE_MARGIN = 50;

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

  private static Field findField(Class<?> clazz, String name, Class<?> type) {
    Class<?> current = clazz;
    while (current != null) {
      for (Field field : current.getDeclaredFields()) {
        if (field.getName().equals(name)) {
          return field;
        }

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
    ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

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

      ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
      ServerLevel level = server.getLevel(dimensionKey);
      if (level != null) {
        return level;
      }

      UUID ageUUID = UUID.randomUUID();
      return createAndRegisterWorld(server, dimensionKey, ageUID, ageUUID);
    }

    UUID newUUID = UUID.randomUUID();
    ServerLevel level = createAgeDimension(server, ageUID, newUUID);
    if (level != null) {
      ResourceLocation newDimLoc = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
      ageManager.registerAge(ageUID, newDimLoc, newUUID);
    }
    return level;
  }

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

      Executor executor = net.minecraft.Util.backgroundExecutor();
      Map<ResourceKey<Level>, ServerLevel> levels = getFieldValue(server, FIELD_LEVELS, Map.class);
      LevelStorageSource.LevelStorageAccess storageSource = getFieldValue(server, FIELD_STORAGE_SOURCE,
          LevelStorageSource.LevelStorageAccess.class);

      if (executor == null || levels == null || storageSource == null) {
        Mystcraft.LOGGER.error("Failed to access MinecraftServer internals for dimension creation");
        return null;
      }

      ServerLevel overworld = server.overworld();
      ServerLevelData overworldData = (ServerLevelData) overworld.getLevelData();

      LevelStem levelStem = createLevelStem(server, ageUID, director);
      if (levelStem == null) {
        Mystcraft.LOGGER.error("Failed to create LevelStem for Age {}", ageUID);
        return null;
      }

      ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, dimensionKey.location());
      Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);

      if (stemRegistry instanceof MappedRegistry<LevelStem> mappedRegistry) {
        registerDimensionStem(mappedRegistry, stemKey, levelStem);
      }

      DerivedLevelData derivedData = new DerivedLevelData(
          server.getWorldData(),
          overworldData
      );

      long seed = BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()) + ageUID;

      ChunkProgressListener progressListener = (ChunkProgressListener) Proxy.newProxyInstance(
          ChunkProgressListener.class.getClassLoader(),
          new Class<?>[]{ChunkProgressListener.class},
          (proxy, method, args) -> null
      );

      ServerLevel newLevel = new ServerLevel(
          server,
          executor,
          storageSource,
          derivedData,
          dimensionKey,
          levelStem,
          progressListener,
          false,
          seed,
          List.of(),
          false,
          null
      );

      var overworldBorder = overworld.getWorldBorder();
      var newBorder = newLevel.getWorldBorder();

      boolean isPersonalPocket = director != null && director.isPersonalPocket();
      boolean isMicroDimension = director != null && director.isMicroDimensionsEnabled();
      if (isPersonalPocket) {

        newBorder.setCenter(0.0, 0.0);
        newBorder.setSize(160);
        newBorder.setDamagePerBlock(0.0);
        newBorder.setDamageSafeZone(0.0);
        newBorder.setWarningBlocks(15);
        newBorder.setWarningTime(15);

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

        newBorder.setCenter(overworldBorder.getCenterX(), overworldBorder.getCenterZ());
        newBorder.setSize(overworldBorder.getSize());
        newBorder.setDamagePerBlock(overworldBorder.getDamagePerBlock());
        newBorder.setDamageSafeZone(overworldBorder.getDamageSafeZone());
        newBorder.setWarningBlocks(overworldBorder.getWarningBlocks());
        newBorder.setWarningTime(overworldBorder.getWarningTime());

        overworldBorder.addListener(new BorderChangeListener.DelegateBorderChangeListener(newBorder));
      }

      levels.put(dimensionKey, newLevel);

      try {
        java.lang.reflect.Method markDirty = MinecraftServer.class.getMethod("markWorldsDirty");
        markDirty.invoke(server);
      } catch (NoSuchMethodException e) {

        Mystcraft.LOGGER.debug("markWorldsDirty() not available");
      } catch (Exception e) {
        Mystcraft.LOGGER.warn("Failed to mark worlds dirty: {}", e.getMessage());
      }

      art.arcane.mystcraft.event.AgeDeathHandler.configureAgeGameRules(newLevel);

      Services.EVENTS.fireLevelLoadEvent(newLevel);

      Mystcraft.LOGGER.info("Created Age dimension {} at {}", ageUID, dimensionKey.location());

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

  @Nullable
  private static LevelStem createLevelStem(@NotNull MinecraftServer server, int ageUID) {
    return createLevelStem(server, ageUID, null);
  }

  @Nullable
  private static LevelStem createLevelStem(@NotNull MinecraftServer server, int ageUID, @Nullable AgeDirectorImpl director) {
    try {

      Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
      LevelStem overworldStem = stemRegistry.get(LevelStem.OVERWORLD);

      if (overworldStem == null) {
        Mystcraft.LOGGER.error("Overworld stem not found");
        return null;
      }

      BiomeSource biomeSource;
      if (director != null) {

        IBiomeController biomeController = director.getBiomeControllerImpl();
        List<Holder<Biome>> directorBiomes = director.getBiomes();

        if (biomeController != null && biomeController.getBiomes().isEmpty() && !directorBiomes.isEmpty()) {
          Mystcraft.LOGGER.info("Age {} controller has 0 biomes but director has {}, recreating controller",
              ageUID, directorBiomes.size());

          String controllerType = biomeController.getType();
          biomeController = switch (controllerType) {
            case "single" ->
                new BiomeControllerSingle(directorBiomes, director.getSeed());
            case "tiny" ->
                new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.TINY);
            case "small" ->
                new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.SMALL);
            case "medium" ->
                new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.MEDIUM);
            case "large" ->
                new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.LARGE);
            case "huge" ->
                new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.HUGE);
            case "tiled" ->
                new BiomeControllerTiled(directorBiomes, director.getSeed());
            case "grid" ->
                new BiomeControllerGrid(directorBiomes, director.getSeed());
            case "shuffle" ->
                new BiomeControllerShuffle(directorBiomes, director.getSeed());
            default ->
                new BiomeControllerNoise(directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.MEDIUM);
          };
          director.registerInterface(biomeController);
        }

        if (biomeController != null && !biomeController.getBiomes().isEmpty()) {

          biomeSource = new AgeBiomeSource(biomeController, director.getSeed());
          Mystcraft.LOGGER.info("Created Age {} with biome controller type: {} ({} biomes)",
              ageUID, biomeController.getType(), biomeController.getBiomes().size());
        } else if (!directorBiomes.isEmpty()) {

          Mystcraft.LOGGER.info("Age {} has {} biomes but no controller, creating default noise controller",
              ageUID, directorBiomes.size());
          IBiomeController defaultController = new BiomeControllerNoise(
              directorBiomes, director.getSeed(), BiomeControllerNoise.Scale.MEDIUM);
          director.registerInterface(defaultController);
          biomeSource = new AgeBiomeSource(defaultController, director.getSeed());
        } else {

          Mystcraft.LOGGER.warn("Age {} has no biomes or controller, using overworld biomes", ageUID);
          biomeSource = overworldStem.generator().getBiomeSource();
        }
      } else {

        biomeSource = overworldStem.generator().getBiomeSource();
      }

      ChunkGenerator generator;
      if (director != null) {

        generator = AgeChunkGenerator.fromDirector(director, biomeSource, ageUID);
        Mystcraft.LOGGER.info("Created Age {} with terrain type: {}", ageUID, director.getTerrainType());
      } else {

        generator = overworldStem.generator();
      }

      Holder<DimensionType> dimensionType = selectDimensionType(server, director, overworldStem.type());

      return new LevelStem(
          dimensionType,
          generator
      );
    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to create LevelStem", e);
      return null;
    }
  }

  private static Holder<DimensionType> selectDimensionType(
      @NotNull MinecraftServer server,
      @Nullable AgeDirectorImpl director,
      @NotNull Holder<DimensionType> defaultType
  ) {
    if (director == null) {
      return defaultType;
    }

    ResourceKey<DimensionType> selectedKey = determineDimensionTypeKey(director);

    Registry<DimensionType> dimTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
    Holder<DimensionType> holder = dimTypeRegistry.getHolder(selectedKey).orElse(null);

    if (holder != null) {
      Mystcraft.LOGGER.debug("Using dimension type {} for age", selectedKey.location());
      return holder;
    }

    Mystcraft.LOGGER.warn("Dimension type {} not found, using default", selectedKey.location());
    return defaultType;
  }

  private static ResourceKey<DimensionType> determineDimensionTypeKey(@NotNull AgeDirectorImpl director) {

    if (director.isPersonalPocket()) {
      return DIM_TYPE_PERSONAL;
    }

    String terrainType = director.getTerrainType();
    String lightingType = director.getLightingType();

    if ("nether".equalsIgnoreCase(terrainType) || director.isNetherFortEnabled()) {
      return DIM_TYPE_NETHER;
    }

    if ("end".equalsIgnoreCase(terrainType)) {
      return DIM_TYPE_END;
    }

    if ("cave".equalsIgnoreCase(terrainType)) {
      return DIM_TYPE_CAVE;
    }

    if (director.areSkylandsEnabled() || director.areFloatingIslandsEnabled() || "skylands".equalsIgnoreCase(terrainType)) {
      return DIM_TYPE_SKYLANDS;
    }

    if ("bright".equalsIgnoreCase(lightingType)) {
      return DIM_TYPE_BRIGHT;
    }

    if ("dark".equalsIgnoreCase(lightingType)) {
      return DIM_TYPE_DARK;
    }

    return DIM_TYPE_NORMAL;
  }

  /**
   * Creates an Age dimension with a specific AgeDirector configuration.
   */
  @Nullable
  public static ServerLevel createAgeDimension(@NotNull MinecraftServer server, int ageUID,
                                               @NotNull UUID ageUUID, @Nullable AgeDirectorImpl director) {
    ResourceLocation dimensionId = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
    ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

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

  private static void registerDimensionStem(
      MappedRegistry<LevelStem> registry,
      ResourceKey<LevelStem> key,
      LevelStem stem
  ) {
    try {

      Field frozenField = findField(MappedRegistry.class, FIELD_FROZEN, boolean.class);
      if (frozenField != null) {
        frozenField.setAccessible(true);
        frozenField.set(registry, false);
      }

      if (!registerWithRegistrationInfo(registry, key, stem)) {
        registerWithLifecycle(registry, key, stem);
      }

      if (frozenField != null) {
        frozenField.set(registry, true);
      }

    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to register dimension stem", e);
    }
  }

  private static boolean registerWithRegistrationInfo(
      MappedRegistry<LevelStem> registry,
      ResourceKey<LevelStem> key,
      LevelStem stem
  ) {
    try {
      Class<?> infoClass = Class.forName("net.minecraft.core.RegistrationInfo");
      Field builtInField = infoClass.getField("BUILT_IN");
      Object builtIn = builtInField.get(null);
      java.lang.reflect.Method register = registry.getClass().getMethod("register", key.getClass(), LevelStem.class, infoClass);
      register.invoke(registry, key, stem, builtIn);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    } catch (ReflectiveOperationException e) {
      return false;
    }
  }

  private static void registerWithLifecycle(
      MappedRegistry<LevelStem> registry,
      ResourceKey<LevelStem> key,
      LevelStem stem
  ) {
    try {
      java.lang.reflect.Method register = registry.getClass().getMethod("register", key.getClass(), LevelStem.class, Lifecycle.class);
      register.invoke(registry, key, stem, Lifecycle.stable());
    } catch (ReflectiveOperationException ignored) {
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
   * Gets the spawn position for an Age. Uses intelligent search to find safe
   * ground if no spawn is set. If no dry land exists, builds a small platform
   * above the fluid surface.
   */
  @NotNull
  public static BlockPos getAgeSpawn(@NotNull ServerLevel level) {
    AgeData ageData = AgeData.getIfPresent(level);
    if (ageData != null && ageData.isSpawnSet()) {
      return new BlockPos(ageData.getSpawnX(), ageData.getSpawnY(), ageData.getSpawnZ());
    }

    BlockPos safeSpawn = findSafeSpawnNear(level, BlockPos.ZERO, 64);
    if (safeSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(safeSpawn.getX(), safeSpawn.getY(), safeSpawn.getZ());
      }
      return safeSpawn;
    }

    BlockPos worldSpawn = level.getSharedSpawnPos();

    safeSpawn = findSafeSpawnNear(level, worldSpawn, 32);
    if (safeSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(safeSpawn.getX(), safeSpawn.getY(), safeSpawn.getZ());
      }
      return safeSpawn;
    }

    BlockPos platformSpawn = buildSpawnPlatform(level, BlockPos.ZERO);
    if (platformSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(platformSpawn.getX(), platformSpawn.getY(), platformSpawn.getZ());
      }
      return platformSpawn;
    }

    BlockPos voidSpawn = buildVoidSpawnBlock(level, BlockPos.ZERO);
    if (voidSpawn != null) {
      if (ageData != null) {
        ageData.setSpawn(voidSpawn.getX(), voidSpawn.getY(), voidSpawn.getZ());
      }
      return voidSpawn;
    }

    return worldSpawn;
  }

  @Nullable
  private static BlockPos buildVoidSpawnBlock(@NotNull ServerLevel level, @NotNull BlockPos center) {
    if (!level.hasChunk(center.getX() >> 4, center.getZ() >> 4)) {
      return null;
    }

    int minY = level.getMinBuildHeight();
    int maxY = level.getMaxBuildHeight();

    int safeMinY = minY + SPAWN_EDGE_MARGIN;
    int safeMaxY = maxY - SPAWN_EDGE_MARGIN;
    int spawnY = (safeMinY + safeMaxY) / 2;

    spawnY = Math.max(safeMinY, Math.min(128, spawnY));

    BlockPos stonePos = new BlockPos(center.getX(), spawnY, center.getZ());
    BlockPos spawnPos = stonePos.above();

    level.setBlock(stonePos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);

    Mystcraft.LOGGER.info("Built void spawn block at {} (void dimension fallback)", stonePos);
    return spawnPos;
  }

  /**
   * Applies a fixed micro-dimension world border for new Ages (if enabled in
   * AgeData). Uses the spawn chunk center as the border center and aligns to
   * chunk edges.
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

    for (int y = startY; y > safeMinY; y--) {
      BlockPos pos = new BlockPos(cx, y, cz);
      var state = level.getBlockState(pos);
      var aboveState = level.getBlockState(pos.above());

      boolean isFluid = !state.getFluidState().isEmpty();
      boolean aboveIsClear = aboveState.isAir() || aboveState.getFluidState().isEmpty();

      if (isFluid && aboveIsClear) {

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
   * Checks if a position is safe for spawning. Safe means: solid non-bedrock
   * ground with at least 2 air blocks above. Avoids upper/lower 50 blocks of
   * the dimension to keep spawns in reasonable areas.
   */
  public static boolean isSafeSpawn(@NotNull ServerLevel level, @NotNull BlockPos pos) {
    int minY = level.getMinBuildHeight();
    int maxY = level.getMaxBuildHeight();

    int safeMinY = minY + SPAWN_EDGE_MARGIN;
    int safeMaxY = maxY - SPAWN_EDGE_MARGIN;
    if (pos.getY() < safeMinY || pos.getY() >= safeMaxY) {
      return false;
    }

    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
      return false;
    }

    BlockPos below = pos.below();
    var groundState = level.getBlockState(below);

    if (!groundState.isSolidRender(level, below)) {
      return false;
    }

    if (groundState.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
      return false;
    }

    var feetState = level.getBlockState(pos);
    var headState = level.getBlockState(pos.above());

    if (!feetState.isAir() && !feetState.getCollisionShape(level, pos).isEmpty()) {
      return false;
    }
    if (!headState.isAir() && !headState.getCollisionShape(level, pos.above()).isEmpty()) {
      return false;
    }

    return !groundState.is(net.minecraft.world.level.block.Blocks.LAVA) &&
        !feetState.is(net.minecraft.world.level.block.Blocks.LAVA) &&
        !feetState.is(net.minecraft.world.level.block.Blocks.FIRE);
  }

  /**
   * Finds a safe spawn position near the given center using a spiral search
   * pattern.
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

    for (int radius = 0; radius <= maxRadius; radius += 4) {
      for (int dx = -radius; dx <= radius; dx += 4) {
        for (int dz = -radius; dz <= radius; dz += 4) {

          if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
            continue;
          }

          int x = center.getX() + dx;
          int z = center.getZ() + dz;

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

  @Nullable
  private static BlockPos findSurfaceY(@NotNull ServerLevel level, int x, int z, int minY, int maxY) {

    if (!level.hasChunk(x >> 4, z >> 4)) {
      return null;
    }

    int safeMinY = minY + SPAWN_EDGE_MARGIN;
    int safeMaxY = maxY - SPAWN_EDGE_MARGIN;

    int startY = Math.min(safeMaxY - 1, 128);

    for (int y = startY; y > safeMinY; y--) {
      BlockPos pos = new BlockPos(x, y, z);
      BlockPos below = pos.below();

      var state = level.getBlockState(pos);
      var belowState = level.getBlockState(below);

      if ((state.isAir() || state.getCollisionShape(level, pos).isEmpty()) &&
          belowState.isSolidRender(level, below)) {
        return pos;
      }
    }

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
