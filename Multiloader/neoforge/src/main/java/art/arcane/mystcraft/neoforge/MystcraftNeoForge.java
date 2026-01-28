package art.arcane.mystcraft.neoforge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.NeoForgeMystcraftConfig;
import art.arcane.mystcraft.network.NeoForgeMystcraftNetwork;
import art.arcane.mystcraft.registry.NeoForgeModBlockEntities;
import art.arcane.mystcraft.registry.NeoForgeModBlocks;
import art.arcane.mystcraft.registry.ModCreativeTabs;
import art.arcane.mystcraft.registry.NeoForgeModEntities;
import art.arcane.mystcraft.registry.NeoForgeModFluids;
import art.arcane.mystcraft.registry.NeoForgeModItems;
import art.arcane.mystcraft.registry.ModLootModifiers;
import art.arcane.mystcraft.registry.NeoForgeModMenuTypes;
import art.arcane.mystcraft.registry.NeoForgeModSounds;
import art.arcane.mystcraft.registry.ModVillagers;
import art.arcane.mystcraft.registry.ModWorldGen;
import art.arcane.mystcraft.registry.MystcraftRegistries;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.structure.NeoForgeModStructures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** NeoForge mod entry point. */
@Mod(Mystcraft.MOD_ID)
public class MystcraftNeoForge {

    private final IEventBus modEventBus;

    public MystcraftNeoForge(IEventBus modEventBus) {
        this.modEventBus = modEventBus;

        Mystcraft.LOGGER.info("[Mystcraft] NeoForge initialization starting...");

        // Populate common stubs from NeoForge registries before any common code runs
        populateCommonRegistries();

        NeoForgeMystcraftConfig.register();
        MystcraftRegistries.register(modEventBus);

        NeoForgeModFluids.register();
        NeoForgeModBlocks.register();
        NeoForgeModItems.register();
        NeoForgeModBlockEntities.register();
        NeoForgeModEntities.register();
        NeoForgeModSounds.register();
        ModCreativeTabs.register();
        NeoForgeModMenuTypes.register();
        ModLootModifiers.register();
        ModWorldGen.register(modEventBus);
        NeoForgeModStructures.register(modEventBus);
        ModVillagers.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModCreativeTabs::onBuildCreativeTabContents);

        NeoForge.EVENT_BUS.register(this);

        Mystcraft.init();

