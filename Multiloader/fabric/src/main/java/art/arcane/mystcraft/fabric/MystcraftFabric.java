package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.FabricMystcraftConfig;
import art.arcane.mystcraft.event.FabricEventRegistration;
import art.arcane.mystcraft.network.FabricMystcraftNetwork;
import art.arcane.mystcraft.registry.FabricModBlockEntities;
import art.arcane.mystcraft.registry.FabricModBlocks;
import art.arcane.mystcraft.registry.ModCreativeTabs;
import art.arcane.mystcraft.registry.FabricModEntities;
import art.arcane.mystcraft.registry.FabricModFluids;
import art.arcane.mystcraft.registry.FabricModItems;
import art.arcane.mystcraft.registry.FabricModMenuTypes;
import art.arcane.mystcraft.registry.FabricModSounds;
import art.arcane.mystcraft.registry.ModVillagers;
import art.arcane.mystcraft.registry.ModWorldGen;
import art.arcane.mystcraft.registry.FabricModStructures;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Fabric mod entry point. */
public class MystcraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Mystcraft.LOGGER.info("[Mystcraft] Fabric initialization starting...");

        FabricMystcraftConfig.load();

        // Register all content
        FabricModFluids.register();
        FabricModBlocks.register();
        FabricModItems.register();
        FabricModBlockEntities.register();
        FabricModEntities.register();
        FabricModSounds.register();
        ModCreativeTabs.register();
        FabricModMenuTypes.register();
        ModVillagers.register();
        ModWorldGen.register();
        FabricModStructures.register();

        // Populate common stubs from Fabric registry objects
        populateCommonRegistries();

        Mystcraft.init();

        // Network registration
        FabricMystcraftNetwork.register();

        // Common setup (equivalent to FMLCommonSetupEvent)
        art.arcane.mystcraft.advancements.ModAdvancements.register();
        Mystcraft.commonSetup();

        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Mystcraft.LOGGER.info("[Mystcraft] Server starting");
            Mystcraft.setCurrentServer(server);

            if (FabricMystcraftConfig.deleteAgesOnStartup.get()) {
                deleteAllAges(server);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Mystcraft.LOGGER.info("[Mystcraft] Server stopped");
            Mystcraft.setCurrentServer(null);
        });

        // Level load event for Age director reconstruction
        ServerWorldEvents.LOAD.register((server, level) -> onLevelLoad(level));

        // Register all Fabric event callbacks
        FabricEventRegistration.registerAll();

        Mystcraft.LOGGER.info("[Mystcraft] Fabric registration complete");
    }

    /** Populates common registry stubs with Fabric-registered objects. */
    private static void populateCommonRegistries() {
        // Blocks
        art.arcane.mystcraft.registry.ModBlocks.INK_MIXER = FabricModBlocks.INK_MIXER;
        art.arcane.mystcraft.registry.ModBlocks.BOOK_BINDER = FabricModBlocks.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModBlocks.BOOK_RECEPTACLE = FabricModBlocks.BOOK_RECEPTACLE;
        art.arcane.mystcraft.registry.ModBlocks.BOOKSTAND = FabricModBlocks.BOOKSTAND;
        art.arcane.mystcraft.registry.ModBlocks.LECTERN = FabricModBlocks.LECTERN;
        art.arcane.mystcraft.registry.ModBlocks.LINK_MODIFIER = FabricModBlocks.LINK_MODIFIER;
        art.arcane.mystcraft.registry.ModBlocks.WRITING_DESK = FabricModBlocks.WRITING_DESK;
        art.arcane.mystcraft.registry.ModBlocks.CRYSTAL = FabricModBlocks.CRYSTAL;
        art.arcane.mystcraft.registry.ModBlocks.DECAY = FabricModBlocks.DECAY;
        art.arcane.mystcraft.registry.ModBlocks.LINK_PORTAL = FabricModBlocks.LINK_PORTAL;
        art.arcane.mystcraft.registry.ModBlocks.STAR_FISSURE = FabricModBlocks.STAR_FISSURE;
        art.arcane.mystcraft.registry.ModBlocks.FLUID_INK = () -> (LiquidBlock) FabricModBlocks.FLUID_INK.get();

        // Items
        art.arcane.mystcraft.registry.ModItems.PAGE = FabricModItems.PAGE;
        art.arcane.mystcraft.registry.ModItems.AGEBOOK = FabricModItems.AGEBOOK;
        art.arcane.mystcraft.registry.ModItems.LINKBOOK = FabricModItems.LINKBOOK;
        art.arcane.mystcraft.registry.ModItems.LINKBOOK_UNLINKED = FabricModItems.LINKBOOK_UNLINKED;
        art.arcane.mystcraft.registry.ModItems.BOOSTER_PACK = FabricModItems.BOOSTER_PACK;
        art.arcane.mystcraft.registry.ModItems.FOLDER = FabricModItems.FOLDER;
        art.arcane.mystcraft.registry.ModItems.PORTFOLIO = FabricModItems.PORTFOLIO;
        art.arcane.mystcraft.registry.ModItems.INK_VIAL = FabricModItems.INK_VIAL;
        art.arcane.mystcraft.registry.ModItems.GLASSES = FabricModItems.GLASSES;
        art.arcane.mystcraft.registry.ModItems.GUIDEBOOK = FabricModItems.GUIDEBOOK;
        art.arcane.mystcraft.registry.ModItems.INK_BUCKET = FabricModItems.INK_BUCKET;
        art.arcane.mystcraft.registry.ModItems.INK_MIXER_ITEM = FabricModItems.INK_MIXER_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOK_BINDER_ITEM = FabricModItems.BOOK_BINDER_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOK_RECEPTACLE_ITEM = FabricModItems.BOOK_RECEPTACLE_ITEM;
        art.arcane.mystcraft.registry.ModItems.BOOKSTAND_ITEM = FabricModItems.BOOKSTAND_ITEM;
        art.arcane.mystcraft.registry.ModItems.LECTERN_ITEM = FabricModItems.LECTERN_ITEM;
        art.arcane.mystcraft.registry.ModItems.LINK_MODIFIER_ITEM = FabricModItems.LINK_MODIFIER_ITEM;
        art.arcane.mystcraft.registry.ModItems.WRITING_DESK_ITEM = FabricModItems.WRITING_DESK_ITEM;
        art.arcane.mystcraft.registry.ModItems.CRYSTAL_ITEM = FabricModItems.CRYSTAL_ITEM;
        art.arcane.mystcraft.registry.ModItems.DECAY_ITEM = FabricModItems.DECAY_ITEM;

        // Block entities
        art.arcane.mystcraft.registry.ModBlockEntities.INK_MIXER = FabricModBlockEntities.INK_MIXER;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOK_BINDER = FabricModBlockEntities.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOK_RECEPTACLE = FabricModBlockEntities.BOOK_RECEPTACLE;
        art.arcane.mystcraft.registry.ModBlockEntities.BOOKSTAND = FabricModBlockEntities.BOOKSTAND;
        art.arcane.mystcraft.registry.ModBlockEntities.LECTERN = FabricModBlockEntities.LECTERN;
        art.arcane.mystcraft.registry.ModBlockEntities.WRITING_DESK = FabricModBlockEntities.WRITING_DESK;
        art.arcane.mystcraft.registry.ModBlockEntities.STAR_FISSURE = FabricModBlockEntities.STAR_FISSURE;
        art.arcane.mystcraft.registry.ModBlockEntities.LINK_MODIFIER = FabricModBlockEntities.LINK_MODIFIER;

        // Entities
        art.arcane.mystcraft.registry.ModEntities.LINKBOOK = FabricModEntities.LINKBOOK;
        art.arcane.mystcraft.registry.ModEntities.FALLING_BLOCK = FabricModEntities.FALLING_BLOCK;
        art.arcane.mystcraft.registry.ModEntities.METEOR = FabricModEntities.METEOR;
        art.arcane.mystcraft.registry.ModEntities.COLORED_LIGHTNING = FabricModEntities.COLORED_LIGHTNING;

        // Fluids
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_SOURCE = FabricModFluids.BLACK_INK_SOURCE;
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_FLOWING = FabricModFluids.BLACK_INK_FLOWING;
        art.arcane.mystcraft.registry.ModFluids.BLACK_INK_BUCKET = FabricModItems.INK_BUCKET;

        // Sounds
        art.arcane.mystcraft.registry.ModSounds.LINKING_POP = FabricModSounds.LINKING_POP;
        art.arcane.mystcraft.registry.ModSounds.LINKING_LINK = FabricModSounds.LINKING_LINK;
        art.arcane.mystcraft.registry.ModSounds.LINKING_DISARM = FabricModSounds.LINKING_DISARM;
        art.arcane.mystcraft.registry.ModSounds.LINKING_FOLLOWING = FabricModSounds.LINKING_FOLLOWING;
        art.arcane.mystcraft.registry.ModSounds.LINKING_INTRA = FabricModSounds.LINKING_INTRA;
        art.arcane.mystcraft.registry.ModSounds.LINKING_FISSURE = FabricModSounds.LINKING_FISSURE;
        art.arcane.mystcraft.registry.ModSounds.LINKING_PORTAL = FabricModSounds.LINKING_PORTAL;
        art.arcane.mystcraft.registry.ModSounds.METEOR_ROAR = FabricModSounds.METEOR_ROAR;
        art.arcane.mystcraft.registry.ModSounds.METEOR_IMPACT = FabricModSounds.METEOR_IMPACT;

        // Menu types
        art.arcane.mystcraft.registry.ModMenuTypes.INK_MIXER = FabricModMenuTypes.INK_MIXER;
        art.arcane.mystcraft.registry.ModMenuTypes.BOOK_BINDER = FabricModMenuTypes.BOOK_BINDER;
        art.arcane.mystcraft.registry.ModMenuTypes.LINK_MODIFIER = FabricModMenuTypes.LINK_MODIFIER;
        art.arcane.mystcraft.registry.ModMenuTypes.WRITING_DESK = FabricModMenuTypes.WRITING_DESK;
        art.arcane.mystcraft.registry.ModMenuTypes.FOLDER = FabricModMenuTypes.FOLDER;
        art.arcane.mystcraft.registry.ModMenuTypes.PORTFOLIO = FabricModMenuTypes.PORTFOLIO;

        // Config
        art.arcane.mystcraft.config.MystcraftConfig.giveGuidebookOnFirstSpawn = () -> FabricMystcraftConfig.giveGuidebookOnFirstSpawn.get();
        art.arcane.mystcraft.config.MystcraftConfig.maxSymbolsPerBook = () -> FabricMystcraftConfig.maxSymbolsPerBook.get();
        art.arcane.mystcraft.config.MystcraftConfig.deleteAgesOnStartup = () -> FabricMystcraftConfig.deleteAgesOnStartup.get();
        art.arcane.mystcraft.config.MystcraftConfig.instabilityEnabled = () -> FabricMystcraftConfig.instabilityEnabled.get();
        art.arcane.mystcraft.config.MystcraftConfig.deathEffectsEnabled = () -> FabricMystcraftConfig.deathEffectsEnabled.get();
        art.arcane.mystcraft.config.MystcraftConfig.allowUnstableAges = () -> FabricMystcraftConfig.allowUnstableAges.get();
        art.arcane.mystcraft.config.MystcraftConfig.instabilityMultiplier = () -> FabricMystcraftConfig.instabilityMultiplier.get();
        art.arcane.mystcraft.config.MystcraftConfig.maxAllowedInstability = () -> FabricMystcraftConfig.maxAllowedInstability.get();
        art.arcane.mystcraft.config.MystcraftConfig.thresholdDecay = () -> FabricMystcraftConfig.thresholdDecay.get();
        art.arcane.mystcraft.config.MystcraftConfig.thresholdTransmute = () -> FabricMystcraftConfig.thresholdTransmute.get();
        art.arcane.mystcraft.config.MystcraftConfig.thresholdLightning = () -> FabricMystcraftConfig.thresholdLightning.get();
        art.arcane.mystcraft.config.MystcraftConfig.thresholdMeteor = () -> FabricMystcraftConfig.thresholdMeteor.get();
        art.arcane.mystcraft.config.MystcraftConfig.thresholdPoison = () -> FabricMystcraftConfig.thresholdPoison.get();
        art.arcane.mystcraft.config.MystcraftConfig.thresholdWither = () -> FabricMystcraftConfig.thresholdWither.get();
        art.arcane.mystcraft.config.MystcraftConfig.chanceDecay = () -> FabricMystcraftConfig.chanceDecay.get();
        art.arcane.mystcraft.config.MystcraftConfig.chanceTransmute = () -> FabricMystcraftConfig.chanceTransmute.get();
        art.arcane.mystcraft.config.MystcraftConfig.chanceLightning = () -> FabricMystcraftConfig.chanceLightning.get();
        art.arcane.mystcraft.config.MystcraftConfig.chanceMeteor = () -> FabricMystcraftConfig.chanceMeteor.get();
        art.arcane.mystcraft.config.MystcraftConfig.chancePlayerEffect = () -> FabricMystcraftConfig.chancePlayerEffect.get();

        // Network
        art.arcane.mystcraft.network.MystcraftNetwork.sendToServerHandler = packet -> {
            FabricMystcraftNetwork.sendToServerGeneric(packet);
        };
        art.arcane.mystcraft.network.MystcraftNetwork.sendToPlayerHandler = (packet, player) -> {
            FabricMystcraftNetwork.sendToPlayerGeneric(packet, player);
        };
        art.arcane.mystcraft.network.MystcraftNetwork.sendToAllHandler = packet -> {
            MinecraftServer server = Mystcraft.getCurrentServer();
            if (server != null) {
                FabricMystcraftNetwork.sendToAll(packet, server);
            }
        };
        art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingHandler = (packet, player) -> {
            FabricMystcraftNetwork.sendToTracking(packet, player);
        };

        // Structure types
        art.arcane.mystcraft.world.structure.ModStructures.ABANDONED_LIBRARY = FabricModStructures.ABANDONED_LIBRARY;
        art.arcane.mystcraft.world.structure.ModStructures.UNDERGROUND_ARCHIVE = FabricModStructures.UNDERGROUND_ARCHIVE;
        art.arcane.mystcraft.world.structure.ModStructures.SCATTERED_LIBRARY = FabricModStructures.SCATTERED_LIBRARY;

        Mystcraft.LOGGER.info("[Mystcraft] Common registry stubs populated from Fabric registrations");
    }

    /** Handles dimension-level load events for Mystcraft Ages. */
    private void onLevelLoad(ServerLevel serverLevel) {
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

    /** Deletes all Mystcraft Ages and their dimension directories. */
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
}
