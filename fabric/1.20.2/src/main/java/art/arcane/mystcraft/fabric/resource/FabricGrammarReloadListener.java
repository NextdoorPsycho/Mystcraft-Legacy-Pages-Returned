package art.arcane.mystcraft.fabric.resource;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric wrapper for grammar reload listener.
 */
public class FabricGrammarReloadListener extends MystcraftGrammarReloadListener implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "datapack_grammar");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
