package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Creative mode tab registration for Mystcraft.
 */
public final class ModCreativeTabs {

    /**
     * Main Mystcraft tab - blocks, items, tools
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MYSTCRAFT_TAB =
            MystcraftRegistries.CREATIVE_TABS.register("mystcraft",
                    () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID))
                            .icon(() -> new ItemStack(NeoForgeModItems.AGEBOOK.get()))
                            .displayItems((params, output) -> {
                                // Guidebook (tutorial)
                                output.accept(NeoForgeModItems.GUIDEBOOK.get());

                                // Books
                                output.accept(NeoForgeModItems.AGEBOOK.get());
                                output.accept(NeoForgeModItems.LINKBOOK.get());
                                output.accept(NeoForgeModItems.LINKBOOK_UNLINKED.get());
                                output.accept(NeoForgeModItems.PERSONAL_LINK_BOOK.get());

                                // Page storage items
                                output.accept(NeoForgeModItems.PAGE.get());
                                output.accept(NeoForgeModItems.FOLDER.get());
                                output.accept(NeoForgeModItems.PORTFOLIO.get());
                                output.accept(NeoForgeModItems.BOOSTER_PACK.get());

                                // Ink and tools
                                output.accept(NeoForgeModItems.INK_VIAL.get());
                                output.accept(NeoForgeModItems.INK_BUCKET.get());

                                // Workstation blocks
                                output.accept(NeoForgeModItems.WRITING_DESK_ITEM.get());
                                output.accept(NeoForgeModItems.INK_MIXER_ITEM.get());
                                output.accept(NeoForgeModItems.BOOK_BINDER_ITEM.get());
                                output.accept(NeoForgeModItems.LINK_MODIFIER_ITEM.get());
                                output.accept(NeoForgeModItems.BOOKSTAND_ITEM.get());

                                // Portal blocks
                                output.accept(NeoForgeModItems.BOOK_RECEPTACLE_ITEM.get());
                                output.accept(NeoForgeModItems.CRYSTAL_ITEM.get());

                                // Special blocks
                                output.accept(NeoForgeModItems.DECAY_ITEM.get());
                            })
                            .build());

    /**
     * Mystcraft Pages tab - link panels and all symbol pages.
     * The displayItems callback populates the link page.
     * Symbol pages are added via BuildCreativeModeTabContentsEvent which fires
     * when the creative inventory is opened, after all symbols are registered.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MYSTCRAFT_PAGES_TAB =
            MystcraftRegistries.CREATIVE_TABS.register("mystcraft_pages",
                    () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
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
     * Adds all symbol pages to the pages tab via NeoForge event.
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
