package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Creative mode tab registration for Mystcraft.
 */
public final class ModCreativeTabs {

    /**
     * Main Mystcraft tab - blocks, items, tools
     */
    public static final RegistryObject<CreativeModeTab> MYSTCRAFT_TAB =
            MystcraftRegistries.CREATIVE_TABS.register("mystcraft",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID))
                            .icon(() -> new ItemStack(ModItems.AGEBOOK.get()))
                            .displayItems((params, output) -> {
                                // Books
                                output.accept(ModItems.AGEBOOK.get());
                                output.accept(ModItems.LINKBOOK.get());
                                output.accept(ModItems.LINKBOOK_UNLINKED.get());

                                // Page storage items
                                output.accept(ModItems.PAGE.get());
                                output.accept(ModItems.FOLDER.get());
                                output.accept(ModItems.PORTFOLIO.get());
                                output.accept(ModItems.BOOSTER_PACK.get());

                                // Ink and tools
                                output.accept(ModItems.INK_VIAL.get());
                                output.accept(ModItems.INK_BUCKET.get());
                                output.accept(ModItems.GLASSES.get());

                                // Workstation blocks
                                output.accept(ModItems.WRITING_DESK_ITEM.get());
                                output.accept(ModItems.INK_MIXER_ITEM.get());
                                output.accept(ModItems.BOOK_BINDER_ITEM.get());
                                output.accept(ModItems.LINK_MODIFIER_ITEM.get());
                                output.accept(ModItems.BOOKSTAND_ITEM.get());
                                output.accept(ModItems.LECTERN_ITEM.get());

                                // Portal blocks
                                output.accept(ModItems.BOOK_RECEPTACLE_ITEM.get());
                                output.accept(ModItems.CRYSTAL_ITEM.get());

                                // Special blocks
                                output.accept(ModItems.DECAY_ITEM.get());
                            })
                            .build());

    /**
     * Mystcraft Pages tab - link panels and all symbol pages
     */
    public static final RegistryObject<CreativeModeTab> MYSTCRAFT_PAGES_TAB =
            MystcraftRegistries.CREATIVE_TABS.register("mystcraft_pages",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID + "_pages"))
                            .icon(() -> Page.createLinkPage())
                            .displayItems((params, output) -> {
                                // Link panel page (required for linking books)
                                output.accept(Page.createLinkPage());

                                // Symbol pages - organized by category
                                for (SymbolCategory category : SymbolCategory.values()) {
                                    List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
                                    for (IAgeSymbol symbol : symbols) {
                                        output.accept(Page.createSymbolPage(symbol.getRegistryName()));
                                    }
                                }
                            })
                            .build());

    private ModCreativeTabs() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
