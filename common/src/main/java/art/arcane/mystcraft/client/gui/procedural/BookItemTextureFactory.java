package art.arcane.mystcraft.client.gui.procedural;

import art.arcane.mystcraft.client.gui.procedural.BookTextureFactory.BookKind;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.util.ItemStackNbt;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Generates the in-inventory item icon for descriptive / linking / personal
 * link books as a closed-book front-view sprite.
 * <p>
 * The icon shares its colour palette and decorative motif with
 * {@link BookTextureFactory} so that the inventory icon, the open-book GUI
 * cover, and the in-world rendering all read as the same object:
 * <ul>
 *   <li>{@link BookKind#AGEBOOK} \u2014 warm gold cover with filigree corners
 *       and a central plus-shaped emblem.</li>
 *   <li>{@link BookKind#LINKBOOK} \u2014 emerald green cover with leaf
 *       corners and a central diamond leaf emblem.</li>
 *   <li>{@link BookKind#PERSONAL_LINK} \u2014 silver-white cover with diamond
 *       corners and a central four-point star emblem.</li>
 * </ul>
 * Output is a 64x64 ARGB texture that fills the inventory cell when blitted
 * by {@link art.arcane.mystcraft.client.render.BookItemRendererBEWLR}.
 */
public final class BookItemTextureFactory {

  /**
   * Texture resolution for the item icon \u2014 4x vanilla item resolution.
   */
  public static final int TEX_SIZE = 64;

  private static final int BOOK_X = 6;
  private static final int BOOK_Y = 6;
  private static final int BOOK_W = 52;
  private static final int BOOK_H = 52;
  private static final int SPINE_W = 8;

  private static final ProceduralTextureCache CACHE = new ProceduralTextureCache("book_item", 64);
  private static final BorderPalette GOLD = new BorderPalette(
      0xFFE6C778, 0xFFFFEFAA, 0xFF8E6418, EmblemStyle.FILIGREE_CROSS);
  private static final BorderPalette GREEN = new BorderPalette(
      0xFF3FA85A, 0xFF8AE3A2, 0xFF1B5C2B, EmblemStyle.LEAF_DIAMOND);
  private static final BorderPalette SILVER = new BorderPalette(
      0xFFE6E8EE, 0xFFFAFCFF, 0xFF7B8088, EmblemStyle.FOUR_POINT_STAR);
  private static final BorderPalette MIDNIGHT = new BorderPalette(
      0xFF1E2748, 0xFFD9B26A, 0xFF0B1024, EmblemStyle.QUILL);
  private static final BorderPalette SLATE = new BorderPalette(
      0xFF40464F, 0xFFB0B6BF, 0xFF1F232A, EmblemStyle.EMPTY_DIAMOND);

  private BookItemTextureFactory() {
  }

  /**
   * Returns a {@link ResourceLocation} for the closed-book icon texture,
   * generating and caching it if not already present.
   */
  @NotNull
  public static ResourceLocation getItemTexture(@NotNull ItemStack stack) {
    BookKind kind = BookTextureFactory.detectKind(stack);
    Inputs in = collect(stack, kind);
    String key = in.cacheKey();
    return CACHE.getOrCreate(key, TEX_SIZE, TEX_SIZE, image -> render(image, in));
  }

  /**
   * Clears the cache. Call from resource pack reload.
   */
  public static void reset() {
    CACHE.clear();
  }

  /**
   * Exposes the cache so the global reload listener can register it.
   */
  static ProceduralTextureCache cache() {
    return CACHE;
  }

  private static Inputs collect(ItemStack book, BookKind kind) {
    CompoundTag tag = ItemStackNbt.getTag(book);
    ResourceLocation coverId = tag != null ? LinkOptions.getCoverItemId(tag) : null;
    int inkTint = tag != null ? LinkOptions.getInkTint(tag) : -1;
    boolean dead = tag != null && LinkOptions.isDead(tag);
    boolean linked = tag != null && LinkOptions.getDimensionUID(tag) != null;

    int pageCount = 0;
    if (book.getItem() instanceof AgebookItem agebook) {
      var pages = agebook.getPageList(book);
      if (pages != null) pageCount = pages.size();
    }

    if (!MystcraftConfig.proceduralBookCoversEnabled.get()) {
      coverId = null;
      inkTint = -1;
    }

    CoverPalette.Entry palette = CoverPalette.get(coverId);
    return new Inputs(coverId, palette, kind, linked, dead, pageCount, inkTint);
  }

  private static void render(NativeImage image, Inputs in) {
    fill(image, 0, 0, TEX_SIZE, TEX_SIZE, 0);

    int base = 0xFF000000 | in.palette.baseColor();
    int accent = 0xFF000000 | in.palette.accentColor();
    int trim = 0xFF000000 | in.palette.trimColor();

    int shadow = 0x55000000;
    fill(image, BOOK_X + 2, BOOK_Y + BOOK_H, BOOK_W, 2, shadow);
    fill(image, BOOK_X + BOOK_W, BOOK_Y + 2, 2, BOOK_H, shadow);

    int coverX = BOOK_X + SPINE_W;
    int coverW = BOOK_W - SPINE_W;
    fill(image, coverX, BOOK_Y, coverW, BOOK_H, base);

    drawIconGrain(image, coverX, BOOK_Y, coverW, BOOK_H, base, accent, in.palette.grain(), in.coverId);

    drawSpine(image, base, accent, trim);

    int pageColor = (in.inkTint == -1 || in.inkTint == 0) ? 0xFFEDE5C8 : 0xFF000000 | (in.inkTint & 0xFFFFFF);
    drawPageEdges(image, pageColor, in.pageCount);

    drawBevel(image, coverX, BOOK_Y, coverW, BOOK_H, base);

    BorderPalette p = paletteFor(in.kind);
    if (p != null) {
      drawDecoFrame(image, coverX, BOOK_Y, coverW, BOOK_H, p);
      drawEmblem(image, coverX + coverW / 2, BOOK_Y + BOOK_H / 2, p);
    }

    drawOuterFrame(image, BOOK_X, BOOK_Y, BOOK_W, BOOK_H);
    if (in.dead) {
      drawDeadSlash(image);
    }
  }

  private static BorderPalette paletteFor(BookKind kind) {
    return switch (kind) {
      case AGEBOOK -> GOLD;
      case LINKBOOK -> GREEN;
      case PERSONAL_LINK -> SILVER;
      case GUIDEBOOK -> MIDNIGHT;
      case LINKBOOK_UNLINKED -> SLATE;
      case GENERIC -> null;
    };
  }

  private static void drawDecoFrame(NativeImage image, int x, int y, int w, int h, BorderPalette p) {
    int inset = 3;

    drawRectOutline(image, x + inset, y + inset, w - inset * 2, h - inset * 2, p.base());

    drawRectOutline(image, x + inset + 1, y + inset + 1, w - inset * 2 - 2, h - inset * 2 - 2, p.lo());

    for (int i = 0; i < w - inset * 2; i++)
      safeSet(image, x + inset + i, y + inset, p.hi());
    for (int i = 0; i < h - inset * 2; i++)
      safeSet(image, x + inset, y + inset + i, p.hi());

    int x0 = x + inset + 2;
    int y0 = y + inset + 2;
    int x1 = x + w - inset - 3;
    int y1 = y + h - inset - 3;
    safeSet(image, x0, y0, p.lo());
    safeSet(image, x1, y0, p.lo());
    safeSet(image, x0, y1, p.lo());
    safeSet(image, x1, y1, p.lo());
    safeSet(image, x0 + 1, y0, p.hi());
    safeSet(image, x1 - 1, y0, p.hi());
    safeSet(image, x0 + 1, y1, p.hi());
    safeSet(image, x1 - 1, y1, p.hi());
  }

  private static void drawEmblem(NativeImage image, int cx, int cy, BorderPalette p) {
    switch (p.emblem()) {
      case FILIGREE_CROSS -> drawFiligreeCross(image, cx, cy, p);
      case LEAF_DIAMOND -> drawLeafDiamond(image, cx, cy, p);
      case FOUR_POINT_STAR -> drawFourPointStar(image, cx, cy, p);
      case QUILL -> drawQuillEmblem(image, cx, cy, p);
      case EMPTY_DIAMOND -> drawEmptyDiamondEmblem(image, cx, cy, p);
    }
  }

  private static void drawFiligreeCross(NativeImage image, int cx, int cy, BorderPalette p) {
    int lo = p.lo();
    int hi = p.hi();

    for (int dy = -5; dy <= 5; dy++) safeSet(image, cx, cy + dy, lo);

    for (int dx = -5; dx <= 5; dx++) safeSet(image, cx + dx, cy, lo);

    safeSet(image, cx - 1, cy - 5, lo);
    safeSet(image, cx + 1, cy - 5, lo);
    safeSet(image, cx - 1, cy + 5, lo);
    safeSet(image, cx + 1, cy + 5, lo);
    safeSet(image, cx - 5, cy - 1, lo);
    safeSet(image, cx - 5, cy + 1, lo);
    safeSet(image, cx + 5, cy - 1, lo);
    safeSet(image, cx + 5, cy + 1, lo);

    safeSet(image, cx, cy, hi);
    safeSet(image, cx - 1, cy, hi);
    safeSet(image, cx + 1, cy, hi);
    safeSet(image, cx, cy - 1, hi);
    safeSet(image, cx, cy + 1, hi);
  }

  private static void drawLeafDiamond(NativeImage image, int cx, int cy, BorderPalette p) {
    int lo = p.lo();
    int hi = p.hi();

    for (int dy = -5; dy <= 5; dy++) {
      int span = 5 - Math.abs(dy);
      for (int dx = -span; dx <= span; dx++) {
        safeSet(image, cx + dx, cy + dy, lo);
      }
    }

    for (int dy = -4; dy <= 4; dy++) safeSet(image, cx, cy + dy, hi);
    safeSet(image, cx + 1, cy - 2, hi);
    safeSet(image, cx + 2, cy - 1, hi);
    safeSet(image, cx + 1, cy + 2, hi);
    safeSet(image, cx + 2, cy + 1, hi);
    safeSet(image, cx - 1, cy - 2, hi);
    safeSet(image, cx - 2, cy - 1, hi);
    safeSet(image, cx - 1, cy + 2, hi);
    safeSet(image, cx - 2, cy + 1, hi);
  }

  private static void drawFourPointStar(NativeImage image, int cx, int cy, BorderPalette p) {
    int lo = p.lo();
    int hi = p.hi();

    for (int d = 0; d <= 5; d++) {
      int width = Math.max(1, 4 - d / 2);
      for (int w = -width / 2; w <= width / 2; w++) {
        safeSet(image, cx + w, cy - d, lo);
        safeSet(image, cx + w, cy + d, lo);
        safeSet(image, cx - d, cy + w, lo);
        safeSet(image, cx + d, cy + w, lo);
      }
    }

    for (int dy = -1; dy <= 1; dy++) {
      for (int dx = -1; dx <= 1; dx++) {
        safeSet(image, cx + dx, cy + dy, hi);
      }
    }
  }

  private static void drawQuillEmblem(NativeImage image, int cx, int cy, BorderPalette p) {
    int lo = p.lo();
    int hi = p.hi();

    for (int i = 0; i < 8; i++) {
      safeSet(image, cx + 4 - i, cy - 4 + i, lo);

      if (i > 0 && i < 7) safeSet(image, cx + 4 - i, cy - 3 + i, hi);
    }

    safeSet(image, cx + 5, cy - 3, lo);
    safeSet(image, cx + 4, cy - 2, lo);
    safeSet(image, cx + 3, cy - 1, lo);
    safeSet(image, cx + 5, cy - 2, hi);
    safeSet(image, cx + 4, cy - 1, hi);

    safeSet(image, cx - 3, cy + 2, lo);
    safeSet(image, cx - 4, cy + 3, lo);
    safeSet(image, cx - 4, cy + 4, lo);
    safeSet(image, cx - 5, cy + 4, lo);
    safeSet(image, cx - 3, cy + 3, hi);

    safeSet(image, cx - 6, cy + 5, hi);
    safeSet(image, cx - 6, cy + 6, lo);
    safeSet(image, cx - 5, cy + 6, lo);
  }

  private static void drawEmptyDiamondEmblem(NativeImage image, int cx, int cy, BorderPalette p) {
    int lo = p.lo();
    int hi = p.hi();

    for (int d = 0; d <= 5; d++) {
      safeSet(image, cx + d, cy - 5 + d, lo);
      safeSet(image, cx - d, cy - 5 + d, lo);
      safeSet(image, cx + d, cy + 5 - d, lo);
      safeSet(image, cx - d, cy + 5 - d, lo);
    }

    for (int d = 1; d <= 4; d++) {
      safeSet(image, cx - d, cy - 5 + d + 1, hi);
    }

    safeSet(image, cx, cy, hi);
  }

  private static void drawSpine(NativeImage image, int base, int accent, int trim) {
    int spineDark = mix(base, 0xFF000000, 0.35f);
    int spineMid = mix(base, 0xFF000000, 0.18f);
    fill(image, BOOK_X, BOOK_Y, SPINE_W, BOOK_H, spineDark);

    for (int y = BOOK_Y; y < BOOK_Y + BOOK_H; y++) {
      safeSet(image, BOOK_X + SPINE_W - 1, y, spineMid);
    }

    int hub1 = BOOK_Y + BOOK_H / 4;
    int hub2 = BOOK_Y + (BOOK_H * 3) / 4;
    for (int x = BOOK_X + 1; x < BOOK_X + SPINE_W - 1; x++) {
      safeSet(image, x, hub1, trim);
      safeSet(image, x, hub1 + 1, mix(trim, 0xFF000000, 0.4f));
      safeSet(image, x, hub2, trim);
      safeSet(image, x, hub2 + 1, mix(trim, 0xFF000000, 0.4f));
    }

    for (int x = BOOK_X + 1; x < BOOK_X + SPINE_W - 1; x++) {
      safeSet(image, x, BOOK_Y, mix(spineDark, 0xFFFFFFFF, 0.2f));
      safeSet(image, x, BOOK_Y + BOOK_H - 1, mix(spineDark, 0xFF000000, 0.5f));
    }
  }

  private static void drawPageEdges(NativeImage image, int pageColor, int pageCount) {

    int rx = BOOK_X + BOOK_W - 1;
    for (int y = BOOK_Y + 1; y < BOOK_Y + BOOK_H - 1; y++) {
      safeSet(image, rx, y, pageColor);
    }

    int by = BOOK_Y + BOOK_H - 1;
    int rowCount = Math.min(3, 1 + Math.max(0, pageCount / 8));
    for (int r = 0; r < rowCount; r++) {
      for (int x = BOOK_X + SPINE_W; x < BOOK_X + BOOK_W; x++) {
        safeSet(image, x, by - r, pageColor);
      }
      pageColor = mix(pageColor, 0xFF000000, 0.18f);
    }
  }

  private static void drawBevel(NativeImage image, int x, int y, int w, int h, int base) {
    int hi = mix(base, 0xFFFFFFFF, 0.25f);
    int lo = mix(base, 0xFF000000, 0.35f);
    for (int i = 0; i < w; i++) {
      safeSet(image, x + i, y, hi);
      safeSet(image, x + i, y + h - 1, lo);
    }
    for (int i = 0; i < h; i++) {
      safeSet(image, x + w - 1, y + i, lo);
    }
  }

  private static void drawOuterFrame(NativeImage image, int x, int y, int w, int h) {
    int outline = 0xFF1A1209;
    drawRectOutline(image, x, y, w, h, outline);
  }

  private static void drawDeadSlash(NativeImage image) {
    int red = 0xFFD92020;
    int redHi = 0xFFFF7060;

    for (int i = 0; i < BOOK_W - 4; i++) {
      int px = BOOK_X + 2 + i;
      int py = BOOK_Y + 2 + i;
      safeSet(image, px, py, red);
      safeSet(image, px + 1, py, redHi);
      safeSet(image, px, py + 1, redHi);
    }
  }

  private static void drawIconGrain(NativeImage image, int x, int y, int w, int h,
                                    int base, int accent, CoverPalette.Grain grain, ResourceLocation seedSrc) {
    long seed = (seedSrc == null ? 0xCAFEBABEL : seedSrc.toString().hashCode()) | 1L;
    switch (grain) {
      case LEATHER -> {
        for (int py = 0; py < h; py++) {
          for (int px = 0; px < w; px++) {
            seed = seed * 2862933555777941757L + 3037000493L;
            int r = (int) (seed >>> 33) & 0xFF;
            if (r < 8) safeSet(image, x + px, y + py, mix(base, accent, 0.35f));
            else if (r > 248)
              safeSet(image, x + px, y + py, mix(base, accent, -0.18f));
          }
        }
      }
      case WOOD -> {
        for (int px = 0; px < w; px++) {
          seed = seed * 6364136223846793005L + 1442695040888963407L;
          if (((seed >>> 40) & 7) == 0) {
            int color = mix(base, accent, 0.18f);
            for (int py = 0; py < h; py++)
              safeSet(image, x + px, y + py, color);
          }
        }
      }
      case METAL -> {
        int sheen = mix(base, 0xFFFFFFFF, 0.3f);
        for (int py = 0; py < h; py++) {
          int diag = (py + (int) (seed & 0xF)) % 16;
          if (diag < 2) {
            for (int px = 0; px < w; px++)
              safeSet(image, x + px, y + py, sheen);
          }
        }
      }
      case STONE, CRYSTAL -> {
        for (int i = 0; i < (w * h) / 24; i++) {
          seed = seed * 6364136223846793005L + 1442695040888963407L;
          int px = (int) ((seed >>> 32) & 0x7FFFFFFF) % w;
          int py = (int) ((seed >>> 16) & 0x7FFFFFFF) % h;
          safeSet(image, x + px, y + py, mix(base, accent, 0.4f));
        }
      }
      case PAPER -> {
        int faded = mix(base, accent, 0.25f);
        for (int ly = 4; ly < h - 4; ly += 6) {
          for (int lx = 3; lx < w - 3; lx++)
            safeSet(image, x + lx, y + ly, faded);
        }
      }
      case FLAT -> {

      }
    }
  }

  private static void fill(NativeImage image, int x, int y, int w, int h, int argb) {
    for (int py = y; py < y + h; py++) {
      for (int px = x; px < x + w; px++) {
        safeSet(image, px, py, argb);
      }
    }
  }

  private static void safeSet(NativeImage image, int x, int y, int argb) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
      return;
    int a = (argb >>> 24) & 0xFF;
    int r = (argb >>> 16) & 0xFF;
    int g = (argb >>> 8) & 0xFF;
    int b = argb & 0xFF;
    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
    image.setPixelRGBA(x, y, abgr);
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

  private static int mix(int a, int b, float t) {
    int aa = (a >>> 24) & 0xFF;
    int ar = (a >>> 16) & 0xFF;
    int ag = (a >>> 8) & 0xFF;
    int ab = a & 0xFF;
    int ba = (b >>> 24) & 0xFF;
    int br = (b >>> 16) & 0xFF;
    int bg = (b >>> 8) & 0xFF;
    int bb = b & 0xFF;
    if (t < 0) {
      float k = Math.min(1f, -t);
      int r = clamp((int) (ar * (1 - k)));
      int g = clamp((int) (ag * (1 - k)));
      int bl = clamp((int) (ab * (1 - k)));
      return (aa << 24) | (r << 16) | (g << 8) | bl;
    }
    float k = Math.min(1f, t);
    int rr = clamp((int) (ar + (br - ar) * k));
    int gg = clamp((int) (ag + (bg - ag) * k));
    int bb2 = clamp((int) (ab + (bb - ab) * k));
    int aa2 = clamp((int) (aa + (ba - aa) * k));
    return (aa2 << 24) | (rr << 16) | (gg << 8) | bb2;
  }

  private static int clamp(int v) {
    return v < 0 ? 0 : Math.min(255, v);
  }

  private enum EmblemStyle {
    FILIGREE_CROSS,
    LEAF_DIAMOND,
    FOUR_POINT_STAR,
    /**
     * Quill nib + ink dot — the Art-of-Writing tutorial.
     */
    QUILL,
    /**
     * Empty diamond outline — the unwritten linkbook.
     */
    EMPTY_DIAMOND
  }

  private record Inputs(
      ResourceLocation coverId,
      CoverPalette.Entry palette,
      BookKind kind,
      boolean linked,
      boolean dead,
      int pageCount,
      int inkTint
  ) {
    private static char kindTag(BookKind kind) {
      return switch (kind) {
        case AGEBOOK -> 'a';
        case LINKBOOK -> 'l';
        case PERSONAL_LINK -> 'p';
        case GUIDEBOOK -> 'G';
        case LINKBOOK_UNLINKED -> 'U';
        case GENERIC -> 'g';
      };
    }

    String cacheKey() {
      StringBuilder sb = new StringBuilder(48);
      sb.append(coverId == null ? "default" : coverId);
      sb.append('|').append(kindTag(kind));
      sb.append(linked ? 'L' : 'u');
      if (dead) sb.append('d');
      sb.append('|').append(Math.min(pageCount, 99));
      sb.append('|').append(Integer.toHexString(inkTint & 0xFFFFFF));
      return sb.toString();
    }
  }

  private record BorderPalette(int base, int hi, int lo, EmblemStyle emblem) {
  }
}
