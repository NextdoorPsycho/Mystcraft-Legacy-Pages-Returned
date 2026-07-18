package art.arcane.mystcraft.villager;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/**
 * Converts a trade's base Page stack into a dynamically selected symbol page.
 * Selection happens when the villager offer is generated, matching the old
 * runtime ItemListing behavior while keeping the trade itself data-driven.
 */
public final class SymbolPageLootFunction implements LootItemFunction {
  private static final Codec<SymbolCategory> CATEGORY_CODEC = Codec.STRING.comapFlatMap(
      name -> {
        SymbolCategory category = SymbolCategory.fromName(name);
        return category == null
            ? DataResult.error(() -> "Unknown Mystcraft symbol category: " + name)
            : DataResult.success(category);
      },
      SymbolCategory::getName
  );

  public static final MapCodec<SymbolPageLootFunction> MAP_CODEC =
      RecordCodecBuilder.mapCodec(instance -> instance.group(
          Selection.CODEC.optionalFieldOf("selection", Selection.WEIGHTED)
              .forGetter(SymbolPageLootFunction::selection),
          Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("rank", 1)
              .forGetter(SymbolPageLootFunction::rank),
          CATEGORY_CODEC.optionalFieldOf("category", SymbolCategory.TERRAIN)
              .forGetter(SymbolPageLootFunction::category)
      ).apply(instance, SymbolPageLootFunction::new));

  private final Selection selection;
  private final int rank;
  private final SymbolCategory category;

  public SymbolPageLootFunction(Selection selection, int rank, SymbolCategory category) {
    this.selection = selection;
    this.rank = rank;
    this.category = category;
  }

  public Selection selection() {
    return selection;
  }

  public int rank() {
    return rank;
  }

  public SymbolCategory category() {
    return category;
  }

  @Override
  public MapCodec<SymbolPageLootFunction> codec() {
    return MAP_CODEC;
  }

  @Override
  public ItemStack apply(ItemStack input, LootContext context) {
    IAgeSymbol symbol = selectSymbol(context.getRandom());
    return symbol == null ? Page.createPage() : Page.createSymbolPage(symbol.getRegistryName());
  }

  IAgeSymbol selectSymbol(RandomSource random) {
    if (selection == Selection.WEIGHTED) {
      return SymbolRegistry.getRandomWeighted(random);
    }

    List<IAgeSymbol> matches = new ArrayList<>();
    for (IAgeSymbol symbol : SymbolRegistry.getAll()) {
      if (selection == Selection.RANK) {
        Integer symbolRank = symbol.getCardRank();
        if (symbolRank != null && symbolRank == rank) {
          matches.add(symbol);
        }
      } else if (symbol.getCategory() == category) {
        matches.add(symbol);
      }
    }

    if (matches.isEmpty()) {
      return SymbolRegistry.getRandomWeighted(random);
    }
    matches.sort(Comparator.comparing(symbol -> symbol.getRegistryName().toString()));
    return matches.get(random.nextInt(matches.size()));
  }

  public enum Selection implements StringRepresentable {
    WEIGHTED("weighted"),
    RANK("rank"),
    CATEGORY("category");

    public static final Codec<Selection> CODEC = StringRepresentable.fromEnum(Selection::values);

    private final String serializedName;

    Selection(String serializedName) {
      this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
      return serializedName;
    }
  }
}
