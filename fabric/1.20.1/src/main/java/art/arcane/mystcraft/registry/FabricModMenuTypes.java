package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.menu.BookBinderMenu;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/**
 * Menu type registrations for Mystcraft (Fabric 1.20.1).
 * Uses Fabric's ExtendedScreenHandlerType for menus with extra data.
 */
public final class FabricModMenuTypes {

    public static final Supplier<MenuType<InkMixerMenu>> INK_MIXER =
            registerMenu("ink_mixer", new ExtendedScreenHandlerType<>(InkMixerMenu::new));

    public static final Supplier<MenuType<BookBinderMenu>> BOOK_BINDER =
            registerMenu("book_binder", new ExtendedScreenHandlerType<>(BookBinderMenu::new));

    public static final Supplier<MenuType<LinkModifierMenu>> LINK_MODIFIER =
            registerMenu("link_modifier", new ExtendedScreenHandlerType<>(LinkModifierMenu::new));

    public static final Supplier<MenuType<WritingDeskMenu>> WRITING_DESK =
            registerMenu("writing_desk", new ExtendedScreenHandlerType<>(WritingDeskMenu::new));

    public static final Supplier<MenuType<FolderMenu>> FOLDER =
            registerMenu("folder", new ExtendedScreenHandlerType<>(FolderMenu::new));

    public static final Supplier<MenuType<PortfolioMenu>> PORTFOLIO =
            registerMenu("portfolio", new ExtendedScreenHandlerType<>(PortfolioMenu::new));

    private FabricModMenuTypes() {
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(
            String name, MenuType<T> menuType) {
        Registry.register(BuiltInRegistries.MENU, new ResourceLocation(Mystcraft.MOD_ID, name), menuType);
        return () -> menuType;
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
