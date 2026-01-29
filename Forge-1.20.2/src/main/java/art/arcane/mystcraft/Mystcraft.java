package art.arcane.mystcraft;

import art.arcane.mystcraft.config.MystcraftConfig;
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
import art.arcane.mystcraft.registry.ModVillagers;
import art.arcane.mystcraft.registry.ModWorldGen;
import art.arcane.mystcraft.registry.MystcraftRegistries;
import art.arcane.mystcraft.world.structure.ModStructures;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.grammar.GrammarRules;
import art.arcane.mystcraft.instability.InstabilityData;
import art.arcane.mystcraft.symbol.ModSymbols;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.logging.LogUtils;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.AgePerformanceTracker;
import art.arcane.mystcraft.world.AgeTrackingData;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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

        // Register configuration
        MystcraftConfig.register();

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
        ModVillagers.register();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Populate creative tab pages after symbols are registered
        modEventBus.addListener(ModCreativeTabs::onBuildCreativeTabContents);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Mystcraft registration complete");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Mystcraft common setup");

        event.enqueueWork(() -> {
            // Register advancement criteria triggers
            art.arcane.mystcraft.advancements.ModAdvancements.register();

            // Initialize networking
            MystcraftNetwork.register();

            // Initialize ink effects registry
            InkEffects.init();

            // Register all built-in symbols
            ModSymbols.registerAll();

            // Fire the event so third-party mods can register their symbols
            art.arcane.mystcraft.api.RegisterSymbolsEvent symbolEvent =
                    new art.arcane.mystcraft.api.RegisterSymbolsEvent();
            FMLJavaModLoadingContext.get().getModEventBus().post(symbolEvent);
            if (symbolEvent.getRegisteredCount() > 0) {
                LOGGER.info("Third-party mods registered {} additional symbols",
                        symbolEvent.getRegisteredCount());
            }

            // Freeze symbol registry to prevent late registration
            SymbolRegistry.freeze();

            // Initialize grammar rules for CFG-based Age generation
            GrammarRules.initialize();

            // Initialize instability providers and decks
            InstabilityData.initialize();
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Mystcraft server starting");
        // Commands are auto-registered via MystcraftCommands @EventBusSubscriber
        // Dimension data is loaded via AgeManager.get() on demand

        if (MystcraftConfig.deleteAgesOnStartup.get()) {
            deleteAllAges(event.getServer());
        }

        AgePerformanceTracker.logWorstAges(event.getServer());
    }

    /**
     * Deletes all Mystcraft Ages from the AgeManager and removes their dimension storage directories.
     */
    private void deleteAllAges(MinecraftServer server) {
        AgeManager ageManager = AgeManager.get(server);
        int ageCount = ageManager.getAgeCount();

        if (ageCount == 0) {
            LOGGER.info("[Mystcraft] deleteAgesOnStartup enabled but no ages to delete");
            return;
        }

        // Collect UIDs before clearing (iterator would be invalidated)
        List<Integer> uids = new ArrayList<>();
        for (int uid : ageManager.getAllAgeUIDs()) {
            uids.add(uid);
        }

        // Clear all tracking data and reset UID counter
        ageManager.clearAllAges();

        // Delete dimension storage directories from disk
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path dimensionsDir = worldDir.resolve("dimensions").resolve(MOD_ID);
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
                LOGGER.error("[Mystcraft] Failed to delete age dimension directories", e);
            }
        }

        LOGGER.info("[Mystcraft] deleteAgesOnStartup: deleted {} ages ({} directories removed)", ageCount, directoriesDeleted);
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if this is a Mystcraft Age
        if (!AgeDimensionFactory.isMystcraftAge(serverLevel.dimension())) {
            return;
        }

        // Ensure tracking data has a creation time for this age
        var ageData = art.arcane.mystcraft.world.AgeData.getIfPresent(serverLevel);
        if (ageData != null && ageData.getAgeUID() > 0) {
            AgeTrackingData.get(serverLevel.getServer()).ensureCreated(ageData.getAgeUID(), System.currentTimeMillis());
        }

        // Check if the chunk generator needs director reconstruction
        ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
        if (generator instanceof AgeChunkGenerator ageGen && ageGen.needsDirectorReconstruction()) {
            LOGGER.info("Reconstructing director for Mystcraft Age: {}", serverLevel.dimension().location());
            ageGen.reconstructDirectorFromAgeData(serverLevel);
        }

        AgePerformanceTracker.logWorstAges(serverLevel.getServer());
    }

    // Client-side setup is handled separately via Mod.EventBusSubscriber
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Mystcraft client setup");

            event.enqueueWork(() -> {
                // Initialize D'ni word rendering system
                art.arcane.mystcraft.client.render.DrawableWordManager.initialize();

                // Pre-warm page textures on a background thread
                art.arcane.mystcraft.client.render.PageItemRendererBEWLR.prewarmCache();

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
            LOGGER.info("Registering Mystcraft model layers");

            // Block entity model layers
            event.registerLayerDefinition(art.arcane.mystcraft.client.model.BookstandModel.LAYER_LOCATION,
                    art.arcane.mystcraft.client.model.BookstandModel::createBodyLayer);
            // LecternModel uses custom vertex rendering, no layer registration needed
            event.registerLayerDefinition(art.arcane.mystcraft.client.model.WritingDeskModel.LAYER_LOCATION,
                    art.arcane.mystcraft.client.model.WritingDeskModel::createBodyLayer);
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
            event.registerBlockEntityRenderer(ModBlockEntities.WRITING_DESK.get(),
                    art.arcane.mystcraft.client.renderer.WritingDeskRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.BOOK_RECEPTACLE.get(),
                    art.arcane.mystcraft.client.renderer.BookReceptacleRenderer::new);

            // Entity renderers
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
        public static void onRegisterItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
            // Tint the guidebook dark gray/black
            event.register((stack, tintIndex) -> 0xFF303030, ModItems.GUIDEBOOK.get());

            // Tint the ink bucket fluid layer black
            event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF, ModItems.INK_BUCKET.get());
        }

        // Model wrapping removed - using builtin/entity parent in page.json instead
        // which automatically uses the BEWLR registered via IClientItemExtensions
    }
}
