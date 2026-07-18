package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModRegistrations;
import art.arcane.mystcraft.world.AgeManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraft.world.level.storage.LevelResource;

/** Forge 65 production entrypoint for Mystcraft Legacy Returned. */
@Mod(Mystcraft.MOD_ID)
public final class MystcraftForge {

  public MystcraftForge(FMLJavaModLoadingContext context) {
    BusGroup modBusGroup = context.getModBusGroup();
    Mystcraft.LOGGER.info("[Mystcraft] Initializing Forge 65 production runtime");

    ForgeMystcraftConfig.register(context);
    ModRegistrations.registerAll(modBusGroup);
    ForgeMystcraftNetwork.register();

    FMLCommonSetupEvent.getBus(modBusGroup).addListener(MystcraftForge::onCommonSetup);
    EntityAttributeCreationEvent.BUS.addListener(MystcraftForge::onEntityAttributes);
    ServerAboutToStartEvent.BUS.addListener(MystcraftForge::onServerAboutToStart);
    ServerStartingEvent.BUS.addListener(MystcraftForge::onServerStarting);
    ServerStoppingEvent.BUS.addListener(MystcraftForge::onServerStopping);
    ServerStoppedEvent.BUS.addListener(MystcraftForge::onServerStopped);

    Services.EVENTS.registerServerEvents();
    Services.EVENTS.registerCommonEvents();
    if (FMLEnvironment.dist.isClient()) {
      Services.EVENTS.registerClientEvents();
      ForgeClientSetup.register(modBusGroup);
    }

    Mystcraft.init();
  }

  private static void onCommonSetup(FMLCommonSetupEvent event) {
    if (Boolean.getBoolean("mystcraft.enableGameTestIdeMode")) {
      SharedConstants.IS_RUNNING_IN_IDE = true;
    }
    event.enqueueWork(() -> {
      Mystcraft.commonSetup();
      Mystcraft.finishSymbolRegistration();
    });
  }

  private static void onEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(ModEntities.PERSONAL_POCKET_PROXY.get(),
        PersonalPocketProxyEntity.createAttributes().build());
  }

  private static void onServerAboutToStart(ServerAboutToStartEvent event) {
    // GameTestServer participates in Forge's about-to-start lifecycle but not
    // its starting lifecycle. Publish the server at the earliest supported
    // point so async Age generation has a valid registry source in every
    // production and GameTest launch target.
    Mystcraft.setCurrentServer(event.getServer());
  }

  private static void onServerStarting(ServerStartingEvent event) {
    if (art.arcane.mystcraft.config.MystcraftConfig.deleteAgesOnStartup.get()) {
      deleteAllAges(event.getServer());
    }
  }

  private static void onServerStopping(ServerStoppingEvent event) {
    art.arcane.mystcraft.world.AgeDimensionFactory.prepareForServerStop(event.getServer());
  }

  private static void onServerStopped(ServerStoppedEvent event) {
    Mystcraft.setCurrentServer(null);
  }

  private static void deleteAllAges(net.minecraft.server.MinecraftServer server) {
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
    Mystcraft.LOGGER.info(
        "[Mystcraft] Deleted {} Age records and {} directories", count, removed);
  }
}
