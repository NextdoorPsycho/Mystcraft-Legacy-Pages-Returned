package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.LinkPortalBlockEntity;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUiReload;
import art.arcane.mystcraft.client.model.WritingDeskModel;
import art.arcane.mystcraft.client.render.DrawableWordManager;
import art.arcane.mystcraft.client.render.PageItemRendererBEWLR;
import art.arcane.mystcraft.client.renderer.BookReceptacleRenderer;
import art.arcane.mystcraft.client.renderer.ColoredLightningRenderer;
import art.arcane.mystcraft.client.renderer.LinkbookEntityRenderer;
import art.arcane.mystcraft.client.renderer.MeteorEntityRenderer;
import art.arcane.mystcraft.client.renderer.PersonalPocketProxyRenderer;
import art.arcane.mystcraft.client.renderer.StarFissureRenderer;
import art.arcane.mystcraft.client.renderer.WritingDeskRenderer;
import art.arcane.mystcraft.client.screen.BookBinderScreen;
import art.arcane.mystcraft.client.screen.BookScreen;
import art.arcane.mystcraft.client.screen.FolderScreen;
import art.arcane.mystcraft.client.screen.GuidebookScreen;
import art.arcane.mystcraft.client.screen.InkMixerScreen;
import art.arcane.mystcraft.client.screen.LinkModifierScreen;
import art.arcane.mystcraft.client.screen.PortfolioScreen;
import art.arcane.mystcraft.client.screen.WritingDeskScreen;
import art.arcane.mystcraft.item.ItemClientHooks;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModMenuTypes;
import art.arcane.mystcraft.util.ClientAccess;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only Forge registration entrypoint. */
final class ForgeClientSetup {

  private ForgeClientSetup() {
  }

