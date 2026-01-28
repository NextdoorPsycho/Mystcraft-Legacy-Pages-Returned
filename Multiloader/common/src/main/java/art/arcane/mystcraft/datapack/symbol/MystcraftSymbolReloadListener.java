package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.grammar.GrammarDatapackLoader;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Reload listener for datapack-provided symbols.
 */
public class MystcraftSymbolReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public MystcraftSymbolReloadListener() {
        super(GSON, "mystcraft/symbols");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         net.minecraft.server.packs.resources.ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Mystcraft.LOGGER.info("[Datapack] Reloading Mystcraft symbols ({} definitions)", object.size());
        SymbolDatapackLoader.apply(object, GrammarDatapackLoader.getRules());
    }
}
