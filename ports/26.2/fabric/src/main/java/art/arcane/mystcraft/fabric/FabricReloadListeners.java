package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.gui.procedural.ProceduralUiReload;
import art.arcane.mystcraft.datapack.affinity.MystcraftAffinityReloadListener;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Stable Fabric IDs for shared server and client reload listeners. */
public final class FabricReloadListeners {

  private FabricReloadListeners() {
  }

  public static final class Grammar extends MystcraftGrammarReloadListener
      implements IdentifiableResourceReloadListener {
    @Override
    public Identifier getFabricId() {
      return id("datapack_grammar");
    }
  }

  public static final class Symbols extends MystcraftSymbolReloadListener
      implements IdentifiableResourceReloadListener {
    @Override
    public Identifier getFabricId() {
      return id("datapack_symbols");
    }
  }

  public static final class Affinities extends MystcraftAffinityReloadListener
      implements IdentifiableResourceReloadListener {
    @Override
    public Identifier getFabricId() {
      return id("datapack_ink_affinity");
    }
  }

  public static final class ProceduralUi implements SimpleSynchronousResourceReloadListener {
    @Override
    public Identifier getFabricId() {
      return ProceduralUiReload.ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
      ProceduralUiReload.reloadAll();
    }
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }
}
