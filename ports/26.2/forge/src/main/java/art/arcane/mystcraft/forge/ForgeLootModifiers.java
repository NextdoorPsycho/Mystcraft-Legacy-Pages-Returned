package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/** Forge global-loot modifiers for guidebooks, booster packs, and symbol pages. */
final class ForgeLootModifiers {

  private ForgeLootModifiers() {
  }

  static final class Guidebook extends LootModifier {
    static final MapCodec<Guidebook> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance)
            .and(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(value -> value.chance))
            .apply(instance, Guidebook::new));

    private final float chance;

    Guidebook(LootItemCondition[] conditions, float chance) {
      super(conditions);
      this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
        LootTable table, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      if (context.getRandom().nextFloat() <= chance) {
        generatedLoot.add(new ItemStack(ModItems.GUIDEBOOK.get()));
      }
      return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
      return MAP_CODEC;
    }
  }

  static final class BoosterPack extends LootModifier {
    static final MapCodec<BoosterPack> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance)
            .and(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(value -> value.chance))
            .and(Codec.intRange(1, 64).fieldOf("min_count").forGetter(value -> value.minCount))
            .and(Codec.intRange(1, 64).fieldOf("max_count").forGetter(value -> value.maxCount))
            .apply(instance, BoosterPack::new));

    private final float chance;
    private final int minCount;
    private final int maxCount;

    BoosterPack(LootItemCondition[] conditions, float chance, int minCount, int maxCount) {
      super(conditions);
      this.chance = chance;
      this.minCount = minCount;
      this.maxCount = maxCount;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
        LootTable table, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      if (!MystcraftConfig.enableBoosterLoot.get()
          || context.getRandom().nextFloat() > chance) {
        return generatedLoot;
      }
      int lower = Math.min(minCount, maxCount);
      int upper = Math.max(minCount, maxCount);
      int count = lower + context.getRandom().nextInt(upper - lower + 1);
      generatedLoot.add(new ItemStack(ModItems.BOOSTER_PACK.get(), count));
      return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
      return MAP_CODEC;
    }
  }

  static final class SymbolPage extends LootModifier {
    static final MapCodec<SymbolPage> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance)
            .and(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(value -> value.chance))
            .and(Codec.intRange(1, 64).fieldOf("min_pages").forGetter(value -> value.minPages))
            .and(Codec.intRange(1, 64).fieldOf("max_pages").forGetter(value -> value.maxPages))
            .apply(instance, SymbolPage::new));

    private final float chance;
    private final int minPages;
    private final int maxPages;

    SymbolPage(LootItemCondition[] conditions, float chance, int minPages, int maxPages) {
      super(conditions);
      this.chance = chance;
      this.minPages = minPages;
      this.maxPages = maxPages;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
        LootTable table, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      if (!MystcraftConfig.enablePageLoot.get()
          || context.getRandom().nextFloat() > chance) {
        return generatedLoot;
      }
      int lower = Math.min(minPages, maxPages);
      int upper = Math.max(minPages, maxPages);
      int count = lower + context.getRandom().nextInt(upper - lower + 1);
      for (int index = 0; index < count; index++) {
        IAgeSymbol symbol = SymbolRegistry.getRandomWeighted(context.getRandom());
        if (symbol != null) {
          generatedLoot.add(Page.createSymbolPage(symbol.getRegistryName()));
        }
      }
      return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
      return MAP_CODEC;
    }
  }
}
