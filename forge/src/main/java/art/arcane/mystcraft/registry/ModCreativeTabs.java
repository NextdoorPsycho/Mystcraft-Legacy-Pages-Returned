package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
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
                    () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID))
                            .icon(() -> new ItemStack(ForgeModItems.AGEBOOK.get()))
                            .displayItems((params, output) -> {
                                // Guidebook (tutorial)
                                output.accept(ForgeModItems.GUIDEBOOK.get());

                                // Books
                                output.accept(ForgeModItems.AGEBOOK.get());
                                output.accept(ForgeModItems.LINKBOOK.get());
                                output.accept(ForgeModItems.LINKBOOK_UNLINKED.get());
                                if (MystcraftConfig.enablePersonalLinkBooks.get()) {
                                    output.accept(ForgeModItems.PERSONAL_LINK_BOOK.get());
                                }

                                // Page storage items
                                output.accept(ForgeModItems.PAGE.get());
                                output.accept(ForgeModItems.FOLDER.get());
                                output.accept(ForgeModItems.PORTFOLIO.get());
                                output.accept(ForgeModItems.BOOSTER_PACK.get());

                                // Ink and tools
                                output.accept(ForgeModItems.INK_VIAL.get());
                                output.accept(ForgeModItems.INK_BUCKET.get());

                                // Workstation blocks
                                output.accept(ForgeModItems.WRITING_DESK_ITEM.get());
                                output.accept(ForgeModItems.INK_MIXER_ITEM.get());
                                output.accept(ForgeModItems.BOOK_BINDER_ITEM.get());
                                output.accept(ForgeModItems.LINK_MODIFIER_ITEM.get());
                                output.accept(ForgeModItems.BOOKSTAND_ITEM.get());

                                // Portal blocks
                                output.accept(ForgeModItems.BOOK_RECEPTACLE_ITEM.get());
                                output.accept(ForgeModItems.CRYSTAL_ITEM.get());

                                // Special blocks
                                output.accept(ForgeModItems.DECAY_ITEM.get());
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
