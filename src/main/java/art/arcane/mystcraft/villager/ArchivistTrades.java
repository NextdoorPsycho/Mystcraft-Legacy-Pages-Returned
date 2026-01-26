package art.arcane.mystcraft.villager;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.registry.ModVillagers;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registers trades for the Archivist villager profession.
 * Trades include various Mystcraft items at different levels.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArchivistTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.ARCHIVIST.get()) {
            return;
        }

        Mystcraft.LOGGER.debug("Registering Archivist trades");

        // Level 1 trades (Novice)
        List<VillagerTrades.ItemListing> level1 = event.getTrades().get(1);
        // Buy: Emeralds for ink vials
        level1.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 2),
                new ItemStack(ModItems.INK_VIAL.get(), 1),
                12, 1, 0.05f
        ));
        // Buy: Pages for emeralds
        level1.add(new BasicItemListing(
                new ItemStack(ModItems.PAGE.get(), 8),
                new ItemStack(Items.EMERALD, 1),
                16, 2, 0.05f
        ));
        // Sell: Basic pages
        level1.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 1),
                new ItemStack(ModItems.PAGE.get(), 4),
                16, 1, 0.05f
        ));

        // Level 2 trades (Apprentice)
        List<VillagerTrades.ItemListing> level2 = event.getTrades().get(2);
        // Sell: Folder
        level2.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 5),
                new ItemStack(ModItems.FOLDER.get(), 1),
                8, 5, 0.05f
        ));
        // Sell: More ink vials
        level2.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 3),
                new ItemStack(ModItems.INK_VIAL.get(), 2),
                12, 5, 0.05f
        ));

        // Level 3 trades (Journeyman)
        List<VillagerTrades.ItemListing> level3 = event.getTrades().get(3);
        // Sell: Portfolio
        level3.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 12),
                new ItemStack(ModItems.PORTFOLIO.get(), 1),
                4, 10, 0.05f
        ));
        // Sell: Booster Pack
        level3.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 8),
                new ItemStack(ModItems.BOOSTER_PACK.get(), 1),
                6, 10, 0.05f
        ));

        // Level 4 trades (Expert)
        List<VillagerTrades.ItemListing> level4 = event.getTrades().get(4);
        // Sell: Unlinked Linkbook
        level4.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 20),
                new ItemStack(ModItems.LINKBOOK_UNLINKED.get(), 1),
                3, 15, 0.05f
        ));
        // Sell: Glasses
        level4.add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 16),
                new ItemStack(ModItems.GLASSES.get(), 1),
                3, 15, 0.05f
        ));

        // Level 5 trades (Master)
        List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
        // Sell: Random symbol pages (special trade)
        level5.add(new SymbolPageTrade(32, 1, 20));
        // Buy: Linkbooks for high emeralds
        level5.add(new BasicItemListing(
                new ItemStack(ModItems.LINKBOOK.get(), 1),
                new ItemStack(Items.EMERALD, 24),
                2, 30, 0.05f
        ));
    }

    /**
     * Special trade that gives a random symbol page.
     */
    private static class SymbolPageTrade implements VillagerTrades.ItemListing {
        private final int emeraldCost;
        private final int maxUses;
        private final int xpValue;

        public SymbolPageTrade(int emeraldCost, int maxUses, int xpValue) {
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.xpValue = xpValue;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, @NotNull RandomSource random) {
            // Get a random symbol using weighted selection
            IAgeSymbol symbol = SymbolRegistry.getRandomWeighted(random);

            ItemStack pageStack;
            if (symbol != null) {
                pageStack = Page.createSymbolPage(symbol.getRegistryName());
            } else {
                // Fallback to blank page if no symbols registered
                pageStack = Page.createPage();
            }

            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    pageStack,
                    maxUses,
                    xpValue,
                    0.05f
            );
        }
    }

    private ArchivistTrades() {
    }
}
