package art.arcane.mystcraft.loot;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.registry.MystcraftRegistries;
import art.arcane.mystcraft.symbol.SymbolRegistry;
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

/** Loot modifier that adds random symbol pages to loot chests. */
public class SymbolPageLootModifier extends LootModifier {

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
        if (context.getRandom().nextFloat() > chance) {
            return generatedLoot;
        }

        int pageCount = minPages + context.getRandom().nextInt(maxPages - minPages + 1);

        for (int i = 0; i < pageCount; i++) {
            IAgeSymbol symbol = SymbolRegistry.getRandomWeighted(context.getRandom());
            if (symbol != null) {
                ItemStack pageStack = new ItemStack(MystcraftRegistries.PAGE.get());
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
