package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Injects the Mystcraft archivist house into vanilla village jigsaw pools at server start.
 * Uses reflection to access private fields (AT ensures this works at runtime).
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillageStructureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillageStructureHandler.class);

    private static final String ARCHIVIST_HOUSE_TEMPLATE =
            Mystcraft.MOD_ID + ":village/archivist_house";

    // Vanilla village house pools to inject into
    private static final ResourceLocation[] VILLAGE_POOLS = {
            new ResourceLocation("minecraft", "village/plains/houses"),
            new ResourceLocation("minecraft", "village/desert/houses"),
            new ResourceLocation("minecraft", "village/savanna/houses"),
            new ResourceLocation("minecraft", "village/snowy/houses"),
            new ResourceLocation("minecraft", "village/taiga/houses")
    };

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Registry<StructureTemplatePool> poolRegistry =
                server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processorRegistry =
                server.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);

        Holder<StructureProcessorList> emptyProcessor =
                processorRegistry.getHolderOrThrow(ResourceKey.create(
                        Registries.PROCESSOR_LIST,
                        new ResourceLocation("minecraft", "empty")));

        int injected = 0;
        for (ResourceLocation poolId : VILLAGE_POOLS) {
            StructureTemplatePool pool = poolRegistry.get(poolId);
            if (pool == null) {
                continue;
            }

            try {
                StructurePoolElement element = SinglePoolElement.single(
                        ARCHIVIST_HOUSE_TEMPLATE, emptyProcessor
                ).apply(StructureTemplatePool.Projection.RIGID);

                // Access fields via reflection (AT makes this work at runtime)
                Field rawTemplatesField = findField(StructureTemplatePool.class, "rawTemplates");
                Field templatesField = findField(StructureTemplatePool.class, "templates");

                if (rawTemplatesField == null || templatesField == null) {
                    LOGGER.warn("[Mystcraft] Could not find pool fields for {}", poolId);
                    continue;
                }

                rawTemplatesField.setAccessible(true);
                templatesField.setAccessible(true);

                @SuppressWarnings("unchecked")
                List<Pair<StructurePoolElement, Integer>> rawTemplates =
                        (List<Pair<StructurePoolElement, Integer>>) rawTemplatesField.get(pool);
                @SuppressWarnings("unchecked")
                ObjectArrayList<StructurePoolElement> templates =
                        (ObjectArrayList<StructurePoolElement>) templatesField.get(pool);

                List<Pair<StructurePoolElement, Integer>> newRawTemplates = new ArrayList<>(rawTemplates);
                newRawTemplates.add(Pair.of(element, 2));
                rawTemplatesField.set(pool, newRawTemplates);

                ObjectArrayList<StructurePoolElement> newTemplates = new ObjectArrayList<>(templates);
                for (int i = 0; i < 2; i++) {
                    newTemplates.add(element);
                }
                templatesField.set(pool, newTemplates);

                injected++;
            } catch (Exception e) {
                LOGGER.warn("[Mystcraft] Failed to inject archivist house into pool {}: {}",
                        poolId, e.getMessage());
            }
        }

        if (injected > 0) {
            LOGGER.info("[Mystcraft] Injected archivist house into {} village pools", injected);
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
            if (field.getType() == List.class && name.equals("rawTemplates")) {
                return field;
            }
            if (field.getType() == ObjectArrayList.class && name.equals("templates")) {
                return field;
            }
        }
        return null;
    }
}
