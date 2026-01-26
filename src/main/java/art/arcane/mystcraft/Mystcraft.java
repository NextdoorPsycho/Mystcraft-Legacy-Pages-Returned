package art.arcane.mystcraft;

import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModCreativeTabs;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModFluids;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.registry.MystcraftRegistries;
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

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Mystcraft registration complete");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Mystcraft common setup");

        // TODO: Common setup tasks
        // - Initialize networking
        // - Register capabilities
        // - Setup grammar system
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Mystcraft server starting");

        // TODO: Register commands
        // TODO: Load dimension data
    }

    // Client-side setup is handled separately via Mod.EventBusSubscriber
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Mystcraft client setup");

            // TODO: Register client-side renderers
            // TODO: Register key bindings
            // TODO: Setup GUI screens
        }
    }
}
