package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.advancements.ModAdvancements;
import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import art.arcane.mystcraft.gametest.MystcraftGameTests;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.registry.ModRegistrations;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.LevelResource;

/** Production Fabric entry point for the Minecraft 26.2 port. */
public final class MystcraftFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    Mystcraft.LOGGER.info("[Mystcraft] Starting Fabric 26.2 initialization");

    FabricMystcraftConfig.load();
    FabricMystcraftNetwork.registerPayloadTypes();
    ModRegistrations.registerAll(null);
    FabricLootModifiers.register();

    FabricDefaultAttributeRegistry.register(
        FabricRegistries.PERSONAL_POCKET_PROXY_ENTITY.get(),
        PersonalPocketProxyEntity.createAttributes());

    Mystcraft.init();
    FabricMystcraftNetwork.registerServerReceivers();
    ModAdvancements.register();
    Mystcraft.commonSetup();
    Mystcraft.finishSymbolRegistration();
    MystcraftGameTests.register();

    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new FabricReloadListeners.Grammar());
    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new FabricReloadListeners.Symbols());
    ResourceManagerHelper.get(PackType.SERVER_DATA)
        .registerReloadListener(new FabricReloadListeners.Affinities());

    Services.EVENTS.registerServerEvents();
    Services.EVENTS.registerCommonEvents();
    registerServerLifecycle();

    Mystcraft.LOGGER.info("[Mystcraft] Fabric 26.2 initialization complete");
  }

  private static void registerServerLifecycle() {
    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      Mystcraft.setCurrentServer(server);
      FabricVillageStructureHandler.inject(server.registryAccess());
      if (FabricMystcraftConfig.deleteAgesOnStartup()) {
        deleteAllAges(server);
      }
    });
    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
      if (success) {
        FabricVillageStructureHandler.inject(server.registryAccess());
      }
    });
    ServerLifecycleEvents.SERVER_STOPPING.register(AgeDimensionFactory::prepareForServerStop);
    ServerLifecycleEvents.SERVER_STOPPED.register(server -> Mystcraft.setCurrentServer(null));
  }

  private static void deleteAllAges(MinecraftServer server) {
    AgeManager manager = AgeManager.get(server);
    int count = manager.getAgeCount();
    if (count == 0) {
      return;
    }
    manager.clearAllAges();

    Path dimensions = server.getWorldPath(LevelResource.ROOT)
        .resolve("dimensions")
        .resolve(Mystcraft.MOD_ID);
    if (!Files.isDirectory(dimensions)) {
      return;
    }

    int removed = 0;
    try (Stream<Path> entries = Files.list(dimensions)) {
      List<Path> ageDirectories = entries
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("mystcraft_age_"))
          .toList();
      for (Path ageDirectory : ageDirectories) {
        try (Stream<Path> walk = Files.walk(ageDirectory)) {
          for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
          }
        }
        removed++;
      }
    } catch (IOException exception) {
      Mystcraft.LOGGER.error("[Mystcraft] Failed to delete configured Age directories", exception);
    }
    Mystcraft.LOGGER.info("[Mystcraft] Deleted {} Age records and {} directories", count, removed);
  }
}
