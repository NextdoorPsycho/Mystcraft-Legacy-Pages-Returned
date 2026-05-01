package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Consolidated loot modifiers for Forge 1.20.1.
 */
public final class ForgeLootModifiers {

  private ForgeLootModifiers() {
  }

  public static class GuidebookLootModifier extends LootModifier {

    public static final Supplier<Codec<GuidebookLootModifier>> CODEC = Suppliers.memoize(() ->
        RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
            .apply(inst, GuidebookLootModifier::new)));

    private final float chance;

    public GuidebookLootModifier(LootItemCondition[] conditions, float chance) {
      super(conditions);
      this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      if (context.getRandom().nextFloat() > chance) {
        return generatedLoot;
      }

      generatedLoot.add(new ItemStack(ModItems.GUIDEBOOK.get()));

      return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
      return CODEC.get();
    }
  }

  public static class BoosterPackLootModifier extends LootModifier {

    public static final Supplier<Codec<BoosterPackLootModifier>> CODEC = Suppliers.memoize(() ->
        RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
            .and(Codec.INT.fieldOf("min_count").forGetter(m -> m.minCount))
            .and(Codec.INT.fieldOf("max_count").forGetter(m -> m.maxCount))
            .apply(inst, BoosterPackLootModifier::new)));

    private final float chance;
    private final int minCount;
    private final int maxCount;

    public BoosterPackLootModifier(LootItemCondition[] conditions, float chance, int minCount, int maxCount) {
      super(conditions);
      this.chance = chance;
      this.minCount = minCount;
      this.maxCount = maxCount;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {

      if (!MystcraftConfig.enableBoosterLoot.get()) {
        return generatedLoot;
      }

      if (context.getRandom().nextFloat() > chance) {
        return generatedLoot;
      }

      int count = minCount + context.getRandom().nextInt(maxCount - minCount + 1);
      ItemStack boosters = new ItemStack(ModItems.BOOSTER_PACK.get(), count);
      generatedLoot.add(boosters);

      return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
      return CODEC.get();
    }
  }

  public static class SymbolPageLootModifier extends LootModifier {

    public static final Supplier<Codec<SymbolPageLootModifier>> CODEC = Suppliers.memoize(() ->
        RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
            .and(Codec.INT.fieldOf("min_pages").forGetter(m -> m.minPages))
            .and(Codec.INT.fieldOf("max_pages").forGetter(m -> m.maxPages))
            .apply(inst, SymbolPageLootModifier::new)));

    private final float chance;
    private final int minPages;
    private final int maxPages;

    public SymbolPageLootModifier(LootItemCondition[] conditions, float chance, int minPages, int maxPages) {
      super(conditions);
      this.chance = chance;
      this.minPages = minPages;
      this.maxPages = maxPages;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {

      if (!MystcraftConfig.enablePageLoot.get()) {
        return generatedLoot;
      }

      if (context.getRandom().nextFloat() > chance) {
        return generatedLoot;
      }

      int pageCount = minPages + context.getRandom().nextInt(maxPages - minPages + 1);

      for (int i = 0; i < pageCount; i++) {
        IAgeSymbol symbol = SymbolRegistry.getRandomWeighted(context.getRandom());
        if (symbol != null) {
          ItemStack pageStack = new ItemStack(ModItems.PAGE.get());
          Page.setSymbol(pageStack, symbol.getRegistryName());
          generatedLoot.add(pageStack);
        }
      }

      return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
      return CODEC.get();
    }
  }
}
