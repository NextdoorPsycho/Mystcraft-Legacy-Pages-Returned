package art.arcane.mystcraft.villager;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.registry.FabricModItems;
import art.arcane.mystcraft.registry.ModVillagers;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

public final class FabricArchivistTrades {

    public static void register() {
        Mystcraft.LOGGER.debug("[Mystcraft] Registering Archivist trades");

        // Level 1 (Novice)
        TradeOfferHelper.registerVillagerOffers(ModVillagers.ARCHIVIST.get(), 1, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2), new ItemStack(FabricModItems.INK_VIAL.get(), 1), 12, 1, 0.05f));
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(FabricModItems.PAGE.get(), 8), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1), new ItemStack(FabricModItems.PAGE.get(), 4), 16, 1, 0.05f));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 5));
        });

        // Level 2 (Apprentice)
        TradeOfferHelper.registerVillagerOffers(ModVillagers.ARCHIVIST.get(), 2, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5), new ItemStack(FabricModItems.FOLDER.get(), 1), 8, 5, 0.05f));
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3), new ItemStack(FabricModItems.INK_VIAL.get(), 2), 12, 5, 0.05f));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 8));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(2, 1, 10));
        });

        // Level 3 (Journeyman)
        TradeOfferHelper.registerVillagerOffers(ModVillagers.ARCHIVIST.get(), 3, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 12), new ItemStack(FabricModItems.PORTFOLIO.get(), 1), 4, 10, 0.05f));
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 8), new ItemStack(FabricModItems.BOOSTER_PACK.get(), 1), 6, 10, 0.05f));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(2, 2, 12));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(3, 1, 15));
        });

        // Level 4 (Expert)
        TradeOfferHelper.registerVillagerOffers(ModVillagers.ARCHIVIST.get(), 4, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 20), new ItemStack(FabricModItems.LINKBOOK_UNLINKED.get(), 1), 3, 15, 0.05f));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(3, 2, 18));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 20));
        });

        // Level 5 (Master)
        TradeOfferHelper.registerVillagerOffers(ModVillagers.ARCHIVIST.get(), 5, factories -> {
            factories.add(new ArchivistTradeListings.SymbolPageTrade(1, 25));
            factories.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 25));
            factories.add((entity, random) -> new MerchantOffer(
                    new ItemStack(FabricModItems.LINKBOOK.get(), 1), new ItemStack(Items.EMERALD, 24), 2, 30, 0.05f));
            factories.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.TERRAIN, 1, 20));
            factories.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.BIOME, 1, 18));
        });
    }

    private FabricArchivistTrades() {}
}
