package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.fabric.mixin.SinglePoolElementAccessor;
import art.arcane.mystcraft.fabric.mixin.StructureTemplatePoolAccessor;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/** Adds each style-specific Archivist house to its vanilla village pool. */
final class FabricVillageStructureHandler {

  private static final int WEIGHT = 2;
  private static final List<PoolAddition> ADDITIONS = List.of(
      addition("plains"),
      addition("desert"),
      addition("savanna"),
      addition("taiga"),
      addition("snowy"));

  private FabricVillageStructureHandler() {
  }

  static void inject(HolderLookup.Provider registries) {
    HolderLookup.RegistryLookup<StructureTemplatePool> pools =
        registries.lookupOrThrow(Registries.TEMPLATE_POOL);
    int injected = 0;
    int alreadyPresent = 0;

    for (PoolAddition addition : ADDITIONS) {
      ResourceKey<StructureTemplatePool> poolKey = ResourceKey.create(
          Registries.TEMPLATE_POOL, addition.poolId());
      StructureTemplatePool pool = pools.get(poolKey).map(holder -> holder.value()).orElse(null);
      if (pool == null) {
        Mystcraft.LOGGER.warn(
            "[Mystcraft] Fabric village template pool {} was not loaded", addition.poolId());
        continue;
      }

      StructureTemplatePoolAccessor accessor = (StructureTemplatePoolAccessor) pool;
      if (containsTemplate(accessor.mystcraft$getRawTemplates(), addition.templateId())) {
        alreadyPresent++;
        continue;
      }

      StructurePoolElement element = StructurePoolElement.legacy(addition.templateId().toString())
          .apply(StructureTemplatePool.Projection.RIGID);
      List<Pair<StructurePoolElement, Integer>> rawTemplates =
          new ArrayList<>(accessor.mystcraft$getRawTemplates());
      rawTemplates.add(Pair.of(element, WEIGHT));
      accessor.mystcraft$setRawTemplates(rawTemplates);

      ObjectArrayList<StructurePoolElement> expandedTemplates =
          new ObjectArrayList<>(accessor.mystcraft$getTemplates());
      for (int copy = 0; copy < WEIGHT; copy++) {
        expandedTemplates.add(element);
      }
      accessor.mystcraft$setTemplates(expandedTemplates);
      injected++;
    }

    Mystcraft.LOGGER.info(
        "[Mystcraft] Fabric Archivist houses: injected into {} village pools; "
            + "already present in {}",
        injected,
        alreadyPresent);
  }

  private static boolean containsTemplate(
      List<Pair<StructurePoolElement, Integer>> templates,
      Identifier templateId
  ) {
    for (Pair<StructurePoolElement, Integer> entry : templates) {
      StructurePoolElement element = entry.getFirst();
      if (element instanceof SinglePoolElement
          && ((SinglePoolElementAccessor) element).mystcraft$getTemplate()
              .left()
              .filter(templateId::equals)
              .isPresent()) {
        return true;
      }
    }
    return false;
  }

  private static PoolAddition addition(String style) {
    return new PoolAddition(
        Identifier.withDefaultNamespace("village/" + style + "/houses"),
        Identifier.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "village/" + style + "/archivist_house"));
  }

  private record PoolAddition(Identifier poolId, Identifier templateId) {
  }
}
