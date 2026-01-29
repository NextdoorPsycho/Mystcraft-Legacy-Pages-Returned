package art.arcane.mystcraft.loot;

import art.arcane.mystcraft.registry.NeoForgeModItems;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Loot modifier that adds booster packs to loot chests.
 */
public class BoosterPackLootModifier extends LootModifier {

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
        if (context.getRandom().nextFloat() > chance) {
            return generatedLoot;
        }

        int count = minCount + context.getRandom().nextInt(maxCount - minCount + 1);
        ItemStack boosters = new ItemStack(NeoForgeModItems.BOOSTER_PACK.get(), count);
        generatedLoot.add(boosters);

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
