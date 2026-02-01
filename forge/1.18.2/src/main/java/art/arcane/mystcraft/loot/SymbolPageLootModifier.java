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
 * Loot modifier that adds blank pages to loot chests (1.18.2 version).
 * Simplified version that adds blank pages instead of symbol pages.
 */
public class SymbolPageLootModifier extends LootModifier {

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
  protected @NotNull List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
    if (!MystcraftConfig.enablePageLoot.get()) {
      return generatedLoot;
    }

    // 1.18.2 uses java.util.Random instead of RandomSource
    Random random = context.getRandom();
    if (random.nextFloat() > chance) {
      return generatedLoot;
    }

    // Determine how many pages to add
    int pageCount = minPages + random.nextInt(maxPages - minPages + 1);

    // Add blank pages - simplified for 1.18.2 without SymbolRegistry
    for (int i = 0; i < pageCount; i++) {
      ItemStack pageStack = new ItemStack(ModItems.PAGE.get());
      generatedLoot.add(pageStack);
    }

    return generatedLoot;
  }

  /**
   * Serializer for 1.18.2 Forge loot modifier system.
   */
  public static class Serializer extends GlobalLootModifierSerializer<SymbolPageLootModifier> {

    @Override
    public SymbolPageLootModifier read(ResourceLocation location, JsonObject object, LootItemCondition[] conditions) {
      float chance = GsonHelper.getAsFloat(object, "chance", 0.3f);
      int minPages = GsonHelper.getAsInt(object, "min_pages", 1);
      int maxPages = GsonHelper.getAsInt(object, "max_pages", 3);
      return new SymbolPageLootModifier(conditions, chance, minPages, maxPages);
    }

    @Override
    public JsonObject write(SymbolPageLootModifier instance) {
      JsonObject json = makeConditions(instance.conditions);
      json.addProperty("chance", instance.chance);
      json.addProperty("min_pages", instance.minPages);
      json.addProperty("max_pages", instance.maxPages);
      return json;
    }
  }
}
