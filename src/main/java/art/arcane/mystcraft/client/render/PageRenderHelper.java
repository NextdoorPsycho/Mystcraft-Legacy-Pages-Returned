package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Helper class for rendering pages with D'ni symbols.
 */
@OnlyIn(Dist.CLIENT)
public class PageRenderHelper {

    private static final ResourceLocation PAGE_BACKGROUND =
            new ResourceLocation(Mystcraft.MOD_ID, "gui/bookui_pagel.png");

    // Texture coordinates for page background in book_page_left.png
    private static final int PAGE_TEX_U = 156;
    private static final int PAGE_TEX_V = 0;
    private static final int PAGE_TEX_WIDTH = 30;
    private static final int PAGE_TEX_HEIGHT = 40;

    // Symbol component constants
    private static final int ICON_SIZE = 64;
    private static final int SPRITESHEET_SIZE = 512;
    private static final float TEX_TRANSFORM = 1.0f / SPRITESHEET_SIZE;

    /**
     * Draws a complete page with its symbol or link panel.
     *
     * @param guiGraphics The graphics context
     * @param page The page ItemStack
     * @param x X position
     * @param y Y position
     * @param width Target width
     * @param height Target height
     * @param zLevel Z-level for rendering
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
     * Draws the page background texture.
     */
    private static void drawPageBackground(GuiGraphics guiGraphics, ItemStack page,
                                           float x, float y, float width, float height, float zLevel) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, PAGE_BACKGROUND);
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
     * Draws a symbol using its poem words arranged in a diamond pattern.
     *
     * @param guiGraphics The graphics context
     * @param symbol The symbol to draw (can be null for unknown/?)
     * @param scale The size scale
     * @param x X position
     * @param y Y position
     * @param zLevel Z-level
     */
    public static void drawSymbol(GuiGraphics guiGraphics, IAgeSymbol symbol, float scale,
                                   float x, float y, float zLevel) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (symbol == null) {
            // Draw unknown symbol (?)
            drawWord(guiGraphics, null, scale, x, y, zLevel);
            return;
        }

        // Calculate sizes for diamond arrangement
        scale /= 2;
        float s = scale / 2.414f; // sqrt(2) + 1
        float o = s * 1.414f; // sqrt(2)

        String[] words = symbol.getPoem();
        if (words == null || words.length == 0) {
            // No poem - draw centered unknown symbol
            drawWord(guiGraphics, DrawableWordManager.getDrawableWord(null), s * 2, x + o, y + o, zLevel);
            return;
        }

        // Draw words in diamond pattern:
        // Position 0: top
        // Position 1: right
        // Position 2: bottom
        // Position 3: left
        if (words.length > 0)
            drawWord(guiGraphics, DrawableWordManager.getDrawableWord(words[0]), 2 * s, x + o, y, zLevel);
        if (words.length > 1)
            drawWord(guiGraphics, DrawableWordManager.getDrawableWord(words[1]), 2 * s, x + o * 2, y + o, zLevel);
        if (words.length > 2)
            drawWord(guiGraphics, DrawableWordManager.getDrawableWord(words[2]), 2 * s, x + o, y + o * 2, zLevel);
        if (words.length > 3)
            drawWord(guiGraphics, DrawableWordManager.getDrawableWord(words[3]), 2 * s, x, y + o, zLevel);

        RenderSystem.disableBlend();
    }

    /**
     * Draws a single D'ni word.
     */
    public static void drawWord(GuiGraphics guiGraphics, DrawableWord word, float scale,
                                 float x, float y, float zLevel) {
        List<Integer> components;
        List<Integer> colors;
        ResourceLocation imageSource;

        if (word != null) {
            components = word.components();
            colors = word.colors();
            imageSource = word.imageSource();
        } else {
            // Unknown word - draw ? symbol (component 0)
            components = List.of(0);
            colors = List.of();
            imageSource = DrawableWord.WORD_COMPONENTS;
        }

        if (components.isEmpty()) {
            components = List.of(0);
            colors = List.of();
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, imageSource);

        for (int i = 0; i < components.size(); i++) {
            int color = 0;
            if (i < colors.size()) {
                color = colors.get(i);
            } else if (!colors.isEmpty()) {
                color = colors.get(0);
            }

            drawComponent(guiGraphics.pose(), components.get(i), color, scale, x, y, zLevel);
        }
    }

    /**
     * Draws a single component from the symbol sprite sheet.
     */
    private static void drawComponent(PoseStack poseStack, int iconIndex, int color,
                                       float scale, float x, float y, float zLevel) {
        int iconX = (iconIndex % 8) * ICON_SIZE;
        int iconY = (iconIndex / 8) * ICON_SIZE;

        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float u0 = iconX * TEX_TRANSFORM;
        float u1 = (iconX + ICON_SIZE) * TEX_TRANSFORM;
        float v0 = iconY * TEX_TRANSFORM;
        float v1 = (iconY + ICON_SIZE) * TEX_TRANSFORM;

        buffer.vertex(matrix, x, y + scale, zLevel).uv(u0, v1).color(red, green, blue, 1.0f).endVertex();
        buffer.vertex(matrix, x + scale, y + scale, zLevel).uv(u1, v1).color(red, green, blue, 1.0f).endVertex();
        buffer.vertex(matrix, x + scale, y, zLevel).uv(u1, v0).color(red, green, blue, 1.0f).endVertex();
        buffer.vertex(matrix, x, y, zLevel).uv(u0, v0).color(red, green, blue, 1.0f).endVertex();

        BufferUploader.drawWithShader(buffer.end());
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
