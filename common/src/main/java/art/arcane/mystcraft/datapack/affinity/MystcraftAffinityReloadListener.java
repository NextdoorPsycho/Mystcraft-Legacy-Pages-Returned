package art.arcane.mystcraft.datapack.affinity;

import art.arcane.mystcraft.Mystcraft;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Reload listener for ink-affinity datapack JSON files.
 * <p>
 * Wired in by both Fabric ({@code FabricReloadListeners}) and Forge
 * ({@code ForgeEventHelper_1_20_1#onAddReloadListeners}).
 */
public class MystcraftAffinityReloadListener extends SimpleJsonResourceReloadListener {

  private static final Gson GSON = new Gson();

  public MystcraftAffinityReloadListener() {
    super(GSON, "mystcraft/ink_affinity");
  }

  @Override
  protected void apply(@NotNull Map<ResourceLocation, JsonElement> object,
                       @NotNull ResourceManager resourceManager,
                       @NotNull ProfilerFiller profiler) {
    Mystcraft.LOGGER.info("[Datapack] Reloading Mystcraft ink affinities ({} entries)", object.size());
    AffinityDatapackLoader.apply(object);
  }
}
