package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUiReload;
import art.arcane.mystcraft.datapack.affinity.MystcraftAffinityReloadListener;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Consolidated Fabric reload listeners for Mystcraft 1.20.1.
 */
public final class FabricReloadListeners {

  private FabricReloadListeners() {
  }

  // ========== GRAMMAR RELOAD LISTENER ==========
  public static class GrammarReloadListener extends MystcraftGrammarReloadListener implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "datapack_grammar");

    @Override
    public ResourceLocation getFabricId() {
      return ID;
    }
  }

  // ========== SYMBOL RELOAD LISTENER ==========
  public static class SymbolReloadListener extends MystcraftSymbolReloadListener implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "datapack_symbols");

    @Override
    public ResourceLocation getFabricId() {
      return ID;
    }
  }

  // ========== INK AFFINITY RELOAD LISTENER ==========
  public static class AffinityReloadListener extends MystcraftAffinityReloadListener implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "datapack_ink_affinity");

    @Override
    public ResourceLocation getFabricId() {
      return ID;
    }
  }

  // ========== PROCEDURAL UI RELOAD LISTENER (CLIENT) ==========
  public static class ProceduralUiReloadListener implements IdentifiableResourceReloadListener,
      net.minecraft.server.packs.resources.ResourceManagerReloadListener {

    @Override
    public ResourceLocation getFabricId() {
      return ProceduralUiReload.ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
      ProceduralUiReload.reloadAll();
    }
  }
}
