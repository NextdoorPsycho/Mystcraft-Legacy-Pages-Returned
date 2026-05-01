package art.arcane.mystcraft.client.gui.procedural.symbol;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Letter-based fallback for {@link SymbolGlyphFactory}, used when
 * {@link
 * art.arcane.mystcraft.config.MystcraftConfig#proceduralSymbolGlyphsEnabled} is
 * {@code false}. Draws the first two characters of the symbol's registry path
 * centred in the tile using a hand-rolled 5&times;7 bitmap font.
 *
 * <p>Why a hand-rolled font instead of {@code Minecraft.getInstance().font}?
 * The pre-warm path runs on a daemon worker thread without a GL context, so
 * {@code Font.draw(...)} cannot rasterise into a {@link NativeImage} directly.
 * A bitmap font keeps the fallback context-independent and deterministic so the
 * same fallback texture round-trips through warm (off-thread) and on-demand
 * (main thread) calls.
 *
 * <p>Coverage: digits, lowercase a-z, uppercase A-Z, underscore. These
 * are the only characters that appear in Mystcraft symbol registry paths (see
 * {@code SymbolRegistry.mystcraftId} for the namespace convention). Characters
 * outside the supported set render as a centred {@code .} dot so missing glyphs
 * stay visible without crashing.
 */
public final class SymbolLetterFallback {

  private static final int GLYPH_W = 5;
  private static final int GLYPH_H = 7;
  private static final int SCALE = 8;

  private static final Map<Character, int[]> FONT = buildFont();

  private SymbolLetterFallback() {
  }

  /**
   * Renders the first two characters of {@code source} into {@code image}
   * centred horizontally and vertically. The image is assumed to be a fresh
   * {@link SymbolGlyphFactory#GLYPH_SIZE 128&times;128} tile cleared to
   * transparent. Letters use {@code accent}; an inset shadow uses {@code ink}
   * so the fallback stays legible against any parchment variant from
   * {@link SymbolPalette}.
   */
  public static void renderInto(@NotNull NativeImage image, @Nullable String source,
                                int accent, int ink) {
    String text = pickFirstTwoChars(source);
    int totalW = (GLYPH_W * SCALE) * 2 + SCALE * 2;
    int totalH = GLYPH_H * SCALE;
    int originX = (image.getWidth() - totalW) / 2;
    int originY = (image.getHeight() - totalH) / 2;

    int accentArgb = 0xFF000000 | (accent & 0xFFFFFF);
    int inkArgb = 0xFF000000 | (ink & 0xFFFFFF);

    for (int i = 0; i < text.length(); i++) {
      int gx = originX + i * (GLYPH_W * SCALE + SCALE * 2);
      int[] mask = glyphFor(text.charAt(i));

      drawMask(image, mask, gx + SCALE, originY + SCALE, inkArgb);
      drawMask(image, mask, gx, originY, accentArgb);
    }
  }

  @NotNull
  private static String pickFirstTwoChars(@Nullable String source) {
    if (source == null || source.isBlank()) return "??";
    int slash = source.lastIndexOf('/');
    int colon = source.lastIndexOf(':');
    int cut = Math.max(slash, colon);
    String trimmed = cut >= 0 && cut + 1 < source.length()
        ? source.substring(cut + 1)
        : source;
    String upper = trimmed.toUpperCase(Locale.ROOT);
    if (upper.length() >= 2) return upper.substring(0, 2);
    if (upper.length() == 1) return upper + "?";
    return "??";
  }

  @NotNull
  private static int[] glyphFor(char c) {
    int[] mask = FONT.get(Character.toUpperCase(c));
    if (mask != null) return mask;
    int[] dot = FONT.get('.');
    return dot != null ? dot : new int[GLYPH_H];
  }

  private static void drawMask(@NotNull NativeImage image, @NotNull int[] mask,
                               int x0, int y0, int argb) {
    int w = image.getWidth();
    int h = image.getHeight();
    for (int row = 0; row < GLYPH_H; row++) {
      int bits = mask[row];
      for (int col = 0; col < GLYPH_W; col++) {
        if ((bits & (1 << (GLYPH_W - 1 - col))) == 0) continue;
        for (int sy = 0; sy < SCALE; sy++) {
          for (int sx = 0; sx < SCALE; sx++) {
            int px = x0 + col * SCALE + sx;
            int py = y0 + row * SCALE + sy;
            if (px < 0 || py < 0 || px >= w || py >= h) continue;
            image.setPixelRGBA(px, py, argb);
          }
        }
      }
    }
  }

  private static Map<Character, int[]> buildFont() {
    Map<Character, int[]> m = new HashMap<>();
    m.put('0', new int[]{0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110});
    m.put('1', new int[]{0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110});
    m.put('2', new int[]{0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111});
    m.put('3', new int[]{0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110});
    m.put('4', new int[]{0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010});
    m.put('5', new int[]{0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110});
    m.put('6', new int[]{0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110});
    m.put('7', new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000});
    m.put('8', new int[]{0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110});
    m.put('9', new int[]{0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100});

    m.put('A', new int[]{0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001});
    m.put('B', new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110});
    m.put('C', new int[]{0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110});
    m.put('D', new int[]{0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110});
    m.put('E', new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111});
    m.put('F', new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000});
    m.put('G', new int[]{0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110});
    m.put('H', new int[]{0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001});
    m.put('I', new int[]{0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110});
    m.put('J', new int[]{0b00111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100});
    m.put('K', new int[]{0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001});
    m.put('L', new int[]{0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111});
    m.put('M', new int[]{0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001});
    m.put('N', new int[]{0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001});
    m.put('O', new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110});
    m.put('P', new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000});
    m.put('Q', new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101});
    m.put('R', new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001});
    m.put('S', new int[]{0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110});
    m.put('T', new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100});
    m.put('U', new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110});
    m.put('V', new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100});
    m.put('W', new int[]{0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b10101, 0b01010});
    m.put('X', new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001});
    m.put('Y', new int[]{0b10001, 0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100});
    m.put('Z', new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111});

    m.put('_', new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b11111});
    m.put('.', new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b01100});
    m.put('/', new int[]{0b00001, 0b00010, 0b00100, 0b00100, 0b01000, 0b10000, 0b10000});
    m.put('?', new int[]{0b01110, 0b10001, 0b00010, 0b00100, 0b00100, 0b00000, 0b00100});
    m.put(' ', new int[]{0, 0, 0, 0, 0, 0, 0});

    return m;
  }
}
