package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.forge.mixin.StructureTemplatePoolAccessor;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * Adds the style-specific Archivist house to each vanilla village house pool.
 *
 * <p>The template-pool registry is datapack-backed in 26.2, so this runs as a
 * reload listener rather than only once during server startup. That keeps the
 * injected entries present after {@code /reload} without replacing any
 * vanilla or third-party pool entries.</p>
 */
final class ForgeVillageStructureHandler implements PreparableReloadListener {

  private static final int WEIGHT = 2;
  private static final List<PoolAddition> ADDITIONS = List.of(
      addition("plains"),
      addition("desert"),
      addition("savanna"),
      addition("taiga"),
      addition("snowy")
  );

  private final HolderLookup.Provider registries;

  ForgeVillageStructureHandler(HolderLookup.Provider registries) {
    this.registries = registries;
  }

  @Override
  public CompletableFuture<Void> reload(
      SharedState currentReload,
      Executor taskExecutor,
      PreparationBarrier preparationBarrier,
      Executor reloadExecutor) {
    return preparationBarrier.wait(registries)
        .thenAcceptAsync(ignored -> injectArchivistHouses(), reloadExecutor);
  }

  @Override
  public String getName() {
    return "Mystcraft Archivist village pool injection";
  }

  private void injectArchivistHouses() {
    HolderLookup.RegistryLookup<StructureTemplatePool> pools =
        registries.lookupOrThrow(Registries.TEMPLATE_POOL);
    int injected = 0;

    for (PoolAddition addition : ADDITIONS) {
      ResourceKey<StructureTemplatePool> poolKey = ResourceKey.create(
          Registries.TEMPLATE_POOL, Identifier.parse(addition.poolId()));
      StructureTemplatePool pool = pools.get(poolKey).map(holder -> holder.value()).orElse(null);
      if (pool == null) {
        Mystcraft.LOGGER.warn("[Mystcraft] Village template pool {} was not loaded",
            addition.poolId());
        continue;
      }

      StructurePoolElement element = StructurePoolElement.legacy(addition.templateId())
          .apply(StructureTemplatePool.Projection.RIGID);
      StructureTemplatePoolAccessor accessor = (StructureTemplatePoolAccessor) pool;

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
        "[Mystcraft] Injected style-specific Archivist houses into {} village pools",
        injected);
  }

  private static PoolAddition addition(String style) {
    return new PoolAddition(
        "minecraft:village/" + style + "/houses",
        Mystcraft.MOD_ID + ":village/" + style + "/archivist_house");
  }

  private record PoolAddition(String poolId, String templateId) {
  }
}
