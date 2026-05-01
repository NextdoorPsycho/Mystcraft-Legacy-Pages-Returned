package art.arcane.mystcraft.client.gui.procedural;

import art.arcane.mystcraft.data.Page;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Generates per-page parchment textures used for thumbnail page rendering on
 * the writing desk and the inside-of-book pages on the {@code BookScreen}.
 * <p>
 * The generated image is laid out as a 256x256 sheet with the parchment fill
 * living in the legacy {156, 0, 30, 40} sub-region so existing draw code in
 * {@link art.arcane.mystcraft.client.render.PageRenderHelper} blits unchanged
 * after swapping the texture id.
 * <p>
 * Parchment style (color, ruling density, ink-stain near the spine) responds to
 * the page's:
 * <ul>
 *   <li>Type: blank vs. symbol vs. link panel</li>
 *   <li>Recorded ink tint (from {@link Page#getInkTint(ItemStack)})</li>
 *   <li>Affinity snapshot presence (subtle "themed" stain)</li>
 * </ul>
 */
public final class PageTextureFactory {

  public static final int TEX_WIDTH = 256;
  public static final int TEX_HEIGHT = 256;

  /**
   * Sub-region matching the legacy {@code bookui_pagel.png} background slot.
   */
  public static final int SUB_U = 156;
  public static final int SUB_V = 0;
  public static final int SUB_W = 30;
  public static final int SUB_H = 40;

  private static final ProceduralTextureCache CACHE = new ProceduralTextureCache("page_bg", 64);

  private PageTextureFactory() {
  }

  /**
   * Reset cache (resource pack reload).
   */
  public static void reset() {
    CACHE.clear();
  }

  /**
   * Returns a {@link ResourceLocation} for a parchment background sized for the
   * legacy {@code bookui_pagel.png} sub-region. Caches by page kind so
   * thumbnail rendering on the writing desk doesn't allocate per-page.
   */
  @NotNull
  public static ResourceLocation getPageBackground(@NotNull ItemStack page) {
    PageInfo info = analyse(page);
    String key = info.kind() + ":" + Integer.toHexString(info.tint() & 0xFFFFFF) + (info.affinity() ? ":a" : "");
    return CACHE.getOrCreate(key, TEX_WIDTH, TEX_HEIGHT,
        image -> render(image, info.kind(), info.tint(), info.affinity()));
  }

  private static PageInfo analyse(ItemStack page) {
    if (page.isEmpty()) {
      return new PageInfo("empty", -1, false);
    }
    if (Page.isLinkPanel(page)) {
      CompoundTag aff = Page.getAffinitySnapshot(page);
      return new PageInfo("linkpanel", Page.getInkTint(page), aff != null && !aff.isEmpty());
    }
    if (Page.getSymbol(page) != null) {
      return new PageInfo("symbol", -1, false);
    }
    return new PageInfo("blank", -1, false);
  }

  private static void render(NativeImage image, String kind, int tint, boolean affinity) {
    fillTransparent(image);

    int parchment = "empty".equals(kind) ? 0xFFEBE3D0 : 0xFFF1E8CC;
    int ruleColor = 0x60A88E5C;
    int edgeColor = 0xFFB89B68;
    int spineShade = 0xFFC4A77A;

    int x0 = SUB_U;
    int y0 = SUB_V;
    int w = SUB_W;
    int h = SUB_H;

    fillRect(image, x0, y0, w, h, parchment);

    int ruleSpacing = 4;
    for (int ly = y0 + 4; ly < y0 + h - 3; ly += ruleSpacing) {
      for (int lx = x0 + 2; lx < x0 + w - 2; lx++) {
        if (((lx + ly) & 1) == 0) {
          safeSet(image, lx, ly, ruleColor);
        }
      }
    }

    drawRectOutline(image, x0, y0, w, h, edgeColor);

    for (int sy = y0; sy < y0 + h; sy++) {
      safeSet(image, x0, sy, spineShade);
    }

    if ("linkpanel".equals(kind)) {
      int ink = (tint == -1 || tint == 0) ? 0xFF101010 : (0xFF000000 | (tint & 0xFFFFFF));

      for (int sy = y0 + 1; sy < y0 + h - 1; sy++) {
        for (int sx = x0 + 1; sx < x0 + 5; sx++) {
          int alpha = (sx == x0 + 1) ? 0xFF : Math.max(0x40, 0xC0 - (sx - x0) * 30);
          int c = (ink & 0x00FFFFFF) | (alpha << 24);
          safeSet(image, sx, sy, c);
        }
      }

      if (affinity) {
        int blobX = x0 + w / 2;
        int blobY = y0 + h / 2;
        for (int dy = -2; dy <= 2; dy++) {
          for (int dx = -3; dx <= 3; dx++) {
            if (dx * dx + dy * dy <= 6) {
              safeSet(image, blobX + dx, blobY + dy, ink);
            }
          }
        }
      }
    }

    if ("symbol".equals(kind)) {
      int frame = 0xFF8C6E3A;
      drawRectOutline(image, x0 + 3, y0 + 3, w - 6, h - 6, frame);
    }

    if ("empty".equals(kind)) {
      for (int sy = y0; sy < y0 + h; sy++) {
        for (int sx = x0; sx < x0 + w; sx++) {
          int existing = readPixel(image, sx, sy);
          int faded = (existing & 0x00FFFFFF) | 0x40000000;
          setPixel(image, sx, sy, faded);
        }
      }
    }
  }

  private static void fillTransparent(NativeImage image) {
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        image.setPixelRGBA(x, y, 0);
      }
    }
  }

  private static void fillRect(NativeImage image, int x, int y, int w, int h, int argb) {
    for (int py = y; py < y + h; py++) {
      for (int px = x; px < x + w; px++) {
        safeSet(image, px, py, argb);
      }
    }
  }

  private static void drawRectOutline(NativeImage image, int x, int y, int w, int h, int argb) {
    int color = 0xFF000000 | (argb & 0xFFFFFF);
    for (int i = 0; i < w; i++) {
      safeSet(image, x + i, y, color);
      safeSet(image, x + i, y + h - 1, color);
    }
    for (int i = 0; i < h; i++) {
      safeSet(image, x, y + i, color);
      safeSet(image, x + w - 1, y + i, color);
    }
  }

  private static void safeSet(NativeImage image, int x, int y, int argb) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
      return;
    setPixel(image, x, y, argb);
  }

  private static void setPixel(NativeImage image, int x, int y, int argb) {
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
    image.setPixelRGBA(x, y, abgr);
  }

  private static int readPixel(NativeImage image, int x, int y) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
      return 0;
    int abgr = image.getPixelRGBA(x, y);
    int a = (abgr >>> 24) & 0xFF;
    int b = (abgr >>> 16) & 0xFF;
    int g = (abgr >>> 8) & 0xFF;
    int r = abgr & 0xFF;
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  private record PageInfo(String kind, int tint, boolean affinity) {
  }
}
