package art.arcane.mystcraft.datapack.affinity;

import art.arcane.mystcraft.Mystcraft;
import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Reload listener for ink-affinity datapack JSON files.
 * <p>
 * Wired in by both Fabric ({@code FabricReloadListeners}) and Forge
 * ({@code ForgeEventHelper_1_20_1#onAddReloadListeners}).
 */
public class MystcraftAffinityReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {

  public MystcraftAffinityReloadListener() {
    super(ExtraCodecs.JSON, FileToIdConverter.json("mystcraft/ink_affinity"));
  }

  @Override
  protected void apply(@NotNull Map<Identifier, JsonElement> object,
                       @NotNull ResourceManager resourceManager,
                       @NotNull ProfilerFiller profiler) {
    Mystcraft.LOGGER.info("[Datapack] Reloading Mystcraft ink affinities ({} entries)", object.size());
    AffinityDatapackLoader.apply(object);
  }
}
