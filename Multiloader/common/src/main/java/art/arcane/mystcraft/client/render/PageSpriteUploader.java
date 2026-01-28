package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Handles generation of dynamic page images for symbol rendering.
 * Each symbol gets a unique image showing its D'ni poem words.
 */
public class PageSpriteUploader {

    private static final ResourceLocation PAGE_BACKGROUND =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/items/page_background.png");

    // Texture size for generated sprites
    private static final int TEXTURE_SIZE = 32;
    private static final int WORK_SIZE = 160;
    private static final int ICON_SIZE = 64;
    private static final int SPRITESHEET_COLS = 8;

    // Cache for sprite locations
    private static final Map<ResourceLocation, ResourceLocation> spriteLocations = new HashMap<>();

    private static BufferedImage pageBackgroundImage = null;
    private static BufferedImage symbolComponentsImage = null;

    /**
     * Gets the sprite location for a symbol page.
     * Returns the base page texture if the symbol sprite isn't available.
     */
    public static ResourceLocation getSpriteLocation(ResourceLocation symbolId) {
        if (symbolId == null) {
            return new ResourceLocation(Mystcraft.MOD_ID, "items/page_background");
        }
        return spriteLocations.getOrDefault(symbolId,
                new ResourceLocation(Mystcraft.MOD_ID, "items/page_background"));
    }

    /**
     * Gets the sprite location for a link panel page.
     */
    public static ResourceLocation getLinkPanelSpriteLocation() {
        return spriteLocations.getOrDefault(
                new ResourceLocation(Mystcraft.MOD_ID, "page_linkpanel"),
                new ResourceLocation(Mystcraft.MOD_ID, "items/page_background"));
    }

    /**
     * Clears cached images to free memory. Call on resource reload.
     */
    public static void clearCache() {
        pageBackgroundImage = null;
        symbolComponentsImage = null;
    }

