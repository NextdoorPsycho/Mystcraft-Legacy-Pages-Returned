package art.arcane.mystcraft.loot;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.registry.FabricRegistries;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class FabricLootModifiers {

  private static final ResourceLocation DUNGEON = new ResourceLocation("minecraft", "chests/simple_dungeon");
  private static final ResourceLocation MINESHAFT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");
  private static final ResourceLocation STRONGHOLD_CORRIDOR = new ResourceLocation("minecraft", "chests/stronghold_corridor");
  private static final ResourceLocation STRONGHOLD_LIBRARY = new ResourceLocation("minecraft", "chests/stronghold_library");
  private static final ResourceLocation DESERT_PYRAMID = new ResourceLocation("minecraft", "chests/desert_pyramid");
  private static final ResourceLocation JUNGLE_TEMPLE = new ResourceLocation("minecraft", "chests/jungle_temple");
  private static final ResourceLocation WOODLAND_MANSION = new ResourceLocation("minecraft", "chests/woodland_mansion");

  private FabricLootModifiers() {
  }

  public static void register() {
    LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
      if (source.isBuiltin()) {
        boolean boosterLootEnabled = MystcraftConfig.enableBoosterLoot.get();
        boolean pageLootEnabled = MystcraftConfig.enablePageLoot.get();

        // Symbol pages in dungeons, mineshafts, strongholds
        if (pageLootEnabled && (id.equals(DUNGEON) || id.equals(MINESHAFT) || id.equals(STRONGHOLD_CORRIDOR))) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 3))
              .when(LootItemRandomChanceCondition.randomChance(0.5f))
              .add(LootItem.lootTableItem(FabricRegistries.PAGE.get())));
        }

        // Guidebook in stronghold library
        if (id.equals(STRONGHOLD_LIBRARY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.25f))
              .add(LootItem.lootTableItem(FabricRegistries.GUIDEBOOK.get())));
        }

        // Booster packs in dungeons
        if (boosterLootEnabled && id.equals(DUNGEON)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.35f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        // Booster packs in mineshafts
        if (boosterLootEnabled && id.equals(MINESHAFT)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.25f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())));
        }

        // Booster packs in stronghold corridor
        if (boosterLootEnabled && id.equals(STRONGHOLD_CORRIDOR)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.4f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        // Booster packs in stronghold library (higher chance, more boosters)
        if (boosterLootEnabled && id.equals(STRONGHOLD_LIBRARY)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.6f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }

        // Booster packs in temples
        if (boosterLootEnabled && (id.equals(DESERT_PYRAMID) || id.equals(JUNGLE_TEMPLE))) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.45f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
        }

        // Booster packs in woodland mansion
        if (boosterLootEnabled && id.equals(WOODLAND_MANSION)) {
          tableBuilder.withPool(LootPool.lootPool()
              .setRolls(UniformGenerator.between(1, 1))
              .when(LootItemRandomChanceCondition.randomChance(0.5f))
              .add(LootItem.lootTableItem(FabricRegistries.BOOSTER_PACK.get())
                  .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }
      }
    });
  }
}
