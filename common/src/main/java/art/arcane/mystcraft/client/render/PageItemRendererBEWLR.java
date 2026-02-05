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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom BlockEntityWithoutLevelRenderer for page items.
 * Renders pages with their D'ni symbols.
 * <p>
 * Texture generation uses bulk int[] pixel array operations for performance.
 * All symbol textures are pre-warmed asynchronously during client setup.
 */
public class PageItemRendererBEWLR extends BlockEntityWithoutLevelRenderer {

  private static final ResourceLocation PAGE_BACKGROUND_LOC =
      new ResourceLocation(Mystcraft.MOD_ID, "textures/items/page_background.png");
  // Cache for generated textures - thread-safe for async prewarm
  private static final Map<String, ResourceLocation> textureCache = new ConcurrentHashMap<>();
  private static final Map<String, DynamicTexture> dynamicTextures = new ConcurrentHashMap<>();
  // Pre-generated images from background thread, awaiting main-thread texture registration
  private static final Map<String, BufferedImage> pendingImages = new ConcurrentHashMap<>();
  private static final int OUTPUT_SIZE = 128;
  private static final int WORK_SIZE = 256;   // 2x oversample is sufficient for 128px output
  private static final int ICON_SIZE = 64;
  private static final int SPRITESHEET_COLS = 8;
  private static PageItemRendererBEWLR instance;
  private static BufferedImage pageBackgroundImage = null;
  private static BufferedImage symbolComponentsImage = null;
  // Cached int[] pixel data for the symbol components spritesheet
  private static int[] componentPixels = null;
  private static int componentImageWidth = 0;
  private static int componentImageHeight = 0;
  private static volatile boolean prewarming = false;
  private static volatile boolean prewarmComplete = false;

