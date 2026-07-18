package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.client.gui.procedural.PageTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolMotif;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPalette;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/**
 * Helper for drawing page parchment + symbol illustrations onto a GUI.
 * <p>
 * <b>Procedural-symbol-pages refactor (post phase 1)</b>: the symbol
 * illustration is now rendered through
 * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolMotif#draw}
 * which composes glyph tiles from
 * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory}
 * onto a cached {@code DynamicTexture}. The previous sprite-atlas path (which
 * iterated over {@code DrawableWord#components()} and bound
 * {@code symbolcomponents.png}) has been replaced.
 */
public class PageRenderHelper {

  private static final int PAGE_TEX_U = PageTextureFactory.SUB_U;
  private static final int PAGE_TEX_V = PageTextureFactory.SUB_V;
  private static final int PAGE_TEX_WIDTH = PageTextureFactory.SUB_W;
  private static final int PAGE_TEX_HEIGHT = PageTextureFactory.SUB_H;

  /**
   * Draws a complete page with its symbol or link panel.
   *
   * @param guiGraphics The graphics context
   * @param page        The page ItemStack
   * @param x           X position
   * @param y           Y position
   * @param width       Target width
   * @param height      Target height
   * @param zLevel      Z-level for rendering
   */
  public static void drawPage(GuiGraphicsExtractor guiGraphics, ItemStack page, float x, float y,
                              float width, float height, float zLevel) {

    drawPageBackground(guiGraphics, page, x, y, width, height, zLevel);

    Identifier symbolId = Page.getSymbol(page);
    if (symbolId != null) {
      IAgeSymbol symbol = SymbolRegistry.get(symbolId);
      drawSymbol(guiGraphics, symbol, width - 1, x + 0.5f, y + (height + 1 - width) / 2, zLevel);
    } else if (Page.isLinkPanel(page)) {

      drawFilledRect(guiGraphics,
          x + width * 0.15f, y + height * 0.15f,
          x + width * 0.85f, y + height * 0.5f,
          0xFF000000, zLevel);
    }
  }

  private static void drawPageBackground(GuiGraphicsExtractor guiGraphics, ItemStack page,
                                         float x, float y, float width, float height, float zLevel) {
    Identifier pageTex = PageTextureFactory.getPageBackground(page);
    float alpha = page.isEmpty() ? 0.2f : 1.0f;
    guiGraphics.blit(
        RenderPipelines.GUI_TEXTURED,
        pageTex,
        Math.round(x),
        Math.round(y),
        PAGE_TEX_U,
        PAGE_TEX_V,
        Math.round(width),
        Math.round(height),
        PAGE_TEX_WIDTH,
        PAGE_TEX_HEIGHT,
        256,
        256,
        ARGB.white(alpha));
  }

  /**
   * Draws a symbol's procedural motif at the given screen position.
   * <p>
   * The motif is selected via the symbol's
   * {@link art.arcane.mystcraft.api.symbol.SymbolCategory category} →
   * {@link SymbolPalette.Entry#defaultMotif() palette default} mapping. For
   * phase 1 every category resolves to {@link SymbolMotif#DIAMOND DIAMOND};
   * phase 2 introduces category-specific motifs.
   *
   * @param guiGraphics The graphics context.
   * @param symbol      The symbol to draw. {@code null} renders the fallback
   *                    (transparent — suppresses the diamond).
   * @param scale       Render size in GUI pixels (square).
   * @param x           X position (top-left).
   * @param y           Y position (top-left).
   * @param zLevel      Z-level. Currently unused — passed through for API
   *                    compatibility with the legacy sprite-atlas pipeline; the
   *                    procedural path uses the GUI z set by the caller's pose
   *                    stack.
   */
  public static void drawSymbol(GuiGraphicsExtractor guiGraphics, IAgeSymbol symbol, float scale,
                                float x, float y, @SuppressWarnings("unused") float zLevel) {
    if (symbol == null || scale <= 0f) {
      return;
    }
    SymbolPalette.Entry palette = SymbolPalette.get(symbol.getCategory());
    SymbolMotif motif = palette.defaultMotif();
    motif.draw(guiGraphics, symbol, palette, x, y, scale);
  }

  private static void drawFilledRect(GuiGraphicsExtractor guiGraphics, float x1, float y1, float x2, float y2,
                                     int color, float zLevel) {
    guiGraphics.fill((int) x1, (int) y1, (int) x2, (int) y2, color);
  }
}
