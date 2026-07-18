package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.villager.SymbolPageLootFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/** Adds Mystcraft guidebooks, booster packs, and real symbol pages to vanilla loot tables. */
public final class FabricLootModifiers {

  private static final Map<ResourceKey<LootTable>, List<LootInjection>> INJECTIONS =
      createInjections();

  private static boolean registered;

  private FabricLootModifiers() {
  }

  /** Registers the Fabric loot callback exactly once. */
  public static synchronized void register() {
    if (registered) {
      return;
    }
    registered = true;

    LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
      if (!source.isBuiltin()) {
        return;
      }

      List<LootInjection> injections = INJECTIONS.get(key);
      if (injections == null) {
        return;
      }
      for (LootInjection injection : injections) {
        if (injection.enabled()) {
          tableBuilder.withPool(injection.createPool());
        }
      }
    });

    Mystcraft.LOGGER.info(
        "[Mystcraft] Registered Fabric loot injections for {} vanilla tables",
        INJECTIONS.size());
  }

  private static Map<ResourceKey<LootTable>, List<LootInjection>> createInjections() {
    Map<ResourceKey<LootTable>, List<LootInjection>> injections = new LinkedHashMap<>();

    add(injections, "chests/simple_dungeon", page(0.50F, 1, 3));
    add(injections, "chests/abandoned_mineshaft", page(0.30F, 1, 2));
    add(injections, "chests/stronghold_library", page(0.80F, 2, 5));
    add(injections, "entities/pillager", pageFromPlayer(0.05F, 1, 1));
    add(injections, "chests/ancient_city", page(0.65F, 1, 4));
    add(injections, "chests/shipwreck_treasure", page(0.35F, 1, 2));
    add(injections, "chests/buried_treasure", page(0.40F, 1, 2));
    add(injections, "chests/end_city_treasure", page(0.50F, 1, 3));
    add(injections, "chests/nether_bridge", page(0.40F, 1, 2));
    add(injections, "chests/ruined_portal", page(0.30F, 1, 2));

    add(injections, "chests/stronghold_library", guidebook(0.25F));

    add(injections, "chests/simple_dungeon", booster(0.35F, 1, 2));
    add(injections, "chests/abandoned_mineshaft", booster(0.25F, 1, 1));
    add(injections, "chests/stronghold_corridor", booster(0.40F, 1, 2));
    add(injections, "chests/stronghold_library", booster(0.60F, 1, 3));
    add(injections, "chests/desert_pyramid", booster(0.45F, 1, 2));
    add(injections, "chests/jungle_temple", booster(0.45F, 1, 2));
    add(injections, "chests/woodland_mansion", booster(0.50F, 1, 3));
    add(injections, "chests/ancient_city", booster(0.55F, 1, 3));
    add(injections, "chests/end_city_treasure", booster(0.60F, 1, 3));
    add(injections, "chests/bastion_treasure", booster(0.45F, 1, 2));
    add(injections, "chests/pillager_outpost", booster(0.35F, 1, 2));
    add(injections, "entities/witch", boosterFromPlayer(0.15F, 1, 1));
    add(injections, "entities/evoker", boosterFromPlayer(0.50F, 1, 2));
    add(injections, "entities/vindicator", boosterFromPlayer(0.10F, 1, 1));
    add(injections, "entities/warden", boosterFromPlayer(1.00F, 2, 4));
    add(injections, "entities/elder_guardian", boosterFromPlayer(0.75F, 1, 3));
    add(injections, "entities/wither", boosterFromPlayer(1.00F, 3, 5));

    Map<ResourceKey<LootTable>, List<LootInjection>> immutable = new LinkedHashMap<>();
    for (Map.Entry<ResourceKey<LootTable>, List<LootInjection>> entry : injections.entrySet()) {
      immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
    }
    return Map.copyOf(immutable);
  }

  private static void add(Map<ResourceKey<LootTable>, List<LootInjection>> injections,
                          String path, LootInjection injection) {
    ResourceKey<LootTable> key = ResourceKey.create(
        Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
    injections.computeIfAbsent(key, ignored -> new ArrayList<>()).add(injection);
  }

  private static LootInjection page(float chance, int minimum, int maximum) {
    return new LootInjection(LootKind.PAGE, chance, minimum, maximum, false);
  }

  private static LootInjection pageFromPlayer(float chance, int minimum, int maximum) {
    return new LootInjection(LootKind.PAGE, chance, minimum, maximum, true);
  }

  private static LootInjection guidebook(float chance) {
    return new LootInjection(LootKind.GUIDEBOOK, chance, 1, 1, false);
  }

  private static LootInjection booster(float chance, int minimum, int maximum) {
    return new LootInjection(LootKind.BOOSTER, chance, minimum, maximum, false);
  }

  private static LootInjection boosterFromPlayer(float chance, int minimum, int maximum) {
    return new LootInjection(LootKind.BOOSTER, chance, minimum, maximum, true);
  }

  private enum LootKind {
    PAGE,
    GUIDEBOOK,
    BOOSTER
  }

  private record LootInjection(LootKind kind, float chance, int minimum, int maximum,
                               boolean killedByPlayer) {

    boolean enabled() {
      return switch (kind) {
        case PAGE -> MystcraftConfig.enablePageLoot.get();
        case GUIDEBOOK -> true;
        case BOOSTER -> MystcraftConfig.enableBoosterLoot.get();
      };
    }

    LootPool.Builder createPool() {
      LootPool.Builder pool = LootPool.lootPool()
          .setRolls(UniformGenerator.between(kind == LootKind.PAGE ? minimum : 1,
              kind == LootKind.PAGE ? maximum : 1))
          .when(LootItemRandomChanceCondition.randomChance(chance));
      if (killedByPlayer) {
        pool.when(LootItemKilledByPlayerCondition.killedByPlayer());
      }

      switch (kind) {
        case PAGE -> pool.add(LootItem.lootTableItem(FabricRegistries.PAGE.get())
            .apply(() -> new SymbolPageLootFunction(
                SymbolPageLootFunction.Selection.WEIGHTED, 1, SymbolCategory.TERRAIN)));
        case GUIDEBOOK -> pool.add(LootItem.lootTableItem(FabricRegistries.GUIDEBOOK.get()));
        case BOOSTER -> pool.add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
            .apply(SetItemCountFunction.setCount(UniformGenerator.between(minimum, maximum))));
      }
      return pool;
    }
  }
}