  static void register(BusGroup modBusGroup) {
    ForgeMystcraftNetwork.installClientContextFactory(ForgeClientPacketContext::new);
    ForgeSpecialModelRenderers.register();

    ClientAccess.install(() -> Minecraft.getInstance().level);
    ItemClientHooks.setBookOpener(BookScreen::open);
    ItemClientHooks.setGuidebookOpener(() ->
        Minecraft.getInstance().setScreenAndShow(new GuidebookScreen()));
    MystcraftLecternHelper.setClientBookOpener(BookScreen::openForBlock);
    SymbolSyncPacket.setClientSymbolsAppliedHandler(
        ForgeClientSetup::refreshProceduralSymbolCache);

    FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientSetup::onClientSetup);
    EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(
        ForgeClientSetup::registerLayerDefinitions);
    EntityRenderersEvent.RegisterRenderers.BUS.addListener(
        ForgeClientSetup::registerRenderers);
    RegisterColorHandlersEvent.Block.BUS.addListener(ForgeClientSetup::registerBlockColors);
    RegisterClientReloadListenersEvent.BUS.addListener(ForgeClientSetup::registerReloadListeners);
  }

  private static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
      DrawableWordManager.initialize();
      PageItemRendererBEWLR.prewarmCache();
      MenuScreens.register(ModMenuTypes.INK_MIXER.get(), InkMixerScreen::new);
      MenuScreens.register(ModMenuTypes.BOOK_BINDER.get(), BookBinderScreen::new);
      MenuScreens.register(ModMenuTypes.LINK_MODIFIER.get(), LinkModifierScreen::new);
      MenuScreens.register(ModMenuTypes.WRITING_DESK.get(), WritingDeskScreen::new);
      MenuScreens.register(ModMenuTypes.FOLDER.get(), FolderScreen::new);
      MenuScreens.register(ModMenuTypes.PORTFOLIO.get(), PortfolioScreen::new);
      Mystcraft.LOGGER.info("[Mystcraft] Forge client setup complete");
    });
  }

  private static void registerLayerDefinitions(
      EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(
        WritingDeskModel.LAYER_LOCATION, WritingDeskModel::createBodyLayer);
  }

  private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(
        ModBlockEntities.STAR_FISSURE.get(), StarFissureRenderer::new);
    event.registerBlockEntityRenderer(
        ModBlockEntities.WRITING_DESK.get(), WritingDeskRenderer::new);
    event.registerBlockEntityRenderer(
        ModBlockEntities.BOOK_RECEPTACLE.get(), BookReceptacleRenderer::new);

    event.registerEntityRenderer(ModEntities.LINKBOOK.get(), LinkbookEntityRenderer::new);
    event.registerEntityRenderer(
        ModEntities.PERSONAL_POCKET_PROXY.get(), PersonalPocketProxyRenderer::new);
    event.registerEntityRenderer(
        ModEntities.FALLING_BLOCK.get(), ForgeMystcraftFallingBlockRenderer::new);
    event.registerEntityRenderer(ModEntities.METEOR.get(), MeteorEntityRenderer::new);
    event.registerEntityRenderer(
        ModEntities.COLORED_LIGHTNING.get(), ColoredLightningRenderer::new);
  }

  private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
    event.register(List.of(new GrassTint()),
        Blocks.GRASS_BLOCK,
        Blocks.SHORT_GRASS,
        Blocks.FERN,
        Blocks.LARGE_FERN,
        Blocks.POTTED_FERN,
        Blocks.TALL_GRASS);
    event.register(List.of(new FoliageTint()),
        Blocks.OAK_LEAVES,
        Blocks.SPRUCE_LEAVES,
        Blocks.BIRCH_LEAVES,
        Blocks.JUNGLE_LEAVES,
        Blocks.ACACIA_LEAVES,
        Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES,
        Blocks.VINE);
    event.register(List.of(new WaterTint()), Blocks.WATER, Blocks.WATER_CAULDRON);
    event.register(List.of(new PortalTint()), ModBlocks.LINK_PORTAL.get());
  }

  private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(ProceduralUiReload.instance());
  }

  private static void refreshProceduralSymbolCache() {
    PageItemRendererBEWLR.clearCache();
    PageItemRendererBEWLR.prewarmCache();
  }

  private static final class GrassTint implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUid);
        if (!colors.isEmpty()) {
          return colors.getFirst();
        }
      }
      return GrassColor.getDefaultColor();
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUid);
        if (colors.size() == 1) {
          return colors.getFirst();
        }
        if (colors.size() >= 2) {
          return AgeColorUtils.selectColorFromPalette(colors, pos, ageUid);
        }
      }
      return BiomeColors.getAverageGrassColor(level, pos);
    }
  }

  private static final class FoliageTint implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        int customColor = ClientAgeDataCache.getFoliageColor(ageUid);
        if (customColor != -1) {
          return customColor;
        }
      }
      return FoliageColor.FOLIAGE_DEFAULT;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        int customColor = ClientAgeDataCache.getFoliageColor(ageUid);
        if (customColor != -1) {
          return customColor;
        }
      }
      return BiomeColors.getAverageFoliageColor(level, pos);
    }
  }

  private static final class WaterTint implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        int customColor = ClientAgeDataCache.getWaterColor(ageUid);
        if (customColor != -1) {
          return customColor;
        }
      }
      return 0x3F76E4;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        int customColor = ClientAgeDataCache.getWaterColor(ageUid);
        if (customColor != -1) {
          return customColor;
        }
      }
      return BiomeColors.getAverageWaterColor(level, pos);
    }
  }

  private static final class PortalTint implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return 0x4488FF;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof LinkPortalBlockEntity portal) {
        return portal.getPortalColor();
      }
      if (Minecraft.getInstance().level != null
          && PortalUtils.findReceptacle(Minecraft.getInstance().level, pos)
              instanceof BookReceptacleBlockEntity receptacle) {
        return receptacle.getPortalColor();
      }
      return 0x4488FF;
    }
  }
}
