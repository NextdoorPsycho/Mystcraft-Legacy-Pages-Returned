package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.PocketHeadClientSync;
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
import art.arcane.mystcraft.fabric.client.FabricClientColors;
import art.arcane.mystcraft.fabric.client.FabricMystcraftFallingBlockRenderer;
import art.arcane.mystcraft.item.ItemClientHooks;
import art.arcane.mystcraft.network.ConfigSyncPacket;
import art.arcane.mystcraft.network.DimensionSyncPacket;
import art.arcane.mystcraft.network.ProfilingStatePacket;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.network.SyncAgeDataPacket;
import art.arcane.mystcraft.platform.Services;
import art.arcane.mystcraft.util.ClientAccess;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.PackType;

/** Production Fabric client entry point for Minecraft 26.2. */
public final class MystcraftFabricClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    Mystcraft.LOGGER.info("[Mystcraft] Starting Fabric 26.2 client initialization");

    FabricSpecialModelRenderers.register();
    FabricMystcraftClientNetwork.register();
    installCommonClientBridges();
    registerConnectionLifecycle();

    DrawableWordManager.initialize();
    PageItemRendererBEWLR.prewarmCache();
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
        .registerReloadListener(new FabricReloadListeners.ProceduralUi());

    registerScreens();
    registerRenderers();
    FabricClientColors.register();
    Services.EVENTS.registerClientEvents();

    Mystcraft.LOGGER.info("[Mystcraft] Fabric 26.2 client initialization complete");
  }

  private static void installCommonClientBridges() {
    ClientAccess.install(() -> Minecraft.getInstance().level);
    ItemClientHooks.setBookOpener(BookScreen::open);
    ItemClientHooks.setGuidebookOpener(() ->
        Minecraft.getInstance().setScreenAndShow(new GuidebookScreen()));
    MystcraftLecternHelper.setClientBookOpener(BookScreen::openForBlock);
    SymbolSyncPacket.setClientSymbolsAppliedHandler(() -> {
      PageItemRendererBEWLR.clearCache();
      PageItemRendererBEWLR.prewarmCache();
    });
  }

  private static void registerConnectionLifecycle() {
    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        PocketHeadClientSync.requestSend());
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      PocketHeadClientSync.reset();
      SyncAgeDataPacket.ClientAgeDataCache.clear();
      DimensionSyncPacket.ClientDimensionCache.clear();
      ConfigSyncPacket.ClientConfigCache.clear();
      ProfilingStatePacket.ProfilingState.reset();
    });
    ClientTickEvents.END_CLIENT_TICK.register(client -> PocketHeadClientSync.tick());
  }

  private static void registerScreens() {
    MenuScreens.register(FabricRegistries.INK_MIXER_MENU.get(), InkMixerScreen::new);
    MenuScreens.register(FabricRegistries.BOOK_BINDER_MENU.get(), BookBinderScreen::new);
    MenuScreens.register(FabricRegistries.LINK_MODIFIER_MENU.get(), LinkModifierScreen::new);
    MenuScreens.register(FabricRegistries.WRITING_DESK_MENU.get(), WritingDeskScreen::new);
    MenuScreens.register(FabricRegistries.FOLDER_MENU.get(), FolderScreen::new);
    MenuScreens.register(FabricRegistries.PORTFOLIO_MENU.get(), PortfolioScreen::new);
  }

  private static void registerRenderers() {
    ModelLayerRegistry.registerModelLayer(
        WritingDeskModel.LAYER_LOCATION, WritingDeskModel::createBodyLayer);

    BlockEntityRendererRegistry.register(
        FabricRegistries.STAR_FISSURE_BE.get(), StarFissureRenderer::new);
    BlockEntityRendererRegistry.register(
        FabricRegistries.WRITING_DESK_BE.get(), WritingDeskRenderer::new);
    BlockEntityRendererRegistry.register(
        FabricRegistries.BOOK_RECEPTACLE_BE.get(), BookReceptacleRenderer::new);

    EntityRendererRegistry.register(
        FabricRegistries.LINKBOOK_ENTITY.get(), LinkbookEntityRenderer::new);
    EntityRendererRegistry.register(
        FabricRegistries.PERSONAL_POCKET_PROXY_ENTITY.get(), PersonalPocketProxyRenderer::new);
    EntityRendererRegistry.register(
        FabricRegistries.METEOR_ENTITY.get(), MeteorEntityRenderer::new);
    EntityRendererRegistry.register(
        FabricRegistries.FALLING_BLOCK_ENTITY.get(), FabricMystcraftFallingBlockRenderer::new);
    EntityRendererRegistry.register(
        FabricRegistries.COLORED_LIGHTNING_ENTITY.get(), ColoredLightningRenderer::new);
  }
}
