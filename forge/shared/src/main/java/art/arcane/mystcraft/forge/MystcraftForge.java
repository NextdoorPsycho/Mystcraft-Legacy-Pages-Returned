package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.registry.*;
import net.minecraft.SharedConstants;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Forge mod entry point.
 */
@Mod(Mystcraft.MOD_ID)
public class MystcraftForge {

  public MystcraftForge() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    Mystcraft.LOGGER.info("[Mystcraft] Forge initialization starting...");

    // Register Forge config
    ForgeMystcraftConfig.register();

    // Use the new centralized registration system
    // This calls: initialize(modEventBus) -> register() -> populateCommonRegistries()
    ModRegistrations.registerAll(modEventBus);

    modEventBus.addListener(this::commonSetup);
    modEventBus.addListener(this::registerGameTests);

    MinecraftForge.EVENT_BUS.register(this);

    // Register all events via a version-specific ForgeEventHelper
    Object eventHelper = instantiateFirst(
        "art.arcane.mystcraft.platform.ForgeEventHelper",
        "art.arcane.mystcraft.platform.ForgeEventHelper_1_20_1");
    invokeNoArg(eventHelper, "registerServerEvents");
    invokeNoArg(eventHelper, "registerClientEvents");
    invokeNoArg(eventHelper, "registerCommonEvents");

    Mystcraft.init();

