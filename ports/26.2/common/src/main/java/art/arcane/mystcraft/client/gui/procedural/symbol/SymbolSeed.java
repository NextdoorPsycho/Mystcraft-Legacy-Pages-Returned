package art.arcane.mystcraft.client.gui.procedural.symbol;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Deterministic seed derivation for procedural symbol glyphs.
 * <p>
 * Uses the FNV-1a 32-bit hash so the result is stable across JVMs and Minecraft
 * versions — unlike {@code String#hashCode()} which is only defined to be
 * consistent within a single JVM run for reference types. The hash is mixed
 * with an explicit unsigned-arithmetic step so the value is independent of
 * platform endianness.
 * <p>
 * Two clients with the same symbol JSON are guaranteed to produce
 * pixel-identical glyphs because the seed they derive matches.
 */
public final class SymbolSeed {

  private static final int FNV_OFFSET_BASIS = 0x811C9DC5;
  private static final int FNV_PRIME = 0x01000193;

  private static final String SEP = "|";

  private SymbolSeed() {
  }

  /**
   * Derives a seed from a symbol id, poem word, and integer salt.
   * Caller-supplied null fields are normalised to empty strings; the result is
   * therefore stable for {@code derive(null, null, 0)}.
   *
   * @param id   symbol registry name (e.g. {@code mystcraft:biome_desert})
   * @param word poem word (e.g. {@code "Desert"}) — case-insensitive
   * @param salt extra mixing input (e.g. layer index, sub-stream index)
   * @return a 32-bit deterministic seed
   */
  public static int derive(@Nullable Identifier id, @Nullable String word, int salt) {
    StringBuilder sb = new StringBuilder(64);
    if (id != null) {
      sb.append(id.getNamespace()).append(':').append(id.getPath());
    }
    sb.append(SEP);
    if (word != null) {
      sb.append(word.toLowerCase(Locale.ROOT));
    }
    sb.append(SEP);
    sb.append(Integer.toUnsignedString(salt));
    return fnv1a32(sb.toString());
  }

  /**
   * Derives a seed from raw text. Useful for non-symbol callers (e.g.
   * theming-only seeds for the page background based on a category name).
   */
  public static int derive(@NotNull String raw, int salt) {
    return fnv1a32(raw + SEP + Integer.toUnsignedString(salt));
  }

  /**
   * 32-bit FNV-1a over the UTF-8 bytes of {@code text}. Independent of the
   * JVM's {@code String#hashCode} implementation; stable across machines and
   * runs.
   */
  public static int fnv1a32(@NotNull String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    int hash = FNV_OFFSET_BASIS;
    for (byte b : bytes) {
      hash ^= (b & 0xFF);
      hash *= FNV_PRIME;
    }
    return hash;
  }
}
