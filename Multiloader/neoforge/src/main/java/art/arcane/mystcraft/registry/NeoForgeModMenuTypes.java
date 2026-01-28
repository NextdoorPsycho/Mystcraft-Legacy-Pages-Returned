package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.menu.BookBinderMenu;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Menu type registrations for Mystcraft.
 */
public final class NeoForgeModMenuTypes {

    public static final RegistryObject<MenuType<InkMixerMenu>> INK_MIXER =
            MystcraftRegistries.MENUS.register("ink_mixer",
                    () -> IForgeMenuType.create(InkMixerMenu::new));

    public static final RegistryObject<MenuType<BookBinderMenu>> BOOK_BINDER =
            MystcraftRegistries.MENUS.register("book_binder",
                    () -> IForgeMenuType.create(BookBinderMenu::new));

    public static final RegistryObject<MenuType<LinkModifierMenu>> LINK_MODIFIER =
            MystcraftRegistries.MENUS.register("link_modifier",
                    () -> IForgeMenuType.create(LinkModifierMenu::new));

    public static final RegistryObject<MenuType<WritingDeskMenu>> WRITING_DESK =
            MystcraftRegistries.MENUS.register("writing_desk",
                    () -> IForgeMenuType.create(WritingDeskMenu::new));

    public static final RegistryObject<MenuType<FolderMenu>> FOLDER =
            MystcraftRegistries.MENUS.register("folder",
                    () -> IForgeMenuType.create(FolderMenu::new));

    public static final RegistryObject<MenuType<PortfolioMenu>> PORTFOLIO =
            MystcraftRegistries.MENUS.register("portfolio",
                    () -> IForgeMenuType.create(PortfolioMenu::new));

    private NeoForgeModMenuTypes() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
