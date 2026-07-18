package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.registry.ModLootFunctions;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.villager.ArchivistTrades;
import art.arcane.mystcraft.villager.SymbolPageLootFunction;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Focused codec and loaded-registry checks for the data-driven Archivist. */
final class ArchivistTradeAssertions {

  private ArchivistTradeAssertions() {
  }

  static void assertCodecContract() {
    SymbolPageLootFunction defaults = decode("{}");
    if (defaults.selection() != SymbolPageLootFunction.Selection.WEIGHTED
        || defaults.rank() != 1
        || defaults.category() != SymbolCategory.TERRAIN) {
      throw new IllegalStateException("Symbol-page loot function defaults changed");
    }

    SymbolPageLootFunction ranked = decode("{\"selection\":\"rank\",\"rank\":4}");
    if (ranked.selection() != SymbolPageLootFunction.Selection.RANK || ranked.rank() != 4) {
      throw new IllegalStateException("Symbol-page loot function lost its rank selection");
    }

    SymbolPageLootFunction categorized = decode(
        "{\"selection\":\"category\",\"category\":\"biome\"}"
    );
    if (categorized.selection() != SymbolPageLootFunction.Selection.CATEGORY
        || categorized.category() != SymbolCategory.BIOME) {
      throw new IllegalStateException("Symbol-page loot function lost its category selection");
    }

    assertDecodeFails("{\"selection\":\"rank\",\"rank\":0}");
    assertDecodeFails("{\"selection\":\"category\",\"category\":\"missing\"}");

    if (BuiltInRegistries.LOOT_FUNCTION_TYPE.getValue(ModLootFunctions.SYMBOL_PAGE_ID)
        != SymbolPageLootFunction.MAP_CODEC) {
      throw new IllegalStateException("mystcraft:symbol_page is not registered as a loot function");
    }

    List<ResourceKey<TradeSet>> expectedKeys = List.of(
        ArchivistTrades.LEVEL_1,
        ArchivistTrades.LEVEL_2,
        ArchivistTrades.LEVEL_3,
        ArchivistTrades.LEVEL_4,
        ArchivistTrades.LEVEL_5
    );
    if (!ArchivistTrades.ALL_LEVELS.equals(expectedKeys)
        || ArchivistTrades.TRADE_SETS_BY_LEVEL.size() != expectedKeys.size()) {
      throw new IllegalStateException("Archivist trade-set keys are incomplete");
    }
    for (int level = 1; level <= expectedKeys.size(); level++) {
      if (ArchivistTrades.forLevel(level) != expectedKeys.get(level - 1)) {
        throw new IllegalStateException("Archivist level " + level + " resolves to the wrong trade set");
      }
    }
    assertLevelFails(0);
    assertLevelFails(6);
  }

  static void assertTradeDataLoaded(GameTestHelper helper) {
    Registry<TradeSet> tradeSets = helper.getLevel().registryAccess()
        .lookupOrThrow(Registries.TRADE_SET);
    for (int level = 1; level <= 5; level++) {
      ResourceKey<TradeSet> key = ArchivistTrades.forLevel(level);
      TradeSet tradeSet = tradeSets.getValue(key);
      if (tradeSet == null) {
        helper.fail("Missing Archivist trade set " + key.identifier());
        return;
      }
      int actualCount = tradeSet.getTrades().size();
      int expectedCount = ArchivistTrades.expectedTradeCount(level);
      if (actualCount != expectedCount) {
        helper.fail(
            "Archivist trade set " + key.identifier() + " contains " + actualCount
                + " trades; expected " + expectedCount
        );
        return;
      }
    }
    assertDynamicPageOutput(helper);
  }

  private static void assertDynamicPageOutput(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    ArmorStand merchant = new ArmorStand(level, 0.5, 2.0, 0.5);
    LootParams params = new LootParams.Builder(level)
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(merchant.blockPosition()))
        .withParameter(LootContextParams.THIS_ENTITY, merchant)
        .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
        .create(LootContextParamSets.VILLAGER_TRADE);
    LootContext context = new LootContext.Builder(params)
        .withOptionalRandomSeed(26L)
        .create(Optional.empty());

    assertSelectedPage(
        helper,
        new SymbolPageLootFunction(
            SymbolPageLootFunction.Selection.CATEGORY,
            1,
            SymbolCategory.BIOME
        ).apply(Page.createPage(), context),
        SymbolCategory.BIOME,
        null
    );
    assertSelectedPage(
        helper,
        new SymbolPageLootFunction(
            SymbolPageLootFunction.Selection.RANK,
            1,
            SymbolCategory.TERRAIN
        ).apply(Page.createPage(), context),
        null,
        1
    );

    ItemStack fallback = new SymbolPageLootFunction(
        SymbolPageLootFunction.Selection.RANK,
        Integer.MAX_VALUE,
        SymbolCategory.TERRAIN
    ).apply(Page.createPage(), context);
    if (fallback.isEmpty() || Page.getSymbol(fallback) == null) {
      helper.fail("Symbol-page trade fallback did not produce a registered symbol page");
    }
  }

  private static void assertSelectedPage(
      GameTestHelper helper,
      ItemStack page,
      SymbolCategory expectedCategory,
      Integer expectedRank
  ) {
    IAgeSymbol symbol = SymbolRegistry.get(Page.getSymbol(page));
    if (symbol == null) {
      helper.fail("Symbol-page trade produced a blank or unknown page");
      return;
    }
    if (expectedCategory != null && symbol.getCategory() != expectedCategory) {
      helper.fail(
          "Symbol-page trade selected category " + symbol.getCategory()
              + "; expected " + expectedCategory
      );
      return;
    }
    if (expectedRank != null && !expectedRank.equals(symbol.getCardRank())) {
      helper.fail(
          "Symbol-page trade selected rank " + symbol.getCardRank()
              + "; expected " + expectedRank
      );
    }
  }

  private static SymbolPageLootFunction decode(String json) {
    JsonElement element = JsonParser.parseString(json);
    return SymbolPageLootFunction.MAP_CODEC.codec()
        .parse(JsonOps.INSTANCE, element)
        .getOrThrow();
  }

  private static void assertDecodeFails(String json) {
    JsonElement element = JsonParser.parseString(json);
    DataResult<SymbolPageLootFunction> result = SymbolPageLootFunction.MAP_CODEC.codec()
        .parse(JsonOps.INSTANCE, element);
    if (!result.isError()) {
      throw new IllegalStateException("Invalid symbol-page loot function decoded successfully: " + json);
    }
  }

  private static void assertLevelFails(int level) {
    try {
      ArchivistTrades.forLevel(level);
      throw new IllegalStateException("Invalid Archivist level resolved successfully: " + level);
    } catch (IllegalArgumentException expected) {
      // Expected: trade-set lookups are intentionally limited to villager levels 1-5.
    }
  }
}
