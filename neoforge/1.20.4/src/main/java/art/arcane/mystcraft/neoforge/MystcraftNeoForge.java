package art.arcane.mystcraft.neoforge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.network.NeoForgeMystcraftNetwork;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import art.arcane.mystcraft.neoforge.NeoForgeAdvancementTriggers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * NeoForge mod entry point.
 */
@Mod(Mystcraft.MOD_ID)
public class MystcraftNeoForge {

  private final IEventBus modEventBus;

  public MystcraftNeoForge(IEventBus modEventBus) {
    this.modEventBus = modEventBus;

    Mystcraft.LOGGER.info("[Mystcraft] NeoForge initialization starting...");

    // Register config first
    NeoForgeMystcraftConfig.register();

    // Use the consolidated registry system
    NeoForgeRegistries.register(modEventBus);
    // Register advancement triggers via DeferredRegister in 1.20.4
    NeoForgeAdvancementTriggers.register(modEventBus);
    art.arcane.mystcraft.advancements.ModAdvancements.register();

    modEventBus.addListener(this::commonSetup);
    modEventBus.addListener(NeoForgeRegistries::onBuildCreativeTabContents);
    modEventBus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) -> {
      Mystcraft.LOGGER.info("[Mystcraft] Registering GameTests (NeoForge)");
      event.register(art.arcane.mystcraft.gametest.MystcraftNeoForgeGameTests.class);
    });

    NeoForge.EVENT_BUS.register(this);

    Mystcraft.init();

    Mystcraft.LOGGER.info("[Mystcraft] NeoForge registration complete");
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      NeoForgeMystcraftNetwork.register();

      Mystcraft.commonSetup();

      Mystcraft.finishSymbolRegistration();
    });
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    Mystcraft.LOGGER.info("[Mystcraft] Server starting");
    Mystcraft.setCurrentServer(event.getServer());

    if (NeoForgeMystcraftConfig.deleteAgesOnStartup.get()) {
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

  @Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      Mystcraft.LOGGER.info("[Mystcraft] Client setup");

      event.enqueueWork(() -> {
        art.arcane.mystcraft.client.render.DrawableWordManager.initialize();
        art.arcane.mystcraft.client.render.PageItemRendererBEWLR.prewarmCache();

        net.minecraft.client.gui.screens.MenuScreens.register(
            NeoForgeRegistries.INK_MIXER_MENU.get(),
            art.arcane.mystcraft.client.screen.InkMixerScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            NeoForgeRegistries.BOOK_BINDER_MENU.get(),
            art.arcane.mystcraft.client.screen.BookBinderScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            NeoForgeRegistries.LINK_MODIFIER_MENU.get(),
            art.arcane.mystcraft.client.screen.LinkModifierScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            NeoForgeRegistries.WRITING_DESK_MENU.get(),
            art.arcane.mystcraft.client.screen.WritingDeskScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            NeoForgeRegistries.FOLDER_MENU.get(),
            art.arcane.mystcraft.client.screen.FolderScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
            NeoForgeRegistries.PORTFOLIO_MENU.get(),
            art.arcane.mystcraft.client.screen.PortfolioScreen::new);
      });
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
      Mystcraft.LOGGER.info("[Mystcraft] Registering model layers");

      event.registerLayerDefinition(art.arcane.mystcraft.client.model.BookstandModel.LAYER_LOCATION,
          art.arcane.mystcraft.client.model.BookstandModel::createBodyLayer);
      event.registerLayerDefinition(art.arcane.mystcraft.client.model.WritingDeskModel.LAYER_LOCATION,
          art.arcane.mystcraft.client.model.WritingDeskModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
      Mystcraft.LOGGER.info("[Mystcraft] Registering renderers");

      event.registerBlockEntityRenderer(NeoForgeRegistries.BOOKSTAND_BE.get(),
          art.arcane.mystcraft.client.renderer.BookstandRenderer::new);
      event.registerBlockEntityRenderer(NeoForgeRegistries.STAR_FISSURE_BE.get(),
          art.arcane.mystcraft.client.renderer.StarFissureRenderer::new);
      event.registerBlockEntityRenderer(NeoForgeRegistries.WRITING_DESK_BE.get(),
          art.arcane.mystcraft.client.renderer.WritingDeskRenderer::new);
      event.registerBlockEntityRenderer(NeoForgeRegistries.BOOK_RECEPTACLE_BE.get(),
          art.arcane.mystcraft.client.renderer.BookReceptacleRenderer::new);

      event.registerEntityRenderer(NeoForgeRegistries.LINKBOOK_ENTITY.get(),
          art.arcane.mystcraft.client.renderer.LinkbookEntityRenderer::new);
      event.registerEntityRenderer(NeoForgeRegistries.METEOR_ENTITY.get(),
          art.arcane.mystcraft.client.renderer.MeteorEntityRenderer::new);
      event.registerEntityRenderer(NeoForgeRegistries.FALLING_BLOCK_ENTITY.get(),
          art.arcane.mystcraft.client.renderer.MystcraftFallingBlockRenderer::new);
      event.registerEntityRenderer(NeoForgeRegistries.COLORED_LIGHTNING_ENTITY.get(),
          art.arcane.mystcraft.client.renderer.ColoredLightningRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
      BlockColor grassColor = (state, level, pos, tintIndex) -> {
        if (level == null || pos == null) {
          return GrassColor.getDefaultColor();
        }

        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          java.util.List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
          if (colors.size() == 1) {
            return colors.get(0);
          } else if (colors.size() >= 2) {
            return AgeColorUtils.selectColorFromPalette(colors, pos, ageUID);
          }
        }

        return BiomeColors.getAverageGrassColor(level, pos);
      };

      BlockColor foliageColor = (state, level, pos, tintIndex) -> {
        if (level == null || pos == null) {
          return FoliageColor.getDefaultColor();
        }

        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          int customColor = ClientAgeDataCache.getFoliageColor(ageUID);
          if (customColor != -1) {
            return customColor;
          }
        }

        return BiomeColors.getAverageFoliageColor(level, pos);
      };

      BlockColor waterColor = (state, level, pos, tintIndex) -> {
        if (level == null || pos == null) {
          return 0x3F76E4;
        }

        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          int customColor = ClientAgeDataCache.getWaterColor(ageUID);
          if (customColor != -1) {
            return customColor;
          }
        }

        return BiomeColors.getAverageWaterColor(level, pos);
      };

      event.register(grassColor,
          Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN,
          Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.TALL_GRASS);

      event.register(foliageColor,
          Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
          Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
          Blocks.MANGROVE_LEAVES, Blocks.VINE);

      event.register(waterColor, Blocks.WATER, Blocks.WATER_CAULDRON);

      BlockColor portalColor = (state, blockAndTintGetter, pos, tintIndex) -> {
        if (pos == null) {
          return 0x4488FF;
        }

        Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel != null) {
          BlockEntity be = PortalUtils.findReceptacle(clientLevel, pos);
          if (be instanceof BookReceptacleBlockEntity receptacle) {
            return receptacle.getPortalColor();
          }
        }

        return 0x4488FF;
      };

      event.register(portalColor, NeoForgeRegistries.LINK_PORTAL.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
      event.register((stack, tintIndex) -> 0xFF303030, NeoForgeRegistries.GUIDEBOOK.get());
      event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF, NeoForgeRegistries.INK_BUCKET.get());

      ItemColor grassItemColor = (stack, tintIndex) -> {
        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          java.util.List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
          if (!colors.isEmpty()) {
            return colors.get(0);
          }
        }
        return GrassColor.getDefaultColor();
      };

      ItemColor foliageItemColor = (stack, tintIndex) -> {
        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          int customColor = ClientAgeDataCache.getFoliageColor(ageUID);
          if (customColor != -1) {
            return customColor;
          }
        }
        return FoliageColor.getDefaultColor();
      };

      event.register(grassItemColor,
          Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN,
          Blocks.LARGE_FERN, Blocks.TALL_GRASS);

      event.register(foliageItemColor,
          Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
          Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
          Blocks.MANGROVE_LEAVES, Blocks.VINE);
    }
  }
}
