package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.loot.BoosterPackLootModifier;
import art.arcane.mystcraft.loot.GuidebookLootModifier;
import art.arcane.mystcraft.loot.SymbolPageLootModifier;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central holder for all Mystcraft DeferredRegister instances (1.18.2 version).
 * Consolidates all registry definitions including loot modifiers and villagers.
 *
 * Note: 1.18.2 Forge does not have:
 * - FluidType registry (uses different fluid system)
 * - CreativeModeTab registry (uses ItemGroup)
 * - World gen registries are handled differently
 * - Uses GlobalLootModifierSerializer instead of Codec for loot modifiers
 */
public final class MystcraftRegistries {

  // ==================== Core Registries ====================

  public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(ForgeRegistries.BLOCKS, Mystcraft.MOD_ID);

  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, Mystcraft.MOD_ID);

  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, Mystcraft.MOD_ID);

  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(ForgeRegistries.ENTITIES, Mystcraft.MOD_ID);

  public static final DeferredRegister<Fluid> FLUIDS =
      DeferredRegister.create(ForgeRegistries.FLUIDS, Mystcraft.MOD_ID);

  public static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Mystcraft.MOD_ID);

  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(ForgeRegistries.CONTAINERS, Mystcraft.MOD_ID);

  // 1.18.2 uses GlobalLootModifierSerializer - use ForgeRegistries.LOOT_MODIFIER_SERIALIZERS directly
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIERS =
      DeferredRegister.create((Class) GlobalLootModifierSerializer.class, Mystcraft.MOD_ID);

  public static final DeferredRegister<PoiType> POI_TYPES =
      DeferredRegister.create(ForgeRegistries.POI_TYPES, Mystcraft.MOD_ID);

  public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
      DeferredRegister.create(ForgeRegistries.PROFESSIONS, Mystcraft.MOD_ID);

  // ==================== Loot Modifier Registrations ====================
  // 1.18.2 uses Serializer classes instead of Codec suppliers

  @SuppressWarnings("rawtypes")
  public static final RegistryObject<GlobalLootModifierSerializer> SYMBOL_PAGE_LOOT =
      LOOT_MODIFIERS.register("symbol_page", SymbolPageLootModifier.Serializer::new);

  @SuppressWarnings("rawtypes")
  public static final RegistryObject<GlobalLootModifierSerializer> GUIDEBOOK_LOOT =
      LOOT_MODIFIERS.register("guidebook", GuidebookLootModifier.Serializer::new);

  @SuppressWarnings("rawtypes")
  public static final RegistryObject<GlobalLootModifierSerializer> BOOSTER_PACK_LOOT =
      LOOT_MODIFIERS.register("booster_pack", BoosterPackLootModifier.Serializer::new);

  // ==================== Villager Registrations ====================

  /**
   * POI type for the Archivist workstation (Bookstand).
   * In 1.18.2, PoiType constructor requires a string name as first parameter.
   */
  public static final RegistryObject<PoiType> ARCHIVIST_POI = POI_TYPES.register(
      "archivist",
      () -> new PoiType(
          "mystcraft:archivist",
          ImmutableSet.copyOf(ModBlocks.BOOKSTAND.get().getStateDefinition().getPossibleStates()),
          1, 1
      )
  );

  /**
   * The Archivist villager profession.
   */
  public static final RegistryObject<VillagerProfession> ARCHIVIST = VILLAGER_PROFESSIONS.register(
      "archivist",
      () -> new VillagerProfession(
          "archivist",
          ARCHIVIST_POI.get(),
          ImmutableSet.of(),
          ImmutableSet.of(),
          SoundEvents.VILLAGER_WORK_LIBRARIAN
      )
  );

  private MystcraftRegistries() {
  }

  /**
   * Registers all DeferredRegister instances to the mod event bus.
   * Call this from the main mod constructor.
   */
  public static void register(IEventBus modEventBus) {
    BLOCKS.register(modEventBus);
    ITEMS.register(modEventBus);
    BLOCK_ENTITIES.register(modEventBus);
    ENTITIES.register(modEventBus);
    FLUIDS.register(modEventBus);
    SOUNDS.register(modEventBus);
    MENUS.register(modEventBus);
    LOOT_MODIFIERS.register(modEventBus);
    POI_TYPES.register(modEventBus);
    VILLAGER_PROFESSIONS.register(modEventBus);

    Mystcraft.LOGGER.info("Registered all Mystcraft deferred registries");
  }
}
