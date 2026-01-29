package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.menu.*;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/**
 * Common accessor for registered menu types.
 * Platform modules populate these suppliers during initialization.
 */
public final class ModMenuTypes {

  public static Supplier<MenuType<InkMixerMenu>> INK_MIXER;
  public static Supplier<MenuType<BookBinderMenu>> BOOK_BINDER;
  public static Supplier<MenuType<LinkModifierMenu>> LINK_MODIFIER;
  public static Supplier<MenuType<WritingDeskMenu>> WRITING_DESK;
  public static Supplier<MenuType<FolderMenu>> FOLDER;
  public static Supplier<MenuType<PortfolioMenu>> PORTFOLIO;

  private ModMenuTypes() {
  }
}
