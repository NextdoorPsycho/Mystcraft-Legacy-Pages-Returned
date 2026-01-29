package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.model.BookstandModel;
import art.arcane.mystcraft.client.model.WritingDeskModel;
import art.arcane.mystcraft.client.render.DrawableWordManager;
import art.arcane.mystcraft.client.render.PageItemRendererBEWLR;
import art.arcane.mystcraft.client.renderer.BookReceptacleRenderer;
import art.arcane.mystcraft.client.renderer.BookstandRenderer;
import art.arcane.mystcraft.client.renderer.ColoredLightningRenderer;
import art.arcane.mystcraft.client.renderer.LinkbookEntityRenderer;
import art.arcane.mystcraft.client.renderer.MeteorEntityRenderer;
import art.arcane.mystcraft.client.renderer.MystcraftFallingBlockRenderer;
import art.arcane.mystcraft.client.renderer.StarFissureRenderer;
import art.arcane.mystcraft.client.renderer.WritingDeskRenderer;
import art.arcane.mystcraft.client.screen.BookBinderScreen;
import art.arcane.mystcraft.client.screen.FolderScreen;
import art.arcane.mystcraft.client.screen.InkMixerScreen;
import art.arcane.mystcraft.client.screen.LinkModifierScreen;
import art.arcane.mystcraft.client.screen.PortfolioScreen;
import art.arcane.mystcraft.client.screen.WritingDeskScreen;
import art.arcane.mystcraft.registry.FabricModBlockEntities;
import art.arcane.mystcraft.registry.FabricModEntities;
import art.arcane.mystcraft.registry.FabricModItems;
import art.arcane.mystcraft.network.FabricMystcraftNetwork;
import art.arcane.mystcraft.registry.FabricModMenuTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

/** Fabric client entry point for 1.20.1. */
public class MystcraftFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Mystcraft.LOGGER.info("[Mystcraft] Client setup (1.20.1)");

        // Register client-side network receivers for S->C packets
        FabricMystcraftNetwork.registerClient();

        // Initialize client-side systems
        DrawableWordManager.initialize();
        PageItemRendererBEWLR.prewarmCache();

        // Register menu screens
        ScreenRegistry.register(FabricModMenuTypes.INK_MIXER.get(), InkMixerScreen::new);
        ScreenRegistry.register(FabricModMenuTypes.BOOK_BINDER.get(), BookBinderScreen::new);
        ScreenRegistry.register(FabricModMenuTypes.LINK_MODIFIER.get(), LinkModifierScreen::new);
        ScreenRegistry.register(FabricModMenuTypes.WRITING_DESK.get(), WritingDeskScreen::new);
        ScreenRegistry.register(FabricModMenuTypes.FOLDER.get(), FolderScreen::new);
        ScreenRegistry.register(FabricModMenuTypes.PORTFOLIO.get(), PortfolioScreen::new);

        // Register block entity renderers
        BlockEntityRendererRegistry.register(FabricModBlockEntities.BOOKSTAND.get(), BookstandRenderer::new);
        BlockEntityRendererRegistry.register(FabricModBlockEntities.STAR_FISSURE.get(), StarFissureRenderer::new);
        BlockEntityRendererRegistry.register(FabricModBlockEntities.WRITING_DESK.get(), WritingDeskRenderer::new);
        BlockEntityRendererRegistry.register(FabricModBlockEntities.BOOK_RECEPTACLE.get(), BookReceptacleRenderer::new);

        // Register entity renderers
        EntityRendererRegistry.register(FabricModEntities.LINKBOOK.get(), LinkbookEntityRenderer::new);
        EntityRendererRegistry.register(FabricModEntities.METEOR.get(), MeteorEntityRenderer::new);
        EntityRendererRegistry.register(FabricModEntities.FALLING_BLOCK.get(), MystcraftFallingBlockRenderer::new);
        EntityRendererRegistry.register(FabricModEntities.COLORED_LIGHTNING.get(), ColoredLightningRenderer::new);

        // Register model layers
        EntityModelLayerRegistry.registerModelLayer(BookstandModel.LAYER_LOCATION, BookstandModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(WritingDeskModel.LAYER_LOCATION, WritingDeskModel::createBodyLayer);

        // Register item colors
        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> 0xFF303030,
                FabricModItems.GUIDEBOOK.get());
        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF,
                FabricModItems.INK_BUCKET.get());

        // Register BEWLR for page item
        BuiltinItemRendererRegistry.INSTANCE.register(
                FabricModItems.PAGE.get(),
                (stack, mode, poseStack, bufferSource, light, overlay) ->
                        PageItemRendererBEWLR.getInstance().renderByItem(
                                stack, mode, poseStack, bufferSource, light, overlay));
    }
}
