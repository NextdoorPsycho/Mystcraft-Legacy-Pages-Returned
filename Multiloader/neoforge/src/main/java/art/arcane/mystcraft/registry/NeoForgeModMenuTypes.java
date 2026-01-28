package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.menu.BookBinderMenu;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Menu type registrations for Mystcraft.
 */
public final class NeoForgeModMenuTypes {

    public static final DeferredHolder<MenuType<?>, MenuType<InkMixerMenu>> INK_MIXER =
            MystcraftRegistries.MENUS.register("ink_mixer",
                    () -> IMenuTypeExtension.create(InkMixerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BookBinderMenu>> BOOK_BINDER =
            MystcraftRegistries.MENUS.register("book_binder",
                    () -> IMenuTypeExtension.create(BookBinderMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LinkModifierMenu>> LINK_MODIFIER =
            MystcraftRegistries.MENUS.register("link_modifier",
                    () -> IMenuTypeExtension.create(LinkModifierMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WritingDeskMenu>> WRITING_DESK =
            MystcraftRegistries.MENUS.register("writing_desk",
                    () -> IMenuTypeExtension.create(WritingDeskMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FolderMenu>> FOLDER =
            MystcraftRegistries.MENUS.register("folder",
                    () -> IMenuTypeExtension.create(FolderMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PortfolioMenu>> PORTFOLIO =
            MystcraftRegistries.MENUS.register("portfolio",
                    () -> IMenuTypeExtension.create(PortfolioMenu::new));

    private NeoForgeModMenuTypes() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
