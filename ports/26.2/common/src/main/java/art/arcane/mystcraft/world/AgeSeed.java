package art.arcane.mystcraft.world;

import art.arcane.mystcraft.util.NbtCompat;

import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Owns the persisted seed used to turn an ordered Age description into world
 * generation. Explicit seeds are preserved, while books without one derive a
 * stable seed from their ordered symbol identifiers.
 */
public final class AgeSeed {

  private static final String TAG_SEED = "Seed";
  private static final byte[] HASH_VERSION = "mystcraft-age-seed-v1".getBytes(StandardCharsets.UTF_8);
  private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x100000001b3L;

  private AgeSeed() {
  }

  /**
   * Reads a seed previously stored on an Agebook.
   */
  @NotNull
  public static OptionalLong read(@NotNull ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag == null || !NbtCompat.contains(tag, TAG_SEED, NbtCompat.TAG_ANY_NUMERIC)) {
      return OptionalLong.empty();
    }
    return OptionalLong.of(tag.getLongOr(TAG_SEED, 0L));
  }

  /**
   * Stores an explicit seed on an Agebook.
   */
  public static void write(@NotNull ItemStack stack, long seed) {
    if (stack.isEmpty()) {
      return;
    }
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    tag.putLong(TAG_SEED, seed);
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Removes an explicit seed. The next resolution derives it from the pages.
   */
  public static void clear(@NotNull ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag == null) {
      return;
    }
    tag.remove(TAG_SEED);
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Returns the stored seed or derives and persists one from the book pages.
   */
  public static long resolve(@NotNull ItemStack stack, @NotNull List<ItemStack> pages) {
    OptionalLong storedSeed = read(stack);
    if (storedSeed.isPresent()) {
      return storedSeed.getAsLong();
    }

    long seed = deriveFromPages(pages);
    write(stack, seed);
    return seed;
  }

  /**
   * Derives a stable seed from ordered symbol pages. Link panels, blank pages,
   * and other non-symbol pages do not affect generation.
   */
  public static long deriveFromPages(@NotNull List<ItemStack> pages) {
    return deriveFromSymbolIds(symbolIdsFromPages(pages));
  }

  /**
   * Extracts the ordered generation symbols from a page sequence.
   */
  @NotNull
  public static List<Identifier> symbolIdsFromPages(@NotNull List<ItemStack> pages) {
    List<Identifier> symbols = new ArrayList<>();
    for (ItemStack page : pages) {
      Identifier symbolId = Page.getSymbol(page);
      if (symbolId != null) {
        symbols.add(symbolId);
      }
    }
    return List.copyOf(symbols);
  }

  /**
   * Derives a stable seed from an ordered canonical symbol sequence. Length
   * framing makes duplicates and boundaries unambiguous.
   */
  public static long deriveFromSymbolIds(@NotNull List<Identifier> symbolIds) {
    long hash = updateBytes(FNV_OFFSET_BASIS, HASH_VERSION);
    hash = updateInt(hash, symbolIds.size());
    for (Identifier symbolId : symbolIds) {
      byte[] identifier = symbolId.toString().getBytes(StandardCharsets.UTF_8);
      hash = updateInt(hash, identifier.length);
      hash = updateBytes(hash, identifier);
    }
    return hash;
  }

  /**
   * Derives a stable, world-specific seed for a command-created random Age.
   */
  public static long deriveForAllocatedAge(long worldSeed, int ageUID) {
    long hash = updateBytes(FNV_OFFSET_BASIS, HASH_VERSION);
    hash = updateLong(hash, worldSeed);
    return updateInt(hash, ageUID);
  }

  private static long updateInt(long hash, int value) {
    for (int shift = 24; shift >= 0; shift -= 8) {
      hash = updateByte(hash, (byte) (value >>> shift));
    }
    return hash;
  }

  private static long updateLong(long hash, long value) {
    for (int shift = 56; shift >= 0; shift -= 8) {
      hash = updateByte(hash, (byte) (value >>> shift));
    }
    return hash;
  }

  private static long updateBytes(long hash, byte[] values) {
    long updated = hash;
    for (byte value : values) {
      updated = updateByte(updated, value);
    }
    return updated;
  }

  private static long updateByte(long hash, byte value) {
    return (hash ^ (value & 0xffL)) * FNV_PRIME;
  }
}
