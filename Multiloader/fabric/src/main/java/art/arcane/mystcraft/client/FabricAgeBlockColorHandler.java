package art.arcane.mystcraft.client;

import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.registry.FabricModBlocks;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;

/** Registers block/item color handlers for Mystcraft blocks in Fabric. */
public final class FabricAgeBlockColorHandler {

    public static void register() {
        BlockColor crystalColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) return 0xFFFFFF;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return 0xFFFFFF;
            if (!AgeDimensionFactory.isMystcraftAge(mc.level.dimension())) return 0xFFFFFF;

            int ageUID = getAgeUID();
            if (ageUID < 0) return 0xFFFFFF;

            int baseColor = ClientAgeDataCache.getSkyColor(ageUID);
            if (baseColor == -1) return 0xFFFFFF;
            return baseColor;
        };

        ColorProviderRegistry.BLOCK.register(crystalColor, FabricModBlocks.CRYSTAL.get());
    }

    private static int getAgeUID() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        String path = mc.level.dimension().location().getPath();
        if (path.startsWith("mystcraft_age_")) {
            try {
                return Integer.parseInt(path.substring("mystcraft_age_".length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private FabricAgeBlockColorHandler() {}
}
