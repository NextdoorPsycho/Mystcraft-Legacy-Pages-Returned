package art.arcane.mystcraft.datapack.grammar;

import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Reload listener for datapack-provided grammar rules.
 */
public class MystcraftGrammarReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {

  public MystcraftGrammarReloadListener() {
    super(ExtraCodecs.JSON, FileToIdConverter.json("mystcraft/grammar"));
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> object,
                       net.minecraft.server.packs.resources.ResourceManager resourceManager,
                       ProfilerFiller profiler) {
    art.arcane.mystcraft.Mystcraft.LOGGER.info("[Datapack] Grammar reload listener starting with {} entries", object.size());
    GrammarDatapackLoader.setRules(object);
    art.arcane.mystcraft.Mystcraft.LOGGER.info("[Datapack] Grammar reload listener complete");
  }
}
