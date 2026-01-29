package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MystcraftGameTestAssertions {

    private MystcraftGameTestAssertions() {
    }

    public static void assertRegistries(ServerLevel level) {
        assertItemRegistered("linkbook");
        assertItemRegistered("linkbook_unlinked");
        assertItemRegistered("personal_link_book");
        assertItemRegistered("agebook");

        assertCreativeTabRegistered("mystcraft");
        assertCreativeTabRegistered("mystcraft_pages");

        assertAdvancementExists(level, "linkbook");
    }

    public static void assertLinkbookDropsByDefault() {
        if (ModItems.LINKBOOK == null) {
            throw new IllegalStateException("ModItems.LINKBOOK is not initialized");
        }
        Item item = ModItems.LINKBOOK.get();
        if (!(item instanceof LinkbookItem linkbook)) {
            throw new IllegalStateException("Mystcraft linkbook item is not a LinkbookItem");
        }
        ItemStack stack = new ItemStack(item);
        if (!linkbook.dropItemOnLink(stack)) {
            throw new IllegalStateException("Linkbook should drop by default when following flag is not set");
        }
    }

    private static void assertItemRegistered(String itemId) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, itemId);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Item not registered: " + id);
        }
    }

    private static void assertCreativeTabRegistered(String tabId) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, tabId);
        if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) {
            throw new IllegalStateException("Creative tab not registered: " + id);
        }
    }

    private static void assertAdvancementExists(ServerLevel level, String advancementId) {
        ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, advancementId);
        AdvancementHolder advancement = level.getServer().getAdvancements().get(id);
        if (advancement == null) {
            throw new IllegalStateException("Advancement not registered: " + id);
        }
    }
}
