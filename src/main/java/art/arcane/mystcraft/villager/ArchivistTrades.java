package art.arcane.mystcraft.villager;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers trades for the Archivist villager profession.
 * Trades include various Mystcraft items at different levels.
 *
 * Symbol pricing formula: 4 * (1 + symbolRank) emeralds
 * - Rank 1: 8 emeralds
 * - Rank 2: 12 emeralds
 * - Rank 3: 16 emeralds
 * - Rank 4: 20 emeralds
 * - Rank 5+: 24+ emeralds
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArchivistTrades {

    // Cache for villager inventory restocking simulation (WeakHashMap to avoid memory leaks)
    private static final Map<Integer, Long> villagerRestockTimes = new WeakHashMap<>();
    private static final int RESTOCK_INTERVAL_TICKS = 1000;

    /**
     * Calculates the emerald price for a symbol based on its rank.
     * Pricing formula: 4 * (1 + symbolRank)
     */
    public static int calculateSymbolPrice(IAgeSymbol symbol) {
        int rank = symbol.getCardRank() != null ? symbol.getCardRank() : 1;
        return 4 * (1 + rank);
    }

    /**
     * Calculates the max uses for a symbol trade based on rarity.
     */
    public static int calculateMaxUses(IAgeSymbol symbol) {
        int rank = symbol.getCardRank() != null ? symbol.getCardRank() : 1;
        // Rarer symbols have fewer available trades
        return Math.max(1, 5 - rank);
    }

    /**
     * Calculates XP value for trading a symbol.
     */
    public static int calculateXpValue(IAgeSymbol symbol) {
        int rank = symbol.getCardRank() != null ? symbol.getCardRank() : 1;
        return 5 * rank;
    }

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
        // Sell: Rank 1 symbol pages (common)
        level1.add(new RankedSymbolTrade(1, 2, 5));

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
        // Sell: Rank 1-2 symbol pages
        level2.add(new RankedSymbolTrade(1, 2, 8));
        level2.add(new RankedSymbolTrade(2, 1, 10));

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
        // Sell: Rank 2-3 symbol pages
        level3.add(new RankedSymbolTrade(2, 2, 12));
        level3.add(new RankedSymbolTrade(3, 1, 15));

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
        // Sell: Rank 3-4 symbol pages
        level4.add(new RankedSymbolTrade(3, 2, 18));
        level4.add(new RankedSymbolTrade(4, 1, 20));

        // Level 5 trades (Master)
        List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
        // Sell: Random symbol pages (any rank, dynamically priced)
        level5.add(new SymbolPageTrade(1, 25));
        // Sell: High-rank symbol pages
        level5.add(new RankedSymbolTrade(4, 1, 25));
        // Buy: Linkbooks for high emeralds
        level5.add(new BasicItemListing(
                new ItemStack(ModItems.LINKBOOK.get(), 1),
                new ItemStack(Items.EMERALD, 24),
                2, 30, 0.05f
        ));
        // Sell: Category-specific symbol trades
        level5.add(new CategorySymbolTrade(SymbolCategory.TERRAIN, 1, 20));
        level5.add(new CategorySymbolTrade(SymbolCategory.BIOME, 1, 18));
    }

    /**
     * Special trade that gives a random symbol page with dynamic pricing based on rank.
     * Uses the formula: 4 * (1 + symbolRank) emeralds
     */
    private static class SymbolPageTrade implements VillagerTrades.ItemListing {
        private final int maxUses;
        private final int baseXpValue;

        public SymbolPageTrade(int maxUses, int baseXpValue) {
            this.maxUses = maxUses;
            this.baseXpValue = baseXpValue;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, @NotNull RandomSource random) {
            // Get a random symbol using weighted selection
            IAgeSymbol symbol = SymbolRegistry.getRandomWeighted(random);

            if (symbol == null) {
                // Fallback to blank page if no symbols registered
                return new MerchantOffer(
                        new ItemStack(Items.EMERALD, 4),
                        Page.createPage(),
                        maxUses,
                        baseXpValue,
                        0.05f
                );
            }

            // Calculate price based on symbol rank
            int emeraldCost = calculateSymbolPrice(symbol);
            int xpValue = calculateXpValue(symbol);
            int uses = calculateMaxUses(symbol);

            ItemStack pageStack = Page.createSymbolPage(symbol.getRegistryName());

            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    pageStack,
                    uses,
                    xpValue,
                    0.05f
            );
        }
    }

    /**
     * Trade that gives a symbol page of a specific rank.
     */
    private static class RankedSymbolTrade implements VillagerTrades.ItemListing {
        private final int targetRank;
        private final int maxUses;
        private final int xpValue;

        public RankedSymbolTrade(int targetRank, int maxUses, int xpValue) {
            this.targetRank = targetRank;
            this.maxUses = maxUses;
            this.xpValue = xpValue;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, @NotNull RandomSource random) {
            // Get symbols of the target rank
            List<IAgeSymbol> matchingSymbols = new ArrayList<>();
            for (IAgeSymbol symbol : SymbolRegistry.getAll()) {
                Integer rank = symbol.getCardRank();
                if (rank != null && rank == targetRank) {
                    matchingSymbols.add(symbol);
                }
            }

            if (matchingSymbols.isEmpty()) {
                // Fallback: try any symbol
                IAgeSymbol anySymbol = SymbolRegistry.getRandomWeighted(random);
                if (anySymbol == null) {
                    return null;
                }
                matchingSymbols.add(anySymbol);
            }

            // Pick a random symbol from the matching ones
            IAgeSymbol symbol = matchingSymbols.get(random.nextInt(matchingSymbols.size()));

            // Calculate price based on rank
            int emeraldCost = calculateSymbolPrice(symbol);

            ItemStack pageStack = Page.createSymbolPage(symbol.getRegistryName());

            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    pageStack,
                    maxUses,
                    xpValue,
                    0.05f
            );
        }
    }

    /**
     * Trade that gives a symbol page of a specific category.
     */
    private static class CategorySymbolTrade implements VillagerTrades.ItemListing {
        private final SymbolCategory targetCategory;
        private final int maxUses;
        private final int xpValue;

        public CategorySymbolTrade(SymbolCategory targetCategory, int maxUses, int xpValue) {
            this.targetCategory = targetCategory;
            this.maxUses = maxUses;
            this.xpValue = xpValue;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, @NotNull RandomSource random) {
            // Get symbols of the target category
            List<IAgeSymbol> matchingSymbols = new ArrayList<>();
            for (IAgeSymbol symbol : SymbolRegistry.getAll()) {
                if (symbol.getCategory() == targetCategory) {
                    matchingSymbols.add(symbol);
                }
            }

            if (matchingSymbols.isEmpty()) {
                // Fallback: try any symbol
                IAgeSymbol anySymbol = SymbolRegistry.getRandomWeighted(random);
                if (anySymbol == null) {
                    return null;
                }
                matchingSymbols.add(anySymbol);
            }

            // Pick a random symbol from the matching ones
            IAgeSymbol symbol = matchingSymbols.get(random.nextInt(matchingSymbols.size()));

            // Calculate price based on rank
            int emeraldCost = calculateSymbolPrice(symbol);

            ItemStack pageStack = Page.createSymbolPage(symbol.getRegistryName());

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
