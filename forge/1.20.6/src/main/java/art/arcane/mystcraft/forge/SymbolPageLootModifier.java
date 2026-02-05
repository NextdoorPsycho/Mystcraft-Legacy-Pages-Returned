package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.registry.ModItems;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
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
 * Loot modifier that adds symbol pages to loot chests.
 * 1.20.6 uses MapCodec-based serialization.
 */
public class SymbolPageLootModifier extends LootModifier {

  public static final Supplier<MapCodec<SymbolPageLootModifier>> CODEC = Suppliers.memoize(() ->
      RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
          .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
          .and(Codec.INT.fieldOf("min_count").forGetter(m -> m.minCount))
          .and(Codec.INT.fieldOf("max_count").forGetter(m -> m.maxCount))
          .apply(inst, SymbolPageLootModifier::new)));

  private final float chance;
  private final int minCount;
  private final int maxCount;

  public SymbolPageLootModifier(LootItemCondition[] conditions, float chance, int minCount, int maxCount) {
    super(conditions);
    this.chance = chance;
    this.minCount = minCount;
    this.maxCount = maxCount;
  }

  @Override
  protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
    if (!MystcraftConfig.enablePageLoot.get()) {
      return generatedLoot;
    }

    if (context.getRandom().nextFloat() > chance) {
      return generatedLoot;
    }

    int count = minCount + context.getRandom().nextInt(maxCount - minCount + 1);
    for (int i = 0; i < count; i++) {
      generatedLoot.add(Page.createPage());
    }

    if (MystcraftConfig.enableBoosterLoot.get()) {
      generatedLoot.add(new ItemStack(ModItems.BOOSTER_PACK.get(), 1));
    }

    return generatedLoot;
  }

  @Override
  public MapCodec<? extends IGlobalLootModifier> codec() {
    return CODEC.get();
  }
}
