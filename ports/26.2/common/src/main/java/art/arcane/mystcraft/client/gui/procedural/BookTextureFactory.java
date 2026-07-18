package art.arcane.mystcraft.client.gui.procedural;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.InkEffects;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.item.*;
import art.arcane.mystcraft.util.ItemStackNbt;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generates the open-book backing texture for
 * {@link art.arcane.mystcraft.client.screen.BookScreen} as a function of the
 * book's contents.
 * <p>
 * Inputs that influence the visual:
 * <ul>
 *   <li>Cover material (from {@link LinkOptions#getCoverItemId(CompoundTag)})</li>
 *   <li>Page count (from {@link AgebookItem#getPageList(ItemStack)})</li>
 *   <li>Blended ink tint (from {@link LinkOptions#getInkTint(CompoundTag)} or the title-page link panel)</li>
 *   <li>Link properties on the first page (sigils overlaid on the cover)</li>
 *   <li>{@link BookKind} — Agebook (gold filigree), Linkbook (green vines),
 *       Personal Link Book (silver stars), or generic (no decorative border)</li>
 *   <li>Authors (decorative crest seed)</li>
 * </ul>
 * Output is one 256x256 ARGB texture matching the legacy {@code bookui_cover.png}
 * sub-region layout so the existing {@link art.arcane.mystcraft.client.screen.BookScreen}
 * blit math is preserved.
 */
public final class BookTextureFactory {

  /**
   * Texture dimensions match the legacy {@code bookui_cover.png}.
   */
  public static final int TEX_WIDTH = 256;
  public static final int TEX_HEIGHT = 256;
  private static final int BOOK_X = 0;
  private static final int BOOK_Y = 0;
  private static final int BOOK_W = 220;
  private static final int BOOK_H = 192;
  private static final ProceduralTextureCache CACHE = new ProceduralTextureCache("book_cover", 64);
  private static final BorderPalette GOLD_PALETTE =
      new BorderPalette(0xFFE6C778, 0xFFFFEFAA, 0xFF8E6418, BorderStyle.FILIGREE);
  private static final BorderPalette GREEN_PALETTE =
      new BorderPalette(0xFF3FA85A, 0xFF8AE3A2, 0xFF1B5C2B, BorderStyle.VINES);
  private static final BorderPalette SILVER_PALETTE =
      new BorderPalette(0xFFE6E8EE, 0xFFFAFCFF, 0xFF7B8088, BorderStyle.STARS);
  private static final BorderPalette MIDNIGHT_PALETTE =
      new BorderPalette(0xFF1E2748, 0xFFD9B26A, 0xFF0B1024, BorderStyle.SCROLL);
  private static final BorderPalette SLATE_PALETTE =
      new BorderPalette(0xFF40464F, 0xFFB0B6BF, 0xFF1F232A, BorderStyle.HEX_PIPS);

  private BookTextureFactory() {
  }

  /**
   * Returns a {@link Identifier} for the open book texture, generating
   * and caching it if not already present.
   * <p>
   * Convenience overload that infers the {@link BookKind} from the item stack —
   * Agebook detection is forced via {@code isAgebook} for callers that already
   * computed it (kept for backward compatibility).
   */
  @NotNull
  public static Identifier getCoverTexture(@NotNull ItemStack book, boolean isAgebook) {
    return getCoverTexture(book, isAgebook ? BookKind.AGEBOOK : detectKind(book));
  }

  /**
   * Returns a {@link Identifier} for the open book texture, generating
   * and caching it if not already present.
   *
   * @param kind The decorative border treatment to apply. See
   *             {@link BookKind}.
   */
  @NotNull
  public static Identifier getCoverTexture(@NotNull ItemStack book, @NotNull BookKind kind) {
    Inputs collected = collect(book, kind);

    final Inputs in = MystcraftConfig.proceduralBookCoversEnabled.get()
        ? collected
        : collected.withNeutralCover();
    String key = in.cacheKey();
    return CACHE.getOrCreate(key, TEX_WIDTH, TEX_HEIGHT, image -> render(image, in));
  }

  /**
   * Detects the {@link BookKind} from the item type. Personal link books extend
   * {@link LinkbookItem}, so the {@code instanceof} order matters.
   */
  @NotNull
  public static BookKind detectKind(@NotNull ItemStack book) {
    if (book.getItem() instanceof AgebookItem) return BookKind.AGEBOOK;
    if (book.getItem() instanceof PersonalLinkBookItem)
      return BookKind.PERSONAL_LINK;
    if (book.getItem() instanceof LinkbookItem) return BookKind.LINKBOOK;
    if (book.getItem() instanceof LinkbookUnlinkedItem)
      return BookKind.LINKBOOK_UNLINKED;
    if (book.getItem() instanceof GuidebookItem) return BookKind.GUIDEBOOK;
    return BookKind.GENERIC;
  }

  /**
   * Clears the cache. Call from resource pack reload.
   */
  public static void reset() {
    CACHE.clear();
  }

  private static Inputs collect(ItemStack book, BookKind kind) {
    CompoundTag tag = ItemStackNbt.getTag(book);

    Identifier coverId = tag != null ? LinkOptions.getCoverItemId(tag) : null;
    int linkInkTint = tag != null ? LinkOptions.getInkTint(tag) : -1;

    List<ItemStack> pages = (book.getItem() instanceof AgebookItem agebook)
        ? agebook.getPageList(book)
        : List.of();

    int pageCount = pages.size();

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
        for (String a : authors)
          authorsHash = authorsHash * 31 + (a == null ? 0 : a.hashCode());
      }
    } else if (book.getItem() instanceof LinkbookItem) {

      String name = tag != null ? LinkOptions.getDisplayName(tag) : null;
      if (name != null) authorsHash = name.hashCode();
    }

    CoverPalette.Entry palette = CoverPalette.get(coverId);

    return new Inputs(coverId, palette, kind, linked, dead, pageCount,
        inkTint, List.copyOf(linkProperties), authorsHash);
  }

  private static void render(NativeImage image, Inputs in) {
    fill(image, 0, 0, TEX_WIDTH, TEX_HEIGHT, 0);

    int base = 0xFF000000 | in.palette.baseColor();
    int accent = 0xFF000000 | in.palette.accentColor();
    int trim = 0xFF000000 | in.palette.trimColor();

    fill(image, BOOK_X, BOOK_Y, BOOK_W, BOOK_H, base);

    drawGrain(image, BOOK_X, BOOK_Y, BOOK_W, BOOK_H, base, accent, in.palette.grain(), in.authorsHash);

    drawSpineShadow(image, base);

    drawBevel(image, BOOK_X, BOOK_Y, BOOK_W, BOOK_H, accent, base);

    int inkColor = (in.inkTint == -1 || in.inkTint == 0) ? trim : (0xFF000000 | (in.inkTint & 0xFFFFFF));
    drawPageEdges(image, in.pageCount, inkColor);

    BorderPalette borderPalette = borderPaletteFor(in.kind);
    if (borderPalette != null) {
      drawDecoBorder(image, borderPalette);
    }

    drawSigils(image, in.linkProperties);

    if (in.dead) {
      drawDeadLinkMark(image);
    }
  }

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

      }
    }
  }

  private static void drawLeather(NativeImage image, int x, int y, int w, int h, int base, int accent, int seed) {

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

    drawRectOutline(image, x + 8, y + 6, w - 16, h - 12, mix(base, accent, 0.4f));
  }

  private static void drawWood(NativeImage image, int x, int y, int w, int h, int base, int accent, int seed) {

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

  private static void drawSpineShadow(NativeImage image, int base) {

    int shadow = mix(base, 0xFF000000, 0.45f);
    for (int x = 137; x < 145; x++) {
      for (int y = 7; y < BOOK_H - 5; y++) {
        safeSet(image, x, y, shadow);
      }
    }

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

    for (int i = 0; i < w; i++) {
      safeSet(image, x + i, y, hi);
      safeSet(image, x + i, y + h - 1, lo);
    }
    for (int i = 0; i < h; i++) {
      safeSet(image, x, y + i, hi);
      safeSet(image, x + w - 1, y + i, lo);
    }

    drawRectOutline(image, x + 3, y + 3, w - 6, h - 6, mix(base, accent, 0.5f));
  }

  private static void drawPageEdges(NativeImage image, int pageCount, int edgeColor) {
    int visible = Math.min(pageCount, 30);
    int top = 12;
    int bottom = BOOK_H - 12;

    for (int i = 0; i < visible; i++) {
      int x = 4 + i / 4;
      int alpha = Math.max(0x40, 0xFF - i * 6);
      int c = (edgeColor & 0x00FFFFFF) | (alpha << 24);
      for (int y = top + (i % 3); y < bottom - (i % 3); y++) {
        safeSet(image, x, y, c);
      }
    }

    for (int i = 0; i < visible; i++) {
      int x = (BOOK_W - 5) - i / 4;
      int alpha = Math.max(0x40, 0xFF - i * 6);
      int c = (edgeColor & 0x00FFFFFF) | (alpha << 24);
      for (int y = top + (i % 3); y < bottom - (i % 3); y++) {
        safeSet(image, x, y, c);
      }
    }
  }

  private static BorderPalette borderPaletteFor(BookKind kind) {
    return switch (kind) {
      case AGEBOOK -> GOLD_PALETTE;
      case LINKBOOK -> GREEN_PALETTE;
      case PERSONAL_LINK -> SILVER_PALETTE;
      case GUIDEBOOK -> MIDNIGHT_PALETTE;
      case LINKBOOK_UNLINKED -> SLATE_PALETTE;
      case GENERIC -> null;
    };
  }

  private static void drawDecoBorder(NativeImage image, BorderPalette p) {
    int x0 = 186;
    int y0 = 0;
    int w = 34;
    int h = 192;

    int base = p.base();
    int hi = p.hi();
    int lo = p.lo();

    fill(image, x0, y0, w, h, base);

    drawRectOutline(image, x0 + 1, y0 + 1, w - 2, h - 2, lo);
    drawRectOutline(image, x0 + 3, y0 + 3, w - 6, h - 6, hi);

    switch (p.style()) {
      case FILIGREE -> drawFiligreeOrnaments(image, x0, y0, w, h, lo);
      case VINES -> drawVineOrnaments(image, x0, y0, w, h, lo, hi);
      case STARS -> drawStarOrnaments(image, x0, y0, w, h, lo, hi);
      case SCROLL -> drawScrollOrnaments(image, x0, y0, w, h, lo, hi);
      case HEX_PIPS -> drawHexPipsOrnaments(image, x0, y0, w, h, lo, hi);
    }
  }

  private static void drawFiligreeOrnaments(NativeImage image, int x0, int y0, int w, int h, int lo) {
    drawCorner(image, x0 + 6, y0 + 6, lo);
    drawCorner(image, x0 + w - 11, y0 + 6, lo);
    drawCorner(image, x0 + 6, y0 + h - 11, lo);
    drawCorner(image, x0 + w - 11, y0 + h - 11, lo);

    int cx = x0 + w / 2;
    for (int y = y0 + 12; y < y0 + h - 12; y += 8) {
      safeSet(image, cx, y, lo);
      safeSet(image, cx - 1, y + 1, lo);
      safeSet(image, cx + 1, y + 1, lo);
    }
  }

  private static void drawVineOrnaments(NativeImage image, int x0, int y0, int w, int h, int lo, int hi) {

    drawLeafCorner(image, x0 + 5, y0 + 5, false, false, lo, hi);
    drawLeafCorner(image, x0 + w - 6, y0 + 5, true, false, lo, hi);
    drawLeafCorner(image, x0 + 5, y0 + h - 6, false, true, lo, hi);
    drawLeafCorner(image, x0 + w - 6, y0 + h - 6, true, true, lo, hi);

    int cx = x0 + w / 2;
    int top = y0 + 14;
    int bottom = y0 + h - 14;
    for (int y = top; y < bottom; y++) {
      double t = (y - top) * 0.18;
      int xOff = (int) Math.round(Math.sin(t) * 2.2);
      safeSet(image, cx + xOff, y, lo);

      safeSet(image, cx + xOff + (int) Math.signum(-Math.cos(t)), y, mix(lo, hi, 0.35f));
    }

    for (int y = top + 4; y < bottom - 4; y += 16) {
      double t = (y - top) * 0.18;
      int xOff = (int) Math.round(Math.sin(t) * 2.2);
      drawDiamond(image, cx + xOff, y, 2, hi);
      safeSet(image, cx + xOff, y, lo);
    }
  }

  private static void drawStarOrnaments(NativeImage image, int x0, int y0, int w, int h, int lo, int hi) {

    drawDiamond(image, x0 + 7, y0 + 7, 2, lo);
    drawDiamond(image, x0 + w - 8, y0 + 7, 2, lo);
    drawDiamond(image, x0 + 7, y0 + h - 8, 2, lo);
    drawDiamond(image, x0 + w - 8, y0 + h - 8, 2, lo);

    safeSet(image, x0 + 7, y0 + 7, hi);
    safeSet(image, x0 + w - 8, y0 + 7, hi);
    safeSet(image, x0 + 7, y0 + h - 8, hi);
    safeSet(image, x0 + w - 8, y0 + h - 8, hi);

    int cx = x0 + w / 2;
    int top = y0 + 16;
    int bottom = y0 + h - 16;
    boolean star = true;
    for (int y = top; y < bottom; y += 10) {
      if (star) {

        safeSet(image, cx, y - 1, lo);
        safeSet(image, cx, y + 1, lo);
        safeSet(image, cx - 1, y, lo);
        safeSet(image, cx + 1, y, lo);
        safeSet(image, cx, y, hi);
      } else {

        safeSet(image, cx, y, lo);
      }
      star = !star;
    }
  }

  private static void drawScrollOrnaments(NativeImage image, int x0, int y0, int w, int h, int lo, int hi) {

    drawScrollCorner(image, x0 + 5, y0 + 5, false, false, lo, hi);
    drawScrollCorner(image, x0 + w - 6, y0 + 5, true, false, lo, hi);
    drawScrollCorner(image, x0 + 5, y0 + h - 6, false, true, lo, hi);
    drawScrollCorner(image, x0 + w - 6, y0 + h - 6, true, true, lo, hi);

    int cx = x0 + w / 2;
    int top = y0 + 16;
    int bottom = y0 + h - 16;
    int row = 0;
    for (int y = top; y < bottom; y += 7) {
      if (row % 2 == 0) {

        safeSet(image, cx - 1, y, lo);
        safeSet(image, cx, y, lo);
        safeSet(image, cx + 1, y, lo);
        safeSet(image, cx, y, hi);
      } else {

        safeSet(image, cx, y, hi);
      }
      row++;
    }
  }

  private static void drawHexPipsOrnaments(NativeImage image, int x0, int y0, int w, int h, int lo, int hi) {

    drawHexPip(image, x0 + 7, y0 + 7, lo, hi);
    drawHexPip(image, x0 + w - 8, y0 + 7, lo, hi);
    drawHexPip(image, x0 + 7, y0 + h - 8, lo, hi);
    drawHexPip(image, x0 + w - 8, y0 + h - 8, lo, hi);

    int cx = x0 + w / 2;
    int top = y0 + 18;
    int bottom = y0 + h - 18;
    for (int y = top; y < bottom; y += 12) {
      drawHexOutline(image, cx, y, 2, lo);
    }
  }

  private static void drawScrollCorner(NativeImage image, int cx, int cy,
                                       boolean flipX, boolean flipY, int lo, int hi) {
    int sx = flipX ? -1 : 1;
    int sy = flipY ? -1 : 1;

    for (int i = 0; i <= 4; i++) {
      safeSet(image, cx + i * sx, cy, lo);
      safeSet(image, cx, cy + i * sy, lo);
    }

    safeSet(image, cx + 2 * sx, cy + 1 * sy, lo);
    safeSet(image, cx + 3 * sx, cy + 1 * sy, lo);
    safeSet(image, cx + 3 * sx, cy + 2 * sy, lo);
    safeSet(image, cx + 1 * sx, cy + 2 * sy, lo);
    safeSet(image, cx + 1 * sx, cy + 3 * sy, lo);
    safeSet(image, cx + 2 * sx, cy + 3 * sy, lo);

    safeSet(image, cx + 2 * sx, cy + 2 * sy, hi);
  }

  private static void drawHexPip(NativeImage image, int cx, int cy, int lo, int hi) {
    safeSet(image, cx, cy - 2, lo);
    safeSet(image, cx - 1, cy - 1, lo);
    safeSet(image, cx + 1, cy - 1, lo);
    safeSet(image, cx - 2, cy, lo);
    safeSet(image, cx + 2, cy, lo);
    safeSet(image, cx - 1, cy + 1, lo);
    safeSet(image, cx + 1, cy + 1, lo);
    safeSet(image, cx, cy + 2, lo);

    safeSet(image, cx, cy, hi);
  }

  private static void drawHexOutline(NativeImage image, int cx, int cy, int r, int color) {
    safeSet(image, cx, cy - r, color);
    safeSet(image, cx - 1, cy - r + 1, color);
    safeSet(image, cx + 1, cy - r + 1, color);
    safeSet(image, cx - r, cy, color);
    safeSet(image, cx + r, cy, color);
    safeSet(image, cx - 1, cy + r - 1, color);
    safeSet(image, cx + 1, cy + r - 1, color);
    safeSet(image, cx, cy + r, color);
  }

  private static void drawLeafCorner(NativeImage image, int cx, int cy, boolean flipX, boolean flipY, int lo, int hi) {
    int sx = flipX ? -1 : 1;
    int sy = flipY ? -1 : 1;

    for (int dy = 0; dy <= 4; dy++) {
      for (int dx = 0; dx <= 4 - dy; dx++) {
        safeSet(image, cx + dx * sx, cy + dy * sy, lo);
      }
    }

    safeSet(image, cx + sx, cy + sy, hi);
    safeSet(image, cx + 2 * sx, cy + sy, hi);
    safeSet(image, cx + sx, cy + 2 * sy, hi);
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

    for (int i = 0; i < 80; i++) {
      safeSet(image, 40 + i, 40 + i, red);
      safeSet(image, 40 + i, 41 + i, red);
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
    image.setPixelABGR(x, y, abgr);
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

  /**
   * Distinguishes the visual treatment given to the decorative side border
   * stamped into the {186,0}-{220,192} sub-region of the cover texture. The
   * border is then blitted twice (left + right edges) by
   * {@link art.arcane.mystcraft.client.screen.BookScreen} for every kind that
   * is not {@link #GENERIC}.
   */
  public enum BookKind {
    /**
     * Descriptive (Age) book — warm gold filigree border.
     */
    AGEBOOK,
    /**
     * Linkbook — emerald green vine border.
     */
    LINKBOOK,
    /**
     * Personal Link Book — silvery white star border.
     */
    PERSONAL_LINK,
    /**
     * Art-of-Writing tutorial / Guidebook — midnight blue with antique gold
     * scroll filigree, signalling the master tome.
     */
    GUIDEBOOK,
    /**
     * Unlinked Linkbook — slate grey with dim copper hex pip pattern,
     * signalling an inactive / unwritten link.
     */
    LINKBOOK_UNLINKED,
    /**
     * Generic / unwritten — no decorative border drawn.
     */
    GENERIC
  }

  private enum BorderStyle {
    /**
     * Plus-sign corners + dotted vertical filigree (Agebook).
     */
    FILIGREE,
    /**
     * Triangular leaf corners + sinusoidal vine centerline (Linkbook).
     */
    VINES,
    /**
     * Diamond corners + dotted star pattern centerline (Personal Link Book).
     */
    STARS,
    /**
     * Spiral scroll corners + central rune-quill column (Guidebook).
     */
    SCROLL,
    /**
     * Hex-pip corners + dim hex column (Unlinked Linkbook).
     */
    HEX_PIPS
  }

  private record Inputs(
      Identifier coverId,
      CoverPalette.Entry palette,
      BookKind kind,
      boolean linked,
      boolean dead,
      int pageCount,
      int inkTint,
      List<String> linkProperties,
      int authorsHash
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
      StringBuilder sb = new StringBuilder(96);
      sb.append(coverId == null ? "default" : coverId);
      sb.append('|').append(kindTag(kind));
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
     * Returns an {@code Inputs} with the cover material forced to the default
     * neutral palette and the ink tint cleared. Used when the
     * {@link MystcraftConfig#proceduralBookCoversEnabled} toggle is off so
     * every book renders with the same neutral cover regardless of the binding
     * material or ink that was used. The {@link BookKind} is preserved so
     * descriptive / linking / personal books still read as visually distinct
     * via their decorative border.
     */
    Inputs withNeutralCover() {
      return new Inputs(null, CoverPalette.get(null), kind, linked, dead,
          pageCount, -1, linkProperties, 0);
    }
  }

  private record BorderPalette(int base, int hi, int lo, BorderStyle style) {
  }
}
