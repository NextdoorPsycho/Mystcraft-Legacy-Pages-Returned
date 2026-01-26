package art.arcane.mystcraft.client.render;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

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
 * Custom BlockEntityWithoutLevelRenderer for page items.
 * Renders pages with their D'ni symbols.
 */
@OnlyIn(Dist.CLIENT)
public class PageItemRendererBEWLR extends BlockEntityWithoutLevelRenderer {

    private static PageItemRendererBEWLR instance;

    public static PageItemRendererBEWLR getInstance() {
        if (instance == null) {
            instance = new PageItemRendererBEWLR();
        }
        return instance;
    }

    private static final ResourceLocation PAGE_BACKGROUND_LOC =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/items/page_background.png");

    // Cache for generated textures
    private static final Map<String, ResourceLocation> textureCache = new HashMap<>();
    private static final Map<String, DynamicTexture> dynamicTextures = new HashMap<>();

    private static BufferedImage pageBackgroundImage = null;
    private static BufferedImage symbolComponentsImage = null;

    private static final int OUTPUT_SIZE = 128;  // Higher resolution for better quality
    private static final int WORK_SIZE = 512;   // Larger work size for crisp symbols
    private static final int ICON_SIZE = 64;
    private static final int SPRITESHEET_COLS = 8;

    private PageItemRendererBEWLR() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof PageItem)) {
            return;
        }

        // Get or create the texture for this page
        String cacheKey = getCacheKey(stack);
        boolean isNewTexture = !textureCache.containsKey(cacheKey);
        ResourceLocation texture = getOrCreateTexture(stack);
        if (isNewTexture) {
            Mystcraft.LOGGER.info("PageItemRendererBEWLR created texture for: {}", cacheKey);
        }

        poseStack.pushPose();

        // Small offset to prevent z-fighting between front and back faces
        float zOffset = 0.003f;

        // Determine light level - GUI always uses full bright
        int light = (displayContext == ItemDisplayContext.GUI) ? 0xF000F0 : packedLight;

        // Use entityCutoutNoCull for proper rendering with transparency
        RenderType renderType = RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        Matrix4f matrix = poseStack.last().pose();

        if (displayContext == ItemDisplayContext.GUI) {
            // GUI: Render 0-1 quad, full brightness
            renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, true);
        } else if (displayContext == ItemDisplayContext.GROUND) {
            // Ground: Render 0-1 quad - model transforms handle the scaling
            // The model JSON already scales to 0.25, so render at full size
            renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, true);
            renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, false);
        } else if (displayContext == ItemDisplayContext.FIXED) {
            // Item frame: render both sides
            renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, true);
            renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, false);
        } else {
            // Hand rendering (first/third person): render both sides
            renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, true);
            renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, false);
        }

        poseStack.popPose();
    }

    private void renderQuad(VertexConsumer consumer, Matrix4f matrix,
                            float x1, float y1, float x2, float y2, float z, float zOffset,
                            int light, int overlay, boolean front) {
        float zPos = front ? z + zOffset : z - zOffset;
        float normalZ = front ? 1 : -1;

        if (front) {
            // Front face (counter-clockwise when viewed from front)
            consumer.vertex(matrix, x1, y1, zPos).color(255, 255, 255, 255)
                    .uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
            consumer.vertex(matrix, x2, y1, zPos).color(255, 255, 255, 255)
                    .uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
            consumer.vertex(matrix, x2, y2, zPos).color(255, 255, 255, 255)
                    .uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
            consumer.vertex(matrix, x1, y2, zPos).color(255, 255, 255, 255)
                    .uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
        } else {
            // Back face (clockwise when viewed from back = counter-clockwise from front)
            consumer.vertex(matrix, x1, y2, zPos).color(255, 255, 255, 255)
                    .uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
            consumer.vertex(matrix, x2, y2, zPos).color(255, 255, 255, 255)
                    .uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
            consumer.vertex(matrix, x2, y1, zPos).color(255, 255, 255, 255)
                    .uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
            consumer.vertex(matrix, x1, y1, zPos).color(255, 255, 255, 255)
                    .uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
        }
    }

    private ResourceLocation getOrCreateTexture(ItemStack stack) {
        String cacheKey = getCacheKey(stack);

        if (textureCache.containsKey(cacheKey)) {
            return textureCache.get(cacheKey);
        }

        try {
            BufferedImage image = generatePageImage(stack);
            ResourceLocation texLoc = registerTexture(cacheKey, image);
            textureCache.put(cacheKey, texLoc);
            return texLoc;
        } catch (Exception e) {
            Mystcraft.LOGGER.error("Failed to generate page texture", e);
            return PAGE_BACKGROUND_LOC;
        }
    }

    private String getCacheKey(ItemStack stack) {
        if (Page.isLinkPanel(stack)) {
            return "linkpanel";
        }
        ResourceLocation symbolId = Page.getSymbol(stack);
        if (symbolId != null) {
            return "symbol_" + symbolId.toString().replace(':', '_').replace('/', '_');
        }
        return "blank";
    }

    private BufferedImage generatePageImage(ItemStack stack) {
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

        if (Page.isLinkPanel(stack)) {
            // Draw black rectangle for link panel
            Graphics2D g2 = work.createGraphics();
            int panelX = (int) (WORK_SIZE * 0.15);
            int panelY = (int) (WORK_SIZE * 0.15);
            int panelWidth = (int) (WORK_SIZE * 0.7);
            int panelHeight = (int) (WORK_SIZE * 0.35);
            g2.setColor(Color.BLACK);
            g2.fillRect(panelX, panelY, panelWidth, panelHeight);
            g2.dispose();
        } else if (components != null) {
            ResourceLocation symbolId = Page.getSymbol(stack);
            if (symbolId != null) {
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
        }

        return scaleImage(work, OUTPUT_SIZE, OUTPUT_SIZE);
    }

    private void drawSymbolOnImage(BufferedImage target, BufferedImage components, String[] words) {
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

    private void drawWord(BufferedImage target, BufferedImage components, String wordName, int x, int y, int size) {
        DrawableWord word = DrawableWordManager.getDrawableWord(wordName);
        if (word == null) {
            return;
        }

        List<Integer> wordComponents = word.components();
        List<Integer> colors = word.colors();

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

    private void drawComponent(BufferedImage target, BufferedImage components,
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

    private void drawUnknownSymbol(BufferedImage target, BufferedImage components) {
        int size = WORK_SIZE / 2;
        int x = (WORK_SIZE - size) / 2;
        int y = (WORK_SIZE - size) / 2;
        drawComponent(target, components, 0, 0, x, y, size);
    }

    private BufferedImage getPageBackground() {
        if (pageBackgroundImage != null) {
            return pageBackgroundImage;
        }

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(PAGE_BACKGROUND_LOC);
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

    private BufferedImage getSymbolComponents() {
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

    private BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private BufferedImage createBlankImage() {
        BufferedImage blank = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = blank.createGraphics();
        g.setColor(new Color(0xF8F0E0));
        g.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
        g.dispose();
        return blank;
    }

    private ResourceLocation registerTexture(String key, BufferedImage image) {
        // Convert to NativeImage
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }

        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
        ResourceLocation texLoc = new ResourceLocation(Mystcraft.MOD_ID, "dynamic/page_" + key);

        // Clean up old texture if exists
        if (dynamicTextures.containsKey(key)) {
            dynamicTextures.get(key).close();
        }

        Minecraft.getInstance().getTextureManager().register(texLoc, dynamicTexture);
        dynamicTextures.put(key, dynamicTexture);

        return texLoc;
    }

    /**
     * Clear cached textures on resource reload.
     */
    public static void clearCache() {
        for (DynamicTexture texture : dynamicTextures.values()) {
            texture.close();
        }
        dynamicTextures.clear();
        textureCache.clear();
        pageBackgroundImage = null;
        symbolComponentsImage = null;
    }
}
