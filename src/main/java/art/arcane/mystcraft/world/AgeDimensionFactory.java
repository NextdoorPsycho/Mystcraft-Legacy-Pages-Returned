package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Factory for dynamically creating Mystcraft Age dimensions at runtime.
 * Uses reflection to access private MinecraftServer internals for dimension registration.
 */
public class AgeDimensionFactory {

    private static final String DIMENSION_PREFIX = "mystcraft_age_";

    // Cached reflection fields
    private static Field executorField;
    private static Field levelsField;
    private static Field storageSourceField;
    private static Method markWorldsDirtyMethod;

    static {
        try {
            // MinecraftServer private field access
            Class<?> serverClass = MinecraftServer.class;

            executorField = findField(serverClass, "executor", Executor.class);
            levelsField = findField(serverClass, "levels", Map.class);
            storageSourceField = findField(serverClass, "storageSource", LevelStorageSource.LevelStorageAccess.class);
            markWorldsDirtyMethod = serverClass.getDeclaredMethod("markWorldsDirty");
            markWorldsDirtyMethod.setAccessible(true);
        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to initialize AgeDimensionFactory reflection", e);
        }
    }

    /**
     * Finds a field by name or type in a class hierarchy.
     */
    private static Field findField(Class<?> clazz, String name, Class<?> type) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().equals(type) || field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        // Check superclass
        if (clazz.getSuperclass() != null) {
            return findField(clazz.getSuperclass(), name, type);
        }
        return null;
    }

    /**
     * Creates a new Age dimension and returns the ServerLevel.
     *
     * @param server The Minecraft server
     * @param ageUID The unique age identifier
     * @param ageUUID The unique age UUID (for the dimension key)
     * @return The created ServerLevel, or null if creation failed
     */
    @Nullable
    public static ServerLevel createAgeDimension(@NotNull MinecraftServer server, int ageUID, @NotNull UUID ageUUID) {
        ResourceLocation dimensionId = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

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
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
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
        if (executorField == null || levelsField == null || storageSourceField == null) {
            Mystcraft.LOGGER.error("Reflection fields not initialized");
            return null;
        }

        try {
            // Get private fields via reflection
            Executor executor = (Executor) executorField.get(server);
            Map<ResourceKey<Level>, ServerLevel> levels = (Map<ResourceKey<Level>, ServerLevel>) levelsField.get(server);
            LevelStorageSource.LevelStorageAccess storageSource =
                    (LevelStorageSource.LevelStorageAccess) storageSourceField.get(server);

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
            ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, dimensionKey.location());
            Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);

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
            long seed = BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()) + ageUID;

            // Create progress listener (use no-op for dynamic dimensions)
            ChunkProgressListener progressListener = new ChunkProgressListener() {
                @Override
                public void updateSpawnPos(net.minecraft.world.level.ChunkPos pos) {}
                @Override
                public void onStatusChange(net.minecraft.world.level.ChunkPos pos, @Nullable net.minecraft.world.level.chunk.ChunkStatus status) {}
                @Override
                public void start() {}
                @Override
                public void stop() {}
            };

            // Create the new ServerLevel
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
                    false, // tickTime
                    null // randomSequences
            );

            // Add world border listener to sync with overworld
            overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(
                    newLevel.getWorldBorder()
            ));

            // Register the level with the server
            levels.put(dimensionKey, newLevel);

            // Mark worlds as dirty
            if (markWorldsDirtyMethod != null) {
                markWorldsDirtyMethod.invoke(server);
            }

            // Fire world load event
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(newLevel));

            Mystcraft.LOGGER.info("Created Age dimension {} at {}", ageUID, dimensionKey.location());

            // Initialize AgeData for this level
            AgeData ageData = AgeData.get(newLevel);
            ageData.setAgeUID(ageUID);
            ageData.setAgeUUID(ageUUID);

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
            Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
            LevelStem overworldStem = stemRegistry.get(LevelStem.OVERWORLD);

            if (overworldStem == null) {
                Mystcraft.LOGGER.error("Overworld stem not found");
                return null;
            }

            // Get biome source from overworld
            BiomeSource biomeSource = overworldStem.generator().getBiomeSource();

            // Create chunk generator based on director configuration
            ChunkGenerator generator;
            if (director != null) {
                // Use custom Age chunk generator with director settings
                generator = AgeChunkGenerator.fromDirector(director, biomeSource);
                Mystcraft.LOGGER.info("Created Age {} with terrain type: {}", ageUID, director.getTerrainType());
            } else {
                // Use overworld generator as fallback
                generator = overworldStem.generator();
            }

            return new LevelStem(
                    overworldStem.type(),
                    generator
            );
        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to create LevelStem", e);
            return null;
        }
    }

    /**
     * Creates an Age dimension with a specific AgeDirector configuration.
     */
    @Nullable
    public static ServerLevel createAgeDimension(@NotNull MinecraftServer server, int ageUID,
                                                  @NotNull UUID ageUUID, @Nullable AgeDirectorImpl director) {
        ResourceLocation dimensionId = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

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
     * Registers a dimension stem in the registry using reflection.
     */
    @SuppressWarnings("unchecked")
    private static void registerDimensionStem(
            MappedRegistry<LevelStem> registry,
            ResourceKey<LevelStem> key,
            LevelStem stem
    ) {
        try {
            // The registry may be frozen, need to unfreeze it
            Field frozenField = MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.set(registry, false);

            // Register the stem
            // Use internal register method
            Method registerMethod = null;
            for (Method m : MappedRegistry.class.getDeclaredMethods()) {
                if (m.getName().equals("register") && m.getParameterCount() == 3) {
                    registerMethod = m;
                    break;
                }
            }

            if (registerMethod != null) {
                registerMethod.setAccessible(true);
                registerMethod.invoke(registry, key, stem, Lifecycle.stable());
            } else {
                // Fallback: try direct registration
                registry.register(key, stem, Lifecycle.stable());
            }

            // Re-freeze the registry
            frozenField.set(registry, true);

        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to register dimension stem", e);
        }
    }

    /**
     * Unloads an Age dimension from the server.
     * Note: This doesn't delete the dimension data.
     */
    @SuppressWarnings("unchecked")
    public static void unloadAgeDimension(@NotNull MinecraftServer server, int ageUID) {
        AgeManager ageManager = AgeManager.get(server);
        ResourceLocation dimLoc = ageManager.getDimension(ageUID);

        if (dimLoc == null) {
            return;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
        ServerLevel level = server.getLevel(dimensionKey);

        if (level == null) {
            return;
        }

        try {
            // Remove from levels map
            Map<ResourceKey<Level>, ServerLevel> levels =
                    (Map<ResourceKey<Level>, ServerLevel>) levelsField.get(server);
            levels.remove(dimensionKey);

            // Fire unload event
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(level));

            // Close the level
            level.close();

            Mystcraft.LOGGER.info("Unloaded Age dimension {}", ageUID);

        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to unload Age {}", ageUID, e);
        }
    }

    /**
     * Gets the dimension ResourceKey for an Age UID.
     */
    @Nullable
    public static ResourceKey<Level> getDimensionKey(int ageUID) {
        ResourceLocation dimensionId = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
        return ResourceKey.create(Registries.DIMENSION, dimensionId);
    }

    /**
     * Checks if a dimension key belongs to a Mystcraft Age.
     */
    public static boolean isMystcraftAge(@NotNull ResourceKey<Level> dimensionKey) {
        return dimensionKey.location().getNamespace().equals(Mystcraft.MOD_ID)
                && dimensionKey.location().getPath().startsWith(DIMENSION_PREFIX);
    }

    /**
     * Gets the spawn position for an Age.
     */
    @NotNull
    public static BlockPos getAgeSpawn(@NotNull ServerLevel level) {
        AgeData ageData = AgeData.getIfPresent(level);
        if (ageData != null && ageData.isSpawnSet()) {
            return new BlockPos(ageData.getSpawnX(), ageData.getSpawnY(), ageData.getSpawnZ());
        }
        // Default spawn at world spawn or 0,64,0
        return level.getSharedSpawnPos();
    }
}
