package art.arcane.mystcraft.client;

import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.registry.FabricRegistries;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;

/** Registers block/item color handlers for Mystcraft blocks in Fabric (1.20.1). */
public final class FabricAgeBlockColorHandler {

    public static void register() {
        BlockColor crystalColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) return 0xFFFFFF;
            int ageUID = AgeColorUtils.getCurrentAgeUID();
            if (ageUID < 0) return 0xFFFFFF;

            int baseColor = ClientAgeDataCache.getSkyColor(ageUID);
            if (baseColor == -1) return 0xFFFFFF;
            return baseColor;
        };

        ColorProviderRegistry.BLOCK.register(crystalColor, FabricRegistries.CRYSTAL.get());
    }

    private FabricAgeBlockColorHandler() {}
}
