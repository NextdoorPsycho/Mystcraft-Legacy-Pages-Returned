package art.arcane.mystcraft;

import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModCreativeTabs;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModFluids;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.registry.ModLootModifiers;
import art.arcane.mystcraft.registry.ModMenuTypes;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.registry.ModWorldGen;
import art.arcane.mystcraft.registry.MystcraftRegistries;
import art.arcane.mystcraft.world.structure.ModStructures;
import art.arcane.mystcraft.symbol.ModSymbols;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mystcraft - A Minecraft mod that brings elements from the Myst series.
 * Allows players to write and travel to their own custom Ages (dimensions).
 *
 * Originally created by XCompWiz.
 * Ported to 1.20.2 and maintained by NextdoorPsycho.
 */
@Mod(Mystcraft.MOD_ID)
public class Mystcraft {
    public static final String MOD_ID = "mystcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Mystcraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("Mystcraft initializing...");

        // Register all deferred registers to the mod event bus
        MystcraftRegistries.register(modEventBus);

        // Force static initialization of all registry classes
        ModFluids.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModSounds.register();
        ModCreativeTabs.register();
        ModMenuTypes.register();
        ModLootModifiers.register();
        ModWorldGen.register(modEventBus);
        ModStructures.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Mystcraft registration complete");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Mystcraft common setup");

        event.enqueueWork(() -> {
            // Initialize networking
            MystcraftNetwork.register();

            // Register all symbols
            ModSymbols.registerAll();

            // Freeze symbol registry to prevent late registration
            SymbolRegistry.freeze();

            // TODO: Register capabilities
            // TODO: Setup grammar system
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Mystcraft server starting");

        // TODO: Register commands
        // TODO: Load dimension data
    }

    // Client-side setup is handled separately via Mod.EventBusSubscriber
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Mystcraft client setup");

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
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            LOGGER.info("Registering Mystcraft renderers");

            // Block entity renderers
            event.registerBlockEntityRenderer(ModBlockEntities.BOOKSTAND.get(),
                    art.arcane.mystcraft.client.renderer.BookstandRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.LECTERN.get(),
                    art.arcane.mystcraft.client.renderer.LecternRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.STAR_FISSURE.get(),
                    art.arcane.mystcraft.client.renderer.StarFissureRenderer::new);

            // Entity renderers
            event.registerEntityRenderer(ModEntities.LINKBOOK.get(),
                    art.arcane.mystcraft.client.renderer.LinkbookEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.METEOR.get(),
                    art.arcane.mystcraft.client.renderer.MeteorEntityRenderer::new);
        }
    }
}
