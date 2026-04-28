package art.arcane.mystcraft.client.gui.procedural;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.util.ItemStackNbt;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generates the open-book backing texture for {@link art.arcane.mystcraft.client.screen.BookScreen}
 * as a function of the book's contents.
 * <p>
 * Inputs that influence the visual:
 * <ul>
 *   <li>Cover material (from {@link LinkOptions#getCoverItemId(CompoundTag)})</li>
 *   <li>Page count (from {@link AgebookItem#getPageList(ItemStack)})</li>
 *   <li>Blended ink tint (from {@link LinkOptions#getInkTint(CompoundTag)} or the title-page link panel)</li>
 *   <li>Link properties on the first page (sigils overlaid on the cover)</li>
 *   <li>Whether the book is an Agebook (gold borders), Linkbook, or unwritten (neutral)</li>
 *   <li>Authors (decorative crest seed)</li>
 * </ul>
 * Output is one 256x256 ARGB texture matching the legacy {@code bookui_cover.png}
 * sub-region layout so the existing {@link art.arcane.mystcraft.client.screen.BookScreen}
 * blit math is preserved.
 */
public final class BookTextureFactory {

  /** Texture dimensions match the legacy {@code bookui_cover.png}. */
  public static final int TEX_WIDTH = 256;
  public static final int TEX_HEIGHT = 256;

  /** Open book interior dimensions inside the texture. */
  private static final int BOOK_X = 0;
  private static final int BOOK_Y = 0;
  private static final int BOOK_W = 220;
  private static final int BOOK_H = 192;

  private static final ProceduralTextureCache CACHE = new ProceduralTextureCache("book_cover", 64);

  private BookTextureFactory() {
  }

  /**
   * Returns a {@link ResourceLocation} for the open book texture, generating
   * and caching it if not already present.
   */
  @NotNull
  public static ResourceLocation getCoverTexture(@NotNull ItemStack book, boolean isAgebook) {
    Inputs collected = collect(book, isAgebook);
    // When procedural covers are disabled, force a neutral palette so every
    // book renders identically (parchment + neutral ink). Page count and
    // link-panel sigils are still allowed to vary so the book remains
    // legible — only the *cover* customisation is suppressed.
    final Inputs in = MystcraftConfig.proceduralBookCoversEnabled.get()
        ? collected
        : collected.withNeutralCover();
    String key = in.cacheKey();
    return CACHE.getOrCreate(key, TEX_WIDTH, TEX_HEIGHT, image -> render(image, in));
  }

  /** Clears the cache. Call from resource pack reload. */
  public static void reset() {
    CACHE.clear();
  }

  // -------------------------------------------------------------------------
  // Inputs
  // -------------------------------------------------------------------------

  private static Inputs collect(ItemStack book, boolean isAgebook) {
    CompoundTag tag = ItemStackNbt.getTag(book);

    ResourceLocation coverId = tag != null ? LinkOptions.getCoverItemId(tag) : null;
    int linkInkTint = tag != null ? LinkOptions.getInkTint(tag) : -1;

    List<ItemStack> pages = (book.getItem() instanceof AgebookItem agebook)
        ? agebook.getPageList(book)
        : List.of();

    int pageCount = pages.size();

    // Pull ink tint preferentially from the book's LinkOptions, fall back to
    // the link panel page's tint if available.
    int inkTint = linkInkTint;
    Collection<String> linkProperties = List.of();
    for (ItemStack page : pages) {
      if (Page.isLinkPanel(page)) {
        if (inkTint == -1 || inkTint == 0) {
          int pt = Page.getInkTint(page);
          if (pt != -1 && pt != 0) inkTint = pt;
        }
        linkProperties = new ArrayList<>(Page.getLinkProperties(page));
        break;
      }
    }

    boolean linked = tag != null && LinkOptions.getDimensionUID(tag) != null;
    boolean dead = tag != null && LinkOptions.isDead(tag);

    int authorsHash = 0;
    if (book.getItem() instanceof AgebookItem agebook) {
      Collection<String> authors = agebook.getAuthors(book);
      if (authors != null) {
        for (String a : authors) authorsHash = authorsHash * 31 + (a == null ? 0 : a.hashCode());
      }
    }

    CoverPalette.Entry palette = CoverPalette.get(coverId);

    return new Inputs(coverId, palette, isAgebook, linked, dead, pageCount,
        inkTint, List.copyOf(linkProperties), authorsHash);
  }

  /**
   * Bundle of all inputs that influence the cover texture. The cache key
   * combines all of these so that two visually identical books share a
   * single GPU texture.
   */
  private record Inputs(
      ResourceLocation coverId,
      CoverPalette.Entry palette,
      boolean isAgebook,
      boolean linked,
      boolean dead,
      int pageCount,
      int inkTint,
      List<String> linkProperties,
      int authorsHash
  ) {

    String cacheKey() {
      StringBuilder sb = new StringBuilder(96);
      sb.append(coverId == null ? "default" : coverId);
      sb.append('|').append(isAgebook ? 'a' : 'l');
      sb.append(linked ? 'L' : 'u');
      if (dead) sb.append('d');
      sb.append('|').append(Math.min(pageCount, 99));
      sb.append('|').append(Integer.toHexString(inkTint & 0xFFFFFF));
      sb.append('|');
      for (String p : linkProperties) sb.append(p, 0, Math.min(p.length(), 3));
      sb.append('|').append(Integer.toHexString(authorsHash));
      return sb.toString();
    }

    /**
     * Returns an {@code Inputs} with the cover material forced to the
     * default neutral palette and the ink tint cleared. Used when the
     * {@link MystcraftConfig#proceduralBookCoversEnabled} toggle is off so
     * every book renders with the same neutral cover regardless of the
     * binding material or ink that was used.
     */
    Inputs withNeutralCover() {
      return new Inputs(null, CoverPalette.get(null), isAgebook, linked, dead,
          pageCount, -1, linkProperties, 0);
    }
  }

  // -------------------------------------------------------------------------
  // Rendering
  // -------------------------------------------------------------------------

  private static void render(NativeImage image, Inputs in) {
    fill(image, 0, 0, TEX_WIDTH, TEX_HEIGHT, 0); // transparent background

    // Layer 0: base cover panel (covers the whole open book area).
    int base = 0xFF000000 | in.palette.baseColor();
    int accent = 0xFF000000 | in.palette.accentColor();
    int trim = 0xFF000000 | in.palette.trimColor();

    fill(image, BOOK_X, BOOK_Y, BOOK_W, BOOK_H, base);

    // Layer 1: grain pattern based on cover material
    drawGrain(image, BOOK_X, BOOK_Y, BOOK_W, BOOK_H, base, accent, in.palette.grain(), in.authorsHash);

    // Layer 2: spine shadow at center
    drawSpineShadow(image, base);

    // Layer 3: outer bevel
    drawBevel(image, BOOK_X, BOOK_Y, BOOK_W, BOOK_H, accent, base);

    // Layer 4: ink-stained edges (page edge stack on left + right)
    int inkColor = (in.inkTint == -1 || in.inkTint == 0) ? trim : (0xFF000000 | (in.inkTint & 0xFFFFFF));
    drawPageEdges(image, in.pageCount, inkColor);

    // Layer 5: gold border for Agebooks (drawn into the legacy
    // {186,0}-{220,192} sub-region so existing blit math at line 212-213 of
    // BookScreen still picks it up).
    if (in.isAgebook) {
      drawGoldBorder(image);
    }

    // Layer 6: link-property sigils on the front cover (visible under the
    // gold border for agebooks; tucked into the left page panel for linkbooks)
    drawSigils(image, in.linkProperties);

    // Layer 7: optional dead-link slash
    if (in.dead) {
      drawDeadLinkMark(image);
    }
  }

  // ---------------- grain ---------------------------------------------------

  private static void drawGrain(NativeImage image, int x, int y, int w, int h,
                                int base, int accent, CoverPalette.Grain grain, int seed) {
    switch (grain) {
      case LEATHER -> drawLeather(image, x, y, w, h, base, accent, seed);
      case WOOD -> drawWood(image, x, y, w, h, base, accent, seed);
      case PAPER -> drawPaper(image, x, y, w, h, accent);
      case STONE -> drawStone(image, x, y, w, h, base, accent, seed);
      case METAL -> drawMetal(image, x, y, w, h, base, accent);
      case CRYSTAL -> drawCrystal(image, x, y, w, h, base, accent, seed);
      case FLAT -> {
        // no overlay
      }
    }
  }

  private static void drawLeather(NativeImage image, int x, int y, int w, int h, int base, int accent, int seed) {
    // Subtle pore stippling
    long s = seed | 1L;
    for (int py = 0; py < h; py++) {
      for (int px = 0; px < w; px++) {
        s = (s * 2862933555777941757L + 3037000493L);
        int r = (int) (s >>> 33) & 0xFF;
        if (r < 20) {
          int mix = mix(base, accent, 0.3f);
          safeSet(image, x + px, y + py, mix);
        } else if (r > 235) {
          int mix = mix(base, accent, -0.15f);
          safeSet(image, x + px, y + py, mix);
        }
      }
    }
    // Inner border crease
    drawRectOutline(image, x + 8, y + 6, w - 16, h - 12, mix(base, accent, 0.4f));
  }

  private static void drawWood(NativeImage image, int x, int y, int w, int h, int base, int accent, int seed) {
    // Vertical streaks with sinusoidal warble
    long s = seed | 1L;
    for (int px = 0; px < w; px++) {
      s = (s * 6364136223846793005L + 1442695040888963407L);
      int streak = (int) ((s >>> 40) % 6);
      if (streak == 0) {
        int strength = ((int) (s >>> 24)) & 0x3F;
        int color = mix(base, accent, 0.15f + strength / 512f);
        for (int py = 0; py < h; py++) {
          int wob = (int) (Math.sin((py + (seed & 0xFF)) * 0.05) * 1.5);
          safeSet(image, x + px + wob, y + py, color);
        }
      }
    }
    // Vertical knots
    for (int k = 0; k < 3; k++) {
      int kx = (int) ((s >>> (8 * k)) & 0xFF) % (w - 12) + 6;
      int ky = (int) ((s >>> (8 * k + 4)) & 0xFF) % (h - 12) + 6;
      int knotColor = mix(base, accent, 0.5f);
      drawCircle(image, x + kx, y + ky, 3, knotColor);
      drawCircle(image, x + kx, y + ky, 2, mix(base, accent, 0.7f));
    }
  }

  private static void drawPaper(NativeImage image, int x, int y, int w, int h, int line) {
    int faded = (line & 0x00FFFFFF) | 0x40000000;
    for (int ly = y + 12; ly < y + h - 12; ly += 8) {
      for (int lx = x + 8; lx < x + w - 8; lx++) {
        safeSet(image, lx, ly, faded);
      }
    }
  }

  private static void drawStone(NativeImage image, int x, int y, int w, int h, int base, int accent, int seed) {
    long s = seed | 1L;
    for (int py = 0; py < h; py++) {
      for (int px = 0; px < w; px++) {
        s = (s * 2862933555777941757L + 3037000493L);
        int r = (int) (s >>> 33) & 0xFF;
        if (r < 25) {
          safeSet(image, x + px, y + py, accent);
        } else if (r > 230) {
          safeSet(image, x + px, y + py, mix(base, accent, -0.3f));
        }
      }
    }
  }

  private static void drawMetal(NativeImage image, int x, int y, int w, int h, int base, int accent) {
    // Diagonal sheen band
    int sheen = mix(base, 0xFFFFFFFF, 0.4f);
    int shade = mix(base, accent, 0.25f);
    for (int py = 0; py < h; py++) {
      for (int px = 0; px < w; px++) {
        int diag = (px + py) % 24;
        if (diag < 4) {
          safeSet(image, x + px, y + py, sheen);
        } else if (diag > 18) {
          safeSet(image, x + px, y + py, shade);
        }
      }
    }
  }

  private static void drawCrystal(NativeImage image, int x, int y, int w, int h, int base, int accent, int seed) {
    long s = (seed | 1L) ^ 0xFEEDFACECAFEBABEL;
    int facets = 6;
    for (int f = 0; f < facets; f++) {
      s = (s * 6364136223846793005L + 1442695040888963407L);
      int fx = (int) (s >>> 32) & 0xFFFF;
      int fy = (int) (s & 0xFFFFFFFFL) & 0xFFFF;
      int cx = x + 12 + (fx % (w - 24));
      int cy = y + 12 + (fy % (h - 24));
      int radius = 8 + (fx & 0xF);
      int color = mix(base, accent, 0.4f);
      drawCircle(image, cx, cy, radius, color);
      drawCircle(image, cx, cy, radius - 2, mix(base, 0xFFFFFFFF, 0.25f));
    }
  }

  // ---------------- structural overlays ------------------------------------

  private static void drawSpineShadow(NativeImage image, int base) {
    // Center vertical strip is darker; the legacy texture has the spine at the
    // {141..186} sub-region. We darken that band so the BookScreen.blit at
    // line 208 lands on a pre-shaded pixel.
    int shadow = mix(base, 0xFF000000, 0.45f);
    for (int x = 137; x < 145; x++) {
      for (int y = 7; y < BOOK_H - 5; y++) {
        safeSet(image, x, y, shadow);
      }
    }
    // Soft falloff
    int soft = mix(base, 0xFF000000, 0.20f);
    for (int x = 145; x < 152; x++) {
      for (int y = 7; y < BOOK_H - 5; y++) {
        safeSet(image, x, y, soft);
      }
    }
    for (int x = 132; x < 137; x++) {
      for (int y = 7; y < BOOK_H - 5; y++) {
        safeSet(image, x, y, soft);
      }
    }
  }

  private static void drawBevel(NativeImage image, int x, int y, int w, int h, int accent, int base) {
    int hi = mix(base, 0xFFFFFFFF, 0.25f);
    int lo = mix(base, 0xFF000000, 0.45f);
    // Top + left highlight, bottom + right shadow (1 px each)
    for (int i = 0; i < w; i++) {
      safeSet(image, x + i, y, hi);
      safeSet(image, x + i, y + h - 1, lo);
    }
    for (int i = 0; i < h; i++) {
      safeSet(image, x, y + i, hi);
      safeSet(image, x + w - 1, y + i, lo);
    }
    // Inner darker frame to read as "cover lip"
    drawRectOutline(image, x + 3, y + 3, w - 6, h - 6, mix(base, accent, 0.5f));
  }

  private static void drawPageEdges(NativeImage image, int pageCount, int edgeColor) {
    int visible = Math.min(pageCount, 30);
    int top = 12;
    int bottom = BOOK_H - 12;
    // Left page edge stack
    for (int i = 0; i < visible; i++) {
      int x = 4 + i / 4;
      int alpha = Math.max(0x40, 0xFF - i * 6);
      int c = (edgeColor & 0x00FFFFFF) | (alpha << 24);
      for (int y = top + (i % 3); y < bottom - (i % 3); y++) {
        safeSet(image, x, y, c);
      }
    }
    // Right page edge stack (mirror)
    for (int i = 0; i < visible; i++) {
      int x = (BOOK_W - 5) - i / 4;
      int alpha = Math.max(0x40, 0xFF - i * 6);
      int c = (edgeColor & 0x00FFFFFF) | (alpha << 24);
      for (int y = top + (i % 3); y < bottom - (i % 3); y++) {
        safeSet(image, x, y, c);
      }
    }
  }

  /**
   * Draws a gold border into the legacy {186,0}-{220,192} sub-region so
   * {@code BookScreen.render} lines 212-213 still pick it up via
   * {@code blit(BOOK_COVER, ..., 186, 0, 34, 192, ...)}.
   */
  private static void drawGoldBorder(NativeImage image) {
    int x0 = 186;
    int y0 = 0;
    int w = 34;
    int h = 192;

    int gold = 0xFFE6C778;
    int goldHi = 0xFFFFEFAA;
    int goldLo = 0xFF8E6418;

    // Solid background so it stamps cleanly over the underlying cover
    fill(image, x0, y0, w, h, gold);

    // Inner darker rim
    drawRectOutline(image, x0 + 1, y0 + 1, w - 2, h - 2, goldLo);
    drawRectOutline(image, x0 + 3, y0 + 3, w - 6, h - 6, goldHi);

    // Decorative corner flourish (small + signs)
    drawCorner(image, x0 + 6, y0 + 6, goldLo);
    drawCorner(image, x0 + w - 11, y0 + 6, goldLo);
    drawCorner(image, x0 + 6, y0 + h - 11, goldLo);
    drawCorner(image, x0 + w - 11, y0 + h - 11, goldLo);

    // Vertical filigree band centered
    int cx = x0 + w / 2;
    for (int y = y0 + 12; y < y0 + h - 12; y += 8) {
      safeSet(image, cx, y, goldLo);
      safeSet(image, cx - 1, y + 1, goldLo);
      safeSet(image, cx + 1, y + 1, goldLo);
    }
  }

  private static void drawSigils(NativeImage image, Collection<String> properties) {
    if (properties == null || properties.isEmpty()) return;
    int x = 60;
    int y = 165;
    for (String prop : properties) {
      int color = InkEffects.getPropertyColorRGB(prop);
      int sigilColor = 0xFF000000 | (color & 0xFFFFFF);
      drawDiamond(image, x, y, 4, sigilColor);
      x += 14;
      if (x > BOOK_W - 20) break;
    }
  }

  private static void drawDeadLinkMark(NativeImage image) {
    int red = 0xFFD92020;
    // Diagonal slash across left page area
    for (int i = 0; i < 80; i++) {
      safeSet(image, 40 + i, 40 + i, red);
      safeSet(image, 40 + i, 41 + i, red);
    }
  }

  // ---------------- low-level helpers --------------------------------------

  private static void fill(NativeImage image, int x, int y, int w, int h, int argb) {
    for (int py = y; py < y + h; py++) {
      for (int px = x; px < x + w; px++) {
        safeSet(image, px, py, argb);
      }
    }
  }

  private static void safeSet(NativeImage image, int x, int y, int argb) {
    if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
    // NativeImage stores ABGR.
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

  private static void drawCircle(NativeImage image, int cx, int cy, int r, int argb) {
    int color = 0xFF000000 | (argb & 0xFFFFFF);
    for (int dy = -r; dy <= r; dy++) {
      for (int dx = -r; dx <= r; dx++) {
        if (dx * dx + dy * dy <= r * r) {
          safeSet(image, cx + dx, cy + dy, color);
        }
      }
    }
  }

  private static void drawDiamond(NativeImage image, int cx, int cy, int r, int argb) {
    int color = 0xFF000000 | (argb & 0xFFFFFF);
    for (int dy = -r; dy <= r; dy++) {
      int span = r - Math.abs(dy);
      for (int dx = -span; dx <= span; dx++) {
        safeSet(image, cx + dx, cy + dy, color);
      }
    }
  }

  private static void drawCorner(NativeImage image, int x, int y, int argb) {
    int color = 0xFF000000 | (argb & 0xFFFFFF);
    safeSet(image, x + 2, y, color);
    safeSet(image, x + 2, y + 1, color);
    safeSet(image, x + 2, y + 2, color);
    safeSet(image, x + 2, y + 3, color);
    safeSet(image, x + 2, y + 4, color);
    safeSet(image, x, y + 2, color);
    safeSet(image, x + 1, y + 2, color);
    safeSet(image, x + 3, y + 2, color);
    safeSet(image, x + 4, y + 2, color);
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
      // Negative t darkens by |t|
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
}