    Mystcraft.LOGGER.info("[Mystcraft] Forge registration complete");
  }

  private void registerGameTests(net.minecraftforge.event.RegisterGameTestsEvent event) {
    Mystcraft.LOGGER.info("[Mystcraft] Registering GameTests");
    try {
      Class<?> gameTestClass = Class.forName("art.arcane.mystcraft.gametest.MystcraftForgeGameTests");
      event.register(gameTestClass);
    } catch (ClassNotFoundException e) {
      Mystcraft.LOGGER.info("[Mystcraft] GameTests not available for this Forge version");
    }
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    // Enable SNBT gametest structure loading in StructureTemplateManager.
    // Must be set after entity registration (which fails with IS_RUNNING_IN_IDE=true due to
    // missing data fixer schemas) but before StructureTemplateManager is constructed during server start.
    SharedConstants.IS_RUNNING_IN_IDE = true;

    event.enqueueWork(() -> {
      art.arcane.mystcraft.advancements.ModAdvancements.register();
      invokeStaticNoArg(
          "art.arcane.mystcraft.network.ForgeMystcraftNetwork",
          "art.arcane.mystcraft.network.ForgeMystcraftNetwork_1_20_1",
          "register");

      Mystcraft.commonSetup();

      Mystcraft.finishSymbolRegistration();
    });
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    Mystcraft.LOGGER.info("[Mystcraft] Server starting");
    Mystcraft.setCurrentServer(event.getServer());

    if (ForgeMystcraftConfig.deleteAgesOnStartup.get()) {
      deleteAllAges(event.getServer());
    }
  }

  @SubscribeEvent
  public void onServerStopped(ServerStoppedEvent event) {
    Mystcraft.LOGGER.info("[Mystcraft] Server stopped");
    Mystcraft.setCurrentServer(null);
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

  @SubscribeEvent
  public void onLevelLoad(LevelEvent.Load event) {
    if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
      return;
    }

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

  @Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      Mystcraft.LOGGER.info("[Mystcraft] Client setup");

      event.enqueueWork(() -> {
        art.arcane.mystcraft.client.render.DrawableWordManager.initialize();
        art.arcane.mystcraft.client.render.PageItemRendererBEWLR.prewarmCache();

        net.minecraft.client.gui.screens.MenuScreens.register(
            ModMenuTypes.INK_MIXER.get(),
            art.arcane.mystcraft.client.screen.InkMixerScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            ModMenuTypes.BOOK_BINDER.get(),
            art.arcane.mystcraft.client.screen.BookBinderScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            ModMenuTypes.LINK_MODIFIER.get(),
            art.arcane.mystcraft.client.screen.LinkModifierScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            ModMenuTypes.WRITING_DESK.get(),
            art.arcane.mystcraft.client.screen.WritingDeskScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            ModMenuTypes.FOLDER.get(),
            art.arcane.mystcraft.client.screen.FolderScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            ModMenuTypes.PORTFOLIO.get(),
            art.arcane.mystcraft.client.screen.PortfolioScreen::new);
      });
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
      Mystcraft.LOGGER.info("[Mystcraft] Registering model layers");

      event.registerLayerDefinition(art.arcane.mystcraft.client.model.BookstandModel.LAYER_LOCATION,
          art.arcane.mystcraft.client.model.BookstandModel::createBodyLayer);
      event.registerLayerDefinition(art.arcane.mystcraft.client.model.WritingDeskModel.LAYER_LOCATION,
          art.arcane.mystcraft.client.model.WritingDeskModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
      Mystcraft.LOGGER.info("[Mystcraft] Registering renderers");

      event.registerBlockEntityRenderer(ModBlockEntities.BOOKSTAND.get(),
          art.arcane.mystcraft.client.renderer.BookstandRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.STAR_FISSURE.get(),
          art.arcane.mystcraft.client.renderer.StarFissureRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.WRITING_DESK.get(),
          art.arcane.mystcraft.client.renderer.WritingDeskRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.BOOK_RECEPTACLE.get(),
          art.arcane.mystcraft.client.renderer.BookReceptacleRenderer::new);

      event.registerEntityRenderer(ModEntities.LINKBOOK.get(),
          art.arcane.mystcraft.client.renderer.LinkbookEntityRenderer::new);
      event.registerEntityRenderer(ModEntities.METEOR.get(),
          art.arcane.mystcraft.client.renderer.MeteorEntityRenderer::new);
      event.registerEntityRenderer(ModEntities.FALLING_BLOCK.get(),
          art.arcane.mystcraft.client.renderer.MystcraftFallingBlockRenderer::new);
      event.registerEntityRenderer(ModEntities.COLORED_LIGHTNING.get(),
          art.arcane.mystcraft.client.renderer.ColoredLightningRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
      // Custom grass color handler with multi-color noise support
      net.minecraft.client.color.block.BlockColor grassColor = (state, level, pos, tintIndex) -> {
        if (level == null || pos == null) {
          return net.minecraft.world.level.GrassColor.getDefaultColor();
        }

        int ageUID = art.arcane.mystcraft.client.AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          java.util.List<Integer> colors = art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache.getGrassColors(ageUID);
          if (colors.size() == 1) {
            return colors.get(0);
          } else if (colors.size() >= 2) {
            return art.arcane.mystcraft.client.AgeColorUtils.selectColorFromPalette(colors, pos, ageUID);
          }
        }

        return net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos);
      };

      // Custom foliage color handler
      net.minecraft.client.color.block.BlockColor foliageColor = (state, level, pos, tintIndex) -> {
        if (level == null || pos == null) {
          return net.minecraft.world.level.FoliageColor.getDefaultColor();
        }

        int ageUID = art.arcane.mystcraft.client.AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          int customColor = art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache.getFoliageColor(ageUID);
          if (customColor != -1) {
            return customColor;
          }
        }

        return net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(level, pos);
      };

      // Custom water color handler
      net.minecraft.client.color.block.BlockColor waterColor = (state, level, pos, tintIndex) -> {
        if (level == null || pos == null) {
          return 0x3F76E4;
        }

        int ageUID = art.arcane.mystcraft.client.AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          int customColor = art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache.getWaterColor(ageUID);
          if (customColor != -1) {
            return customColor;
          }
        }

        return net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(level, pos);
      };

      // Register for grass blocks
      event.register(grassColor,
          net.minecraft.world.level.block.Blocks.GRASS_BLOCK, resolveShortGrass(), net.minecraft.world.level.block.Blocks.FERN,
          net.minecraft.world.level.block.Blocks.LARGE_FERN, net.minecraft.world.level.block.Blocks.POTTED_FERN, net.minecraft.world.level.block.Blocks.TALL_GRASS);

      // Register for foliage blocks
      event.register(foliageColor,
          net.minecraft.world.level.block.Blocks.OAK_LEAVES, net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES, net.minecraft.world.level.block.Blocks.BIRCH_LEAVES,
          net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES, net.minecraft.world.level.block.Blocks.ACACIA_LEAVES, net.minecraft.world.level.block.Blocks.DARK_OAK_LEAVES,
          net.minecraft.world.level.block.Blocks.MANGROVE_LEAVES, net.minecraft.world.level.block.Blocks.VINE);

      // Register for water
      event.register(waterColor, net.minecraft.world.level.block.Blocks.WATER, net.minecraft.world.level.block.Blocks.WATER_CAULDRON);

      // Portal color handler
      net.minecraft.client.color.block.BlockColor portalColor = (state, blockAndTintGetter, pos, tintIndex) -> {
        if (pos == null) {
          return 0x4488FF;
        }

        net.minecraft.world.level.Level clientLevel = net.minecraft.client.Minecraft.getInstance().level;
        if (clientLevel != null) {
          net.minecraft.world.level.block.entity.BlockEntity be = art.arcane.mystcraft.portal.PortalUtils.findReceptacle(clientLevel, pos);
          if (be instanceof art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity receptacle) {
            return receptacle.getPortalColor();
          }
        }

        return 0x4488FF;
      };

      event.register(portalColor, ModBlocks.LINK_PORTAL.get());

      Mystcraft.LOGGER.info("Registered Age block color handlers");
    }

    @SubscribeEvent
    public static void onRegisterItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
      // Guidebook and ink bucket colors
      event.register((stack, tintIndex) -> 0xFF303030, ModItems.GUIDEBOOK.get());
      event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF, ModItems.INK_BUCKET.get());

      // Item colors for grass/foliage blocks in inventory
      net.minecraft.client.color.item.ItemColor grassItemColor = (stack, tintIndex) -> {
        int ageUID = art.arcane.mystcraft.client.AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          java.util.List<Integer> colors = art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache.getGrassColors(ageUID);
          if (!colors.isEmpty()) {
            return colors.get(0);
          }
        }
        return net.minecraft.world.level.GrassColor.getDefaultColor();
      };

      net.minecraft.client.color.item.ItemColor foliageItemColor = (stack, tintIndex) -> {
        int ageUID = art.arcane.mystcraft.client.AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          int customColor = art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache.getFoliageColor(ageUID);
          if (customColor != -1) {
            return customColor;
          }
        }
        return net.minecraft.world.level.FoliageColor.getDefaultColor();
      };

      event.register(grassItemColor,
          net.minecraft.world.level.block.Blocks.GRASS_BLOCK, resolveShortGrass(), net.minecraft.world.level.block.Blocks.FERN,
          net.minecraft.world.level.block.Blocks.LARGE_FERN, net.minecraft.world.level.block.Blocks.TALL_GRASS);

      event.register(foliageItemColor,
          net.minecraft.world.level.block.Blocks.OAK_LEAVES, net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES, net.minecraft.world.level.block.Blocks.BIRCH_LEAVES,
          net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES, net.minecraft.world.level.block.Blocks.ACACIA_LEAVES, net.minecraft.world.level.block.Blocks.DARK_OAK_LEAVES,
          net.minecraft.world.level.block.Blocks.MANGROVE_LEAVES, net.minecraft.world.level.block.Blocks.VINE);
    }

    @SubscribeEvent
    public static void onRegisterDimensionEffects(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
      ResourceLocation ageEffectsKey = new ResourceLocation(Mystcraft.MOD_ID, "age");
      event.register(ageEffectsKey, new art.arcane.mystcraft.client.AgeDimensionSpecialEffects());
      Mystcraft.LOGGER.info("Registered shared DimensionSpecialEffects under key '{}'", ageEffectsKey);
    }
  }

  private static Object instantiateFirst(String preferredClass, String fallbackClass) {
    Class<?> type = loadClass(preferredClass, fallbackClass);
    try {
      return type.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to instantiate " + type.getName(), e);
    }
  }

  private static void invokeNoArg(Object target, String methodName) {
    try {
      target.getClass().getMethod(methodName).invoke(target);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to invoke " + methodName + " on " + target.getClass().getName(), e);
    }
  }

  private static net.minecraft.world.level.block.Block resolveShortGrass() {
    try {
      return (net.minecraft.world.level.block.Block) net.minecraft.world.level.block.Blocks.class.getField("SHORT_GRASS").get(null);
    } catch (ReflectiveOperationException ignored) {
      // Older versions use GRASS.
    }
    try {
      return (net.minecraft.world.level.block.Block) net.minecraft.world.level.block.Blocks.class.getField("GRASS").get(null);
    } catch (ReflectiveOperationException ignored) {
      // Fallback below.
    }
    return net.minecraft.world.level.block.Blocks.GRASS_BLOCK;
  }

  private static void invokeStaticNoArg(String preferredClass, String fallbackClass, String methodName) {
    Class<?> type = loadClass(preferredClass, fallbackClass);
    try {
      type.getMethod(methodName).invoke(null);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to invoke " + methodName + " on " + type.getName(), e);
    }
  }

  private static Class<?> loadClass(String preferredClass, String fallbackClass) {
    try {
      return Class.forName(preferredClass);
    } catch (ClassNotFoundException ignored) {
      try {
        return Class.forName(fallbackClass);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Missing expected class: " + preferredClass + " or " + fallbackClass, e);
      }
    }
  }
}
