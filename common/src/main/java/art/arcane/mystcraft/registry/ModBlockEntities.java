package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Common accessor for registered block entity types.
 * Platform modules populate these suppliers during initialization.
 */
public final class ModBlockEntities {

  public static Supplier<BlockEntityType<InkMixerBlockEntity>> INK_MIXER;
  public static Supplier<BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER;
  public static Supplier<BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE;
  public static Supplier<BlockEntityType<BookstandBlockEntity>> BOOKSTAND;
  public static Supplier<BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK;
  public static Supplier<BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE;
  public static Supplier<BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER;

  private ModBlockEntities() {
  }
}
