package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Generates the Art-of-Writing Guidebook screen background at runtime.
 *
 * <p>The aesthetic mirrors {@code BookTextureFactory.BookKind#GUIDEBOOK}: deep
 * midnight-navy leather borders, cream parchment pages, gold ruling and corner
 * filigree, and a quill emblem at the spine top. When a player opens the
 * Guidebook the screen reads as the same book they were holding moments
 * before.
 */
public class GuidebookTexture {

  private static final int TEXTURE_WIDTH = 512;
  private static final int TEXTURE_HEIGHT = 256;
  private static final int BOOK_WIDTH = 276;
  private static final int BOOK_HEIGHT = 180;

  private static Identifier textureLocation;
  private static boolean initialized = false;

  public static Identifier getTexture() {
    if (!initialized) {
      createTexture();
      initialized = true;
    }
    return textureLocation;
  }

  private static void createTexture() {
    NativeImage image = new NativeImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, true);

    for (int x = 0; x < TEXTURE_WIDTH; x++) {
      for (int y = 0; y < TEXTURE_HEIGHT; y++) {
        image.setPixelABGR(x, y, 0);
      }
    }

    int leather = abgr(255, 70, 38, 26);
    int leatherDeep = abgr(255, 50, 25, 18);
    int leatherHi = abgr(255, 100, 60, 42);
    int parchment = abgr(255, 230, 240, 245);
    int parchmentShade = abgr(255, 200, 215, 224);
    int gold = abgr(255, 60, 175, 220);
    int goldDim = abgr(255, 40, 130, 170);
    int ink = abgr(255, 50, 50, 80);

    int leftPageX = 4;
    int rightPageX = 138;
    int pageWidth = 130;
    int pageY = 4;
    int pageHeight = 172;

    fillRect(image, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, leather);

    int grainAlpha = abgr(40, 100, 60, 42);
    for (int y = 0; y < BOOK_HEIGHT; y += 3) {
      for (int x = 0; x < BOOK_WIDTH; x++) {
        if ((x + y) % 5 == 0) {
          safeBlend(image, x, y, grainAlpha);
        }
      }
    }

    for (int i = 0; i < 2; i++) {

      fillRect(image, i, i, BOOK_WIDTH - 2 * i, 1, leatherHi);

      fillRect(image, i, i, 1, BOOK_HEIGHT - 2 * i, leatherHi);

      fillRect(image, i, BOOK_HEIGHT - 1 - i, BOOK_WIDTH - 2 * i, 1, leatherDeep);

      fillRect(image, BOOK_WIDTH - 1 - i, i, 1, BOOK_HEIGHT - 2 * i, leatherDeep);
    }

    fillRect(image, 134, 0, 8, BOOK_HEIGHT, leatherDeep);

    fillRect(image, 134, 0, 1, BOOK_HEIGHT, leatherHi);
    fillRect(image, 141, 0, 1, BOOK_HEIGHT, leatherHi);

    fillRect(image, 135, 12, 6, 1, gold);
    fillRect(image, 135, 14, 6, 1, gold);

    fillRect(image, 135, BOOK_HEIGHT - 14, 6, 1, gold);
    fillRect(image, 135, BOOK_HEIGHT - 16, 6, 1, gold);

    drawSpineQuill(image, 138, BOOK_HEIGHT / 2, gold, ink);

    fillRect(image, leftPageX, pageY, pageWidth, pageHeight, parchment);
    fillRect(image, rightPageX, pageY, pageWidth, pageHeight, parchment);

    int parchmentDark = abgr(255, 180, 195, 205);
    for (int i = 0; i < 2; i++) {

      fillRect(image, leftPageX + i, pageY + i, pageWidth - 2 * i, 1, parchment);
      fillRect(image, leftPageX + i, pageY + i, 1, pageHeight - 2 * i, parchment);

      fillRect(image, leftPageX + i, pageY + pageHeight - 1 - i, pageWidth - 2 * i, 1, parchmentDark);
      fillRect(image, leftPageX + pageWidth - 1 - i, pageY + i, 1, pageHeight - 2 * i, parchmentDark);

      fillRect(image, rightPageX + i, pageY + i, pageWidth - 2 * i, 1, parchment);
      fillRect(image, rightPageX + i, pageY + i, 1, pageHeight - 2 * i, parchment);
      fillRect(image, rightPageX + i, pageY + pageHeight - 1 - i, pageWidth - 2 * i, 1, parchmentDark);
      fillRect(image, rightPageX + pageWidth - 1 - i, pageY + i, 1, pageHeight - 2 * i, parchmentDark);
    }

    for (int i = 0; i < 6; i++) {
      int alpha = 200 - (i * 32);
      if (alpha < 0) alpha = 0;
      int shadowColor = abgr(alpha,
          200 - i * 4, 215 - i * 4, 224 - i * 4);

      fillRect(image, leftPageX + pageWidth - 6 + i, pageY, 1, pageHeight, shadowColor);

      fillRect(image, rightPageX + i, pageY, 1, pageHeight, shadowColor);
    }

    for (int lineY = pageY + 20; lineY < pageY + pageHeight - 14; lineY += 12) {

      for (int x = leftPageX + 12; x < leftPageX + pageWidth - 12; x++) {
        if ((x + lineY) % 4 != 0) {
          safeBlend(image, x, lineY, abgr(80, 40, 130, 170));
        }
      }

      for (int x = rightPageX + 12; x < rightPageX + pageWidth - 12; x++) {
        if ((x + lineY) % 4 != 0) {
          safeBlend(image, x, lineY, abgr(80, 40, 130, 170));
        }
      }
    }

    drawCorner(image, leftPageX + 4, pageY + 4, gold, 0);
    drawCorner(image, leftPageX + pageWidth - 5, pageY + 4, gold, 1);
    drawCorner(image, leftPageX + 4, pageY + pageHeight - 5, gold, 2);
    drawCorner(image, leftPageX + pageWidth - 5, pageY + pageHeight - 5, gold, 3);

    drawCorner(image, rightPageX + 4, pageY + 4, gold, 0);
    drawCorner(image, rightPageX + pageWidth - 5, pageY + 4, gold, 1);
    drawCorner(image, rightPageX + 4, pageY + pageHeight - 5, gold, 2);
    drawCorner(image, rightPageX + pageWidth - 5, pageY + pageHeight - 5, gold, 3);

    safeSetPixel(image, leftPageX + 1, pageY + pageHeight / 2, gold);
    safeSetPixel(image, leftPageX + 2, pageY + pageHeight / 2, goldDim);
    safeSetPixel(image, rightPageX + pageWidth - 2, pageY + pageHeight / 2, gold);
    safeSetPixel(image, rightPageX + pageWidth - 3, pageY + pageHeight / 2, goldDim);

    DynamicTexture dynamicTexture = new DynamicTexture(
        () -> "Mystcraft guidebook texture",
        image);
    textureLocation = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "guidebook");
    Minecraft.getInstance().getTextureManager().register(textureLocation, dynamicTexture);

    Mystcraft.LOGGER.debug("Created guidebook texture: {}", textureLocation);
  }

  private static void drawSpineQuill(NativeImage image, int cx, int cy, int gold, int ink) {

    for (int i = -10; i <= 10; i++) {
      safeSetPixel(image, cx + i / 2, cy - i, gold);
    }

    for (int i = 0; i < 3; i++) {
      int by = cy - 6 + i * 2;
      safeSetPixel(image, cx - 2, by, gold);
      safeSetPixel(image, cx - 3, by + 1, gold);
    }

    safeSetPixel(image, cx + 5, cy + 10, ink);
    safeSetPixel(image, cx + 6, cy + 10, ink);
    safeSetPixel(image, cx + 5, cy + 11, ink);
  }

  private static void drawCorner(NativeImage image, int x, int y, int color, int orient) {
    int dx = (orient == 1 || orient == 3) ? -1 : 1;
    int dy = (orient == 2 || orient == 3) ? -1 : 1;

    for (int i = 0; i < 5; i++) {
      safeSetPixel(image, x + dx * i, y, color);
      safeSetPixel(image, x, y + dy * i, color);
    }

    safeSetPixel(image, x + dx * 2, y + dy * 2, color);
    safeSetPixel(image, x + dx * 3, y + dy * 3, color);
  }

  private static void fillRect(NativeImage image, int x, int y, int width, int height, int color) {
    for (int px = x; px < x + width && px < image.getWidth(); px++) {
      for (int py = y; py < y + height && py < image.getHeight(); py++) {
        if (px >= 0 && py >= 0) {
          image.setPixelABGR(px, py, color);
        }
      }
    }
  }

  private static void safeSetPixel(NativeImage image, int x, int y, int color) {
    if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
      image.setPixelABGR(x, y, color);
    }
  }

  private static void safeBlend(NativeImage image, int x, int y, int abgr) {
    if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
      return;
    }
    int dst = net.minecraft.util.ARGB.toABGR(image.getPixel(x, y));
    int sa = (abgr >>> 24) & 0xFF;
    if (sa == 0) {
      return;
    }
    if (sa == 255 || ((dst >>> 24) & 0xFF) == 0) {
      image.setPixelABGR(x, y, abgr);
      return;
    }
    int sb = (abgr >>> 16) & 0xFF;
    int sg = (abgr >>> 8) & 0xFF;
    int sr = abgr & 0xFF;
    int da = (dst >>> 24) & 0xFF;
    int db = (dst >>> 16) & 0xFF;
    int dg = (dst >>> 8) & 0xFF;
    int dr = dst & 0xFF;
    int outA = sa + (da * (255 - sa)) / 255;
    int outR = (sr * sa + dr * (255 - sa)) / 255;
    int outG = (sg * sa + dg * (255 - sa)) / 255;
    int outB = (sb * sa + db * (255 - sa)) / 255;
    image.setPixelABGR(x, y, abgr(outA, outB, outG, outR));
  }

  private static int abgr(int a, int b, int g, int r) {
    return (a << 24) | (b << 16) | (g << 8) | r;
  }

  public static void reset() {
    initialized = false;
    textureLocation = null;
  }
}
