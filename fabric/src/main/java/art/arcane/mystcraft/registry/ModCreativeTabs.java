package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Creative mode tab registration for Mystcraft (Fabric).
 */
public final class ModCreativeTabs {

    private static final ResourceKey<CreativeModeTab> MYSTCRAFT_TAB_KEY =
            ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(),
                    new ResourceLocation(Mystcraft.MOD_ID, "mystcraft"));

    private static final ResourceKey<CreativeModeTab> MYSTCRAFT_PAGES_TAB_KEY =
            ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(),
                    new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_pages"));

    /**
     * Main Mystcraft tab - blocks, items, tools.
     */
    public static final Supplier<CreativeModeTab> MYSTCRAFT_TAB;

    /**
     * Mystcraft Pages tab - link panels and all symbol pages.
     */
    public static final Supplier<CreativeModeTab> MYSTCRAFT_PAGES_TAB;

    static {
        CreativeModeTab mainTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID))
                .icon(() -> new ItemStack(FabricModItems.AGEBOOK.get()))
                .displayItems((params, output) -> {
                    // Guidebook (tutorial)
                    output.accept(FabricModItems.GUIDEBOOK.get());

                    // Books
                    output.accept(FabricModItems.AGEBOOK.get());
                    output.accept(FabricModItems.LINKBOOK.get());
                    output.accept(FabricModItems.LINKBOOK_UNLINKED.get());
                    if (MystcraftConfig.enablePersonalLinkBooks.get()) {
                        output.accept(FabricModItems.PERSONAL_LINK_BOOK.get());
                    }

                    // Page storage items
                    output.accept(FabricModItems.PAGE.get());
                    output.accept(FabricModItems.FOLDER.get());
                    output.accept(FabricModItems.PORTFOLIO.get());
                    output.accept(FabricModItems.BOOSTER_PACK.get());

                    // Ink and tools
                    output.accept(FabricModItems.INK_VIAL.get());
                    output.accept(FabricModItems.INK_BUCKET.get());

                    // Workstation blocks
                    output.accept(FabricModItems.WRITING_DESK_ITEM.get());
                    output.accept(FabricModItems.INK_MIXER_ITEM.get());
                    output.accept(FabricModItems.BOOK_BINDER_ITEM.get());
                    output.accept(FabricModItems.LINK_MODIFIER_ITEM.get());
                    output.accept(FabricModItems.BOOKSTAND_ITEM.get());

                    // Portal blocks
                    output.accept(FabricModItems.BOOK_RECEPTACLE_ITEM.get());
                    output.accept(FabricModItems.CRYSTAL_ITEM.get());

                    // Special blocks
                    output.accept(FabricModItems.DECAY_ITEM.get());
                })
                .build();
        MYSTCRAFT_TAB = () -> mainTab;

        CreativeModeTab pagesTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
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
                .build();
        MYSTCRAFT_PAGES_TAB = () -> pagesTab;
    }

    private ModCreativeTabs() {
    }

    /**
     * Registers creative tabs and sets up Fabric ItemGroupEvents for the pages tab.
     */
    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(Mystcraft.MOD_ID, "mystcraft"), MYSTCRAFT_TAB.get());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_pages"), MYSTCRAFT_PAGES_TAB.get());

        // Fabric event-based tab content modification for symbol pages
        ItemGroupEvents.modifyEntriesEvent(MYSTCRAFT_PAGES_TAB_KEY).register(content -> {
            int count = 0;
            for (SymbolCategory category : SymbolCategory.values()) {
                List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
                for (IAgeSymbol symbol : symbols) {
                    content.accept(Page.createSymbolPage(symbol.getRegistryName()));
                    count++;
                }
            }
            Mystcraft.LOGGER.info("[ModCreativeTabs] ItemGroupEvents added {} symbol pages", count);
        });
    }
}
