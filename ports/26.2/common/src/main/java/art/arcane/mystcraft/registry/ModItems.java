package art.arcane.mystcraft.registry;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Common accessor for registered items. Platform modules populate these
 * suppliers during initialization.
 */
public final class ModItems {

  public static Supplier<Item> PAGE;
  public static Supplier<Item> AGEBOOK;
  public static Supplier<Item> LINKBOOK;
  public static Supplier<Item> LINKBOOK_UNLINKED;
  public static Supplier<Item> PERSONAL_LINK_BOOK;
  public static Supplier<Item> BOOSTER_PACK;
  public static Supplier<Item> FOLDER;
  public static Supplier<Item> PORTFOLIO;
  public static Supplier<Item> INK_VIAL;
  public static Supplier<Item> GUIDEBOOK;
  public static Supplier<Item> INK_BUCKET;
  public static Supplier<Item> INK_MIXER_ITEM;
  public static Supplier<Item> BOOK_BINDER_ITEM;
  public static Supplier<Item> BOOK_RECEPTACLE_ITEM;
  public static Supplier<Item> LINK_MODIFIER_ITEM;
  public static Supplier<Item> WRITING_DESK_ITEM;
  public static Supplier<Item> CRYSTAL_ITEM;
  public static Supplier<Item> DECAY_ITEM;

  private ModItems() {
  }
}
