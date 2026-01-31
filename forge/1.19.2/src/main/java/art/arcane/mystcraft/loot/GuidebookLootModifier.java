package art.arcane.mystcraft.loot;

import art.arcane.mystcraft.registry.ModItems;
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
 * Loot modifier that adds the Mystcraft Guidebook to loot chests with a configurable chance.
 */
public class GuidebookLootModifier extends LootModifier {

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

    // Add a guidebook to the loot
    generatedLoot.add(new ItemStack(ModItems.GUIDEBOOK.get()));

    return generatedLoot;
  }

  @Override
  public Codec<? extends IGlobalLootModifier> codec() {
    return CODEC.get();
  }
}
