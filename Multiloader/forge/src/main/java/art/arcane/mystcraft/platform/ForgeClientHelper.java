package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IClientHelper;

// Forge client registration is handled via @SubscribeEvent on FMLClientSetupEvent and related events.
public class ForgeClientHelper implements IClientHelper {

    @Override
    public void registerMenuScreens() {
        // Registered in MystcraftForge via FMLClientSetupEvent
    }

    @Override
    public void registerBlockEntityRenderers() {
        // Registered via EntityRenderersEvent.RegisterRenderers
    }

    @Override
    public void registerEntityRenderers() {
        // Registered via EntityRenderersEvent.RegisterRenderers
    }

    @Override
    public void registerModelLayers() {
        // Registered via EntityRenderersEvent.RegisterLayerDefinitions
    }

    @Override
    public void registerColorHandlers() {
        // Registered via RegisterColorHandlersEvent
    }

    @Override
    public void registerRenderTypes() {
        // Registered in FMLClientSetupEvent
    }
}