        Mystcraft.LOGGER.info("[Mystcraft] NeoForge registration complete");
    }

    /**
     * Populates common registry stubs from NeoForge DeferredHolders.
     * DeferredHolder implements Supplier, so direct assignment works.
     */
    private static void populateCommonRegistries() {
        // Blocks
        art.arcane.mystcraft.registry.ModBlocks.INK_MIXER = NeoForgeModBlocks.INK_MIXER;
        art.arcane.mystcraft.registry.ModBlocks.BOOK_BINDER = NeoForgeModBlocks.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModBlocks.BOOK_RECEPTACLE = NeoForgeModBlocks.BOOK_RECEPTACLE;
        art.arcane.mystcraft.registry.ModBlocks.BOOKSTAND = NeoForgeModBlocks.BOOKSTAND;
        art.arcane.mystcraft.registry.ModBlocks.LECTERN = NeoForgeModBlocks.LECTERN;
        art.arcane.mystcraft.registry.ModBlocks.LINK_MODIFIER = NeoForgeModBlocks.LINK_MODIFIER;
        art.arcane.mystcraft.registry.ModBlocks.WRITING_DESK = NeoForgeModBlocks.WRITING_DESK;
        art.arcane.mystcraft.registry.ModBlocks.CRYSTAL = NeoForgeModBlocks.CRYSTAL;
        art.arcane.mystcraft.registry.ModBlocks.DECAY = NeoForgeModBlocks.DECAY;
        art.arcane.mystcraft.registry.ModBlocks.LINK_PORTAL = NeoForgeModBlocks.LINK_PORTAL;
        art.arcane.mystcraft.registry.ModBlocks.STAR_FISSURE = NeoForgeModBlocks.STAR_FISSURE;
        art.arcane.mystcraft.registry.ModBlocks.FLUID_INK = NeoForgeModBlocks.FLUID_INK;

        // Items
        art.arcane.mystcraft.registry.ModItems.PAGE = NeoForgeModItems.PAGE;
        art.arcane.mystcraft.registry.ModItems.AGEBOOK = NeoForgeModItems.AGEBOOK;
        art.arcane.mystcraft.registry.ModItems.LINKBOOK = NeoForgeModItems.LINKBOOK;
        art.arcane.mystcraft.registry.ModItems.LINKBOOK_UNLINKED = NeoForgeModItems.LINKBOOK_UNLINKED;
        art.arcane.mystcraft.registry.ModItems.BOOSTER_PACK = NeoForgeModItems.BOOSTER_PACK;
        art.arcane.mystcraft.registry.ModItems.FOLDER = NeoForgeModItems.FOLDER;
        art.arcane.mystcraft.registry.ModItems.PORTFOLIO = NeoForgeModItems.PORTFOLIO;
        art.arcane.mystcraft.registry.ModItems.INK_VIAL = NeoForgeModItems.INK_VIAL;
        art.arcane.mystcraft.registry.ModItems.GLASSES = NeoForgeModItems.GLASSES;
        art.arcane.mystcraft.registry.ModItems.GUIDEBOOK = NeoForgeModItems.GUIDEBOOK;
        art.arcane.mystcraft.registry.ModItems.INK_BUCKET = NeoForgeModItems.INK_BUCKET;
        art.arcane.mystcraft.registry.ModItems.INK_MIXER_ITEM = NeoForgeModItems.INK_MIXER_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOK_BINDER_ITEM = NeoForgeModItems.BOOK_BINDER_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOK_RECEPTACLE_ITEM = NeoForgeModItems.BOOK_RECEPTACLE_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOKSTAND_ITEM = NeoForgeModItems.BOOKSTAND_ITEM;
        art.arcane.mystcraft.registry.ModItems.LECTERN_ITEM = NeoForgeModItems.LECTERN_ITEM;
        art.arcane.mystcraft.registry.ModItems.LINK_MODIFIER_ITEM = NeoForgeModItems.LINK_MODIFIER_ITEM;
        art.arcane.mystcraft.registry.ModItems.WRITING_DESK_ITEM = NeoForgeModItems.WRITING_DESK_ITEM;
        art.arcane.mystcraft.registry.ModItems.CRYSTAL_ITEM = NeoForgeModItems.CRYSTAL_ITEM;
        art.arcane.mystcraft.registry.ModItems.DECAY_ITEM = NeoForgeModItems.DECAY_ITEM;

        // Entities
        art.arcane.mystcraft.registry.ModEntities.LINKBOOK = NeoForgeModEntities.LINKBOOK;
        art.arcane.mystcraft.registry.ModEntities.FALLING_BLOCK = NeoForgeModEntities.FALLING_BLOCK;
        art.arcane.mystcraft.registry.ModEntities.METEOR = NeoForgeModEntities.METEOR;
        art.arcane.mystcraft.registry.ModEntities.COLORED_LIGHTNING = NeoForgeModEntities.COLORED_LIGHTNING;

        // Block Entities
        art.arcane.mystcraft.registry.ModBlockEntities.INK_MIXER = NeoForgeModBlockEntities.INK_MIXER;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOK_BINDER = NeoForgeModBlockEntities.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOK_RECEPTACLE = NeoForgeModBlockEntities.BOOK_RECEPTACLE;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOKSTAND = NeoForgeModBlockEntities.BOOKSTAND;
        art.arcane.mystcraft.registry.ModBlockEntities.LECTERN = NeoForgeModBlockEntities.LECTERN;
        art.arcane.mystcraft.registry.ModBlockEntities.WRITING_DESK = NeoForgeModBlockEntities.WRITING_DESK;
        art.arcane.mystcraft.registry.ModBlockEntities.STAR_FISSURE = NeoForgeModBlockEntities.STAR_FISSURE;
        art.arcane.mystcraft.registry.ModBlockEntities.LINK_MODIFIER = NeoForgeModBlockEntities.LINK_MODIFIER;

        // Fluids
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_SOURCE = NeoForgeModFluids.BLACK_INK_SOURCE;
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_FLOWING = NeoForgeModFluids.BLACK_INK_FLOWING;
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_BUCKET = NeoForgeModItems.INK_BUCKET;

        // Sounds
        art.arcane.mystcraft.registry.ModSounds.LINKING_POP = NeoForgeModSounds.LINKING_POP;
        art.arcane.mystcraft.registry.ModSounds.LINKING_LINK = NeoForgeModSounds.LINKING_LINK;
        art.arcane.mystcraft.registry.ModSounds.LINKING_DISARM = NeoForgeModSounds.LINKING_DISARM;
        art.arcane.mystcraft.registry.ModSounds.LINKING_FOLLOWING = NeoForgeModSounds.LINKING_FOLLOWING;
        art.arcane.mystcraft.registry.ModSounds.LINKING_INTRA = NeoForgeModSounds.LINKING_INTRA;
        art.arcane.mystcraft.registry.ModSounds.LINKING_FISSURE = NeoForgeModSounds.LINKING_FISSURE;
        art.arcane.mystcraft.registry.ModSounds.LINKING_PORTAL = NeoForgeModSounds.LINKING_PORTAL;
        art.arcane.mystcraft.registry.ModSounds.METEOR_ROAR = NeoForgeModSounds.METEOR_ROAR;
        art.arcane.mystcraft.registry.ModSounds.METEOR_IMPACT = NeoForgeModSounds.METEOR_IMPACT;

        // Menu Types
        art.arcane.mystcraft.registry.ModMenuTypes.INK_MIXER = NeoForgeModMenuTypes.INK_MIXER;
        art.arcane.mystcraft.registry.ModMenuTypes.BOOK_BINDER = NeoForgeModMenuTypes.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModMenuTypes.LINK_MODIFIER = NeoForgeModMenuTypes.LINK_MODIFIER;
        art.arcane.mystcraft.registry.ModMenuTypes.WRITING_DESK = NeoForgeModMenuTypes.WRITING_DESK;
        art.arcane.mystcraft.registry.ModMenuTypes.FOLDER = NeoForgeModMenuTypes.FOLDER;
        art.arcane.mystcraft.registry.ModMenuTypes.PORTFOLIO = NeoForgeModMenuTypes.PORTFOLIO;

        // Structures
        art.arcane.mystcraft.world.structure.ModStructures.ABANDONED_LIBRARY = NeoForgeModStructures.ABANDONED_LIBRARY;
        art.arcane.mystcraft.world.structure.ModStructures.UNDERGROUND_ARCHIVE = NeoForgeModStructures.UNDERGROUND_ARCHIVE;
        art.arcane.mystcraft.world.structure.ModStructures.SCATTERED_LIBRARY = NeoForgeModStructures.SCATTERED_LIBRARY;

        // Config - wire NeoForge config values to common suppliers
        art.arcane.mystcraft.config.MystcraftConfig.giveGuidebookOnFirstSpawn = NeoForgeMystcraftConfig.giveGuidebookOnFirstSpawn::get;
        art.arcane.mystcraft.config.MystcraftConfig.maxSymbolsPerBook = NeoForgeMystcraftConfig.maxSymbolsPerBook::get;
        art.arcane.mystcraft.config.MystcraftConfig.deleteAgesOnStartup = NeoForgeMystcraftConfig.deleteAgesOnStartup::get;
        art.arcane.mystcraft.config.MystcraftConfig.instabilityEnabled = NeoForgeMystcraftConfig.instabilityEnabled::get;
        art.arcane.mystcraft.config.MystcraftConfig.deathEffectsEnabled = NeoForgeMystcraftConfig.deathEffectsEnabled::get;
        art.arcane.mystcraft.config.MystcraftConfig.allowUnstableAges = NeoForgeMystcraftConfig.allowUnstableAges::get;
        art.arcane.mystcraft.config.MystcraftConfig.instabilityMultiplier = NeoForgeMystcraftConfig.instabilityMultiplier::get;
        art.arcane.mystcraft.config.MystcraftConfig.maxAllowedInstability = NeoForgeMystcraftConfig.maxAllowedInstability::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdDecay = NeoForgeMystcraftConfig.thresholdDecay::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdTransmute = NeoForgeMystcraftConfig.thresholdTransmute::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdLightning = NeoForgeMystcraftConfig.thresholdLightning::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdMeteor = NeoForgeMystcraftConfig.thresholdMeteor::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdPoison = NeoForgeMystcraftConfig.thresholdPoison::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdWither = NeoForgeMystcraftConfig.thresholdWither::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceDecay = NeoForgeMystcraftConfig.chanceDecay::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceTransmute = NeoForgeMystcraftConfig.chanceTransmute::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceLightning = NeoForgeMystcraftConfig.chanceLightning::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceMeteor = NeoForgeMystcraftConfig.chanceMeteor::get;
        art.arcane.mystcraft.config.MystcraftConfig.chancePlayerEffect = NeoForgeMystcraftConfig.chancePlayerEffect::get;

        // Network
        art.arcane.mystcraft.network.MystcraftNetwork.sendToServerHandler = NeoForgeMystcraftNetwork::sendToServer;
        art.arcane.mystcraft.network.MystcraftNetwork.sendToPlayerHandler = NeoForgeMystcraftNetwork::sendToPlayer;
        art.arcane.mystcraft.network.MystcraftNetwork.sendToAllHandler = NeoForgeMystcraftNetwork::sendToAll;
        art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingHandler = NeoForgeMystcraftNetwork::sendToTracking;
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            art.arcane.mystcraft.advancements.ModAdvancements.register();
            NeoForgeMystcraftNetwork.register();

            Mystcraft.commonSetup();

            art.arcane.mystcraft.api.RegisterSymbolsEvent symbolEvent =
                    new art.arcane.mystcraft.api.RegisterSymbolsEvent();
            modEventBus.post(symbolEvent);
            if (symbolEvent.getRegisteredCount() > 0) {
                Mystcraft.LOGGER.info("[Mystcraft] Third-party mods registered {} additional symbols",
                        symbolEvent.getRegisteredCount());
            }
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
                        NeoForgeModMenuTypes.INK_MIXER.get(),
                        art.arcane.mystcraft.client.screen.InkMixerScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        NeoForgeModMenuTypes.BOOK_BINDER.get(),
                        art.arcane.mystcraft.client.screen.BookBinderScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        NeoForgeModMenuTypes.LINK_MODIFIER.get(),
                        art.arcane.mystcraft.client.screen.LinkModifierScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        NeoForgeModMenuTypes.WRITING_DESK.get(),
                        art.arcane.mystcraft.client.screen.WritingDeskScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        NeoForgeModMenuTypes.FOLDER.get(),
                        art.arcane.mystcraft.client.screen.FolderScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        NeoForgeModMenuTypes.PORTFOLIO.get(),
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

            event.registerBlockEntityRenderer(NeoForgeModBlockEntities.BOOKSTAND.get(),
                    art.arcane.mystcraft.client.renderer.BookstandRenderer::new);
            event.registerBlockEntityRenderer(NeoForgeModBlockEntities.LECTERN.get(),
                    art.arcane.mystcraft.client.renderer.LecternRenderer::new);
            event.registerBlockEntityRenderer(NeoForgeModBlockEntities.STAR_FISSURE.get(),
                    art.arcane.mystcraft.client.renderer.StarFissureRenderer::new);
            event.registerBlockEntityRenderer(NeoForgeModBlockEntities.WRITING_DESK.get(),
                    art.arcane.mystcraft.client.renderer.WritingDeskRenderer::new);
            event.registerBlockEntityRenderer(NeoForgeModBlockEntities.BOOK_RECEPTACLE.get(),
                    art.arcane.mystcraft.client.renderer.BookReceptacleRenderer::new);

            event.registerEntityRenderer(NeoForgeModEntities.LINKBOOK.get(),
                    art.arcane.mystcraft.client.renderer.LinkbookEntityRenderer::new);
            event.registerEntityRenderer(NeoForgeModEntities.METEOR.get(),
                    art.arcane.mystcraft.client.renderer.MeteorEntityRenderer::new);
            event.registerEntityRenderer(NeoForgeModEntities.FALLING_BLOCK.get(),
                    art.arcane.mystcraft.client.renderer.MystcraftFallingBlockRenderer::new);
            event.registerEntityRenderer(NeoForgeModEntities.COLORED_LIGHTNING.get(),
                    art.arcane.mystcraft.client.renderer.ColoredLightningRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
            event.register((stack, tintIndex) -> 0xFF303030, NeoForgeModItems.GUIDEBOOK.get());
            event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF, NeoForgeModItems.INK_BUCKET.get());
        }
    }
}
