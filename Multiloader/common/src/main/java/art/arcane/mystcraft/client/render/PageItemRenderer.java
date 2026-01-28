package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates dynamic textures for page items based on their symbols.
 * Each symbol gets a unique texture with its D'ni words rendered on the page background.
 */
public class PageItemRenderer {

    private static final ResourceLocation PAGE_BACKGROUND =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/items/page_background.png");

    private static final Map<ResourceLocation, ResourceLocation> symbolTextureCache = new HashMap<>();
    private static final Map<ResourceLocation, DynamicTexture> dynamicTextures = new HashMap<>();

    private static BufferedImage pageBackgroundImage = null;
    private static BufferedImage symbolComponentsImage = null;

    // Output texture size (standard item texture)
    private static final int OUTPUT_SIZE = 32;
    // Working size for symbol rendering (scaled up for quality)
    private static final int WORK_SIZE = 160;
    // Symbol component size in sprite sheet
    private static final int ICON_SIZE = 64;
    // Sprite sheet is 8x8 grid
    private static final int SPRITESHEET_COLS = 8;

    /**
     * Gets or creates a dynamic texture for the given page item.
     * Returns the texture's ResourceLocation for use in item rendering.
     */
    public static ResourceLocation getPageTexture(ItemStack pageStack) {
        if (pageStack.isEmpty()) {
            return PAGE_BACKGROUND;
        }

        // Check for link panel
        if (Page.isLinkPanel(pageStack)) {
            return getOrCreateLinkPanelTexture();
        }

        // Check for symbol
        ResourceLocation symbolId = Page.getSymbol(pageStack);
        if (symbolId == null) {
            return PAGE_BACKGROUND;
        }

        return getOrCreateSymbolTexture(symbolId);
    }

    /**
     * Gets or creates a texture for a link panel page.
     */
    private static ResourceLocation getOrCreateLinkPanelTexture() {
        ResourceLocation cacheKey = new ResourceLocation(Mystcraft.MOD_ID, "page_linkpanel");

        if (symbolTextureCache.containsKey(cacheKey)) {
            return symbolTextureCache.get(cacheKey);
        }

        try {
            BufferedImage background = getPageBackground();
            if (background == null) return PAGE_BACKGROUND;

            // Create working copy
            BufferedImage work = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = work.createGraphics();

            // Draw scaled background
            g.drawImage(background, 0, 0, WORK_SIZE, WORK_SIZE, null);

            // Draw black rectangle for link panel
            int panelX = (int) (WORK_SIZE * 0.15);
            int panelY = (int) (WORK_SIZE * 0.15);
            int panelWidth = (int) (WORK_SIZE * 0.7);
            int panelHeight = (int) (WORK_SIZE * 0.35);
            g.setColor(Color.BLACK);
            g.fillRect(panelX, panelY, panelWidth, panelHeight);

            g.dispose();

            // Scale down to output size
            BufferedImage output = scaleImage(work, OUTPUT_SIZE, OUTPUT_SIZE);

            // Register dynamic texture
            ResourceLocation textureLoc = registerDynamicTexture(cacheKey, output);
            symbolTextureCache.put(cacheKey, textureLoc);
            return textureLoc;

        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to create link panel texture", e);
            return PAGE_BACKGROUND;
        }
    }

    /**
     * Gets or creates a texture for a symbol page.
     */
    private static ResourceLocation getOrCreateSymbolTexture(ResourceLocation symbolId) {
        if (symbolTextureCache.containsKey(symbolId)) {
            return symbolTextureCache.get(symbolId);
        }

        try {
            BufferedImage background = getPageBackground();
            BufferedImage components = getSymbolComponents();
            if (background == null || components == null) return PAGE_BACKGROUND;

            IAgeSymbol symbol = SymbolRegistry.get(symbolId);

            // Create working image
            BufferedImage work = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = work.createGraphics();

            // Draw scaled background
            g.drawImage(background, 0, 0, WORK_SIZE, WORK_SIZE, null);
            g.dispose();

            // Draw symbol words
            if (symbol != null) {
                String[] words = symbol.getPoem();
                if (words != null && words.length > 0) {
                    drawSymbolOnImage(work, components, words);
                } else {
                    // Unknown symbol - draw ?
                    drawUnknownSymbol(work, components);
                }
            } else {
                drawUnknownSymbol(work, components);
            }

            // Scale down to output size
            BufferedImage output = scaleImage(work, OUTPUT_SIZE, OUTPUT_SIZE);

            // Register dynamic texture
            ResourceLocation textureLoc = registerDynamicTexture(symbolId, output);
            symbolTextureCache.put(symbolId, textureLoc);
            return textureLoc;

        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to create symbol texture for " + symbolId, e);
            return PAGE_BACKGROUND;
        }
    }

