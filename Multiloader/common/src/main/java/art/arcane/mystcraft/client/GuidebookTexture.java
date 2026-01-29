package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Generates a clean black and white book texture at runtime.
 */
public class GuidebookTexture {

  private static final int TEXTURE_WIDTH = 512;
  private static final int TEXTURE_HEIGHT = 256;
  private static final int BOOK_WIDTH = 276;
  private static final int BOOK_HEIGHT = 180;

  private static ResourceLocation textureLocation;
  private static boolean initialized = false;

  public static ResourceLocation getTexture() {
    if (!initialized) {
      createTexture();
      initialized = true;
    }
    return textureLocation;
  }

  private static void createTexture() {
    NativeImage image = new NativeImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, true);

    // Fill with transparent
    for (int x = 0; x < TEXTURE_WIDTH; x++) {
      for (int y = 0; y < TEXTURE_HEIGHT; y++) {
        image.setPixelRGBA(x, y, 0);
      }
    }

    // Colors (ABGR format for NativeImage)
    int pageBg = abgr(255, 245, 240, 230);      // Cream/off-white page
    int border = abgr(255, 60, 50, 40);          // Dark brown border
    int spine = abgr(255, 80, 70, 60);           // Darker spine
    int shadow = abgr(255, 200, 190, 180);       // Light shadow
    int pageLine = abgr(255, 220, 210, 200);     // Faint line color

    int leftPageX = 4;
    int rightPageX = 138;
    int pageWidth = 130;
    int pageY = 4;
    int pageHeight = 172;

    // Draw book cover/border
    fillRect(image, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, border);

    // Draw spine (center vertical strip)
    fillRect(image, 134, 0, 8, BOOK_HEIGHT, spine);

    // Draw left page background
    fillRect(image, leftPageX, pageY, pageWidth, pageHeight, pageBg);

    // Draw right page background
    fillRect(image, rightPageX, pageY, pageWidth, pageHeight, pageBg);

    // Draw page shadows (inner edges near spine)
    for (int i = 0; i < 4; i++) {
      int alpha = 255 - (i * 40);
      int shadowColor = abgr(alpha, 180 + i * 15, 170 + i * 15, 160 + i * 15);
      // Left page right edge shadow
      fillRect(image, leftPageX + pageWidth - 4 + i, pageY, 1, pageHeight, shadowColor);
      // Right page left edge shadow
      fillRect(image, rightPageX + i, pageY, 1, pageHeight, shadowColor);
    }

    // Draw subtle horizontal lines on pages (like ruled paper)
    for (int lineY = pageY + 20; lineY < pageY + pageHeight - 10; lineY += 12) {
      // Left page lines
      for (int x = leftPageX + 10; x < leftPageX + pageWidth - 10; x++) {
        if ((x + lineY) % 3 != 0) { // Dotted effect
          image.setPixelRGBA(x, lineY, pageLine);
        }
      }
      // Right page lines
      for (int x = rightPageX + 10; x < rightPageX + pageWidth - 10; x++) {
        if ((x + lineY) % 3 != 0) {
          image.setPixelRGBA(x, lineY, pageLine);
        }
      }
    }

    // Draw corner decorations - each corner points outward
    // Left page corners
    drawCornerTopLeft(image, leftPageX + 2, pageY + 2, border);
    drawCornerTopRight(image, leftPageX + pageWidth - 7, pageY + 2, border);
    drawCornerBottomLeft(image, leftPageX + 2, pageY + pageHeight - 7, border);
    drawCornerBottomRight(image, leftPageX + pageWidth - 7, pageY + pageHeight - 7, border);

    // Right page corners
    drawCornerTopLeft(image, rightPageX + 2, pageY + 2, border);
    drawCornerTopRight(image, rightPageX + pageWidth - 7, pageY + 2, border);
    drawCornerBottomLeft(image, rightPageX + 2, pageY + pageHeight - 7, border);
    drawCornerBottomRight(image, rightPageX + pageWidth - 7, pageY + pageHeight - 7, border);

    // Create dynamic texture and register it
    DynamicTexture dynamicTexture = new DynamicTexture(image);
    textureLocation = Minecraft.getInstance().getTextureManager()
        .register("mystcraft_guidebook", dynamicTexture);

    Mystcraft.LOGGER.debug("Created guidebook texture: {}", textureLocation);
  }

  private static void fillRect(NativeImage image, int x, int y, int width, int height, int color) {
    for (int px = x; px < x + width && px < image.getWidth(); px++) {
      for (int py = y; py < y + height && py < image.getHeight(); py++) {
        if (px >= 0 && py >= 0) {
          image.setPixelRGBA(px, py, color);
        }
      }
    }
  }

  private static void drawCornerTopLeft(NativeImage image, int x, int y, int color) {
    // L shape pointing top-left: horizontal goes right, vertical goes down
    for (int i = 0; i < 5; i++) {
      safeSetPixel(image, x + i, y, color);      // horizontal right
      safeSetPixel(image, x, y + i, color);      // vertical down
    }
  }

  private static void drawCornerTopRight(NativeImage image, int x, int y, int color) {
    // L shape pointing top-right: horizontal goes left, vertical goes down
    for (int i = 0; i < 5; i++) {
      safeSetPixel(image, x + 4 - i, y, color);  // horizontal left
      safeSetPixel(image, x + 4, y + i, color);  // vertical down
    }
  }

  private static void drawCornerBottomLeft(NativeImage image, int x, int y, int color) {
    // L shape pointing bottom-left: horizontal goes right, vertical goes up
    for (int i = 0; i < 5; i++) {
      safeSetPixel(image, x + i, y + 4, color);  // horizontal right
      safeSetPixel(image, x, y + 4 - i, color);  // vertical up
    }
  }

  private static void drawCornerBottomRight(NativeImage image, int x, int y, int color) {
    // L shape pointing bottom-right: horizontal goes left, vertical goes up
    for (int i = 0; i < 5; i++) {
      safeSetPixel(image, x + 4 - i, y + 4, color);  // horizontal left
      safeSetPixel(image, x + 4, y + 4 - i, color);  // vertical up
    }
  }

  private static void safeSetPixel(NativeImage image, int x, int y, int color) {
    if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
      image.setPixelRGBA(x, y, color);
    }
  }

  private static int abgr(int a, int b, int g, int r) {
    return (a << 24) | (b << 16) | (g << 8) | r;
  }

  public static void reset() {
    initialized = false;
    textureLocation = null;
  }
}
