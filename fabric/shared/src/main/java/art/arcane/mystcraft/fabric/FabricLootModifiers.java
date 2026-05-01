package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.config.MystcraftConfig;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Adds Mystcraft pages, guidebooks, and booster packs to Fabric loot tables.
 */
public final class FabricLootModifiers {

  private static final ResourceLocation DUNGEON = new ResourceLocation("minecraft", "chests/simple_dungeon");
  private static final ResourceLocation MINESHAFT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");
  private static final ResourceLocation STRONGHOLD_CORRIDOR = new ResourceLocation("minecraft", "chests/stronghold_corridor");
  private static final ResourceLocation STRONGHOLD_LIBRARY = new ResourceLocation("minecraft", "chests/stronghold_library");
  private static final ResourceLocation DESERT_PYRAMID = new ResourceLocation("minecraft", "chests/desert_pyramid");
  private static final ResourceLocation JUNGLE_TEMPLE = new ResourceLocation("minecraft", "chests/jungle_temple");
  private static final ResourceLocation WOODLAND_MANSION = new ResourceLocation("minecraft", "chests/woodland_mansion");
  private static final ResourceLocation ANCIENT_CITY = new ResourceLocation("minecraft", "chests/ancient_city");
  private static final ResourceLocation SHIPWRECK_TREASURE = new ResourceLocation("minecraft", "chests/shipwreck_treasure");
  private static final ResourceLocation BURIED_TREASURE = new ResourceLocation("minecraft", "chests/buried_treasure");
  private static final ResourceLocation END_CITY = new ResourceLocation("minecraft", "chests/end_city_treasure");
  private static final ResourceLocation BASTION_TREASURE = new ResourceLocation("minecraft", "chests/bastion_treasure");
  private static final ResourceLocation NETHER_BRIDGE = new ResourceLocation("minecraft", "chests/nether_bridge");
  private static final ResourceLocation RUINED_PORTAL = new ResourceLocation("minecraft", "chests/ruined_portal");
  private static final ResourceLocation PILLAGER_OUTPOST = new ResourceLocation("minecraft", "chests/pillager_outpost");

  private static final ResourceLocation WITCH = new ResourceLocation("minecraft", "entities/witch");
  private static final ResourceLocation EVOKER = new ResourceLocation("minecraft", "entities/evoker");
  private static final ResourceLocation VINDICATOR = new ResourceLocation("minecraft", "entities/vindicator");
  private static final ResourceLocation PILLAGER = new ResourceLocation("minecraft", "entities/pillager");
  private static final ResourceLocation WARDEN = new ResourceLocation("minecraft", "entities/warden");
  private static final ResourceLocation ELDER_GUARDIAN = new ResourceLocation("minecraft", "entities/elder_guardian");
  private static final ResourceLocation WITHER = new ResourceLocation("minecraft", "entities/wither");

  private FabricLootModifiers() {
  }

  public static void register() {
    LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
      boolean boosterLootEnabled = MystcraftConfig.enableBoosterLoot.get();
      boolean pageLootEnabled = MystcraftConfig.enablePageLoot.get();

      if (source.isBuiltin()) {

        if (pageLootEnabled && (id.equals(DUNGEON) || id.equals(MINESHAFT) || id.equals(STRONGHOLD_CORRIDOR))) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 3))
              .when(LootItemRandomChanceCondition.randomChance(0.5f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (id.equals(STRONGHOLD_LIBRARY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.25f))
              .add(LootItem.lootTableItem(FabricRegistries.GUIDEBOOK.get())));
        }

        if (boosterLootEnabled && id.equals(DUNGEON)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.35f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        if (boosterLootEnabled && id.equals(MINESHAFT)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.25f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())));
        }

        if (boosterLootEnabled && id.equals(STRONGHOLD_CORRIDOR)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.4f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        if (boosterLootEnabled && id.equals(STRONGHOLD_LIBRARY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.6f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }

        if (boosterLootEnabled && (id.equals(DESERT_PYRAMID) || id.equals(JUNGLE_TEMPLE))) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.45f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        if (boosterLootEnabled && id.equals(WOODLAND_MANSION)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.5f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }

        if (boosterLootEnabled && id.equals(ANCIENT_CITY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.55f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }
        if (pageLootEnabled && id.equals(ANCIENT_CITY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 4))
              .when(LootItemRandomChanceCondition.randomChance(0.65f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (pageLootEnabled && id.equals(SHIPWRECK_TREASURE)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 2))
              .when(LootItemRandomChanceCondition.randomChance(0.35f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (pageLootEnabled && id.equals(BURIED_TREASURE)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 2))
              .when(LootItemRandomChanceCondition.randomChance(0.4f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (boosterLootEnabled && id.equals(END_CITY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.6f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }
        if (pageLootEnabled && id.equals(END_CITY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 3))
              .when(LootItemRandomChanceCondition.randomChance(0.5f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (boosterLootEnabled && id.equals(BASTION_TREASURE)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.45f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        if (pageLootEnabled && id.equals(NETHER_BRIDGE)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 2))
              .when(LootItemRandomChanceCondition.randomChance(0.4f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (pageLootEnabled && id.equals(RUINED_PORTAL)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 2))
              .when(LootItemRandomChanceCondition.randomChance(0.3f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (boosterLootEnabled && id.equals(PILLAGER_OUTPOST)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.35f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        if (boosterLootEnabled && id.equals(WITCH)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemKilledByPlayerCondition.killedByPlayer())
              .when(LootItemRandomChanceCondition.randomChance(0.15f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())));
        }

        if (boosterLootEnabled && id.equals(EVOKER)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemKilledByPlayerCondition.killedByPlayer())
              .when(LootItemRandomChanceCondition.randomChance(0.5f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        if (boosterLootEnabled && id.equals(VINDICATOR)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemKilledByPlayerCondition.killedByPlayer())
              .when(LootItemRandomChanceCondition.randomChance(0.1f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())));
        }

        if (pageLootEnabled && id.equals(PILLAGER)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemKilledByPlayerCondition.killedByPlayer())
              .when(LootItemRandomChanceCondition.randomChance(0.05f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        if (boosterLootEnabled && id.equals(WARDEN)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemKilledByPlayerCondition.killedByPlayer())
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4)))));
        }

        if (boosterLootEnabled && id.equals(ELDER_GUARDIAN)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemKilledByPlayerCondition.killedByPlayer())
              .when(LootItemRandomChanceCondition.randomChance(0.75f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }

        if (boosterLootEnabled && id.equals(WITHER)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemKilledByPlayerCondition.killedByPlayer())
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 5)))));
        }
      }
    });
  }
}
