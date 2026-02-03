package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

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
}
