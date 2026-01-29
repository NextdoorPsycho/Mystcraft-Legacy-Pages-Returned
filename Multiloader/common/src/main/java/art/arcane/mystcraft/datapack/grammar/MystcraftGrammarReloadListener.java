package art.arcane.mystcraft.datapack.grammar;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Reload listener for datapack-provided grammar rules.
 */
public class MystcraftGrammarReloadListener extends SimpleJsonResourceReloadListener {

  private static final Gson GSON = new Gson();

  public MystcraftGrammarReloadListener() {
    super(GSON, "mystcraft/grammar");
  }

  @Override
  protected void apply(Map<ResourceLocation, JsonElement> object,
                       net.minecraft.server.packs.resources.ResourceManager resourceManager,
                       ProfilerFiller profiler) {
    GrammarDatapackLoader.setRules(object);
  }
}