    /**
     * Draws symbol words in diamond pattern on the image.
     */
    private static void drawSymbolOnImage(BufferedImage target, BufferedImage components, String[] words) {
        // Diamond pattern positions (relative to WORK_SIZE)
        // Scale factor for symbol size
        float scale = WORK_SIZE / 2.414f; // sqrt(2) + 1
        float offset = scale * 1.414f / 2; // sqrt(2) / 2
        int symbolSize = (int) (scale * 0.8f);

        // Center offset
        int centerX = WORK_SIZE / 2;
        int centerY = WORK_SIZE / 2;

        // Position 0: top
        if (words.length > 0) {
            drawWord(target, components, words[0], centerX - symbolSize / 2, (int) (centerY - offset - symbolSize / 2), symbolSize);
        }
        // Position 1: right
        if (words.length > 1) {
            drawWord(target, components, words[1], (int) (centerX + offset - symbolSize / 2), centerY - symbolSize / 2, symbolSize);
        }
        // Position 2: bottom
        if (words.length > 2) {
            drawWord(target, components, words[2], centerX - symbolSize / 2, (int) (centerY + offset - symbolSize / 2), symbolSize);
        }
        // Position 3: left
        if (words.length > 3) {
            drawWord(target, components, words[3], (int) (centerX - offset - symbolSize / 2), centerY - symbolSize / 2, symbolSize);
        }
    }

    /**
     * Draws a single word at the specified position.
     */
    private static void drawWord(BufferedImage target, BufferedImage components, String wordName, int x, int y, int size) {
        DrawableWord word = DrawableWordManager.getDrawableWord(wordName);
        if (word == null) {
            word = DrawableWordManager.getDrawableWord(null);
        }

        List<Integer> wordComponents = word.components();
        List<Integer> colors = word.colors();

        if (wordComponents.isEmpty()) {
            wordComponents = List.of(0); // ? symbol
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

    /**
     * Draws a single component from the sprite sheet.
     */
    private static void drawComponent(BufferedImage target, BufferedImage components,
                                       int componentIndex, int color, int x, int y, int size) {
        int srcX = (componentIndex % SPRITESHEET_COLS) * ICON_SIZE;
        int srcY = (componentIndex / SPRITESHEET_COLS) * ICON_SIZE;

        float colorR = ((color >> 16) & 0xFF) / 255.0f;
        float colorG = ((color >> 8) & 0xFF) / 255.0f;
        float colorB = (color & 0xFF) / 255.0f;

        // Scale and blend component onto target
        for (int dy = 0; dy < size; dy++) {
            for (int dx = 0; dx < size; dx++) {
                int targetX = x + dx;
                int targetY = y + dy;

                if (targetX < 0 || targetX >= target.getWidth() ||
                    targetY < 0 || targetY >= target.getHeight()) {
                    continue;
                }

                // Sample from component sprite
                int srcSampleX = srcX + (dx * ICON_SIZE / size);
                int srcSampleY = srcY + (dy * ICON_SIZE / size);

                if (srcSampleX >= components.getWidth() || srcSampleY >= components.getHeight()) {
                    continue;
                }

                int srcPixel = components.getRGB(srcSampleX, srcSampleY);
                int srcAlpha = (srcPixel >> 24) & 0xFF;

                if (srcAlpha > 0) {
                    // Blend with color tint
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

    /**
     * Draws the unknown symbol (?) centered on the image.
     */
    private static void drawUnknownSymbol(BufferedImage target, BufferedImage components) {
        int size = WORK_SIZE / 2;
        int x = (WORK_SIZE - size) / 2;
        int y = (WORK_SIZE - size) / 2;
        drawComponent(target, components, 0, 0, x, y, size);
    }

    /**
     * Loads and caches the page background image.
     */
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

    /**
     * Loads and caches the symbol components sprite sheet.
     */
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

    /**
     * Scales an image to the target size.
     */
    private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    /**
     * Registers a BufferedImage as a dynamic texture.
     */
    private static ResourceLocation registerDynamicTexture(ResourceLocation id, BufferedImage image) {
        // Convert BufferedImage to NativeImage
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                // Convert ARGB to ABGR (NativeImage format)
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }

        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
        ResourceLocation textureLoc = new ResourceLocation(Mystcraft.MOD_ID, "dynamic/page_" + id.getPath().replace('/', '_'));

        // Clean up old texture if exists
        if (dynamicTextures.containsKey(id)) {
            dynamicTextures.get(id).close();
        }

        Minecraft.getInstance().getTextureManager().register(textureLoc, dynamicTexture);
        dynamicTextures.put(id, dynamicTexture);

        return textureLoc;
    }

    /**
     * Clears all cached textures. Call on resource reload.
     */
    public static void clearCache() {
        for (DynamicTexture texture : dynamicTextures.values()) {
            texture.close();
        }
        dynamicTextures.clear();
        symbolTextureCache.clear();
        pageBackgroundImage = null;
        symbolComponentsImage = null;
    }
}
