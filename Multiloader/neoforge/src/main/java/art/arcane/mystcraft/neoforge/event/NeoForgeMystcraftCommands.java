package art.arcane.mystcraft.neoforge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.command.MystcraftCommands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** NeoForge event wrapper that delegates to the common MystcraftCommands. */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class NeoForgeMystcraftCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MystcraftCommands.registerCommands(event.getDispatcher());
    }
}
