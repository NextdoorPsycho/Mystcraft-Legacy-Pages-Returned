package art.arcane.mystcraft.villager;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.registry.NeoForgeModItems;
import art.arcane.mystcraft.registry.ModVillagers;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.List;

/**
 * Registers trades for the Archivist villager profession.
 * Uses common trade listings from ArchivistTradeListings.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class ArchivistTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.ARCHIVIST.get()) {
            return;
        }

        Mystcraft.LOGGER.debug("[Mystcraft] Registering Archivist trades");

        // Level 1 trades (Novice)
        List<VillagerTrades.ItemListing> level1 = event.getTrades().get(1);
        level1.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 2),
                new ItemStack(NeoForgeModItems.INK_VIAL.get(), 1),
                12, 1, 0.05f
        ));
        level1.add(new BasicItemListing(
                new ItemStack(NeoForgeModItems.PAGE.get(), 8),
                new ItemStack(Items.EMERALD, 1),
                16, 2, 0.05f
        ));
        level1.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 1),
                new ItemStack(NeoForgeModItems.PAGE.get(), 4),
                16, 1, 0.05f
        ));
        level1.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 5));

        // Level 2 trades (Apprentice)
        List<VillagerTrades.ItemListing> level2 = event.getTrades().get(2);
        level2.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 5),
                new ItemStack(NeoForgeModItems.FOLDER.get(), 1),
                8, 5, 0.05f
        ));
        level2.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 3),
                new ItemStack(NeoForgeModItems.INK_VIAL.get(), 2),
                12, 5, 0.05f
        ));
        level2.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 8));
        level2.add(new ArchivistTradeListings.RankedSymbolTrade(2, 1, 10));

        // Level 3 trades (Journeyman)
        List<VillagerTrades.ItemListing> level3 = event.getTrades().get(3);
        level3.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 12),
                new ItemStack(NeoForgeModItems.PORTFOLIO.get(), 1),
                4, 10, 0.05f
        ));
        level3.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 8),
                new ItemStack(NeoForgeModItems.BOOSTER_PACK.get(), 1),
                6, 10, 0.05f
        ));
        level3.add(new ArchivistTradeListings.RankedSymbolTrade(2, 2, 12));
        level3.add(new ArchivistTradeListings.RankedSymbolTrade(3, 1, 15));

        // Level 4 trades (Expert)
        List<VillagerTrades.ItemListing> level4 = event.getTrades().get(4);
        level4.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 20),
                new ItemStack(NeoForgeModItems.LINKBOOK_UNLINKED.get(), 1),
                3, 15, 0.05f
        ));
        level4.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 16),
                new ItemStack(NeoForgeModItems.GLASSES.get(), 1),
                3, 15, 0.05f
        ));
        level4.add(new ArchivistTradeListings.RankedSymbolTrade(3, 2, 18));
        level4.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 20));

        // Level 5 trades (Master)
        List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
        level5.add(new ArchivistTradeListings.SymbolPageTrade(1, 25));
        level5.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 25));
        level5.add(new BasicItemListing(
                new ItemStack(NeoForgeModItems.LINKBOOK.get(), 1),
                new ItemStack(Items.EMERALD, 24),
                2, 30, 0.05f
        ));
        level5.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.TERRAIN, 1, 20));
        level5.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.BIOME, 1, 18));
    }

    private ArchivistTrades() {
    }
}
