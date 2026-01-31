package art.arcane.mystcraft.loot;

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
 * Loot modifier that adds guidebooks to loot chests (1.18.2 version).
 */
public class GuidebookLootModifier extends LootModifier {

  private final float chance;

  public GuidebookLootModifier(LootItemCondition[] conditions, float chance) {
    super(conditions);
    this.chance = chance;
  }

  @Override
  protected @NotNull List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
    // 1.18.2 uses java.util.Random instead of RandomSource
    Random random = context.getRandom();
    if (random.nextFloat() <= chance) {
      generatedLoot.add(new ItemStack(ModItems.GUIDEBOOK.get()));
    }
    return generatedLoot;
  }

  /**
   * Serializer for 1.18.2 Forge loot modifier system.
   */
  public static class Serializer extends GlobalLootModifierSerializer<GuidebookLootModifier> {

    @Override
    public GuidebookLootModifier read(ResourceLocation location, JsonObject object, LootItemCondition[] conditions) {
      float chance = GsonHelper.getAsFloat(object, "chance", 0.1f);
      return new GuidebookLootModifier(conditions, chance);
    }

    @Override
    public JsonObject write(GuidebookLootModifier instance) {
      JsonObject json = makeConditions(instance.conditions);
      json.addProperty("chance", instance.chance);
      return json;
    }
  }
}
