package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Injects Mystcraft structures into village jigsaw pools.
 * Uses reflection to access private fields (AW ensures this works at runtime).
 */
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
            // Access fields via reflection (AW makes this work at runtime)
            Field templatesField = findField(StructureTemplatePool.class, "templates");
            if (templatesField == null) {
                Mystcraft.LOGGER.warn("[Mystcraft] Could not find templates field for pool {}", poolId);
                return;
            }

            templatesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            ObjectArrayList<StructurePoolElement> templates =
                    (ObjectArrayList<StructurePoolElement>) templatesField.get(pool);

            ObjectArrayList<StructurePoolElement> newTemplates = new ObjectArrayList<>(templates);
            for (int i = 0; i < weight; i++) {
                newTemplates.add(element);
            }
            templatesField.set(pool, newTemplates);

            Mystcraft.LOGGER.debug("[Mystcraft] Added {} to pool {} with weight {}", pieceId, poolId, weight);
        } catch (Exception e) {
            Mystcraft.LOGGER.warn("[Mystcraft] Failed to add {} to pool {}: {}", pieceId, poolId, e.getMessage());
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        // Fallback: search by type for obfuscated environments
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == ObjectArrayList.class && name.equals("templates")) {
                return field;
            }
        }
        return null;
    }

    private FabricVillageStructureHandler() {}
}
