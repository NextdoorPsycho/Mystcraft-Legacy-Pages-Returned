package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.config.ForgeMystcraftConfig;
import art.arcane.mystcraft.network.ForgeMystcraftNetwork_1_18_2;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.platform.ForgeEventHelper_1_18_2;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.*;
import art.arcane.mystcraft.world.AgeManager;
import com.floopowder.api.Floo;
import com.floopowder.forge.ForgeFlooRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.WorldEvent;
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
 * Forge mod entry point for 1.18.2.
 *
 * Note: 1.18.2 doesn't have some features available in 1.20.x:
 * - No AgeDimensionSpecialEffects registration event
 * - No PageItemRendererBEWLR
 * - No DrawableWordManager
 * - Different color handler events
 * - Different block entity renderer events
 */
@Mod(Mystcraft.MOD_ID)
public class MystcraftForge {

  // Default grass color for inventory items (1.18.2 doesn't have GrassColor.getDefaultColor())
  private static final int DEFAULT_GRASS_COLOR = GrassColor.get(0.5, 1.0);

  public MystcraftForge() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    Mystcraft.LOGGER.info("[Mystcraft] Forge 1.18.2 initialization starting...");

    // Initialize Floo_Powder registry FIRST
    Floo.setRegistry(new ForgeFlooRegistry(Mystcraft.MOD_ID, modEventBus));
    Mystcraft.LOGGER.info("[Mystcraft] Floo registry initialized");

    // Register Forge config
    ForgeMystcraftConfig.register();

    // Use the new centralized registration system
    ModRegistrations.registerAll(modEventBus);

    modEventBus.addListener(this::commonSetup);
    modEventBus.addListener(this::registerGameTests);

    MinecraftForge.EVENT_BUS.register(this);

    // Register all events via the consolidated ForgeEventHelper
    ForgeEventHelper_1_18_2 eventHelper = new ForgeEventHelper_1_18_2();
    eventHelper.registerServerEvents();
    eventHelper.registerClientEvents();
    eventHelper.registerCommonEvents();

    Mystcraft.init();

    Mystcraft.LOGGER.info("[Mystcraft] Forge 1.18.2 registration complete");
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      art.arcane.mystcraft.advancements.ModAdvancements.register();
      ForgeMystcraftNetwork_1_18_2.register();

      Mystcraft.commonSetup();

      Mystcraft.finishSymbolRegistration();
    });
  }

  private void registerGameTests(net.minecraftforge.event.RegisterGameTestsEvent event) {
    Mystcraft.LOGGER.info("[Mystcraft] Registering GameTests");
    event.register(art.arcane.mystcraft.gametest.MystcraftForgeGameTests.class);
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

  // 1.18.2: WorldEvent.Load instead of LevelEvent.Load
  @SubscribeEvent
  public void onWorldLoad(WorldEvent.Load event) {
    if (!(event.getWorld() instanceof ServerLevel serverLevel)) {
      return;
    }

    // 1.18.2: Check dimension without AgeDimensionFactory (excluded from common)
    String dimPath = serverLevel.dimension().location().getPath();
    if (!dimPath.startsWith("mystcraft_age_")) {
      return;
    }

    art.arcane.mystcraft.event.AgeDeathHandler.configureAgeGameRules(serverLevel);
  }

  /**
   * 1.18.2 client events - use different event classes
   */
  @Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      Mystcraft.LOGGER.info("[Mystcraft] Client setup");

      event.enqueueWork(() -> {
        // Register menu screens
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

      // 1.18.2: Register only available renderers (some are excluded from common)
      event.registerEntityRenderer(ModEntities.FALLING_BLOCK.get(),
          art.arcane.mystcraft.client.renderer.MystcraftFallingBlockRenderer::new);
    }

    // 1.18.2: ColorHandlerEvent.Block instead of RegisterColorHandlersEvent.Block
    @SubscribeEvent
    public static void onRegisterBlockColors(ColorHandlerEvent.Block event) {
      // Custom grass color handler with multi-color noise support
      BlockColor grassColor = (state, level, pos, tintIndex) -> {
        if (level == null || pos == null) {
          return DEFAULT_GRASS_COLOR;
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

      // Custom foliage color handler
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

      // Custom water color handler
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

      // Register for grass blocks
      event.getBlockColors().register(grassColor,
          Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.FERN,
          Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.TALL_GRASS);

      // Register for foliage blocks (no MANGROVE_LEAVES in 1.18.2)
      event.getBlockColors().register(foliageColor,
          Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
          Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
          Blocks.VINE);

      // Register for water
      event.getBlockColors().register(waterColor, Blocks.WATER, Blocks.WATER_CAULDRON);

      // Portal color handler
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

      event.getBlockColors().register(portalColor, ModBlocks.LINK_PORTAL.get());

      Mystcraft.LOGGER.info("Registered Age block color handlers");
    }

    // 1.18.2: ColorHandlerEvent.Item instead of RegisterColorHandlersEvent.Item
    @SubscribeEvent
    public static void onRegisterItemColors(ColorHandlerEvent.Item event) {
      // Guidebook and ink bucket colors
      event.getItemColors().register((stack, tintIndex) -> 0xFF303030, ModItems.GUIDEBOOK.get());
      event.getItemColors().register((stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF, ModItems.INK_BUCKET.get());

      // Item colors for grass/foliage blocks in inventory
      ItemColor grassItemColor = (stack, tintIndex) -> {
        int ageUID = AgeColorUtils.getCurrentAgeUID();
        if (ageUID >= 0) {
          java.util.List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
          if (!colors.isEmpty()) {
            return colors.get(0);
          }
        }
        return DEFAULT_GRASS_COLOR;
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

      event.getItemColors().register(grassItemColor,
          Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.FERN,
          Blocks.LARGE_FERN, Blocks.TALL_GRASS);

      // No MANGROVE_LEAVES in 1.18.2
      event.getItemColors().register(foliageItemColor,
          Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
          Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
          Blocks.VINE);
    }
  }
}
