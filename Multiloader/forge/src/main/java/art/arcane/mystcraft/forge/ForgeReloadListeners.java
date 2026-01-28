package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers datapack reload listeners on Forge.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeReloadListeners {

    private ForgeReloadListeners() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MystcraftGrammarReloadListener());
        event.addListener(new MystcraftSymbolReloadListener());
    }
}