    /**
     * Generates a page image for a symbol.
     */
    public static BufferedImage generateSymbolPageImage(ResourceLocation symbolId) {
        BufferedImage background = getPageBackground();
        BufferedImage components = getSymbolComponents();

        if (background == null) {
            return createBlankImage();
        }

        // Create working image
        BufferedImage work = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = work.createGraphics();
        g.drawImage(background, 0, 0, WORK_SIZE, WORK_SIZE, null);
        g.dispose();

        if (components != null && symbolId != null) {
            IAgeSymbol symbol = SymbolRegistry.get(symbolId);
            if (symbol != null) {
                String[] words = symbol.getPoem();
                if (words != null && words.length > 0) {
                    drawSymbolOnImage(work, components, words);
                } else {
                    drawUnknownSymbol(work, components);
                }
            } else {
                drawUnknownSymbol(work, components);
            }
        }

        return scaleImage(work, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * Generates a link panel page image.
     */
    public static BufferedImage generateLinkPanelImage() {
        BufferedImage background = getPageBackground();

        if (background == null) {
            return createBlankImage();
        }

        BufferedImage work = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = work.createGraphics();
        g.drawImage(background, 0, 0, WORK_SIZE, WORK_SIZE, null);

        // Draw black rectangle for link panel
        int panelX = (int) (WORK_SIZE * 0.15);
        int panelY = (int) (WORK_SIZE * 0.15);
        int panelWidth = (int) (WORK_SIZE * 0.7);
        int panelHeight = (int) (WORK_SIZE * 0.35);
        g.setColor(Color.BLACK);
        g.fillRect(panelX, panelY, panelWidth, panelHeight);

        g.dispose();

        return scaleImage(work, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * Draws symbol words in diamond pattern.
     */
    private static void drawSymbolOnImage(BufferedImage target, BufferedImage components, String[] words) {
        float scale = WORK_SIZE / 2.414f;
        float offset = scale * 1.414f / 2;
        int symbolSize = (int) (scale * 0.8f);

        int centerX = WORK_SIZE / 2;
        int centerY = WORK_SIZE / 2;

        if (words.length > 0) {
            drawWord(target, components, words[0], centerX - symbolSize / 2, (int) (centerY - offset - symbolSize / 2), symbolSize);
        }
        if (words.length > 1) {
            drawWord(target, components, words[1], (int) (centerX + offset - symbolSize / 2), centerY - symbolSize / 2, symbolSize);
        }
        if (words.length > 2) {
            drawWord(target, components, words[2], centerX - symbolSize / 2, (int) (centerY + offset - symbolSize / 2), symbolSize);
        }
        if (words.length > 3) {
            drawWord(target, components, words[3], (int) (centerX - offset - symbolSize / 2), centerY - symbolSize / 2, symbolSize);
        }
    }

    private static void drawWord(BufferedImage target, BufferedImage components, String wordName, int x, int y, int size) {
        DrawableWord word = DrawableWordManager.getDrawableWord(wordName);
        if (word == null) {
            word = DrawableWordManager.getDrawableWord(null);
        }

        List<Integer> wordComponents = word != null ? word.components() : List.of(0);
        List<Integer> colors = word != null ? word.colors() : List.of();

        if (wordComponents.isEmpty()) {
            wordComponents = List.of(0);
        }

        for (int i = 0; i < wordComponents.size(); i++) {
            int componentIndex = wordComponents.get(i);
            int color = 0;
            if (i < colors.size()) {
                color = colors.get(i);
            } else if (!colors.isEmpty()) {
                color = colors.get(0);
            }

            drawComponent(target, components, componentIndex, color, x, y, size);
        }
    }

    private static void drawComponent(BufferedImage target, BufferedImage components,
                                       int componentIndex, int color, int x, int y, int size) {
        int srcX = (componentIndex % SPRITESHEET_COLS) * ICON_SIZE;
        int srcY = (componentIndex / SPRITESHEET_COLS) * ICON_SIZE;

        float colorR = ((color >> 16) & 0xFF) / 255.0f;
        float colorG = ((color >> 8) & 0xFF) / 255.0f;
        float colorB = (color & 0xFF) / 255.0f;

        for (int dy = 0; dy < size; dy++) {
            for (int dx = 0; dx < size; dx++) {
                int targetX = x + dx;
                int targetY = y + dy;

                if (targetX < 0 || targetX >= target.getWidth() ||
                    targetY < 0 || targetY >= target.getHeight()) {
                    continue;
                }

                int srcSampleX = srcX + (dx * ICON_SIZE / size);
                int srcSampleY = srcY + (dy * ICON_SIZE / size);

                if (srcSampleX >= components.getWidth() || srcSampleY >= components.getHeight()) {
                    continue;
                }

                int srcPixel = components.getRGB(srcSampleX, srcSampleY);
                int srcAlpha = (srcPixel >> 24) & 0xFF;

                if (srcAlpha > 0) {
                    int targetPixel = target.getRGB(targetX, targetY);

                    float srcAlphaF = srcAlpha / 255.0f;
                    int targetR = (targetPixel >> 16) & 0xFF;
                    int targetG = (targetPixel >> 8) & 0xFF;
                    int targetB = targetPixel & 0xFF;

                    int newR = (int) (colorR * 255 * srcAlphaF + targetR * (1 - srcAlphaF));
                    int newG = (int) (colorG * 255 * srcAlphaF + targetG * (1 - srcAlphaF));
                    int newB = (int) (colorB * 255 * srcAlphaF + targetB * (1 - srcAlphaF));

                    newR = Math.min(255, Math.max(0, newR));
                    newG = Math.min(255, Math.max(0, newG));
                    newB = Math.min(255, Math.max(0, newB));

                    target.setRGB(targetX, targetY, 0xFF000000 | (newR << 16) | (newG << 8) | newB);
                }
            }
        }
    }

    private static void drawUnknownSymbol(BufferedImage target, BufferedImage components) {
        int size = WORK_SIZE / 2;
        int x = (WORK_SIZE - size) / 2;
        int y = (WORK_SIZE - size) / 2;
        drawComponent(target, components, 0, 0, x, y, size);
    }

    private static BufferedImage getPageBackground() {
        if (pageBackgroundImage != null) {
            return pageBackgroundImage;
        }

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(PAGE_BACKGROUND);
            if (resource.isPresent()) {
                try (InputStream is = resource.get().open()) {
                    pageBackgroundImage = ImageIO.read(is);
                    return pageBackgroundImage;
                }
            }
        } catch (IOException e) {
            Mystcraft.LOGGER.error("Failed to load page background texture", e);
        }
        return null;
    }

    private static BufferedImage getSymbolComponents() {
        if (symbolComponentsImage != null) {
            return symbolComponentsImage;
        }

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(DrawableWord.WORD_COMPONENTS);
            if (resource.isPresent()) {
                try (InputStream is = resource.get().open()) {
                    symbolComponentsImage = ImageIO.read(is);
                    return symbolComponentsImage;
                }
            }
        } catch (IOException e) {
            Mystcraft.LOGGER.error("Failed to load symbol components texture", e);
        }
        return null;
    }

    private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage createBlankImage() {
        BufferedImage blank = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = blank.createGraphics();
        g.setColor(new Color(0xF8F0E0));
        g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        g.dispose();
        return blank;
    }

    /**
     * Converts a BufferedImage to NativeImage for use with Minecraft's texture system.
     */
    public static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                // NativeImage uses ABGR format
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }
        return nativeImage;
    }
}
