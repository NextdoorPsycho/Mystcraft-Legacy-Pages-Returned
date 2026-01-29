package art.arcane.mystcraft.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.function.Supplier;

/**
 * Common accessor for registered blocks.
 * Platform modules populate these suppliers during initialization.
 */
public final class ModBlocks {

  public static Supplier<Block> INK_MIXER;
  public static Supplier<Block> BOOK_BINDER;
  public static Supplier<Block> BOOK_RECEPTACLE;
  public static Supplier<Block> BOOKSTAND;
  public static Supplier<Block> LECTERN;
  public static Supplier<Block> LINK_MODIFIER;
  public static Supplier<Block> WRITING_DESK;
  public static Supplier<Block> CRYSTAL;
  public static Supplier<Block> DECAY;
  public static Supplier<Block> LINK_PORTAL;
  public static Supplier<Block> STAR_FISSURE;
  public static Supplier<LiquidBlock> FLUID_INK;

  private ModBlocks() {
  }
}
