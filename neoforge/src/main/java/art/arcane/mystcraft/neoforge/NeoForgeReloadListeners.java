package art.arcane.mystcraft.neoforge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Registers datapack reload listeners on NeoForge.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NeoForgeReloadListeners {

    private NeoForgeReloadListeners() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MystcraftGrammarReloadListener());
        event.addListener(new MystcraftSymbolReloadListener());
    }
}
