package art.arcane.mystcraft.villager;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Common trade listing implementations for the Archivist villager profession.
 * These implement vanilla VillagerTrades.ItemListing and can be used across all platforms.
 */
public final class ArchivistTradeListings {

    /** Calculates the emerald price for a symbol based on its rank. */
    public static int calculateSymbolPrice(IAgeSymbol symbol) {
        int rank = symbol.getCardRank() != null ? symbol.getCardRank() : 1;
        return 4 * (1 + rank);
    }

    /** Calculates the max uses for a symbol trade based on rarity. */
    public static int calculateMaxUses(IAgeSymbol symbol) {
        int rank = symbol.getCardRank() != null ? symbol.getCardRank() : 1;
        return Math.max(1, 5 - rank);
    }

    /** Calculates XP value for trading a symbol. */
    public static int calculateXpValue(IAgeSymbol symbol) {
        int rank = symbol.getCardRank() != null ? symbol.getCardRank() : 1;
        return 5 * rank;
    }

    /** Trade that gives a random symbol page with dynamic pricing based on rank. */
    public static class SymbolPageTrade implements VillagerTrades.ItemListing {
        private final int maxUses;
        private final int baseXpValue;

        public SymbolPageTrade(int maxUses, int baseXpValue) {
            this.maxUses = maxUses;
            this.baseXpValue = baseXpValue;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, @NotNull RandomSource random) {
            IAgeSymbol symbol = SymbolRegistry.getRandomWeighted(random);

            if (symbol == null) {
                return new MerchantOffer(
                        new ItemStack(Items.EMERALD, 4),
                        Page.createPage(),
                        maxUses,
                        baseXpValue,
                        0.05f
                );
            }

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

    /** Trade that gives a symbol page of a specific rank. */
    public static class RankedSymbolTrade implements VillagerTrades.ItemListing {
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
            List<IAgeSymbol> matchingSymbols = new ArrayList<>();
            for (IAgeSymbol symbol : SymbolRegistry.getAll()) {
                Integer rank = symbol.getCardRank();
                if (rank != null && rank == targetRank) {
                    matchingSymbols.add(symbol);
                }
            }

            if (matchingSymbols.isEmpty()) {
                IAgeSymbol anySymbol = SymbolRegistry.getRandomWeighted(random);
                if (anySymbol == null) {
                    return null;
                }
                matchingSymbols.add(anySymbol);
            }

            IAgeSymbol symbol = matchingSymbols.get(random.nextInt(matchingSymbols.size()));
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

    /** Trade that gives a symbol page of a specific category. */
    public static class CategorySymbolTrade implements VillagerTrades.ItemListing {
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
            List<IAgeSymbol> matchingSymbols = new ArrayList<>();
            for (IAgeSymbol symbol : SymbolRegistry.getAll()) {
                if (symbol.getCategory() == targetCategory) {
                    matchingSymbols.add(symbol);
                }
            }

            if (matchingSymbols.isEmpty()) {
                IAgeSymbol anySymbol = SymbolRegistry.getRandomWeighted(random);
                if (anySymbol == null) {
                    return null;
                }
                matchingSymbols.add(anySymbol);
            }

            IAgeSymbol symbol = matchingSymbols.get(random.nextInt(matchingSymbols.size()));
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

    private ArchivistTradeListings() {
    }
}
