package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.FabricMystcraftConfig;
import art.arcane.mystcraft.event.FabricEventHelper;
import art.arcane.mystcraft.network.FabricMystcraftNetwork;
import art.arcane.mystcraft.registry.FabricRegistries;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Fabric mod entry point for Mystcraft 1.18.2.
 * Self-contained - does not pull from fabric/src.
 */
public class MystcraftFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    Mystcraft.LOGGER.info("[Mystcraft] Fabric 1.18.2 initialization starting...");

    FabricMystcraftConfig.load();

    // Register all content via consolidated registries
    FabricRegistries.register();

    // Populate common stubs from Fabric registry objects
    FabricRegistries.populateCommonRegistries();

    Mystcraft.init();

    // Network registration
    FabricMystcraftNetwork.register();

    // Common setup (equivalent to FMLCommonSetupEvent)
    art.arcane.mystcraft.advancements.ModAdvancements.register();
    Mystcraft.commonSetup();
    Mystcraft.finishSymbolRegistration();

    // Server lifecycle events
    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      Mystcraft.LOGGER.info("[Mystcraft] Server starting");
      Mystcraft.setCurrentServer(server);

      if (FabricMystcraftConfig.deleteAgesOnStartup.get()) {
        deleteAllAges(server);
      }
    });

    ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
      Mystcraft.LOGGER.info("[Mystcraft] Server stopped");
      Mystcraft.setCurrentServer(null);
    });

    // Level load event for Age director reconstruction
    ServerWorldEvents.LOAD.register((server, level) -> onLevelLoad(level));

    // Datapack reload listeners
    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new art.arcane.mystcraft.fabric.resource.FabricGrammarReloadListener());
    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new art.arcane.mystcraft.fabric.resource.FabricSymbolReloadListener());

    // Register all Fabric event callbacks
    FabricEventHelper.registerAll();

    // Register lectern interaction handler for Mystcraft books
    art.arcane.mystcraft.fabric.event.LecternInteractionHandler.register();

    Mystcraft.LOGGER.info("[Mystcraft] Fabric 1.18.2 registration complete");
  }

  /**
   * Handles dimension-level load events for Mystcraft Ages.
   */
  private void onLevelLoad(ServerLevel serverLevel) {
    if (!AgeDimensionFactory.isMystcraftAge(serverLevel.dimension())) {
      return;
    }
    art.arcane.mystcraft.event.AgeDeathHandler.configureAgeGameRules(serverLevel);
    ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
    if (generator instanceof AgeChunkGenerator ageGen && ageGen.needsDirectorReconstruction()) {
      Mystcraft.LOGGER.info("[Mystcraft] Reconstructing director for Age: {}", serverLevel.dimension().location());
      ageGen.reconstructDirectorFromAgeData(serverLevel);
    }
  }

  /**
   * Deletes all Mystcraft Ages and their dimension directories.
   */
  private void deleteAllAges(MinecraftServer server) {
    AgeManager ageManager = AgeManager.get(server);
    int ageCount = ageManager.getAgeCount();

    if (ageCount == 0) {
      Mystcraft.LOGGER.info("[Mystcraft] deleteAgesOnStartup enabled but no ages to delete");
      return;
    }

    List<Integer> uids = new ArrayList<>();
    for (int uid : ageManager.getAllAgeUIDs()) {
      uids.add(uid);
    }

    ageManager.clearAllAges();

    Path worldDir = server.getWorldPath(LevelResource.ROOT);
    Path dimensionsDir = worldDir.resolve("dimensions").resolve(Mystcraft.MOD_ID);
    int directoriesDeleted = 0;

    if (Files.isDirectory(dimensionsDir)) {
      try (Stream<Path> entries = Files.list(dimensionsDir)) {
        List<Path> ageDirs = entries
            .filter(Files::isDirectory)
            .filter(p -> p.getFileName().toString().startsWith("mystcraft_age_"))
            .toList();

        for (Path ageDir : ageDirs) {
          try (Stream<Path> walk = Files.walk(ageDir)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
          }
          directoriesDeleted++;
        }
      } catch (IOException e) {
        Mystcraft.LOGGER.error("[Mystcraft] Failed to delete age dimension directories", e);
      }
    }

    Mystcraft.LOGGER.info("[Mystcraft] deleteAgesOnStartup: deleted {} ages ({} directories removed)", ageCount, directoriesDeleted);
  }
}
