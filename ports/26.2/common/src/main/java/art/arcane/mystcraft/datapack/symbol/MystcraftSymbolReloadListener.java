package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.grammar.GrammarDatapackLoader;
import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Reload listener for datapack-provided symbols.
 */
public class MystcraftSymbolReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {

  public MystcraftSymbolReloadListener() {
    super(ExtraCodecs.JSON, FileToIdConverter.json("mystcraft/symbols"));
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> object,
                       net.minecraft.server.packs.resources.ResourceManager resourceManager,
                       ProfilerFiller profiler) {
    Mystcraft.LOGGER.info("[Datapack] Reloading Mystcraft symbols ({} definitions)", object.size());
    SymbolDatapackLoader.apply(object, GrammarDatapackLoader.getRules());
  }
}
