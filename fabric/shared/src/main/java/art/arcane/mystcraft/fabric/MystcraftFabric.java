package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import art.arcane.mystcraft.network.FabricMystcraftNetwork;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
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
 * Fabric mod entry point for Mystcraft 1.20.1.
 */
public class MystcraftFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    Mystcraft.LOGGER.info("[Mystcraft] Fabric 1.20.1 initialization starting...");

    FabricMystcraftConfig.load();

    FabricRegistries.register();
    FabricDefaultAttributeRegistry.register(
        FabricRegistries.PERSONAL_POCKET_PROXY_ENTITY.get(),
        PersonalPocketProxyEntity.createAttributes());

    FabricRegistries.populateCommonRegistries();

    Mystcraft.init();

    FabricMystcraftNetwork.register();

    art.arcane.mystcraft.advancements.ModAdvancements.register();
    Mystcraft.commonSetup();
    Mystcraft.finishSymbolRegistration();

    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      Mystcraft.LOGGER.info("[Mystcraft] Server starting");
      Mystcraft.setCurrentServer(server);

      if (FabricMystcraftConfig.deleteAgesOnStartup.get()) {
        deleteAllAges(server);
      }
    });

    ServerLifecycleEvents.SERVER_STOPPING.register(AgeDimensionFactory::prepareForServerStop);

    ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
      Mystcraft.LOGGER.info("[Mystcraft] Server stopped");
      art.arcane.mystcraft.event.AgeEffectsHandler.clearServerState(server);
      art.arcane.mystcraft.event.AgeDeathHandler.clearServerState(server);
      Mystcraft.setCurrentServer(null);
    });

    ServerWorldEvents.LOAD.register((server, level) -> onLevelLoad(level));

    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new FabricReloadListeners.GrammarReloadListener());
    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new FabricReloadListeners.SymbolReloadListener());
    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new FabricReloadListeners.AffinityReloadListener());

    FabricEventHelper.registerAll();

    LecternInteractionHandler.register();

    Mystcraft.LOGGER.info("[Mystcraft] Fabric 1.20.1 registration complete");
  }

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
