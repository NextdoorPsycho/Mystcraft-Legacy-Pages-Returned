package art.arcane.mystcraft.loot;

import art.arcane.mystcraft.registry.FabricModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class FabricLootModifiers {

    private static final ResourceLocation DUNGEON = new ResourceLocation("minecraft", "chests/simple_dungeon");
    private static final ResourceLocation MINESHAFT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");
    private static final ResourceLocation STRONGHOLD_CORRIDOR = new ResourceLocation("minecraft", "chests/stronghold_corridor");
    private static final ResourceLocation STRONGHOLD_LIBRARY = new ResourceLocation("minecraft", "chests/stronghold_library");

    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (source.isBuiltin()) {
                if (id.equals(DUNGEON) || id.equals(MINESHAFT) || id.equals(STRONGHOLD_CORRIDOR)) {
                    tableBuilder.withPool(LootPool.lootPool()
                            .setRolls(UniformGenerator.between(1, 3))
                            .when(LootItemRandomChanceCondition.randomChance(0.5f))
                            .add(LootItem.lootTableItem(FabricModItems.PAGE.get())));
                }

                if (id.equals(STRONGHOLD_LIBRARY)) {
                    tableBuilder.withPool(LootPool.lootPool()
                            .setRolls(UniformGenerator.between(1, 1))
                            .when(LootItemRandomChanceCondition.randomChance(0.25f))
                            .add(LootItem.lootTableItem(FabricModItems.GUIDEBOOK.get())));
                }
            }
        });
    }

    private FabricLootModifiers() {}
}
