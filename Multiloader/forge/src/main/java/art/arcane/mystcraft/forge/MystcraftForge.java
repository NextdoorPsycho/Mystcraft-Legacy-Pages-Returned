package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.ForgeMystcraftConfig;
import art.arcane.mystcraft.network.ForgeMystcraftNetwork;
import art.arcane.mystcraft.registry.ForgeModBlockEntities;
import art.arcane.mystcraft.registry.ForgeModBlocks;
import art.arcane.mystcraft.registry.ModCreativeTabs;
import art.arcane.mystcraft.registry.ForgeModEntities;
import art.arcane.mystcraft.registry.ForgeModFluids;
import art.arcane.mystcraft.registry.ForgeModItems;
import art.arcane.mystcraft.registry.ModLootModifiers;
import art.arcane.mystcraft.registry.ForgeModMenuTypes;
import art.arcane.mystcraft.registry.ForgeModSounds;
import art.arcane.mystcraft.registry.ModVillagers;
import art.arcane.mystcraft.registry.ModWorldGen;
import art.arcane.mystcraft.registry.MystcraftRegistries;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.structure.ForgeModStructures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
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

/** Forge mod entry point. */
@Mod(Mystcraft.MOD_ID)
public class MystcraftForge {

    public MystcraftForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Mystcraft.LOGGER.info("[Mystcraft] Forge initialization starting...");

        // Populate common stubs from Forge registries before any common code runs
        populateCommonRegistries();

        ForgeMystcraftConfig.register();
        MystcraftRegistries.register(modEventBus);

