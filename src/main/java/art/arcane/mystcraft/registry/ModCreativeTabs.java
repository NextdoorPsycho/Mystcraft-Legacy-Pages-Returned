package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/**
 * Creative mode tab registration for Mystcraft.
 */
public final class ModCreativeTabs {

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

                                // Pages and storage
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

    private ModCreativeTabs() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