  private PageItemRendererBEWLR() {
    super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
        Minecraft.getInstance().getEntityModels());
  }

  public static PageItemRendererBEWLR getInstance() {
    if (instance == null) {
      instance = new PageItemRendererBEWLR();
    }
    return instance;
  }

  private static String getCacheKey(ItemStack stack) {
    if (Page.isLinkPanel(stack)) {
      return "linkpanel";
    }
    ResourceLocation symbolId = Page.getSymbol(stack);
    if (symbolId != null) {
      return "symbol_" + symbolId.toString().replace(':', '_').replace('/', '_');
    }
    return "blank";
  }

  /**
   * Gets the cache key for a symbol by its ResourceLocation directly.
   */
  private static String getCacheKeyForSymbol(ResourceLocation symbolId) {
    return "symbol_" + symbolId.toString().replace(':', '_').replace('/', '_');
  }

  // --- Texture Cache ---

  /**
   * Generates a page image for a symbol without needing an ItemStack.
   * Used by the async prewarm system.
   */
  private static BufferedImage generateSymbolImage(ResourceLocation symbolId, BufferedImage background) {
    if (background == null) {
      return createBlankImage();
    }

    int[] workPixels = createWorkImage(background);
    drawSymbolById(workPixels, symbolId);
    return scaleWorkToOutput(workPixels);
  }

  /**
   * Creates a WORK_SIZE x WORK_SIZE int[] from the background image.
   */
  private static int[] createWorkImage(BufferedImage background) {
    BufferedImage work = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = work.createGraphics();
    g.drawImage(background, 0, 0, WORK_SIZE, WORK_SIZE, null);
    g.dispose();
    return ((DataBufferInt) work.getRaster().getDataBuffer()).getData();
  }

  private static void drawLinkPanel(int[] pixels) {
    int panelX = (int) (WORK_SIZE * 0.15);
    int panelY = (int) (WORK_SIZE * 0.15);
    int panelWidth = (int) (WORK_SIZE * 0.7);
    int panelHeight = (int) (WORK_SIZE * 0.35);

    for (int dy = 0; dy < panelHeight; dy++) {
      int rowStart = (panelY + dy) * WORK_SIZE + panelX;
      Arrays.fill(pixels, rowStart, rowStart + panelWidth, 0xFF000000);
    }
  }

  // --- Image Generation ---

  private static void drawSymbolById(int[] workPixels, ResourceLocation symbolId) {
    if (!ensureComponentPixels()) return;

    IAgeSymbol symbol = SymbolRegistry.get(symbolId);
    if (symbol != null) {
      String[] words = symbol.getPoem();
      if (words != null && words.length > 0) {
        drawSymbolWords(workPixels, words);
      } else {
        drawUnknownSymbol(workPixels);
      }
    } else {
      drawUnknownSymbol(workPixels);
    }
  }

  private static void drawSymbolWords(int[] targetPixels, String[] words) {
    float scale = WORK_SIZE / 2.414f;
    float offset = scale * 1.414f / 2;
    int symbolSize = (int) (scale * 0.8f);

    int centerX = WORK_SIZE / 2;
    int centerY = WORK_SIZE / 2;

    if (words.length > 0) {
      drawWord(targetPixels, words[0], centerX - symbolSize / 2, (int) (centerY - offset - symbolSize / 2), symbolSize);
    }
    if (words.length > 1) {
      drawWord(targetPixels, words[1], (int) (centerX + offset - symbolSize / 2), centerY - symbolSize / 2, symbolSize);
    }
    if (words.length > 2) {
      drawWord(targetPixels, words[2], centerX - symbolSize / 2, (int) (centerY + offset - symbolSize / 2), symbolSize);
    }
    if (words.length > 3) {
      drawWord(targetPixels, words[3], (int) (centerX - offset - symbolSize / 2), centerY - symbolSize / 2, symbolSize);
    }
  }

  private static void drawWord(int[] targetPixels, String wordName, int x, int y, int size) {
    DrawableWord word = DrawableWordManager.getDrawableWord(wordName);
    if (word == null) return;

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

      drawComponent(targetPixels, componentIndex, color, x, y, size);
    }
  }

  /**
   * Draws a component from the spritesheet onto the target pixel array.
   * Uses direct int[] array access for performance (no getRGB/setRGB calls).
   */
  private static void drawComponent(int[] targetPixels, int componentIndex, int color,
                                    int x, int y, int size) {
    int srcX = (componentIndex % SPRITESHEET_COLS) * ICON_SIZE;
    int srcY = (componentIndex / SPRITESHEET_COLS) * ICON_SIZE;

    int colorR = (color >> 16) & 0xFF;
    int colorG = (color >> 8) & 0xFF;
    int colorB = color & 0xFF;

    for (int dy = 0; dy < size; dy++) {
      int targetY = y + dy;
      if (targetY < 0 || targetY >= WORK_SIZE) continue;

      int targetRowOffset = targetY * WORK_SIZE;
      int srcSampleY = srcY + (dy * ICON_SIZE / size);
      if (srcSampleY >= componentImageHeight) continue;

      int srcRowOffset = srcSampleY * componentImageWidth;

      for (int dx = 0; dx < size; dx++) {
        int targetX = x + dx;
        if (targetX < 0 || targetX >= WORK_SIZE) continue;

        int srcSampleX = srcX + (dx * ICON_SIZE / size);
        if (srcSampleX >= componentImageWidth) continue;

        int srcPixel = componentPixels[srcRowOffset + srcSampleX];
        int srcAlpha = (srcPixel >> 24) & 0xFF;

        if (srcAlpha > 0) {
          int targetIdx = targetRowOffset + targetX;
          int targetPixel = targetPixels[targetIdx];

          int targetR = (targetPixel >> 16) & 0xFF;
          int targetG = (targetPixel >> 8) & 0xFF;
          int targetB = targetPixel & 0xFF;

          // Alpha blend with integer math (avoid float per-pixel)
          int invAlpha = 255 - srcAlpha;
          int newR = (colorR * srcAlpha + targetR * invAlpha) / 255;
          int newG = (colorG * srcAlpha + targetG * invAlpha) / 255;
          int newB = (colorB * srcAlpha + targetB * invAlpha) / 255;

          targetPixels[targetIdx] = 0xFF000000 | (newR << 16) | (newG << 8) | newB;
        }
      }
    }
  }

  private static void drawUnknownSymbol(int[] targetPixels) {
    if (!ensureComponentPixels()) return;
    int size = WORK_SIZE / 2;
    int x = (WORK_SIZE - size) / 2;
    int y = (WORK_SIZE - size) / 2;
    drawComponent(targetPixels, 0, 0, x, y, size);
  }

  private static BufferedImage scaleWorkToOutput(int[] workPixels) {
    BufferedImage work = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
    work.setRGB(0, 0, WORK_SIZE, WORK_SIZE, workPixels, 0, WORK_SIZE);

    BufferedImage scaled = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = scaled.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(work, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, null);
    g.dispose();
    return scaled;
  }

  private static BufferedImage createBlankImage() {
    BufferedImage blank = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = blank.createGraphics();
    g.setColor(new Color(0xF8F0E0));
    g.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
    g.dispose();
    return blank;
  }

  private static BufferedImage loadPageBackground() {
    try {
      if (Minecraft.getInstance().getResourceManager() == null) {
        return null;
      }
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(PAGE_BACKGROUND_LOC);
      if (resource.isPresent()) {
        try (InputStream is = resource.get().open()) {
          return ImageIO.read(is);
        }
      }
    } catch (IOException e) {
      Mystcraft.LOGGER.error("Failed to load page background texture", e);
    }
    return null;
  }

  /**
   * Ensures the component spritesheet pixels are loaded into the cached int[] array.
   */
  private static boolean ensureComponentPixels() {
    if (componentPixels != null) return true;

    try {
      if (Minecraft.getInstance().getResourceManager() == null) {
        return false;
      }
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(DrawableWord.WORD_COMPONENTS);
      if (resource.isPresent()) {
        try (InputStream is = resource.get().open()) {
          BufferedImage img = ImageIO.read(is);
          componentImageWidth = img.getWidth();
          componentImageHeight = img.getHeight();
          componentPixels = img.getRGB(0, 0, componentImageWidth, componentImageHeight,
              null, 0, componentImageWidth);
          return true;
        }
      }
    } catch (IOException e) {
      Mystcraft.LOGGER.error("Failed to load symbol components texture", e);
    }
    return false;
  }

  // --- Image Utilities ---

  /**
   * Pre-generates all symbol page textures on a background thread.
   * Call this during client setup after DrawableWordManager is initialized.
   * The generated images are queued and registered on the render thread.
   */
  public static void prewarmCache() {
    if (prewarming || prewarmComplete) return;
    prewarming = true;

    Thread thread = new Thread(() -> {
      boolean success = false;
      try {
        // Load resources we need
        BufferedImage background = loadPageBackground();
        if (background == null) {
          Mystcraft.LOGGER.debug("Cannot prewarm page textures: background image not available");
          return;
        }

        // Ensure component pixels are loaded
        if (!ensureComponentPixels()) {
          Mystcraft.LOGGER.debug("Cannot prewarm page textures: component sprites not available");
          return;
        }

        // Cache the background for later use
        pageBackgroundImage = background;

        // Generate link panel
        int[] linkPixels = createWorkImage(background);
        drawLinkPanel(linkPixels);
        pendingImages.put("linkpanel", scaleWorkToOutput(linkPixels));

        // Generate all symbol textures
        Collection<IAgeSymbol> allSymbols = SymbolRegistry.getAll();
        int count = 0;
        for (IAgeSymbol symbol : allSymbols) {
          String key = getCacheKeyForSymbol(symbol.getRegistryName());
          if (!textureCache.containsKey(key)) {
            BufferedImage img = generateSymbolImage(symbol.getRegistryName(), background);
            pendingImages.put(key, img);
            count++;
          }
        }

        Mystcraft.LOGGER.info("Pre-warmed {} page textures (awaiting GPU upload)", count + 1);
        success = true;
      } catch (Exception e) {
        Mystcraft.LOGGER.error("Failed to prewarm page textures", e);
      } finally {
        prewarming = false;
        prewarmComplete = success;
      }
    }, "Mystcraft-PageTexture-Prewarm");
    thread.setDaemon(true);
    thread.start();
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
    pendingImages.clear();
    pageBackgroundImage = null;
    symbolComponentsImage = null;
    componentPixels = null;
    componentImageWidth = 0;
    componentImageHeight = 0;
    prewarmComplete = false;
  }

  // --- Resource Loading ---

  @Override
  public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    if (!(stack.getItem() instanceof PageItem)) {
      return;
    }

    // Register any pending pre-warmed textures on the main thread
    flushPendingTextures();

    // Get or create the texture for this page
    ResourceLocation texture = getOrCreateTexture(stack);

    poseStack.pushPose();

    float zOffset = 0.003f;
    int light = (displayContext == ItemDisplayContext.GUI) ? 0xF000F0 : packedLight;

    RenderType renderType = RenderType.entityCutoutNoCull(texture);
    VertexConsumer consumer = bufferSource.getBuffer(renderType);
    Matrix4f matrix = poseStack.last().pose();

    if (displayContext == ItemDisplayContext.GUI) {
      renderQuad(consumer, matrix, 0, 0, 1, 1, 0.5f, zOffset, light, packedOverlay, true);
    } else {
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
      consumer.vertex(matrix, x1, y1, zPos).color(255, 255, 255, 255)
          .uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y1, zPos).color(255, 255, 255, 255)
          .uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x2, y2, zPos).color(255, 255, 255, 255)
          .uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
      consumer.vertex(matrix, x1, y2, zPos).color(255, 255, 255, 255)
          .uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, normalZ).endVertex();
    } else {
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

    ResourceLocation cached = textureCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    try {
      BufferedImage image = generatePageImage(stack);
      ResourceLocation texLoc = registerTexture(cacheKey, image);
      return texLoc;
    } catch (Exception e) {
      Mystcraft.LOGGER.error("Failed to generate page texture", e);
      return PAGE_BACKGROUND_LOC;
    }
  }

  // --- Texture Registration ---

  private BufferedImage generatePageImage(ItemStack stack) {
    BufferedImage background = getPageBackground();

    if (background == null) {
      return createBlankImage();
    }

    int[] workPixels = createWorkImage(background);

    if (Page.isLinkPanel(stack)) {
      drawLinkPanel(workPixels);
    } else {
      ResourceLocation symbolId = Page.getSymbol(stack);
      if (symbolId != null) {
        drawSymbolById(workPixels, symbolId);
      }
    }

    return scaleWorkToOutput(workPixels);
  }

  // --- Async Pre-warming ---

  private BufferedImage getPageBackground() {
    if (pageBackgroundImage != null) {
      return pageBackgroundImage;
    }
    pageBackgroundImage = loadPageBackground();
    return pageBackgroundImage;
  }

  private ResourceLocation registerTexture(String key, BufferedImage image) {
    int w = image.getWidth();
    int h = image.getHeight();

    // Bulk read all pixels at once
    int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);

    NativeImage nativeImage = new NativeImage(w, h, false);
    for (int i = 0; i < pixels.length; i++) {
      int argb = pixels[i];
      int a = (argb >> 24) & 0xFF;
      int r = (argb >> 16) & 0xFF;
      int g = (argb >> 8) & 0xFF;
      int b = argb & 0xFF;
      // NativeImage uses ABGR format
      nativeImage.setPixelRGBA(i % w, i / w, (a << 24) | (b << 16) | (g << 8) | r);
    }

    DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
    ResourceLocation texLoc = new ResourceLocation(Mystcraft.MOD_ID, "dynamic/page_" + key);

    DynamicTexture old = dynamicTextures.put(key, dynamicTexture);
    if (old != null) {
      old.close();
    }

    Minecraft.getInstance().getTextureManager().register(texLoc, dynamicTexture);
    textureCache.put(key, texLoc);

    return texLoc;
  }

  // --- Cache Management ---

  /**
   * Registers any pre-warmed images as GPU textures.
   * Must be called from the render thread.
   */
  private void flushPendingTextures() {
    if (pendingImages.isEmpty()) return;

    // Process a batch per frame to avoid stalling
    Iterator<Map.Entry<String, BufferedImage>> it = pendingImages.entrySet().iterator();
    int batchSize = 10;
    int processed = 0;

    while (it.hasNext() && processed < batchSize) {
      Map.Entry<String, BufferedImage> entry = it.next();
      if (!textureCache.containsKey(entry.getKey())) {
        registerTexture(entry.getKey(), entry.getValue());
      }
      it.remove();
      processed++;
    }
  }
}
