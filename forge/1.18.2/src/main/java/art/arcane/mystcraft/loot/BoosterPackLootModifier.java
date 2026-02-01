package art.arcane.mystcraft.loot;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.registry.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * Loot modifier that adds booster packs to loot chests (1.18.2 version).
 */
public class BoosterPackLootModifier extends LootModifier {

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
  protected @NotNull List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
    if (!MystcraftConfig.enableBoosterLoot.get()) {
      return generatedLoot;
    }

    // 1.18.2 uses java.util.Random instead of RandomSource
    Random random = context.getRandom();
    if (random.nextFloat() > chance) {
      return generatedLoot;
    }

    // Determine how many booster packs to add
    int count = minCount + random.nextInt(maxCount - minCount + 1);

    for (int i = 0; i < count; i++) {
      generatedLoot.add(new ItemStack(ModItems.BOOSTER_PACK.get()));
    }

    return generatedLoot;
  }

  /**
   * Serializer for 1.18.2 Forge loot modifier system.
   */
  public static class Serializer extends GlobalLootModifierSerializer<BoosterPackLootModifier> {

    @Override
    public BoosterPackLootModifier read(ResourceLocation location, JsonObject object, LootItemCondition[] conditions) {
      float chance = GsonHelper.getAsFloat(object, "chance", 0.15f);
      int minCount = GsonHelper.getAsInt(object, "min_count", 1);
      int maxCount = GsonHelper.getAsInt(object, "max_count", 2);
      return new BoosterPackLootModifier(conditions, chance, minCount, maxCount);
    }

    @Override
    public JsonObject write(BoosterPackLootModifier instance) {
      JsonObject json = makeConditions(instance.conditions);
      json.addProperty("chance", instance.chance);
      json.addProperty("min_count", instance.minCount);
      json.addProperty("max_count", instance.maxCount);
      return json;
    }
  }
}
