package art.arcane.mystcraft.fabric.resource;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric wrapper for symbol reload listener (1.20.1).
 */
public class FabricSymbolReloadListener extends MystcraftSymbolReloadListener implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = new ResourceLocation(Mystcraft.MOD_ID, "datapack_symbols");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
