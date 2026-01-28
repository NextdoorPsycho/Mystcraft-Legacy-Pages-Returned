package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.loot.GuidebookLootModifier;
import art.arcane.mystcraft.loot.SymbolPageLootModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for Mystcraft loot modifiers.
 */
public final class ModLootModifiers {

    public static final RegistryObject<Codec<SymbolPageLootModifier>> SYMBOL_PAGE =
            MystcraftRegistries.LOOT_MODIFIERS.register("symbol_page", SymbolPageLootModifier.CODEC);

    public static final RegistryObject<Codec<GuidebookLootModifier>> GUIDEBOOK =
            MystcraftRegistries.LOOT_MODIFIERS.register("guidebook", GuidebookLootModifier.CODEC);

    public static void register() {
    }

    private ModLootModifiers() {
    }
}
