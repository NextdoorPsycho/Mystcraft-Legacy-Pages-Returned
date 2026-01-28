package art.arcane.mystcraft.forge.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.command.MystcraftCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge event wrapper that delegates to the common MystcraftCommands.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class ForgeMystcraftCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MystcraftCommands.registerCommands(event.getDispatcher());
    }
}
