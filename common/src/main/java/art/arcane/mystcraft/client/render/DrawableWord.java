package art.arcane.mystcraft.client.render;

/**
 * A curated D'ni word — pins a thematic colour and a deterministic seed
 * for one of the words in a Narayan Poem.
 * <p>
 * <b>Seed pin model</b>: the procedural glyph generator in
 * {@link art.arcane.mystcraft.client.gui.procedural.symbol.SymbolGlyphFactory}
 * uses {@link #seed()} to lock the visual identity of vocabulary words
 * (so "Fire" always renders the same primitives), and {@link #pinnedColor()}
 * to override the per-category palette accent for that one tile.
 * <p>
 * Created by {@link DrawableWordManager#registerWord(String, int, int)}.
 * Curated entries are pinned (non-zero seed and a colour); novel/unknown
 * words get a name-derived seed and {@code null} colour so the
 * procedural pipeline can still draw them consistently.
 */
public class DrawableWord {

  /** Procedural seed pinned to this word. 0 means "no pin — derive from name". */
  private final int seed;

  /** Optional thematic colour pin. {@code null} means "use category palette accent". */
  private final Integer pinnedColor;

  /**
   * Primary constructor.
   *
   * @param seed        a deterministic seed used by
   *                    {@code SymbolGlyphFactory} for primitive selection.
   *                    Combined (XOR) with the symbol id mix-in so two
   *                    different symbols sharing the same poem word never
   *                    render identically. {@code 0} disables the pin.
   * @param pinnedColor optional thematic colour (0xRRGGBB).
   *                    {@code null} disables the colour pin.
   */
  public DrawableWord(int seed, Integer pinnedColor) {
    this.seed = seed;
    this.pinnedColor = pinnedColor;
  }

  /** The deterministic seed pinned to this word ({@code 0} = unset). */
  public int seed() {
    return seed;
  }

  /** The pinned thematic colour, or {@code null} if none. */
  public Integer pinnedColor() {
    return pinnedColor;
  }
}
