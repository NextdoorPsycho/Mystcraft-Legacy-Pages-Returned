package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.client.gui.procedural.PageTextureFactory;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolMotif;
import art.arcane.mystcraft.client.gui.procedural.symbol.SymbolPalette;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Helper for drawing page parchment + symbol illustrations onto a GUI.
 * <p>
 * <b>Procedural-symbol-pages refactor (post phase 1)</b>: the symbol
 * illustration is now rendered through
 * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolMotif#draw}
 * which composes glyph tiles from
 * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory}
 * onto a cached {@code DynamicTexture}. The previous sprite-atlas path
 * (which iterated over {@code DrawableWord#components()} and bound
 * {@code symbolcomponents.png}) has been replaced.
 */
public class PageRenderHelper {

  // Texture coordinates for page background sub-region (matches the legacy
  // {@code bookui_pagel.png} {156, 0, 30, 40} layout so existing draw maths
  // does not change). The actual image now comes from PageTextureFactory.
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
  public static void drawPage(GuiGraphics guiGraphics, ItemStack page, float x, float y,
                              float width, float height, float zLevel) {
    // Draw the page background
    drawPageBackground(guiGraphics, page, x, y, width, height, zLevel);

    // Draw the symbol or link panel
    ResourceLocation symbolId = Page.getSymbol(page);
    if (symbolId != null) {
      IAgeSymbol symbol = SymbolRegistry.get(symbolId);
      drawSymbol(guiGraphics, symbol, width - 1, x + 0.5f, y + (height + 1 - width) / 2, zLevel);
    } else if (Page.isLinkPanel(page)) {
      // Draw black rectangle for link panel
      drawFilledRect(guiGraphics,
          x + width * 0.15f, y + height * 0.15f,
          x + width * 0.85f, y + height * 0.5f,
          0xFF000000, zLevel);
    }
  }

  /**
   * Draws the page background texture. The texture is generated procedurally
   * by {@link PageTextureFactory} with style varying by page kind (blank,
   * symbol, link panel) and ink tint.
   */
  private static void drawPageBackground(GuiGraphics guiGraphics, ItemStack page,
                                         float x, float y, float width, float height, float zLevel) {
    ResourceLocation pageTex = PageTextureFactory.getPageBackground(page);
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, pageTex);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();

    float alpha = page.isEmpty() ? 0.2f : 1.0f;
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

    // Draw textured rectangle
    drawTexturedRect(guiGraphics.pose(), x, y, width, height,
        PAGE_TEX_U, PAGE_TEX_V, PAGE_TEX_WIDTH, PAGE_TEX_HEIGHT, zLevel);

    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    RenderSystem.disableBlend();
  }

  /**
   * Draws a symbol's procedural motif at the given screen position.
   * <p>
   * The motif is selected via the symbol's
   * {@link art.arcane.mystcraft.api.symbol.SymbolCategory category} →
   * {@link SymbolPalette.Entry#defaultMotif() palette default} mapping.
   * For phase 1 every category resolves to {@link SymbolMotif#DIAMOND DIAMOND};
   * phase 2 introduces category-specific motifs.
   *
   * @param guiGraphics The graphics context.
   * @param symbol      The symbol to draw. {@code null} renders the
   *                    fallback (transparent — suppresses the diamond).
   * @param scale       Render size in GUI pixels (square).
   * @param x           X position (top-left).
   * @param y           Y position (top-left).
   * @param zLevel      Z-level. Currently unused — passed through for
   *                    API compatibility with the legacy sprite-atlas
   *                    pipeline; the procedural path uses the GUI z
   *                    set by the caller's pose stack.
   */
  public static void drawSymbol(GuiGraphics guiGraphics, IAgeSymbol symbol, float scale,
                                float x, float y, @SuppressWarnings("unused") float zLevel) {
    if (symbol == null || scale <= 0f) {
      return;
    }
    SymbolPalette.Entry palette = SymbolPalette.get(symbol.getCategory());
    SymbolMotif motif = palette.defaultMotif();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    motif.draw(guiGraphics, symbol, palette, x, y, scale);
    RenderSystem.disableBlend();
  }

  /**
   * Draws a textured rectangle.
   */
  private static void drawTexturedRect(PoseStack poseStack, float x, float y, float width, float height,
                                       int u, int v, int texWidth, int texHeight, float zLevel) {
    float texScale = 1.0f / 256.0f; // Standard Minecraft GUI texture size

    Matrix4f matrix = poseStack.last().pose();
    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

    float u0 = u * texScale;
    float u1 = (u + texWidth) * texScale;
    float v0 = v * texScale;
    float v1 = (v + texHeight) * texScale;

    buffer.vertex(matrix, x, y + height, zLevel).uv(u0, v1).endVertex();
    buffer.vertex(matrix, x + width, y + height, zLevel).uv(u1, v1).endVertex();
    buffer.vertex(matrix, x + width, y, zLevel).uv(u1, v0).endVertex();
    buffer.vertex(matrix, x, y, zLevel).uv(u0, v0).endVertex();

    BufferUploader.drawWithShader(buffer.end());
  }

  /**
   * Draws a filled rectangle.
   */
  private static void drawFilledRect(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2,
                                     int color, float zLevel) {
    guiGraphics.fill((int) x1, (int) y1, (int) x2, (int) y2, color);
  }
}
