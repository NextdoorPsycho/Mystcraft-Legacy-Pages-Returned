package art.arcane.mystcraft.client.gui.procedural;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.GuidebookTexture;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolMotif;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPageTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPalette;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Coordinates reload of every procedural UI subsystem when the resource pack
 * stack changes (player switches packs, F3+T, datapack reload, etc.).
 * <p>
 * Registers itself as a {@link ResourceManagerReloadListener} on
 * {@link net.minecraft.server.packs.PackType#CLIENT_RESOURCES} via the
 * platform-specific entry points (Fabric: {@code ResourceManagerHelper.get(...)
 * .registerReloadListener}, Forge:
 * {@code RegisterClientReloadListenersEvent#registerReloadListener}).
 * <p>
 * Subsystems register themselves through {@link #registerCache(ProceduralTextureCache)}
 * so a single hook flushes them all in a deterministic order.
 */
public final class ProceduralUiReload implements ResourceManagerReloadListener {

  public static final ResourceLocation ID =
      new ResourceLocation(Mystcraft.MOD_ID, "procedural_ui_reload");

  private static final ProceduralUiReload INSTANCE = new ProceduralUiReload();
  private static final Set<ProceduralTextureCache> CACHES = new LinkedHashSet<>();

  private ProceduralUiReload() {
  }

  public static ProceduralUiReload instance() {
    return INSTANCE;
  }

  /**
   * Registers a cache so it will be cleared on resource pack reloads.
   * Idempotent; the same instance can be passed repeatedly.
   */
  public static synchronized void registerCache(@NotNull ProceduralTextureCache cache) {
    CACHES.add(cache);
  }

  /**
   * Manual reload entry point; used by tests and for hot-reloads outside of the
   * normal resource pack cycle.
   */
  public static synchronized void reloadAll() {
    Mystcraft.LOGGER.debug("[ProceduralUiReload] Reloading {} cache(s) + theme + guidebook", CACHES.size());
    GuiTheme.reload();
    try {
      SymbolPalette.reload();
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[ProceduralUiReload] SymbolPalette.reload failed: {}", e.toString());
    }
    for (ProceduralTextureCache cache : CACHES) {
      try {
        cache.clear();
      } catch (Exception e) {
        Mystcraft.LOGGER.warn("[ProceduralUiReload] Cache clear failed: {}", e.toString());
      }
    }
    try {
      GuidebookTexture.reset();
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[ProceduralUiReload] GuidebookTexture.reset failed: {}", e.toString());
    }
    try {
      BookTextureFactory.reset();
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[ProceduralUiReload] BookTextureFactory.reset failed: {}", e.toString());
    }
    try {
      PageTextureFactory.reset();
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[ProceduralUiReload] PageTextureFactory.reset failed: {}", e.toString());
    }
    // Procedural symbol-page caches (added in plan §5 task 5.4). These
    // hold composed page textures, motif sub-textures, and individual
    // glyph tiles — all of which must invalidate when the resource pack
    // changes the symbol palette overrides or fonts.
    try {
      SymbolPageTextureFactory.reset();
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[ProceduralUiReload] SymbolPageTextureFactory.reset failed: {}", e.toString());
    }
    try {
      SymbolMotif.invalidateCache();
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[ProceduralUiReload] SymbolMotif.invalidateCache failed: {}", e.toString());
    }
    try {
      SymbolGlyphFactory.reset();
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[ProceduralUiReload] SymbolGlyphFactory.reset failed: {}", e.toString());
    }
  }

  @Override
  public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
    reloadAll();
  }
}
