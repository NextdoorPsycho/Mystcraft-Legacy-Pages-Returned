package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Injects Mystcraft structures into village jigsaw pools. */
public final class FabricVillageStructureHandler {

    public static void onServerStarting(MinecraftServer server) {
        try {
            Registry<StructureTemplatePool> templatePools = server.registryAccess()
                    .registryOrThrow(Registries.TEMPLATE_POOL);

            addToPool(templatePools, "minecraft:village/plains/houses",
                    "mystcraft:village/plains/archivist_house", 2);
            addToPool(templatePools, "minecraft:village/desert/houses",
                    "mystcraft:village/desert/archivist_house", 2);
            addToPool(templatePools, "minecraft:village/savanna/houses",
                    "mystcraft:village/savanna/archivist_house", 2);
            addToPool(templatePools, "minecraft:village/taiga/houses",
                    "mystcraft:village/taiga/archivist_house", 2);
            addToPool(templatePools, "minecraft:village/snowy/houses",
                    "mystcraft:village/snowy/archivist_house", 2);

        } catch (Exception e) {
            Mystcraft.LOGGER.error("[Mystcraft] Failed to inject village structures", e);
        }
    }

    private static void addToPool(Registry<StructureTemplatePool> pools, String poolId, String pieceId, int weight) {
        StructureTemplatePool pool = pools.get(new ResourceLocation(poolId));
        if (pool == null) return;

        StructurePoolElement element = StructurePoolElement.legacy(pieceId).apply(StructureTemplatePool.Projection.RIGID);

        try {
            // Access the templates list via reflection (it's normally immutable)
            Field templatesField = StructureTemplatePool.class.getDeclaredField("templates");
            templatesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<StructurePoolElement> templates = (List<StructurePoolElement>) templatesField.get(pool);

            // The list might be immutable, so create a mutable copy
            List<StructurePoolElement> mutableTemplates = new ArrayList<>(templates);
            for (int i = 0; i < weight; i++) {
                mutableTemplates.add(element);
            }
            templatesField.set(pool, mutableTemplates);

            Mystcraft.LOGGER.debug("[Mystcraft] Added {} to pool {} with weight {}", pieceId, poolId, weight);
        } catch (Exception e) {
            Mystcraft.LOGGER.warn("[Mystcraft] Failed to add {} to pool {}: {}", pieceId, poolId, e.getMessage());
        }
    }

    private FabricVillageStructureHandler() {}
}
