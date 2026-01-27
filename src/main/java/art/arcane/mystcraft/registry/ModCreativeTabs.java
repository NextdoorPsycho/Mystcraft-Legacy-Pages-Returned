package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
                                // Guidebook (tutorial)
                                output.accept(ModItems.GUIDEBOOK.get());

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
     * Mystcraft Pages tab - link panels and all symbol pages.
     * The displayItems callback populates the link page.
     * Symbol pages are added via BuildCreativeModeTabContentsEvent which fires
     * when the creative inventory is opened, after all symbols are registered.
     */
    public static final RegistryObject<CreativeModeTab> MYSTCRAFT_PAGES_TAB =
            MystcraftRegistries.CREATIVE_TABS.register("mystcraft_pages",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID + "_pages"))
                            .icon(() -> Page.createLinkPage())
                            .displayItems((params, output) -> {
                                Mystcraft.LOGGER.warn("[ModCreativeTabs] Pages tab displayItems CALLED - registry size: {}, frozen: {}",
                                        SymbolRegistry.size(), SymbolRegistry.isFrozen());

                                try {
                                    output.accept(Page.createLinkPage());
                                } catch (Exception e) {
                                    Mystcraft.LOGGER.error("[ModCreativeTabs] Failed to create link page", e);
                                }

                                int count = 0;
                                for (SymbolCategory category : SymbolCategory.values()) {
                                    List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
                                    for (IAgeSymbol symbol : symbols) {
                                        try {
                                            output.accept(Page.createSymbolPage(symbol.getRegistryName()));
                                            count++;
                                        } catch (Exception e) {
                                            Mystcraft.LOGGER.error("[ModCreativeTabs] Failed to create page for symbol: {}", symbol.getRegistryName(), e);
                                        }
                                    }
                                }
                                Mystcraft.LOGGER.warn("[ModCreativeTabs] displayItems populated {} symbol pages", count);
                            })
                            .build());

    private ModCreativeTabs() {
    }

    /**
     * Adds all symbol pages to the pages tab via Forge event.
     * This fires when creative tab contents are built (when the player opens the creative inventory).
     */
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == MYSTCRAFT_PAGES_TAB.get()) {
            int count = 0;
            for (SymbolCategory category : SymbolCategory.values()) {
                List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
                for (IAgeSymbol symbol : symbols) {
                    event.accept(Page.createSymbolPage(symbol.getRegistryName()));
                    count++;
                }
            }
            Mystcraft.LOGGER.info("[ModCreativeTabs] BuildCreativeModeTabContentsEvent added {} symbol pages", count);
        }
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
