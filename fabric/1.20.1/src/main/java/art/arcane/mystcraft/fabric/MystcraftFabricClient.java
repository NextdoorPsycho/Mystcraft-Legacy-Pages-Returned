package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.FabricAgeBlockColorHandler;
import art.arcane.mystcraft.client.PocketHeadClientSync;
import art.arcane.mystcraft.client.model.BookstandModel;
import art.arcane.mystcraft.client.model.WritingDeskModel;
import art.arcane.mystcraft.client.render.DrawableWordManager;
import art.arcane.mystcraft.client.render.PageItemRendererBEWLR;
import art.arcane.mystcraft.client.renderer.*;
import art.arcane.mystcraft.client.screen.*;
import art.arcane.mystcraft.network.FabricMystcraftNetwork;
import art.arcane.mystcraft.registry.FabricRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

/**
 * Fabric client entry point for Mystcraft 1.20.1.
 * Self-contained - does not pull from fabric/src.
 */
public class MystcraftFabricClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    Mystcraft.LOGGER.info("[Mystcraft] Client setup (1.20.1)");

    // Register client-side network receivers for S->C packets
    FabricMystcraftNetwork.registerClient();

    // Client -> Server head palette sync (dev/offline-safe)
    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PocketHeadClientSync.requestSend());
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PocketHeadClientSync.reset());
    ClientTickEvents.END_CLIENT_TICK.register(client -> PocketHeadClientSync.tick());

    // Initialize client-side systems
    DrawableWordManager.initialize();
    PageItemRendererBEWLR.prewarmCache();

    // Register menu screens
    ScreenRegistry.register(FabricRegistries.INK_MIXER_MENU.get(), InkMixerScreen::new);
    ScreenRegistry.register(FabricRegistries.BOOK_BINDER_MENU.get(), BookBinderScreen::new);
    ScreenRegistry.register(FabricRegistries.LINK_MODIFIER_MENU.get(), LinkModifierScreen::new);
    ScreenRegistry.register(FabricRegistries.WRITING_DESK_MENU.get(), WritingDeskScreen::new);
    ScreenRegistry.register(FabricRegistries.FOLDER_MENU.get(), FolderScreen::new);
    ScreenRegistry.register(FabricRegistries.PORTFOLIO_MENU.get(), PortfolioScreen::new);

    // Register block entity renderers
    BlockEntityRendererRegistry.register(FabricRegistries.BOOKSTAND_BE.get(), BookstandRenderer::new);
    BlockEntityRendererRegistry.register(FabricRegistries.STAR_FISSURE_BE.get(), StarFissureRenderer::new);
    BlockEntityRendererRegistry.register(FabricRegistries.WRITING_DESK_BE.get(), WritingDeskRenderer::new);
    BlockEntityRendererRegistry.register(FabricRegistries.BOOK_RECEPTACLE_BE.get(), BookReceptacleRenderer::new);

    // Register entity renderers
    EntityRendererRegistry.register(FabricRegistries.LINKBOOK_ENTITY.get(), LinkbookEntityRenderer::new);
    EntityRendererRegistry.register(FabricRegistries.METEOR_ENTITY.get(), MeteorEntityRenderer::new);
    EntityRendererRegistry.register(FabricRegistries.FALLING_BLOCK_ENTITY.get(), MystcraftFallingBlockRenderer::new);
    EntityRendererRegistry.register(FabricRegistries.COLORED_LIGHTNING_ENTITY.get(), ColoredLightningRenderer::new);

    // Register model layers
    EntityModelLayerRegistry.registerModelLayer(BookstandModel.LAYER_LOCATION, BookstandModel::createBodyLayer);
    EntityModelLayerRegistry.registerModelLayer(WritingDeskModel.LAYER_LOCATION, WritingDeskModel::createBodyLayer);

    // Register item colors
    ColorProviderRegistry.ITEM.register(
        (stack, tintIndex) -> 0xFF303030,
        FabricRegistries.GUIDEBOOK.get());
    ColorProviderRegistry.ITEM.register(
        (stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF,
        FabricRegistries.INK_BUCKET.get());

    // Register Mystcraft age-related block/item colors
    FabricAgeBlockColorHandler.register();

    // Register BEWLR for page item
    BuiltinItemRendererRegistry.INSTANCE.register(
        FabricRegistries.PAGE.get(),
        (stack, mode, poseStack, bufferSource, light, overlay) ->
            PageItemRendererBEWLR.getInstance().renderByItem(
                stack, mode, poseStack, bufferSource, light, overlay));
  }
}