        ForgeModFluids.register();
        ForgeModBlocks.register();
        ForgeModItems.register();
        ForgeModBlockEntities.register();
        ForgeModEntities.register();
        ForgeModSounds.register();
        ModCreativeTabs.register();
        ForgeModMenuTypes.register();
        ModLootModifiers.register();
        ModWorldGen.register(modEventBus);
        ForgeModStructures.register(modEventBus);
        ModVillagers.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModCreativeTabs::onBuildCreativeTabContents);

        MinecraftForge.EVENT_BUS.register(this);

        Mystcraft.init();

        Mystcraft.LOGGER.info("[Mystcraft] Forge registration complete");
    }

    /**
     * Populates common registry stubs from Forge RegistryObjects.
     * RegistryObject implements Supplier, so direct assignment works.
     */
    private static void populateCommonRegistries() {
        // Blocks
        art.arcane.mystcraft.registry.ModBlocks.INK_MIXER = ForgeModBlocks.INK_MIXER;
        art.arcane.mystcraft.registry.ModBlocks.BOOK_BINDER = ForgeModBlocks.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModBlocks.BOOK_RECEPTACLE = ForgeModBlocks.BOOK_RECEPTACLE;
        art.arcane.mystcraft.registry.ModBlocks.BOOKSTAND = ForgeModBlocks.BOOKSTAND;
        art.arcane.mystcraft.registry.ModBlocks.LECTERN = ForgeModBlocks.LECTERN;
        art.arcane.mystcraft.registry.ModBlocks.LINK_MODIFIER = ForgeModBlocks.LINK_MODIFIER;
        art.arcane.mystcraft.registry.ModBlocks.WRITING_DESK = ForgeModBlocks.WRITING_DESK;
        art.arcane.mystcraft.registry.ModBlocks.CRYSTAL = ForgeModBlocks.CRYSTAL;
        art.arcane.mystcraft.registry.ModBlocks.DECAY = ForgeModBlocks.DECAY;
        art.arcane.mystcraft.registry.ModBlocks.LINK_PORTAL = ForgeModBlocks.LINK_PORTAL;
        art.arcane.mystcraft.registry.ModBlocks.STAR_FISSURE = ForgeModBlocks.STAR_FISSURE;
        art.arcane.mystcraft.registry.ModBlocks.FLUID_INK = ForgeModBlocks.FLUID_INK;

        // Items
        art.arcane.mystcraft.registry.ModItems.PAGE = ForgeModItems.PAGE;
        art.arcane.mystcraft.registry.ModItems.AGEBOOK = ForgeModItems.AGEBOOK;
        art.arcane.mystcraft.registry.ModItems.LINKBOOK = ForgeModItems.LINKBOOK;
        art.arcane.mystcraft.registry.ModItems.LINKBOOK_UNLINKED = ForgeModItems.LINKBOOK_UNLINKED;
        art.arcane.mystcraft.registry.ModItems.BOOSTER_PACK = ForgeModItems.BOOSTER_PACK;
        art.arcane.mystcraft.registry.ModItems.FOLDER = ForgeModItems.FOLDER;
        art.arcane.mystcraft.registry.ModItems.PORTFOLIO = ForgeModItems.PORTFOLIO;
        art.arcane.mystcraft.registry.ModItems.INK_VIAL = ForgeModItems.INK_VIAL;
        art.arcane.mystcraft.registry.ModItems.GLASSES = ForgeModItems.GLASSES;
        art.arcane.mystcraft.registry.ModItems.GUIDEBOOK = ForgeModItems.GUIDEBOOK;
        art.arcane.mystcraft.registry.ModItems.INK_BUCKET = ForgeModItems.INK_BUCKET;
        art.arcane.mystcraft.registry.ModItems.INK_MIXER_ITEM = ForgeModItems.INK_MIXER_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOK_BINDER_ITEM = ForgeModItems.BOOK_BINDER_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOK_RECEPTACLE_ITEM = ForgeModItems.BOOK_RECEPTACLE_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOKSTAND_ITEM = ForgeModItems.BOOKSTAND_ITEM;
        art.arcane.mystcraft.registry.ModItems.LECTERN_ITEM = ForgeModItems.LECTERN_ITEM;
        art.arcane.mystcraft.registry.ModItems.LINK_MODIFIER_ITEM = ForgeModItems.LINK_MODIFIER_ITEM;
        art.arcane.mystcraft.registry.ModItems.WRITING_DESK_ITEM = ForgeModItems.WRITING_DESK_ITEM;
        art.arcane.mystcraft.registry.ModItems.CRYSTAL_ITEM = ForgeModItems.CRYSTAL_ITEM;
        art.arcane.mystcraft.registry.ModItems.DECAY_ITEM = ForgeModItems.DECAY_ITEM;

        // Entities
        art.arcane.mystcraft.registry.ModEntities.LINKBOOK = ForgeModEntities.LINKBOOK;
        art.arcane.mystcraft.registry.ModEntities.FALLING_BLOCK = ForgeModEntities.FALLING_BLOCK;
        art.arcane.mystcraft.registry.ModEntities.METEOR = ForgeModEntities.METEOR;
        art.arcane.mystcraft.registry.ModEntities.COLORED_LIGHTNING = ForgeModEntities.COLORED_LIGHTNING;

        // Block Entities
        art.arcane.mystcraft.registry.ModBlockEntities.INK_MIXER = ForgeModBlockEntities.INK_MIXER;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOK_BINDER = ForgeModBlockEntities.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOK_RECEPTACLE = ForgeModBlockEntities.BOOK_RECEPTACLE;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOKSTAND = ForgeModBlockEntities.BOOKSTAND;
        art.arcane.mystcraft.registry.ModBlockEntities.LECTERN = ForgeModBlockEntities.LECTERN;
        art.arcane.mystcraft.registry.ModBlockEntities.WRITING_DESK = ForgeModBlockEntities.WRITING_DESK;
        art.arcane.mystcraft.registry.ModBlockEntities.STAR_FISSURE = ForgeModBlockEntities.STAR_FISSURE;
        art.arcane.mystcraft.registry.ModBlockEntities.LINK_MODIFIER = ForgeModBlockEntities.LINK_MODIFIER;

        // Fluids
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_SOURCE = ForgeModFluids.BLACK_INK_SOURCE;
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_FLOWING = ForgeModFluids.BLACK_INK_FLOWING;
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_BUCKET = ForgeModItems.INK_BUCKET;

        // Sounds
        art.arcane.mystcraft.registry.ModSounds.LINKING_POP = ForgeModSounds.LINKING_POP;
        art.arcane.mystcraft.registry.ModSounds.LINKING_LINK = ForgeModSounds.LINKING_LINK;
        art.arcane.mystcraft.registry.ModSounds.LINKING_DISARM = ForgeModSounds.LINKING_DISARM;
        art.arcane.mystcraft.registry.ModSounds.LINKING_FOLLOWING = ForgeModSounds.LINKING_FOLLOWING;
        art.arcane.mystcraft.registry.ModSounds.LINKING_INTRA = ForgeModSounds.LINKING_INTRA;
        art.arcane.mystcraft.registry.ModSounds.LINKING_FISSURE = ForgeModSounds.LINKING_FISSURE;
        art.arcane.mystcraft.registry.ModSounds.LINKING_PORTAL = ForgeModSounds.LINKING_PORTAL;
        art.arcane.mystcraft.registry.ModSounds.METEOR_ROAR = ForgeModSounds.METEOR_ROAR;
        art.arcane.mystcraft.registry.ModSounds.METEOR_IMPACT = ForgeModSounds.METEOR_IMPACT;

        // Menu Types
        art.arcane.mystcraft.registry.ModMenuTypes.INK_MIXER = ForgeModMenuTypes.INK_MIXER;
        art.arcane.mystcraft.registry.ModMenuTypes.BOOK_BINDER = ForgeModMenuTypes.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModMenuTypes.LINK_MODIFIER = ForgeModMenuTypes.LINK_MODIFIER;
        art.arcane.mystcraft.registry.ModMenuTypes.WRITING_DESK = ForgeModMenuTypes.WRITING_DESK;
        art.arcane.mystcraft.registry.ModMenuTypes.FOLDER = ForgeModMenuTypes.FOLDER;
        art.arcane.mystcraft.registry.ModMenuTypes.PORTFOLIO = ForgeModMenuTypes.PORTFOLIO;

        // Structures
        art.arcane.mystcraft.world.structure.ModStructures.ABANDONED_LIBRARY = ForgeModStructures.ABANDONED_LIBRARY;
        art.arcane.mystcraft.world.structure.ModStructures.UNDERGROUND_ARCHIVE = ForgeModStructures.UNDERGROUND_ARCHIVE;
        art.arcane.mystcraft.world.structure.ModStructures.SCATTERED_LIBRARY = ForgeModStructures.SCATTERED_LIBRARY;

        // Config - wire Forge config values to common suppliers
        art.arcane.mystcraft.config.MystcraftConfig.giveGuidebookOnFirstSpawn = ForgeMystcraftConfig.giveGuidebookOnFirstSpawn::get;
        art.arcane.mystcraft.config.MystcraftConfig.maxSymbolsPerBook = ForgeMystcraftConfig.maxSymbolsPerBook::get;
        art.arcane.mystcraft.config.MystcraftConfig.deleteAgesOnStartup = ForgeMystcraftConfig.deleteAgesOnStartup::get;
        art.arcane.mystcraft.config.MystcraftConfig.instabilityEnabled = ForgeMystcraftConfig.instabilityEnabled::get;
        art.arcane.mystcraft.config.MystcraftConfig.deathEffectsEnabled = ForgeMystcraftConfig.deathEffectsEnabled::get;
        art.arcane.mystcraft.config.MystcraftConfig.allowUnstableAges = ForgeMystcraftConfig.allowUnstableAges::get;
        art.arcane.mystcraft.config.MystcraftConfig.instabilityMultiplier = ForgeMystcraftConfig.instabilityMultiplier::get;
        art.arcane.mystcraft.config.MystcraftConfig.maxAllowedInstability = ForgeMystcraftConfig.maxAllowedInstability::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdDecay = ForgeMystcraftConfig.thresholdDecay::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdTransmute = ForgeMystcraftConfig.thresholdTransmute::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdLightning = ForgeMystcraftConfig.thresholdLightning::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdMeteor = ForgeMystcraftConfig.thresholdMeteor::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdPoison = ForgeMystcraftConfig.thresholdPoison::get;
        art.arcane.mystcraft.config.MystcraftConfig.thresholdWither = ForgeMystcraftConfig.thresholdWither::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceDecay = ForgeMystcraftConfig.chanceDecay::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceTransmute = ForgeMystcraftConfig.chanceTransmute::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceLightning = ForgeMystcraftConfig.chanceLightning::get;
        art.arcane.mystcraft.config.MystcraftConfig.chanceMeteor = ForgeMystcraftConfig.chanceMeteor::get;
        art.arcane.mystcraft.config.MystcraftConfig.chancePlayerEffect = ForgeMystcraftConfig.chancePlayerEffect::get;

        // Network
        art.arcane.mystcraft.network.MystcraftNetwork.sendToServerHandler = ForgeMystcraftNetwork::sendToServer;
        art.arcane.mystcraft.network.MystcraftNetwork.sendToPlayerHandler = ForgeMystcraftNetwork::sendToPlayer;
        art.arcane.mystcraft.network.MystcraftNetwork.sendToAllHandler = ForgeMystcraftNetwork::sendToAll;
        art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingHandler = ForgeMystcraftNetwork::sendToTracking;
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            art.arcane.mystcraft.advancements.ModAdvancements.register();
            ForgeMystcraftNetwork.register();

            Mystcraft.commonSetup();

            art.arcane.mystcraft.api.RegisterSymbolsEvent symbolEvent =
                    new art.arcane.mystcraft.api.RegisterSymbolsEvent();
            FMLJavaModLoadingContext.get().getModEventBus().post(symbolEvent);
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

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!AgeDimensionFactory.isMystcraftAge(serverLevel.dimension())) {
            return;
        }

        ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
        if (generator instanceof AgeChunkGenerator ageGen && ageGen.needsDirectorReconstruction()) {
            Mystcraft.LOGGER.info("[Mystcraft] Reconstructing director for Age: {}", serverLevel.dimension().location());
            ageGen.reconstructDirectorFromAgeData(serverLevel);
        }
    }

    @Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            Mystcraft.LOGGER.info("[Mystcraft] Client setup");

            event.enqueueWork(() -> {
                art.arcane.mystcraft.client.render.DrawableWordManager.initialize();
                art.arcane.mystcraft.client.render.PageItemRendererBEWLR.prewarmCache();

                net.minecraft.client.gui.screens.MenuScreens.register(
                        ForgeModMenuTypes.INK_MIXER.get(),
                        art.arcane.mystcraft.client.screen.InkMixerScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        ForgeModMenuTypes.BOOK_BINDER.get(),
                        art.arcane.mystcraft.client.screen.BookBinderScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        ForgeModMenuTypes.LINK_MODIFIER.get(),
                        art.arcane.mystcraft.client.screen.LinkModifierScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        ForgeModMenuTypes.WRITING_DESK.get(),
                        art.arcane.mystcraft.client.screen.WritingDeskScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        ForgeModMenuTypes.FOLDER.get(),
                        art.arcane.mystcraft.client.screen.FolderScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        ForgeModMenuTypes.PORTFOLIO.get(),
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

            event.registerBlockEntityRenderer(ForgeModBlockEntities.BOOKSTAND.get(),
                    art.arcane.mystcraft.client.renderer.BookstandRenderer::new);
            event.registerBlockEntityRenderer(ForgeModBlockEntities.LECTERN.get(),
                    art.arcane.mystcraft.client.renderer.LecternRenderer::new);
            event.registerBlockEntityRenderer(ForgeModBlockEntities.STAR_FISSURE.get(),
                    art.arcane.mystcraft.client.renderer.StarFissureRenderer::new);
            event.registerBlockEntityRenderer(ForgeModBlockEntities.WRITING_DESK.get(),
                    art.arcane.mystcraft.client.renderer.WritingDeskRenderer::new);
            event.registerBlockEntityRenderer(ForgeModBlockEntities.BOOK_RECEPTACLE.get(),
                    art.arcane.mystcraft.client.renderer.BookReceptacleRenderer::new);

            event.registerEntityRenderer(ForgeModEntities.LINKBOOK.get(),
                    art.arcane.mystcraft.client.renderer.LinkbookEntityRenderer::new);
            event.registerEntityRenderer(ForgeModEntities.METEOR.get(),
                    art.arcane.mystcraft.client.renderer.MeteorEntityRenderer::new);
            event.registerEntityRenderer(ForgeModEntities.FALLING_BLOCK.get(),
                    art.arcane.mystcraft.client.renderer.MystcraftFallingBlockRenderer::new);
            event.registerEntityRenderer(ForgeModEntities.COLORED_LIGHTNING.get(),
                    art.arcane.mystcraft.client.renderer.ColoredLightningRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
            event.register((stack, tintIndex) -> 0xFF303030, ForgeModItems.GUIDEBOOK.get());
            event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF1A1A1A : 0xFFFFFFFF, ForgeModItems.INK_BUCKET.get());
        }
    }
}
