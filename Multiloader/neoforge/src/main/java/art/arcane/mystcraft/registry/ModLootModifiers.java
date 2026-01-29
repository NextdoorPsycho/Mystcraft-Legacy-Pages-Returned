package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.loot.BoosterPackLootModifier;
import art.arcane.mystcraft.loot.GuidebookLootModifier;
import art.arcane.mystcraft.loot.SymbolPageLootModifier;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registry for Mystcraft loot modifiers.
 */
public final class ModLootModifiers {

    public static final DeferredHolder<Codec<? extends IGlobalLootModifier>, Codec<SymbolPageLootModifier>> SYMBOL_PAGE =
            MystcraftRegistries.LOOT_MODIFIERS.register("symbol_page", SymbolPageLootModifier.CODEC);

    public static final DeferredHolder<Codec<? extends IGlobalLootModifier>, Codec<GuidebookLootModifier>> GUIDEBOOK =
            MystcraftRegistries.LOOT_MODIFIERS.register("guidebook", GuidebookLootModifier.CODEC);

    public static final DeferredHolder<Codec<? extends IGlobalLootModifier>, Codec<BoosterPackLootModifier>> BOOSTER_PACK =
            MystcraftRegistries.LOOT_MODIFIERS.register("booster_pack", BoosterPackLootModifier.CODEC);

    public static void register() {
    }

    private ModLootModifiers() {
    }
}
